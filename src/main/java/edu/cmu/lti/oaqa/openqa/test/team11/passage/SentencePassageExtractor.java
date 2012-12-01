package edu.cmu.lti.oaqa.openqa.test.team11.passage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;

import edu.cmu.lti.oaqa.framework.data.Keyterm;
import edu.cmu.lti.oaqa.framework.data.PassageCandidate;
import edu.cmu.lti.oaqa.framework.data.RetrievalResult;
import edu.cmu.lti.oaqa.openqa.hello.passage.SimplePassageExtractor;

public class SentencePassageExtractor extends SimplePassageExtractor {
  
  private static final String DOC_ID = "doc_id";
  private static final String PASSAGE = "passage";
  private static final String START = "start";
  private static final String END = "end";
  
  @Override
  protected List<PassageCandidate> extractPassages(String question, List<Keyterm> keyterms,
          List<RetrievalResult> documents) {
    List<PassageCandidate> result = new ArrayList<PassageCandidate>();
    
    Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_36); 
    Directory index = new RAMDirectory();
    
    for (RetrievalResult document : documents) {
      System.out.println("RetrievalResult: " + document.toString());
      String id = document.getDocID();
      try {
        // create Index
        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_36, analyzer);
        IndexWriter writer = new IndexWriter(index, config);
        
        String htmlText = wrapper.getDocText(id);
        htmlText = htmlText.substring(0, Math.min(5000, htmlText.length()));
        
        Map<Integer, Integer> paragraphs = extractParagraphs(htmlText);
        Iterator<Integer> it = paragraphs.keySet().iterator();
        while (it.hasNext()) {
          int begin = it.next();
          int end = paragraphs.get(begin);
          Map<Integer, Integer> sentences = extractSentences(htmlText, begin, end);
          createIndex(writer, id, htmlText, sentences);
        }
        writer.close();
        
        // Create query
        StringBuilder queryStr = new StringBuilder();
        for (Keyterm keyterm : keyterms) {
          queryStr.append(keyterm.getText());
          queryStr.append(" ");
        }
        
        Query q = new QueryParser(Version.LUCENE_36, PASSAGE, analyzer).parse(queryStr.toString());
        
        // Search through the passages
        int hitsPerPage = keyterms.size();
        IndexReader reader = IndexReader.open(index);
        IndexSearcher searcher = new IndexSearcher(reader);
        TopScoreDocCollector collector = TopScoreDocCollector.create(hitsPerPage, true);
        searcher.search(q, collector);
        ScoreDoc[] hits = collector.topDocs().scoreDocs;
        
        // Add passages
        for(int i=0;i<hits.length; ++i) {
          int docId = hits[i].doc;
          Document d = searcher.doc(docId);
          String psgDocId = d.get(DOC_ID);
          int start = Integer.parseInt(d.get(START));
          int end = Integer.parseInt(d.get(END));
          result.add(new PassageCandidate(psgDocId, start, end, hits[i].score, null));
        }
        searcher.close();
        
      } catch (SolrServerException e) {
        e.printStackTrace();
      } catch (CorruptIndexException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (LockObtainFailedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (ParseException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (AnalysisEngineProcessException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    return result;
  }
  
  /**
   * Creates an index 
   * 
   * @param writer
   * @param docId
   * @param text
   * @param sentences
   * @throws CorruptIndexException
   * @throws IOException
   */
  private void createIndex(IndexWriter writer, String docId, String text, 
          Map<Integer, Integer> sentences) throws CorruptIndexException, IOException {
    Iterator<Integer> it = sentences.keySet().iterator();
    while (it.hasNext()) {
      int begin = it.next();
      int end = sentences.get(begin);
      
      Document doc = new Document();
      doc.add(new Field(PASSAGE, text.substring(begin, end), Field.Store.YES, Field.Index.ANALYZED));
      doc.add(new Field(DOC_ID, docId, Field.Store.YES, Field.Index.ANALYZED));
      doc.add(new Field(START, Integer.toString(begin), Field.Store.YES, Field.Index.ANALYZED));
      doc.add(new Field(END, Integer.toString(end), Field.Store.YES, Field.Index.ANALYZED));
      
      writer.addDocument(doc);
    }
  }
  
  /**
   * Extracts paragraphs from document text
   * 
   * @param text document text
   * @return
   */
  private Map<Integer, Integer> extractParagraphs(String text) {
    Map<Integer, Integer> paragraphs = new HashMap<Integer, Integer>();
    Scanner scan = new Scanner(text);
    boolean inParagraph = false;
    int beginOffset = 0;
    int paraBegin = 0;
    int paraEnd = 0;
    while (scan.hasNext()) {
      String token = scan.next();
      if (token.contains("<P>") || token.contains("<p>")) {
        token = token.replace("<p>", "<P>"); // lower to upper
        // Set appropriate length offset for <P> tag
        int offset = 3;
        if (token.endsWith("<P>")) {
          offset = 4;
        }
        if (!inParagraph) {
          paraBegin = beginOffset + token.indexOf("<P>") + offset;
          inParagraph = true;
        }
        else {
          paraEnd = beginOffset + token.indexOf("<P>");
          System.out.println("----------Paragraph----------");
          System.out.println("begin: " + paraBegin + " end: " + paraEnd);
          System.out.println(text.substring(paraBegin, paraEnd));
          paragraphs.put(paraBegin, paraEnd);
          paraBegin = beginOffset + token.indexOf("<P>") + offset;
        }
      }
      else if (token.contains("</P>") || token.contains("</p>")) {
        token = token.replace("</p>", "</P>"); // lower to upper
        inParagraph = false;
        paraEnd = beginOffset + token.indexOf("</P>");
        System.out.println("begin: " + paraBegin + " end: " + paraEnd);
        System.out.println(text.substring(paraBegin, paraEnd));
        paragraphs.put(paraBegin, paraEnd);
      }
      beginOffset += token.length() + 1;
    }
    if (inParagraph) {
      paraEnd = text.length();
      System.out.println("----------Paragraph----------");
      System.out.println("begin: " + paraBegin + " end: " + paraEnd);
      System.out.println(text.substring(paraBegin, paraEnd));
      paragraphs.put(paraBegin, paraEnd);
    }
    return paragraphs;
  }
  
  /**
   * Extracts sentences from a paragraph
   * 
   * @param text document text
   * @param paraBegin beginning position of paragraph
   * @param paraEnd ending position of paragraph
   * @return
   */
  private Map<Integer, Integer> extractSentences(String text, int paraBegin, int paraEnd) {
    Map<Integer, Integer> sentences = new HashMap<Integer, Integer>();
    String paragraph = text.substring(paraBegin, paraEnd);
    
    int beginOffset = 0;
    int endOffset = 0;
    boolean inParantheses = false;
    Scanner scan = new Scanner(paragraph);
    while (scan.hasNext()) {
      String token = scan.next();
      endOffset += token.length();
      if (token.contains("(")) {
        inParantheses = true;
      }
      if (token.contains(")")) {
        inParantheses = false;
      }
      
      if (!token.contains(".") || (token.contains(".") && inParantheses)) {
        endOffset += 1;
      }
      else {
        System.out.println("<<<< Sentence >>>>");
        System.out.print("begin: " + beginOffset);
        System.out.println(" end: " + endOffset);
        System.out.println(text.substring(paraBegin + beginOffset, paraBegin + endOffset));
        sentences.put(paraBegin + beginOffset, paraBegin + endOffset);
        
        endOffset++;
        beginOffset = endOffset;
      }
    }
    
    return sentences;
  }
}
