/********************************************************************************/
/*                                                                              */
/*          LimbaConstants.java                                                 */
/*                                                                              */
/*      Language Intelligence Model as a Bubbles Assistant global constants     */
/*                                                                              */
/********************************************************************************/


package edu.brown.cs.limba.limba;

import org.w3c.dom.Element;

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

}   // end of interface LimbaConstants


/* end of LimbaConstants.java */

