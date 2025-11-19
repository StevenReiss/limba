/********************************************************************************/
/*                                                                              */
/*              LimbaCodeDecl.java                                              */
/*                                                                              */
/*      Analyze a code declaration and provide output to front end              */
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

import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import edu.brown.cs.ivy.xml.IvyXmlWriter;

abstract class LimbaCodeDecl implements LimbaConstants
{



/********************************************************************************/
/*                                                                              */
/*      Creation methods                                                        */
/*                                                                              */
/********************************************************************************/

static LimbaCodeDecl createDeclaration(BodyDeclaration bd)
{
   switch (bd.getNodeType()) {
      case ASTNode.TYPE_DECLARATION :
         return new LimbaTypeDecl((TypeDeclaration) bd);
      case ASTNode.ENUM_DECLARATION :
         break;
      case ASTNode.FIELD_DECLARATION :
         return new LimbaFieldDecl((FieldDeclaration) bd);
      case ASTNode.METHOD_DECLARATION :
         return new LimbaMethodDecl((MethodDeclaration) bd);
      default :
         break;
    }
   return null;
}


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

protected String decl_code;
protected BodyDeclaration body_decl;
private boolean is_helper;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

protected LimbaCodeDecl(BodyDeclaration bd)
{
   body_decl = bd;
   is_helper = true;
   decl_code = bd.toString();
}


/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

void setIsHelper(boolean fg)                    { is_helper = fg; }



/********************************************************************************/
/*                                                                              */
/*      Output methods                                                          */
/*                                                                              */
/********************************************************************************/

void outputXml(IvyXmlWriter xw,String what)
{
   xw.begin(what);
   if (is_helper) xw.field("HELPER",is_helper);
   else xw.field("TEST",true);
   xw.field("MODINT",body_decl.getModifiers());
   
   localOutputXml(xw);
   
   Javadoc jd = body_decl.getJavadoc();
   if (jd != null) {
      xw.cdataElement("JAVADOC",jd.toString());
    }
   xw.textElement("ATTRIBUTES",getAttributes());
   xw.cdataElement("RAWCODE",decl_code);
   
   xw.end(what);
}

abstract void localOutputXml(IvyXmlWriter xw);



/********************************************************************************/
/*                                                                              */
/*      Helper methods                                                          */
/*                                                                              */
/********************************************************************************/

private String getAttributes() 
{
   StringBuffer buf = new StringBuffer();
   for (Object o : body_decl.modifiers()) {
      IExtendedModifier em = (IExtendedModifier) o;
      if (em.isAnnotation()) {
         if (!buf.isEmpty()) buf.append(" ");
         buf.append(o.toString());
       }
    }
   return buf.toString();
}


protected String getListValue(List<?> data,String sep) 
{
   StringBuffer buf = new StringBuffer();
   for (Object o : data) {
      if (!buf.isEmpty()) buf.append(sep);
      buf.append(o.toString());
    }
   return buf.toString();
}


/********************************************************************************/
/*                                                                              */
/*      LimbaTypeDecl -- Code Declaration for a type                            */
/*                                                                              */
/********************************************************************************/

private static class LimbaTypeDecl extends LimbaCodeDecl {
   
   LimbaTypeDecl(TypeDeclaration td) {
      super(td);
    }
   
   @Override void localOutputXml(IvyXmlWriter xw) {
      TypeDeclaration td = (TypeDeclaration) body_decl;
      if (td.getParent() instanceof AbstractTypeDeclaration) {
         xw.field("INNERTYPE",true);
       }
      if (td.isInterface()) xw.field("INTERFACE",true);
      if (td.getSuperclassType() != null) {
         xw.field("SUPERCLASS",td.getSuperclassType().toString());
       }
      xw.textElement("IMPLEMENTS",getListValue(td.superInterfaceTypes(),","));
      String cnts = decl_code;
      int idx1 = cnts.indexOf("{");
      int idx2 = cnts.lastIndexOf("}");
      cnts = cnts.substring(idx1,idx2+1);
      xw.cdataElement("CONTENTS",cnts);
    }
   
}       // end of inner class LimbaTypeDecl



/********************************************************************************/
/*                                                                              */
/*      LimbaFieldDecl -- Code declaration for a field                          */
/*                                                                              */
/********************************************************************************/

private static class LimbaFieldDecl extends LimbaCodeDecl {
   
   LimbaFieldDecl(FieldDeclaration fd) {
      super(fd);
    }
   
   @Override void localOutputXml(IvyXmlWriter xw) {
      FieldDeclaration fd = (FieldDeclaration) body_decl;
      xw.textElement("RETURNS",fd.getType().toString());
      for (Object o : fd.fragments()) {
         VariableDeclarationFragment vdf = (VariableDeclarationFragment) o;
         xw.begin("FIELD");
         xw.field("NAME",vdf.getName().toString());
         if (vdf.getExtraDimensions() != 0) {
            xw.field("DIMS",vdf.getExtraDimensions());
          }
         if (vdf.getInitializer() != null) {
            xw.cdataElement("INIT",vdf.getInitializer().toString());
          }
         xw.end("FIELD");
       }
    }
   
}       // end of inner class LimbaFieldDecl


/********************************************************************************/
/*                                                                              */
/*      LimbaMethodDecl -- Code declaration for a method                        */
/*                                                                              */
/********************************************************************************/

private static class LimbaMethodDecl extends LimbaCodeDecl {

   LimbaMethodDecl(MethodDeclaration fd) {
      super(fd);
    }
   
   @Override void localOutputXml(IvyXmlWriter xw) {
      MethodDeclaration md = (MethodDeclaration) body_decl;
      xw.field("NAME",md.getName().toString());
      if (md.isConstructor()) xw.field("CONSTRUCTOR",true);
      else {
         xw.textElement("RETURNS",md.getReturnType2().toString());
       }
      StringBuffer buf = new StringBuffer();
      xw.textElement("PARAMETERS","(" + getListValue(md.parameters(),",") + ")");
      for (Object o : md.parameters()) {
         SingleVariableDeclaration svd = (SingleVariableDeclaration) o;
         if (!buf.isEmpty()) buf.append(",");
         buf.append(svd.toString());
       }
      xw.textElement("PARAMTERS","(" + buf.toString() + ")");
      xw.cdataElement("CONTENTS",md.getBody().toString());
    }

}       // end of inner class LimbaFieldDecl




}       // end of class LimbaCodeDecl




/* end of LimbaCodeDecl.java */

