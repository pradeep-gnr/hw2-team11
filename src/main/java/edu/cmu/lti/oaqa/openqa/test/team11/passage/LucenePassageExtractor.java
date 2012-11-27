package edu.cmu.lti.oaqa.openqa.test.team11.passage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.jsoup.Jsoup;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

import edu.cmu.lti.oaqa.framework.data.Keyterm;
import edu.cmu.lti.oaqa.framework.data.PassageCandidate;
import edu.cmu.lti.oaqa.framework.data.RetrievalResult;
import edu.cmu.lti.oaqa.openqa.hello.passage.KeytermWindowScorerSum;
import edu.cmu.lti.oaqa.openqa.hello.passage.PassageCandidateFinder;
import edu.cmu.lti.oaqa.openqa.hello.passage.SimplePassageExtractor;

public class LucenePassageExtractor extends SimplePassageExtractor {

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
      String id = document.getDocID();
      
      try {
        // Create index
        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_36, analyzer);
        IndexWriter writer = new IndexWriter(index, config);
        createIndex(id, keyterms, writer);
        writer.close();
        
        // Create query
        StringBuilder queryStr = new StringBuilder();
        for (Keyterm keyterm : keyterms) {
          queryStr.append(keyterm.getText());
          queryStr.append(" ");
        }
        
        Query q = new QueryParser(Version.LUCENE_36, "passage", analyzer).parse(queryStr.toString());
        
        // Search through the passages
        int hitsPerPage = keyterms.size();
        IndexReader reader = IndexReader.open(index);
        IndexSearcher searcher = new IndexSearcher(reader);
        TopScoreDocCollector collector = TopScoreDocCollector.create(hitsPerPage, true);
        searcher.search(q, collector);
        ScoreDoc[] hits = collector.topDocs().scoreDocs;
        
        // Add passages
        for(int i=0;i<hits.length;++i) {
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
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      } catch (ParseException e) {
        e.printStackTrace();
      } catch (AnalysisEngineProcessException e) {
        e.printStackTrace();
      }
    }
    return result;
  }
  
  private void createIndex(String id, List<Keyterm> keyterms, IndexWriter writer) throws SolrServerException, CorruptIndexException, IOException {
    String htmlText = wrapper.getDocText(id);
    
    // cleaning HTML text
    String text = Jsoup.parse(htmlText).text().replaceAll("([\177-\377\0-\32]*)", "")/* .trim() */;
    // for now, making sure the text isn't too long
    text = text.substring(0, Math.min(5000, text.length()));
    
    PassageCandidateFinder finder = new PassageCandidateFinder(id, text,
            new KeytermWindowScorerSum());
    List<String> keytermStrings = Lists.transform(keyterms, new Function<Keyterm, String>() {
      public String apply(Keyterm keyterm) {
        return keyterm.getText();
      }
    });
    
    List<PassageCandidate> passageSpans = finder.extractPassages(keytermStrings.toArray(new String[0]));
    
    // Create index
    for (PassageCandidate passageSpan : passageSpans) {
      Document doc = new Document();
      doc.add(new Field(PASSAGE, text.substring(passageSpan.getStart(), passageSpan.getEnd()), 
              Field.Store.YES, Field.Index.ANALYZED));
      doc.add(new Field(DOC_ID, passageSpan.getDocID(), Field.Store.YES, Field.Index.ANALYZED));
      doc.add(new Field(START, Integer.toString(passageSpan.getStart()), Field.Store.YES, Field.Index.ANALYZED));
      doc.add(new Field(END, Integer.toString(passageSpan.getEnd()), Field.Store.YES, Field.Index.ANALYZED));
      
      writer.addDocument(doc);
    }
  }

}
