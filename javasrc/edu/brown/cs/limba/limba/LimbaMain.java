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
import java.net.http.HttpTimeoutException;
import java.util.List;

import io.github.ollama4j.OllamaAPI;
import io.github.ollama4j.exceptions.OllamaBaseException;
import io.github.ollama4j.models.generate.OllamaStreamHandler;
import io.github.ollama4j.models.response.Model;
import io.github.ollama4j.models.response.ModelDetail;
import io.github.ollama4j.models.response.OllamaResult;
import io.github.ollama4j.utils.Options;
import io.github.ollama4j.utils.OptionsBuilder;
import edu.brown.cs.ivy.file.IvyLog;
import edu.brown.cs.ivy.mint.MintArguments;
import edu.brown.cs.ivy.mint.MintControl;
import edu.brown.cs.ivy.mint.MintHandler;
import edu.brown.cs.ivy.mint.MintMessage;
import edu.brown.cs.ivy.mint.MintConstants.MintSyncMode; 

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
private MintControl mint_control;
private LimbaCommand process_command;
private File project_file;
private File input_file;
private OllamaAPI ollama_api;
private boolean raw_flag;
private boolean think_flag;
private Options generate_options;



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
   process_command = null;
   project_file = null;
   input_file = null;
   raw_flag = false;
   think_flag = false;
   generate_options = new OptionsBuilder().build();
   
   scanArgs(args);
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
          }
         if (args[i].startsWith("-")) {
            if (args[i].startsWith("-i")) {                     // -interactive
               interactive_mode = true;
             }
            else if (args[i].startsWith("-d")) {
               raw_flag = true;
               think_flag = true;
             }
            else if (args[i].startsWith("-L")) {                   // -LIST_MODEL
               process_command = LimbaCommand.LIST_MODELS; 
             }  
            else if (args[i].startsWith("-D")) {                   // -DETAILS
               process_command = LimbaCommand.MODEL_DETAILS;
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
   System.err.println("          [-directory <project file or directory>] [-interactive]");
   System.err.println("          [-LIST_MODEL | -DETAILS]");
   System.exit(1);
}



/********************************************************************************/
/*                                                                              */
/*      Processing methods                                                      */
/*                                                                              */
/********************************************************************************/

private void process()
{
   IvyLog.setupLogging("LIMBA",true);
   IvyLog.setLogLevel(IvyLog.LogLevel.DEBUG);
   String home = System.getProperty("user.home");
   File f1 = new File(home);
   File f2 = new File(f1,"limba.log");
   IvyLog.setLogFile(f2);
   IvyLog.useStdErr(true);
         
   startOllama();
   
   if (process_command != null) {
      switch (process_command) {
         case LIST_MODELS:
            handleListModels();
            break;
         case MODEL_DETAILS :
            handleModelDetails();
            break;
       }
      return;
    }
   
   if (server_mode && mint_id != null) {
      mint_control = MintControl.create(mint_id,MintSyncMode.ONLY_REPLIES);
      mint_control.register("<LIMBA DO='_VAR_0' SID='_VAR_1'/>",
            new CommandHandler());
    }
   
   if (input_file != null) {
      try (FileReader fr = new FileReader(input_file)) {
         processFile(fr,false);
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
      for ( ; ; ) {
         if (prompt) System.out.print("LIMBA> ");
         String line = rdr.readLine();
         if (line == null) break;
         line = line.trim();
         if (line.isEmpty() && buf.isEmpty()) continue;
         boolean fini = true;
         if (line.endsWith("\\")) {
            line = line.substring(0,line.length()-1);
            fini = false;
          }
         if (!buf.isEmpty()) buf.append(" ");
         buf.append(line);
         if (fini) {
            handleQuery(buf.toString());
            buf.setLength(0);
            System.out.println();
          }
       }
    }
   catch (IOException e) {
      IvyLog.logE("LIMBA","Problem reading commands",e);
    }
}



/********************************************************************************/
/*                                                                              */
/*      Start the ollama server if needed                                       */
/*                                                                              */
/********************************************************************************/

private void startOllama()
{
   String host = "http://" + ollama_host + ":" + ollama_port + "/";
   try {
      ollama_api = new OllamaAPI(host);
      boolean ping = ollama_api.ping();
      if (ping) return;
    }
   catch (Throwable t) {
      IvyLog.logE("LIMBA","Problem with ollama",t);
    }
   
   // start ollama server if needed:  command: ollama serve
   System.err.println("OLLAMA server not running");
   System.exit(1);
}



/********************************************************************************/
/*                                                                              */
/*      List models                                                             */
/*                                                                              */
/********************************************************************************/

private void handleListModels()
{
   try {
      List<Model> models = ollama_api.listModels();
      for (Model m : models) {
         System.out.println(m.getName());
       }
    }
   catch (Throwable t) {
      IvyLog.logE("LIMBA","Problem with list models",t);
    }
}



/********************************************************************************/
/*                                                                              */
/*      Model details                                                           */
/*                                                                              */
/********************************************************************************/

private void handleModelDetails()
{
   try {
      ModelDetail md = ollama_api.getModelDetails(ollama_model);
      System.out.println(md);
    }
   catch (Throwable t) {
      IvyLog.logE("LIMBA","Problem with model details",t);
    }
}




/********************************************************************************/
/*                                                                              */
/*      Send text to LLM                                                        */
/*                                                                              */
/********************************************************************************/

private void handleQuery(String q)
{
   for (int i = 0; i < 10; ++i) { 
      try {
         IvyLog.logD("LIMBA","Query: " + q);
//       OllamaResult rslt = ollama_api.generate(ollama_model,q,
//             raw_flag,think_flag,generate_options);
//       IvyLog.logD("LIMBA","Response: " + rslt.getResponse());
//       System.out.println(rslt.getResponse());
         StreamHandler hdlr = new StreamHandler();
         OllamaResult rslt = ollama_api.generate(ollama_model,q,
               raw_flag,generate_options,hdlr);
         IvyLog.logD("LIMBA","Response: " + rslt.getResponse());
         System.out.println(rslt.getResponse());
         return;
       }
      catch (OllamaBaseException e) {
         IvyLog.logE("LIMBA","Problem processinq query",e);
         return;
       }
      catch (HttpTimeoutException e) {
         IvyLog.logE("LIMBA","Query timeout",e);
         continue;
       }
      catch (IOException e) {
         IvyLog.logE("LIMBA","I/O Problem processinq query",e);
         return;
       }
      catch (InterruptedException e) {
         IvyLog.logE("LIMBA","Query interrputed",e);
         continue;
       }
      catch (Throwable t) {
         IvyLog.logE("LIMBA","Query processing problem",t);
         return;
       }
    }
}



/********************************************************************************/
/*                                                                              */
/*      Handle stream output                                                    */
/*                                                                              */
/********************************************************************************/

private final class StreamHandler implements OllamaStreamHandler {

   @Override public void accept(String message) {
//    System.out.println(message);
//    IvyLog.logD("LIMBA","Received: " + message);
}

}       // end of inner class StreamHandler



/********************************************************************************/
/*                                                                              */
/*      Handle commands from BUBBLES                                            */
/*                                                                              */
/********************************************************************************/

private final class CommandHandler implements MintHandler {
   
   @Override public void receive(MintMessage msg,MintArguments args) {
      IvyLog.logD("LIMBA","Process command " + msg.getText());
    }
   
}       // end of inner class CommandHandler


}       // end of class LimbaMain




/* end of LimbaMain.java */

