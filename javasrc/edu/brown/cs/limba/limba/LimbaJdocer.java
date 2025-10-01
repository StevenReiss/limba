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
   pbuf.append("\nPlease create JavaDoc for the " + find_what + " below.\n");
   if (prior_jdoc != null && !prior_jdoc.isEmpty()) {
      pbuf.append("This will replace the prior JavaDoc which was:\n");
      pbuf.append(prior_jdoc);
      pbuf.append("\n");
    }
   pbuf.append("Return only the javadoc.\n");
   pbuf.append("The code is: \n");
   pbuf.append(method_body);
   
   IvyLog.logD("LIMBA","Find " + pbuf.toString());
   
   String resp = limba_main.askOllama(pbuf.toString());
   String jdoc = LimbaMain.getJavaDoc(resp);
   if (jdoc != null) {
      xw.cdataElement("JDOC",jdoc);
    }
}


}       // end of class LimbaJdocer




/* end of LimbaJdocer.java */

