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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpTimeoutException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.w3c.dom.Element;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.ivy.file.IvyLog;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;
import io.github.ollama4j.OllamaAPI;
import io.github.ollama4j.exceptions.OllamaBaseException;
import io.github.ollama4j.models.response.Model;
import io.github.ollama4j.models.response.ModelDetail;

class LimbaCommandFactory implements LimbaConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private LimbaMain                limba_main;
private Map<String,ChatMemory>   memory_map;

      

/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

LimbaCommandFactory(LimbaMain lm) 
{
   limba_main = lm;
   memory_map = new HashMap<>();
}


LimbaCommand createCommand(String line)
{
   StringTokenizer tok = new StringTokenizer(line);
   if (!tok.hasMoreTokens()) return null;
   String cmd = tok.nextToken();
   cmd = cmd.toUpperCase();
   String prompt = getPrompt(cmd);
   
   switch (cmd) {
      case "PING" :
         return new CommandPing(line);
      case "LIST" :
         return new CommandList(line);
      case "STYLE" :
         return new CommandStyle(line);
      case "CONTEXT" :
         return new CommandContext(line);
      case "SETMODEL" :
         return new CommandSetModel(line);
      case "CLEAR" :
         return new CommandClear(line);
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
         return new CommandProject(line);
      case "PROPERTY" :
         return new CommandProperty(line);
      case "FIND" :
         return new CommandFind(prompt,line);
      case "FINDJDOC" :
         return new CommandJdoc(prompt,line);
      case "TESTS" :
         return new CommandTests(prompt,line);
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
   InputStream ins = getClass().getClassLoader().getResourceAsStream("resources/prompts.xml");
   if (ins == null) {
      ins = getClass().getClassLoader().getResourceAsStream("prompts.xml");
    }
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
   
   Map<String,String> keymap = limba_main.getKeyMap(); 
   if (keymap != null) {
      base = IvyFile.expandName(base,keymap);
      ptxt = IvyFile.expandName(ptxt,keymap);
    }
   
   if (base == null) return ptxt;
   if (ptxt == null) return base;
   return base + " " + ptxt;
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
   protected String command_id;
   private String first_line;
   
   CommandBase(String line) {
      end_token  = "EOL";
      option_set = null;
      end_on_blank = false;
      command_text = null;
      command_id = null;
      readOptions(line);
    }

   @Override public boolean getEndOnBlank()             { return end_on_blank; }
   @Override public String getEndToken()                { return end_token; } 
   
   protected OllamaAPI getOllama() {
      return limba_main.getOllama();
    }
   protected String getModel() {
      return limba_main.getModel();
    }
   
  @Override public void setOptions(String opts) {
      if (opts == null || opts.isEmpty()) return;
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
   
   @Override public void setupCommand(Element xml) {
      String opts = IvyXml.getAttrString(xml,"OPTIONS");
      setOptions(opts);
      command_id = IvyXml.getAttrString(xml,"ID");
      String body = IvyXml.getTextElement(xml,"BODY");
      if (body != null) setupCommand(body,false);
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
/*      Ping command                                                            */
/*                                                                              */
/********************************************************************************/

private class CommandPing extends CommandBase {
   
   CommandPing(String line) { 
      super(line);
    }
   
   @Override public String getCommandName()             { return "PING"; }
   @Override public void setupCommand(String complete,boolean user)  { }
   
   @Override public void localProcess(IvyXmlWriter xw) throws Exception {
      xw.text("PONG");
    }
   
}       // end of inner class CommandPing



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
/*      SETMODEL command                                                        */
/*                                                                              */
/********************************************************************************/

private class CommandSetModel extends CommandBase {
   
   CommandSetModel(String line) {
      super(line);
    }
   
   @Override public String getCommandName()             { return "SETMODEL"; }
   @Override public String getEndToken()                { return null; } 
   
   @Override public void localProcess(IvyXmlWriter xw) throws Exception {
      limba_main.setModel(command_text.trim());
    }
   
}       // end of inner class CommandSetModel




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

}       // end of inner class CommandDetails


/********************************************************************************/
/*                                                                              */
/*      Context and style commands                                              */
/*                                                                              */
/********************************************************************************/

private class CommandStyle extends CommandBase {
   
   CommandStyle(String line) { 
      super(line);
    }
   
   @Override public String getCommandName()             { return "STYLE"; }
   
   @Override public void localProcess(IvyXmlWriter xw) throws Exception {
      limba_main.setUserStyle(command_text);
    }
   
}       // end of inner class CommandStyle



private class CommandContext extends CommandBase {
   
   CommandContext(String line) { 
      super(line);
    }
   
   @Override public String getCommandName()             { return "CONTEXT"; }
   
   @Override public void localProcess(IvyXmlWriter xw) throws Exception {
      limba_main.setUserContext(command_text); 
    }
   
}       // end of inner class CommandContext



private class CommandClear extends CommandBase {
   
   CommandClear(String line) {
      super(line);
    }
   
   @Override public String getCommandName()             { return "CLEAR"; }
   
   @Override public void localProcess(IvyXmlWriter xw) throws Exception {
      if (command_id != null) {
        memory_map.remove(command_id);
       }
    }
   
}       // end of inner class CommandClear



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
   
   @Override public String getCommandName()             { return "QUERY"; }
   
   @Override public void localProcess(IvyXmlWriter xw) throws Exception {
      String cmd = command_text;
      boolean usectx = false;
      if (programmer_prompt != null) {
         cmd = programmer_prompt + "\n" + command_text;
         usectx = true;
       }
      ChatMemory history = null;
      if (command_id != null) {
         history = memory_map.get(command_id);
         if (history == null) {
            history = MessageWindowChatMemory.builder()
               .maxMessages(10)
               .build();
          }
       } 
       
      String resp = limba_main.askOllama(cmd,usectx,history);  
      if (xw != null) {
         xw.cdataElement("RESPONSE",resp);
         List<String> jcodes = LimbaMain.getJavaCode(resp);
         if (jcodes != null) {
            for (String jcode : jcodes) {
               xw.cdataElement("JAVA",jcode);
             }
          }
         String jdoc = LimbaMain.getJavaDoc(resp);
         if (jdoc != null) xw.cdataElement("JAVADOC",jdoc);
       }
      else {
         System.out.println(resp);
         List<String> jcodes = LimbaMain.getJavaCode(resp);
         if (jcodes != null) {
            for (String jcode : jcodes) {
               IvyLog.logD("LIMBA","JAVA CODE:\n" + jcode);
   //          System.out.println("\n\nJAVA CODE:\n" + jcode + "\n\n");
             }
          }
         String jdoc = LimbaMain.getJavaDoc(resp);
         if (jdoc != null) {
            IvyLog.logD("LIMBA","JAVADOC:\n" + jdoc);
   //       System.out.println("\n\nJAVA DOC:\n" + jdoc + "\n\n");
          }
       }
      return;
    }
   
}       // end of inner class CommandQuery


/********************************************************************************/
/*                                                                              */
/*      Local commands                                                          */
/*                                                                              */
/********************************************************************************/

private abstract class LocalCommand extends CommandBase {
   
   LocalCommand(String line) {
      super(line);
    }
   
}       // end of inner class LocalCommand



private class CommandProperty extends LocalCommand {

   CommandProperty(String line) {
      super(line);
    }
   
   @Override public String getCommandName()             { return "PROPERTY"; }
   @Override public String getEndToken()                { return null; }
   
   @Override public void localProcess(IvyXmlWriter xw) {
      // PROPERTY x=y or PROPERTY x y
    }
   
}       // end of inner class CommandProperty



private class CommandProject extends LocalCommand {
   
   CommandProject(String line) {
      super(line);
    }
   
   @Override public String getCommandName()             { return "PROPERTY"; }
   @Override public String getEndToken()                { return null; }
   
   @Override public void localProcess(IvyXmlWriter xw) {
      String fnm = null;
      if (command_text == null) command_text = "";
      fnm = command_text.trim();
      String [] args = fnm.split("\\s");
      if (args.length > 1) {
         limba_main.setWorkspace(args[1]);
         fnm = args[0];
       }
      IvyLog.logD("LIMBA","Load project from " + fnm);
      if (fnm.equals("QUERY")) {
         limba_main.setupRag(); 
       }
      else {
         File f = new File(fnm);
         if (limba_main.getWorkspace() == null) {
            limba_main.setWorkspace(f.getName());
          }
         if (f.exists()) limba_main.setupRag(f);
       }
    }
   
}       // end of inner class CommandProject



/********************************************************************************/
/*                                                                              */
/*      FIND (generate and test) commands                                       */
/*                                                                              */
/********************************************************************************/

private class CommandFind extends CommandBase {

   private String use_prompt;
   private LimbaFinder limba_finder;
   
   CommandFind(String prompt,String line) {
      super(line);
      use_prompt = prompt;
    }
   
   @Override public String getCommandName()             { return "FIND"; }
   
   @Override public void setupCommand(Element xml) {
      super.setupCommand(xml);          // handle options
      limba_finder = new LimbaFinder(limba_main,use_prompt,xml); 
    }
   
   @Override public void localProcess(IvyXmlWriter xw) throws Exception {
      limba_finder.process(xw);   
    }
   
}       // end of inner class CommandFind



/********************************************************************************/
/*                                                                              */
/*      FINDJDOC command to find JavaDoc                                        */
/*                                                                              */
/********************************************************************************/

private class CommandJdoc extends CommandBase {

   private String use_prompt;
   private LimbaJdocer limba_jdocer;
   
   CommandJdoc(String prompt,String line) {
      super(line);
      use_prompt = prompt;
    }
   
   @Override public String getCommandName()             { return "FINDJDOC"; }
   
   @Override public void setupCommand(Element xml) {
      super.setupCommand(xml);          // handle options
      limba_jdocer = new LimbaJdocer(limba_main,use_prompt,xml);  
    }
   
   @Override public void localProcess(IvyXmlWriter xw) throws Exception {
      limba_jdocer.process(xw);   
    }
   
}       // end of inner class CommandJdoc



/********************************************************************************/
/*                                                                              */
/*      TESTS command to find test cases                                        */
/*                                                                              */
/********************************************************************************/

private class CommandTests extends CommandBase {
   
   private String use_prompt;
   private LimbaTestGenerator test_gen;
   
   CommandTests(String prompt,String line) {
      super(line);
      use_prompt = prompt;
    }
   
   @Override public String getCommandName()             { return "TESTS"; }
   
   @Override public void setupCommand(Element xml) {
      super.setupCommand(xml);          // handle options
      test_gen = new LimbaTestGenerator(limba_main,use_prompt,xml);  
    }
   
   @Override public void localProcess(IvyXmlWriter xw) throws Exception {
      // NEED TO CREATE TEST_GEN for non-xml case
      if (test_gen != null) {
         test_gen.process(xw);  
       }
    }
   
}       // end of inner class CommandJdoc


}       // end of class LimbaCommandBase




/* end of LimbaCommandBase.java */

