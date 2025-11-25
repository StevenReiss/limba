/********************************************************************************/
/*                                                                              */
/*              LimbaTools.java                                                 */
/*                                                                              */
/*      Tools for use in program-related queries                                */
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
import java.util.List;

import org.w3c.dom.Element;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import edu.brown.cs.ivy.file.IvyLog;
import edu.brown.cs.ivy.xml.IvyXml;

public class LimbaTools implements LimbaConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private LimbaMsg        message_server;
private Collection<File> project_files;

/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

LimbaTools(LimbaMsg msg,Collection<File> files)
{
   message_server = msg;
   project_files = new ArrayList<>(files);
}



/********************************************************************************/
/*                                                                              */
/*      Tool to return constructor information                                  */
/*                                                                              */
/********************************************************************************/

@Tool("returns the set of available constructors for a given class")
public List<String> getConstructorsForClass(@P("name of the class") String name)
{
   List<String> rslt = new ArrayList<>();
   
   findClass(name);
   
   IvyLog.logD("LIMBA","Find constructors for class " + name);
   
   return rslt;
}



/********************************************************************************/
/*                                                                              */
/*      Tool to return information about a method                               */
/*                                                                              */
/********************************************************************************/

@Tool("return the signature and javadoc describing a method")
public String getMethodInformation(@P("full name of the method") String name)
{
   int idx = name.indexOf("(");
   int idx1 = 0;
   if (idx1 > 0) {
      idx1 = name.lastIndexOf(".",idx);
    }
   else {
      idx1 = name.lastIndexOf(".");
    }
   String cnm = null;
   String mnm = name;
   if (idx1 > 0) {
      cnm = name.substring(0,idx1);
      mnm = name.substring(idx1+1);
    }
   
   if (cnm != null) {
      findClass(cnm);
    }
   
   IvyLog.logD("LIMBA","Get info for class " + cnm + " and method " + mnm);

   return name;
}


/********************************************************************************/
/*                                                                              */
/*      Tool to return information about a class                                */
/*                                                                              */
/********************************************************************************/

@Tool("return the set of methods of a class")
public List<String> getClassMethods(@P("name of the class") String name)
{
   List<String> rslt = new ArrayList<>();
   
   findClass(name);
   
   IvyLog.logD("LIMBA","Find methods for class " + name);
   
   return rslt;
}


/********************************************************************************/
/*                                                                              */
/*      Helper methods                                                          */
/*                                                                              */
/********************************************************************************/

private void findClass(String name) 
{
   if (message_server != null) {
      Element xml = message_server.findClass(name);
      if (xml != null && IvyXml.isElement(xml,"RESULT")) {
         // handle result
       }
    }
   
   for (File f : project_files) {
      String pnm = f.getPath();
      pnm = pnm.replace("/",".");
      // find file that matches the name -- take inner classes into account
    }
}


}       // end of class LimbaTools




/* end of LimbaTools.java */

