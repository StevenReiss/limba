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
/*                                                                              */
/*      Default constants                                                       */
/*                                                                              */
/********************************************************************************/

private static final double DEFAULT_DELTA = 1e-10;
private static final float DEFAULT_FLOAT_DELTA = 1e-6f;



/********************************************************************************/
/*                                                                              */
/*      Standard assertions                                                     */
/*                                                                              */
/********************************************************************************/

public static void limbaAssertEquals(String msg,long exp,long act)
{
   if (exp != act) {
      limbaFailEquals(msg,exp,act);
    }
}


public static void limbaAssertEquals(String msg,Object exp,Object act)
{
   if (exp == null && act == null) return;
   if (exp != null && act != null && exp.equals(act)) return;
   limbaFailEquals(msg,exp,act);
}


public static void limbaAssertEquals(String msg,int exp,int act)
{
   if (exp != act) {
      limbaFailEquals(msg,exp,act);
    }
}


public static void limbaAssertEquals(String msg,byte exp,byte act)
{
   if (exp != act) {
      limbaFailEquals(msg,exp,act);
    }
}


public static void limbaAssertEquals(String msg,char exp,char act)
{
   if (exp != act) {
      limbaFailEquals(msg,exp,act);
    }
}


public static void limbaAssertEquals(String msg,boolean exp,boolean act)
{
   if (exp != act) {
      limbaFailEquals(msg,exp,act);
    }
}


public static void limbaAssertEquals(String msg,short exp,short act)
{
   if (exp != act) {
      limbaFailEquals(msg,exp,act);
    }
}



/********************************************************************************/
/*										*/
/*	Not equal assertions							*/
/*										*/
/********************************************************************************/

public static void limbaAssertNotEquals(String msg,Object exp,Object act)
{
   if (exp == null && act != null) return;
   else if (exp != null && act == null) return;
   else if (exp == null && act == null) limbaFailNotEquals(msg,exp,act);
   else if (exp != null && exp.equals(act)) limbaFailNotEquals(msg,exp,act);
}


public static void limbaAssertNotEquals(String msg,double exp,double act,double delta)
{
   if (Double.isInfinite(exp)) {
      if ((exp == act)) {
         limbaFailNotEquals(msg,Double.valueOf(exp),Double.valueOf(act));
       }
    }
   else if (!(Math.abs(exp-act) > delta)) {
      limbaFailNotEquals(msg,Double.valueOf(exp),Double.valueOf(act));
    }
}



public static void limbaAssertNotEquals(String msg,float exp,float act,float delta)
{
   if (Float.isInfinite(exp)) {
      if ((exp == act)) {
         limbaFailNotEquals(msg,Float.valueOf(exp),Float.valueOf(act));
       }
    }
   else if (!(Math.abs(exp-act) > delta)) {
      limbaFailNotEquals(msg,Float.valueOf(exp),Float.valueOf(act));
    }
}


public static void limbaAssertNotEquals(String msg,long exp,long act)
{
   if (exp != act) {
      limbaFailNotEquals(msg,exp,act);
    }
}


public static void limbaAssertNotEquals(String msg,boolean exp,boolean act)
{
   if (exp != act) {
      limbaFailNotEquals(msg,exp,act);
    }
}


public static void limbaAssertNotEquals(String msg,byte exp,byte act)
{
   if (exp != act) {
      limbaFailNotEquals(msg,exp,act);
    }
}


public static void limbaAssertNotEquals(String msg,char exp,char act)
{
   if (exp != act) {
      limbaFailNotEquals(msg,exp,act);
    }
}


public static void limbaAssertNotEquals(String msg,short exp,short act)
{
   if (exp != act) {
      limbaFailNotEquals(msg,exp,act);
    }
}


public static void limbaAssertNotEquals(String msg,int exp,int act)
{
   if (exp != act) {
      limbaFailNotEquals(msg,exp,act);
    }
}


/********************************************************************************/
/*										*/
/*	Additional equals assertions for handling arrays			*/
/*										*/
/********************************************************************************/

public static void limbaAssertEquals(String msg,Object [] exp,Object [] act)
{
   if (exp == null && act == null) return;
   else if (exp == null || act == null) limbaFailEquals(msg,exp,act);
   else if (exp.length != act.length) limbaFailEquals(msg,exp,act);
   else {
      for (int i = 0; i < exp.length; ++i) {
	 limbaAssertEquals(msg,exp[i],act[i]);
       }
    }
}

public static void limbaAssertEquals(String msg,double [] exp,double [] act,double delta)
{
   if (exp == null && act == null) return;
   else if (exp == null || act == null) limbaFailEquals(msg,exp,act);
   else if (exp.length != act.length) limbaFailEquals(msg,exp,act);
   else {
      for (int i = 0; i < exp.length; ++i) {
	 limbaAssertEquals(msg,exp[i],act[i],delta);
       }
    }
}



public static void limbaAssertEquals(String msg,float [] exp,float [] act,float delta)
{
   if (exp == null && act == null) return;
   else if (exp == null || act == null) limbaFailEquals(msg,exp,act);
   else if (exp.length != act.length) limbaFailEquals(msg,exp,act);
   else {
      for (int i = 0; i < exp.length; ++i) {
	 limbaAssertEquals(msg,exp[i],act[i],delta);
       }
    }
}



public static void limbaAssertEquals(String msg,int [] exp,int [] act)
{
   if (exp == null && act == null) return;
   else if (exp == null || act == null) limbaFailEquals(msg,exp,act);
   else if (exp.length != act.length) limbaFailEquals(msg,exp,act);
   else {
      for (int i = 0; i < exp.length; ++i) {
	 limbaAssertEquals(msg,exp[i],act[i]);
       }
    }
}



public static void limbaAssertEquals(String msg,short [] exp,short [] act)
{
   if (exp == null && act == null) return;
   else if (exp == null || act == null) limbaFailEquals(msg,exp,act);
   else if (exp.length != act.length) limbaFailEquals(msg,exp,act);
   else {
      for (int i = 0; i < exp.length; ++i) {
	 limbaAssertEquals(msg,exp[i],act[i]);
       }
    }
}



public static void limbaAssertEquals(String msg,byte [] exp,byte [] act)
{
   if (exp == null && act == null) return;
   else if (exp == null || act == null) limbaFailEquals(msg,exp,act);
   else if (exp.length != act.length) limbaFailEquals(msg,exp,act);
   else {
      for (int i = 0; i < exp.length; ++i) {
	 limbaAssertEquals(msg,exp[i],act[i]);
       }
    }
}



public static void limbaAssertEquals(String msg,char [] exp,char [] act)
{
   if (exp == null && act == null) return;
   else if (exp == null || act == null) limbaFailEquals(msg,exp,act);
   else if (exp.length != act.length) limbaFailEquals(msg,exp,act);
   else {
      for (int i = 0; i < exp.length; ++i) {
	 limbaAssertEquals(msg,exp[i],act[i]);
       }
    }
}



public static void limbaAssertEquals(String msg,long [] exp,long [] act)
{
   if (exp == null && act == null) return;
   else if (exp == null || act == null) limbaFailEquals(msg,exp,act);
   else if (exp.length != act.length) limbaFailEquals(msg,exp,act);
   else {
      for (int i = 0; i < exp.length; ++i) {
	 limbaAssertEquals(msg,exp[i],act[i]);
       }
    }
}



public static void limbaAssertEquals(String msg,double [][] exp,double [][] act,double delta)
{
   if (exp == null && act == null) return;
   else if (exp == null || act == null) limbaFailEquals(msg,exp,act);
   else if (exp.length != act.length) limbaFailEquals(msg,exp,act);
   else {
      for (int i = 0; i < exp.length; ++i) {
	 limbaAssertEquals(msg,exp[i],act[i],delta);
       }
    }
}


public static void limbaAssertEquals(String msg,float [][] exp,float [][] act,float delta)
{
   if (exp == null && act == null) return;
   else if (exp == null || act == null) limbaFailEquals(msg,exp,act);
   else if (exp.length != act.length) limbaFailEquals(msg,exp,act);
   else {
      for (int i = 0; i < exp.length; ++i) {
	 limbaAssertEquals(msg,exp[i],act[i],delta);
       }
    }
}


public static void limbaAssertNotEquals(String msg,double [] exp,double [] act,double delta)
{
   if (exp == null && act == null) return;
   else if (exp == null || act == null) limbaFailNotEquals(msg,exp,act);
   else if (exp.length != act.length) limbaFailNotEquals(msg,exp,act);
   else {
      for (int i = 0; i < exp.length; ++i) {
	 limbaAssertNotEquals(msg,exp[i],act[i],delta);
       }
    }
}


public static void limbaAssertNotEquals(String msg,float [] exp,float [] act,float delta)
{
   if (exp == null && act == null) return;
   else if (exp == null || act == null) limbaFailNotEquals(msg,exp,act);
   else if (exp.length != act.length) limbaFailNotEquals(msg,exp,act);
   else {
      for (int i = 0; i < exp.length; ++i) {
	 limbaAssertNotEquals(msg,exp[i],act[i],delta);
       }
    }
}



public static void limbaAssertNotEquals(String msg,int [] exp,int [] act)
{
   if (exp == null && act == null) return;
   else if (exp == null || act == null) limbaFailNotEquals(msg,exp,act);
   else if (exp.length != act.length) limbaFailNotEquals(msg,exp,act);
   else {
      for (int i = 0; i < exp.length; ++i) {
	 limbaAssertNotEquals(msg,exp[i],act[i]);
       }
    }
}



public static void limbaAssertNotEquals(String msg,short [] exp,short [] act)
{
   if (exp == null && act == null) return;
   else if (exp == null || act == null) limbaFailNotEquals(msg,exp,act);
   else if (exp.length != act.length) limbaFailNotEquals(msg,exp,act);
   else {
      for (int i = 0; i < exp.length; ++i) {
	 limbaAssertNotEquals(msg,exp[i],act[i]);
       }
    }
}



public static void limbaAssertNotEquals(String msg,byte [] exp,byte [] act)
{
   if (exp == null && act == null) return;
   else if (exp == null || act == null) limbaFailNotEquals(msg,exp,act);
   else if (exp.length != act.length) limbaFailNotEquals(msg,exp,act);
   else {
      for (int i = 0; i < exp.length; ++i) {
	 limbaAssertNotEquals(msg,exp[i],act[i]);
       }
    }
}



public static void limbaAssertNotEquals(String msg,char [] exp,char [] act)
{
   if (exp == null && act == null) return;
   else if (exp == null || act == null) limbaFailNotEquals(msg,exp,act);
   else if (exp.length != act.length) limbaFailNotEquals(msg,exp,act);
   else {
      for (int i = 0; i < exp.length; ++i) {
	 limbaAssertNotEquals(msg,exp[i],act[i]);
       }
    }
}



public static void limbaAssertNotEquals(String msg,long [] exp,long [] act)
{
   if (exp == null && act == null) return;
   else if (exp == null || act == null) limbaFailNotEquals(msg,exp,act);
   else if (exp.length != act.length) limbaFailNotEquals(msg,exp,act);
   else {
      for (int i = 0; i < exp.length; ++i) {
	 limbaAssertNotEquals(msg,exp[i],act[i]);
       }
    }
}



public static void limbaAssertNotEquals(String msg,double [][] exp,double [][] act,double delta)
{
   if (exp == null && act == null) return;
   else if (exp == null || act == null) limbaFailNotEquals(msg,exp,act);
   else if (exp.length != act.length) limbaFailNotEquals(msg,exp,act);
   else {
      for (int i = 0; i < exp.length; ++i) {
	 limbaAssertNotEquals(msg,exp[i],act[i],delta);
       }
    }
}



public static void limbaAssertNotEquals(String msg,float [][] exp,float [][] act,float delta)
{
   if (exp == null && act == null) return;
   else if (exp == null || act == null) limbaFailNotEquals(msg,exp,act);
   else if (exp.length != act.length) limbaFailNotEquals(msg,exp,act);
   else {
      for (int i = 0; i < exp.length; ++i) {
	 limbaAssertNotEquals(msg,exp[i],act[i],delta);
       }
    }
}



public static void limbaAssertNotEquals(String msg,int [][] exp,int [][] act)
{
   if (exp == null && act == null) return;
   else if (exp == null || act == null) limbaFailNotEquals(msg,exp,act);
   else if (exp.length != act.length) limbaFailNotEquals(msg,exp,act);
   else {
      for (int i = 0; i < exp.length; ++i) {
	 limbaAssertNotEquals(msg,exp[i],act[i]);
       }
    }
}



public static void limbaAssertNotEquals(String msg,short [][] exp,short [][] act)
{
   if (exp == null && act == null) return;
   else if (exp == null || act == null) limbaFailNotEquals(msg,exp,act);
   else if (exp.length != act.length) limbaFailNotEquals(msg,exp,act);
   else {
      for (int i = 0; i < exp.length; ++i) {
	 limbaAssertNotEquals(msg,exp[i],act[i]);
       }
    }
}



public static void limbaAssertNotEquals(String msg,byte [][] exp,byte [][] act)
{
   if (exp == null && act == null) return;
   else if (exp == null || act == null) limbaFailNotEquals(msg,exp,act);
   else if (exp.length != act.length) limbaFailNotEquals(msg,exp,act);
   else {
      for (int i = 0; i < exp.length; ++i) {
	limbaAssertNotEquals(msg,exp[i],act[i]);
       }
    }
}



public static void limbaAssertNotEquals(String msg,char [][] exp,char [][] act)
{
   if (exp == null && act == null) return;
   else if (exp == null || act == null) limbaFailNotEquals(msg,exp,act);
   else if (exp.length != act.length) limbaFailNotEquals(msg,exp,act);
   else {
      for (int i = 0; i < exp.length; ++i) {
	 limbaAssertNotEquals(msg,exp[i],act[i]);
       }
    }
}



public static void limbaAssertNotEquals(String msg,long [][] exp,long [][] act)
{
   if (exp == null && act == null) return;
   else if (exp == null || act == null) limbaFailNotEquals(msg,exp,act);
   else if (exp.length != act.length) limbaFailNotEquals(msg,exp,act);
   else {
      for (int i = 0; i < exp.length; ++i) {
	 limbaAssertNotEquals(msg,exp[i],act[i]);
       }
    }
}



/********************************************************************************/
/*										*/
/*	Simplified assertions for handling real values				*/
/*										*/
/********************************************************************************/

public static void limbaAssertEquals(String msg,double exp,double act,double delta)
{
   if (Math.abs(exp-act) <= delta) return;
   limbaFailEquals(msg,exp,act);
}


public static void limbaAssertEquals(String msg,float exp,float act,double delta)
{
   if (Math.abs(exp-act) <= delta) return;
   limbaFailEquals(msg,exp,act);
}


public static void limbaAssertEquals(String msg,double exp,double act)
{
   limbaAssertEquals(msg,exp,act,DEFAULT_DELTA);
}

public static void limbaAssertEquals(String msg,float exp,float act)
{
   limbaAssertEquals(msg,exp,act,DEFAULT_FLOAT_DELTA);
}

public static void limbaAssertNotEquals(String msg,double exp,double act)
{
   limbaAssertNotEquals(msg,exp,act,DEFAULT_DELTA);
}

public static void limbaAssertNotEquals(String msg,float exp,float act)
{
   limbaAssertNotEquals(msg,exp,act,DEFAULT_FLOAT_DELTA);
}

public static void limbaAssertEquals(String msg,double [] exp,double [] act)
{
   limbaAssertEquals(msg,exp,act,DEFAULT_DELTA);
}

public static void limbaAssertEquals(String msg,float [] exp,float [] act)
{
   limbaAssertEquals(msg,exp,act,DEFAULT_FLOAT_DELTA);
}

public static void limbaAssertNotEquals(String msg,double [] exp,double [] act)
{
   limbaAssertNotEquals(msg,exp,act,DEFAULT_DELTA);
}

public static void limbaAssertNotEquals(String msg,float [] exp,float [] act)
{
   limbaAssertNotEquals(msg,exp,act,DEFAULT_FLOAT_DELTA);
}

public static void limbaAssertEquals(String msg,double [][] exp,double [][] act)
{
   limbaAssertEquals(msg,exp,act,DEFAULT_DELTA);
}

public static void limbaAssertEquals(String msg,float [][] exp,float [][] act)
{
   limbaAssertEquals(msg,exp,act,DEFAULT_FLOAT_DELTA);
}

public static void limbaAssertNotEquals(String msg,double [][] exp,double [][] act)
{
   limbaAssertNotEquals(msg,exp,act,DEFAULT_DELTA);
}

public static void limbaAssertNotEquals(String msg,float [][] exp,float [][] act)
{
   limbaAssertNotEquals(msg,exp,act,DEFAULT_FLOAT_DELTA);
}


/********************************************************************************/
/*                                                                              */
/*      Map JUNIT calls to our calls for user tests                             */
/*                                                                              */
/********************************************************************************/

public static void assertEquals(String message,Object exp,Object act)
{
   limbaAssertEquals(message,exp,act);
}


/********************************************************************************/
/*										*/
/*	Utility routines for helping with equals/not equals output		*/
/*										*/
/********************************************************************************/

private static void limbaFailEquals(String message, Object expected, Object actual)
{
   String formatted= "";
   if (message != null) formatted = message + " ";
   fail(formatted+"expected:<"+String.valueOf(expected)+"> but was:<"+String.valueOf(actual)+">");
}

private static void limbaFailNotEquals(String msg,Object e,Object a)
{
   String f = "";
   if (msg != null) f = msg + " ";
   f += "expected not:<" + String.valueOf(e) + "> but was:<" + String.valueOf(a) + ">";
   fail(f);
}





}       // end of class RunnerTestCase




/* end of RunnerTestCase.java */

