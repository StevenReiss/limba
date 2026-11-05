/********************************************************************************/
/*                                                                              */
/*              SpecerCommand.java                                              */
/*                                                                              */
/*      Main entry point for specification operations                           */
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import dev.langchain4j.memory.ChatMemory;
import edu.brown.cs.ivy.file.IvyLog;
import edu.brown.cs.ivy.mint.MintConstants.CommandArgs;
import edu.brown.cs.ivy.xml.IvyXmlWriter;
import edu.brown.cs.limba.limba.LimbaMain;
import edu.brown.cs.limba.limba.LimbaConstants.LimbaToolSet;

public class SpecerCommand implements SpecerConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private LimbaMain       limba_main;
private String          cmd_contents;
private ChatMemory      cmd_history;
private String          limba_model;
private CommandArgs     query_context;




/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public SpecerCommand(LimbaMain lm,String cnts,ChatMemory history,
      CommandArgs context,String model)
{
   limba_main = lm;
   cmd_contents = cnts;
   cmd_history = history;
   query_context = context;
   limba_model = model;
}



/********************************************************************************/
/*                                                                              */
/*      Processing methods                                                      */
/*                                                                              */
/********************************************************************************/

public void process(IvyXmlWriter xw) throws Exception 
{
   String ask = cmd_contents;
   Set<String> priorerrs = new HashSet<>();
   for (int i = 0; i < 3; ++i) {
      String resp = limba_main.askOllamaWithFunctions(ask,
            false,cmd_history,
            EnumSet.noneOf(LimbaToolSet.class),query_context,
            limba_model);
      
      String code = getCode(resp);
      if (code == null) {
         ask = "You were supposed to provide a functional description " +
            "in terms of Java interfaces with appropriate JavaDoc.\n" +
            "Please try again.";
         continue;
       }
      
      IvyLog.logD("SPECER","Work on specification:\n " + code);
      
      SpecerChecker chk = new SpecerChecker(code);
      List<String> errs = chk.process();
      for (Iterator<String> it = errs.iterator(); it.hasNext(); ) {
         String e = it.next();
         if (!priorerrs.add(e)) {
            IvyLog.logD("SPECER","Remove duplicate error " + e);
            it.remove();
          }
       }
      
      IvyLog.logD("SPECER","Errors detected: " + errs);
      
      if (i == 1) errs.clear();
      
      if (errs.isEmpty()) {
         code = checkWithLLM(code);
         xw.begin("SPECIFICATION");
         xw.textElement("CONTENTS",code);
         xw.end("SPECIFICATION");
         return;
       }
      
      StringBuffer buf = new StringBuffer();
      buf.append("That specification is incomplete.  Please address the ");
      buf.append("following issues and regenerate it.  Remember that the ");
      buf.append("output should be Java abstract classes, interfaces, enums ");
      buf.append("and JavaDoc.  Include all the code from the previous ");
      buf.append("specification, augmented as necessary.  The problems are: ");
      for (String s : errs) {
         buf.append("\n * " + s + "\n");
       }
      ask = buf.toString();
    }
}


private String getCode(String resp)
{
   List<String> jcodes = LimbaMain.getJavaCode(resp);
   if (jcodes == null || jcodes.isEmpty()) return null;
   String prefix = getPrefix(resp);
   StringBuffer code = new StringBuffer();
   if (prefix != null) {
      code.append(prefix);
    }
   code.append("package edu.brown.limba.generated;\n\n");
   code.append("import java.util.*;\n");
   code.append("import java.io.*;\n");
   
   for (String jc : jcodes) {
      code.append("\n\n");
      int idx = jc.indexOf("{");
      boolean embed = false;
      if (idx < 0) embed = true;
      else {
         int idx1 = jc.indexOf("class");
         int idx2 = jc.indexOf("interface");
         int idx3 = jc.indexOf("enum");
         if (idx1 < 0) idx1 = idx+10;
         if (idx2 < 0) idx2 = idx+10;
         if (idx3 < 0) idx3 = idx+10;
         idx1 = Math.min(idx1,idx2);
         idx1 = Math.min(idx1,idx3);
         if (idx1 > idx) embed = true;
       }
      if (embed) {
         IvyLog.logD("SPECER","Code skipped: " + jc);
       }
      else {
         jc = jc.replace("##","//");
//       jc = jc.replaceAll("^\\*\\* ","//*");
         code.append(jc);
       }
    }
   
   return code.toString();
}


private String checkWithLLM(String code) throws Exception
{
   String ask = limba_main.getPrompt("SPEC5");
   ask += "\nPlease return the complete refined design";
   String resp = limba_main.askOllamaWithFunctions(ask,
         false,cmd_history,
         EnumSet.noneOf(LimbaToolSet.class),null,
         limba_model);
   
   String code1 = getCode(resp);
   if (code1 == null) return code;
   
   return code1;
}



private String getPrefix(String resp)
{
   StringBuffer buf = new StringBuffer();
   buf.append("/**\n");
   
   try (BufferedReader br = new BufferedReader(new StringReader(resp))) {
      boolean incode = false;
      for ( ; ; ) {
         String line = br.readLine();
         if (line == null) break;
         if (line.startsWith("```")) {
            if (incode) {
               incode = false;
               continue;
             }  
            else {
               incode = true;
               continue;
             }
          }
         else if (!incode) {
            line = line.replace("*/","@/");
//          buf.append("/// " + line + "\n");
            buf.append(" * " + line + "\n");
          }
       }
    }
   catch (IOException e) {
      IvyLog.logE("SPECER","Unexpected I/O exception",e);
    }
   
   buf.append(" */\n");
   
   return buf.toString();
}

}       // end of class SpecerFactory




/* end of SpecerFactory.java */

