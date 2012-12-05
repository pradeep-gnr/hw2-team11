package edu.cmu.lti.oaqa.openqa.test.team11.keyterm;


import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.aliasi.chunk.Chunk;
import com.aliasi.chunk.Chunker;
import com.aliasi.chunk.Chunking;

import edu.cmu.lti.oaqa.cse.basephase.keyterm.AbstractKeytermExtractor;
import edu.cmu.lti.oaqa.framework.data.Keyterm;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.trees.Tree;


public class zhiqilKeytermExtractor extends AbstractKeytermExtractor {
  //set lingpipe bio gene tag chunker
  //java.net.URL url = getClass().getResource("/genetag/ne-en-bio-genetag.HmmChunker");

  
  @Override
  protected List<Keyterm> getKeyterms(String question) {

    ArrayList<Keyterm> keyTermList = new ArrayList<Keyterm>();   

    try {
      // get gene term 
      URL url = getClass().getResource("/genetag/ne-en-bio-genetag.HmmChunker"); 
      ObjectInputStream in = new ObjectInputStream(url.openStream());
      Chunker chunker = (Chunker) in.readObject();
      in.close();
      
      Chunking chunking = chunker.chunk(question);
      Set<Chunk> chunkSet = chunking.chunkSet();
      for(Chunk chunk : chunkSet) {  
        Keyterm k = new Keyterm(question.substring(chunk.start(), chunk.end()));    
        keyTermList.add(k);          
      }
      
      // get noun and verb
      LexicalizedParser lp = LexicalizedParser.loadModel("edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz"); 
     
      Tree tree = lp.apply(question);
      List<TaggedWord> taggedWords = tree.taggedYield();
      for (TaggedWord tw : taggedWords) {
        if (tw.tag().startsWith("N") || tw.tag().startsWith("V")) {
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