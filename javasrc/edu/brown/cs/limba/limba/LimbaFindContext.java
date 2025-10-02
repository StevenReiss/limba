/********************************************************************************/
/*                                                                              */
/*              LimbaTestContext.java                                           */
/*                                                                              */
/*      Holder of a test context                                                */
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
import java.util.ArrayList;
import java.util.Collection;

import org.w3c.dom.Element;

class LimbaFindContext implements LimbaConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

@SuppressWarnings("unused")
private LimbaFinder     limba_finder;
@SuppressWarnings("unused")
private Element         context_xml;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

LimbaFindContext(LimbaFinder fdr,Element xml)
{
   limba_finder = fdr;
   context_xml = xml;
}


/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

String getPackage()                     { return null; }
String getClassName()                   { return null; }
String getJarFileName()                 { return null; }
File getContextDirectory()              { return null; }
Collection<UserFile> getUserFiles()     { return new ArrayList<>(); }
String getSourceFileName()              { return null; }
Collection<LimbaTestCase> getTests()    { return null; }
String getJunitClass()                  { return null; }
String getJunitName()                   { return null; }


/********************************************************************************/
/*                                                                              */
/*      User data file                                                          */
/*                                                                              */
/********************************************************************************/

class UserFile {
   
   UserFile(Element xml) { }
   
   LimbaUserFileType getFileType()      { return LimbaUserFileType.READ; } 
   String getLocalName()                { return "LOCAL"; }
   String getUserName()                 { return "USER"; }
   String getDisplayName()              { return "DISPLAY"; }
   String getProjectId()                { return "PROJECTID"; }

}


}       // end of class LimbaTestContext




/* end of LimbaTestContext.java */

