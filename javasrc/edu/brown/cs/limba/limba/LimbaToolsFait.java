/********************************************************************************/
/*                                                                              */
/*              LimbaToolsFait.java                                             */
/*                                                                              */
/*      description of class                                                    */
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

public class LimbaToolsFait extends LimbaToolBase
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

LimbaToolsFait(LimbaMain lm,Map<String,?> context)
{
   super(lm,null);
   query_context = context;
}





/********************************************************************************/
/*                                                                              */
/*      Access to initial fault localization                                    */
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


}       // end of class LimbaToolsFait



/* end of LimbaToolsFait.java */

