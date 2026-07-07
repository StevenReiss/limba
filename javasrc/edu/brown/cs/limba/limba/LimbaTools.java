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

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.json.JSONArray;
import org.json.JSONObject;
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
   limba_main.transcriptAgent("Get method information for " + name); 
   
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
   limba_main.transcriptAgent("Get source code for " + name); 
   
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
            String cnts = IvyFile.loadFile(new File(fnm));
            IvyLog.logD("LIMBA","Find source " + fnm + " " + soff + " " + eoff + " " +
                  (cnts == null ? 0 : cnts.length()));
            List<String> lines0 = getLineNumbersAndText(cnts,soff,eoff);
            lines.addAll(lines0);
          }
         IvyLog.logD("LIMBA","FOUND source for method " + name0 + " " + lines);
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
   limba_main.transcriptAgent("Get source code for line " + linenumber + " in " + name); 
   
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
            if (lines0 != null && !lines0.isBlank()) {
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
 * @param startoffset       Inclusive beginning offset (0bQbased).
 * @param endoffset         Exclusive ending offset (b  $ length of src).
 * @return                  An ArrayList<String> with "lineNumber<TAB>line".
 */
private static List<String> getLineNumbersAndText(String src,
      int startoffset, int endoffset)
{ 
   List<String> lines = new ArrayList<String>();

   // sanity checks
   if (src == null || src.isEmpty()) {
      IvyLog.logD("LIMBA","Source is empty");
      return lines;
    }
   if (startoffset < 0) startoffset = 0;
   if (endoffset > src.length()) endoffset = src.length();
   if (startoffset >= endoffset) {
      IvyLog.logD("LIMBA","Start is after end: " + startoffset + " " + endoffset);
      return lines;
    }

   int lineno = 1;                                 // humanbQreadable line count
   int pos   = 0;
   int linestart = -1;                             // position of current line start

   for (int i = pos; i < src.length(); ) {
      // detect the beginning of a new line
      // need to handle \r as EOL terminator?
      if (src.charAt(i) == '\n') {                // \n is always used as line terminator here
         if (linestart >= 0) {                    // we already have a complete line before it
            String txt = src.substring(linestart, i);
            int relPos = startoffset - linestart;
            if (relPos < txt.length() && i <= endoffset) {
               lines.add(String.format("%d\t%s", lineno, txt));
             }
          }
         ++lineno;                               // next line
         linestart = i + 1;                      // after the \n character
       }

      if (i == endoffset - 1) {
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



/********************************************************************************/
/*                                                                              */
/*      Find references to a name using the IDE                                 */
/*                                                                              */
/********************************************************************************/

@Tool("This agent will find all references to a method, field, or class. " + 
      "It returns a string " +
      "representing a JSON array of references where each reference is a JSON object " +
      "with fields for FILE, INSIDE, LINE, DEFINITION indicating whether the reference is " +
      "a definition or not, the TYPE of item (METHOD, FIELD, or TYPE), " +
      "and INSIDETYPE indicating the type of container.")
public String getReferences(@P("Name of field, method, or class") String name)
{
   long start = System.currentTimeMillis();
   
   limba_main.transcriptAgent("Find references to " + name); 
   
   JSONArray ja = getReferenceArray(name);
   
   String rslt = null;
   if (ja != null) {
      rslt = ja.toString(2);
    }
   else {
      rslt = "{ error: 'No debugid given' }";
    }
   
   long time = System.currentTimeMillis() - start;
   IvyLog.logI("DICONTROL","Time for getReferences: " + time);
   
   return rslt;
}

private JSONArray getReferenceArray(String name)
{
   JSONArray rslt = new JSONArray();
   
   Element xml1 = message_server.findClass(name,true);
   Element xml2 = null;
   String typ = null;
   if (isMatch(xml1)) {
      typ = "TYPE";
      xml2 = message_server.findClass(name,true);
    }
   else {
      xml1 = message_server.findMethod(name,true,false);
      if (isMatch(xml1)) {
         typ = "METHOD";
         xml2 = message_server.findMethod(name,false,false);
       }
      else {
         xml1 = message_server.findField(name,true);
         if (isMatch(xml1)) {
            typ = "FIELD";
            xml2 = message_server.findField(name,false);
          }
       }
    }
   
   for (Element me: IvyXml.children(xml1,"MATCH")) {
      JSONObject jo = outputMatch(me,typ,true);
      if (jo != null) rslt.put(jo);
    }
   for (Element me: IvyXml.children(xml2,"MATCH")) {
      JSONObject jo = outputMatch(me,typ,false);
      if (jo != null) rslt.put(jo);
    }
   
   return rslt;
}


private boolean isMatch(Element xml)
{
   if (xml == null) return false;
   if (IvyXml.getChild(xml,"MATCH") == null) return false;
   return true;
}


private JSONObject outputMatch(Element me,String typ,boolean def)
{
   Element mi = IvyXml.getChild(me,"ITEM");
   String intyp = IvyXml.getAttrString(mi,"TYPE");
   intyp = getReturnType(intyp);
   if (intyp == null) return null;
   String usrc = IvyXml.getAttrString(mi,"SOURCE");
   if (usrc == null || !usrc.equals("USERSOURCE")) return null;
   
   String fnm = IvyXml.getTextElement(me,"FILE");
   if (fnm == null) return null;;
   File fil = new File(fnm);
   int offset = IvyXml.getAttrInt(me,"STARTOFFSET");
   String pnm = IvyXml.getAttrString(me,"PROJECT");
   if (pnm == null) pnm = IvyXml.getAttrString(mi,"PROJECT");
   
   CompilationUnit cu = findFileUnit(fil); 
   int line = cu.getLineNumber(offset);
   String inside = IvyXml.getAttrString(mi,"QNAME");
   if (inside == null) inside = IvyXml.getAttrString(mi,"NAME");
   
   JSONObject jo = new JSONObject();
   jo.put("FILE",fnm);
   jo.put("LINE",line);
   jo.put("TYPE",typ);
   jo.put("INSIDE",inside);
   jo.put("INSIDETYPE",intyp);
   jo.put("DEFINITION",def);
   
   return jo;
}



private String getReturnType(String typ)
{
   if (typ == null) return null;
   
   String rslt = null;
   
   switch (typ) {
      case "Class" :
      case "Throwable" :
      case "Exception" :
      case "Interface" :
      case "Enum" :
         rslt = "TYPE";
         break;
      case "Function" :
      case "Method" :
      case "Constructor" :
      case "StaticInitializer" :
         rslt = "METHOD";
         break;
      case "Field" :
      case "EnumConstant" :
      case "Variable" :
         rslt = "FIELD";
         break;
    }
   
   return rslt;
}


}       // end of class LimbaTools




/* end of LimbaTools.java */

