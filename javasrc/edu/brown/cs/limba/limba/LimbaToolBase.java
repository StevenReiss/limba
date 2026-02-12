/********************************************************************************/
/*                                                                              */
/*              LimbaToolBase.java                                              */
/*                                                                              */
/*      description of class                                                    */
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



package edu.brown.cs.limba.limba;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.w3c.dom.Element;

import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.ivy.file.IvyFormat;
import edu.brown.cs.ivy.file.IvyLog;
import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.jcomp.JcompControl;
import edu.brown.cs.ivy.jcomp.JcompProject;
import edu.brown.cs.ivy.xml.IvyXml;

abstract class LimbaToolBase implements LimbaConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

protected LimbaMain     limba_main;
protected LimbaMonitor  message_server;
private Collection<File> project_files;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

protected LimbaToolBase(LimbaMain lm,Collection<File> files)
{
   limba_main = lm;
   message_server = lm.getMessageServer();
   
   if (files != null) {
      project_files = new ArrayList<>(files);
    }
   else {
      project_files = null;
    }
}



/********************************************************************************/
/*                                                                              */
/*      Helper methods                                                          */
/*                                                                              */
/********************************************************************************/

protected TypeDeclaration findClassAst(String name,boolean resolve)
{
   File f1 = findClassFile(name);
   if (f1 == null) return null;
   
   String cnts = null;
   try {
      cnts = IvyFile.loadFile(f1);
    }
   catch (IOException e) { }
   if (cnts == null || cnts.isEmpty()) return null;
   
   CompilationUnit cu = JcompAst.parseSourceFile(cnts);
   
   if (resolve) {
      JcompControl jcomp = limba_main.getJcompControl();
      JcompProject jp = JcompAst.getResolvedAst(jcomp,cu,null);
      if (jp == null) {
         IvyLog.logD("LIMBA","Unable to resolve AST for tools " + name);
       }
    }
   
   String cnm = f1.getName();
   int idx = cnm.lastIndexOf(".");
   cnm = cnm.substring(0,idx);
   String name1 = name.replace("$",".");
   int idx1 = name1.lastIndexOf(cnm + ".");
   String subnm = null;
   if (idx1 > 0) {
      subnm = name1.substring(idx1+1);
    }
   
   for (Object o1 : cu.types()) {
      TypeDeclaration td = (TypeDeclaration) o1;
      String tnm = td.getName().getIdentifier();
      if (tnm.equals(cnm)) {
         if (subnm == null) return td;
         return findInnerType(td,subnm);
       }
    }
   
   return null;
}


private TypeDeclaration findInnerType(TypeDeclaration td,String name)
{
   String subnm = null;
   String name1 = name;
   int idx1 = name1.indexOf(".");
   if (idx1 > 0) {
      subnm = name1.substring(idx1+1);
      name1 = name1.substring(0,idx1);
    }
   
   for (Object o1 : td.bodyDeclarations()) {
      if (o1 instanceof TypeDeclaration) {
         TypeDeclaration intd = (TypeDeclaration) o1;
         if (td.getName().getIdentifier().equals(name1)) {
            if (subnm == null) return intd;
            return findInnerType(intd,subnm);
          }
       }
    }
   return null;
}


private File findClassFile(String name)
{
   if (message_server != null) {
      Element xml = message_server.findClass(name);
      if (xml != null && IvyXml.isElement(xml,"RESULT")) {
         Element xml1 = IvyXml.getChild(xml,"MATCH");
         String f = IvyXml.getAttrString(xml1,"FILE");
         if (f != null) {
            return new File(f);
          }
       }
    }
   
   if (project_files == null) return null;
   
   List<String> possibles = new ArrayList<>();
   String n1 = name;
   int idx = n1.indexOf("$");
   if (idx > 0) {
      n1 = n1.substring(0,idx);
    }
   n1 = n1.replace(".",File.separator);
   for ( ; ; ) {
      String n2 = n1 + ".java";
      possibles.add(n2);
      int idx1 = n1.lastIndexOf(File.separator);
      if (idx < 0) break;
      n1 = n1.substring(0,idx1);
    }
   
   for (File f : project_files) {
      String pnm = f.getPath();
      for (String n3 : possibles) {
         if (pnm.endsWith(n3)) {
            return f;
          }
       }
    }
   
   return null;
}


/********************************************************************************/
/*                                                                              */
/*      Get method description                                                  */
/*                                                                              */
/********************************************************************************/

protected String getMethodDescription(MethodDeclaration md)
{
   String mtxt = md.toString();
   int idx3 = mtxt.indexOf("(");
   if (idx3 < 0) idx3 = 0;
   int idx2 = mtxt.indexOf("{",idx3);
   if (idx2 < 0) idx2 = mtxt.indexOf(";",idx3);
   if (idx2 > 0) mtxt = mtxt.substring(0,idx2);
   mtxt = mtxt.trim();
   
   return mtxt;
}


protected String getFieldDescription(FieldDeclaration fd,VariableDeclarationFragment vdf)
{
   StringBuffer buf = new StringBuffer();
   Javadoc jd = fd.getJavadoc();
   if (jd != null) buf.append(jd);
   
   for (Object o1 : fd.modifiers()) {
      IExtendedModifier mod = (IExtendedModifier) o1;
      if (!buf.isEmpty()) buf.append(" ");
      buf.append(mod.toString());
    }
   
   if (!buf.isEmpty()) buf.append(" ");
   buf.append(fd.getType().toString());
   buf.append(" ");
   buf.append(vdf.toString());
   buf.append(";");
   
   return buf.toString();
}


protected Element getMethodMatches(String name)
{
   if (message_server == null || name == null) return null;
   
   Element xml = message_server.findMethod(name);
   if (xml != null && IvyXml.getChild(xml, "MATCH") != null) {
      return xml;
    }
   
   Element xml1 = message_server.findClass(name);
   if (xml1 != null && IvyXml.getChild(xml, "MATCH") != null) {
      return xml;
    }
   
   return xml;
}


/********************************************************************************/
/*                                                                              */
/*      Get source lines with line numbers                                      */
/*                                                                              */
/********************************************************************************/


protected static String normalizeMethodName(String name0)
{
   if (name0 == null) return null;
   
   String name = name0;
   if (name.contains(":(")) name = name.replace(":(","(");
   if (name.contains("(")) {
      int idx0 = name.indexOf("(");
      int idx1 = name.lastIndexOf(")");
      String args0 = name.substring(idx0,idx1+1);
      if (args0.contains(" ")) args0 = args0.replace(" ","");
      else {
         String args1 = IvyFormat.formatTypeName(args0);
         if (!args1.contains(",,")) args0 = args1;
       }
      name = name.substring(0,idx0) + args0;
    }
   if (name.contains(":")) {
      name = name.replace(":\\d+","");
    }
   int idx4 = name.indexOf("(");
   if (idx4 < 0) idx4 = name.length();
   int idx2 = name.lastIndexOf(".",idx4);
   if (idx2 > 0) {
      String mnm = name.substring(idx2+1);
      String match = mnm + "." + mnm;
      if (name.equals(match) || name.endsWith("." + match)) {
         name = name.substring(0,idx2) + ".<init>";
       }
    }
   
   if (!name0.equals(name)) {
      IvyLog.logD("LIMBA","Normalize " + name0 + " = " + name);
    }
   
   return name;
}



}       // end of class LimbaToolBase




/* end of LimbaToolBase.java */

