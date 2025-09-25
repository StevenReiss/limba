/********************************************************************************/
/*                                                                              */
/*              LimbaFinder.java                                                */
/*                                                                              */
/*      Search and test solutions from the LLM                                  */
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

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

import edu.brown.cs.ivy.file.IvyLog;
import edu.brown.cs.ivy.xml.IvyXml;

class LimbaFinder implements LimbaConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private LimbaMain       limba_main;
private String          base_prompt;
private List<LimbaTestCase> test_cases;
private String          find_description;
private String          find_signature;
private String          find_name;
private boolean         use_context;
private String          find_file;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

LimbaFinder(LimbaMain lm,String prompt,Element xml)
{
   limba_main = lm;
   base_prompt = prompt;
   Element sxml = IvyXml.getChild(xml,"SEARCH");
   if (sxml != null) xml = sxml;
   find_description = IvyXml.getTextElement(xml,"DESCRIPTION");
   find_signature = IvyXml.getTextElement(xml,"SIGNATURE");
   find_name = IvyXml.getAttrString(xml,"NAME");
   find_file = IvyXml.getAttrString(xml,"FILE");
   use_context = IvyXml.getAttrBool(xml,"USECONTEXT");
   test_cases = new ArrayList<>();
   Element testsxml = IvyXml.getChild(xml,"TESTS");  
   for (Element test : IvyXml.children(testsxml,"TESTCASE")) {
      try {
         LimbaTestCase ltc = LimbaTestCase.createTestCase(test);  
         if (ltc != null) test_cases.add(ltc);
       }
      catch (LimbaException e) {
         IvyLog.logE("LIMBA","Problem reading test case",e);
       }
    }
}

/********************************************************************************/
/*                                                                              */
/*      Processing methods                                                      */
/*                                                                              */
/********************************************************************************/

void process() throws Exception
{
   // create a JcompContext based on user context
   
   StringBuffer pbuf = new StringBuffer();
   if (base_prompt != null) pbuf.append(base_prompt);
   pbuf.append("\nPlease generate a method with the signature\n");
   pbuf.append("'" + find_signature + "'\n");
   pbuf.append("that does the following: \n");
   pbuf.append(find_description);
   pbuf.append("\n");
   pbuf.append("Generate 3 alternative versions of the code.\n");
   pbuf.append("Include explicit import statements in the code as needed.\n");
   pbuf.append("Include any auxilliary code that is needed.");
   
   IvyLog.logD("LIMBA","Find " + pbuf.toString());

   String resp = limba_main.askOllama(pbuf.toString());
   List<String> code = LimbaMain.getJavaCode(resp);
   
   List<LimbaSolution> tocheck = new ArrayList<>();
   for (String s : code) {
      try {
         // pass user context to solution so it can be used to resolve things
         LimbaSolution sol = new LimbaSolution(s);
         tocheck.add(sol);
       }
      catch (Throwable t) {
         IvyLog.logE("Problem parsing solution",t);
       }
    }
   
   IvyLog.logD("LIMBA","Found possible solutions: " + tocheck.size() + " " +
         code);
   
   // Then check the test cases
   // if a test passes, just return it
   // otherwise determine what is wrong and issue a new generate with the
   //           additional inforamtion
   // iterate this process up to k times
}



}       // end of class LimbaFinder




/* end of LimbaFinder.java */

