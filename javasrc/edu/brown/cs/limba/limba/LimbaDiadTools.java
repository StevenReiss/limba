/********************************************************************************/
/*                                                                              */
/*              LimbaDiadTools.java                                             */
/*                                                                              */
/*      Debugging assistant tools                                               */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2025 Brown University -- Steven P. Reiss                    */
/*********************************************************************************
 *  Copyright 2025, Brown University, Providence, RI.                            *
 *                                                                               *
 *                        All Rights Reserved                                    *
 *                                                                               *
 *  Permission to use, copy, modify, and distribute this software and its        *
 *  documentation for any purpose other than its incorporation into a            *
 *  commercial product is hereby granted without fee, provided that the          *
 *  above copyright notice appear in all copies and that both that               *
 *  copyright notice and this permission notice appear in supporting             *
 *  documentation, and that the name of Brown University not be used in          *
 *  advertising or publicity pertaining to distribution of the software          *
 *  without specific, written prior permission.                                  *
 *                                                                               *
 *  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS                *
 *  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND            *
 *  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY      *
 *  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY          *
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,              *
 *  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS               *
 *  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE          *
 *  OF THIS SOFTWARE.                                                            *
 *                                                                               *
 ********************************************************************************/



package edu.brown.cs.limba.limba;

import java.util.Map;

import org.w3c.dom.Element;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import edu.brown.cs.ivy.file.IvyLog;
import edu.brown.cs.ivy.mint.MintControl;
import edu.brown.cs.ivy.mint.MintDefaultReply;
import edu.brown.cs.ivy.mint.MintConstants.CommandArgs;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

public class LimbaDiadTools implements LimbaConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private LimbaMain       limba_main;
private MintControl     mint_control;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

LimbaDiadTools(LimbaMain lm)
{
   limba_main = lm;
   mint_control = lm.getMintControl();
}


/********************************************************************************/
/*                                                                              */
/*      Access to the debugger stack                                            */
/*                                                                              */
/********************************************************************************/

@Tool("Return a list of the frames on the current execution stack. " +
      "This returns a string representing a JSON array where each element " +
      "is a JSON object representing a stack frame, with the 0 element being " +
      "the current user frame and the subsequent elements being the calling " +
      "frames.  Each frame object contains the class, method, line number, " +
      "full method name, method signature, and a JSON array of local variables " +
      "given as JSON objects with the variable name, its data type, and its value.")
public String getStackFrames()
{
   CommandArgs args = new CommandArgs("FORMAT","JSON");
   Element rslt = sendToDiad("Q_STACK",args,null);
   if (rslt != null) {
      String json = IvyXml.getTextElement(rslt,"JSON");
      return json;
    }
   
   return null;
}



/********************************************************************************/
/*                                                                              */
/*      Access to initial fault localization                                   */
/*                                                                              */
/********************************************************************************/

@Tool("Return a list of locations that can affect the problematic symptom and " +
      "thus might be faulty and that are executed. This is returned as a string " +
      "representing a JSON array where " +
      "each element represents a method with its full name and then a JSON array of line " +
      "numbers for the identified lines in that method.")
public String getFaultLocations()
{
   CommandArgs args = new CommandArgs("FORMAT","JSON","ALL",false);
   Element rslt = sendToDiad("Q_LOCATIONS",args,null);
   if (rslt != null) {
      String json = IvyXml.getTextElement(rslt,"JSON");
      return json;
    }
   
   return null;
}


@Tool("Return a list of locations that can affect the problematic symptom and " +
      "thus might be faulty even if not executed. This is returned as a string " +
      "representing a JSON array where " +
      "each element represents a method with its full name and then a JSON array of line " +
      "numbers for the identified lines in that method.")
public String getAllFaultLocations()
{
   CommandArgs args = new CommandArgs("FORMAT","JSON","ALL",true);
   Element rslt = sendToDiad("Q_LOCATIONS",args,null);
   if (rslt != null) {
      String json = IvyXml.getTextElement(rslt,"JSON");
      return json;
    }
   
   return null;
}



@Tool("Return the call tree of the execution leading to the problematic symptom. " +
      "This returns a string representing a JSONObject containing the top level call. " + 
      "Each call object contains " +
      "the method, and ID, the start and end times, and a list of call objects called by " +
      "this method.  The IDs can be used to get details of the exeuction of this call " +
      "including line numbers and variable values")
public String getExecutionTrace()
{
   CommandArgs args = new CommandArgs("FORMAT","JSON");
   Element rslt = sendToDiad("Q_EXECTRACE",args,null);
   if (rslt != null) {
      String json = IvyXml.getTextElement(rslt,"JSON");
      return json;
    }
   
   return null;
}


@Tool("Return the sequence of line numbers executed in a particular call frame " +
      "along with their times.  This returns a string representing a JSONArray that " +
      "contains a JSONObject for each line along with the time stamps for that line.")
public String getLineNumberTrace(
      @P("ID of the particular call (from getExecutionTrace)") String callid)
{
   CommandArgs args = new CommandArgs("FORMAT","JSON",
         "CALLID",callid);
   Element rslt = sendToDiad("Q_LINETRACE",args,null);
   if (rslt != null) {
      String json = IvyXml.getTextElement(rslt,"JSON");
      return json;
    }
   
   return null;
}
     

@Tool("Return the history of a variable during the execution of a particular " +
"method or call.  This takes the call id of the call frame as well as the name " +
"of the variable in question.  It returns a string representing a JSON Object " +
"that gives information about the variable as well as all value changes.")
public String getVariableTrace(
      @P("ID of the particular call (from getExecutionTrace)") String callid,
      @P("Name of the variable") String variable) 
{
   CommandArgs args = new CommandArgs("FORMAT","JSON",
         "CALLID",callid,"VARIABLE",variable);
   Element rslt = sendToDiad("Q_VARTRACE",args,null);
   if (rslt != null) {
      String json = IvyXml.getTextElement(rslt,"JSON");
      return json;
    }
   
   return null;
}



@Tool("Return the value returned by the given call as a string representating a " +
      "JSON Object")
public String getReturnValue(@P("ID of the particular call (from getExecutionTrace)") String callid)
{
   CommandArgs args = new CommandArgs("FORMAT","JSON",
         "CALLID",callid,"VARIABLE","*RETURNS*");
   Element rslt = sendToDiad("Q_VARTRACE",args,null);
   if (rslt != null) {
      String json = IvyXml.getTextElement(rslt,"JSON");
      return json;
    }
   
   return null;
}



@Tool("Return the value of a given variable at a given time.  The time is based on " +
      "the execution trace.  A local variable is given by its name; a field is specified " +
      "by name?field_name; an array element is specified by name?[index].  The time can be " +
      "given either by the execution trace time or the line number (or both).  The returned " +
      "value is a string representing a JSONObject containing the VALUE at the time.")
public String getVariableValue(
      @P("ID of the particular call (from getExecutionTrace)") String callid,
      @P("Name of the variable, using ? for subelements") String variable,
      @P("Optional line number use 0 if not known") int line,
      @P("Optional execution time; use -1 if not known") long time)
{
   CommandArgs args = new CommandArgs("FORMAT","JSON",
         "CALLID",callid,"VARIABLE",variable);
   if (line > 0) args.put("LINE",line);
   if (time >= 0) args.put("TIME",time);
   Element rslt = sendToDiad("Q_VARVALUE",args,null);
   if (rslt != null) {
      String json = IvyXml.getTextElement(rslt,"JSON");
      return json;
    }
   
   return null;
}    


@Tool("Return a graph showing how a variable got its value at a given time.  The time " +
      "is based on the execution trace. A local variable is given by its name; a " +
      "field is specified " +
      "by name?field_name; an array element is specified by name?[index].  The time can be " +
      "given either by the execution trace time or the line number (or both).  The " +
      "returned value is a sgtring representing a JSONObject which represents a graph " +
      "with nodes representing change points and arcs showing the temporal relation.")
public String getVariableHistory(
            @P("ID of the particular call (from getExecutionTrace)") String callid,
            @P("Name of the variable, using ? for subelements") String variable,
            @P("Optional line number use 0 if not known") int line,
            @P("Optional execution time; use -1 if not known") long time)
{
   CommandArgs args = new CommandArgs("FORMAT","JSON",
         "CALLID",callid,"VARIABLE",variable);
   if (line > 0) args.put("LINE",line);
   if (time >= 0) args.put("TIME",time);
   Element rslt = sendToDiad("Q_VARHISTORY",args,null);
   if (rslt != null) {
      String json = IvyXml.getTextElement(rslt,"JSON");
      return json;
    }
   
   return null;
}    
/********************************************************************************/
/*                                                                              */
/*      Send reqeust to diad                                                    */
/*                                                                              */
/********************************************************************************/

private Element sendToDiad(final String what,final CommandArgs args0,String cnts)
{
   CommandArgs args = args0;
   MintDefaultReply rply = new MintDefaultReply();
   
   Map<String,?> ctx = limba_main.getQueryContext().get();
   if (ctx != null) {
      for (Map.Entry<String,?> ent : ctx.entrySet()) {
         String key = ent.getKey();
         if (args == null) args = new CommandArgs();
         else if (args.containsKey(key)) ;
         else args.put(key,ent.getValue());
       }
    }
   
   IvyXmlWriter xw = new IvyXmlWriter();
   xw.begin("DIAD");
   xw.field("DO",what);
   if (args != null) {
      for (Map.Entry<String,?> ent : args.entrySet()) {
         xw.field(ent.getKey(),ent.getValue());
       }
    }
   if (cnts != null) {
      xw.xmlText(cnts);
    }
   xw.end("DIAD");
   String msg = xw.toString();
   xw.close();
   
   IvyLog.logD("LIMBA","Send to DIAD: " + msg);
   
   mint_control.send(msg,rply,MintControl.MINT_MSG_FIRST_NON_NULL);
   
   Element rslt = rply.waitForXml(0);
   
   IvyLog.logD("DICONTROL","Reply from DIAD: " + IvyXml.convertXmlToString(rslt));
   
   return rslt;
}


}       // end of class LimbaDiadTools




/* end of LimbaDiadTools.java */

