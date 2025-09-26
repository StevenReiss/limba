/********************************************************************************/
/*                                                                              */
/*          LimbaConstants.java                                                 */
/*                                                                              */
/*      Language Intelligence Model as a Bubbles Assistant global constants     */
/*                                                                              */
/********************************************************************************/


package edu.brown.cs.limba.limba;

import org.w3c.dom.Element;

import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

public interface LimbaConstants {

enum LimbaCommandType {
   LIST_MODELS,
   MODEL_DETAILS;
}

enum LimbaTestType {
   USERCODE,
   CALLS,
   JUNIT
}


enum LimbaTestOp {        
   NONE,
   EQL,
   NEQ,
   SAVE,
   IGNORE,
   THROW,
   SAME,
   DIFF,
   SHOW,
   INTERACT,
   HIERARCHY,
   SCOREHIER
}


enum LimbaTestArgType {
   LITERAL,		// value should match literal
   STRING,		// value should match string
   VARIABLE,		// value should match contents of variable
   SAVE 		// store value in variable, no checking
}

interface LimbaCommand {
   String getCommandName();
   boolean getEndOnBlank();
   String getEndToken();
   
   void setupCommand(String complete,boolean user);
   void setupCommand(Element xml);
   void setOptions(String options);
   void process(IvyXmlWriter rslt);
}


/********************************************************************************/
/*                                                                              */
/*      Testing definitions                                                     */
/*                                                                              */
/********************************************************************************/

String LIMBA_PACKAGE_PREFIX = "limbatest_";
String LIMBA_TEST_CLASS = "LimbaTestClass";
String LIMBA_USER_CLASS = "LimbaUserClass";
String ANT_FILE = "build.xml";
String LIMBA_BINARY_DIR = "bin";
String JAVA_TEST_PROTO = "resources/JavaMethodTest.proto";
String JAVA_ANT_PROTO = "resources/build.xml.proto";
String JUNIT_CLASSPATH = IvyFile.expandName("$(ROOT)/ivy/lib/junit.jar");
String IVY_CLASSPATH = IvyFile.expandName("$(ROOT)/ivy/lib/ivy.jar");
String JUNIT_RUNNER = "junit.textui.TestRunner";
String JUNIT_OUT = "test.out.xml";



}   // end of interface LimbaConstants


/* end of LimbaConstants.java */

