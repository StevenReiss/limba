/********************************************************************************/
/*                                                                              */
/*          LimbaConstants.java                                                 */
/*                                                                              */
/*      Language Intelligence Model as a Bubbles Assistant global constants     */
/*                                                                              */
/********************************************************************************/


package edu.brown.cs.limba.limba;

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
   
}


enum LimbaTestArgType {
   LITERAL,		// value should match literal
   STRING,		// value should match string
   VARIABLE,		// value should match contents of variable
   SAVE 		// store value in variable, no checking
}

enum LimbaFindType {
   METHOD,
   CLASS,
}

interface LimbaCommand {
   String getCommandName();
   
   void process(IvyXmlWriter rslt);
}


enum LimbaSolutionFlag {
   FAIL,		// the solution failed tests
   PASS,		// the solution passed tests
   NONE 		// dummy flag
};


enum LimbaToolSet {
   PROJECT,             // limba tools for the current project
   DEBUG,               // debugging
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
String JAVA_TEST_PROTO = "JavaMethodTest.proto";
String JAVA_ANT_PROTO = "build.xml.proto";
String JUNIT_CLASSPATH = IvyFile.expandName("$(ROOT)/ivy/lib/junit.jar");
String IVY_CLASSPATH = IvyFile.expandName("$(ROOT)/ivy/lib/ivy.jar");
String LIMBA_CLASSPATH = IvyFile.expandName("$(ROOT)/limba/limba.jar");
String JUNIT_RUNNER = "junit.textui.TestRunner";
String JUNIT_OUT = "test.out.xml";
String LIMBA_TEST_DIR = "limbatest";
String ANT_COMMAND = "ant";
String CODE_START = "/*** START OF CODE ***/";
String CODE_END = "/*** END OF CODE ***/";


enum LimbaUserFileType {
   READ,
   WRITE,
   DIRECTORY,
}



}   // end of interface LimbaConstants


/* end of LimbaConstants.java */

