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

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.w3c.dom.Element;

import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.ivy.file.IvyLog;
import edu.brown.cs.ivy.mint.MintArguments;
import edu.brown.cs.ivy.mint.MintConstants;
import edu.brown.cs.ivy.mint.MintControl;
import edu.brown.cs.ivy.mint.MintDefaultReply;
import edu.brown.cs.ivy.mint.MintHandler;
import edu.brown.cs.ivy.mint.MintMessage;
import edu.brown.cs.ivy.mint.MintConstants.MintSyncMode;
import edu.brown.cs.ivy.xml.IvyXml;
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
   mint_control.register("<BUBBLES DO='EXIT' />",new ExitHandler());
   
   IvyLog.logD("LIMBA","Listening for messages on " + mintid);
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
         case "LIST" :
         case "DETAILS" :
         case "PING" :
            // immediate commands
            LimbaCommand lcmd = limba_main.setupLimbaCommand(xml);
            lcmd.process(xw);
            break;
         default :
            // background commands
            LimbaCommand bcmd = limba_main.setupLimbaCommand(xml);
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






List<File> getSources()
{
   List<File> srcs = new ArrayList<>();
   
   MintDefaultReply rply = new MintDefaultReply();
   
   String msg = "<BUBBLES DO='PROJECTS' />";
   IvyLog.logD("LIMBA","Send to bubbles: " + msg);
   mint_control.send(msg,rply,MintControl.MINT_MSG_FIRST_NON_NULL);
   
   Element r = rply.waitForXml();
   
   if (!IvyXml.isElement(r,"RESULT")) {
      System.err.println("BATT: Problem getting project information: " +
            IvyXml.convertXmlToString(r));
      System.exit(2);
    }
   
   for (Element pe : IvyXml.children(r,"PROJECT")) {
      String pnm = IvyXml.getAttrString(pe,"NAME");
      MintDefaultReply prply = new MintDefaultReply();
      String pmsg = "<BUBBLES DO='OPENPROJECT' PROJECT='" + pnm +
            "' CLASSES='false' FILES='true' PATHS='false' OPTIONS='false' />";
      IvyLog.logD("LIMBA","Send to bubbles: " + pmsg);
      mint_control.send(pmsg,prply,MintControl.MINT_MSG_FIRST_NON_NULL);
      Element pr = prply.waitForXml();
      if (!IvyXml.isElement(pr,"RESULT")) {
	 IvyLog.logI("LIMBA","Problem opening project " + pnm + ": " +
               IvyXml.convertXmlToString(pr));
	 continue;
       }
      Element ppr = IvyXml.getChild(pr,"PROJECT");
      Element files = IvyXml.getChild(ppr,"FILES");
      Set<File> done = new HashSet<>();
      for (Element finfo : IvyXml.children(files,"FILE")) {
         if (!IvyXml.getAttrBool(finfo,"SOURCE")) continue;
         String fpath = IvyXml.getAttrString(finfo,"PATH");
         File f1 = new File(fpath);
         f1 = IvyFile.getCanonical(f1);
         if (!done.add(f1)) continue;
         if (f1.getName().endsWith(".java")) {
            srcs.add(f1);
          }
       }
    }
   
   return srcs;
}

/********************************************************************************/
/*                                                                              */
/*      Send message to client                                                  */
/*                                                                              */
/********************************************************************************/

boolean sendPing()
{
   try (IvyXmlWriter xw = new IvyXmlWriter()) {
      xw.begin("LIMBAREPLY");
      xw.field("DO","PING");
      xw.end("LIMBAREPLY");
      MintDefaultReply mdr = new MintDefaultReply();
      mint_control.send(xw.toString(),mdr,
            MintConstants.MINT_MSG_FIRST_NON_NULL);
      String s = mdr.waitForString();
      if (s != null) return true;
    }
   return false;
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
         xw.begin("LIMBAREPLY");
         xw.field("RID",reply_id);
         xw.begin("RESULT");
         for_command.process(xw);
         xw.end("RESULT");
         xw.end("LIMBAREPLY");
         mint_control.send(xw.toString());
       }
      catch (Throwable t) {
         IvyXmlWriter xw = new IvyXmlWriter();
         xw.begin("LIMBAREPLY");
         xw.field("RID",reply_id);
         xw.begin("ERROR");
         xw.textElement("MESSAGE",t);
         xw.end("ERROR");
         xw.end("LIMBAREPLY");
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
      IvyLog.logD("LIMBA","Reply for " + cmd + ": " + rslt);
      msg.replyTo(rslt);
    }
}



private final class ExitHandler implements MintHandler {
   
   @Override public void receive(MintMessage msg,MintArguments args) {
      System.exit(0);
    }

}	// end of inner class ExitHandler


}       // end of class LimbaMsg




/* end of LimbaMsg.java */

