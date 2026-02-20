/********************************************************************************/
/*                                                                              */
/*              LimbaToolsStruct.java                                           */
/*                                                                              */
/*      Tool agents involving structural information about system               */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2025 Brown University -- Steven P. Reiss                    */
/*********************************************************************************
 *  Copyright 2025, Brown University, Providence, RI.                            *
 *                                                                               *
 *                        All Rights Reserved                                    *
 *                                                                               *
 *  Permission to use, copy, modify, and distribute this software and its        *
 *  documentation for any purpose other than its incorporation into a            *
 *  commercial product is hereby granted without fee, provided that the          *
 *  above copyright notice appear in all copies and that both that               *
 *  copyright notice and this permission notice appear in supporting             *
 *  documentation, and that the name of Brown University not be used in          *
 *  advertising or publicity pertaining to distribution of the software          *
 *  without specific, written prior permission.                                  *
 *                                                                               *
 *  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS                *
 *  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND            *
 *  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY      *
 *  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY          *
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,              *
 *  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS               *
 *  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE          *
 *  OF THIS SOFTWARE.                                                            *
 *                                                                               *
 ********************************************************************************/



package edu.brown.cs.limba.limba;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import edu.brown.cs.ivy.file.IvyLog;

public class LimbaToolsStruct extends LimbaToolBase
{



/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

LimbaToolsStruct(LimbaMain lm,Collection<File> files)
{
   super(lm,files);
}


/********************************************************************************/
/*                                                                              */
/*      Tool to return constructor information                                  */
/*                                                                              */
/********************************************************************************/

@Tool("This agent returns the set of available constructors for a given class")
public List<String> getConstructorsForClass(@P("name of the class") String name) 
{
   List<String> rslt = new ArrayList<>();
   
   TypeDeclaration td = findClassAst(name,false);
   
   IvyLog.logD("LIMBA","Find constructors for class " + name);
   limba_main.transcriptAgent("Get constructors for " + name); 
   
   if (td != null) {
      for (Object o1 : td.bodyDeclarations()) {
         if (o1 instanceof MethodDeclaration) {
            MethodDeclaration md = (MethodDeclaration) o1;
            if (!md.isConstructor()) continue;
            String txt = getMethodDescription(md);
            rslt.add(txt);
          }
       }
    }
   
   IvyLog.logD("LIMBA","Result is " + rslt);
   
   return rslt;
}


/********************************************************************************/
/*                                                                              */
/*      Tool to return information about a class                                */
/*                                                                              */
/********************************************************************************/

@Tool("This agent returns the set of methods of a class.  Each method is returned as " +
      "its signature and description.  Will return an empty list if " +
      "the class does not exist.")
public List<String> getClassMethods(@P("name of the class") String name)
{
   List<String> rslt = new ArrayList<>();
   
   IvyLog.logD("LIMBA","Find methods for class " + name);
   limba_main.transcriptAgent("Get class methods for " + name); 
   
   TypeDeclaration td = findClassAst(name,false);
   if (td != null) {
      for (Object o1 : td.bodyDeclarations()) {
         if (o1 instanceof MethodDeclaration) {
            MethodDeclaration md = (MethodDeclaration) o1;
            String txt = getMethodDescription(md);
            rslt.add(txt);
          }
       }
    }
   
   IvyLog.logD("LIMBA","Result is " + rslt);
   
   return rslt;
}


@Tool("This agent returns the set of fields of a class.  Each field is returns with its " +
      "declaration and its description.  Will return an empty list if " +
      "the class does not exist.")
public List<String> getClassFields(@P("name of the class") String name)
{
   List<String> rslt = new ArrayList<>();
   
   IvyLog.logD("LIMBA","FIND FIELDS for class " + name);
   limba_main.transcriptAgent("Get class fields for " + name); 
   
   TypeDeclaration td = findClassAst(name,false);
   if (td != null) {
      for (Object o1 : td.bodyDeclarations()) {
         if (o1 instanceof FieldDeclaration) {
            FieldDeclaration fd = (FieldDeclaration) o1;
            for (Object o2 : fd.fragments()) {
               VariableDeclarationFragment vdf = (VariableDeclarationFragment) o2;
               String txt = getFieldDescription(fd,vdf);
               rslt.add(txt);
             }
          }
       }
    }
   
   IvyLog.logD("LIMBA","Result is " + rslt);
   
   return rslt;
}



}       // end of class LimbaToolsStruct




/* end of LimbaToolsStruct.java */

