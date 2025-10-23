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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

import dev.langchain4j.chain.ConversationalRetrievalChain;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import io.github.ollama4j.OllamaAPI;
import io.github.ollama4j.models.response.Model;
import io.github.ollama4j.utils.Options;
import io.github.ollama4j.utils.OptionsBuilder;
import edu.brown.cs.ivy.exec.IvyExec;
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
private String ollama_usehost;
private String alt_host;
private int alt_port;
private String alt_usehost;
private String ollama_model;
private boolean interactive_mode;
private boolean server_mode;
private File project_file;
private File input_file;
private OllamaAPI ollama_api;
private boolean raw_flag;
private Options generate_options;
private LimbaCommandFactory command_factory;
private LimbaRag rag_model;
private Map<String,String> key_map;
private boolean remote_files;
private File log_file;
private IvyLog.LogLevel log_level;
private boolean log_stderr;
private JcompControl jcomp_main;
private LimbaMsg msg_server;
private String user_style;
private String user_context;
private String inited_model;

private static final String SPLIT_PATTERN;

static {
   String split = "^\\s*//\\s*Version\\s+\\d+$";
   SPLIT_PATTERN = split;
}



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
   ollama_usehost = null;
   alt_host = "localhost";
   alt_port = 11434;
   alt_usehost = null;
   interactive_mode = false;
   server_mode = false;
   ollama_model = "llama4:scout";
   project_file = null;
   input_file = null;
   raw_flag = false;
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
   user_style = "";
   user_context = "";
   ollama_api = null;
   inited_model = null;
   
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

Options getOllamaOptions()              { return generate_options; }

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

JcompControl getJcompControl()          { return jcomp_main; }

String getUserStyle()                   { return user_style; }

String getUserContext()                 { return user_context; }
void setUserStyle(String s)             
{ 
   if (s == null) s = "";
   else if (!s.isEmpty() && !s.startsWith("\n")) s = "\n" + s;
   user_style = s; 
}


void setUserContext(String s)             
{ 
   if (s == null) s = "";
   else if (!s.isEmpty() && !s.startsWith("\n")) s = "\n" + s;
   user_context = s;
}


boolean setModel(String model) 
{
   if (getOllama() != null && model != null) {
      boolean fnd = false;
      try {
         List<Model> mdls = getOllama().listModels();
         for (Model m : mdls) {
            if (model.equals(m.getModelName()) || 
                  model.equals(m.getModel())) {
               fnd = true;
               break;
             }
          }
       }
      catch (Exception e) { }
      if (!fnd) {
         IvyLog.logI("LIMBA","Model " + model + " not found");
         model = null;
       }
    }
   
   ollama_model = model;
   
   return true;
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
            else if (args[i].startsWith("-u")) {                // -use <ollama actual host>
               ollama_usehost = args[++i];
               continue;
             }
            else if (args[i].startsWith("-alth")) {             // -althost <ollama host>
               alt_host = args[++i];
               continue;
             }
            else if (args[i].startsWith("-altp")) {             // -altport <ollama port>
               try {
                  alt_port = Integer.parseInt(args[++i]);
                }
               catch (NumberFormatException e) {
                  badArgs();
                }
               continue;
             }
            else if (args[i].startsWith("-altu")) {             // -altuse <ollama usehost>
               alt_usehost = args[++i];
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
   
   if (alt_host != null && alt_host.equals(ollama_host) && alt_port == ollama_port) {
      alt_host = null;
      alt_port = 0;
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
   msg_server = null;
   
   IvyLog.setupLogging("LIMBA",true);
   IvyLog.setLogLevel(log_level);
   IvyLog.setLogFile(log_file);
   IvyLog.useStdErr(log_stderr);
   
   IvyLog.logD("LIMBA","Running with " + getUrl() + " " + getModel() + " " +
         new Date());
   
   boolean fg = startOllama(ollama_host,ollama_port,ollama_usehost);
   if (!fg) {
      IvyLog.logD("LIMBA","Try alternate host " + alt_host + ":" + alt_port);
      if (alt_host != null && alt_port != 0) {
         fg = startOllama(alt_host,alt_port,alt_usehost);
         if (fg) {
            ollama_host = alt_host;
            ollama_port = alt_port;
            ollama_usehost = alt_usehost;
          }
       }
    }
   if (!fg) {
      System.err.println("OLLAMA server not running");
      System.exit(1);
    }
   
   if (getModel() != null) {
      setModel(ollama_model);
    }
   
   setupRag(project_file);
    
   command_factory = new LimbaCommandFactory(this);
   
   if (server_mode && mint_id != null) {
      msg_server = new LimbaMsg(this,mint_id);
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
      boolean haveping = msg_server.sendPing();
      synchronized (this) {
         for ( ; ; ) {
            // wait for explicit exit command
            try {
               wait(10000);
             }
            catch (InterruptedException e) { }
            boolean chk = msg_server.sendPing();
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
   try (BufferedReader rdr = new IncludeReader(r)) {
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
         IvyLog.logD("LIMBA","Process XML command: " + xmlstr);
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
/*      Class for nesting inputs                                                */
/*                                                                              */
/********************************************************************************/

private static class IncludeReader extends BufferedReader {
   
   private Deque<BufferedReader> reader_stack;
   
   IncludeReader(Reader in) {
      super(in);
      reader_stack = new ArrayDeque<>();
      reader_stack.push(new BufferedReader(in));     // Push the initial reader onto the stack
    }
   
   @Override
   public String readLine() throws IOException {
      String currentline = null;
      while (true) {
         if (reader_stack.isEmpty()) {
            return null; // No more readers in the stack
          }
         
         BufferedReader currentReader = reader_stack.peek();
         currentline = currentReader.readLine();
         
         if (currentline == null) {
            // End of current reader, pop it and try the next one
            reader_stack.pop().close(); // Close the popped reader
            continue;
          }
         
         if (currentline.startsWith(">")) {
            String fileName = currentline.substring(1).trim();
            try {
               BufferedReader includedReader = new BufferedReader(new FileReader(fileName));
               reader_stack.push(includedReader);
               // Continue to read from the new included file
               continue;
             }
            catch (IOException e) {
               // Handle file not found or other I/O errors during inclusion
               System.err.println("Error including file: " + fileName + " - " + e.getMessage());
               // Skip this include and continue with the current reader
               continue;
             }
          }
         return currentline; // Return a regular line
       }
    }
   
   @Override
   public void close() throws IOException {
      while (!reader_stack.isEmpty()) {
         reader_stack.pop().close();
       }
      super.close(); // Close the initial reader passed to the constructor
    }
   
}       // end of inner class IncldueHandlingReader


/********************************************************************************/
/*                                                                              */
/*      RAG processing                                                          */
/*                                                                              */
/********************************************************************************/

LimbaRag getRagModel()                  { return rag_model; }

void setupRag(File f)
{
   if (f != null) {
      rag_model = new LimbaRag(this,f);
      rag_model.getContentRetriever();         // FOR DEBUGGING ONLY
    }
   else {
      rag_model = null;
    }
}

void setupRag() 
{
   IvyLog.logD("LIMBA","Get sources from bubbles " + msg_server);
   
   if (msg_server == null) {
      rag_model = null;
    }
   else { 
      List<File> sources = msg_server.getSources();
      IvyLog.logD("LIMBA","Found " + sources.size() + " sources");
      if (sources != null && !sources.isEmpty()) {
         rag_model = new LimbaRag(this,sources);  
       }
      else {
         rag_model = null;
       }
    }
}


/********************************************************************************/
/*                                                                              */
/*      Issue long ollama query                                                 */
/*                                                                              */
/********************************************************************************/

String askOllama(String cmd0,boolean usectx) throws Exception 
{
   return askOllama(cmd0,usectx,null);
}


String askOllama(String cmd0,boolean usectx,ChatMemory history) throws Exception
{
   String cmd = cmd0;
   if (user_style != null) {
      cmd = cmd.replace("$STYLE",user_style);
    }
   if (user_context != null) {
      cmd = cmd.replace("$CONTEXT",user_context);
    }
   if (getModel() == null) return null;
   
   initializeModel();
   
   IvyLog.logD("LIMBA","Query " + usectx + " " + getModel() + " " +
         rag_model + ":\n" + cmd);
   
   try {
      // might need to add to history
      String resp = getChain(history,usectx).execute(cmd);
      IvyLog.logD("LIMBA","Context Response: " + resp);
      IvyLog.logD("LIMBA","\n------------------------\n\n");
      return resp;
    }
   catch (Throwable t) {
      IvyLog.logE("LIMBA","Problem with chained response",t);
      throw t;
    }
}


private ConversationalRetrievalChain getChain(ChatMemory mem,boolean usectx)
{
   OllamaChatModel chat = OllamaChatModel.builder()
      .baseUrl(getUrl())
      .logRequests(true)
      .logResponses(true)
      .modelName(getModel())
      .build();
   ConversationalRetrievalChain.Builder bldr = ConversationalRetrievalChain.builder();
   bldr.chatModel(chat);
   ContentRetriever cr = null;
   if (rag_model != null && usectx) {
      cr = rag_model.getContentRetriever();
    }
   if (cr == null) {
      cr = new EmptyContentRetriever();
    }
   bldr.contentRetriever(cr);
   if (mem != null) {
      bldr.chatMemory(mem);
    }
   ConversationalRetrievalChain chain = bldr.build();
   
   return chain;
}


private static final class EmptyContentRetriever implements ContentRetriever {

   @Override public List<Content> retrieve(Query query) { 
      return new ArrayList<>();
    }
   
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
      String type = resp.substring(idx0+3,idx1).trim();
      if (type.isEmpty()) {
         idx1 = idx0 + 3;
       }
      else idx1 = idx1+1;
      int idx2 = resp.indexOf("```",idx1);
      if (idx2 < 0)  {
         break;
       }
      else {
         if (type.isEmpty() || type.equals("java")) {
            extractFragments(resp.substring(idx1,idx2),rslt);
          }
         start = idx2+3;
       }
    }
   return rslt;
}


private static void extractFragments(String text,List<String> rslt)
{
   String [] elements = text.split(SPLIT_PATTERN);
   for (String s : elements) {
      rslt.add(s);
    }
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
/*      Start the ollama server if needed                                       */
/*                                                                              */
/********************************************************************************/

private boolean startOllama(String hostname,int port,String usehost)
{
   String host = "http://" + hostname + ":" + port + "/";
  
   IvyLog.logD("LIMBA","Starting OLLAMA at " + host);
   try {
      ollama_api = new OllamaAPI(host);
      ollama_api.setRequestTimeoutSeconds(900L);
      for (int i = 0; i < 5; ++i) {
         boolean ping = ollama_api.ping();
         if (ping) {
            IvyLog.logD("LIMBA","OLLAMA started successfully on " + host);
            return true;
          }
       }
      IvyLog.logD("LIMBA","Pings failed to talk to ollama api");
    }
   catch (Throwable t) {
      IvyLog.logI("LIMBA","Problem with ollama on " + host + ": " + t);
    }
   
   return false;
}


private void initializeModel()
{
   if (getModel() == null) return;
   if (getModel().equals(inited_model)) return;
   inited_model = getModel();
   
   try {
      String host = "http://" + ollama_host + ":" + ollama_port + "/";
      String cmd = "limballama.csh " + inited_model;
      if (ollama_usehost != null && !ollama_host.isEmpty()) {
         cmd += " " + ollama_usehost + " " + ollama_host;
       }
      else {
         cmd += " " + host;
       }
      IvyExec exec = new IvyExec(cmd,IvyExec.IGNORE_OUTPUT);
      IvyLog.logD("LIMBA","Running setup commands: " + exec.getCommand());
      exec.waitFor();
    }
   catch (IOException e) {
      IvyLog.logI("LIMBA","Problem prepping ollama: " + e);
    }
   
}




}       // end of class LimbaMain




/* end of LimbaMain.java */

