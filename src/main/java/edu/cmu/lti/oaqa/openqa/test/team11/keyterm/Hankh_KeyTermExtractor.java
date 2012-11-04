package edu.cmu.lti.oaqa.openqa.test.team11.keyterm;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.uima.UIMAFramework;
import org.apache.uima.UimaContext;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;

import edu.cmu.lti.oaqa.cse.basephase.keyterm.AbstractKeytermExtractor;
import edu.cmu.lti.oaqa.framework.data.Keyterm;
import edu.cmu.lti.oaqa.openqa.test.team11.keyterm.hankh.LocalGeneSearch;
import edu.cmu.lti.oaqa.openqa.test.team11.keyterm.hankh.NcbiQuery;
import edu.cmu.lti.oaqa.openqa.test.team11.keyterm.hankh.PosTagNamedEntityRecognizer;

public class Hankh_KeyTermExtractor extends AbstractKeytermExtractor {

  private String mGeneFilename;
  
  @Override
  public void initialize(UimaContext c) throws ResourceInitializationException {
    super.initialize(c);
    mGeneFilename = (String) c.getConfigParameterValue("gene_set");
  }
  
  @Override
  protected List<Keyterm> getKeyterms(String question) {
    List<Keyterm> keytermList = new ArrayList<Keyterm>();
    
    List<String> posList = getPosTagNamedEntity(question);
    
    // Local Gene Search
    LocalGeneSearch lgs = new LocalGeneSearch();
    boolean opened = lgs.openGeneSet(mGeneFilename);
    if (opened) {
      lgs.search(posList);
      for (String gene : lgs.getFoundList()) {
        keytermList.add(new Keyterm(gene));
      }
      posList = lgs.getUnfoundList();
    }
    
    // NCBI Query
    NcbiQuery ncbiQuery = new NcbiQuery(lgs.getGeneSet());
    List<String> geneList = ncbiQuery.search(posList);
    for (String gene : geneList) {
      keytermList.add(new Keyterm(gene));
    }
    saveGeneSet(ncbiQuery.getGeneSet());
    
    return keytermList;
  }

  /**
   * Uses Stanford's Part of Speech Tag Named Entity Recognizer
   * @param text
   * @return
   */
  private List<String> getPosTagNamedEntity(String text) {
    List<String> posTagList = new ArrayList<String>();
    try {
      PosTagNamedEntityRecognizer ptner = new PosTagNamedEntityRecognizer();
      
      Map<Integer, Integer> neMap = ptner.getGeneSpans(text);
      Iterator neMapKeyIter = neMap.keySet().iterator();
      while (neMapKeyIter.hasNext()) {
        // get next begin-end pair
        Integer neMapKey = (Integer) neMapKeyIter.next();
        int begin = neMapKey.intValue();
        int end = neMap.get(neMapKey).intValue();
        
        posTagList.add(text.substring(begin, end));
      }
      
    } catch (ResourceInitializationException e) {
//      throw new AnalysisEngineProcessException(e);
    }
    return posTagList;
  }
  
  private void saveGeneSet(Set<String> geneSet) {
    try {
      File file = new File(mGeneFilename);
      OutputStream os = new FileOutputStream(file);
      for (String gene : geneSet) {
        os.write(String.format("%s\n", gene).getBytes());
      }
      os.close();
    } catch (FileNotFoundException e) {
//      UIMAFramework.getLogger(this.getClass()).log(Level.FINEST, "Unable to find file");
    } catch (IOException e) {
//      UIMAFramework.getLogger(this.getClass()).log(Level.FINEST, "Unable to wrtie file");
    }
  }
  
}
