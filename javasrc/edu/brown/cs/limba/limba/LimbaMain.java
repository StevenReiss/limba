/********************************************************************************/
/*                                                                              */
/*              LimbaMain.java                                                  */
/*                                                                              */
/*      Language Intelligence Model as a Bubbles Assistant main program         */
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

import io.github.ollama4j.OllamaAPI;
import io.github.ollama4j.models.generate.OllamaStreamHandler;
import io.github.ollama4j.models.response.OllamaResult;
import io.github.ollama4j.utils.Options;
import io.github.ollama4j.utils.OptionsBuilder;
import edu.brown.cs.ivy.file.IvyLog;
import edu.brown.cs.ivy.jcomp.JcompControl;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlReader;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

public final class LimbaMain implements LimbaConstants
{


/********************************************************************************/
/*                                                                              */
/*      Main program                                                            */
/*                                                                              */
/********************************************************************************/

public static void main(String [] args)
{
   LimbaMain lm = new LimbaMain(args);
   lm.process();
}


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private String mint_id;
private String ollama_host;
private int ollama_port;
private String ollama_model;
private boolean interactive_mode;
private boolean server_mode;
private File project_file;
private File input_file;
private OllamaAPI ollama_api;
private boolean raw_flag;
private boolean think_flag;
private Options generate_options;
private LimbaCommandFactory command_factory;
private LimbaRag rag_model;
private Map<String,String> key_map;
private boolean remote_files;
private File log_file;
private IvyLog.LogLevel log_level;
private boolean log_stderr;
private JcompControl jcomp_main;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

private LimbaMain(String [] args) 
{
   mint_id = null;
   ollama_host = "localhost";
   ollama_port = 11434;
   interactive_mode = false;
   server_mode = false;
   ollama_model = "codellama:latest";
   project_file = null;
   input_file = null;
   raw_flag = false;
   think_flag = false;
   rag_model = null;
   generate_options = new OptionsBuilder().build();
   command_factory = null;
   key_map = new HashMap<>();
   key_map.put("LANGUAGE","java");
   remote_files = false;
   log_level = IvyLog.LogLevel.DEBUG;
   String home = System.getProperty("user.home");
   File f1 = new File(home);
   log_file = new File(f1,"limba.log");
   log_stderr = false;
   jcomp_main = new JcompControl();
   
   scanArgs(args);
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

String getModel()                       { return ollama_model; }

OllamaAPI getOllama()                   { return ollama_api; }

boolean getRawFlag()                    { return raw_flag; }

boolean getThinkFlag()                  { return think_flag; }

Options getOllamaOptions()              { return generate_options; }

LimbaRag getRagModel()                  { return rag_model; }

String getUrl()
{
   return "http://" + ollama_host + ":" + ollama_port;
}

LimbaCommand createCommand(String line)
{
   return command_factory.createCommand(line);
}

Map<String,String> getKeyMap()          { return key_map; }

void setKeyMap(String key,String val)
{
   if (val == null) key_map.remove(key);
   else key_map.put(key,val);
}

boolean getRemoteFileAccess()           { return remote_files; }

JcompControl getJcompControl()          { return jcomp_main; 
}


/********************************************************************************/
/*                                                                              */
/*      Argument processing                                                     */
/*                                                                              */
/********************************************************************************/

private void scanArgs(String [] args)
{
   for (int i = 0; i < args.length; ++i) {
      if (args[i].startsWith("-")) {
         if (i+1 < args.length) {
            if (args[i].startsWith("-m")) {                     // -m <mint id>
               mint_id = args[++i];
               server_mode = true;
               continue;
             }
            else if (args[i].startsWith("-h")) {                // -host <ollama host>
               ollama_host = args[++i];
               continue;
             }
            else if (args[i].startsWith("-p")) {                // -port <ollama port>
               try {
                  ollama_port = Integer.parseInt(args[++i]);
                }
               catch (NumberFormatException e) {
                  badArgs();
                }
               continue;
             }
            else if (args[i].startsWith("-l")) {                // -l <llama model>
               ollama_model = args[++i];
               continue;
             }
            else if (args[i].startsWith("-d")) {                // -d <project file | dir>
               project_file = new File(args[++i]);
               continue;
             }
            else if (args[i].startsWith("-f")) {                // -f <input file>
               input_file = new File(args[++i]);
               continue;
             }
            else if (args[i].startsWith("-L")) {                // -Log <logfile>
               log_file = new File(args[++i]);
               continue;
             }
          }
         if (args[i].startsWith("-")) {
            if (args[i].startsWith("-i")) {                     // -interactive
               interactive_mode = true;
             }
            else if (args[i].startsWith("-think")) {
               raw_flag = true;
               think_flag = true;
             }
            else if (args[i].startsWith("-remote")) {           // -remoteFileAccess
               remote_files = true;
             }
            else if (args[i].startsWith("-D")) {                // -DEBUG  
               log_level = IvyLog.LogLevel.DEBUG;
               log_stderr = true;
               // set log level
             }
            else badArgs();
          }
         else {
            badArgs();
          }
       }
    }
}



private void badArgs()
{
   System.err.println("LIMBA: limba [-host <ollama host>] [-port <ollama port>] ");
   System.err.println("          [-llama <llama model>] [-m <mint id>]");
   System.err.println("          [-sh host -sp port]");
   System.err.println("          [-directory <project file or directory>] [-interactive]");
   System.exit(1);
}



/********************************************************************************/
/*                                                                              */
/*      Processing methods                                                      */
/*                                                                              */
/********************************************************************************/

private void process()
{
   LimbaMsg msg = null;
   
   IvyLog.setupLogging("LIMBA",true);
   IvyLog.setLogLevel(log_level);
   IvyLog.setLogFile(log_file);
   IvyLog.useStdErr(log_stderr);
   
   IvyLog.logD("LIMBA","Running with " + getUrl() + " " + getModel());
   
   startOllama();
   
   if (project_file != null) {
      rag_model = new LimbaRag(this,project_file);
      rag_model.getChain();             // FOR DEBUGGING ONLY
    }
    
   command_factory = new LimbaCommandFactory(this);
   
   if (server_mode && mint_id != null) {
      msg = new LimbaMsg(this,mint_id);
    }
   
   if (input_file != null) {
      try (FileReader fr = new FileReader(input_file)) {
         if (input_file.getName().endsWith(".xml")) {
            processXmlFile(fr);
          }
         else {
            processFile(fr,false);
          }
       }
      catch (IOException e) {
         IvyLog.logE("LIMBA","Problem reading input file " + input_file,e);
       }
    }
   
   if (interactive_mode) {
      try (Reader r = new InputStreamReader(System.in)) {
         processFile(r,true);
       }
      catch (IOException e) {
         IvyLog.logE("LIMBA","Problem reading stdin",e);
       }
    }
   else if (server_mode) {
      boolean haveping = msg.sendPing();
      synchronized (this) {
         for ( ; ; ) {
            // wait for explicit exit command
            try {
               wait(10000);
             }
            catch (InterruptedException e) { }
            boolean chk = msg.sendPing();
            if (haveping && !chk) break;
            else if (!haveping && chk) haveping = true;
          }
       }
    }
}



/********************************************************************************/
/*                                                                              */
/*      Process multiple queries/commands                                       */
/*                                                                              */
/********************************************************************************/

private void processFile(Reader r,boolean prompt)
{
   try (BufferedReader rdr = new BufferedReader(r)) {
      StringBuffer buf = new StringBuffer();
      
      String promptxt = "LIMBA> ";
      String endtoken = "END";
      boolean endonblank = false;
      LimbaCommand cmd = null;
            
      for ( ; ; ) {
         if (prompt) {
            System.out.print(promptxt);
            promptxt = "LIMBA>>> ";
          }
         
         String line = rdr.readLine();
         if (line == null) break;
         line = line.trim();
         if (line.isEmpty() && buf.isEmpty()) continue;
         if (buf.isEmpty()) {
            cmd = command_factory.createCommand(line); 
            if (cmd == null) continue;
            endonblank = cmd.getEndOnBlank();
            endtoken = cmd.getEndToken(); 
          }
         boolean fini = false;
         if (endtoken != null && line.trim().equals(endtoken)) {
            line = "";
            fini = true;
          }
         if (endonblank && line.isEmpty()) fini = true;
         if (!endonblank && endtoken == null) fini = true;
         
         if (line.endsWith("\\")) {
            line = line.substring(0,line.length()-1);
          }

         
         if (!buf.isEmpty()) buf.append("\n");
         buf.append(line);
         if (fini) {
            handleCommand(cmd,buf.toString());
            buf.setLength(0);
            if (prompt) {
               System.out.println();
               promptxt = "LIMBA> ";
             }
            endtoken = "END";
            endonblank = false;
            cmd = null;
          }
       }
    }
   catch (IOException e) {
      IvyLog.logE("LIMBA","Problem reading commands",e);
    }
}


private void processXmlFile(FileReader fr)
{
   try (IvyXmlReader xr = new IvyXmlReader(fr)) {
      for ( ; ; ) {
         String xmlstr = xr.readXml();
         if (xmlstr == null) break;
         Element xml = IvyXml.convertStringToXml(xmlstr);
         try {
            LimbaCommand cmd = setupLimbaCommand(xml);
            try (IvyXmlWriter xw = new IvyXmlWriter()) {
               xw.begin("RESULT");
               cmd.process(xw);
               xw.end("RESULT");
               IvyLog.logD("LIMBA","Command " + cmd.getCommandName() + ":\n");
               IvyLog.logD("LIMBA","RESULT: " + xw.toString());
             }
            catch (Throwable t) {
               IvyLog.logE("LIMBA",
                   "Problem prcessing command " + cmd.getCommandName(),t);
             }
          }
         catch (LimbaException e) {
            IvyLog.logE("LIMBA","Bad command",e);
          }
       }
    }
   catch (IOException e) {}
}


LimbaCommand setupLimbaCommand(Element xml) throws LimbaException
{
   String cmd = IvyXml.getAttrString(xml,"DO");
   LimbaCommand lcmd = createCommand(cmd);
   if (lcmd == null) {
      throw new LimbaException("Invalid command " + cmd);
    }
   lcmd.setupCommand(xml); 
   
   return lcmd;
}


private void handleCommand(LimbaCommand cmd,String cmdtxt)
{
   try {
      cmd.setupCommand(cmdtxt,true);
      cmd.process(null);
    }
   catch (Throwable t) {
      IvyLog.logE("LIMBA","Problem handling command",t);
    }
}


/********************************************************************************/
/*                                                                              */
/*      Issue long ollama query                                                 */
/*                                                                              */
/********************************************************************************/

String askOllama(String cmd) throws Exception
{
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
   IvyLog.logD("LIMBA","Stats: " + rslt.getDoneReason() + " " +
         rslt.getEvalCount() + " " + rslt.getEvalDuration() + " " +
         rslt.getLoadDuration() + " " + rslt.getPromptEvalCount() + " " +
         rslt.getTotalDuration());
   if (getThinkFlag()) {
      String thnk = rslt.getThinking();
      IvyLog.logD("LIMBA","Thinging: " + thnk);
    }
   IvyLog.logD("LIMBA","\n------------------------\n\n");
   
   return rslt.getResponse();
}


/********************************************************************************/
/*                                                                              */
/*      Response extraction methods                                             */
/*                                                                              */
/********************************************************************************/

static List<String> getJavaCode(String resp)
{
   if (resp == null) return null;
   if (!resp.contains("```")) return List.of(resp);
   
   List<String> rslt = new ArrayList<>();
   int start = 0;
   for ( ; ; ) {
      int idx0 = resp.indexOf("```",start);
      if (idx0 < 0) break;
      int idx1 = resp.indexOf("\n",idx0);
      if (idx1 < 0) {
         break;
       }
      String text0 = resp.substring(idx0+3,idx1).trim();
      if (!text0.isEmpty() && !text0.equals("java")) {
         idx1 = idx0 + 3;
       }
      else idx1 = idx1+1;
      int idx2 = resp.indexOf("```",idx1);
      if (idx2 < 0)  {
         break;
       }
      else {
         rslt.add(resp.substring(idx1,idx2));
         start = idx2+3;
       }
    }
   return rslt;
}


static String getJavaDoc(String resp)
{
   List<String> jcodes = getJavaCode(resp);
   if (jcodes == null) return null;
   for (String jcode : jcodes) {
      int idx0 = jcode.indexOf("/**");
      if (idx0 < 0) continue;
      int idx1 = jcode.indexOf("*/",idx0);
      if (idx1 < 0) jcode.substring(idx0);
      int idx2 = jcode.indexOf("\n",idx1);
      if (idx2 > 0) idx1 = idx2+1;
      String jdoc = jcode.substring(idx0,idx1);
      if (!jdoc.endsWith("\n")) jdoc += "\n";
      return jdoc;
    }
   return null;
}




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
    }
   
}       // end of inner class StreamHandler


/********************************************************************************/
/*                                                                              */
/*      Start the ollama server if needed                                       */
/*                                                                              */
/********************************************************************************/

private void startOllama()
{
   String host = "http://" + ollama_host + ":" + ollama_port + "/";
   IvyLog.logD("LIMBA","Starting OLLAMA at " + host);
   try {
      ollama_api = new OllamaAPI(host);
      ollama_api.setRequestTimeoutSeconds(300L);
      boolean ping = ollama_api.ping();
      if (ping) {
         IvyLog.logD("LIMBA","OLLAMA started successfully");
         return;
       }
    }
   catch (Throwable t) {
      IvyLog.logE("LIMBA","Problem with ollama",t);
    }
   
   // start ollama server if needed:  command: ollama serve
   System.err.println("OLLAMA server not running");
   System.exit(1);
}



}       // end of class LimbaMain




/* end of LimbaMain.java */

