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
import edu.brown.cs.ivy.mint.MintConstants.CommandArgs;
import edu.brown.cs.ivy.xml.IvyXml;

public class LimbaToolsDiad extends LimbaToolBase
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private Map<String,?>   query_context;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

LimbaToolsDiad(LimbaMain lm,Map<String,?> context)
{
   super(lm,null);
   query_context = context;
}



/********************************************************************************/
/*                                                                              */
/*      Access to initial fault localization                                   */
/*                                                                              */
/********************************************************************************/

@Tool("This agent returns a list of locations that can affect the problematic symptom and " +
      "thus might be faulty and that are executed. This is returned as a string " +
      "representing a JSON array where " +
      "each element represents a method with its full name (key METHOD) " +
      "and an array of lines in the method that might " +
      "be problematic (key LINES).  The source code for these lines can be " +
      "found using the tool getSourceCode")
public String getFaultLocations()
{
   limba_main.transcriptAgent("Get fault locations"); 
   
   CommandArgs args = new CommandArgs("FORMAT","JSON","ALL",false);
   Element rslt = sendToDiad("Q_LOCATIONS",args,null,query_context);
   if (rslt != null) {
      String json = IvyXml.getTextElement(rslt,"JSON");
      return json;
    }

   return "{ error: 'No debugid given' }";
}


@Tool("This agent returns a list of locations that can affect the problematic symptom and " +
      "thus might be faulty even if not executed. This is returned as a string " +
      "representing a JSON array where " +
      "each element represents a method with its full name and then a JSON array of line " +
      "numbers for the identified lines in that method.")
public String getAllFaultLocations()
{
   limba_main.transcriptAgent("Get all fault locations"); 
   
   CommandArgs args = new CommandArgs("FORMAT","JSON","ALL",true);
   Element rslt = sendToDiad("Q_LOCATIONS",args,null,query_context);
   if (rslt != null) {
      String json = IvyXml.getTextElement(rslt,"JSON");
      return json;
    }

   return "{ error: 'No debugid given' }";
}




@Tool("This agent returns information about one call in the execution trace leading to a problem. " +
"The call is identified by the callid parameter which can be 0 to indicate the " +
"top-level call.  The tool returns a string representing a JSON object containing " +
"the context id of the call (key ID), the full name of the method being executed " +
"(key METHOD), the start time of the method execution (key START_TIME), the end " +
"time of the method execution (key END_TIME), and a JSON array of the methods called " +
"directly from this method.  Each call contains its callid (key ID), its full method " +
"name (key METHOD), and its start and end time (keys START_TIME and END_TIME). " +
"It can also include a key EXCEPTION which indicates the call exited by throwing " +
"an exception; if this is not present the call returned normally. Additional " +
"information about that call can be obtained by using this tool again or by using the " +
"getLineNubmerTrace or getVariableTrace tools.")
public String getCallTrace(String callid)
{
   limba_main.transcriptAgent("Get call trace for " + callid); 
   
   CommandArgs args = new CommandArgs("FORMAT","JSON",
         "CALLID",callid);
   Element rslt = sendToDiad("Q_EXECTRACE",args,null,query_context);
   if (rslt != null) {
      String json = IvyXml.getTextElement(rslt,"JSON");
      return json;
    }

   return "{ error: 'No debugid given' }";
}


@Tool("This agent returns the sequence of line numbers executed in a particular call frame " +
      "along with their times.  This returns a string representing a JSONArray that " +
      "contains a JSONObject for each line along with the time stamps for that line.")
public String getLineNumberTrace(
      @P("ID of the particular call (from getCallTrace)") String callid)
{
   limba_main.transcriptAgent("Get line number trace for " + callid); 
   
   CommandArgs args = new CommandArgs("FORMAT","JSON",
         "CALLID",callid);
   Element rslt = sendToDiad("Q_LINETRACE",args,null,query_context);
   if (rslt != null) {
      String json = IvyXml.getTextElement(rslt,"JSON");
      return json;
    }

   return "{ error: 'No debugid given' }";
}


@Tool("This agent returns the history of a variable during the execution of a particular " +
      "method or call.  This takes the call id of the call frame as well as the name " +
      "of the variable in question.  This should be a variable, not an expression. " +
      "It returns a string representing a JSON Object " +
      "that gives information about the variable as well as all value changes.")
public String getVariableTrace(
      @P("ID of the particular call (from getCallTrace)") String callid,
      @P("Name of the variable") String variable)
{
   limba_main.transcriptAgent("Get variable trace for " + callid + " " + variable); 
   
   CommandArgs args = new CommandArgs("FORMAT","JSON",
         "CALLID",callid,"VARIABLE",variable);
   Element rslt = sendToDiad("Q_VARTRACE",args,null,query_context);
   if (rslt != null) {
      String json = IvyXml.getTextElement(rslt,"JSON");
      return json;
    }

   return "{ error: 'No debugid given' }";
}



@Tool("This agent returns the value returned by the given call as a string representating a " +
      "JSON Object")
public String getReturnValue(@P("ID of the particular call (from getCallTrace)") String callid)
{
   limba_main.transcriptAgent("Get return value for " + callid); 
   
   CommandArgs args = new CommandArgs("FORMAT","JSON",
         "CALLID",callid,"VARIABLE","*RETURNS*");
   Element rslt = sendToDiad("Q_VARTRACE",args,null,query_context);
   if (rslt != null) {
      String json = IvyXml.getTextElement(rslt,"JSON");
      return json;
    }

   return "{ error: 'No debugid given' }";
}



@Tool("This agent returns the value of a given variable at a given time.  The time is based on " +
      "the execution trace.  A local variable is given by its name; a field is specified " +
      "by name?field_name; an array element is specified by name?[index].  It cannot " +
      "be an expression such as a call or a field reference.  The time can be " +
      "given either by the execution trace time or the line number (or both).  The returned " +
      "value is a string representing a JSONObject containing the VALUE at the time. ")
public String getVariableValue(
      @P("Numeric ID of the particular call (from getCallTrace)") String callid,
      @P("Name of the variable, using ? for subelements") String variable,
      @P("Optional line number use 0 if not known") int line,
      @P("Optional execution time; use -1 if not known") long time)
{
   limba_main.transcriptAgent("Get variable value for " + callid + " " + variable + " " + 
         line + " " + time); 
   
   if (variable.contains("(")) {
      return "{ 'ERROR': 'Expression given; only variables allowed' }";
    }

   CommandArgs args = new CommandArgs("FORMAT","JSON",
         "CALLID",callid,"VARIABLE",variable);
   if (line > 0) args.put("LINE",line);
   if (time >= 0) args.put("WHEN",time);
   Element rslt = sendToDiad("Q_VARVALUE",args,null,query_context);
   if (rslt != null) {
      String json = IvyXml.getTextElement(rslt,"JSON");
      return json;
    }

   return "{ 'ERROR': 'No debugid given' }";
}


@Tool("This agent returns a graph showing how a variable got its value at a given time.  The time " +
      "is based on the execution trace. A local variable is given by its name; a " +
      "field is specified " +
      "by name?field_name; an array element is specified by name?[index].  The time can be " +
      "given either by the execution trace time or the line number (or both).  The " +
      "returned value is a sgtring representing a JSONObject which represents a graph " +
      "with nodes representing change points and arcs showing the temporal relation.")
public String getVariableHistory(
            @P("ID of the particular call (from getCallTrace)") String callid,
            @P("Name of the variable, using ? for subelements") String variable,
            @P("Optional line number use 0 if not known") int line,
            @P("Optional execution time; use -1 if not known") long time)
{
   limba_main.transcriptAgent("Get variable history for " + callid + " " + variable +
         " " + line + " " + time); 
   
   CommandArgs args = new CommandArgs("FORMAT","JSON",
         "CALLID",callid,"VARIABLE",variable);
   if (line > 0) args.put("LINE",line);
   if (time >= 0) args.put("WHEN",time);
   Element rslt = sendToDiad("Q_VARHISTORY",args,null,query_context);
   if (rslt != null) {
      String json = IvyXml.getTextElement(rslt,"JSON");
      return json;
    }

   return "{ error: 'No debugid given' }";
}



@Tool("This agent returns a stromg representation of a JSONArray of call for a particular " +
      "method.  Each element of the JSONArray is a JSONObject containing the " +
      "CALLID and the START and END times of the invocation.  If the method is never " +
      "called, the array is empty.  Otherwise callid inforamtion for all instances " +
      "of the particular method are returned.")
public String getCallIdsForMethod(
      @P("Name of the method") String method0)
{
   String method = normalizeMethodName(method0); 
   
   limba_main.transcriptAgent("Get callids for method for " + method); 

   CommandArgs args = new CommandArgs("FORMAT","JSON",
         "METHOD",method);
   
   Element rslt = sendToDiad("Q_METHODCALLS",args,null,query_context);
   if (rslt != null) {
      String json = IvyXml.getTextElement(rslt,"JSON");
      return json;
    }
   
   return "[ ]";
}     


}       // end of class LimbaDiadTools




/* end of LimbaDiadTools.java */

