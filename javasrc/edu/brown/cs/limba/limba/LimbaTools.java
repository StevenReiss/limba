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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.w3c.dom.Element;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.ivy.file.IvyLog;
import edu.brown.cs.ivy.xml.IvyXml;

public class LimbaTools extends LimbaToolBase
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

LimbaTools(LimbaMain lm,Collection<File> files)
{
   super(lm,files); 
}


/********************************************************************************/
/*                                                                              */
/*      Tool to return information about a method                               */
/*                                                                              */
/********************************************************************************/

@Tool("This agent returns the signature and javadoc describing a method")
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
   limba_main.transcriptNote("Get method information for " + name); 
   
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


@Tool("This agent returns the signature and javadoc describing a method.  Alias for " +
   "getMethodInformation")
public String getMethodSignature(@P("full name of the method") String name)
{
  return getMethodInformation(name);
}

/********************************************************************************/
/*                                                                              */
/*      Return the code of a method with line numbers                           */
/*                                                                              */
/********************************************************************************/

@Tool("This agent returns the source code for a method with line numbers. Each source line " +
      "is prefixed by its line number and a tab.  This only works for user code, " +
      "not for system code.  The full method name should be provided as the " +
      "parameter. It will return an empty list if the method or class can't be found " +
      "or if the name is null.")
public List<String> getSourceCode(
      @P("full name of the method") String name0)
{
   long start = System.currentTimeMillis();
   String name = normalizeMethodName(name0);

   IvyLog.logD("LIMBA","GET SOURCE CODE with line numbers for " + name);
   limba_main.transcriptNote("Get source code for " + name); 
   
   List<String> lines = new ArrayList<>(); 
   if (message_server != null && name != null) {
      try {
         Element xml = getMethodMatches(name);
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

   long time = System.currentTimeMillis() - start;
   IvyLog.logI("LIMBA","Time for get source code: " + time);

   return lines;
}


@Tool("Alias for getSourceCode. " +
      "This agent returns the source code for a method with line numbers. Each source line " +
      "is prefixed by its line number and a tab.  This only works for user code, " +
      "not for system code.  The full method name should be provided as the " +
      "parameter. It will return an empty list if the method can't be found.")
public List<String> getSourceLines(
            @P("full name of the method") String name)
{
   return getSourceCode(name);
}


@Tool("Alias for getSourceCode. " +
      "This agent returns the source code for a method with line numbers. Each source line " +
      "is prefixed by its line number and a tab.  This only works for user code, " +
      "not for system code.  The full method name should be provided as the " +
      "parameter. It will return an empty list if the method can't be found.")
public List<String> getSourceCodeForMethod(
      @P("full name of the method") String name)
{
   return getSourceCode(name);
}


@Tool("This agent returns the source code for a method with line numbers. Each source line " +
      "is prefixed by its line number and a tab.  This only works for user code, " +
      "not for system code.  The full method name should be provided as the " +
      "parameter. It will return an empty list if the method can't be found.  "+
      "this is an alias for getSourceCode")
public List<String> getMethodSource(
            @P("full name of the method") String name)
{
   return getSourceCode(name);
}




@Tool("This agent returns the source code for a given line in a method.  The parameters are " +
"the full method name and the line number.  The tool returns the given line as a string.")
public String getSourceLine(
      @P("full name of the method") String name0,
      @P("line number") int linenumber)
{
   long start = System.currentTimeMillis();
   String name = normalizeMethodName(name0);

   IvyLog.logD("LIMBA","GET SOURCE LINE for " + name + " " + linenumber);
   limba_main.transcriptNote("Get source code for line " + linenumber + " in " + name); 
   
   if (message_server != null && name != null) {
      try {
         Element xml = getMethodMatches(name);
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
               long time = System.currentTimeMillis() - start;
               IvyLog.logI("LIMBA","Time for get source code: " + time);
               return lines0;
             }
          }
         
         return "// NO SUCH LINE OR EMPTY LINE";
       }
      catch (Throwable t) {
         IvyLog.logE("LIMBA","Problem getting source lines",t);
       }
    }

   return "// NO SUCH LINE OR EMPTY LINE";
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
 * @param src               The complete source code (one string).
 * @param startOffset       Inclusive beginning offset (0bQbased).
 * @param endOffset         Exclusive ending offset (b  $ length of src).
 * @return                  An ArrayList<String> with "lineNumber<TAB>line".
 */
private static List<String> getLineNumbersAndText(String src,
      int startOffset, int endOffset)
{ 
   List<String> lines = new ArrayList<String>();

   // sanity checks
   if (src == null || src.isEmpty()) return lines;
   if (startOffset < 0) startOffset = 0;
   if (endOffset > src.length()) endOffset = src.length();
   if (startOffset >= endOffset) return lines;

   int lineno = 1;                                 // humanbQreadable line count
   int pos   = 0;
   int linestart = -1;                             // position of current line start

   for (int i = pos; i < src.length(); ) {
      // detect the beginning of a new line
      // need to handle \r as EOL terminator?
      if (src.charAt(i) == '\n') {                // \n is always used as line terminator here
         if (linestart >= 0) {                    // we already have a complete line before it
            String txt = src.substring(linestart, i);
            int relPos = startOffset - linestart;
            if (relPos < txt.length() && i <= endOffset) {
               lines.add(String.format("%d\t%s", lineno, txt));
             }
          }
         ++lineno;                               // next line
         linestart = i + 1;                      // after the \n character
       }

      if (i == endOffset - 1) {
         // last requested line  capture it even if it does not end with '\n'
         String txt = src.substring(linestart, i + 1);
         lines.add(String.format("%d\t%s", lineno, txt));
         linestart = -1;
         break;
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

   int lineno = 1;                                 // human-readable line count
   int pos   = 0;
   int lineStart = -1;                             // position of current line start

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
         ++lineno;                               // next line
         lineStart = i + 1;                      // after the \n character
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


}       // end of class LimbaTools




/* end of LimbaTools.java */

