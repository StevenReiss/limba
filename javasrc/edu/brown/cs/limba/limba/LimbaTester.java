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

import java.util.HashMap;
import java.util.Map;

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
   String pkgfix = null;
   idmap.put("PREFIX",clsname);
   
   setupTestPackage(idmap);
   
   StringBuffer imports = new StringBuffer();
   for (String jt : for_solution.getImportTypes()) {
      imports.append("import " + jt + ";\n");
    }
   for (String s : limba_finder.getTests().getImportTypes()) {
      imports.append("import " + s + ";\n");
    }
   if (user_context != null) {
      for (String s : user_context.getContextImports()) {
	 imports.append("import " + s + ";\n");
       }
    }
   idmap.put("IMPORTS",imports.toString());
   
   setupTests(idmap);
   
   JavaAstClassName cn = null;
   if (pkgfix != null) {
      JavaAst.mapPackageNames(java_fragment.getAstNode(),pkgfix,idmap.get("PACKAGE"));
    }
   else {
      cn = for_solution.getClassNamer();
      if (cn != null) {
	 String cnm = idmap.get("PACKAGEDOT") + clsname;
	 cn.setClassName(cnm,clsname);
       }
    }
   
   setupCode(idmap);
   
   setupSourceFile(idmap);
}



private void setupUserContext(Map<String,String> idmap) throws S6Exception
{
   if (user_context == null) return;
   
   String s = for_request.getPackage();
   if (s == null) s = user_context.getContextPackage();
   if (s != null) idmap.put("PACKAGE",s);
   else idmap.put("PACKAGE","");
   
   String cls = user_context.getContextClass();
   if (cls != null) {
      String fcls;
      fcls = cls;
      idmap.put("TESTCLASS",fcls);
      idmap.put("PREFIX",fcls);
    }
   
   String jnm = user_context.getJarFileName();
   if (jnm != null) idmap.put("S6CTX",jnm);
   
   File cdir = user_context.getContextDirectory();
   if (cdir == null) return;
   
   StringBuffer buf = new StringBuffer();
   StringBuffer ebuf = new StringBuffer();
   for (S6Context.UserFile uf : user_context.getUserFiles()) {
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
   idmap.put("S6_CONTEXT_MAP",ebuf.toString());
   // System.err.println("CONTEXT SETUP: ANT = " + buf.toString());
   // System.err.println("CONTEXT SETUP: S6 = " + ebuf.toString());
}




private void setupTestPackage(Map<String,String> idmap) throws S6Exception
{
   File root = new File(System.getProperty("java.io.tmpdir") + File.separator + S6_TEST_DIR);
   if (!root.exists() && !root.mkdir())
      throw new S6Exception("Can't create S6 test directory: " + root);
   idmap.put("ROOT",root.getPath());
   
   String pkg = null;
   Random r = new Random();
   File dir = null;
   for (int i = 0; i < 1000; ++i) {
      pkg = S6_PACKAGE_PREFIX + r.nextInt(131256);
      dir = new File(root.getPath() + File.separator + pkg);
      if (dir.exists()) continue;
      if (dir.mkdir()) break;
      dir = null;
    }
   
   if (dir == null) throw new S6Exception("S6 test directory not created");
   
   idmap.put("DIRECTORY",dir.getPath());
   if (idmap.get("PACKAGE") == null) idmap.put("PACKAGE",pkg);
   idmap.put("SRCDIR",dir.getPath());
   idmap.put("PROJECTNAME",pkg);
   
   File sf1 = new File(dir,"S6SOURCE");
   try {
      PrintWriter fw = new PrintWriter(new FileWriter(sf1));
      try {
	 fw.println(for_source.getDisplayName());
	 fw.println(for_source.getName());
	 fw.println(for_source.getProjectId());
       }
      catch (Throwable t) { }
      fw.close();
    }
   catch (IOException e) { }
   
   File bin = new File(dir,S6_BINARY_DIR);
   if (for_request.getSearchType() == S6SearchType.ANDROIDUI) {
      File f1 = new File(dir,"src");
      File f2 = new File(f1,"s6");
      File f3 = new File(f2,pkg);
      idmap.put("SRCDIR",f3.getPath());
      if (!f3.mkdirs()) {
	 System.err.println("Problem creating source subdirectory: " + f3);
       }
      idmap.put("PACKAGE","s6." + pkg);
      bin = new File(bin,"classes");
    }
   
   if (!bin.mkdirs()) {
      System.err.println("Problem creating binary subdirectory: " + bin);
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
   JavaContracts jc = new JavaContracts(for_request.getContracts(),java_fragment);
   if (jc.insertContracts()) idmap.put("ANTRUN","jmltest");
   
   String gencode = "";
   switch (for_request.getSearchType()) {
      case METHOD :
      case CLASS :
      case FULLCLASS :
      case TESTCASES :
	 gencode = java_fragment.getText();
	 break;
      case PACKAGE :
      case APPLICATION :
      case UIFRAMEWORK :
      case ANDROIDUI :
	 break;
    }
   
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
/*	Methods to generate static initializers 				*/
/*										*/
/********************************************************************************/

private void setupUIHierarchy(Map<String,String> idmap)
{
   S6Request.UISignature usg = (S6Request.UISignature) for_request.getSignature();
   
   StringBuffer buf = new StringBuffer();
   buf.append("private edu.brown.cs.s6.runner.RunnerS6HierData [] s6_hier_data = new edu.brown.cs.s6.runner.RunnerS6HierData[] {\n");
   addUIComponent(buf,usg.getHierarchy());
   buf.append("};\n");
   
   idmap.put("STATICS",buf.toString());
}


private void addUIComponent(StringBuffer buf,S6Request.UIComponent c)
{
   buf.append("new edu.brown.cs.s6.runner.RunnerS6HierData(");
   addString(buf,c.getId());
   buf.append(",");
   buf.append(c.getXposition());
   buf.append(",");
   buf.append(c.getYposition());
   buf.append(",");
   buf.append(c.getWidth());
   buf.append(",");
   buf.append(c.getHeight());
   buf.append(",\"");
   for (String s : c.getTypes()) {
      buf.append(s);
      buf.append(",");
    }
   buf.append("\",");
   addComp(buf,c.getTopAnchor());
   buf.append(",");
   addComp(buf,c.getBottomAnchor());
   buf.append(",");
   addComp(buf,c.getLeftAnchor());
   buf.append(",");
   addComp(buf,c.getRightAnchor());
   buf.append(",");
   addString(buf,c.getData());
   buf.append(",");
   List<S6Request.UIComponent> ch = c.getChildren();
   if (ch == null) {
      buf.append("0),\n");
    }
   else {
      buf.append(ch.size());
      buf.append("),\n");
      for (S6Request.UIComponent cc : ch) addUIComponent(buf,cc);
    }
}


private void addComp(StringBuffer buf,S6Request.UIComponent c)
{
   if (c == null) addString(buf,null);
   else addString(buf,c.getId());
}


private void addString(StringBuffer buf,String s)
{
   if (s == null) buf.append("null");
   else {
      buf.append("\"");
      buf.append(s);
      buf.append("\"");
    }
}




/********************************************************************************/
/*										*/
/*	Methods to actually generate test code					*/
/*										*/
/********************************************************************************/

private void setupTests(Map<String,String> idmap) throws S6Exception
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

