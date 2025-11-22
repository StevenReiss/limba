/********************************************************************************/
/*                                                                              */
/*              LimbaRag.java                                                   */
/*                                                                              */
/*      description of class                                                    */
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
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.splitter.DocumentByLineSplitter;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.chroma.ChromaApiVersion;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.ivy.file.IvyLog;
import edu.brown.cs.ivy.project.IvyProject;
import edu.brown.cs.ivy.project.IvyProjectManager;

class LimbaRag implements LimbaConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private LimbaMain limba_main;
private Collection<File> project_files;
private ContentRetriever content_retriever;
private String workspace_name;
private long last_modified;
private File config_file;
private boolean remove_old;
      

/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

LimbaRag(LimbaMain lm,File base,String ws)
{
   limba_main = lm;
   project_files = new HashSet<>();
   content_retriever = null;
   workspace_name = ws;
  
   IvyLog.logD("LIMBA","Loading project files for " + base);
   if (base != null) findProjectFiles(base);
   
   checkUpdates();
}


LimbaRag(LimbaMain lm,List<File> files,String ws)
{
   this(lm,(File) null,ws);
   if (files != null) {
      project_files.addAll(files);
    }
   
   checkUpdates();
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                    */
/********************************************************************************/

ContentRetriever getContentRetriever()
{
   if (content_retriever == null && !project_files.isEmpty()) {
      content_retriever = new EmptyContentRetriever();
      content_retriever = setupRAG();
    }
   
   return content_retriever;
}



/********************************************************************************/
/*                                                                              */
/*      File/update management                                                  */
/*                                                                              */
/********************************************************************************/

private void checkUpdates()
{
   last_modified = 0;
   remove_old = false;
   
   File f1 = new File(System.getProperty("user.home"));
   File f2 = new File(f1,".config");
   File f3 = new File(f2,"limba");
   f3.mkdirs();
   config_file = new File(f3,workspace_name + ".json");
   try {
      String cnts = IvyFile.loadFile(config_file);
      if (cnts != null && !cnts.isEmpty()) {
         JSONObject jo = new JSONObject(cnts);
         last_modified = jo.optLong("lastupdate",0);
       }
    }
   catch (IOException e) {
      // use 0 as last modified
    }   
   
   if (last_modified > 0) {
      remove_old = true;
      for (Iterator<File> it = project_files.iterator(); it.hasNext(); ) {
         File f = it.next();
         long lm = f.lastModified();
         if (lm < last_modified) {
            // remove files that don't need updating
            it.remove();
          }
       }
    }
}



/********************************************************************************/
/*                                                                              */
/*      Build the content retriever                                             */
/*                                                                              */
/********************************************************************************/

private ContentRetriever setupRAG()
{
   List<Document> docs = new ArrayList<>();
   for (File f : project_files) {
      if (f.length() == 0) continue;
      Path p = f.toPath();
      Document d = FileSystemDocumentLoader.loadDocument(p);
      d.metadata().put("LIMBAID",getUID(f));
      docs.add(d);
    }
   DocumentSplitter spliter = new DocumentByLineSplitter(64,0);
// List<TextSegment> segs = spliter.splitAll(docs);
   
   OllamaEmbeddingModel embed = OllamaEmbeddingModel.builder()
         .baseUrl(limba_main.getUrl()) 
         .modelName("nomic-embed-text")
         .timeout(Duration.ofMinutes(2))
         .maxRetries(10)
         .logRequests(true)
         .logResponses(true)
         .build();
   
   EmbeddingStore<TextSegment> store = null;
   try {
      store = ChromaEmbeddingStore.builder()
         .apiVersion(ChromaApiVersion.V2)
         .collectionName("LIMBA_" + workspace_name)
         .baseUrl("http://localhost:8000/")
//       .tenantName("LIMBA_" + IvyExecQuery.getProcessId())
         .tenantName("LIMBA")
         .logRequests(true)
         .logResponses(true)
         .build();
    }
   catch (Throwable t) {
      IvyLog.logE("LIMBA","Can't create chroma store", t);
    }
// if (store == null) {
//    try {
//       store = RedisEmbeddingStore.builder()
//          .indexName("LIMBA" + IvyExecQuery.getProcessId()) 
//          .metadataKeys(Set.of("file_name"))
//          .host("localhost")
//          .port(6379)
//          .build();
//     } 
//    catch (Throwable t) {
//       IvyLog.logI("LIMBA","Can't create redis store: " + t);
//     }
//  }
  
   if (store == null) {
      store = new InMemoryEmbeddingStore<>();
      last_modified = -1;
      remove_old = false;
    }
   
// need class okhttp3/Interceptor -- if this fails, defer to immemboery mode
   ContentRetriever retrv;
   try {
      if (remove_old) {
         // remove old files from store
       }
      EmbeddingStoreIngestor ingest = EmbeddingStoreIngestor.builder()
         .documentSplitter(spliter)
         .embeddingModel(embed)
         .embeddingStore(store)
         .build();
      IvyLog.logD("LIMBA","Ingest documents " + docs.size());
      ingest.ingest(docs);
      IvyLog.logD("LIMBA","Done ingest");
      
      JSONObject jo = new JSONObject();
      jo.put("lastupdate",System.currentTimeMillis());
      try (FileWriter fw = new FileWriter(config_file)) {
         fw.write(jo.toString() + "\n");
       }
      catch (IOException e) {
         IvyLog.logE("LIMBA","Problem writing config file",e);
       }
            
      retrv = EmbeddingStoreContentRetriever.builder()
            .embeddingModel(embed)
            .embeddingStore(store)
            .maxResults(5)
            .build();
      IvyLog.logD("LIMBA","Build RAG content retreiver " + retrv);
    }
   catch (Throwable t) {
      IvyLog.logE("LIMBA","Problem setting up RAG",t);
      retrv = new EmptyContentRetriever();
    }
   
   return retrv;
}



private static final class EmptyContentRetriever implements ContentRetriever {
   
   @Override public List<Content> retrieve(Query query) { 
      return new ArrayList<>();
    }

}       // end of inner class EmptyContentRetriever



private String getUID(File f)
{
   String p = IvyFile.getCanonicalPath(f);
   return p;
}

/********************************************************************************/
/*                                                                              */
/*      Find files for a project                                                */
/*                                                                              */
/********************************************************************************/

private void findProjectFiles(File base)
{
   if (base == null) return;
   else if (isEclipseWorkspace(base)) {
      addEclipseFiles(base);
    }
   else if (base.isDirectory()) {
      addDirectoryFiles(base);
    }
}



/********************************************************************************/
/*                                                                              */
/*      Handle Eclipse workspaces                                               */
/*                                                                              */
/********************************************************************************/

private boolean isEclipseWorkspace(File base)
{
   if (base == null) return false;
   if (!base.exists()) return false;
   if (!base.isDirectory()) return false;
   
   File df1 = new File(base,".metadata");
   if (!df1.exists() || !df1.canRead()) return false;
   File df2 = new File(df1,"version.ini");
   if (!df2.exists()) return false;
      
   return true;
}
   

private void addEclipseFiles(File base)
{
   IvyProjectManager pm = IvyProjectManager.getManager();
   List<IvyProject> projs = pm.defineEclipseProjects(base);
   for (IvyProject ip : projs) {
      for (File f : ip.getSourceFiles()) {
         project_files.add(f);
       }
    }
}
  
   
/********************************************************************************/
/*                                                                              */
/*      Handle directory                                                        */
/*                                                                              */
/********************************************************************************/

private void addDirectoryFiles(File base)
{
   if (base.isDirectory()) {
      for (File f : base.listFiles()) {
         addDirectoryFiles(f);
       }
    }
   else if (isRelevant(base)) {
      project_files.add(base);
    }
}


private boolean isRelevant(File base)
{
   String nm = base.getName();
   int idx = nm.lastIndexOf(".");
   if (idx < 0) return false;
   String ext = nm.substring(idx).toLowerCase();
   switch (ext) {
      case ".java" :
      case ".xml" :
      case ".json" :
      case ".txt" :
      case ".md" :
         break;
      default :
         return false;
    }
   
   return true;
}

}       // end of class LimbaRag




/* end of LimbaRag.java */

