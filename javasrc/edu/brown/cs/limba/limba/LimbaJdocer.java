/********************************************************************************/
/*                                                                              */
/*              LimbaJdocer.java                                                */
/*                                                                              */
/*      Produce JavaDoc for an element                                          */
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

import java.util.Map;

import org.w3c.dom.Element;

import edu.brown.cs.ivy.file.IvyLog;
import edu.brown.cs.ivy.xml.IvyXml;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

class LimbaJdocer implements LimbaConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private LimbaMain       limba_main;
private String          base_prompt;
private String          prior_jdoc;
private String          method_body;
private boolean         use_context;
private String          find_what;
private String          method_types;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

LimbaJdocer(LimbaMain lm,String prompt,Element xml)
{
   limba_main = lm;
   base_prompt = prompt;
   Element sxml = IvyXml.getChild(xml,"JAVADOC");
   find_what = IvyXml.getAttrString(xml,"WHAT","METHOD").toLowerCase();
   method_types = IvyXml.getAttrString(xml,"TYPES","*").toUpperCase();
// if (method_types.equals("ALL")) {
//    method_types = "PUBLIC,PRIVATE,PACKAGE,PROTECTED";
//  }
   use_context = IvyXml.getAttrBool(xml,"USECONTEXT",true);
   prior_jdoc = IvyXml.getTextElement(sxml,"PRIOR");
   method_body = IvyXml.getTextElement(sxml,"CODE");
}


/********************************************************************************/
/*                                                                              */
/*      Processing methods                                                      */
/*                                                                              */
/********************************************************************************/

void process(IvyXmlWriter xw) throws Exception
{
   StringBuffer pbuf = new StringBuffer();
   if (base_prompt != null) pbuf.append(base_prompt);
   String p = "Please create JavaDoc for the " + find_what + " below.";
   if (find_what.equals("class") && !method_types.equals("*")) {
      p = "Please create JavaDoc for all";
      boolean have = false;
      if (method_types.contains("PUBLIC")) {
         p += " public";
         have = true;
       }
      if (method_types.contains("PROTECTED")) {
         if (have) p += " or";
         p += " protected";
         have = true;
       }
      if (method_types.contains("PACKAGE")) {
         if (have) p += " or";
         p += " package protected";
         have = true;
       }
      if (method_types.contains("PRIVATE")) {
         if (have) p += " or";
         p += " private";
         have = true;
       }
      p += " method in the class below.";
    }
   else if (find_what.equals("class") && method_types.equals("*")) {
      p = "Please create just the JavaDoc for the whole class, ";
      p += "not the individual methods, for the class below.\n";
      p += "Do not modify or create JavaDoc for the methods of the class.";
    }
   else if (find_what.equals("method")) {
      p = "Please create JavaDoc for the single method below.";
    }
   pbuf.append("\n" + p + "\n");
   if (prior_jdoc != null && !prior_jdoc.isEmpty()) {
      pbuf.append("This will replace the prior JavaDoc which was:\n");
      pbuf.append(prior_jdoc);
      pbuf.append("\n");
    }
// if (method_types.equals("*")) {
//    pbuf.append("Return only the javadoc.\n");
//  }
   pbuf.append("Return a JSON array where each element is a JSON object ");
   pbuf.append("with 2 fields,\nNAME containing the method/class name ");
   pbuf.append("(possibly with parameter types), ");
   pbuf.append("and DOC containing the JavaDoc.\n");
   
   pbuf.append("The " + find_what + " is: \n");
   pbuf.append(method_body);
   
   IvyLog.logD("LIMBA","Find  " + pbuf.toString());
   
   String resp = limba_main.askOllamaWithRetry(pbuf.toString(),use_context);
   Map<String,String> jdocs = LimbaMain.getJavaDoc(resp); 
   if (jdocs != null) {
      for (Map.Entry<String,String> ent : jdocs.entrySet()) {
         xw.begin("JDOC");
         xw.field("NAME",ent.getKey());
         xw.cdata(ent.getValue());
         xw.end("JDOC");
       }
    }
}






}       // end of class LimbaJdocer




/* end of LimbaJdocer.java */

