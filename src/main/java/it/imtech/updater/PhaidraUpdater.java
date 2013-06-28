/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package it.imtech.updater;

import it.imtech.bookimporter.BookImporter;
import java.io.File;
import java.net.URL;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.commons.configuration.XMLConfiguration;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 *
 * @author mauro
 */
public class PhaidraUpdater {
    
    public String getLatestVersion() throws Exception {
        XMLConfiguration config = new XMLConfiguration(BookImporter.urlConfig);

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        
        URL url = new URL(config.getString("urlupdater.descrurl"));
        Document doc = dBuilder.parse(url.openStream());
         
        NodeList nl = doc.getElementsByTagName("version");
        
        Element el = (Element) nl.item(0);
        
        return el.getTextContent();
    }

    public String getWhatsNew() throws Exception {
        XMLConfiguration config = new XMLConfiguration(BookImporter.urlConfig);
        
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            
        URL url = new URL(config.getString("urlupdater.descrurl"));
        
        Document doc = dBuilder.parse(url.openStream());
        
        NodeList nl = doc.getElementsByTagName("history");
        
        Element el = (Element) nl.item(0);
        
        return el.getTextContent();
    }
}
