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
import edu.brown.cs.ivy.file.IvyFormat;
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
private LimbaMonitor    message_server;
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
public String getMethodInformation(@P("full name of the method") String name0)
{
   String name = normalizeMethodName(name0);
   
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
   
   if (rslt == null) rslt = "*ERROR*";
   
   IvyLog.logD("LIMBA","Return " + rslt);

   return rslt;
}


@Tool("return the signature and javadoc describing a method.  Alias for " +
   "getMethodInformation")
public String getMethodSignature(@P("full name of the method") String name)
{
  return getMethodInformation(name);
} 

/********************************************************************************/
/*                                                                              */
/*      Tool to return information about a class                                */
/*                                                                              */
/********************************************************************************/

@Tool("return the set of methods of a class.  Will return an empty list if " +
      "the class does not exist.")
public List<String> getClassMethods(@P("name of the class") String name)
{
   List<String> rslt = new ArrayList<>();
   
   IvyLog.logD("LIMBA","Find methods for class " + name);
   IvyLog.logD("LIMBA","Thread " +  Thread.currentThread().threadId() + " " +
         Thread.currentThread().getName());
   
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
/*      Return the code of a method with line numbers                           */
/*                                                                              */
/********************************************************************************/

@Tool("Return the source code for a method with line numbers. Each source line " +
      "is prefixed by its line number and a tab.  This only works for user code, " +
      "not for system code.  The full method name should be provided as the " +
      "parameter. It will return an empty list if the method or class can't be found.")
public List<String> getSourceCode(
      @P("full name of the method") String name0)
{
   String name = normalizeMethodName(name0);
   
   IvyLog.logD("LIMBA","GET SOURCE CODE with line numbers for " + name);
   
   List<String> lines = new ArrayList<>();
   if (message_server != null && name != null) {
      try {
         Element xml = message_server.findMethod(name); 
         for (Element xml1 : IvyXml.children(xml,"MATCH")) {
            Element xml2 = IvyXml.getChild(xml1,"ITEM");
            if (xml2 == null) xml2 = xml1;
            int soff = IvyXml.getAttrInt(xml2,"STARTOFFSET");
            int eoff = IvyXml.getAttrInt(xml2,"ENDOFFSET");  
            String fnm = IvyXml.getAttrString(xml2,"PATH");
            if (fnm == null) fnm = IvyXml.getAttrString(xml1,"FILE");
            String cnds = IvyFile.loadFile(new File(fnm));
            List<String> lines0 = getLineNumbersAndText(cnds,soff,eoff);
            lines.addAll(lines0);
          }
         IvyLog.logD("LIMBA","FOUND source for method " + name0 + " " + lines);
         return lines;
       }
      catch (Throwable t) {
         IvyLog.logE("LIMBA","Problem getting source lines",t);
       }
    }
   
   return lines;
}


@Tool("Alias for getSourceCode. " +
      "Return the source code for a method with line numbers. Each source line " +
      "is prefixed by its line number and a tab.  This only works for user code, " +
      "not for system code.  The full method name should be provided as the " +
      "parameter. It will return an empty list if the method can't be found.")
public List<String> getSourceLines(
            @P("full name of the method") String name)
{
   return getSourceCode(name);
}
      

@Tool("Return the source code for a method with line numbers. Each source line " +
      "is prefixed by its line number and a tab.  This only works for user code, " +
      "not for system code.  The full method name should be provided as the " +
      "parameter. It will return an empty list if the method can't be found.  "+
      "this is an alias for getSourceCode")
public List<String> getMethodSource(
            @P("full name of the method") String name)
{
   return getSourceCode(name);
}




@Tool("Return the source code for a given line in a method.  The parameters are " +
"the full method name and the line number.  The tool returns the given line as a string.")
public String getSourceLine(
      @P("full name of the method") String name0,
      @P("line number") int linenumber)
{
   String name = normalizeMethodName(name0);
   
   IvyLog.logD("LIMBA","Get source line for " + name + " " + linenumber);
   
   if (message_server != null && name != null) {
      try {
         Element xml = message_server.findMethod(name); 
         for (Element xml1 : IvyXml.children(xml,"MATCH")) {
            Element xml2 = IvyXml.getChild(xml1,"ITEM");
            if (xml2 == null) xml2 = xml1;
            int soff = IvyXml.getAttrInt(xml2,"STARTOFFSET");
            int eoff = IvyXml.getAttrInt(xml2,"ENDOFFSET");  
            String fnm = IvyXml.getAttrString(xml2,"PATH");
            if (fnm == null) fnm = IvyXml.getAttrString(xml1,"FILE");
            String cnds = IvyFile.loadFile(new File(fnm));
            String lines0 = getLineText(cnds,soff,eoff,linenumber);
            if (lines0 != null && !lines0.isEmpty()) {
               IvyLog.logD("LIMBA","Result: " + lines0);
               return lines0;
             }
          }
         return "";
       }
      catch (Throwable t) {
         IvyLog.logE("LIMBA","Problem getting source lines",t);
       }
    }
   
   return "// NO SUCH LINE OR EMPTY LINE";
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


/********************************************************************************/
/*                                                                              */
/*      Get source lines with line numbers                                      */
/*                                                                              */
/********************************************************************************/

/**
 * Returns the line numbers together with their text for a range of character
 * offsets in a source file.
 *
 * @param src		    The complete source code (one string).
 * @param startOffset	    Inclusive beginning offset (0bQbased).
 * @param endOffset	    Exclusive ending offset (b	$ length of src).
 * @return		    An ArrayList<String> with "lineNumber<TAB>line".
 */
private static ArrayList<String> getLineNumbersAndText(String src,
      int startOffset, int endOffset) {
   
   // sanity checks
   if (src == null || src.isEmpty()) return new ArrayList<>();
   if (startOffset < 0) startOffset = 0;
   if (endOffset > src.length()) endOffset = src.length();
   if (startOffset >= endOffset) return new ArrayList<>();
   
   var lines = new ArrayList<String>();
   int lineNo = 1;				   // humanbQreadable line count
   int pos   = 0;
   
   while (pos < src.length() && pos <= startOffset) {
      // skip leading whitespace before the requested range starts
      if (!Character.isWhitespace(src.charAt(pos))) break;
      if (src.charAt(pos++) == '\n') ++lineNo;     // still in prebQrange area
    }
   
   int lineStart = -1;				   // position of current line start
   
   for (int i = pos; i < src.length(); ) {
      // detect the beginning of a new line
      // need to handle \r as EOL terminator?
      if (src.charAt(i) == '\n') {                // \n is always used as line terminator here
	 if (lineStart >= 0) {			  // we already have a complete line before it
	    String txt = src.substring(lineStart, i);
	    int relPos = startOffset - lineStart;
	    if (relPos < txt.length() && i <= endOffset) {
	       lines.add(String.format("%d\t%s", lineNo, txt));
	     }
	  }
	 ++lineNo;				 // next line
	 lineStart = i + 1;			 // after the \n character
       }
      
      if (i == endOffset - 1) {
	 // last requested line bS capture it even if it does not end with '\n'
	 String txt = src.substring(lineStart, i + 1);
	 lines.add(String.format("%d\t%s", lineNo, txt));
       }
      
      ++i;
    }
   return lines;
}


private static String getLineText(String src,
      int startOffset, int endOffset,int lno) {
   
   // sanity checks
   if (src == null || src.isEmpty()) return "";
   if (startOffset < 0) startOffset = 0;
   if (endOffset > src.length()) endOffset = src.length();
   if (startOffset >= endOffset) return "";
   
   int lineno = 1;				   // humanbQreadable line count
   int pos   = 0;
   
   while (pos < src.length() && pos <= startOffset) {
      // skip leading whitespace before the requested range starts
      if (!Character.isWhitespace(src.charAt(pos))) break;
      if (src.charAt(pos++) == '\n') ++lineno;     // still in prebQrange area
    }
   
   int lineStart = -1;				   // position of current line start
   
   for (int i = pos; i < src.length(); ) {
      // detect the beginning of a new line
      // need to handle \r as EOL terminator?
      if (src.charAt(i) == '\n') {                // \n is always used as line terminator here
	 if (lineStart >= 0) {		
            if (lineno == lno) {
               String txt = src.substring(lineStart, i);
               return txt;
             }
	  }
	 ++lineno;				 // next line
	 lineStart = i + 1;			 // after the \n character
       }
      
      if (i == endOffset - 1 || i == src.length() - 1) {
	 // last requested line bS capture it even if it does not end with '\n'
         if (lineno == lno) {
            String txt = src.substring(lineStart, i + 1);
            return txt;
          }
       }
      
      ++i;
    }
   
   return "";
}


private String normalizeMethodName(String name0)
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
   int idx2 = name.lastIndexOf(".");
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



}       // end of class LimbaTools




/* end of LimbaTools.java */

