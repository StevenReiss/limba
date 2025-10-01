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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.w3c.dom.Element;

import edu.brown.cs.ivy.exec.IvyExec;
import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.ivy.file.IvyLog;
import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.xml.IvyXml;

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
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

Collection<LimbaTestCase> getTestCases()
{
   return limba_finder.getTestCases();
}



/********************************************************************************/
/*                                                                              */
/*      Run and check the test                                                  */
/*                                                                              */
/********************************************************************************/

LimbaSuiteReport runTester()
{
   Map<String,String> idmap = new HashMap<>();
   
   try {
      setupForTesting(idmap);
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

private void setupForTesting(Map<String,String> idmap)
   throws LimbaException
{
   idmap.put("CLASS",LIMBA_TEST_CLASS);
   idmap.put("SOURCEFILE",LIMBA_TEST_CLASS + ".java");
   idmap.put("JUNITCP",JUNIT_CLASSPATH);
   idmap.put("JUNIT",JUNIT_RUNNER); 
   idmap.put("JUNITOUT",JUNIT_OUT);
   idmap.put("TESTCLASS",LIMBA_USER_CLASS);
   idmap.put("IVY",IVY_CLASSPATH); 
   idmap.put("LIMBACLS",LIMBA_CLASSPATH); 
   
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
      importstr.append("import " + s + ";\n");
    }
   idmap.put("IMPORTS",importstr.toString());
   
   setupTests(idmap);
   
   setupCode(idmap);
   
   setupSourceFile(idmap);
}



private void setupUserContext(Map<String,String> idmap) throws LimbaException
{
   LimbaFindContext ctx = limba_finder.getFindContext(); 
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
   if (jnm != null) {
      idmap.put("LIMBACTX",jnm);
      idmap.put("CTXPATH","<pathelement location='" + jnm + "' />");
    }
   
   File cdir = ctx.getContextDirectory();
   if (cdir == null) return; 
}




private void setupTestPackage(Map<String,String> idmap) throws LimbaException
{
   File f1 = new File(System.getProperty("java.io.tmpdir"));
   File root = new File(f1,LIMBA_TEST_DIR);
   if (!root.exists() && !root.mkdir())
      throw new LimbaException("Can't create Limba test directory: " + root);
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
   
   if (dir == null) throw new LimbaException("Limba test directory not created");
   
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
   gencode = for_solution.getText();
   
   if (for_solution.getFindType() == LimbaFindType.METHOD) {
      gencode = "private static class " + idmap.get("TESTCLASS") + " {\n\n" + gencode;
      gencode += "\n}\t//end of class " + idmap.get("TESTCLASS") + "\n";
    }
   
   idmap.put("CODE",gencode);
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
   
   if (for_solution.getUseConstructor()) {
      create = idmap.get("TESTCLASS") + " __object = new " + idmap.get("TESTCLASS") + "();\n";
      idmap.put("PREFIX","__object");
      idmap.put("SETUP",create);
    }
   
   boolean havetest = false;
   for (LimbaTestCase tc : limba_finder.getTestCases()) { 
      if (tc.getTestType() == LimbaTestType.JUNIT) {
	 handleJunitTest(tc,idmap);
	 continue;
       }
      havetest = true;
      String fnm = tc.getName();
      if (!fnm.startsWith("test_")) fnm = "test_" + fnm;
      
      buf.append("\n\n");
      buf.append("@org.junit.Test public void " + fnm + "() throws Exception {\n");
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
      
      buf.append("}\n");
    }
   
   idmap.put("HAVETEST",Boolean.toString(havetest));
   idmap.put("TESTS",buf.toString());
}



private void setupSourceFile(Map<String,String> idmap)
{
   LimbaFindContext ctx = limba_finder.getFindContext();
   if (ctx == null) return;
   
   String cls = ctx.getClassName();
   if (cls == null) return;
   
   String src = ctx.getSourceFileName(); 
   if (src == null) return;
   
   CompilationUnit cu = JcompAst.parseSourceFile(src);
   if (cu == null) return;
   
// for (JcompType jt : for_solution.getImportTypes()) {
//    while (jt.isParameterizedType()) jt = jt.getBaseType();
//    boolean fnd = false;
//    for (Object o : cu.imports()) {
// 	 ImportDeclaration id = (ImportDeclaration) o;
// 	 if (id.isOnDemand()) continue;
// 	 String nm = id.getName().getFullyQualifiedName();
// 	 if (nm.equals(jt.getName())) fnd = true;
//     }
//    if (!fnd) {
// 	 AST ast = cu.getAST();
// 	 ImportDeclaration id = ast.newImportDeclaration();
// 	 id.setName(JavaAst.getQualifiedName(ast,jt.getName()));
// 	 cu.imports().add(id);
//     }
//  }
// 
// AbstractTypeDeclaration typ = null;
// for (Iterator<?> it = cu.types().iterator(); it.hasNext() && typ == null; ) {
//    AbstractTypeDeclaration atd = (AbstractTypeDeclaration) it.next();
//    String tnm = atd.getName().getIdentifier();
//    if (tnm.equals(cls)) typ = atd;
//  }
// if (typ == null) return;
// 
// LimbaRequest.MethodSignature msg = null;
// switch (for_request.getSearchType()) {
//    case METHOD :
// 	 msg = (LimbaRequest.MethodSignature) for_request.getSignature();
// 	 break;
//    default :
// 	 break;
//  }
// 
// List<ASTNode> decls = typ.bodyDeclarations();
// for (Iterator<ASTNode> it = decls.iterator(); it.hasNext(); ) {
//    ASTNode hn = it.next();
//    if (hn.getNodeType() == ASTNode.METHOD_DECLARATION && msg !=  null) {
// 	 MethodDeclaration md = (MethodDeclaration) hn;
// 	 String nm = md.getName().getIdentifier();
// 	 if (nm.equals(msg.getName())) {
// 	    it.remove();
//        }
//     }
//  }
// 
// for (ASTNode hn : for_solution.getHelpers()) {
//    ASTNode nhn = ASTNode.copySubtree(cu.getAST(),hn);
//    decls.add(nhn);
//  }
// ASTNode bn = for_solution.getAstNode();
// ASTNode nbn = ASTNode.copySubtree(cu.getAST(),bn);
// 
// decls.add(nbn);
// 
// 
// String rsrc = cu.toString();
// 
// idmap.remove("CODE");
// 
// idmap.put("SOURCECODE",rsrc);
// idmap.put("SOURCECLASS",cls);
}


private void generateUserTest(LimbaTestCase tc,Map<String,String> idmap,StringBuffer buf)
{
   buf.append(expandCode(tc.getUserCode(),idmap));
}



private void generateCallsTest(LimbaTestCase tc,Map<String,String> idmap,StringBuffer buf)
{
   String uc = tc.getUserCode();
   if (uc != null) {
      buf.append(expandCode(uc,idmap));
      buf.append("\n");
    }
   
   for (LimbaTestCase.CallTest ct : tc.getCalls()) {
      for (LimbaTestCase.CallArg ca : ct.getArguments()) {
	 String s = ca.getArgCode();
	 if (s != null) buf.append(expandCode(s,idmap));
       }
      LimbaTestCase.CallArg cr = ct.getReturnValue();
      if (cr != null && cr.getArgCode() != null) {
	 buf.append(cr.getArgCode());
	 buf.append("\n");
       }
      
      LimbaTestOp op = ct.getOperator();
      switch (op) {
	 case SAVE :
	    if (cr != null) {
	       buf.append(cr.getArgValue());
	       buf.append(" = ");
	     }
	    break;
	 case NONE :
	 case IGNORE :
	    break;
	 case EQL :
	    buf.append("assertEquals(\"Result of call\",");
	    buf.append(codeString(cr));
	    buf.append(",");
	    break;
	 case NEQ :
	    buf.append("assertNotEquals(\"Result of call\",");
	    buf.append(codeString(cr));
	    buf.append(",");
	    break;
	 case SAME :
	    buf.append("assertSame(");
	    buf.append(codeString(cr));
	    buf.append(",");
	    break;
	 case DIFF :
	    buf.append("assertNotSame(");
	    buf.append(codeString(cr));
	    buf.append(",");
	    break;
	 case THROW :
	    buf.append("try {\n");
	    break;
       }
      
      String mthd = ct.getMethod();
      String fld = null;
      if (ct.isConstructor()) buf.append("new ");
      else if (ct.isAccess()) {
	 fld = mthd;
	 mthd = null;
	 if (fld == null) fld = "$(PREFIX)";
	 fld = expandCode(fld,idmap);
       }
      else {
	 int idx = mthd.indexOf(".");
	 if (idx < 0) mthd = "$(PREFIX)." + mthd;
	 mthd = expandCode(mthd,idmap);
       }
      if (mthd != null) {
	 buf.append(mthd);
	 buf.append("(");
	 int i = 0;
	 for (LimbaTestCase.CallArg ca : ct.getArguments()) {
	    if (i++ != 0) buf.append(",");
	    buf.append(codeString(ca));
	  }
	 buf.append(")");
       }
      else {
	 if (fld != null) buf.append("(" + fld + ")");
	 for (LimbaTestCase.CallArg ca : ct.getArguments()) {
	    buf.append(",");
	    buf.append(codeString(ca));
	  }
       }
      
      switch (op) {
	 default :
	    buf.append(";\n");
	    break;
	 case EQL :
	 case NEQ :
	 case SAME :
	 case DIFF :
	    buf.append(");\n");
	    break;
	 case THROW :
	    String ex = ct.getThrows();
	    if (ex == null) ex = "java.lang.Throwable";
	    buf.append(";\nfail(\"Exception " + ex + " expected\");\n");
	    buf.append("}\n");
	    buf.append("catch (junit.framework.AssertionFailedError __e) { throw __e; }\n");
	    buf.append("catch (" + ex + " __e) { }\n");
	    break;
       }
    }
}


private void handleJunitTest(LimbaTestCase tc,Map<String,String> idmap)
{
   String tc1 = idmap.get("TESTCLASSES");
   String tc2 = idmap.get("TESTCASES");
   
   if (tc1 == null) {
      idmap.put("ANNOTATION","@org.junit.runner.RunWith(LimbaTestClass.LimbaTestSelectRunner.class)");
      tc1 = "";
      tc2 = "";
    }
   tc1 += "\"" + tc.getJunitClass() + "\", ";
   tc2 += "\"" + tc.getJunitName() + "\", ";
   idmap.put("TESTCLASSES",tc1);
   idmap.put("TESTCASES",tc2);
}



private String codeString(LimbaTestCase.CallArg ca)
{
   String r = null;
   
   if (ca == null) return null;
   
   switch (ca.getArgType()) {
      default :
      case LITERAL :
      case VARIABLE :
      case SAVE :
	 r = ca.getArgValue();
	 break;
      case STRING :
	 String s = ca.getArgValue();
	 if (s == null) s = "";
	 StringBuffer buf = new StringBuffer();
	 buf.append("\"");
	 for (int i = 0; i < s.length(); ++i) {
	    char c = s.charAt(i);
	    if (c == '\n') buf.append("\\n");
	    else if (c == '\t') buf.append("\\t");
	    else {
	       if (c == '"') buf.append("\\");
	       buf.append(c);
	     }
	  }
	 buf.append("\"");
	 r = buf.toString();
	 break;
    }
   
   return r;
}

private String expandCode(String s,Map<String,String> idmap)
{
   if (s == null) return "";
   
   int idx = s.indexOf("$(");
   if (idx < 0) return s;
   s = IvyFile.expandName(s,idmap);
   return s;
}



/********************************************************************************/
/*                                                                              */
/*      Actually build and run the test                                         */
/*                                                                              */
/********************************************************************************/

private LimbaSuiteReport runJunitTest(Map<String,String> idmap) 
   throws LimbaException
{
   try {
      produceTestFile(idmap);
      produceSourceFile(idmap);
      produceAntFile(idmap);		// should be last
      compileAndRunTestFile(idmap);
      LimbaSuiteReport sr = readTestStatus(idmap);
      // test passes -- set final class name
      // should use name in request
//    if (cn != null) cn.setClassName(idmap.get("PACKAGEDOT") + Limba_TEST_CLASS,clsname);
      return sr;
    }
   finally {
//    if (pkgfix != null) {
// 	 JavaAst.mapPackageNames(java_fragment.getAstNode(),idmap.get("PACKAGE"),pkgfix);
//     }
//    else {
// 	 if (cn != null) cn.resetClassName();
//     }
      clear(idmap);
      for_solution.clearResolve(); 
    }
}


private void produceTestFile(Map<String,String> idmap) throws LimbaException
{
   String dir = idmap.get("SRCDIR");
   File f1 = new File(dir);
   File f = new File(f1,LIMBA_TEST_CLASS + ".java");
   
   try (InputStream ins = this.getClass().getClassLoader().getResourceAsStream(JAVA_TEST_PROTO)) {
      try (BufferedReader br = new BufferedReader(new InputStreamReader(ins))) {
         try (PrintWriter pw = new PrintWriter(new FileWriter(f))) {
            for ( ; ; ) {
               String ln = br.readLine();
               if (ln == null) break;
               ln = IvyFile.expandName(ln,idmap);
               pw.println(ln);
             }
          }
       }
    }
   catch (IOException e) {
      throw new LimbaException("Problem creating test file: " + e);
    }
}



private void produceSourceFile(Map<String,String> idmap) throws LimbaException
{
   String cls = idmap.get("SOURCECLASS");
   if (cls == null) return;
   
   String dir = idmap.get("SRCDIR");
   File f = new File(dir,cls + ".java");
   
   try {
      FileWriter fw = new FileWriter(f);
      fw.write(idmap.get("SOURCECODE"));
      fw.close();
    }
   catch (IOException e) {
      throw new LimbaException("Problem creating source file: " + e);
    }
}



private void produceAntFile(Map<String,String> idmap) throws LimbaException
{
   String dir = idmap.get("DIRECTORY");
   File f = new File(dir + File.separator + ANT_FILE);
   String proto = JAVA_ANT_PROTO;
   try (InputStream ins = getClass().getClassLoader().getResourceAsStream(JAVA_ANT_PROTO)) {
      try (BufferedReader fr = new BufferedReader(new InputStreamReader(ins));
         PrintWriter pw = new PrintWriter(new FileWriter(f))) {
         for ( ; ; ) {
            String ln = fr.readLine();
            if (ln == null) break;
            ln = IvyFile.expandName(ln,idmap);
            pw.println(ln);
          }
       }
    }
   catch (IOException e) {
      throw new LimbaException("Problem creating ant file: " + e,e);
    }
}


/********************************************************************************/
/*										*/
/*	Methods for using ant to run junit					*/
/*										*/
/********************************************************************************/

private void compileAndRunTestFile(Map<String,String> idmap) throws LimbaException
{
   String [] env = null;
   try {
      String cmd = ANT_COMMAND + " $(DIRECTORY)";
      cmd = IvyFile.expandName(cmd,idmap);
      
      int fgs = IvyExec.IGNORE_OUTPUT;
      IvyLog.logD("LIMBA","RUN ANT: " + cmd);
      
      IvyExec ex = new IvyExec(cmd,env,fgs);
      
      ex.waitFor();
      
      // if (sts != 0) throw new LimbaException("Error running ant (" + sts + ")");
    }
   catch (IOException e) {
      throw new LimbaException("Problem running ant: " + e,e);
    }
}


private LimbaSuiteReport readTestStatus(Map<String,String> idmap) throws LimbaException
{
   LimbaSuiteReport sr = new LimbaSuiteReport(limba_finder,idmap); 
   
   String onm = IvyFile.expandName("$(DIRECTORY)/$(JUNITOUT)",idmap);
   File f = new File(onm);
   if (!f.exists()) throw new LimbaException("Junit failed: " + f);
   File jarf = IvyFile.expandFile("$(DIRECTORY)/$(UIJAR)",idmap);
   
   Element e = IvyXml.loadXmlFromFile(onm);
   if (e == null) throw new LimbaException("No junit output found in " + onm);
   
   int tct = 0;
   for (Element te : IvyXml.elementsByTag(e,"testcase")) {
      boolean iserr = false;
      String cnm = IvyXml.getAttrString(te,"classname");
      String nm = IvyXml.getAttrString(te,"name");
      String msg = null;
      double tm = IvyXml.getAttrDouble(te,"time");
      Element ee = IvyXml.getElementByTag(te,"error");
      if (ee != null) iserr = true;
      else ee = IvyXml.getElementByTag(te,"failure");
      
      if (ee != null) {
	 msg = IvyXml.getAttrString(ee,"message");
	 if (msg == null) msg = IvyXml.getText(ee);
	 if (msg == null) msg = "UNKNOWN ERROR";
       }
      ++tct;
      
      sr.addReport(nm,cnm,tm,msg,iserr,jarf);
    }
   
   if (tct == 0) throw new LimbaException("No test case output found in " + onm);
   
   return sr;
}


private void clear(Map<String,String> idmap)
{
// try {
//    String dnm = IvyFile.expandName("$(DIRECTORY)",idmap);
//    IvyFile.remove(dnm);
//  }
// catch (IOException e) { }
}



}       // end of class LimbaTester




/* end of LimbaTester.java */

