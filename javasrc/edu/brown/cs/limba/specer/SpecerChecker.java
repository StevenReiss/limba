/********************************************************************************/
/*                                                                              */
/*              SpecerChecker.java                                              */
/*                                                                              */
/*      Check the Java interface definition for completness & consistency       */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2025 Brown University -- Steven P. Reiss                    */
/*********************************************************************************
 *  Copyright 2025, Brown University, Providence, RI.                            *
 *                                                                               *
 *                        All Rights Reserved                                    *
 *                                                                               *
 *  Permission to use, copy, modify, and distribute this software and its        *
 *  documentation for any purpose other than its incorporation into a            *
 *  commercial product is hereby granted without fee, provided that the          *
 *  above copyright notice appear in all copies and that both that               *
 *  copyright notice and this permission notice appear in supporting             *
 *  documentation, and that the name of Brown University not be used in          *
 *  advertising or publicity pertaining to distribution of the software          *
 *  without specific, written prior permission.                                  *
 *                                                                               *
 *  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS                *
 *  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND            *
 *  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY      *
 *  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY          *
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,              *
 *  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS               *
 *  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE          *
 *  OF THIS SOFTWARE.                                                            *
 *                                                                               *
 ********************************************************************************/



package edu.brown.cs.limba.specer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.Message;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.jcomp.JcompControl;
import edu.brown.cs.ivy.jcomp.JcompProject;
import edu.brown.cs.ivy.jcomp.JcompSemantics;
import edu.brown.cs.ivy.jcomp.JcompSource;
import edu.brown.cs.ivy.jcomp.JcompType;

class SpecerChecker implements SpecerConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private String       java_code;

private static JcompControl jcomp_control = new JcompControl();

private static final Set<String> SAFE_TYPES;

static {
   SAFE_TYPES = new HashSet<>();
   SAFE_TYPES.add("Point2D");
}


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

SpecerChecker(String code)
{
   java_code = code;
}



/********************************************************************************/
/*                                                                              */
/*      Processing methods                                                      */
/*                                                                              */
/********************************************************************************/

List<String> process()
{
   JcompSource src = new DummySource("LimbaSpecCode.java",java_code);
   List<JcompSource> srcs = List.of(src);
   JcompProject jproj = jcomp_control.getProject(srcs);
   
   jproj.resolve(); 
   
   List<String> errors = new ArrayList<>();
   
   Collection<JcompSemantics> sems = jproj.getSources();
   for (JcompSemantics jsem : sems) {
      CompilationUnit cu = jsem.getRootNode();
      addErrors(cu,errors);
    }
   
   return errors;
}



/********************************************************************************/
/*                                                                              */
/*      Add errors for a file                                                   */
/*                                                                              */
/********************************************************************************/

private void addErrors(CompilationUnit cu,List<String> errors)
{
   CheckParseErrors cpe = new CheckParseErrors(cu,errors);
   cpe.process();
   if (errors.size() > 0) return;
   
   CheckTypesDefined ctd = new CheckTypesDefined(cu,errors);
   ctd.process();
   
   CheckMissingJavaDoc mjd = new CheckMissingJavaDoc(cu,errors);
   mjd.process();
   
   // check that there are methods that return items of each type
   //    that isn't an enum, exception, or callback for items that
   //    are used as parameters
   // check that all types are used by some method (except enums,
   //    exceptions, top-level items)
   // check for a main program?
}


/********************************************************************************/
/*                                                                              */
/*      Handle parse errors                                                     */
/*                                                                              */
/********************************************************************************/

private class CheckParseErrors {
   
   private CompilationUnit comp_unit;
   private List<String> error_list;
   
   CheckParseErrors(CompilationUnit cu,List<String> errs) {
      comp_unit = cu;
      error_list = errs;;
    }
   
   void process() {
      for (Message m : comp_unit.getMessages()) {
         String txt = m.getMessage();
         int lno = comp_unit.getLineNumber(m.getStartPosition());
         String err = txt + " at line " + lno;
         error_list.add(err);
       }
    }
   
}       // end of inner class CheckParseErrors



/********************************************************************************/
/*                                                                              */
/*      Utility function -- get outer class name                                */
/*                                                                              */
/********************************************************************************/

private String getClassName(ASTNode n)
{
   for (ASTNode p = n; p != null; p = p.getParent()) {
      if (p instanceof AbstractTypeDeclaration) {
         AbstractTypeDeclaration atd = (AbstractTypeDeclaration) p;
         String nm = atd.getName().getIdentifier();
         return nm;
       }
    }
   
   return null;
}



/********************************************************************************/
/*                                                                              */
/*      Find undefined types                                                    */
/*                                                                              */
/********************************************************************************/

private class CheckTypesDefined extends ASTVisitor {

   private CompilationUnit comp_unit;
   private List<String> error_list;
   private Set<String> bad_types;
   private boolean uses_object;
   
   CheckTypesDefined(CompilationUnit cu,List<String> errs) {
      comp_unit = cu;
      error_list = errs;
      bad_types = new HashSet<>();
      uses_object = false;
    }
   
   void process() {
      comp_unit.accept(this);
      for (String bt : bad_types) {
         String err = "The type " + bt + " is not defined.";
         error_list.add(err);
       }
      if (uses_object) {
         String err = "Avoid using the type java.lang.Object.";
         error_list.add(err);
       }
    }
   
   @Override public boolean visit(MethodDeclaration md) {
      if (md.getName().getIdentifier().equals("equals")) return false;
      return true;
    }
   
   @Override public boolean visit(ParameterizedType pt) {
      if (pt.toString().contains("Object")) return false;
      return true;
    }
   
   @Override public void endVisit(SimpleName n) {
      JcompType typ = JcompAst.getJavaType(n);
      if (typ != null && typ.isUndefined()) {
         String nm = typ.getName();
         int idx = nm.lastIndexOf(".");
         if (idx > 0) {
            nm = nm.substring(idx+1);
          }
         if (!SAFE_TYPES.contains(nm)) {
            bad_types.add(nm);
          }
       }
      else if (typ != null && typ.isJavaLangObject()) {
         System.err.println("CHECK OBJECT");
         // need to allow Object in equals and other
         //   generic methods
   //    uses_object = true;
       }
    }
   
}       // end of inner class CheckTypesDefines



/********************************************************************************/
/*                                                                              */
/*      Check for missing JavaDoc                                               */
/*                                                                              */
/********************************************************************************/

private class CheckMissingJavaDoc extends ASTVisitor {

   private CompilationUnit comp_unit;
   private List<String> error_list;
   
   CheckMissingJavaDoc(CompilationUnit cu,List<String> errs) {
      comp_unit = cu;
      error_list = errs;
    }
   
   void process() {
      comp_unit.accept(this);
    }
   
   @Override public void endVisit(FieldDeclaration n) {
      List<?> frags = n.fragments();
      if (frags.size() > 1) {
         error_list.add("Declare fields ones at a time: " + frags + ".");
       }
      VariableDeclarationFragment vdf = (VariableDeclarationFragment) frags.get(0);
      Javadoc jd = n.getJavadoc();
      if (jd == null) {
         String nm = vdf.getName().getIdentifier();
         String cnm = getClassName(n);
         if (cnm != null) nm = cnm + "." + nm;
         error_list.add("Field " + nm + " is missing JavaDoc.");
       }
    }
   
   @Override public void endVisit(MethodDeclaration n) {
      Javadoc jd = n.getJavadoc();
      if (jd == null && !n.isConstructor()) {
         String nm = n.getName().getIdentifier();
         String cnm = getClassName(n);
         if (cnm != null) nm = cnm + "." + nm;
         error_list.add("Method " + nm + " is missing JavaDoc.");
       }
    }

}       // end of inner class CheckTypesDefines




/********************************************************************************/
/*                                                                              */
/*      Dummy source file                                                       */
/*                                                                              */
/********************************************************************************/

private static class DummySource implements JcompSource {
   
   private String source_name;
   private String source_cnts;
   
   DummySource(String nm,String cnts) {
      source_name = nm;
      source_cnts = cnts;
    }
   
   @Override public String getFileContents()            { return source_cnts; }
   @Override public String getFileName()                { return source_name; }
   
}

}       // end of class SpecerChecker




/* end of SpecerChecker.java */

