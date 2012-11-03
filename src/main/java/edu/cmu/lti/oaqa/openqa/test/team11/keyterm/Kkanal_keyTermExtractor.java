package edu.cmu.lti.oaqa.openqa.test.team11.keyterm;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.UimaContext;
import org.apache.uima.resource.ResourceInitializationException;

import abner.Tagger;
import edu.cmu.lti.oaqa.cse.basephase.keyterm.AbstractKeytermExtractor;
import edu.cmu.lti.oaqa.framework.data.Keyterm;

public class Kkanal_keyTermExtractor extends AbstractKeytermExtractor {

   
  @Override
  protected List<Keyterm> getKeyterms(String arg0) {
    System.out.println("string is:: "+arg0);
    // TODO Auto-generated method stub
    ArrayList<Keyterm> keyTermList;
    keyTermList = new ArrayList<Keyterm>(); 
    try {
           Tagger t = new Tagger(Tagger.BIOCREATIVE);
      String[] op = t.getEntities(arg0, "PROTEIN");
      if (op.length != 0) {
        for (int i1 = 0; i1 < op.length; i1++) {
          Keyterm key = new Keyterm(op[i1]);
          keyTermList.add(key);
        }
      }
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return keyTermList;
  }

}
