/********************************************************************************/
/*                                                                              */
/*              RunnerTestCase.java                                             */
/*                                                                              */
/*      Support code for testing solutions                                      */
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



package edu.brown.cs.limba.runner;

import org.junit.Assert;

public class RunnerTestCase extends Assert
{


/********************************************************************************/
/*										*/
/*	Not equal assertions							*/
/*										*/
/********************************************************************************/

public static void assertNotEquals(String msg,Object exp,Object act)
{
   if (exp == null && act != null) return;
   else if (exp != null && act == null) return;
   else if (exp == null && act == null) failNotEquals(msg,exp,act);
   else if (exp != null && exp.equals(act)) failNotEquals(msg,exp,act);
}


public static void assertNotEquals(String msg,double exp,double act,double delta)
{
   if (Double.isInfinite(exp)) {
      if ((exp == act))
	 failNotEquals(msg,Double.valueOf(exp),Double.valueOf(act));
    }
   else if (!(Math.abs(exp-act) > delta))
      failNotEquals(msg,Double.valueOf(exp),Double.valueOf(act));
}



public static void assertNotEquals(String msg,float exp,float act,float delta)
{
   if (Float.isInfinite(exp)) {
      if ((exp == act))
	 failNotEquals(msg,Float.valueOf(exp),Float.valueOf(act));
    }
   else if (!(Math.abs(exp-act) > delta))
      failNotEquals(msg,Float.valueOf(exp),Float.valueOf(act));
}


public static void assertNotEquals(String msg,long exp,long act)
{
   assertNotEquals(msg,Long.valueOf(exp),Long.valueOf(act));
}


public static void assertNotEquals(String msg,boolean exp,boolean act)
{
   assertNotEquals(msg,Boolean.valueOf(exp),Boolean.valueOf(act));
}


public static void assertNotEquals(String msg,byte exp,byte act)
{
   assertNotEquals(msg,Byte.valueOf(exp),Byte.valueOf(act));
}


public static void assertNotEquals(String msg,char exp,char act)
{
   assertNotEquals(msg,Character.valueOf(exp),Character.valueOf(act));
}


public static void assertNotEquals(String msg,short exp,short act)
{
   assertNotEquals(msg,Short.valueOf(exp),Short.valueOf(act));
}


public static void assertNotEquals(String msg,int exp,int act)
{
   assertNotEquals(msg,Integer.valueOf(exp),Integer.valueOf(act));
}

private static void failNotEquals(String msg,Object e,Object a)
{
   String f = "";
   if (msg != null) f = msg + " ";
   f += "expected not:<" + e + "> but was:<" + a + ">";
   fail(f);
}



/********************************************************************************/
/*										*/
/*	Additional equals assertions for handling arrays			*/
/*										*/
/********************************************************************************/
// 
// public static void assertEquals(String msg,Object [] exp,Object [] act)
// {
// assertArrayEquals(msg,exp,act);
// }



public static void assertEquals(String msg,double [] exp,double [] act,double delta)
{
   assertArrayEquals(msg,exp,act,delta);
}



public static void assertEquals(String msg,float [] exp,float [] act,float delta)
{
   assertArrayEquals(msg,exp,act,delta);
}



public static void assertEquals(String msg,int [] exp,int [] act)
{
   assertArrayEquals(msg,exp,act);
}



public static void assertEquals(String msg,short [] exp,short [] act)
{
   assertArrayEquals(msg,exp,act);
}



public static void assertEquals(String msg,byte [] exp,byte [] act)
{
   assertArrayEquals(msg,exp,act);
}



public static void assertEquals(String msg,char [] exp,char [] act)
{
   assertArrayEquals(msg,exp,act);
}



public static void assertEquals(String msg,long [] exp,long [] act)
{
   assertArrayEquals(msg,exp,act);
}



public static void assertEquals(String msg,double [][] exp,double [][] act,double delta)
{
   if (exp == null && act == null) return;
   else if (exp == null || act == null) failEquals(msg,exp,act);
   else if (exp.length != act.length) failEquals(msg,exp,act);
   else {
      for (int i = 0; i < exp.length; ++i) {
	 assertEquals(msg,exp[i],act[i],delta);
       }
    }
}



public static void assertEquals(String msg,float [][] exp,float [][] act,float delta)
{
   if (exp == null && act == null) return;
   else if (exp == null || act == null) failEquals(msg,exp,act);
   else if (exp.length != act.length) failEquals(msg,exp,act);
   else {
      for (int i = 0; i < exp.length; ++i) {
	 assertEquals(msg,exp[i],act[i],delta);
       }
    }
}





public static void assertNotEquals(String msg,double [] exp,double [] act,double delta)
{
   if (exp == null && act == null) return;
   else if (exp == null || act == null) failNotEquals(msg,exp,act);
   else if (exp.length != act.length) failNotEquals(msg,exp,act);
   else {
      for (int i = 0; i < exp.length; ++i) {
	 assertNotEquals(msg,exp[i],act[i],delta);
       }
    }
}



public static void assertNotEquals(String msg,float [] exp,float [] act,float delta)
{
   if (exp == null && act == null) return;
   else if (exp == null || act == null) failNotEquals(msg,exp,act);
   else if (exp.length != act.length) failNotEquals(msg,exp,act);
   else {
      for (int i = 0; i < exp.length; ++i) {
	 assertNotEquals(msg,exp[i],act[i],delta);
       }
    }
}



public static void assertNotEquals(String msg,int [] exp,int [] act)
{
   if (exp == null && act == null) return;
   else if (exp == null || act == null) failNotEquals(msg,exp,act);
   else if (exp.length != act.length) failNotEquals(msg,exp,act);
   else {
      for (int i = 0; i < exp.length; ++i) {
	 assertNotEquals(msg,exp[i],act[i]);
       }
    }
}



public static void assertNotEquals(String msg,short [] exp,short [] act)
{
   if (exp == null && act == null) return;
   else if (exp == null || act == null) failNotEquals(msg,exp,act);
   else if (exp.length != act.length) failNotEquals(msg,exp,act);
   else {
      for (int i = 0; i < exp.length; ++i) {
	 assertNotEquals(msg,exp[i],act[i]);
       }
    }
}



public static void assertNotEquals(String msg,byte [] exp,byte [] act)
{
   if (exp == null && act == null) return;
   else if (exp == null || act == null) failNotEquals(msg,exp,act);
   else if (exp.length != act.length) failNotEquals(msg,exp,act);
   else {
      for (int i = 0; i < exp.length; ++i) {
	 assertNotEquals(msg,exp[i],act[i]);
       }
    }
}



public static void assertNotEquals(String msg,char [] exp,char [] act)
{
   if (exp == null && act == null) return;
   else if (exp == null || act == null) failNotEquals(msg,exp,act);
   else if (exp.length != act.length) failNotEquals(msg,exp,act);
   else {
      for (int i = 0; i < exp.length; ++i) {
	 assertNotEquals(msg,exp[i],act[i]);
       }
    }
}



public static void assertNotEquals(String msg,long [] exp,long [] act)
{
   if (exp == null && act == null) return;
   else if (exp == null || act == null) failNotEquals(msg,exp,act);
   else if (exp.length != act.length) failNotEquals(msg,exp,act);
   else {
      for (int i = 0; i < exp.length; ++i) {
	 assertNotEquals(msg,exp[i],act[i]);
       }
    }
}



public static void assertNotEquals(String msg,double [][] exp,double [][] act,double delta)
{
   if (exp == null && act == null) return;
   else if (exp == null || act == null) failNotEquals(msg,exp,act);
   else if (exp.length != act.length) failNotEquals(msg,exp,act);
   else {
      for (int i = 0; i < exp.length; ++i) {
	 assertNotEquals(msg,exp[i],act[i],delta);
       }
    }
}



public static void assertNotEquals(String msg,float [][] exp,float [][] act,float delta)
{
   if (exp == null && act == null) return;
   else if (exp == null || act == null) failNotEquals(msg,exp,act);
   else if (exp.length != act.length) failNotEquals(msg,exp,act);
   else {
      for (int i = 0; i < exp.length; ++i) {
	 assertNotEquals(msg,exp[i],act[i],delta);
       }
    }
}



public static void assertNotEquals(String msg,int [][] exp,int [][] act)
{
   if (exp == null && act == null) return;
   else if (exp == null || act == null) failNotEquals(msg,exp,act);
   else if (exp.length != act.length) failNotEquals(msg,exp,act);
   else {
      for (int i = 0; i < exp.length; ++i) {
	 assertNotEquals(msg,exp[i],act[i]);
       }
    }
}



public static void assertNotEquals(String msg,short [][] exp,short [][] act)
{
   if (exp == null && act == null) return;
   else if (exp == null || act == null) failNotEquals(msg,exp,act);
   else if (exp.length != act.length) failNotEquals(msg,exp,act);
   else {
      for (int i = 0; i < exp.length; ++i) {
	 assertNotEquals(msg,exp[i],act[i]);
       }
    }
}



public static void assertNotEquals(String msg,byte [][] exp,byte [][] act)
{
   if (exp == null && act == null) return;
   else if (exp == null || act == null) failNotEquals(msg,exp,act);
   else if (exp.length != act.length) failNotEquals(msg,exp,act);
   else {
      for (int i = 0; i < exp.length; ++i) {
	 assertNotEquals(msg,exp[i],act[i]);
       }
    }
}



public static void assertNotEquals(String msg,char [][] exp,char [][] act)
{
   if (exp == null && act == null) return;
   else if (exp == null || act == null) failNotEquals(msg,exp,act);
   else if (exp.length != act.length) failNotEquals(msg,exp,act);
   else {
      for (int i = 0; i < exp.length; ++i) {
	 assertNotEquals(msg,exp[i],act[i]);
       }
    }
}



public static void assertNotEquals(String msg,long [][] exp,long [][] act)
{
   if (exp == null && act == null) return;
   else if (exp == null || act == null) failNotEquals(msg,exp,act);
   else if (exp.length != act.length) failNotEquals(msg,exp,act);
   else {
      for (int i = 0; i < exp.length; ++i) {
	 assertNotEquals(msg,exp[i],act[i]);
       }
    }
}



/********************************************************************************/
/*										*/
/*	Simplified assertions for handling real values				*/
/*										*/
/********************************************************************************/

public static void assertEquals(String msg,double exp,double act)
{
   assertEquals(msg,exp,act,1e-12);
}

public static void assertEquals(String msg,float exp,float act)
{
   assertEquals(msg,exp,act,1e-6f);
}

public static void assertNotEquals(String msg,double exp,double act)
{
   assertNotEquals(msg,exp,act,1e-12);
}

public static void assertNotEquals(String msg,float exp,float act)
{
   assertNotEquals(msg,exp,act,1e-6f);
}



public static void assertEquals(String msg,double [] exp,double [] act)
{
   assertEquals(msg,exp,act,1e-12);
}

public static void assertEquals(String msg,float [] exp,float [] act)
{
   assertEquals(msg,exp,act,1e-6f);
}

public static void assertNotEquals(String msg,double [] exp,double [] act)
{
   assertNotEquals(msg,exp,act,1e-12);
}

public static void assertNotEquals(String msg,float [] exp,float [] act)
{
   assertNotEquals(msg,exp,act,1e-6f);
}



public static void assertEquals(String msg,double [][] exp,double [][] act)
{
   assertEquals(msg,exp,act,1e-12);
}

public static void assertEquals(String msg,float [][] exp,float [][] act)
{
   assertEquals(msg,exp,act,1e-6f);
}

public static void assertNotEquals(String msg,double [][] exp,double [][] act)
{
   assertNotEquals(msg,exp,act,1e-12);
}

public static void assertNotEquals(String msg,float [][] exp,float [][] act)
{
   assertNotEquals(msg,exp,act,1e-6f);
}




/********************************************************************************/
/*										*/
/*	Utility routines for helping with equals/not equals output		*/
/*										*/
/********************************************************************************/

private static void failEquals(String message, Object expected, Object actual)
{
   String formatted= "";
   if (message != null) formatted= message+" ";
   fail(formatted+"expected:<"+expected+"> but was:<"+actual+">");
}



}       // end of class RunnerTestCase




/* end of RunnerTestCase.java */

