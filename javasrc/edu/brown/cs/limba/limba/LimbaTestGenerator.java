/********************************************************************************/
/*                                                                              */
/*              LimbaTestGenerator.java                                         */
/*                                                                              */
/*      Generate and insert test case                                           */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2011 Brown University -- Steven P. Reiss                    */
/*********************************************************************************
 *  Copyright 2011, Brown University, Providence, RI.                            *
 *                                                                               *
 *                        All Rights Reserved                                    *
 *                                                                               *
 * This program and the accompanying materials are made available under the      *
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, *
 * and is available at                                                           *
 *      http://www.eclipse.org/legal/epl-v10.html                                *
 *                                                                               *
 ********************************************************************************/



package edu.brown.cs.limba.limba;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.w3c.dom.Element;

import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.ivy.file.IvyLog;
import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;


class LimbaTestGenerator implements LimbaConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private LimbaMain       limba_main;
private String          base_prompt;
private String          code_totest;
private boolean         use_context;
private String          source_type;
private String          source_file;
private String          target_class;
private File            target_file;
private boolean         new_class;

private Set<String>    import_types;
private Set<String>    static_imports;
private List<TestDecl> test_decls;

private Set<String>     prior_imports;
private Set<String>     prior_statics;
private String          prior_package;




/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

LimbaTestGenerator(LimbaMain lm,String prompt,Element xml)
{
   limba_main = lm;
   base_prompt = prompt;
   use_context = IvyXml.getAttrBool(xml,"USECONTEXT",true);  Element txml = IvyXml.getChild(xml,"TOTEST");
   code_totest = IvyXml.getTextElement(txml,"CODE");
   new_class = IvyXml.getAttrBool(txml,"NEWCLASS");
   source_type = IvyXml.getAttrString(txml,"SOURCETYPE").toLowerCase();
   source_file = IvyXml.getAttrString(txml,"SOURCEFILE");
   
   target_class = IvyXml.getAttrString(txml,"TARGETCLASS");
   String tnm = IvyXml.getAttrString(txml,"TARGETFILE");
   if (tnm == null) target_file = null;
   else target_file = new File(tnm);
   
   import_types = new TreeSet<>();
   static_imports = new TreeSet<>();
   prior_imports = new HashSet<>();
   prior_statics = new HashSet<>();
   prior_package = null;
   test_decls = new ArrayList<>();
}


/********************************************************************************/
/*                                                                              */
/*      Processing methods                                                      */
/*                                                                              */
/********************************************************************************/

void process(IvyXmlWriter xw) throws Exception
{
   StringBuffer pbuf = new StringBuffer();
   if (base_prompt != null) pbuf.append(base_prompt);
   pbuf.append("\nPlease create test cases for the " + source_type);
   if (source_file != null) {
      pbuf.append(" from the file " + source_file);
    }
   pbuf.append(" for the code below.\n");
   pbuf.append("\nTne test cases will be inserted in the ");
   if (new_class) pbuf.append("new ");
   pbuf.append("class " + target_class + ".\n");
   pbuf.append("The code to test is: \n");
   pbuf.append(code_totest);
   
   IvyLog.logD("LIMBA","Find  " + pbuf.toString());
   
   String resp = limba_main.askOllama(pbuf.toString(),use_context);
   
   List<String> code = LimbaMain.getJavaCode(resp);
   TestDecl top = null;
   for (String s : code) {
      top = analyzeResult(s);
      xw.cdataElement("TESTCODE",s);
    }
   if (top != null) {
     top.outputXml(xw,"TOP");
    }
   
   for (String s : import_types) {
      xw.begin("IMPORT");
      xw.text(s);
      xw.end("IMPORT");
    }
   for (String s : static_imports) {
      xw.begin("IMPORT");
      xw.field("STATIC",true);
      xw.text(s);
      xw.end("IMPORT");
    }
   for (TestDecl td : test_decls) {
      td.outputXml(xw,"DECL");
    }
}


/********************************************************************************/
/*                                                                              */
/*      Process the returned items                                              */
/*                                                                              */
/********************************************************************************/

TestDecl analyzeResult(String code)
{
   CompilationUnit cu;
   
   try {
      cu = JcompAst.parseSourceFile(code);
    }
   catch (Throwable t) {
      IvyLog.logE("LIMBA","Result is not a compilation unit");
      return null;
    }
   
   getTargetInformation();
   
   for (Object o : cu.imports()) {
      ImportDeclaration id = (ImportDeclaration) o;
      String typ = id.getName().toString();
      if (id.isOnDemand()) typ += ".*";
      if (isImportNeeded(typ,id.isStatic())) {
         if (id.isStatic()) { 
            static_imports.add(typ);
          }
         else {
            import_types.add(typ);
          }
       }
    }
   
   TestDecl top = null;
   TypeDeclaration typedecl = null;
   for (Object o1 : cu.types()) {
      if (o1 instanceof TypeDeclaration) {
         typedecl = (TypeDeclaration) o1;
         top = new TestDecl(typedecl);
         break;
       }
    }
   if (typedecl == null) return null;
   
   for (Object o1 : typedecl.bodyDeclarations()) {
      TestDecl testd = null;
      if (o1 instanceof TypeDeclaration) {
         testd = new TestDecl((TypeDeclaration) o1);
       }
      else if (o1 instanceof FieldDeclaration) {
         testd = new TestDecl((FieldDeclaration) o1);
       }
      else if (o1 instanceof MethodDeclaration) {
         testd = new TestDecl((MethodDeclaration) o1);
       }
      if (testd != null) test_decls.add(testd);
    }
   
   return top;
}



/********************************************************************************/
/*                                                                              */
/*      Get information for the target file to insert into                      */
/*                                                                              */
/********************************************************************************/

private void getTargetInformation()
{
   if (target_file == null) return;
   
   CompilationUnit cu = null; 
   try {
      String src = IvyFile.loadFile(target_file);
      cu = JcompAst.parseSourceFile(src);
    }
   catch (Throwable t) {
      IvyLog.logE("LIMBA","Problem reading test case source file",t);
      return;
    }
   if (cu == null) return;
   
   
   for (Object o : cu.imports()) {
      ImportDeclaration id = (ImportDeclaration) o;
      String typ = id.getName().toString();
      if (id.isOnDemand()) typ += ".*";
      if (id.isStatic()) { 
         prior_statics.add(typ);
       }
      else {
         prior_imports.add(typ);
       }
    }
   
   PackageDeclaration pd = cu.getPackage();
   prior_package = pd.getName().getFullyQualifiedName();
}


private boolean isImportNeeded(String typ,boolean isstatic)
{
   if (isstatic && prior_statics.contains(typ)) return false;
   if (!isstatic && prior_imports.contains(typ)) return false;
   
   String pkg = "";
   int idx = typ.lastIndexOf(".");
   if (idx > 0) pkg = typ.substring(0,idx);
   if (pkg.equals("java.lang")) return false;
   if (prior_package != null && pkg.equals(prior_package)) return false;
   
   return true;
}



/********************************************************************************/
/*                                                                              */
/*      Declaration of a test case or helper                                    */
/*                                                                              */
/********************************************************************************/

private final class TestDecl {

   private String decl_code;
   private boolean is_field;
   private boolean is_helper;
   private BodyDeclaration body_decl;
   
   TestDecl(TypeDeclaration td) {
      body_decl = td;
      is_helper = true;
      is_field = false;
      decl_code = td.toString();
    }
   
   TestDecl(FieldDeclaration fd) {
      body_decl = fd;
      is_helper = true;
      is_field = true;
      decl_code = fd.toString();
    }
   
   TestDecl(MethodDeclaration md) {
      body_decl = md;
      decl_code = md.toString();
      if (!decl_code.startsWith("@Test") && !md.getName().getIdentifier().startsWith("test")) {
         is_helper = true;
       }
      is_field = false;
    }
   
   void outputXml(IvyXmlWriter xw,String what) {
      xw.begin(what);
      if (is_field) xw.field("FIELD",is_field);
      if (is_helper) xw.field("HELPER",is_helper);
      else xw.field("TEST",true);
      xw.field("MODINT",body_decl.getModifiers());
      
      if (body_decl instanceof MethodDeclaration) {
         MethodDeclaration md = (MethodDeclaration) body_decl;
         xw.field("NAME",md.getName().toString());
         if (md.isConstructor()) xw.field("CONSTRUCTOR",true);
         else {
            xw.textElement("RETURNS",md.getReturnType2().toString());
          }
         StringBuffer buf = new StringBuffer();
         xw.textElement("PARAMETERS","(" + getListValue(md.parameters(),",") + ")");
         for (Object o : md.parameters()) {
            SingleVariableDeclaration svd = (SingleVariableDeclaration) o;
            if (!buf.isEmpty()) buf.append(",");
            buf.append(svd.toString());
          }
         xw.textElement("PARAMTERS","(" + buf.toString() + ")");
         xw.cdataElement("CONTENTS",md.getBody().toString());
       }
      else if (body_decl instanceof FieldDeclaration) {
         FieldDeclaration fd = (FieldDeclaration) body_decl;
         xw.textElement("RETURNS",fd.getType().toString());
         for (Object o : fd.fragments()) {
            VariableDeclarationFragment vdf = (VariableDeclarationFragment) o;
            xw.begin("FIELD");
            xw.field("NAME",vdf.getName().toString());
            if (vdf.getExtraDimensions() != 0) {
               xw.field("DIMS",vdf.getExtraDimensions());
             }
            if (vdf.getInitializer() != null) {
               xw.cdataElement("INIT",vdf.getInitializer().toString());
             }
            xw.end("FIELD");
          }
       }
      else if (body_decl instanceof TypeDeclaration) {
         if (what.equals("DECL")) xw.field("INNERTYPE",true);
         TypeDeclaration td = (TypeDeclaration) body_decl;
         if (td.isInterface()) xw.field("INTERFACE",true);
         if (td.getSuperclassType() != null) {
            xw.field("SUPERCLASS",td.getSuperclassType().toString());
          }
         xw.textElement("IMPLEMENTS",getListValue(td.superInterfaceTypes(),","));
         String cnts = decl_code;
         int idx1 = cnts.indexOf("{");
         int idx2 = cnts.lastIndexOf("}");
         cnts = cnts.substring(idx1,idx2+1);
         xw.cdataElement("CONTENTS",cnts);
       }
      
      Javadoc jd = body_decl.getJavadoc();
      if (jd != null) {
         xw.cdataElement("JAVADOC",jd.toString());
       }
      xw.textElement("ATTRIBUTES",getAttributes());
      xw.cdataElement("RAWCODE",decl_code);
      
      xw.end(what);
    }
   
   private String getAttributes() {
      StringBuffer buf = new StringBuffer();
      for (Object o : body_decl.modifiers()) {
         IExtendedModifier em = (IExtendedModifier) o;
         if (em.isAnnotation()) {
            if (!buf.isEmpty()) buf.append(" ");
            buf.append(o.toString());
          }
       }
      return buf.toString();
    }
   
   private String getListValue(List<?> data,String sep) {
      StringBuffer buf = new StringBuffer();
      for (Object o : data) {
         if (!buf.isEmpty()) buf.append(sep);
         buf.append(o.toString());
       }
      return buf.toString();
    }
   
}       // end of inner class TestDecl

}       // end of class LimbaTestGenerator




/* end of LimbaTestGenerator.java */
