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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

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
private File            context_jar;

static AtomicInteger    test_counter = new AtomicInteger(0);



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
   context_jar = null;
   Element ctxfile = IvyXml.getChild(xml,"CONTEXTFILE");
   if (ctxfile != null) {
      try {
         context_jar = setupContextFile(ctxfile);
       }
      catch (LimbaException e) {
         IvyLog.logE("LIMBA","Problem reading jar file for context",e);
       }
    }
}


/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

LimbaMain getLimbaMain()                { return limba_main; }
File getContextJar()                    { return context_jar; }
String getResultName()                  { return find_name; }
String getResultFile()                  { return find_file; }



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
   pbuf.append("Include any auxilliary code that is needed.\n");
   pbuf.append("This code will be used as method " + find_name + ".\n");
   // might want to extract package and class and inner classes and pass separately
   
   IvyLog.logD("LIMBA","Find " + pbuf.toString());

   String resp = limba_main.askOllama(pbuf.toString());
   List<String> code = LimbaMain.getJavaCode(resp);
   
   List<LimbaSolution> tocheck = new ArrayList<>();
   for (String s : code) {
      try {
         // pass user context to solution so it can be used to resolve things
         LimbaSolution sol = new LimbaSolution(this,s); 
         tocheck.add(sol);
       }
      catch (Throwable t) {
         IvyLog.logE("Problem parsing solution",t);
       }
    }
   
   IvyLog.logD("LIMBA","Found possible solutions: " + tocheck.size() + " " +
         code);
   for (LimbaSolution sol : tocheck) {
      if (test_cases.isEmpty()) {
         sol.setTestsPassed(true); 
       }
      else {
         TestRunner tr = new TestRunner(sol);
         tr.start();
       } 
    }
   for (LimbaSolution sol : tocheck) {
      sol.waitForTesting();
    }
   // Then check the test cases
   // if a test passes, just return it
   // otherwise determine what is wrong and issue a new generate with the
   //           additional inforamtion
   // iterate this process up to k times
}




/********************************************************************************/
/*                                                                              */
/*      File access methods                                                     */
/*                                                                              */
/********************************************************************************/

private File setupContextFile(Element xml) throws LimbaException
{
   int len = IvyXml.getAttrInt(xml,"LENGTH");
   File tdir = new File(System.getProperty("java.io.tmpdir"));
   String cnts = IvyXml.getTextElement(xml,"CONTENTS");
   String ext = IvyXml.getAttrString(xml,"EXTENSION");
   cnts = cnts.replace("\n","");
   cnts = cnts.replace("\r","");
   int pos = 0;
   try {
      File tmp = File.createTempFile("limbadata",ext,tdir);
      tmp.deleteOnExit();
      try (BufferedOutputStream ots = new BufferedOutputStream(new FileOutputStream(tmp))) {
         for ( ; pos < len; ++pos) {
            int c0 = Character.digit(cnts.charAt(2*pos),16);
            int c1 = Character.digit(cnts.charAt(2*pos+1),16);
            byte b = (byte) (((c0 & 0xf) << 4) + (c1 & 0xf));
            ots.write(b);
          }
       }
      return tmp;
    }
   catch (IOException e) {
      throw new LimbaException("Problem creating user file",e);
    }    
}



/********************************************************************************/
/*                                                                              */
/*      Test Runner                                                             */
/*                                                                              */
/********************************************************************************/

private class TestRunner extends Thread {
  
   private LimbaSolution for_solution;
   
   TestRunner(LimbaSolution sol) {
      super("TestRunner_" + test_counter.getAndIncrement());
      for_solution = sol;
    }
   
   @Override public void run() {
      LimbaTester tester = new LimbaTester(LimbaFinder.this,for_solution);
      LimbaTestReport rpt = tester.runTester();
      // check if tests passed or not, save result for later queries
      for_solution.setTestsPassed(true);
    }
   
}       // end of inner class TestRunner


}       // end of class LimbaFinder




/* end of LimbaFinder.java */

