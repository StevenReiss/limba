/********************************************************************************/
/*                                                                              */
/*              LimbaTester.java                                                */
/*                                                                              */
/*      Run Junit teest for a solution                                          */
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
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import edu.brown.cs.ivy.file.IvyLog;

class LimbaTester implements LimbaConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private LimbaSolution   for_solution;
private LimbaFinder     limba_finder;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

LimbaTester(LimbaFinder lf,LimbaSolution sol)
{
   limba_finder = lf;
   for_solution = sol;
}



/********************************************************************************/
/*                                                                              */
/*      Run and check the test                                                  */
/*                                                                              */
/********************************************************************************/

LimbaTestReport runTester()
{
   Map<String,String> idmap = new HashMap<>();
   
   try {
      setupJunitTest(idmap);
      return runJunitTest(idmap);
    }
   catch (LimbaException e) {
      IvyLog.logD("LIMBA","Problem running tests",e);
    }
   
   return null;
}



/********************************************************************************/
/*                                                                              */
/*      Setup all map properties to generate the test                           */
/*                                                                              */
/********************************************************************************/

private void setupJunitTest(Map<String,String> idmap)
   throws LimbaException
{
   idmap.put("CLASS",LIMBA_TEST_CLASS);
   idmap.put("SOURCEFILE",LIMBA_TEST_CLASS + ".java");
   idmap.put("JUNITCP",JUNIT_CLASSPATH);
   idmap.put("JUNIT",JUNIT_RUNNER); 
   idmap.put("JUNITOUT",JUNIT_OUT);
   idmap.put("TESTCLASS",LIMBA_USER_CLASS);
   idmap.put("IVY",IVY_CLASSPATH); 
   
   idmap.put("ANTRUN","test");
   idmap.put("MAXTIME","10000L");
   idmap.put("SHARED_EXT","so");
   if (System.getProperty("os.name").startsWith("Mac")) {
      idmap.put("SHARED_EXT","dynlib");
    }
   idmap.put("SETUP","");
   
   setupUserContext(idmap);
   
   String clsname = idmap.get("TESTCLASS");
   idmap.put("PREFIX",clsname);
   
   setupTestPackage(idmap);
   
   Set<String> imports = new HashSet<>();
   for (String jt : for_solution.getImportTypes()) {
      imports.add(jt);
    }
   for (LimbaTestCase tc : limba_finder.getTestCases()) { 
      Collection<String> imps = tc.getImports();
      if (imps != null) {
         for (String im : imps) {
            imports.add(im);
          }
       }
    }
   Collection<String> cimps = limba_finder.getContextImports(); 
   if (cimps != null) {
      for (String im : cimps) {
         imports.add(im);
       }
    }
   StringBuffer importstr = new StringBuffer();
   for (String s : imports) {
      importstr.append("Import " + s + ";\n");
    }
   idmap.put("IMPORTS",importstr.toString());
   
   setupTests(idmap);
   
   setupCode(idmap);
   
   setupSourceFile(idmap);
}



private void setupUserContext(Map<String,String> idmap) throws LimbaException
{
   LimbaTestContext ctx = limba_finder.getTestContext();
   if (ctx == null) return;
   
   String s = limba_finder.getPackageName();
   if (s == null) s = ctx.getPackage(); 
   if (s != null) idmap.put("PACKAGE",s);
   else idmap.put("PACKAGE","");
   
   String cls = ctx.getClassName(); 
   if (cls != null) {
      String fcls;
      fcls = cls;
      idmap.put("TESTCLASS",fcls);
      idmap.put("PREFIX",fcls);
    }
   
   String jnm = ctx.getJarFileName();
   if (jnm != null) idmap.put("LIMBACTX",jnm);
   
   File cdir = ctx.getContextDirectory();
   if (cdir == null) return; 
   
   StringBuffer buf = new StringBuffer();
   StringBuffer ebuf = new StringBuffer();
   for (LimbaTestContext.UserFile uf : ctx.getUserFiles()) {  
      String nm = uf.getLocalName();
      File cfl = new File(cdir,nm);
      String unm = uf.getUserName();
      ebuf.append(nm); 
      ebuf.append(">");
      ebuf.append(unm);
      ebuf.append(">");
      switch (uf.getFileType()) {
 	 case READ :
 	    ebuf.append("R");
 	    buf.append("<exec executable='ln'><arg value='-s' /><arg value='");
 	    buf.append(cfl.getPath());
 	    buf.append("' /></exec>\n");
 	    break;
 	 case WRITE :
 	    ebuf.append("W");
 	    if (cfl.exists()) {
 	       buf.append("<copy file='");
 	       buf.append(cfl.getPath());
 	       buf.append("' todir='.' />");
 	     }
 	    break;
 	 case DIRECTORY :
 	    ebuf.append("D");
 	    break;
       }
      ebuf.append("&");
    }
   idmap.put("CONTEXT_ANT",buf.toString());
   idmap.put("LIMBA_CONTEXT_MAP",ebuf.toString());
   // System.err.println("CONTEXT SETUP: ANT = " + buf.toString());
   // System.err.println("CONTEXT SETUP: S6 = " + ebuf.toString());
}




private void setupTestPackage(Map<String,String> idmap) throws LimbaException
{
   File root = new File(System.getProperty("java.io.tmpdir") + File.separator + LIMBA_TEST_DIR);
   if (!root.exists() && !root.mkdir())
      throw new LimbaException("Can't create S6 test directory: " + root);
   idmap.put("ROOT",root.getPath()); 
   
   String pkg = null;
   Random r = new Random();
   File dir = null;
   for (int i = 0; i < 1000; ++i) {
      pkg = LIMBA_PACKAGE_PREFIX + r.nextInt(131256);
      dir = new File(root.getPath() + File.separator + pkg);
      if (dir.exists()) continue;
      if (dir.mkdir()) break;
      dir = null;
    }
   
   if (dir == null) throw new LimbaException("S6 test directory not created");
   
   idmap.put("DIRECTORY",dir.getPath());
   if (idmap.get("PACKAGE") == null) idmap.put("PACKAGE",pkg);
   idmap.put("SRCDIR",dir.getPath());
   idmap.put("PROJECTNAME",pkg);
   
   File sf1 = new File(dir,"LIMBASOURCE");
   try {
      PrintWriter fw = new PrintWriter(new FileWriter(sf1));
      try {
// 	 fw.println(src.getDisplayName());
// 	 fw.println(src.getUserName());
// 	 fw.println(src.getProjectId()); 
       }
      catch (Throwable t) { }
      fw.close();
    }
   catch (IOException e) { }
   
   File bin = new File(dir,LIMBA_BINARY_DIR);
   
   if (!bin.mkdirs()) {
      throw new LimbaException("Problem creating binary subdirectory: " + bin);
    }
   idmap.put("BIN",bin.getPath());
   
   String npkg = idmap.get("PACKAGE");
   if (npkg == null || npkg.equals("*") || npkg.equals("?") || npkg.equals("")) npkg = null;
   if (npkg != null) {
      idmap.put("PACKAGESTMT","package " + npkg + ";\n");
      idmap.put("PACKAGEDOT",npkg + ".");
    }
}


/********************************************************************************/
/*										*/
/*	Methods to generate the searched for code				*/
/*										*/
/********************************************************************************/

private void setupCode(Map<String,String> idmap)
{	
   String gencode = "";
   gencode = java_fragment.getText();
   
   if (java_fragment.getFragmentType() == CoseResultType.METHOD) {
      gencode = "private static class " + idmap.get("TESTCLASS") + " {\n\n" + gencode;
      gencode += "\n}\t//end of class " + idmap.get("TESTCLASS") + "\n";
    }
   
   gencode = jc.fixupJmlCode(gencode);
   
   idmap.put("CODE",gencode);
   
   jc.removeContracts();
}


/********************************************************************************/
/*										*/
/*	Methods to actually generate test code					*/
/*										*/
/********************************************************************************/

private void setupTests(Map<String,String> idmap) throws LimbaException
{
   StringBuffer buf = new StringBuffer();
   String create = "";
   
   if (java_fragment.getUseConstructor()) {
      create = idmap.get("TESTCLASS") + " __object = new " + idmap.get("TESTCLASS") + "();\n";
      idmap.put("PREFIX","__object");
      idmap.put("SETUP",create);
    }
   
   boolean havetest = false;
   for (S6TestCase tc : for_request.getTests().getTestCases()) {
      if (tc.getTestType() == S6TestType.JUNIT) {
	 handleJunitTest(tc,idmap);
	 continue;
       }
      havetest = true;
      String fnm = tc.getName();
      if (!fnm.startsWith("test_")) fnm = "test_" + fnm;
      
      buf.append("\n\n");
      buf.append("@org.junit.Test public void " + fnm + "() throws Exception\n");
      buf.append("{\n");
      if (idmap.get("SEC_PREFIX") != null) buf.append(idmap.get("SEC_PREFIX"));
      
      buf.append(create);
      
      switch (tc.getTestType()) {
	 case USERCODE :
	    generateUserTest(tc,idmap,buf);
	    break;
	 case CALLS :
	    generateCallsTest(tc,idmap,buf);
	    break;
	 case JUNIT :
	    // shouldn't get here
	    break;
       }
      
      if (idmap.get("SEC_SUFFIX") != null)
	 buf.append(idmap.get("SEC_SUFFIX"));
      if (idmap.get("TEST_FINISHER") != null)
	 buf.append(idmap.get("TEST_FINISHER"));
      
      buf.append("}\n");
    }
   
   idmap.put("HAVETEST",Boolean.toString(havetest));
   idmap.put("TESTS",buf.toString());
}


private void setupTesting(Map<String,String> idmap)
{
   StringBuffer buf = new StringBuffer();
   
   String nm = for_request.getSignature().getClassSignature().getName();
   String pnm = for_request.getSignature().getName();
   String cnm = idmap.get("CLASS");
   String nm1 = pnm + "." + cnm + "." + nm;
   String nm2 = pnm + "." + cnm + "." + "S6TestFinisher";
   buf.append("@org.junit.runner.RunWith(org.junit.runners.Suite.class)\n");
   buf.append("@org.junit.runners.Suite.SuiteClasses({");
   buf.append(nm1 + ".class,");
   buf.append(nm2 + ".class,");
   buf.append("})\n");
   idmap.put("ANNOTATION",buf.toString());
}



@SuppressWarnings("unchecked")
private void setupSourceFile(Map<String,String> idmap)
{
   if (user_context == null) return;
   
   String cls = user_context.getContextClass();
   if (cls == null) return;
   
   String src = user_context.getSourceFile();
   if (src == null) return;
   
   CompilationUnit cu = JavaAst.parseSourceFile(src);
   if (cu == null) return;
   
   for (JcompType jt : java_fragment.getImportTypes()) {
      while (jt.isParameterizedType()) jt = jt.getBaseType();
      boolean fnd = false;
      for (Object o : cu.imports()) {
	 ImportDeclaration id = (ImportDeclaration) o;
	 if (id.isOnDemand()) continue;
	 String nm = id.getName().getFullyQualifiedName();
	 if (nm.equals(jt.getName())) fnd = true;
       }
      if (!fnd) {
	 AST ast = cu.getAST();
	 ImportDeclaration id = ast.newImportDeclaration();
	 id.setName(JavaAst.getQualifiedName(ast,jt.getName()));
	 cu.imports().add(id);
       }
    }
   
   AbstractTypeDeclaration typ = null;
   for (Iterator<?> it = cu.types().iterator(); it.hasNext() && typ == null; ) {
      AbstractTypeDeclaration atd = (AbstractTypeDeclaration) it.next();
      String tnm = atd.getName().getIdentifier();
      if (tnm.equals(cls)) typ = atd;
    }
   if (typ == null) return;
   
   S6Request.MethodSignature msg = null;
   switch (for_request.getSearchType()) {
      case METHOD :
	 msg = (S6Request.MethodSignature) for_request.getSignature();
	 break;
      default :
	 break;
    }
   
   List<ASTNode> decls = typ.bodyDeclarations();
   for (Iterator<ASTNode> it = decls.iterator(); it.hasNext(); ) {
      ASTNode hn = it.next();
      if (hn.getNodeType() == ASTNode.METHOD_DECLARATION && msg !=  null) {
	 MethodDeclaration md = (MethodDeclaration) hn;
	 String nm = md.getName().getIdentifier();
	 if (nm.equals(msg.getName())) {
	    it.remove();
          }
       }
    }
   
   for (ASTNode hn : java_fragment.getHelpers()) {
      ASTNode nhn = ASTNode.copySubtree(cu.getAST(),hn);
      decls.add(nhn);
    }
   ASTNode bn = java_fragment.getAstNode();
   ASTNode nbn = ASTNode.copySubtree(cu.getAST(),bn);
   
   decls.add(nbn);
   
   // TODO: Add imports here
   // TODO: handle jml here
   
   String rsrc = cu.toString();
   
   idmap.remove("CODE");
   
   idmap.put("SOURCECODE",rsrc);
   idmap.put("SOURCECLASS",cls);
}



/********************************************************************************/
/*                                                                              */
/*      Actually build and run the test                                         */
/*                                                                              */
/********************************************************************************/

private LimbaTestReport runJunitTest(Map<String,String> idmap) 
   throws LimbaException
{
   return null;
}

}       // end of class LimbaTester




/* end of LimbaTester.java */

