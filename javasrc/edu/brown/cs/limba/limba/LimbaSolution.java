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

import edu.brown.cs.ivy.file.IvyLog;
import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.jcomp.JcompControl;
import edu.brown.cs.ivy.jcomp.JcompProject;

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

private LimbaFinder     limba_finder;
private CompilationUnit java_ast;
private Boolean         tests_passed;


/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

LimbaSolution(LimbaFinder lf,String text) throws LimbaException
{
   limba_finder = lf;
   tests_passed = null;
   if (text.contains("class")) {
      java_ast = JcompAst.parseSourceFile(text);
    }
   else {
      // need to extract imports and then build a file with imports, dummy class, 
      //   and body
      // alternatively, extract import statements and parse the rest using
      //   JcompAst.parseDeclarations() and then build a compilation unit
      ASTNode decls = JcompAst.parseDeclarations(text); 
      IvyLog.logD("LIMBA","Scanned declarations, not compilation unit " + decls);
      throw new LimbaException("Not a compilation unit");
    }
   
   JcompControl jcomp = limba_finder.getLimbaMain().getJcompControl(); 
   List<String> jars = null;
// if (limba_finder.getContextJar() != null) {
//    jars = new ArrayList<>();
//    jars.add(limba_finder.getContextJar().getPath());
//  }

   JcompProject jp = JcompAst.getResolvedAst(jcomp,java_ast,jars);
   if (jp == null) {
      throw new LimbaException("Unable to resolve AST");
    }
 
   // then we might want to clean it up -- isolate imports, remove class,
   //   isolate tests (and add to our test suite), identify primary method and
   //   ordered helpers
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

String getText()                        { return null; }

LimbaFindType getFindType()             { return LimbaFindType.METHOD; } 

synchronized void setTestsPassed(boolean fg) 
{
   tests_passed = fg; 
   notifyAll();
}

synchronized boolean waitForTesting()
{
   while (tests_passed == null) {
      try {
         wait(3000);
       }
      catch (InterruptedException e) { }
    }
   
   return tests_passed;
}


void clearResolve()
{
   
}



}       // end of class LimbaSolution




/* end of LimbaSolution.java */

