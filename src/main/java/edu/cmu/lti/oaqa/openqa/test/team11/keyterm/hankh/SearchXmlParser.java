package edu.cmu.lti.oaqa.openqa.test.team11.keyterm.hankh;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * SAX Parser to parse NCBI search results
 * 
 * @author Hank
 *
 */
public class SearchXmlParser extends DefaultHandler {
  private static final String TAG_ID = "id";
  
  private String mTempVal;
  private List<String> mIdList = new ArrayList<String>();
  
  /**
   * Takes the input and parses the input document
   * 
   * @param input document to be parsed 
   * @throws ParserConfigurationException - if an error occurs while creating a new SAX Parser
   * @throws SAXException - if an error occurs while creating a new SAX Parser or parsing a document
   * @throws IOException - if an error occurs while parsing a document
   */
  public void parseDocument(InputStream input) throws ParserConfigurationException, SAXException, IOException {
    SAXParserFactory spf = SAXParserFactory.newInstance();
    SAXParser parser = spf.newSAXParser();
    parser.parse(input, this);
  }
  
  @Override
  public void endElement(String uri, String localName, String qName) {
    if (qName.equalsIgnoreCase(TAG_ID)) {
      mIdList.add(mTempVal);
    }
  }
  
  @Override
  public void characters(char[] ch, int start, int length) {
    mTempVal = new String(ch, start, length);
  }
  
  /**
   * @return list of id
   */
  public List<String> getIdList() {
    return mIdList;
  }

}
