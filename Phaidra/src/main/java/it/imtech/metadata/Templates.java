/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package it.imtech.metadata;

import it.imtech.bookimporter.BookImporter;
import it.imtech.globals.Globals;
import it.imtech.xmltree.XMLUtil;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author mauro
 */
public  class Templates {
    public final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(Templates.class);
    
    public static boolean deleteTemplateXML(String filename, String filetitle){
        return true;
    }
    
    public static boolean addTemplateXML(String filename, String filetitle){
        File templatexml = new File(Globals.TEMPLATES_XML);

        boolean result = false;
        
        Document doc = null;
        Element root = null;   
        
        try {
            DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();        
                          
            if (!templatexml.isFile()){
                doc = dBuilder.newDocument();
                root = doc.createElement("templates");
                doc.appendChild(root);
            }
            else{
                doc = dBuilder.parse(templatexml);
                NodeList roots = doc.getElementsByTagName("templates");
                
                if (roots.getLength()==1){
                    root = (Element) roots.item(0);
                }
            }
            
            if (root != null){
                Element template = doc.createElement("template");
                template.setAttribute("title", filetitle);
                template.setAttribute("file", filename);
                
                root.appendChild(template);
                XMLUtil.xmlWriter(doc, Globals.TEMPLATES_XML);
                result = true;
            }
            else{
                throw new Exception("Errore durante la scrittura del file templates.xml");
            }
        } catch (ParserConfigurationException ex) {
            logger.error(ex.getMessage());
        } catch (SAXException ex) {
            logger.error(ex.getMessage());
        } catch (IOException ex) {
            logger.error(ex.getMessage());
        } catch (Exception ex) {
            logger.error(ex.getMessage());
        }
        
        return result;
    }
}
