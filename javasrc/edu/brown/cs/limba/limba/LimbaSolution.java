/********************************************************************************/
/*										*/
/*		LimbaSolution.java						*/
/*										*/
/*	Hold a potential FIND solution						*/
/*										*/
/********************************************************************************/
/*	Copyright 2011 Brown University -- Steven P. Reiss		      */
/*********************************************************************************
 *  Copyright 2011, Brown University, Providence, RI.				 *
 *										 *
 *			  All Rights Reserved					 *
 *										 *
 * This program and the accompanying materials are made available under the	 *
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, *
 * and is available at								 *
 *	http://www.eclipse.org/legal/epl-v10.html				 *
 *										 *
 ********************************************************************************/



package edu.brown.cs.limba.limba;

import edu.brown.cs.ivy.file.IvyLog;
import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.jcomp.JcompControl;
import edu.brown.cs.ivy.jcomp.JcompMessage;
import edu.brown.cs.ivy.jcomp.JcompProject;
import edu.brown.cs.ivy.xml.IvyXmlWriter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.TypeDeclaration;


class LimbaSolution implements LimbaConstants
{


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private LimbaFinder	limba_finder;
private CompilationUnit base_ast;
private ASTNode 	main_node;
private List<ASTNode>	helper_nodes;
private boolean 	use_constructor;
private Set<String> import_set;
private Boolean 	tests_passed;
private List<JcompMessage> compilation_errors;
private List<String>    fail_messages;
private int             line_offset;
private int             end_offset;
private String          solution_name;


/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

LimbaSolution(LimbaFinder lf,String name,String text) throws LimbaException
{
   limba_finder = lf;
   tests_passed = null;
   main_node = null;
   helper_nodes = new ArrayList<>();
   use_constructor = false;
   import_set = new HashSet<>();
   fail_messages = new ArrayList<>();
   line_offset = -1;
   end_offset = -1;
   solution_name = name;

   if (text.contains("class")) {
      base_ast = JcompAst.parseSourceFile(text);
      setupCompilationUnit(base_ast);
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

   JcompProject jp = JcompAst.getResolvedAst(jcomp,base_ast,jars);
   if (jp == null) {
      throw new LimbaException("Unable to resolve AST");
    }
   
   List<JcompMessage> errs = JcompAst.getMessages(null,base_ast);
   compilation_errors = new ArrayList<>();
   for (JcompMessage jm : errs) {
      switch (jm.getSeverity()) {
         case ERROR :
         case FATAL :
            compilation_errors.add(jm);
            break;
         case NOTICE: 
         case WARNING :
            break;
       }
    }

   // then we might want to clean it up -- isolate imports, remove class,
   //	isolate tests (and add to our test suite), identify primary method and
   //	ordered helpers
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

Set<String> getImportTypes()
{
   return import_set;
}


ASTNode getAstNode()			{ return main_node; }

boolean getUseConstructor()
{
   return use_constructor;
}

String getText()	
{
   if (main_node == null) return null;
   
   StringBuffer buf = new StringBuffer();

   for (ASTNode hn : helper_nodes) {
      if (!buf.isEmpty()) buf.append("\n");
      buf.append(hn.toString());
    }
   if (!buf.isEmpty()) buf.append("\n");
   buf.append(main_node.toString());

   return buf.toString();
}


LimbaFindType getFindType()		{ return LimbaFindType.METHOD; }

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

boolean getTestsPassed()
{
   return tests_passed == Boolean.TRUE;
}


List<JcompMessage> getCompilationErrors()
{
   return compilation_errors;
}


void clearFailures()
{
   fail_messages.clear();
}


List<String> getFailures()
{
   return fail_messages;
} 


void addFailure(String msg) 
{
   fail_messages.add(msg);
}


void setLineOffset(int offset,int end)
{
   line_offset = offset;
   end_offset = end;
}


int getSolutionLine(int line)
{
   if (line_offset <= 0) return 0;
   if (line < line_offset) return 0;
   if (line >= end_offset) return 0;
   return line - line_offset;
}


String getName() 
{
   return solution_name;
}


/********************************************************************************/
/*										*/
/*	Setup methods to parse result						*/
/*										*/
/********************************************************************************/

private void setupCompilationUnit(CompilationUnit cu)
{
   for (Object o : cu.imports()) {
      ImportDeclaration id = (ImportDeclaration) o;
      String typ = id.getName().toString();
      if (id.isOnDemand()) typ = typ + ".*";
      import_set.add(typ);
    }

   String tgt = limba_finder.getResultName();
   String tgtend = tgt;
   int idx = tgt.lastIndexOf(".");
   if (idx > 0) tgtend = tgt.substring(idx+1);

   for (Object o : cu.types()) {
      TypeDeclaration td = (TypeDeclaration) o;
      if (limba_finder.getFindType() == LimbaFindType.CLASS) {
	 if (td.getName().getIdentifier().equals(tgtend)) {
	    main_node = td;
	  }
	 else {
	    helper_nodes.add(td);
	  }
       }
      else if (limba_finder.getFindType() == LimbaFindType.METHOD) {
	 for (Object o1 : td.bodyDeclarations()) {
	    if (o1 instanceof MethodDeclaration) {
	       MethodDeclaration md = (MethodDeclaration) o1;
	       if (md.getName().getIdentifier().equals(tgtend)) {
		  main_node = md;
		  if (!Modifier.isStatic(md.getModifiers())) use_constructor = true;
		}
	       else if (md.getName().getIdentifier().equals("main")) {
		  IvyLog.logD("LIMBA","Extract tests from main method");
		}
	       else {
		  helper_nodes.add(md);
		}
	     }
	    else {
	       helper_nodes.add((ASTNode) o1);
	     }
	  }
       }
    }
}


/********************************************************************************/
/*										*/
/*	Ouptut methods								*/
/*										*/
/********************************************************************************/

void output(IvyXmlWriter xw)
{
   xw.begin("SOLUTION"); 
   xw.field("NAME",getName());
   String code = getText();
   if (code.contains("]]>")) xw.textElement("CODE",code);
   else xw.cdataElement("CODE",code);

   xw.begin("COMPLEXITY");
   xw.field("LINES",getCodeLines(code));
   xw.field("CODE",code.length());
   xw.end("COMPLEXITY");

   xw.end("SOLUTION");
}



private int getCodeLines(String code)
{
   StringTokenizer tok = new StringTokenizer(code,"\n");
   int codelines = 0;
   boolean incmmt = false;
   while (tok.hasMoreTokens()) {
      String lin = tok.nextToken();
      boolean hascode = false;
      for (int i = 0; i < lin.length() && !hascode; ++i) {
	 int ch = lin.charAt(i);
	 if (Character.isWhitespace(ch)) continue;
	 if (incmmt) {
	    if (ch == '*' && i+1 < lin.length() && lin.charAt(i+1) == '/') {
	       ++i;
	       incmmt = false;
	     }
	  }
	 else if (ch == '/' && i+1 < lin.length()) {
	    if (lin.charAt(i+1) == '/') break;
	    else if (lin.charAt(i+1) == '*') {
	       incmmt = true;
	     }
	  }
	 else hascode = true;
       }
      if (hascode) ++codelines;
    }

   return codelines;
}


}	// end of class LimbaSolution




/* end of LimbaSolution.java */

