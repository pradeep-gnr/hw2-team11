package edu.cmu.lti.oaqa.openqa.test.team11.keyterm.hankh;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

public class LocalGeneSearch {
  private Set<String> mGeneSet = new HashSet<String>();
  private List<String> foundList = new ArrayList<String>();
  private List<String> unfoundList = new ArrayList<String>();
  
  public List<String> getFoundList() {
    return foundList;
  }
  
  public Set<String> getGeneSet() {
    return mGeneSet;
  }
  
  public List<String> getUnfoundList() {
    return unfoundList;
  }
  
  public boolean openGeneSet(String filename) {
    try {
      File geneSetFile = new File(filename);
      Scanner fileScanner = new Scanner(geneSetFile);
      while(fileScanner.hasNextLine()) {
        mGeneSet.add(fileScanner.nextLine());
      }
      return true;
    } catch (FileNotFoundException e) {
      return false;
    }
  }
  
  public void search(List<String> strList) {
    for (String str : strList) {
      boolean found = false;
      for (String gene : mGeneSet) {
        int[] result = searchNamedEntity(str, gene);
        if (result != null) {
          int begin = result[0];
          int end = result[1];

          // store gene into found list
          foundList.add(str.substring(begin, end));
          found = true;
        }
      }
      
      // Add unfound named entity to unfound list
      if (!found) {
        unfoundList.add(str);
      }
    }
  }
  
  /**
   * Search NamedEntity for specific string found from local gene set
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
      int begin = namedEntity.indexOf(name);
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
}
