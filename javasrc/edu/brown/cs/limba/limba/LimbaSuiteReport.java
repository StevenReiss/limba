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

import java.io.File;
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
private String base_directory;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

LimbaSuiteReport(LimbaFinder lf,Map<String,String> idmap)
{ 
   base_directory = idmap.get("DIRECTORY");
   test_cases = new HashMap<>();
   for (LimbaTestCase tc : lf.getTestCases()) {
      test_cases.put(tc.getName(),new TestReport(tc));
    }
}

 


/********************************************************************************/
/*                                                                              */
/*      Setup methods                                                           */
/*                                                                              */
/********************************************************************************/

void addReport(String nm,String cnm,double time,String errmsg,boolean iserr,File jarf) 
{
   TestReport tr = test_cases.get(nm);
   if (tr == null && nm.startsWith("test") && nm.length() > 4)
      tr = test_cases.get(nm.substring(5));
   if (tr == null && cnm != null)
      tr = test_cases.get(nm + "(" + cnm + ")");
   if (tr != null) tr.setReport(time,errmsg,iserr,jarf);
   else if (iserr) {
      tr = test_cases.get("S6testFinisher");
      if (tr != null) tr.addError();
    }
}



private static class TestReport {
   
   private boolean test_passed;
   private boolean is_optional;
   private boolean is_error;
   private String error_message;
   private double test_time;
   private String user_value;
   private String user_type;
   private byte [] jar_file;
   private int num_error;
   
   TestReport(LimbaTestCase tc) {
      this(tc.isOptional());
    }
   
   TestReport(boolean opt) {
      test_passed = false;
      is_optional = opt;
      error_message = null;
      is_error = false;
      test_time = 0;
      user_value = null;
      jar_file = null;
      num_error = 0;
    }
   
   double getTime()			{ return test_time; }
   boolean getPassed()			{ return test_passed; }
   boolean isOptional() 		{ return is_optional; }
   boolean getFailed()			{ return !test_passed; }
   boolean getError()			{ return !test_passed && is_error; }
   String getErrorMessage()		{ return error_message; }
   
   String getUserType() 		{ return user_type; }
   String getUserValue()		{ return user_value; }
   byte [] getJarFile() 		{ return jar_file; }
   void addError()			{ ++num_error; }
   
   void setTestStatus(LimbaSolutionFlag sts) {
      switch (sts) {
         case FAIL :
            test_passed = false;
            is_error = true;
            error_message = "User Decision";
            break;
         case PASS :
            user_value = null;
            user_type = null;
            break;
         default :
            break;
       }
    }
   
   void setReport(double time,String errmsg,boolean iserr,File jarf) {
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

