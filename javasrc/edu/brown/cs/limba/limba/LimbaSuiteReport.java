/********************************************************************************/
/*                                                                              */
/*              LimbaTestReport.java                                            */
/*                                                                              */
/*      Result of running tests on a solution                                   */
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

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Element;

import edu.brown.cs.ivy.xml.IvyXml;

class LimbaSuiteReport implements LimbaConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private LimbaFinder limba_finder;
private Map<String,TestReport> test_cases;
private String test_directory;

private static final Pattern AT_PATTERN;
private static final Pattern EXPECT_ACTUAL_PATTERN;

static {
   String pat = "at .*\\." + LIMBA_TEST_CLASS + ".*\\(" + LIMBA_TEST_CLASS + ".java:" +
        "([0-9]+)\\)";
   AT_PATTERN = Pattern.compile(pat);
   
   String p1 = "expected:<(.*)> but was:<(.*)>$";
   EXPECT_ACTUAL_PATTERN = Pattern.compile(p1);
}
 

/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

LimbaSuiteReport(LimbaFinder lf,Map<String,String> idmap)
{ 
   limba_finder = lf;
   test_directory = idmap.get("DIRECTORY");
   test_cases = new HashMap<>();
   for (LimbaTestCase tc : lf.getTestCases()) {
      test_cases.put(tc.getName(),new TestReport(tc));
    }
}

 
/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

boolean allPassed()
{
   for (TestReport tr : test_cases.values()) {
      if (!tr.getPassed()) return false;
    }
   return true;
}


void addMessages(LimbaSolution sol)
{
   for (LimbaTestCase ltc : limba_finder.getTestCases()) {
      TestReport tr = test_cases.get(ltc.getName());
      if (tr.getPassed()) continue;
      
    }
}


String getTestDirectory()
{
   return test_directory;
}


/********************************************************************************/
/*                                                                              */
/*      Setup methods                                                           */
/*                                                                              */
/********************************************************************************/

void addReport(String nm,String cnm,double time,String errmsg,boolean iserr) 
{
   TestReport tr = test_cases.get(nm);
   if (tr == null && nm.startsWith("test") && nm.length() > 4)
      tr = test_cases.get(nm.substring(5));
   if (tr == null && cnm != null)
      tr = test_cases.get(nm + "(" + cnm + ")");
   if (tr != null) tr.setReport(time,errmsg,iserr);
}


boolean addReport(LimbaSolution sol,LimbaTestCase ltc,Element te) 
{
   boolean iserr = false;
   String nm = IvyXml.getAttrString(te,"name");
   if (nm == null) return false;
   String cnm = IvyXml.getAttrString(te,"classname");
   String msg = null;
   double tm = IvyXml.getAttrDouble(te,"time");
   Element ee = IvyXml.getElementByTag(te,"error");
   Element fail = IvyXml.getElementByTag(te,"failure");
   
   if (ee != null) {
      iserr = true;
      int lno = 0;
      msg = IvyXml.getAttrString(ee,"message");
      String exc = IvyXml.getAttrString(ee,"type");
      if (exc != null) {
         String tb = IvyXml.getText(ee);
         Matcher m = AT_PATTERN.matcher(tb);
         while (m.find()) {
            lno = Integer.parseInt(m.group(1));
            lno = sol.getSolutionLine(lno);
            if (lno > 0) break;
          }
        String nmsg = "Test " + ltc.getDescription() + 
                " failed with the exception " + exc;
        if (lno > 0) nmsg += " at line " + lno;
        if (msg != null) nmsg += " due to " + msg;
        msg = nmsg;
       }
      else if (msg == null) msg = IvyXml.getText(ee);
      
    }
   else if (fail != null) {
      msg = IvyXml.getAttrString(fail,"message");
      if (msg == null) msg = IvyXml.getText(fail);
      String nmsg = getFailureMessage(ltc,msg);
      msg = nmsg;
    }
   if (msg != null) sol.addFailure(msg);
   
   addReport(nm,cnm,tm,msg,iserr);
   
   return msg == null;
}


private String getFailureMessage(LimbaTestCase ltc,String msg)
{
   String desc = ltc.getDescription();
   int idx1 = desc.indexOf(" should yield ");
   if (idx1 > 0) {
      Matcher m1 = EXPECT_ACTUAL_PATTERN.matcher(msg);
      if (m1.find()) {
         String exp = m1.group(1);
         String act = m1.group(2);
         String d1 = desc.substring(0,idx1);
         return d1 + " should yield  <" + exp +
               ">  but returned <" + act + ">";
       }
    }
   
   return "Test " + desc + " produced the wrong result: " + msg;
}


/********************************************************************************/
/*                                                                              */
/*      Information for a single test                                           */
/*                                                                              */
/********************************************************************************/

private static class TestReport {
   
   private boolean test_passed;
   
   TestReport(LimbaTestCase tc) {
      test_passed = false;
    }
   
   boolean getPassed()                  { return test_passed; }
   
   void setReport(double time,String errmsg,boolean iserr) {
      if (errmsg != null && errmsg.startsWith("Throws java.lang.AssertionError: ")) {
         int idx0 = errmsg.indexOf(":");
         errmsg = errmsg.substring(idx0+2);
       }
      
      if (errmsg == null) test_passed = true;
      else {
         test_passed = false;
       }
    }
   
}       // end of subclass TestReport



}       // end of class LimbaTestReport




/* end of LimbaTestReport.java */

