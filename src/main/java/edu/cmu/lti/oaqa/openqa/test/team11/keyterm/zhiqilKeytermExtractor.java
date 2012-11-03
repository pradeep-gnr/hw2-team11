package edu.cmu.lti.oaqa.openqa.test.team11.keyterm;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.aliasi.chunk.Chunk;
import com.aliasi.chunk.Chunker;
import com.aliasi.chunk.Chunking;
import com.aliasi.util.AbstractExternalizable;

import edu.cmu.lti.oaqa.cse.basephase.keyterm.AbstractKeytermExtractor;
import edu.cmu.lti.oaqa.framework.data.Keyterm;


public class zhiqilKeytermExtractor extends AbstractKeytermExtractor {
  //set lingpipe bio gene tag chunker
  File geneTag = new File("src/main/resources/genetag/ne-en-bio-genetag.HmmChunker");

  @Override
  protected List<Keyterm> getKeyterms(String question) {

    ArrayList<Keyterm> keyTermList = new ArrayList<Keyterm>();   

    try {
      Chunker chunker = (Chunker) AbstractExternalizable.readObject(geneTag); 
          
      Chunking chunking = chunker.chunk(question);
      Set<Chunk> chunkSet = chunking.chunkSet();
      for(Chunk chunk : chunkSet) {  
        Keyterm k = new Keyterm(question.substring(chunk.start(), chunk.end()));    
        keyTermList.add(k);          
      }
    } catch (IOException e) {
      e.printStackTrace();
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    }

    return keyTermList;
  }
   
}