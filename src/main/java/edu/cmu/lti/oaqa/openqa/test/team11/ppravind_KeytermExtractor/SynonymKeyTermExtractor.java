package edu.cmu.lti.oaqa.openqa.test.team11.ppravind_KeytermExtractor;

/*
 * Class for Expanding Synonym Sets for words in KeyTerm Extractor and building a 
 * bigger list
 * 
 */

import java.io.File;
import java.net.URISyntaxException;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.net.URL;

import com.aliasi.chunk.Chunk;
import com.aliasi.chunk.Chunker;
import com.aliasi.chunk.Chunking;
import com.aliasi.util.AbstractExternalizable;

import edu.cmu.lti.oaqa.cse.basephase.keyterm.AbstractKeytermExtractor;
import edu.cmu.lti.oaqa.framework.data.Keyterm;
import edu.cmu.lti.oaqa.framework.data.KeytermList;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.trees.Tree;

import edu.smu.*;
import edu.smu.tspell.*;
import edu.smu.tspell.wordnet.*;


public class SynonymKeyTermExtractor extends AbstractKeytermExtractor {
  //set lingpipe bio gene tag chunker
  File geneTag;
  public HashMap stopWords = new HashMap();
  
  /*
   * WordNet database
   */  
  WordNetDatabase database = WordNetDatabase.getFileInstance();
  
  
  public SynonymKeyTermExtractor()
  {
	  /*
	   * Initialize the WordNet dictionary Files for Synonym Extraction
	   */
	  URL word_path = this.getClass().getClassLoader().getResource("wordnet_dict");
	  System.setProperty("wordnet.database.dir", word_path.getPath());
	  /*
	   * Initialize Stop Words Dictionary !
	   */
	  System.out.println("Initializing Stop Words");
	  File file;
	  URL url = this.getClass().getClassLoader().getResource("stopwords.txt");
	  
	  try {
		  file = new File(url.toURI());
		} catch(URISyntaxException e) {
		  file = new File(url.getPath());
		}
	  //File file = new File("/stopwords.txt");
	  BufferedReader br=null;
	  
	/*
	 * Initialize the Model File
	 */
	  
	 URL url2 = this.getClass().getResource("/genetag/ne-en-bio-genetag.HmmChunker");
	 File file1;	  
	  
	  try {
		  file1 = new File(url2.toURI());
		} catch(URISyntaxException e) {
		  file1 = new File(url2.getPath());
		}
	  this.geneTag = file1;
	try {
		br = new BufferedReader(new FileReader(file));
		String line;
		System.out.println("Initializing StopWords");
		try {
			while ((line = br.readLine()) != null) {
			   //
				line = line.replaceAll("\n ", " ");
				
				stopWords.put(line,true);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	} catch (FileNotFoundException e) {
		// TODO Auto-generated catch block
		System.out.println("StopWords.txt not found");
		e.printStackTrace();
	}
	  try {
		br.close();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	  /*
	   * Initialize WordNet databases
	   */
	  
  }
  
  public String getWordSense(String word, String tagType)
  {
	  /*
	   * Get the Word Sense for a Particular Word !
	   */
	  return null;
  }
  
  public ArrayList<String> getSynonyms(String keyterm, String tagType,int noSyn)
     {
 	  /*
	   * Returns the Synonyms of the KeyTerms
	   */
	  ArrayList synList = new ArrayList();
	  NounSynset nounSynset; 
	  NounSynset[] nounHyponyms;
	  VerbSynset verbSynset;
	  VerbSynset[] verbHyponyms;
	  
	  Synset[] synsets = null;
	  synList.add(keyterm);
	  
	  /*
	   * Set of all Synonyms
	   */
	  HashSet finalSet ;
	  int synCount = 0 ;
	  
	  if(tagType=="noun")
	  {
		  synsets = database.getSynsets(keyterm,SynsetType.NOUN);
		  for (int i = 0; i < synsets.length; i++) { 
		      nounSynset = (NounSynset)(synsets[i]);
		     
		      String[] wordForms = nounSynset.getWordForms();
		      for(String each_word: wordForms)
		      {
		    	  synList.add(each_word);
		    	  synCount+=1;
		    	  if(synCount==noSyn)		    		  
		    		  break;
		    			  
		      }
		      //System.out.println(hyponyms);		      
		  }
	  }
	  
	  else if(tagType=="verb")
	  {
		 synsets = database.getSynsets(keyterm,SynsetType.VERB);
		 for (int i = 0; i < synsets.length; i++) { 
		      verbSynset = (VerbSynset)(synsets[i]);		      
		      String[] wordForms = verbSynset.getWordForms();
		      for(String each_word: wordForms)
		      {
		    	  synList.add(each_word);
		    	  synCount+=1;
		    	  if(synCount==noSyn)	    		  
		    		  break;
		    	  
		      }
		      //System.out.println(hyponyms);
		      
		  }
	  }	 	  
	  
	  finalSet = new HashSet(synList);
	  ArrayList<String> finalKeytermList = new ArrayList<String>(finalSet); 
	  
	  return finalKeytermList;	  
  }
  
  public ArrayList<Keyterm> addSynonyms(ArrayList<Keyterm> keyTermList,HashMap tagMap)
  {
	  ArrayList<Keyterm> newKeyTermList = new ArrayList<Keyterm> ();
	  
	  for(Keyterm keyterm: keyTermList)
	  {
		  String key = keyterm.getText();		  
		  ArrayList<String> compSynList = this.getSynonyms(key, (String) tagMap.get(key),3);
		  for (String eachSynonym: compSynList)
		  	{
			  newKeyTermList.add(new Keyterm(eachSynonym));
		  	}
	  }
	  
	  return newKeyTermList;
  }

  @Override
  protected List<Keyterm> getKeyterms(String question) {

    ArrayList<Keyterm> keyTermList = new ArrayList<Keyterm>();
    HashMap tagMap = new HashMap() ;
    ArrayList<Keyterm> keytermWithSynonyms = null;
    

    try {
      Chunker chunker = (Chunker) AbstractExternalizable.readObject(geneTag); 
          
      Chunking chunking = chunker.chunk(question);
      Set<Chunk> chunkSet = chunking.chunkSet();
      for(Chunk chunk : chunkSet) {  
        Keyterm k = new Keyterm(question.substring(chunk.start(), chunk.end()));    
        keyTermList.add(k);          
      }
      
      LexicalizedParser lp = LexicalizedParser.loadModel("edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz"); 
     
      Tree tree = lp.apply(question);
      List<TaggedWord> taggedWords = tree.taggedYield();
      for (TaggedWord tw : taggedWords) {
    	  String tagType = null;
        if (tw.tag().startsWith("N") || tw.tag().startsWith("V")) {
        	
        	if(tw.tag().startsWith("N"))
        	   tagType = "noun";
        	
        	else
        		tagType ="verb";
          	
          if (stopWords.get(tw.word())==null)	
          {   
        	  tagMap.put(tw.word(),tagType);
        	  Keyterm k = new Keyterm(tw.word());
              keyTermList.add(k);                          
          }
          /*
          if (!tw.word().equals("is") && (!tw.word().equals("are")) & (!tw.word().equals("do")) && (!tw.word().equals("does"))) {
            Keyterm k = new Keyterm(tw.word());
            keyTermList.add(k);
           
          }
          */          
          keytermWithSynonyms = this.addSynonyms(keyTermList,tagMap);
        }
      }  
   
    } catch (IOException e) {
      e.printStackTrace();
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    }
    System.out.println(keyTermList);
    return keytermWithSynonyms;
  }
  
  public static void main(String args[])
  {
	  SynonymKeyTermExtractor key = new SynonymKeyTermExtractor();
	  System.out.println(key.getKeyterms("What is the role of APC (adenomatous polyposis coli) in colon cancer?"));
	  
	  
  }
  
   
}