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
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import dev.langchain4j.chain.ConversationalRetrievalChain;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.splitter.DocumentByLineSplitter;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
// import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
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
private EmbeddingStoreContentRetriever content_retriever;
      

/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

LimbaRag(LimbaMain lm,File base)
{
   limba_main = lm;
   project_files = new HashSet<>();
   content_retriever = null;
   
   findProjectFiles(base);
}



/********************************************************************************/
/*                                                                              */
/*      Access methods                                                          */
/*                                                                              */
/********************************************************************************/

ConversationalRetrievalChain getChain()
{
   if (content_retriever == null && !project_files.isEmpty()) {
      content_retriever = setupRAG();
    }
   
   if (content_retriever == null) return null;
   
   OllamaChatModel chat = OllamaChatModel.builder()
         .baseUrl(limba_main.getUrl())
         .logRequests(true)
         .logResponses(true)
         .modelName(limba_main.getModel())
         .build();
   ConversationalRetrievalChain chain = ConversationalRetrievalChain.builder()
         .chatModel(chat)
         .contentRetriever(content_retriever)
         .build();
   
   return chain;
}

/********************************************************************************/
/*                                                                              */
/*      Build the content retriever                                             */
/*                                                                              */
/********************************************************************************/

private EmbeddingStoreContentRetriever setupRAG()
{
   List<Document> docs = new ArrayList<>();
   for (File f : project_files) {
      if (f.length() == 0) continue;
      Path p = f.toPath();
      Document d = FileSystemDocumentLoader.loadDocument(p);
      docs.add(d);
    }
   DocumentSplitter spliter = new DocumentByLineSplitter(128,0);
// List<TextSegment> segs = spliter.splitAll(docs);
   
   OllamaEmbeddingModel embed = OllamaEmbeddingModel.builder()
         .baseUrl(limba_main.getUrl()) 
//       .modelName(limba_main.getModel())
         .modelName("nomic-embed-text")
         .timeout(Duration.ofMinutes(2))
         .maxRetries(10)
         .logRequests(true)
         .logResponses(true)
         .build();
   EmbeddingStore<TextSegment> store;
   store = new InMemoryEmbeddingStore<>();
// need class okhttp3/Interceptor
// store = ChromaEmbeddingStore.builder()
//       .baseUrl(limba_main.getUrl())
//       .build();
   EmbeddingStoreIngestor ingest = EmbeddingStoreIngestor.builder()
         .documentSplitter(spliter)
         .embeddingModel(embed)
         .embeddingStore(store)
         .build();
   IvyLog.logD("LIMBA","Ingest documents " + docs.size());
   ingest.ingest(docs);
   IvyLog.logD("LIMBA","Done ingest");
   EmbeddingStoreContentRetriever retrv = EmbeddingStoreContentRetriever.builder()
         .embeddingModel(embed)
         .embeddingStore(store)
         .maxResults(10)
         .build();
   IvyLog.logD("LIMBA","Build RAG content retreiver " + retrv);
   
   return retrv;
}



/********************************************************************************/
/*                                                                              */
/*      Find files for a project                                                */
/*                                                                              */
/********************************************************************************/

private void findProjectFiles(File base)
{
   if (isEclipseWorkspace(base)) {
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

