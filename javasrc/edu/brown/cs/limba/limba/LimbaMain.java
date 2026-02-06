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

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

import dev.langchain4j.chain.ConversationalRetrievalChain;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.service.AiServices;
import io.github.ollama4j.OllamaAPI;
import io.github.ollama4j.models.response.Model;
import io.github.ollama4j.utils.Options;
import io.github.ollama4j.utils.OptionsBuilder;
import edu.brown.cs.ivy.exec.IvyExec;
import edu.brown.cs.ivy.file.IvyFormat;
import edu.brown.cs.ivy.file.IvyLog;
import edu.brown.cs.ivy.jcomp.JcompControl;
import edu.brown.cs.ivy.mint.MintControl;
import edu.brown.cs.ivy.mint.MintConstants.CommandArgs;
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
private boolean server_mode;
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
private LimbaMonitor msg_server;
private String user_style;
private String user_context;
private String inited_model;
private String workspace_name;
private Map<String,LimbaChatter> chat_interfaces;
private PrintWriter limba_transcript;

private static final String SPLIT_PATTERN;
private static boolean http_log = false;

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
   server_mode = false;
   ollama_model = "llama4:scout";
   workspace_name = null;
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
   chat_interfaces = new HashMap<>();
   limba_transcript = null;

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


Map<String,String> getKeyMap()          { return key_map; }

void setKeyMap(String key,String val)
{
   if (val == null) key_map.remove(key);
   else key_map.put(key,val);
}

boolean getRemoteFileAccess()           { return remote_files; }

LimbaMonitor getMessageServer()             { return msg_server; }
public MintControl getMintControl()     
{
   return msg_server.getMintControl();
}


void setupMessageServer(String mintid)
{
   mint_id = mintid;
   msg_server = new LimbaMonitor(this,mint_id);
}

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

String getWorkspace()                   { return workspace_name; }
void setWorkspace(String nm)
{
   workspace_name = nm;
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
   chat_interfaces.clear();

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
            else if (args[i].startsWith("-f")) {                // -f <input file>
               input_file = new File(args[++i]);
               continue;
             }
            else if (args[i].startsWith("-L")) {                // -Log <logfile>
               log_file = new File(args[++i]);
               continue;
             }
            else if (args[i].startsWith("-T")) {                // -Transcript <file>
               startTranscript(args[++i]);
               continue;
             }
          }
         if (args[i].startsWith("-")) {
            if (args[i].startsWith("-remote")) {                // -remoteFileAccess
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

   rag_model = null;
   chat_interfaces.clear();

   command_factory = new LimbaCommandFactory(this);

   if (server_mode && mint_id != null) {
      setupMessageServer(mint_id);
    }

   if (input_file != null) {
      try (FileReader fr = new FileReader(input_file)) {
         processXmlFile(fr);
       }
      catch (IOException e) {
         IvyLog.logE("LIMBA","Problem reading input file " + input_file,e);
       }
    }

   if (server_mode) {
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
/*      Transcript methods                                                      */
/*                                                                              */
/********************************************************************************/

private void startTranscript(String nm)
{
   IvyLog.logD("LIMBA","Start transcript " + nm);
   File ft = new File(nm);
   boolean fg = ft.exists();
   try {
      limba_transcript = new PrintWriter(new FileWriter(ft,true),true);
      if (!fg) {
         transcript("<html>");
       }
      transcript("<br><div align='center'><p><font color='darkgreen'>" + 
            (new Date().toString()) + "</font></p></div><br>");
    }
   catch (IOException e) {
      IvyLog.logE("LIMBA","Can't open transcript file " + ft);
    }
}



void transcript(String cnts)
{
   if (limba_transcript == null) return;
   
   limba_transcript.println(cnts);
}


void transcriptResponse(String cnts)
{
   if (limba_transcript == null) return;
   
   String text = IvyFormat.formatText(cnts);
   String disp = "<div align='left'><p><font color='black'>" + text +
         "</font></p></div>";
   transcript(disp);
   
   transcript("<br><hl><br>");
}


void transcriptRequest(String cnts)
{
   if (limba_transcript == null) return;
   
   String text = IvyFormat.formatText(cnts);
   String disp = "<div align='right'><p style='text-indent: 50px;'><font color='blue'>" + text + 
         "</font></p></div>";
   transcript(disp);
}


void transcriptNote(String cnts)
{
   if (limba_transcript == null) return;
   
   String text = IvyFormat.formatText(cnts);
   String disp = "<br><div align='left'><p><font color='darkmagenta'>AGENT: " + text +
         "</font></p></div>";
   transcript(disp);
}


/********************************************************************************/
/*                                                                              */
/*      Process multiple queries/commands                                       */
/*                                                                              */
/********************************************************************************/

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
   LimbaCommand lcmd = command_factory.createCommand(xml);
   if (lcmd == null) {
      throw new LimbaException("Invalid command " + cmd);
    }

   return lcmd;
}



/********************************************************************************/
/*                                                                              */
/*      RAG processing                                                          */
/*                                                                              */
/********************************************************************************/

LimbaRag getRagModel()                  { return rag_model; }



void setupRag()
{
   IvyLog.logD("LIMBA","Get sources from bubbles " + msg_server);

   if (msg_server == null) {
      rag_model = null;
    }
   else {
      List<File> sources = msg_server.getSources();
      IvyLog.logD("LIMBA","Found " + sources.size() + " sources");
      if (sources != null && !sources.isEmpty() && workspace_name != null) {
         rag_model = new LimbaRag(this,sources,workspace_name); 
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
   return askOllama(cmd0,usectx,null,null,null);
}


String askOllama(String cmd0,boolean usectx,ChatMemory history,
      EnumSet<LimbaToolSet> tools,Map<String,?> context)
   throws Exception
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
         rag_model + " " + Thread.currentThread().hashCode() + " " +
         tools + " " +
         Thread.currentThread().getName() + ":\n" + cmd);
   
   transcriptRequest(cmd);

   try {
      // might need to add to history
      String resp = getChain(history,usectx,tools,context).chat(cmd);
      IvyLog.logD("LIMBA","Context Response: " + resp);
      IvyLog.logD("LIMBA","\n------------------------\n\n");
      transcriptResponse(resp);
      return resp;
    }
   catch (Throwable t) {
      IvyLog.logE("LIMBA","Problem with chained response",t);
      throw t;
    }
}


private LimbaChatter getChain(ChatMemory mem,boolean usectx,
      EnumSet<LimbaToolSet> toolids,Map<String,?> context)
{
   if (toolids == null) {
      toolids = EnumSet.of(LimbaToolSet.PROJECT);
    }
   String key = getKey(toolids,context);

   LimbaChatter rslt = chat_interfaces.get(key);
   if (rslt != null) return rslt;

   OllamaChatModel chat = OllamaChatModel.builder()
      .baseUrl(getUrl())
      .maxRetries(3)
      .timeout(Duration.ofMinutes(15))
      .logRequests(http_log)
      .logResponses(http_log)
      .modelName(getModel())
      .build();
   ConversationalRetrievalChain.Builder bldr = ConversationalRetrievalChain.builder();
   bldr.chatModel(chat);

   ContentRetriever cr = null;
   if (rag_model == null) {
      setupRag();
    }
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

   List<Object> tools = new ArrayList<>();
   if (!toolids.isEmpty()) {
      if (toolids.contains(LimbaToolSet.PROJECT)) {
         if (rag_model != null) {
            tools.add(new LimbaTools(this,rag_model.getFiles()));
          }
         else {
            IvyLog.logE("LIMBA","no project found");
          }
       }
      if (toolids.contains(LimbaToolSet.DEBUG)) {
         tools.add(new LimbaDiadTools(this,context));
       }
    }

   // should pass in tool set and save chat_interface for those tools
   if (!tools.isEmpty()) {
      AiServices<LimbaAssistant> aib = AiServices.builder(LimbaAssistant.class)
         .chatModel(chat)
         .tools(tools)
         .contentRetriever(cr);
      if (mem != null) {
         aib.chatMemory(mem);
       }
      LimbaAssistant la = aib.build();
      IvyLog.logD("LIMBA","Built limba assistant " + la);
      rslt = la;
    }
   else {
      ConversationalRetrievalChain chain = bldr.build();
      rslt = new ChainChatter(chain);
    }

   chat_interfaces.put(key,rslt);

   return rslt;
}


private String getKey(EnumSet<LimbaToolSet> tools,Map<String,?> context)
{
   if (tools.isEmpty()) return "*";

   String k = tools.toString();
   if (tools.contains(LimbaToolSet.DEBUG)) {
      k += "." + context.get("DEBUGID") + ".";
    }

   return k;
}

void removeDebugContext(String debugid)
{
   for (Iterator<String> it = chat_interfaces.keySet().iterator(); it.hasNext(); ) {
      String key = it.next();
      if (key.contains("DEBUG") && key.contains(debugid + ".")) {
         it.remove();
       }
    }
}



private interface LimbaChatter {
   String chat(String msg);
}

private interface LimbaAssistant extends LimbaChatter {
   String chat(String msg);
}

private class ChainChatter implements LimbaChatter {

   private ConversationalRetrievalChain ret_chain;

   ChainChatter(ConversationalRetrievalChain chain) {
      ret_chain = chain;
    }

   @Override public String chat(String msg) {
      return ret_chain.execute(msg);
    }

}       // end of inner class ChainChatter



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

   List<String> base = new ArrayList<>();
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
         if (type.equals("java")) {
            extractFragments(resp.substring(idx1,idx2),rslt);
          }
         else if (type.isEmpty()) {
            extractFragments(resp.substring(idx1,idx2),base);
          }
        
         start = idx2+3;
       }
    }

   if (!base.isEmpty() && rslt.isEmpty()) rslt = base;

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


/********************************************************************************/
/*                                                                              */
/*      Setup bedrock for debugging                                             */
/*                                                                              */
/********************************************************************************/

private static final String BROWN_ECLIPSE = "/u/spr/java-2024-09/eclipse/eclipse";
private static final String BROWN_WS = "/u/spr/Eclipse/";
private static final String HOME_MAC_ECLIPSE =
   "/vol/Developer/java-2024-09/Eclipse.app/Contents/MacOS/eclipse";
private static final String HOME_MAC_WS = "/Users/spr/Eclipse/";
private static final String HOME_LINUX_ECLIPSE = "/pro/eclipse/java-2023-12/eclipse/eclipse";
private static final String HOME_LINUX_WS = "/home/spr/Eclipse/";

void setupBedrock(String workspace,String mint)
{
   IvyLog.logI("LIMBA","Starting bedrock/eclipse for debugging");

   File ec1 = new File(BROWN_ECLIPSE);
   File ec2 = new File(BROWN_WS);
   if (!ec1.exists()) {
      ec1 = new File(HOME_MAC_ECLIPSE);
      ec2 = new File(HOME_MAC_WS);
    }
   if (!ec1.exists()) {
      ec1 = new File(HOME_LINUX_ECLIPSE);
      ec2 = new File(HOME_LINUX_WS);
    }
   if (!ec1.exists()) {
      System.err.println("Can't find bubbles version of eclipse to run");
      throw new Error("No eclipse");
    }
   ec2 = new File(ec2,workspace);

   setupMessageServer(mint);

   String cmd = ec1.getAbsolutePath();
   cmd += " -application edu.brown.cs.bubbles.bedrock.application";
   cmd += " -data " + ec2.getAbsolutePath();
   cmd += " -nosplash";
   cmd += " -vmargs -Dedu.brown.cs.bubbles.MINT=" + mint;
   cmd += " -Xmx16000m";

   IvyLog.logI("LIMBA","RUN: " + cmd);

   try {
      for (int i = 0; i < 250; ++i) {
         try {
            Thread.sleep(1000);
          }
         catch (InterruptedException e) { }
         if (msg_server.pingEclipse()) {
            CommandArgs a1 = new CommandArgs("LEVEL","DEBUG");
            msg_server.sendBubblesMessage("LOGLEVEL",a1,null);
            msg_server.sendBubblesMessage("ENTER",null,null);
            setWorkspace(workspace);
            return;
          }
         if (i == 0) {
            new IvyExec(cmd);
            msg_server.pongEclipse();
          }
       }
    }
   catch (IOException e) {
      IvyLog.logE("LIMBA","Problem with eclipse",e);
    }

   throw new Error("Problem running Eclipse: " + cmd);
}




}       // end of class LimbaMain




/* end of LimbaMain.java */

