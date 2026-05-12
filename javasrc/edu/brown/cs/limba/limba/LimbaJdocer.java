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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

private static final Pattern ITEM_PATTERN = Pattern.compile("### `([^`])+`"); 


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
   if (find_what.equals("class") && !method_types.equals("ALL")) {
      p = "Please create JavaDoc for each ";
      if (method_types.contains("PUBLIC")) p += " public, ";
      if (method_types.contains("PROTECTED")) p += " protected, ";
      if (method_types.contains("PACKAGE")) p += " package protected, ";
      if (method_types.contains("PRIVATE")) p += " private, ";
      p += " method in the class below.";
    }
   else if (find_what.equals("class") && method_types.equals("ALL")) {
      p = "Please create JavaDoc for each ";
      p += " method in the class below.";
    }
   pbuf.append("\n" + p + "\n");
   if (prior_jdoc != null && !prior_jdoc.isEmpty()) {
      pbuf.append("This will replace the prior JavaDoc which was:\n");
      pbuf.append(prior_jdoc);
      pbuf.append("\n");
    }
   if (method_types.equals("*")) {
      pbuf.append("Return only the javadoc.\n");
    }
   pbuf.append("The " + find_what + " is: \n");
   pbuf.append(method_body);
   
   IvyLog.logD("LIMBA","Find  " + pbuf.toString());
   
   String resp = limba_main.askOllama(pbuf.toString(),use_context);
   List<String> jdocs = LimbaMain.getJavaDoc(resp);
   if (jdocs != null) {
      List<String> items = getMethodList(resp);
      int i = 0;
      for (String s : jdocs) {
         if (i >= items.size()) {
            xw.cdataElement("JDOC",s);
          }
         else {
            xw.begin("JDOC");
            xw.field("METHOD",items.get(i));
            xw.cdata(s);
            xw.end("JDOC");
          }
       }
    }
}


/********************************************************************************/
/*                                                                              */
/*      Get method names                                                        */
/*                                                                              */
/********************************************************************************/

private List<String> getMethodList(String resp)
{
   List<String> rslt = new ArrayList<>();

   Matcher m = ITEM_PATTERN.matcher(resp);
   while (m.find()) {
      String s = m.group(1);
      rslt.add(s);
    }   
   
   return rslt;
}



}       // end of class LimbaJdocer




/* end of LimbaJdocer.java */

