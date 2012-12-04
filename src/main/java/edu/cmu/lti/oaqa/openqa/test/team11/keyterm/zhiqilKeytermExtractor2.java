package edu.cmu.lti.oaqa.openqa.test.team11.keyterm;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.uima.resource.ResourceAccessException;

import com.aliasi.chunk.Chunk;
import com.aliasi.chunk.Chunker;
import com.aliasi.chunk.Chunking;
import com.aliasi.util.AbstractExternalizable;

import edu.cmu.lti.oaqa.cse.basephase.keyterm.AbstractKeytermExtractor;
import edu.cmu.lti.oaqa.framework.data.Keyterm;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.trees.Tree;


public class zhiqilKeytermExtractor2 extends AbstractKeytermExtractor {
  //set lingpipe bio gene tag chunker
  File geneTag = new File("src/main/resources/genetag/ne-en-bio-genetag.HmmChunker");
  File diseaseTag = new File("src/main/resources/genetag/ne-en-bio-genia.TokenShapeChunker");

  @Override
  protected List<Keyterm> getKeyterms(String question) {

    ArrayList<Keyterm> keyTermList = new ArrayList<Keyterm>();   

    try {
      /*
       * ObjectInputStream ois = new ObjectInputStream(url.openStream());
         chunker = (Chunker) ois.readObject();
         Streams.closeQuietly(ois);
      */
      //File geneTag = new File(getContext()
      //          .getResourceFilePath("file:genetag/ne-en-bio-genetag.HmmChunker"));
 
     // File diseaseTag = new File(getContext()
     //         .getResourceFilePath("file:genetag/ne-en-bio-genia.TokenShapeChunker"));
      
    
         
      // Extract disease keyterm
      Chunker chunker2 = (Chunker) AbstractExternalizable.readObject(diseaseTag); 
      
      Chunking chunking2 = chunker2.chunk(question);
      Set<Chunk> chunkSet2 = chunking2.chunkSet();
      for(Chunk chunk : chunkSet2) {  
        Keyterm k = new Keyterm(question.substring(chunk.start(), chunk.end()));    
        keyTermList.add(k);          
      }
      
      // Extract gene keyterm      
      Chunker chunker = (Chunker) AbstractExternalizable.readObject(geneTag); 
      
      Chunking chunking = chunker.chunk(question);
      Set<Chunk> chunkSet = chunking.chunkSet();
      for(Chunk chunk : chunkSet) {  
        Keyterm k = new Keyterm(question.substring(chunk.start(), chunk.end()));
        if (!keyTermList.contains(k)) {
          keyTermList.add(k);
        }
      }
      
      // Extract verbs      
      LexicalizedParser lp = LexicalizedParser.loadModel("edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz"); 
     
      Tree tree = lp.apply(question);
      List<TaggedWord> taggedWords = tree.taggedYield();
      for (TaggedWord tw : taggedWords) {
        if (tw.tag().startsWith("V")) {
          if (!tw.word().equals("is") && (!tw.word().equals("are")) & (!tw.word().equals("do")) && (!tw.word().equals("does"))) {
            Keyterm k = new Keyterm(tw.word());
            keyTermList.add(k);
          }
        }
      }  
   
    } catch (IOException e) {
      e.printStackTrace();
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    } 
    return keyTermList;
  }
   
}