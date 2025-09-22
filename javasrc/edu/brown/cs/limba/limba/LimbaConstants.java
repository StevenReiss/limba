/********************************************************************************/
/*                                                                              */
/*          LimbaConstants.java                                                 */
/*                                                                              */
/*      Language Intelligence Model as a Bubbles Assistant global constants     */
/*                                                                              */
/********************************************************************************/


package edu.brown.cs.limba.limba;

import edu.brown.cs.ivy.xml.IvyXmlWriter;

public interface LimbaConstants {

enum LimbaCommandType {
   LIST_MODELS,
   MODEL_DETAILS;
}


interface LimbaCommand {
   String getCommandName();
   boolean getEndOnBlank();
   String getEndToken();
   
   void setupCommand(String complete,boolean user);
   void setOptions(String options);
   void process(IvyXmlWriter rslt);
}

}   // end of interface LimbaConstants


/* end of LimbaConstants.java */

