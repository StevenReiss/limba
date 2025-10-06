/********************************************************************************/
/*                                                                              */
/*              LimbaRelay.java                                                 */
/*                                                                              */
/*      Relay to forward ollama requests                                        */
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
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;


public final class LimbaRelay
{

/********************************************************************************/
/*                                                                              */
/*      Main program                                                            */
/*                                                                              */
/********************************************************************************/

public static void main(String [] args)
{
   LimbaRelay relay = new LimbaRelay(args);
   
   relay.process();
}



/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private String  host_name;
private int     host_port;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

private LimbaRelay(String [] args)
{
   host_name = "fred4.cs.brown.edu";
   host_port = 11434;
   
   scanArgs(args);
}


/********************************************************************************/
/*                                                                              */
/*      Argument scanning                                                       */
/*                                                                              */
/********************************************************************************/

private void scanArgs(String [] args)
{
   for (int i = 0; i < args.length; ++i) {
      if (i+1 < args.length) {
         if (args[i].startsWith("-h")) {
            host_name = args[++i];
          }
         else if (args[i].startsWith("-p")) {
            try {
               host_port = Integer.parseInt(args[++i]);
             }
            catch (NumberFormatException e) {
               badArgs();
             }
          }
         else badArgs();
       }
      else badArgs();
    }
   if (host_name == null) badArgs();
}


private void badArgs()
{
   System.err.println("LimbaRelay -h <host> [-p <port>]");
   System.exit(1);
}



/********************************************************************************/
/*                                                                              */
/*      Processing methods                                                      */
/*                                                                              */
/********************************************************************************/

@SuppressWarnings("resource")
private void process()
{
   ServerSocket server = null;
   try {
      server = new ServerSocket(host_port);
      System.err.println("LIMBARELAY: Relay created on port " + host_port);
    }
   catch (IOException e) {
      System.err.println("LIMBARELAY: Failed to create server socket: " + e);
    }
   
   for ( ; ; ) {
      try {
         Socket client = server.accept();
         System.err.println("LIMBARELAY: new client connection " +
               client.getRemoteSocketAddress());
         Socket relay = new Socket(host_name,host_port);
         System.err.println("LIMBARELAY: Connected to relay host " + host_name + 
               " " + host_port);
         InputStream clientin = client.getInputStream();
         OutputStream clientout = client.getOutputStream();
         InputStream relayin = relay.getInputStream();
         OutputStream relayout = relay.getOutputStream();
         RelayThread rt1 = new RelayThread(clientin,relayout);
         RelayThread rt2 = new RelayThread(relayin,clientout);
         rt1.start();
         rt2.start();
       }
      catch (IOException e) {
         System.err.println("LIMBARELAY: Problem setting up client: " + e);
       }
    }
}



/********************************************************************************/
/*                                                                              */
/*      Socket Relay                                                            */
/*                                                                              */
/********************************************************************************/

private static class RelayThread extends Thread {
   
   private InputStream in_stream;
   private OutputStream out_stream;
   
   RelayThread(InputStream ins,OutputStream ots) {
      in_stream = ins;
      out_stream = ots;
    }
   
   @Override public void run() {
      try {
         byte [] buffer = new byte[10240];
         int read = 0;
         for ( ; ; ) {
            read = in_stream.read(buffer);
            System.err.println("LIMBARELAY: Read " + read + " bytes");
            if (read < 0) break;
            out_stream.write(buffer,0,read);
            System.err.println("LIMBAREALY: Relayed " + read + " btyes");
          }
       }
      catch (IOException e) {
         System.err.println("LIMBARELAY: Error forwarding data: " + e);
       }
    }
   
}       // end of inner class RelayThread

}       // end of class LimbaRelay




/* end of LimbaRelay.java */

