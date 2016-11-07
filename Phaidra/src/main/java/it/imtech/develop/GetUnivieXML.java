package it.imtech.develop;

import it.imtech.globals.Globals;
import it.imtech.xmltree.XMLUtil;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import javax.swing.SwingUtilities;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author mauro
 */
public class GetUnivieXML {
    private static final Logger logger = Logger.getLogger(AddPageToBook.class);
    
    public GetUnivieXML(){
        try {
            Globals.setGlobalVariables();
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            
            File urlf = null;
            URL url = null;
            Document doc;
            String path;
            
            path = "http://static.phaidra.univie.ac.at/xml/config.xml";
            url = new URL(path);
            
            doc = dBuilder.parse(url.openStream());

            XMLUtil.xmlWriter(doc, "C:\\Users\\mauro\\Desktop\\univie\\config.xml");
        } catch (MalformedURLException ex) {
            logger.error(ex.getMessage());
        } catch (IOException ex) {
            logger.error(ex.getMessage());
        } catch (ParserConfigurationException ex) {
            logger.error(ex.getMessage());
        } catch (SAXException ex) {
            logger.error(ex.getMessage());
        }
    }
    
    public static void main(String[] args){
        SwingUtilities.invokeLater(new Runnable(){
            
            public void run(){
                   new GetUnivieXML();
            }
        });
    };
}
