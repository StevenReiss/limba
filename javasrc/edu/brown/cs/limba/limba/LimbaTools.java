/********************************************************************************/
/*                                                                              */
/*              LimbaTools.java                                                 */
/*                                                                              */
/*      Tools for use in program-related queries                                */
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.w3c.dom.Element;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.ivy.file.IvyLog;
import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.jcomp.JcompControl;
import edu.brown.cs.ivy.jcomp.JcompProject;
import edu.brown.cs.ivy.xml.IvyXml;

public class LimbaTools implements LimbaConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private LimbaMain       limba_main;
private LimbaMsg        message_server;
private Collection<File> project_files;

/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

LimbaTools(LimbaMain lm,Collection<File> files)
{
   limba_main = lm;
   message_server = lm.getMessageServer();
   project_files = new ArrayList<>(files);
}



/********************************************************************************/
/*                                                                              */
/*      Tool to return constructor information                                  */
/*                                                                              */
/********************************************************************************/

@Tool("returns the set of available constructors for a given class")
public List<String> getConstructorsForClass(@P("name of the class") String name)
{
   List<String> rslt = new ArrayList<>();
   
   TypeDeclaration td = findClassAst(name,false);
   
   IvyLog.logD("LIMBA","Find constructors for class " + name);
   
   if (td != null) {
      for (Object o1 : td.bodyDeclarations()) {
         if (o1 instanceof MethodDeclaration) {
            MethodDeclaration md = (MethodDeclaration) o1;
            if (!md.isConstructor()) continue;
            String txt = getMethodDescription(md);
            rslt.add(txt);
          }
       }
    }
   
   IvyLog.logD("LIMBA","Result is " + rslt);
   
   return rslt;
}



/********************************************************************************/
/*                                                                              */
/*      Tool to return information about a method                               */
/*                                                                              */
/********************************************************************************/

@Tool("return the signature and javadoc describing a method")
public String getMethodInformation(@P("full name of the method") String name)
{
   int idx = name.indexOf("(");
   int idx1 = 0;
   if (idx1 > 0) {
      idx1 = name.lastIndexOf(".",idx);
    }
   else {
      idx1 = name.lastIndexOf(".");
    }
   String cnm = null;
   String mnm = name;
   if (idx1 > 0) {
      cnm = name.substring(0,idx1);
      mnm = name.substring(idx1+1);
    }
   
   IvyLog.logD("LIMBA","Get info for class " + cnm + " and method " + mnm);
   
   String rslt = name;
   
   if (cnm != null) {
      TypeDeclaration td = findClassAst(cnm,false);
      if (td != null) {
         for (Object o1 : td.bodyDeclarations()) {
            if (o1 instanceof MethodDeclaration) {
               MethodDeclaration md = (MethodDeclaration) o1;
               if (md.getName().getIdentifier().equals(mnm)) {
                  rslt = getMethodDescription(md);
                  break;
                }
             }
          }
       }
    }
   else {
      // handle case where only method is given -- if msg server is available
    }
   
   IvyLog.logD("LIMBA","Return " + rslt);

   return rslt;
}


/********************************************************************************/
/*                                                                              */
/*      Tool to return information about a class                                */
/*                                                                              */
/********************************************************************************/

@Tool("return the set of methods of a class")
public List<String> getClassMethods(@P("name of the class") String name)
{
   List<String> rslt = new ArrayList<>();
   
   IvyLog.logD("LIMBA","Find methods for class " + name);
   
   TypeDeclaration td = findClassAst(name,false);
   if (td != null) {
      for (Object o1 : td.bodyDeclarations()) {
         if (o1 instanceof MethodDeclaration) {
            MethodDeclaration md = (MethodDeclaration) o1;
            String txt = getMethodDescription(md);
            rslt.add(txt);
          }
       }
    }
   
   IvyLog.logD("LIMBA","Result is " + rslt);
   
   return rslt;
}


/********************************************************************************/
/*                                                                              */
/*      Helper methods                                                          */
/*                                                                              */
/********************************************************************************/

private TypeDeclaration findClassAst(String name,boolean resolve)
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

private String getMethodDescription(MethodDeclaration md)
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


}       // end of class LimbaTools




/* end of LimbaTools.java */

