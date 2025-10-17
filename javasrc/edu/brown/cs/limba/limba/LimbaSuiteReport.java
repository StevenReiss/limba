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

class LimbaSuiteReport implements LimbaConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private Map<String,TestReport> test_cases;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

LimbaSuiteReport(LimbaFinder lf,Map<String,String> idmap)
{ 
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


public double getTime(String test)
{
   TestReport tr = test_cases.get(test);
   if (tr == null) return 0;
   return tr.getTime();
}


public boolean getPassed(String test)
{
   TestReport tr = test_cases.get(test);
   if (tr == null) return false;
   return tr.getPassed();
}


public boolean getFailed(String test)
{
   TestReport tr = test_cases.get(test);
   if (tr == null) return true;
   return tr.getFailed();
}


public boolean getError(String test) 
{
   TestReport tr = test_cases.get(test);
   if (tr == null) return true;
   return tr.getError();
}


public String getErrorMessage(String test) 
{
   TestReport tr = test_cases.get(test);
   if (tr == null) return null;
   return tr.getErrorMessage();
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


/********************************************************************************/
/*                                                                              */
/*      Information for a single test                                           */
/*                                                                              */
/********************************************************************************/

private static class TestReport {
   
   private boolean test_passed;
   private boolean is_error;
   private String error_message;
   private double test_time;
   
   TestReport(LimbaTestCase tc) {
      test_passed = false;
      error_message = null;
      is_error = false;
      test_time = 0;
    }
   
   double getTime()			{ return test_time; }
   boolean getPassed()			{ return test_passed; }
   boolean getFailed()			{ return !test_passed; }
   boolean getError()			{ return !test_passed && is_error; }
   String getErrorMessage()		{ return error_message; }
   
   void setReport(double time,String errmsg,boolean iserr) {
      test_time = time;
      
      if (errmsg != null && errmsg.startsWith("Throws java.lang.AssertionError: ")) {
         int idx0 = errmsg.indexOf(":");
         errmsg = errmsg.substring(idx0+2);
       }
      
      if (errmsg == null) test_passed = true;
      else {
         test_passed = false;
         error_message = errmsg;
         is_error = iserr;
       }
    }
   
}	// end of subclass TestReport



}       // end of class LimbaTestReport




/* end of LimbaTestReport.java */

