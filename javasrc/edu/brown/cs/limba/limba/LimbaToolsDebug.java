/********************************************************************************/
/*                                                                              */
/*              LimbaToolsDebug.java                                            */
/*                                                                              */
/*      Tools that just use the debugger                                        */
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
import edu.brown.cs.ivy.xml.IvyXmlWriter;

public class LimbaToolsDebug extends LimbaToolBase
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

LimbaToolsDebug(LimbaMain lm,Map<String,?> context)
{
   super(lm,null);
   query_context = context;
}


/********************************************************************************/
/*                                                                              */
/*      Access to the debugger stack                                            */
/*                                                                              */
/********************************************************************************/

@Tool("This agent returns a list of the frames on the current execution stack. " +
      "This returns a string representing a JSON array where each element " +
      "is a JSON object representing a stack frame, with the 0 element being " +
      "the current user frame and the subsequent elements being the calling " +
      "frames.  Each frame object contains the method name which includes " +
      "the class, method name and signature (key METHOD); the line number " +
      "in that method (key LINE); and a list of local variables (key LOCALS). " +
      "Each local includes its data type (key TYPE), its name (key NAME), and " +
      "its value if it is a string or a primitive (key VALUE). ")
      public String getStackFrames()
{
   limba_main.transcriptAgent("Get stack frames"); 
   
   CommandArgs args = new CommandArgs("FORMAT","JSON");
   
   Element rslt = sendToDiad("Q_STACK",args,null,query_context);
   if (rslt != null) {
      String json = IvyXml.getTextElement(rslt,"JSON");
      return json;
    }
   
   return "{ error: 'No debugid given' }";
}


@Tool("Alias for getStackFrames. This agent returns a list of the frames on the " + 
      "current execution stack. " +
      "This returns a string representing a JSON array where each element " +
      "is a JSON object representing a stack frame, with the 0 element being " +
      "the current user frame and the subsequent elements being the calling " +
      "frames.  Each frame object contains the method name which includes " +
      "the class, method name and signature (key METHOD); the line number " +
      "in that method (key LINE); and a list of local variables (key LOCALS). " +
      "Each local includes its data type (key TYPE), its name (key NAME), and " +
      "its value if it is a string or a primitive (key VALUE). ")
public String getCallStack()
{
   return getStackFrames();
}


/********************************************************************************/
/*                                                                              */
/*      Evaluation in the debugger                                              */
/*                                                                              */
/********************************************************************************/

@Tool("This agent evaluates an expression in the current frame and returns " +
       "a string represeting the JSON representation of the result.  The resultant " +
       "value is in the VALUE field of the returned object.")
public String getEvaluation(@P("Expression to evaluate") String expr)
{
   limba_main.transcriptAgent("Evaluate " + expr); 
   
   CommandArgs args = new CommandArgs("FORMAT","JSON");
   IvyXmlWriter xw = new IvyXmlWriter();
   xw.cdataElement("EXPRESSION",expr);
   String cnts = xw.toString();
   xw.close();
   
   Element rslt = sendToDiad("Q_EVAL",args,cnts,query_context);
   if (rslt != null) {
      String json = IvyXml.getTextElement(rslt,"JSON");
      return json;
    }
   
   return "{ error: 'No debugid given' }";
}




}       // end of class LimbaToolsDebug




/* end of LimbaToolsDebug.java */

