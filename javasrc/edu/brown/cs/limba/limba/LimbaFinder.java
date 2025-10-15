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
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.w3c.dom.Element;

import edu.brown.cs.ivy.file.IvyLog;
import edu.brown.cs.ivy.jcomp.JcompMessage;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

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
private String          find_prefix;
private boolean         use_context;
// private boolean         is_remote;
private String          find_file;
private LimbaFindContext find_context;
private LimbaFindType   find_type;

private static AtomicInteger    test_counter = new AtomicInteger(0);



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
   find_prefix = IvyXml.getAttrString(xml,"PREFIX");
   find_file = IvyXml.getAttrString(xml,"FILE");
   use_context = IvyXml.getAttrBool(xml,"USECONTEXT");
// is_remote = IvyXml.getAttrBool(xml,"REMOTE");
   find_type = IvyXml.getAttrEnum(xml,"WHAT",LimbaFindType.METHOD);
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
   find_context = null;
   
   Element ctxxml = IvyXml.getChild(xml,"CONTEXT");
   if (ctxxml != null) {
      find_context = new LimbaFindContext(this,ctxxml);
    }
}


/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

LimbaMain getLimbaMain()                { return limba_main; }
LimbaFindContext getFindContext()       { return find_context; }
String getResultName()                  { return find_name; }
String getResultFile()                  { return find_file; }
LimbaFindType getFindType()             { return find_type; }

Collection<LimbaTestCase> getTestCases() 
{
   return test_cases;
}
Collection<String> getContextImports()  
{ 
   return null;
}
String getPackageName()                 { return null; }
String getSignatureClassName()          { return null; }
String getSignatureName()               { return null; }



/********************************************************************************/
/*                                                                              */
/*      Processing methods                                                      */
/*                                                                              */
/********************************************************************************/

void process(IvyXmlWriter xw) throws Exception
{
   StringBuffer addendum = new StringBuffer();
   
   for (int i = 0; i < 10; ++i) {
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
      switch (find_type) {
         case METHOD :
            pbuf.append("This code will be used as method " + find_name);
            if (find_prefix != null) {
               pbuf.append(" in class " + find_prefix);
             }
            break;
         case CLASS :
            pbuf.append("This code will be used as class " + find_name);
            if (find_prefix != null) {
               pbuf.append(" in package " + find_prefix);
             }
            break;
       }
      pbuf.append(".\n");
      if (addendum != null) {
         pbuf.append(addendum + "\n");
       }
      
      IvyLog.logD("LIMBA","Find " + pbuf.toString());
      
      String resp = limba_main.askOllama(pbuf.toString(),use_context);
      List<String> code = LimbaMain.getJavaCode(resp);
      
      List<LimbaSolution> tocheck = new ArrayList<>();
      for (String s : code) {
         try {
            // pass user context to solution so it can be used to resolve things
            LimbaSolution sol = new LimbaSolution(this,s); 
            if (sol.getAstNode() == null) {
               IvyLog.logD("LIMBA","Invalid solution -- target not found");
               // invalid solution 
               continue;
             }
            tocheck.add(sol);
          }
         catch (Throwable t) {
            IvyLog.logE("Problem parsing solution",t);
          }
       }
      
      IvyLog.logD("LIMBA","Found possible solutions: " + tocheck.size() + " " +
            code);
      
      List<LimbaSolution> rslt = new ArrayList<>();
      Set<String> allimports = new HashSet<>();
      for (LimbaTestCase ct : test_cases) {
         Collection<String> imps = ct.getImports();
         if (imps != null) allimports.addAll(imps);
       }
      
      boolean retry = false;
      for (LimbaSolution sol : tocheck) {
         List<JcompMessage> errs = sol.getCompilationErrors(); 
         if (errs != null && errs.size() > 0) {
            for (JcompMessage jm : errs) {
               IvyLog.logD("LIMBA","Handle compiler error: " + jm.getText());
               if (addendum.isEmpty()) {
                  addendum.append("Please avoid the following potential problems " +
                        " in the generated code:\n");
                }
               String err = jm.getText();
               if (!err.startsWith("Undefined ")) continue;
               if (addendum.toString().contains(jm.getText())) continue;
               addendum.append(jm.getText() + ".\n");
               retry = true;
             }
          }
       }
      if (retry) continue;
      
      for (Iterator<LimbaSolution> it = tocheck.iterator(); it.hasNext(); ) {
         LimbaSolution sol = it.next();
         sol.getImportTypes().addAll(allimports);
         if (test_cases.isEmpty()) {
            sol.setTestsPassed(true);
            rslt.add(sol);
            it.remove();
          }
         else {
            TestRunner tr = new TestRunner(sol);
            tr.start();
          } 
       }
      for (LimbaSolution sol : tocheck) {
         sol.waitForTesting();
       }
      
      for (Iterator<LimbaSolution> it = tocheck.iterator(); it.hasNext(); ) {
         LimbaSolution sol = it.next();
         if (sol.getTestsPassed()) {
            rslt.add(sol);
            it.remove();
          }
       }
      
      if (rslt.isEmpty()) {
         IvyLog.logD("No solution passed the tests -- need to retry with more information");
       }
      
      xw.begin("SOLUTIONS");
      xw.field("COUNT",rslt.size());
      
      int ct = 0;
      for (LimbaSolution sol : rslt) {
         String nm = "SOLUTION_" + (++ct);
         sol.output(xw,nm);  
       }
      
      xw.end("SOLUTIONS");
      break;
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

// private File setupContextFile(Element xml) throws LimbaException
// {
// int len = IvyXml.getAttrInt(xml,"LENGTH");
// File tdir = new File(System.getProperty("java.io.tmpdir"));
// String cnts = IvyXml.getTextElement(xml,"CONTENTS");
// String ext = IvyXml.getAttrString(xml,"EXTENSION");
// cnts = cnts.replace("\n","");
// cnts = cnts.replace("\r","");
// int pos = 0;
// try {
//    File tmp = File.createTempFile("limbadata",ext,tdir);
//    tmp.deleteOnExit();
//    try (BufferedOutputStream ots = new BufferedOutputStream(new FileOutputStream(tmp))) {
//          for ( ; pos < len; ++pos) {
//             int c0 = Character.digit(cnts.charAt(2*pos),16);
//             int c1 = Character.digit(cnts.charAt(2*pos+1),16);
//             byte b = (byte) (((c0 & 0xf) << 4) + (c1 & 0xf));
//             ots.write(b);
//           }
//        }
//    return tmp;
//  }
// catch (IOException e) {
//       throw new LimbaException("Problem creating user file",e);
//     }    
// }



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
      LimbaSuiteReport rpt = tester.runTester();
      IvyLog.logD("LIMBA","Check test result in " + rpt);
      // check if tests passed or not, save result for later queries
      for_solution.setTestsPassed(true);
    }
   
}       // end of inner class TestRunner


}       // end of class LimbaFinder




/* end of LimbaFinder.java */

