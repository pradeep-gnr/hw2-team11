package edu.cmu.lti.oaqa.openqa.test.team11.ppravind_KeytermExtractor;


import com.aliasi.chunk.Chunker;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.aliasi.chunk.Chunking;
import com.aliasi.util.AbstractExternalizable;
import java.io.File;
import java.util.Set;
import java.util.HashSet;

import java.util.List;
import java.util.ArrayList;

import org.apache.uima.resource.ResourceInitializationException;

import edu.cmu.lti.oaqa.cse.basephase.keyterm.AbstractKeytermExtractor;
import edu.cmu.lti.oaqa.framework.data.Keyterm;

public class NounPhraseGeneExtractor extends AbstractKeytermExtractor {
	
	Chunker chunker;
	NounPhraseExtractor np;
	
	public NounPhraseGeneExtractor()
	{
		File modelFile = new File ("src/main/java/edu/cmu/lti/oaqa/openqa/test/team11/ppravind_KeytermExtractor/models/ne-en-bio-genetag.HmmChunker");
		try {
			this.np = new NounPhraseExtractor();
		} catch (ResourceInitializationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	 	  
		  try{	  		  
			 this.chunker = (Chunker) AbstractExternalizable.readObject(modelFile);
		  }  
		 catch(Exception e)
		 {
			 System.out.println("Exception Occured" );
		 }		
	}
	
	public <T> List<T> union(List<T> list1, List<T> list2) {
        Set<T> set = new HashSet<T>();

        set.addAll(list1);
        set.addAll(list2);

        return new ArrayList<T>(set);
    }
	
	public Chunking getNamedEntities(String contents)
	{				
           			
				Chunking chunking = this.chunker.chunk(contents);				
				return chunking;			
			
	}

	@Override
	protected List<Keyterm> getKeyterms(String arg0) {
		// Return a List of KeyTerms
		ArrayList<Keyterm> keyTermList;

		keyTermList=  new ArrayList();
		
		Chunking chunking = this.getNamedEntities(arg0);		
		String chunkingOutput = chunking.chunkSet().toString();	
		String pattern = "(\\d*-\\d*):GENE@-Infinity";			
		Pattern genePattern = Pattern.compile(pattern);
		
		// Read a question, extaract keyTerms and add them to the ArrayList
		Matcher matcher = genePattern.matcher(chunkingOutput);
		
		while(matcher.find())
		{			
		int pos1 = Integer.parseInt(matcher.group().split(":")[0].split("-")[0]);		
		int pos2 = Integer.parseInt(matcher.group().split(":")[0].split("-")[1]);	
		Keyterm kterm = new Keyterm(arg0.substring(pos1,pos2));
		
		keyTermList.add(kterm);
			
		}
		
		ArrayList genericKeyterms = this.np.getNounPhrases(arg0);
		ArrayList finalKeyTerms = (ArrayList) this.union(keyTermList,genericKeyterms);
		
		return finalKeyTerms;
	}	
	
	public static void main(String args[])
	{
	  NounPhraseGeneExtractor np = new NounPhraseGeneExtractor();
	  ArrayList queryTerms =(ArrayList) np.getKeyterms("This is a Sample C43 gene question DNA crap");
	  System.out.println(queryTerms);
	  ArrayList query1Terms =(ArrayList) np.getKeyterms("What is the role of APC (adenomatous polyposis coli) in colon cancer?");
	  System.out.println(query1Terms);
	  
	}
	

}
