package edu.cmu.lti.oaqa.openqa.test.team11.keyterm.hankh;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;

public class NcbiQuery {
  
  private HttpClient mClient = new DefaultHttpClient();
  private HttpGet mGet = new HttpGet();
  
  private Set<String> mGeneSet = new HashSet<String>();
  
  public NcbiQuery(Set<String> geneSet) {
    mGeneSet = geneSet;
  }
  
  public Set<String> getGeneSet() {
    return mGeneSet;
  }
  
  public List<String> search(List<String> posTagList) {
    List<String> geneList = new ArrayList<String>();
    
    for (String posTag : posTagList) {
      try {
        // send NCBI query using named entity
        List<String> idList = searchQuery(posTag);
        if (idList.size() > 0) {
          // retrieve summaries of ids found by search query 
          List<String> nameList = summaryQuery(idList);
          
          for (String name : nameList) {
            int[] result = searchNamedEntity(posTag, name);
            if (result != null) {
              int begin = result[0];
              int end = result[1];
              
              geneList.add(posTag.substring(begin, end).trim());
            }
          }
          
          // add gene list to gene set
          mGeneSet.addAll(nameList);
        }
      } catch (Exception e) {
        // log exception 
      } 
    }
    return geneList;
  }
  
  /**
   * Search NamedEntity for specific string found from NCBI gene database
   * 
   * @param namedEntity the named entity subject of search
   * @param name the specific gene to search for in the named entity
   * @return an array containing the begin and end offsets of where the name is 
   * found in the named entity
   */
  private int[] searchNamedEntity(String namedEntity, String name) {
    int[] result = null;
    if (namedEntity.toLowerCase().contains(name.toLowerCase()) && name.length() != 0) {
      result = new int[2];
      int begin = namedEntity.indexOf(name.toLowerCase());
      int end = begin + name.length();

      result[0] = begin;
      result[1] = end;
      
      // Check to make sure results are valid
      if (begin < 0 || end < 0 || end < begin) {
        return null;
      }
    }
    
    return result;
  }
  
  /**
   * Queries NCBI gene database for search of specific query string
   * 
   * @param query the query string to search for on NCBI gene database
   * @return  list of ids 
   * @throws AnalysisEngineProcessException - if an error occurs while searching or parsing
   */
  private List<String> searchQuery(String query) throws AnalysisEngineProcessException {
    try {
      // create uri
      List<NameValuePair> formParams = new ArrayList<NameValuePair>();
      formParams.add(new BasicNameValuePair("db", "gene"));
      formParams.add(new BasicNameValuePair("term", query));
      URI uri = URIUtils.createURI("http", "eutils.ncbi.nlm.nih.gov", -1,
              "/entrez/eutils/esearch.fcgi", URLEncodedUtils.format(formParams, "UTF-8"), null);

      // send query
      mGet.setURI(uri);
      HttpEntity result = mClient.execute(mGet).getEntity();

      // parse results and return id list
      SearchXmlParser searchParser = new SearchXmlParser();
      searchParser.parseDocument(result.getContent());
      return searchParser.getIdList();
    } catch (Exception e) {
      throw new AnalysisEngineProcessException(e);
    } 
  }

  /**
   * Queries NCBI database for summaries of specified ids
   * 
   * @param idList list of ids to retrieve summaries
   * @return list of gene names
   * @throws AnalysisEngineProcessException - if an error occurs while searching or parsing 
   */
  private List<String> summaryQuery(List<String> idList) throws AnalysisEngineProcessException {
    try {
      // combine ids into one string
      StringBuilder ids = new StringBuilder();
      for (String id : idList) {
        ids.append(id);
        ids.append(' ');
      }

      // create uri
      List<NameValuePair> formParams = new ArrayList<NameValuePair>();
      formParams.add(new BasicNameValuePair("db", "gene"));
      formParams.add(new BasicNameValuePair("id", ids.toString()));
      URI uri = URIUtils.createURI("http", "eutils.ncbi.nlm.nih.gov", -1,
              "/entrez/eutils/esummary.fcgi", URLEncodedUtils.format(formParams, "UTF-8"), null);

      // send summary request
      mGet.setURI(uri);
      HttpEntity result = mClient.execute(mGet).getEntity();

      // parse results and return gene list
      SummaryXmlParser summaryParser = new SummaryXmlParser();
      summaryParser.parseDocument(result.getContent());
      return summaryParser.getNameList();
    } catch (Exception e) {
      throw new AnalysisEngineProcessException(e);
    }
  }
}
