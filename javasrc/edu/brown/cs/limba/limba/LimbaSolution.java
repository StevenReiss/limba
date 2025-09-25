/********************************************************************************/
/*                                                                              */
/*              LimbaSolution.java                                              */
/*                                                                              */
/*      Hold a potential FIND solution                                          */
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

import edu.brown.cs.ivy.jcomp.JcompAst;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ImportDeclaration;


class LimbaSolution implements LimbaConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private String          start_text;
private CompilationUnit java_ast;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

LimbaSolution(String text)
{
   start_text = text;
   if (text.contains("class")) {
      java_ast = JcompAst.parseSourceFile(text);
    }
   else {
      // need to extract imports and then build a file with imports, dummy class, 
      //   and body
      // alternatively, extract import statements and parse the rest using
      //   JcompAst.parseDeclarations() and then build a compilation unit
      ASTNode decls = JcompAst.parseDeclarations(text); 
    }
   
   // identify the primary method
   
   // probably want to compile as well -- context should be passed in as jar file.
   // jcomp_main = new JcompControl in limbaMain.
   // JcompProject jp = jcomp_main.getProject(contextjar,JcompSource for text,,false);
   //  then jp.resolve() and jcomp_main.freeProject()
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

List<String> getImportTypes()
{
   List<String> types = new ArrayList<>();
   
   for (Object o : java_ast.imports()) {
      ImportDeclaration id = (ImportDeclaration) o;
      if (id.isOnDemand()) continue;
      String typ = id.getName().toString();
      types.add(typ);
    }
   
   return types;
}


ASTNode getAstNode()                    { return java_ast; }

boolean getUseConstructor() 
{
   // if method is static, return false
   return true;
}

}       // end of class LimbaSolution




/* end of LimbaSolution.java */

