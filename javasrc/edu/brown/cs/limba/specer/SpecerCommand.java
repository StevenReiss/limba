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
import java.util.List;


import dev.langchain4j.memory.ChatMemory;
import edu.brown.cs.ivy.file.IvyLog;
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




/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

public SpecerCommand(LimbaMain lm,String cnts,ChatMemory history,String model)
{
   limba_main = lm;
   cmd_contents = cnts;
   cmd_history = history;
   limba_model = model;
}



/********************************************************************************/
/*                                                                              */
/*      Processing methods                                                      */
/*                                                                              */
/********************************************************************************/

public void process(IvyXmlWriter xw) throws Exception 
{
   String resp = limba_main.askOllamaWithFunctions(cmd_contents,
         false,cmd_history,
         EnumSet.noneOf(LimbaToolSet.class),null,
         limba_model);
   
   List<String> jcodes = LimbaMain.getJavaCode(resp);
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
      code.append(jc);
    }
   
   IvyLog.logD("SPECER","Work on specification:\n " + code);
   
   SpecerChecker chk = new SpecerChecker(code.toString());
   List<String> errs = chk.process();
   
   if (errs.isEmpty()) {
      xw.begin("SPECIFICATION");
      xw.textElement("CONTENTS",code);
      xw.end("SPECIFICATION");
    }
   
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

