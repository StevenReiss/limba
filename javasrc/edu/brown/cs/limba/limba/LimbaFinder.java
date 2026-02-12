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
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.w3c.dom.Element;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
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
   find_context = new LimbaFindContext(this,ctxxml);
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

LimbaTestCase findTestCase(String nm)
{
   if (nm == null) return null;
   
   for (LimbaTestCase ltc : test_cases) {
      if (nm.equals(ltc.getName()) ||
            nm.equals(ltc.getJunitName()) ||
            nm.equals("test_" + ltc.getName())) {
         return ltc;
       }
    }
   return null;
}


/********************************************************************************/
/*                                                                              */
/*      Processing methods                                                      */
/*                                                                              */
/********************************************************************************/

void process(IvyXmlWriter xw) throws Exception
{
   ChatMemory history = MessageWindowChatMemory.builder()
      .maxMessages(10)
      .build();
   
   PriorityQueue<LimbaSolution> finalset = new PriorityQueue<>();
   List<LimbaSolution> rslt = new ArrayList<>();
   
   Set<String> undefs = new HashSet<>();
   String addendum = null;
   String testerrs = null;
   boolean again = false;
   for (int i = 0; i < 10; ++i) {
      StringBuffer pbuf = new StringBuffer();
      if (base_prompt != null) pbuf.append(base_prompt);
      if (!undefs.isEmpty()) {
         pbuf.append("\nNote the following are not defined in Java:\n");
         for (String s : undefs) {
            pbuf.append(" ");
            pbuf.append(s);
            pbuf.append(",");
          }
         pbuf.append("\n");
         pbuf.append("Do not use any of these in your solution.\n");
       }
      if (testerrs != null) {
         pbuf.append(testerrs);
       }
      if (again) {
         pbuf.append("Pleasse provide the complete solution, not a correction.\n"); 
         pbuf.append("Recall the problem you are supposed to solve:\n");
       }
      pbuf.append("Please generate a ");
      switch (find_type) {
         case METHOD :
            pbuf.append("method");
            break;
         case CLASS :
            pbuf.append("class");
            break;
       }
      pbuf.append(" with the signature\n");
      pbuf.append("'" + find_signature + "'\n");
      pbuf.append("that does the following: \n");
      pbuf.append(find_description);
      pbuf.append("\n");
      pbuf.append("Generate 3 separate and complete alternative versions of the code.\n");
      pbuf.append("Include explicit import statements in the code as needed.\n");
      pbuf.append("Include any auxilliary code that is needed.\n");
      pbuf.append("Be sure to handle exceptions correctly. Avoid unreachable statements.\n");
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
         pbuf.append(addendum + ".\n");
       }
      
      String resp = limba_main.askOllama(pbuf.toString(),
            use_context,history,
            EnumSet.of(LimbaToolSet.PROJECT,LimbaToolSet.STRUCTURE),null);
      List<String> code = LimbaMain.getJavaCode(resp);
      
      List<LimbaSolution> tocheck = getSolutions(code);
      
      IvyLog.logD("LIMBA","Found possible solutions: " + tocheck.size() + " " +
            code);
      
      Set<String> allimports = new HashSet<>();
      for (LimbaTestCase ct : test_cases) {
         Collection<String> imps = ct.getImports();
         if (imps != null) allimports.addAll(imps);
       }
      
      String newadd = checkCompilation(undefs,tocheck,(i < 4));
      if (newadd != null && !newadd.isEmpty()) addendum = newadd;
      else addendum = null;
      if (newadd != null) {
         again = true;
         continue;
       }
      
      runTests(tocheck,allimports,rslt);
      
      for (LimbaSolution sol : tocheck) {
         sol.waitForTesting();
       }
      
      for (Iterator<LimbaSolution> it = tocheck.iterator(); it.hasNext(); ) {
         LimbaSolution sol = it.next();
         if (sol.getTestsPassed()) {
            sol.setScore(10);
            rslt.add(sol);
            it.remove();
          }
         else if (sol.getScore() >= 0.5) {
            finalset.add(sol);
          }
       }
      
      if (!rslt.isEmpty()) break;
      
      IvyLog.logD("None of these solution passed the tests -- " + 
           "need to retry with more information");
      testerrs = getTestCorrections(tocheck);
      again = true;
    }
      
   int ct0 = rslt.size();
   if (ct0 == 0) {
      while (rslt.size() < 3 && finalset.peek() != null) {
         rslt.add(finalset.remove());
       }
    }
   
   xw.begin("SOLUTIONS");
   xw.field("ALLPASSED",ct0);
   xw.field("TESTCOUNT",test_cases.size());
   xw.field("COUNT",rslt.size());
   for (LimbaSolution sol : rslt) {
      sol.output(xw);  
    }
   xw.end("SOLUTIONS");
}



private List<LimbaSolution> getSolutions(List<String> code)
{
   List<LimbaSolution> tocheck = new ArrayList<>();
   int ct = 1;
   for (String s : code) {
      String name = "Solution " + ct++;
      try {
         // pass user context to solution so it can be used to resolve things
         LimbaSolution sol = new LimbaSolution(this,name,s); 
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
   
   return tocheck;
}



private void runTests(List<LimbaSolution> tocheck,Set<String> allimports,List<LimbaSolution> pass)
{
   for (Iterator<LimbaSolution> it = tocheck.iterator(); it.hasNext(); ) {
      LimbaSolution sol = it.next();
      sol.getImportTypes().addAll(allimports);
      if (test_cases.isEmpty()) {
         sol.setTestsPassed(true);
         pass.add(sol);
         it.remove();
       }
      else {
         TestRunner tr = new TestRunner(sol);
         tr.start();
       } 
    }
}


private String checkCompilation(Set<String> undefs,List<LimbaSolution> tocheck,boolean recheck)
{
   boolean retry = false;
   Set<String> priorundef = new HashSet<>(undefs);
   String addendum = null;
   for (LimbaSolution sol : tocheck) {
      List<JcompMessage> errs = sol.getCompilationErrors(); 
      if (errs != null && errs.size() > 0) {
         for (JcompMessage jm : errs) {
            String cnts = sol.getFullText();
            int s0 = jm.getStartOffset();
            s0 = Math.max(0,s0-5);
            int s1 = jm.getEndOffset();
            s1 = Math.min(s1+5,cnts.length());
            String str = cnts.substring(s0,s1);
            IvyLog.logD("LIMBA","Handle compiler error: " + 
                  jm.getLineNumber() + " @ " + str + " : " +
                  jm.getText());
            String err = jm.getText();
            if (err.startsWith("Undefined ")) {
               int idx = err.lastIndexOf(" ");
               String undef = err.substring(idx+1).trim();
               if (undefs.add(undef)) {
                  retry = true;
                }
               else if (priorundef.contains(undef) && recheck) {
                  retry = true;
                }
             }
            else {
               if (addendum == null) {
                  addendum = "Please avoid syntax errors.";
                  retry = true;
                }
             }
          }
       }
    }
   if (retry && addendum == null) addendum = "";
   return addendum;
}


private String getTestCorrections(List<LimbaSolution> tocheck)
{
   StringBuffer ebuf = new StringBuffer();
   for (LimbaSolution sol : tocheck) {
      List<String> fails = sol.getFailures();
      if (fails == null || fails.isEmpty()) continue;
      if (ebuf.isEmpty()) {
         ebuf.append("These solutions do not work.  In particular: ");
       }
      ebuf.append("For solution " + sol.getName() + ":\n");
      for (String msg : sol.getFailures()) {
         ebuf.append("*  " + msg + ";\n");
       }
    }
   if (ebuf.isEmpty()) return null;
   ebuf.append("Please try again taking this information into account.\n");
   return ebuf.toString();
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
      if (rpt == null) {
         IvyLog.logD("LIMBA","No suite report for " + for_solution.getName());
         for_solution.setTestsPassed(false);
         // add problem indication here
         return;
       }
      IvyLog.logD("LIMBA","Check test result for " + for_solution.getName() +
            " in " + rpt.getTestDirectory()); 
      
      // check if tests passed or not, save result for later queries
      if (rpt.allPassed()) {
         for_solution.setTestsPassed(true);
       }
      else {
         for_solution.setTestsPassed(false);
       }
      // add text to the solution indicating what test(s) failed so we can
      // ask the LLM another time.
    }
   
}       // end of inner class TestRunner


}       // end of class LimbaFinder




/* end of LimbaFinder.java */

