/********************************************************************************/
/*                                                                              */
/*              LimbaTestCase.java                                              */
/*                                                                              */
/*      Test case for checking LLM result                                       */
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
import java.util.List;

import org.w3c.dom.Element;

import edu.brown.cs.ivy.xml.IvyXml;

abstract class LimbaTestCase implements LimbaConstants
{


/********************************************************************************/
/*                                                                              */
/*      Static creators                                                         */
/*                                                                              */
/********************************************************************************/

static LimbaTestCase createTestCase(Element xml) throws LimbaException
{
   String typ = IvyXml.getAttrString(xml,"TYPE");
   if (typ == null) return null;
   typ = typ.toUpperCase();
   switch (typ) {
      case "CALLS" :
         return new CallSetTest(xml);
      case "USERCODE" :
         return new UserCodeTest(xml);
      case "JUNIT" :
         return new UserJunitTest(xml);
    }
   
   return null;
}


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private String		test_name;
private boolean 	is_optional;
protected boolean	user_input;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

protected LimbaTestCase(Element xml) throws LimbaException
{
   test_name = IvyXml.getAttrString(xml,"NAME");
   is_optional = IvyXml.getAttrBool(xml,"OPTIONAL",false);
   user_input = false;
   
   if (test_name == null) throw new LimbaException("TESTCASE must be named");
}




/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

String getName() 			{ return test_name; }

boolean isOptional()			{ return is_optional; }

abstract LimbaTestType getTestType();

String getUserCode()			{ return null; }

List<CallTest> getCalls()		{ return null; }

String getJunitClass()                   { return null; }
String getJunitName()                    { return null; }

boolean getNeedsUserInput()		{ return user_input; }

Collection<String> getImports()         { return null; }


/********************************************************************************/
/*										*/
/*	UserCodeTest -- test case where user defines the whole method		*/
/*										*/
/********************************************************************************/

private static class UserCodeTest extends LimbaTestCase {
   
   private String user_code;
   
   UserCodeTest(Element xml) throws LimbaException {
      super(xml);
      user_code = IvyXml.getTextElement(xml,"CODE");
    }
   
    LimbaTestType getTestType()		{ return LimbaTestType.USERCODE; } 
   
    String getUserCode()			{ return user_code; }
   
}	// end of subclass UserCodeTest



/********************************************************************************/
/*                                                                              */
/*      Context-based Junit test                                                */
/*                                                                              */
/********************************************************************************/

private static class UserJunitTest extends LimbaTestCase {
   
   private String junit_class;
   private String junit_method;
   private String junit_name;
   
   UserJunitTest(Element xml) throws LimbaException {
      super(xml);
      junit_class = IvyXml.getTextElement(xml,"CLASS");
      junit_method = IvyXml.getTextElement(xml,"METHOD");
      junit_name = IvyXml.getTextElement(xml,"TESTNAME");
    }
   
    LimbaTestType getTestType()           { return LimbaTestType.JUNIT; }
   
    String getJunitClass()                { return junit_class; }
    String getJunitName()                 { return junit_name; }
   
    String getUserCode() {
      return "// " + junit_name + " " + junit_class + "." + junit_method + "\n";
    }
   
}       // end of innter class UserJunitTest 


/********************************************************************************/
/*										*/
/*	CallSetTest -- test where user provides input and output		*/
/*										*/
/********************************************************************************/

private static class CallSetTest extends LimbaTestCase {
   
   private List<CallTest> call_set;
   private String setup_code;
   
   CallSetTest(Element xml) throws LimbaException {
      super(xml);
      call_set = new ArrayList<>();
      for (Element e : IvyXml.elementsByTag(xml,"CALL")) {
         if (!isValidCallTest(e)) continue;
         CallTest cti = new CallTest(e);
         call_set.add(cti);
         if (cti.getNeedsUserInput()) user_input = true;
       }
      setup_code = IvyXml.getTextElement(xml,"CODE");
    }
   
    LimbaTestType getTestType()		{ return LimbaTestType.CALLS; }
    String getUserCode()		{ return setup_code; }
   
    List<CallTest> getCalls()		{ return call_set; }
   
}	// end of subclass CallSetTest


private static boolean isValidCallTest(Element e)
{
   if (IvyXml.getAttrString(e,"THIS") != null) return true;
   if (IvyXml.getAttrString(e,"METHOD") != null) return true;
   if (IvyXml.getAttrBool(e,"NEW")) return true;
   
   return false;
}



static class CallTest {
   
   private String call_name;
   private List<CallArg> call_args;
   private CallArg	 result_code;
   private boolean	is_new;
   private LimbaTestOp	test_op; 
   private String	throw_type;
   private boolean	user_input;
   private boolean	is_access;
   
   CallTest(Element xml) throws LimbaException {
      result_code = null;
      user_input = false;
      is_new = IvyXml.getAttrBool(xml,"NEW");
      call_name = IvyXml.getTextElement(xml,"METHOD");
      test_op = IvyXml.getAttrEnum(xml,"OP",LimbaTestOp.NONE);
      if (test_op == LimbaTestOp.SHOW || test_op == LimbaTestOp.HIERARCHY ||
            test_op == LimbaTestOp.SCOREHIER || test_op == LimbaTestOp.INTERACT) {
         user_input = true;
       }
      String cthis = IvyXml.getTextElement(xml,"THIS");
      if (call_name == null) {
         is_access = true;
         call_name = cthis;
       }
      else if (cthis != null) call_name = cthis + "." + call_name;
      
      call_args = new ArrayList<CallArg>();
      for (Element e : IvyXml.elementsByTag(xml,"INPUT")) {
         call_args.add(new CallArg(e));
       }
      
      Element oe = IvyXml.getElementByTag(xml,"OUTPUT");
      if (oe != null) result_code = new CallArg(oe);
      
      if (test_op == LimbaTestOp.NONE) {
         if (result_code == null) test_op = LimbaTestOp.IGNORE;
         else test_op = LimbaTestOp.EQL;
       }
      
      throw_type = IvyXml.getTextElement(xml,"THROW");
    }
   
   String getMethod()			        { return call_name; }
   List<CallArg> getArguments()		{ return call_args; }
   CallArg getReturnValue()		        { return result_code; }
   boolean isConstructor()	        	{ return is_new; }
   LimbaTestOp getOperator()		        { return test_op; }
   String getThrows()			        { return throw_type; }
   boolean getNeedsUserInput()		{ return user_input; }
   boolean isAccess()		        	{ return is_access; }
   
}	// end of subclass CallTest



static class CallArg {
    
   private LimbaTestArgType arg_type;
   private String arg_value;
   private String arg_code;
   
   CallArg(Element xml) throws LimbaException {
      arg_type = IvyXml.getAttrEnum(xml,"TYPE",LimbaTestArgType.LITERAL);
      arg_code = IvyXml.getTextElement(xml,"CODE");
      arg_value = IvyXml.getTextElement(xml,"VALUE");
      if (arg_value == null) arg_value = IvyXml.getText(xml);
    }
   
   LimbaTestArgType getArgType()		{ return arg_type; }
   String getArgValue()			{ return arg_value; }
   String getArgCode()			        { return arg_code; }
   
}	// end of subclass CallArg


}       // end of class LimbaTestCase




/* end of LimbaTestCase.java */

