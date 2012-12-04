package edu.cmu.lti.f12.hw2.hw2_team11.retrieval;

import java.util.ArrayList;
import java.util.List;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import edu.cmu.lti.oaqa.framework.data.Keyterm;
import edu.cmu.lti.oaqa.framework.data.RetrievalResult;
import edu.cmu.lti.oaqa.openqa.hello.retrieval.SimpleSolrRetrievalStrategist;

public class SimpleBioSolrRetrievalStrategist extends SimpleSolrRetrievalStrategist {

  protected List<RetrievalResult> retrieveDocuments(String query) {
    ArrayList<Keyterm> keyTermList = new ArrayList<Keyterm>();
    List<RetrievalResult> result = new ArrayList<RetrievalResult>();
    try {
      String[] keytermlist = query.split(" ");
      int flag = 0;
      for (String k : keytermlist) {
        Keyterm i = new Keyterm(k);  
        for (Keyterm j : keyTermList) {
          if (i.getText().equals(j.getText())) {
            flag = 1;
            break;
          }
        }
        if (flag == 0) {
          keyTermList.add(i);
        }       
      }
      System.out.println(keyTermList);
      
      SolrDocumentList docs = wrapper.runQuery(keyTermList.toString(), hitListSize);

      for (SolrDocument doc : docs) {

        RetrievalResult r = new RetrievalResult((String) doc.getFieldValue("id"),
                (Float) doc.getFieldValue("score"), query);
        result.add(r);
        System.out.println(doc.getFieldValue("id"));
      }
    } catch (Exception e) {
      System.err.println("Error retrieving documents from Solr: " + e);
    }
    return result;
  }

}
