/********************************************************************************/
/*                                                                              */
/*          LimbaConstants.java                                                 */
/*                                                                              */
/*      Language Intelligence Model as a Bubbles Assistant global constants     */
/*                                                                              */
/********************************************************************************/


package edu.brown.cs.limba.limba;


public interface LimbaConstants {

enum LimbaCommandType {
   LIST_MODELS,
   MODEL_DETAILS;
}


interface LimbaCommand {
   String getCommandName();
   boolean getEndOnBlank();
   boolean getNeedsInput();
   String getEndToken();
   
   void setupCommand(String complete);
   void process();
}

}   // end of interface LimbaConstants


/* end of LimbaConstants.java */

