/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package it.imtech.vocabularies;

import it.imtech.bookimporter.BookImporter;
import it.imtech.globals.Globals;
import it.imtech.upload.PhaidraUtils;
import it.imtech.utility.Utility;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.TreeMap;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author mauro
 */
public class Vocabulary {
  private static Vocabulary istance;
  private ResourceBundle bundle = ResourceBundle.getBundle(BookImporter.RESOURCES, BookImporter.currentLocale, Globals.loader);
                               
  public static Vocabulary getInstance(){
    if (istance == null){
      istance = new Vocabulary();
    }
    return istance;
  }
  
  public HashMap<String,TreeMap<String,VocEntry>> getVocabularies() throws Exception{
    HashMap values = new HashMap();
     try {
        Document doc = Utility.getDocument(Globals.URL_VOCABULARY,false);
        
        
        Node n = doc.getFirstChild();
        if (n.getNodeType() == Node.ELEMENT_NODE) {
            Element iENode = (Element) n;
            if (iENode.getTagName().equals("vocabularies")) {
                NodeList nList = iENode.getChildNodes();

                for (int s = 0; s < nList.getLength(); s++) {
                    if (nList.item(s).getNodeType() == Node.ELEMENT_NODE) {
                        Element iInnerNode = (Element) nList.item(s);

                        if (iInnerNode.getTagName().equals("vocabulary")) {
                            String MID = iInnerNode.getAttribute("MID");
                            if (MID != null)
                            {
                                if (!(values.containsKey(MID)))
                                {
                                    TreeMap<String,VocEntry> entries = get_vocabulary_entries(iInnerNode);
                                    values.put(MID, entries);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    catch (Exception ex) {
        throw ex; 
    } 
       
    return values;     
  } 
  
  private TreeMap<String,VocEntry> get_vocabulary_entries(Element iInnerNode) throws Exception{
    TreeMap<String,VocEntry> values = new TreeMap<String,VocEntry>();
    String ID = null;
    String description = null;
    String DE_VOC = null;
    String sLang = BookImporter.currentLocale.toString();
    
    NodeList nList = iInnerNode.getChildNodes();
    
    for (int s = 0; s < nList.getLength(); s++) {
        if (nList.item(s).getNodeType() == Node.ELEMENT_NODE) {
           Element iNode = (Element) nList.item(s);
           
           if (iNode.getTagName().equals("entry") && ID==null) {
                ID = iNode.getAttribute("ID");
                
                //Inner description nodes
                NodeList descrList = iNode.getChildNodes();
                for (int z = 0; z < descrList.getLength(); z++) {
                    if (descrList.item(z).getNodeType() == Node.ELEMENT_NODE) {
                         Element descrNode = (Element) descrList.item(z);
                         if (descrNode.getTagName().equals("description")){
                             if (descrNode.getAttribute("isocode").equals(sLang)){
                                description = descrNode.getTextContent();
                             }
                             else 
                                 if (descrNode.getAttribute("isocode").equals("de") && description == null){
                                     DE_VOC = descrNode.getTextContent();
                                 }
                         }
                    }
                }
           }
           
           description = (description == null)?DE_VOC:description;
                          
           if (description == null){
                description = "";
           }
           else{
                values.put(description,new VocEntry(ID, description));
                ID = null;
                description = null;
           }                       
        }
    }
    return values;    
  }
}
