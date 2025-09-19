/********************************************************************************/
/*                                                                              */
/*              LimbaCommandBase.java                                           */
/*                                                                              */
/*      Command implementions for LIMBA                                         */
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

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpTimeoutException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.w3c.dom.Element;

import edu.brown.cs.ivy.file.IvyLog;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;
import io.github.ollama4j.OllamaAPI;
import io.github.ollama4j.exceptions.OllamaBaseException;
import io.github.ollama4j.models.generate.OllamaStreamHandler;
import io.github.ollama4j.models.response.Model;
import io.github.ollama4j.models.response.ModelDetail;
import io.github.ollama4j.models.response.OllamaResult;
import io.github.ollama4j.utils.Options;

class LimbaCommandFactory implements LimbaConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private LimbaMain                limba_main;
      

/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

LimbaCommandFactory(LimbaMain lm) 
{
   limba_main = lm;
}


LimbaCommand createCommand(String line)
{
   StringTokenizer tok = new StringTokenizer(line);
   if (!tok.hasMoreTokens()) return null;
   String cmd = tok.nextToken();
   cmd = cmd.toUpperCase();
   String prompt = getPrompt(cmd);
   
   switch (cmd) {
      case "LIST" :
         return new CommandList(line);
      case "DETAIL" :
      case "DETAILS" :
         return new CommandDetails(line);
      case "QUERY" :
         return new CommandQuery(prompt,line);
      case "ASK" :
         return new CommandQuery(prompt,line);
      case "CLEAN" :
         return new CommandQuery(prompt,line);
      case "GENERATE" :
         return new CommandQuery(prompt,line);
      case "JAVADOC" :
         return new CommandQuery(prompt,line);
      case "SUGGEST" :
         return new CommandQuery(prompt,line);
      case "EXPLAIN" :
         return new CommandQuery(prompt,line);
      case "PROJECT" :
         break;
      case "EXIT" :
         System.exit(0);
    }
   
   return null;
}



/********************************************************************************/
/*                                                                              */
/*      Get prompt from resource file                                           */
/*                                                                              */
/********************************************************************************/

private String getPrompt(String cmd)
{
   InputStream ins = getClass().getClassLoader().getResourceAsStream("prompts.xml");
   if (ins == null) return null;
   Element xml = IvyXml.loadXmlFromStream(ins);
   if (xml == null) return null;
   String base = null;
   String ptxt = null;
   for (Element pmpt : IvyXml.children(xml,"PROMPT")) {
      String what = IvyXml.getAttrString(pmpt,"COMMAND");
      if (what == null) base = IvyXml.getText(pmpt).trim();
      else if (what.equals(cmd)) {
         ptxt = IvyXml.getText(pmpt).trim();
       }
    }
   
   if (base == null) return ptxt;
   if (ptxt == null) return base;
   return base + " " + ptxt;
}



/********************************************************************************/
/*                                                                              */
/*      Response extraction methods                                             */
/*                                                                              */
/********************************************************************************/

private String getJavaCode(String resp)
{
   if (resp == null) return null;
   int idx0 = resp.indexOf("```");
   if (idx0 < 0) return resp;
   int idx1 = resp.indexOf("\n",idx0);
   if (idx1 < 0) {
      return resp.substring(idx0+3);
    }
   String text0 = resp.substring(idx0+3,idx1).trim();
   if (!text0.isEmpty() && !text0.equals("java")) {
      idx1 = idx0 + 3;
    }
   else idx1 = idx1+1;
   
   int idx2 = resp.indexOf("```",idx1);
   if (idx2 < 0) return resp.substring(idx1);
   return resp.substring(idx1,idx2);
}


private String getJavaDoc(String resp)
{
   String jcode = getJavaCode(resp);
   if (jcode == null) return null;
   int idx0 = jcode.indexOf("/**");
   if (idx0 < 0) return null;
   int idx1 = jcode.indexOf("*/",idx0);
   if (idx1 < 0) jcode.substring(idx0);
   int idx2 = jcode.indexOf("\n",idx1);
   if (idx2 > 0) idx1 = idx2+1;
   String jdoc = jcode.substring(idx0,idx1);
   if (!jdoc.endsWith("\n")) jdoc += "\n";
   return jdoc;
}



/********************************************************************************/
/*                                                                              */
/*      Base for commands                                                       */
/*                                                                              */
/********************************************************************************/

private abstract class CommandBase implements LimbaCommand {
   
   private String end_token;
   private boolean end_on_blank;
   protected Map<String,String> option_set;
   protected String command_text;
   private String first_line;
   
   CommandBase(String line) {
      end_token  = "END";
      option_set = null;
      end_on_blank = false;
      command_text = null;
      readOptions(line);
    }

   @Override public boolean getEndOnBlank()             { return end_on_blank; }
   @Override public boolean getNeedsInput()             { return false; }
   @Override public String getEndToken()                { return end_token; } 
   
   protected OllamaAPI getOllama() {
      return limba_main.getOllama();
    }
   protected String getModel() {
      return limba_main.getModel();
    }
   protected boolean getRawFlag() {
      return limba_main.getRawFlag();
    }
   protected boolean getThinkFlag() {
      return limba_main.getThinkFlag();
    }
   protected Options getOllamaOptions() {
      return limba_main.getOllamaOptions();
    }
   
  @Override public void setOptions(String opts) {
      if (opts == null) return;
      StringTokenizer tok = new StringTokenizer(opts);
      while (tok.hasMoreTokens()) {
         String opt = tok.nextToken();
         if (option_set == null) option_set = new HashMap<>();
         int idx = opt.indexOf("=");
         String key = opt;
         String value = "";
         if (idx > 0) {
            key = opt.substring(0,idx);
            value = opt.substring(idx+1);
          }
         option_set.put(key,value);
       }
    }
   
   private  void readOptions(String line) {
      StringTokenizer tok = new StringTokenizer(line);
      if (tok.hasMoreTokens()) {
         tok.nextToken();                       // skip the command
       }
      
      boolean haveeol = false;
      while (tok.hasMoreTokens()) {
         String opt = tok.nextToken();
         if (opt.equals("--")) {
            if (!haveeol) end_on_blank = true;
            haveeol = true;
            first_line = tok.nextToken("\n");
            break;
          }
         if (opt.equals("-b")) {
            end_on_blank = true;
            continue;
          }
         if (opt.startsWith("-")) {
            haveeol = true;
            if (option_set == null) option_set = new HashMap<>();
            int idx = opt.indexOf("=");
            String key = opt;
            String value = "";
            if (idx > 0) {
               key = opt.substring(0,idx);
               value = opt.substring(idx+1);
             }
            option_set.put(key,value);
          }
         else if (!haveeol) {
            end_token = opt;
            haveeol = true;
          }
         else {
            first_line = opt + " " + tok.nextToken("\n");
          }
       }
    }
   
   @Override public void setupCommand(String complete,boolean user) {
      StringBuffer text = new StringBuffer();
      if (complete == null) complete = "";
      StringTokenizer lines = new StringTokenizer(complete,"\n");
      int ct = (user ? 0 : 1);
      while (lines.hasMoreTokens()) {
         String line = lines.nextToken();
         if (ct++ == 0) { 
            if (first_line == null) continue;
            line = first_line.trim();
          }
         text.append(line);
         text.append("\n");
       }
      command_text = text.toString();
    }
   
   @Override public void process(IvyXmlWriter rslt) { 
      boolean retry = true;
      for (int i = 0; retry && i < 10; ++i) {
         retry = false;
         try {
            localProcess(rslt);
          }
         catch (OllamaBaseException e) {
            IvyLog.logE("LIMBA","Problem processinq " + getCommandName(),e);
          }
         catch (HttpTimeoutException e) {
            IvyLog.logE("LIMBA","Timeout processing " + getCommandName(),e);
            retry = true;
          }
         catch (IOException e) {
            IvyLog.logE("LIMBA","I/O Problem processinq query",e);
          }
         catch (InterruptedException e) {
            IvyLog.logE("LIMBA","Command " + getCommandName() + " interrputed",e);
            retry = true;
          }
         catch (Throwable t) {
            IvyLog.logE("LIMBA","Problem with " + getCommandName(),t);
          }
       }
    }
   
   protected abstract void localProcess(IvyXmlWriter rslt) throws Exception;
   
}       // end of abstract class CommandBase



/********************************************************************************/
/*                                                                              */
/*      List Models command                                                     */
/*                                                                              */
/********************************************************************************/

private class CommandList extends CommandBase {
   
   CommandList(String line) { 
      super(line);
    }
   
   @Override public String getCommandName()             { return "LIST"; }
   @Override public void setupCommand(String complete,boolean user)  { }
   
   @Override public void localProcess(IvyXmlWriter xw) throws Exception {
      List<Model> models = getOllama().listModels();
      for (Model m : models) {
         if (xw != null) {
            xw.textElement("MODEL",m.getName());
          }
         else {
            System.out.println(m.getName());
          }
       }
    }
   
}       // end of inner class CommandList



/********************************************************************************/
/*                                                                              */
/*      DETAILS command                                                         */
/*                                                                              */
/********************************************************************************/

private class CommandDetails extends CommandBase {

   CommandDetails(String line) { 
      super(line);
    }
   
   @Override public String getCommandName()             { return "DETAILS"; }
   @Override public void setupCommand(String complete,boolean user)  { }
   
   @Override public void localProcess(IvyXmlWriter xw) throws Exception {
      ModelDetail md = getOllama().getModelDetails(getModel());
      if (xw != null) {
         xw.textElement("DETAILS",md.toString());
       }
      else {
         System.out.println(md);
       }
    }

}       // end of inner class CommandList



/********************************************************************************/
/*                                                                              */
/*      QUERY command                                                           */
/*                                                                              */
/********************************************************************************/

private class CommandQuery extends CommandBase {
   
   private String programmer_prompt;
   
   CommandQuery(String prompt,String line) {
      super(line);
      programmer_prompt = prompt;
    }
   
   @Override public boolean getNeedsInput()             { return true; }
   @Override public String getCommandName()             { return "QUERY"; }
   
   @Override public void localProcess(IvyXmlWriter xw) throws Exception {
      String cmd = command_text;
      if (programmer_prompt != null) {
         cmd = programmer_prompt + "\n" + command_text;
       }
      IvyLog.logD("LIMBA","Query: " + cmd);
      StreamHandler hdlr = new StreamHandler();
      OllamaResult rslt = null;
      if (getThinkFlag()) {
         rslt = getOllama().generate(getModel(),cmd,
               getRawFlag(),getOllamaOptions(),hdlr,
               new ThinkHandler());
       }
      else {
         rslt = getOllama().generate(getModel(),cmd,
            getRawFlag(),getOllamaOptions(),hdlr);
       }
      IvyLog.logD("LIMBA","Response: " + rslt.getResponse());
      IvyLog.logD("LIMBA","\n------------------------\n\n");
      String resp = rslt.getResponse();
      if (xw != null) {
         xw.field("TIME",rslt.getResponseTime());
         xw.cdataElement("RESPONSE",rslt.getResponse());
         String jcode = getJavaCode(resp);
         if (jcode != null) xw.cdataElement("JAVA",jcode);
         String jdoc = getJavaDoc(resp);
         if (jdoc != null) xw.cdataElement("JAVADOC",jdoc);
       }
      else {
         System.out.println("RESPONSE TIME: " + rslt.getResponseTime());
         System.out.println(rslt.getResponse());
         String jcode = getJavaCode(resp);
         if (jcode != null) {
            IvyLog.logD("LIMBA","JAVA CODE" + jcode);
            System.out.println("\n\nJAVA CODE:\n" + jcode + "\n\n");
          }
         String jdoc = getJavaDoc(resp);
         if (jdoc != null) {
            IvyLog.logD("LIMBA","JAVADOC" + jdoc);
            System.out.println("\n\nJAVA DOC:\n" + jdoc + "\n\n");
          }
       }
      return;
    }
   
}       // end of inner class CommandQuery



/********************************************************************************/
/*                                                                              */
/*      Stream handler for async processing                                     */
/*                                                                              */
/********************************************************************************/

private final class StreamHandler implements OllamaStreamHandler {
   
   @Override public void accept(String message) {
//    System.out.println(message);
//    IvyLog.logD("LIMBA","Received: " + message);
    }
   
}       // end of inner class StreamHandler


private final class ThinkHandler implements OllamaStreamHandler {
   
   @Override public void accept(String message) {
      System.out.print(message);
      IvyLog.logD("LIMBA","THINK: " + message);
    }

}       // end of inner class StreamHandler


}       // end of class LimbaCommandBase




/* end of LimbaCommandBase.java */

