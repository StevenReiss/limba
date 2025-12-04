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
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.json.JSONObject;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.splitter.DocumentByLineSplitter;
import dev.langchain4j.data.document.splitter.DocumentByRegexSplitter;
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
import dev.langchain4j.store.embedding.filter.comparison.IsIn;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import edu.brown.cs.ivy.file.IvyFile;
import edu.brown.cs.ivy.file.IvyLog;

class LimbaRag implements LimbaConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private LimbaMain limba_main;
private Collection<File> project_files;
private Collection<File> all_files;
private ContentRetriever content_retriever;
private String workspace_name;
private long last_modified;
private File config_file;
private JSONObject local_data;
private JSONObject global_data;
private boolean remove_old;
private String chroma_url;

private static boolean use_java_splitter = true;
private static boolean rag_log = false;
      

/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

LimbaRag(LimbaMain lm,List<File> files,String ws)
{
   limba_main = lm;
   project_files = new HashSet<>();
   content_retriever = null;
   workspace_name = ws;
   chroma_url = "http://localhost:8000";
   
   loadConfigData();
   
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


Collection<File> getFiles()
{
   return all_files;
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
   
   IvyLog.logD("LIMBA","Update based on " + new Date(last_modified));
   
   // force update -- DEBUGGING ONLY
   last_modified = 0;
   remove_old = true;
   
   if (last_modified > 0) {
      all_files = new ArrayList<>(project_files);
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
   else {
      all_files = project_files;
    }
}


private void loadConfigData() 
{
   File f1 = new File(System.getProperty("user.home"));
   File f2 = new File(f1,".config");
   File f3 = new File(f2,"limba");
   f3.mkdirs();
   
   local_data = new JSONObject();
   global_data = new JSONObject();
   
   File f4 = new File(f2,"Config.json");
   try {
      String cnts = IvyFile.loadFile(f4);
      if (cnts != null && !cnts.isEmpty()) {
         global_data = new JSONObject(cnts);
         chroma_url = local_data.optString("chromaUrl",chroma_url);
         global_data.put("chromaUrl",chroma_url);
       }
    }
   catch (IOException e) {
      // missing config file is okay for now
    }
   
   config_file = new File(f3,workspace_name + ".json");
   try {
      String cnts = IvyFile.loadFile(config_file);
      if (cnts != null && !cnts.isEmpty()) {
         local_data = new JSONObject(cnts);
         last_modified = local_data.optLong("lastupdate",0);
         chroma_url = local_data.optString("chromaUrl",chroma_url);
       }
    }
   catch (IOException e) {
      // use 0 as last modified
    }   
}


private void updateLocalConfig()
{
   local_data.put("lastupdate",last_modified);
   try (FileWriter fw = new FileWriter(config_file)) {
      fw.write(local_data.toString() + "\n");
    }
   catch (IOException e) {
      IvyLog.logE("LIMBA","Problem writing config file",e);
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
   List<String> uids = new ArrayList<>();
   for (File f : project_files) {
      if (f.length() == 0) continue;
      Path p = f.toPath();
      Document d = FileSystemDocumentLoader.loadDocument(p);
      d.metadata().put("id",getUID(f));
      docs.add(d);
      uids.add(getUID(f));
    }
   DocumentSplitter splitter = getSplitter();
   
   OllamaEmbeddingModel embed = OllamaEmbeddingModel.builder()
         .baseUrl(limba_main.getUrl()) 
         .modelName("nomic-embed-text")
         .timeout(Duration.ofMinutes(2))
         .maxRetries(10)
         .logRequests(rag_log)
         .logResponses(rag_log)
         .build();
   
   EmbeddingStore<TextSegment> store = null;
   if (chroma_url != null && !chroma_url.isEmpty() && !chroma_url.startsWith("*")) {
      try {
         ChromaEmbeddingStore cstore = ChromaEmbeddingStore.builder()
            .apiVersion(ChromaApiVersion.V2)
            .collectionName("LIMBA_" + workspace_name)
            .baseUrl("http://localhost:8000/")
            .tenantName("LIMBA")
            .logRequests(rag_log)
            .logResponses(rag_log)
            .build();
         if (last_modified <= 0) {
            cstore.removeAll();
            remove_old = false;
          }
         store = cstore;
       }
      catch (Throwable t) {
         IvyLog.logE("LIMBA","Can't create chroma store", t);
       }
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
         store.removeAll(new IsIn("id",uids));
       }
      EmbeddingStoreIngestor ingest = EmbeddingStoreIngestor.builder()
         .documentSplitter(splitter)
         .embeddingModel(embed)
         .embeddingStore(store)
         .build();
      IvyLog.logD("LIMBA","Ingest documents " + docs.size());
      ingest.ingest(docs);
      IvyLog.logD("LIMBA","Done ingest");
      
      last_modified = System.currentTimeMillis();
      updateLocalConfig();
            
      retrv = EmbeddingStoreContentRetriever.builder()
            .embeddingModel(embed)
            .embeddingStore(store)
            .maxResults(10)
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
// try {
//    MessageDigest md = MessageDigest.getInstance("MD5");
//    byte [] dvl = md.digest(p.getBytes());
//    String rslt = Base64.getEncoder().encodeToString(dvl);
//    if (rslt.length() > 16) rslt = rslt.substring(0,16);
//    return rslt;
//  }
// catch (Exception e) {
//    IvyLog.logE("LIMBA","Problem with MD5 encoding of " + p);
//  }
   
   return p;
}



/********************************************************************************/
/*                                                                              */
/*      Document splitter for Java Source Code                                  */
/*                                                                              */
/********************************************************************************/

private DocumentSplitter getSplitter()
{
   DocumentSplitter splitter1 = new DocumentByLineSplitter(128,4);
   
   // and may need refinement for complex real-world code.
   String regex = "(?=\\s*(public|protected|private|static|void|int|String).*\\w+\\s*\\([^)]*\\)\\s*\\{)";
   
   // Use the DocumentByRegexSplitter
   // Parameters: regex, joinDelimiter, maxSegmentSize (chars), maxOverlap (chars), subSplitter
   DocumentByRegexSplitter splitter2 = new DocumentByRegexSplitter(
         regex,      // The regex to split by
         "\\n\\n",   // The delimiter to join parts with if they fit in max size
         1000,       // Max segment size in characters (adjust as needed)
         100,                    // Max overlap in characters (adjust as needed)
         splitter1
   );
   
   
   // SHOULD USE DocumentByCodeSplitter when available
   
   if (use_java_splitter) return splitter2;
   
   return splitter1;
}



}       // end of class LimbaRag




/* end of LimbaRag.java */

