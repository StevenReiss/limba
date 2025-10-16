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
import java.util.HashSet;
import java.util.Set;

import org.w3c.dom.Element;

import edu.brown.cs.ivy.xml.IvyXml;

class LimbaFindContext implements LimbaConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private Element         context_xml;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

LimbaFindContext(LimbaFinder fdr,Element xml)
{
   context_xml = xml;
}


/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

boolean getUsePath()
{
   return IvyXml.getAttrBool(context_xml,"USEPATH");
}


String getPackage()                     
{
   return IvyXml.getAttrString(context_xml,"PACKAGE");
}

String getClassName()                  
{ 
   return IvyXml.getAttrString(context_xml,"CLASS");
}

String getSourceFileName()   
{ 
  Element sxml = IvyXml.getChild(context_xml,"SOURCE"); 
  return IvyXml.getAttrString(sxml,"NAME");
}


String getClassPath()
{
   StringBuffer rslt = new StringBuffer();
   for (Element cpxml : IvyXml.children(context_xml,"CLASSPATH")) {
      String cpe = IvyXml.getText(cpxml);
      if (cpe.contains("jrt-fs.jar")) continue;
      if (cpe.contains("poppy.jar")) continue;
      if (!rslt.isEmpty()) rslt.append(File.pathSeparator);
      rslt.append(cpe);
    }
   
   return rslt.toString();
}


Set<String> getImports()
{
   Set<String> rslt = new HashSet<>();
   for (Element ielt : IvyXml.children(context_xml,"IMPORTS")) {
      String impstr = IvyXml.getText(ielt).trim();
      if (impstr.endsWith(";")) {
         int idx = impstr.lastIndexOf(";");
         impstr = impstr.substring(0,idx).trim();
       }
      if (impstr.startsWith("import ")) {
         impstr = impstr.substring(7).trim();
       }
      rslt.add(impstr);
    }

   return rslt;
}



}       // end of class LimbaTestContext




/* end of LimbaTestContext.java */

