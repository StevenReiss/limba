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

import io.github.ollama4j.OllamaAPI;
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
private File project_file;
private File input_file;
private OllamaAPI ollama_api;
private boolean raw_flag;
private boolean think_flag;
private Options generate_options;
private LimbaCommandFactory command_factory;



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
   generate_options = new OptionsBuilder().build();
   
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
   
   if (project_file != null) {
      // set up RAG for project
    }
   
   command_factory = new LimbaCommandFactory(this);
   
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
      
      String promptxt = "LIMBA> ";
      String endtoken = "END";
      boolean endonblank = false;
            
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
            LimbaCommand cmd = command_factory.createCommand(line); 
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
         
         if (line.endsWith("\\")) {
            line = line.substring(0,line.length()-1);
          }
         
         if (!buf.isEmpty()) buf.append("\n");
         buf.append(line);
         if (fini) {
            handleCommand(buf.toString());
            buf.setLength(0);
            if (prompt) {
               System.out.println();
               promptxt = "LIMBA> ";
             }
            endtoken = "END";
            endonblank = false;
          }
       }
    }
   catch (IOException e) {
      IvyLog.logE("LIMBA","Problem reading commands",e);
    }
}


private void handleCommand(String cmd)
{
   try {
      // handle command here
    }
   catch (Throwable t) {
      IvyLog.logE("LIMBA","Problem handling command",t);
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

