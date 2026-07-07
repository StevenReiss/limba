/********************************************************************************/
/*                                                                              */
/*              LimbaToolsStack.java                                            */
/*                                                                              */
/*      Tools for stack-trace based stack access                                */
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

import dev.langchain4j.agent.tool.Tool;
import edu.brown.cs.ivy.mint.MintConstants.CommandArgs;
import edu.brown.cs.ivy.xml.IvyXml;

public class LimbaToolsStack extends LimbaToolBase
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


LimbaToolsStack(LimbaMain lm,Map<String,?> context)
{
   super(lm,null);
   query_context = context;
}


/********************************************************************************/
/*                                                                              */
/*      Return stack frames without local variables                             */
/*                                                                              */
/********************************************************************************/

@Tool("This agent returns a list of the frames on the current execution stack. " +
      "This returns a string representing a JSON array where each element " +
      "is a JSON object representing a stack frame, with the 0 element being " +
      "the current user frame and the subsequent elements being the calling " +
      "frames.  Each frame object contains the method name which includes " +
      "the class, method name and signature (key METHOD); and the line number " +
      "in that method (key LINE).")
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
      "the class, method name and signature (key METHOD); and the line number " +
      "in that method (key LINE). ")
public String getCallStack()
{
   return getStackFrames();
}




}       // end of class LimbaToolsStack




/* end of LimbaToolsStack.java */

