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
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Message;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeParameter;

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
/*      Find undefined types                                                    */
/*                                                                              */
/********************************************************************************/

private class CheckTypesDefined extends ASTVisitor {

   private CompilationUnit comp_unit;
   private List<String> error_list;
   private Set<String> bad_types;
   
   CheckTypesDefined(CompilationUnit cu,List<String> errs) {
      comp_unit = cu;
      error_list = errs;
    }
   
   void process() {
      comp_unit.accept(this);
      for (String bt : bad_types) {
         String err = "The type " + bt + " is not defined.";
         error_list.add(err);
       }
    }
   
   @Override public void endVisit(SimpleName n) {
      JcompType typ = JcompAst.getJavaType(n);
      if (typ.isUnknown()) {
         bad_types.add(typ.getName());
       }
    }
   
   @Override public void endVisit(TypeDeclaration n) { 
    }
   
   @Override public void endVisit(TypeParameter n) { 
    }
   
}

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

