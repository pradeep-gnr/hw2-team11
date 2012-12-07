package edu.cmu.lti.oaqa.openqa.test.team11.retrieval;

/*
 *  Copyright 2012 Carnegie Mellon University
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.resource.ResourceInitializationException;

import edu.cmu.lti.oaqa.core.provider.solr.SolrWrapper;
import edu.cmu.lti.oaqa.cse.basephase.retrieval.AbstractRetrievalStrategist;
import edu.cmu.lti.oaqa.framework.data.Keyterm;
import edu.cmu.lti.oaqa.framework.data.RetrievalResult;

/**
 * 
 * @author Zi Yang <ziy@cs.cmu.edu>
 * 
 */
public class ZhiqilDocumentRetrieval2 extends AbstractRetrievalStrategist {

  protected Integer hitListSize;

  protected SolrWrapper wrapper;

  @Override
  public void initialize(UimaContext aContext) throws ResourceInitializationException {
    super.initialize(aContext);
    try {
      this.hitListSize = (Integer) aContext.getConfigParameterValue("hit-list-size");
    } catch (ClassCastException e) { // all cross-opts are strings?
      this.hitListSize = Integer.parseInt((String) aContext
              .getConfigParameterValue("hit-list-size"));
    }
    String serverUrl = (String) aContext.getConfigParameterValue("server");
    Integer serverPort = (Integer) aContext.getConfigParameterValue("port");
    Boolean embedded = (Boolean) aContext.getConfigParameterValue("embedded");
    String core = (String) aContext.getConfigParameterValue("core");
    try {
      this.wrapper = new SolrWrapper(serverUrl, serverPort, embedded, core);
    } catch (Exception e) {
      throw new ResourceInitializationException(e);
    }
  }

  @Override
  protected final List<RetrievalResult> retrieveDocuments(String questionText,
          List<Keyterm> keyterms) {
    String query = formulateQuery(keyterms);
    return retrieveDocuments(query);
  };

  protected String formulateQuery(List<Keyterm> keyterms) {
    StringBuffer result = new StringBuffer();
    for (Keyterm keyterm : keyterms) {
      result.append(keyterm.getText() + " ");
    }
    String query = result.toString();
    System.out.println(" QUERY: " + query);
    return query;
  }
  
  // compare probability of 2 retrieval result
  public class Mycomparator implements Comparator<RetrievalResult>{  
    @Override
    public int compare(RetrievalResult o1, RetrievalResult o2) {
      RetrievalResult r1=(RetrievalResult)o1;  
      RetrievalResult r2=(RetrievalResult)o2;    
     if(r1.getProbability()<r2.getProbability())  
         return 1;  
     else  
         return 0;
    }  
  } 
  
  /**
   * 
   * @author Zhiqi Li <zhiqil@andrew.cmu.edu>
   * 
   */
  private List<RetrievalResult> retrieveDocuments(String query) {
    List<RetrievalResult> result = new ArrayList<RetrievalResult>();
    List<RetrievalResult> processed = new ArrayList<RetrievalResult>();
 
    try {   
      // iterate each query keyterm   
      
      String[] keytermlist = query.split(" ");
      
      for (String keyterm : keytermlist) {
        System.out.println(keyterm);
        // Needs to be tuned
        SolrDocumentList docs = wrapper.runQuery(keyterm, 200);
        
        for (SolrDocument doc : docs) {
          // get each document's id, score and query
          RetrievalResult r = new RetrievalResult((String) doc.getFieldValue("id"),
                  (Float) doc.getFieldValue("score"), query);
          
          // add to result
          result.add(r);                     
        }
      }  
      
      System.out.println("keyterm:" + result.size());
      
      // update common documents scores
      for (int i = 0; i < result.size(); i++) {
        for (int j = i+1; j< result.size(); j++) {
          if (result.get(i).getDocID().equals(result.get(j).getDocID())) {
            result.get(i).setProbablity(result.get(i).getProbability() + result.get(j).getProbability());
            result.remove(result.get(j));
          }
        }
      }
      
   
      // sort the result
      Comparator<RetrievalResult> comp = new Mycomparator();  
      Collections.sort(result,comp);
      
      // get the top hitListSize documents
      for(int i = 0; i < Math.min(result.size(), hitListSize); i++){  
        RetrievalResult p = (RetrievalResult)result.get(i); 
        processed.add(p);
      }
      
      
    } catch (Exception e) {
      System.err.println("Error retrieving documents from Solr: " + e);
    }
    return processed;
  }

  @Override
  public void collectionProcessComplete() throws AnalysisEngineProcessException {
    super.collectionProcessComplete();
    wrapper.close();
  }
}