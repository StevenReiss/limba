/********************************************************************************/
/*                                                                              */
/*              LimbaMsg.java                                                   */
/*                                                                              */
/*      Provide a MSG interface to LIMBA                                        */
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
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.Socket;
import java.util.Random;

import org.w3c.dom.Element;

import edu.brown.cs.ivy.file.IvyLog;
import edu.brown.cs.ivy.mint.MintArguments;
import edu.brown.cs.ivy.mint.MintControl;
import edu.brown.cs.ivy.mint.MintHandler;
import edu.brown.cs.ivy.mint.MintMessage;
import edu.brown.cs.ivy.mint.MintConstants.MintSyncMode;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlReaderThread;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

class LimbaMsg implements LimbaConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private MintControl     mint_control;
private LimbaMain       limba_main;
private SocketClient    socket_client;

private static Random   random_gen = new Random();


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

LimbaMsg(LimbaMain lm,String mintid)
{
   limba_main = lm;
   mint_control = MintControl.create(mintid,MintSyncMode.ONLY_REPLIES);
   mint_control.register("<LIMBA DO='_VAR_0' />",
         new CommandHandler());
   socket_client = null;
}


LimbaMsg(LimbaMain lm,String host,int port) throws IOException
{
   limba_main = lm;
   mint_control = null;
   socket_client = setupSocketClient(host,port);
   socket_client.start();
}


/********************************************************************************/
/*                                                                              */
/*      Command processing                                                      */
/*                                                                              */
/********************************************************************************/

private String processCommand(String cmd,Element xml) throws LimbaException
{
   try (IvyXmlWriter xw = new IvyXmlWriter()) {
      xw.begin("RESULT");
      
      switch (cmd) {
         case "PING" :
            xw.text("PONG");
            break;
         case "PROJECT" :
            loadProjectData();
            break;
         case "LIST" :
         case "DETAILS" :
            // immediate commands
            LimbaCommand lcmd = setupLimbaCommand(cmd,xml);
            lcmd.process(xw);
            break;
         default :
            // background commands
            LimbaCommand bcmd = setupLimbaCommand(cmd,xml);
            String rid = IvyXml.getAttrString(xml,"RID");
            if (rid == null) {
               rid = "LIMBA_" + random_gen.nextInt(1000000);
             }
            CommandProcessor cp = new CommandProcessor(bcmd,rid);
            xw.field("RID",rid);
            cp.start();
       }
      
      xw.end("RESULT");
      return xw.toString();
    }
}


private LimbaCommand setupLimbaCommand(String cmd,Element xml)
   throws LimbaException
{
   LimbaCommand lcmd = limba_main.createCommand(cmd);
   if (lcmd == null) {
      throw new LimbaException("Invalid command " + cmd);
    }
   String opts = IvyXml.getAttrString(xml,"OPTIONS");
   lcmd.setOptions(opts);
   String body = IvyXml.getTextElement(xml,"BODY");
   lcmd.setupCommand(body,false);
   
   return lcmd;
}


/********************************************************************************/
/*                                                                              */
/*      Load project infromation from BUBBLES                                   */
/*                                                                              */
/********************************************************************************/

private void loadProjectData()
{
   
}



/********************************************************************************/
/*                                                                              */
/*      Background command processor                                            */
/*                                                                              */
/********************************************************************************/

private class CommandProcessor extends Thread {
   
   private LimbaCommand for_command;
   private String reply_id;
   
   CommandProcessor(LimbaCommand cmd,String rid) {
      super("LIMBA_" + cmd.getCommandName() + "_" + rid);
      for_command = cmd;
      reply_id = rid;
    }
   
   @Override public void run() {
      try (IvyXmlWriter xw = new IvyXmlWriter()) {
         xw.begin("LIMBA");
         xw.field("RID",reply_id);
         xw.begin("RESULT");
         for_command.process(xw);
         xw.end("RESULT");
         xw.end("LIMBA");
         mint_control.send(xw.toString());
       }
      catch (Throwable t) {
         IvyXmlWriter xw = new IvyXmlWriter();
         xw.begin("LIMBA");
         xw.field("RID",reply_id);
         xw.begin("ERROR");
         xw.textElement("MESSAGE",t);
         xw.end("ERROR");
         xw.end("LIMBA");
         mint_control.send(xw.toString());
         xw.close();
       }
    }

}



/********************************************************************************/
/*                                                                              */
/*      Command handler class                                                   */
/*                                                                              */
/********************************************************************************/

private final class CommandHandler implements MintHandler {
   
   @Override public void receive(MintMessage msg,MintArguments args) {
      IvyLog.logD("LIMBA","PROCESS MSG COMMAND " + msg.getText());
      String cmd = args.getArgument(0);
      Element xml = msg.getXml();
      String rslt = null;
      try {
         rslt = processCommand(cmd,xml);
       }
      catch (LimbaException e) {
         String xmsg = "LIMBA error in command " + cmd + ": " + e;
         IvyLog.logE("LIMBA",xmsg,e);
         IvyXmlWriter xw = new IvyXmlWriter();
         xw.cdataElement("ERROR",xmsg);
         rslt = xw.toString();
         xw.close();
       }
      catch (Throwable t) {
         IvyLog.logE("LIMBA","Problem processing MSG command " + cmd,t);
         IvyXmlWriter xw = new IvyXmlWriter();
         String xmsg = "Problem processing command " + cmd + ":" + t;
         xw.begin("ERROR");
         xw.textElement("MESSAGE",xmsg);
         xw.end("ERROR");
         rslt = xw.toString();
         xw.close();
       }
      msg.replyTo(rslt);
    }
}



/********************************************************************************/
/*                                                                              */
/*      SocketClient for handling non-MSG based interface                       */
/*                                                                              */
/********************************************************************************/

private SocketClient setupSocketClient(String host,int port) throws IOException
{
   Socket s = new Socket(host,port);
   InputStream ins = s.getInputStream();
   Reader r = new InputStreamReader(ins);
   OutputStream ots = s.getOutputStream();
   Writer w = new OutputStreamWriter(ots);
  
   return new SocketClient(s,r,w);
}

private final class SocketClient extends IvyXmlReaderThread {
   
   private Writer output_writer;
   
   SocketClient(Socket s,Reader r,Writer ots) {
      super("LIMBA_SERVER_READER",r);
      output_writer = ots;
    }
   
   @Override public void processXmlMessage(String msg) {
      IvyLog.logD("LIMBA","Process socket client message " + msg);
      Element xml = IvyXml.convertStringToXml(msg);
      String cmd = IvyXml.getAttrString(xml,"DO");
      String rslt = null;
      try {
         rslt = processCommand(cmd,xml);
       }
      catch (LimbaException e) {
         String xmsg = "LIMBA error in command " + cmd + ": " + e;
         IvyLog.logE("LIMBA",xmsg,e);
         IvyXmlWriter xw = new IvyXmlWriter();
         xw.cdataElement("ERROR",xmsg);
         rslt = xw.toString();
         xw.close();
       }
      catch (Throwable t) {
         IvyLog.logE("LIMBA","Problem processing SOCKET command " + cmd,t);
         IvyXmlWriter xw = new IvyXmlWriter();
         String xmsg = "Problem processing command " + cmd + ":" + t;
         xw.begin("ERROR");
         xw.textElement("MESSAGE",xmsg);
         xw.end("ERROR");
         rslt = xw.toString();
         xw.close();
       }
      send(rslt);
    }
   
   @Override public void processDone() {}
   
   void send(String resp) {
      if (resp == null) resp = "";
      try {
         output_writer.write(resp + "\n");
       }
      catch (IOException e) {
         IvyLog.logE("LIMBA","Write error for socket client",e);
       }
    }
   
}       // end of inner class SocketClient



}       // end of class LimbaMsg




/* end of LimbaMsg.java */

