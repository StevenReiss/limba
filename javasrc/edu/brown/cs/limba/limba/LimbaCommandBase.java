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
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.ivy.file.IvyLog;
import edu.brown.cs.ivy.mint.MintConstants.CommandArgs;
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


LimbaCommand createCommand(Element xml)
{
   String cmd = IvyXml.getAttrString(xml,"DO");
   cmd = cmd.toUpperCase();
   
   String prompt = getPrompt(cmd);
   Element pelt = IvyXml.getChild(xml,"PROMPT");
   if (pelt != null) {
      String p1 = IvyXml.getText(pelt);
      if (IvyXml.getAttrBool(pelt,"REPLACE")) {
         prompt = p1;
       }
      else if (p1 != null) {
         prompt += "\n" + p1.trim();
       }
    }
   
   switch (cmd) {
      case "PING" :
         return new CommandPing(xml);
      case "LIST" :
         return new CommandList(xml);
      case "STYLE" :
         return new CommandStyle(xml);
      case "CONTEXT" :
         return new CommandContext(xml);
      case "SETMODEL" :
         return new CommandSetModel(xml);
      case "CLEAR" :
         return new CommandClear(xml);
      case "DETAIL" :
      case "DETAILS" :
         return new CommandDetails(xml);
      case "QUERY" :
         return new CommandQuery("QUERY",prompt,xml);
      case "ASK" :
         return new CommandQuery("ASK",prompt,xml);
      case "CLEAN" :
         return new CommandQuery("CLEAN",prompt,xml);
      case "GENERATE" :
         return new CommandQuery("GENERATE",prompt,xml);
      case "JAVADOC" :
         return new CommandQuery("JAVADOC",prompt,xml);
      case "SUGGEST" :
         return new CommandQuery("SUGGEST",prompt,xml);
      case "EXPLAIN" :
         return new CommandQuery("EXPLAIN",prompt,xml);
      case "PROJECT" :
         return new CommandProject(xml);
      case "FIND" :
         return new CommandFind(prompt,xml);
      case "FINDJDOC" :
         return new CommandJdoc(prompt,xml);
      case "TESTS" :
         return new CommandTests(prompt,xml);
      case "SETUPBUBBLES" :
         return new CommandSetupBubbles(xml);
      case "DEBUGREMOVE" :
         return new CommandDebugRemove(xml);
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
   
   protected String command_id;
   
   CommandBase(Element xml) {
      command_id = IvyXml.getAttrString(xml,"ID");
    }

   protected OllamaAPI getOllama() {
      return limba_main.getOllama();
    }
   protected String getModel() {
      return limba_main.getModel();
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
   
   CommandPing(Element xml) { 
      super(xml);
    }
   
   @Override public String getCommandName()             { return "PING"; }
   
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
   
   CommandList(Element xml) {
      super(xml);
    }
   
   @Override public String getCommandName()             { return "LIST"; }
   
   @Override public void localProcess(IvyXmlWriter xw) throws Exception {
      List<Model> models = getOllama().listModels();
      for (Model m : models) {
         xw.textElement("MODEL",m.getName());
       }
    }

}       // end of inner class CommandList



/********************************************************************************/
/*                                                                              */
/*      SETMODEL command                                                        */
/*                                                                              */
/********************************************************************************/

private class CommandSetModel extends CommandBase {
   
   private String model_name;
   
   CommandSetModel(Element xml) {
      super(xml);
      model_name = IvyXml.getTextElement(xml,"MODEL");
      if (model_name != null) model_name = model_name.trim();
    }
   
   @Override public String getCommandName()             { return "SETMODEL"; }
   
   @Override public void localProcess(IvyXmlWriter xw) throws Exception {
      if (model_name != null) limba_main.setModel(model_name);
    }
   
}       // end of inner class CommandSetModel



/********************************************************************************/
/*                                                                              */
/*      DETAILS command                                                         */
/*                                                                              */
/********************************************************************************/

private class CommandDetails extends CommandBase {

   CommandDetails(Element xml) { 
      super(xml);
    }
   
   @Override public String getCommandName()             { return "DETAILS"; }
   
   @Override public void localProcess(IvyXmlWriter xw) throws Exception {
      ModelDetail md = getOllama().getModelDetails(getModel());
      if (xw != null) {
         xw.textElement("DETAILS",md.toString());
       }
    }

}       // end of inner class CommandDetails


/********************************************************************************/
/*                                                                              */
/*      Context and style commands                                              */
/*                                                                              */
/********************************************************************************/

private class CommandStyle extends CommandBase {
   
   private String style_text;
   
   CommandStyle(Element xml) { 
      super(xml);
      style_text = IvyXml.getTextElement(xml,"STYLE");
    }
   
   @Override public String getCommandName()             { return "STYLE"; }
   
   @Override public void localProcess(IvyXmlWriter xw) throws Exception {
      limba_main.setUserStyle(style_text);
    }
   
}       // end of inner class CommandStyle



private class CommandContext extends CommandBase {
   
   private String context_text;
   
   CommandContext(Element xml) { 
      super(xml);
      context_text = IvyXml.getTextElement(xml,"CONTEXT");
    }
   
   @Override public String getCommandName()             { return "CONTEXT"; }
   
   @Override public void localProcess(IvyXmlWriter xw) throws Exception {
      limba_main.setUserContext(context_text); 
    }
   
}       // end of inner class CommandContext



private class CommandClear extends CommandBase {
   
   CommandClear(Element xml) {
      super(xml);
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

private final class CommandQuery extends CommandBase {
   
   private String programmer_prompt;
   private String command_name;
   private String query_text;
   private EnumSet<LimbaToolSet> tool_set;
   private CommandArgs query_context;
   
   CommandQuery(String nm,String prompt,Element xml) {
      super(xml);
      command_name = nm;
      programmer_prompt = prompt;
      query_text = IvyXml.getTextElement(xml,"CONTENTS");
      query_context = null; 
      tool_set = IvyXml.getAttrEnumSet(xml,"TOOLS",
            LimbaToolSet.class,LimbaToolSet.PROJECT);
      for (Element ctxelt : IvyXml.children(xml,"CONTEXT")) {
         String k = IvyXml.getAttrString(ctxelt,"KEY");
         String v = IvyXml.getAttrString(ctxelt,"VALUE");
         if (v == null) v = IvyXml.getText(ctxelt);
         if (k != null && v != null) {
            if (query_context == null) {
               query_context = new CommandArgs(k,v);
             }
            else {
               query_context.put(k,v);
             }
          }
       }
      
      IvyLog.logD("LIMBA","Query " + nm + " " + tool_set + " " +
            query_context);
    }
   
   @Override public String getCommandName()             { return command_name; }
   
   @Override public void localProcess(IvyXmlWriter xw) throws Exception {
      String cmd = query_text;
      boolean usectx = false;
      if (programmer_prompt != null) {
         cmd = programmer_prompt + "\n" + query_text;
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
       
      String resp = limba_main.askOllama(cmd,usectx,
            history,tool_set,query_context);   
      
      xw.cdataElement("RESPONSE",resp);
      List<String> jcodes = LimbaMain.getJavaCode(resp);
      if (jcodes != null) {
         for (String jcode : jcodes) {
            xw.cdataElement("JAVA",jcode);
          }
       }
      String jdoc = LimbaMain.getJavaDoc(resp);
      if (jdoc != null) xw.cdataElement("JAVADOC",jdoc);
      
      return;
    }
   
}       // end of inner class CommandQuery



/********************************************************************************/
/*                                                                              */
/*      Project command                                                         */
/*                                                                              */
/********************************************************************************/

private class CommandProject extends CommandBase {
   
   CommandProject(Element xml) {
      super(xml);
    }
   
   @Override public String getCommandName()             { return "PROJECT"; }
   
   @Override public void localProcess(IvyXmlWriter xw) {
      limba_main.setupRag();
    }
   
}       // end of inner class CommandProject



/********************************************************************************/
/*                                                                              */
/*      Setup BEDROCK for debugging purposes                                    */
/*                                                                              */
/********************************************************************************/

private class CommandSetupBubbles extends CommandBase {
   
   private String workspace_name;
   private String mint_name;
   
   CommandSetupBubbles(Element xml) {
      super(xml);
      workspace_name = IvyXml.getTextElement(xml,"WORKSPACE");
      mint_name = IvyXml.getAttrString(xml,"MINT");
    }
   
   @Override public String getCommandName()             { return "SETUPBUBBLES"; }
  
   @Override public void localProcess(IvyXmlWriter xw) {
      limba_main.setupBedrock(workspace_name,mint_name); 
    }
   
}       // end of inner class CommandSetupBubbles



/********************************************************************************/
/*                                                                              */
/*      FIND (generate and test) commands                                       */
/*                                                                              */
/********************************************************************************/

private class CommandFind extends CommandBase {

   private String use_prompt;
   private LimbaFinder limba_finder;
   
   CommandFind(String prompt,Element xml) {
      super(xml);
      use_prompt = prompt;
      limba_finder = new LimbaFinder(limba_main,use_prompt,xml); 
    }
   
   @Override public String getCommandName()             { return "FIND"; }
   
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
   
   CommandJdoc(String prompt,Element xml) {
      super(xml);
      use_prompt = prompt;
      limba_jdocer = new LimbaJdocer(limba_main,use_prompt,xml);  
    }
   
   @Override public String getCommandName()             { return "FINDJDOC"; }
   
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
   
   CommandTests(String prompt,Element xml) {
      super(xml);
      use_prompt = prompt;
      test_gen = new LimbaTestGenerator(limba_main,use_prompt,xml);  
    }
   
   @Override public String getCommandName()             { return "TESTS"; }
   
   @Override public void localProcess(IvyXmlWriter xw) throws Exception {
      test_gen.process(xw);  
    }
   
}       // end of inner class CommandTests



/********************************************************************************/
/*                                                                              */
/*      DEBUG REMOVE command to clean up unneeded contexts                      */
/*                                                                              */
/********************************************************************************/

private class CommandDebugRemove extends CommandBase {
 
   private String debug_id;
   
   CommandDebugRemove(Element xml) {
      super(xml);
      debug_id = IvyXml.getAttrString(xml,"DEBUGID");
    }
   
   @Override public String getCommandName()             { return "DEBUGREMOVE"; }
   
   @Override public void localProcess(IvyXmlWriter xw) { 
      limba_main.removeDebugContext(debug_id);
    }

}




}       // end of class LimbaCommandBase




/* end of LimbaCommandBase.java */

