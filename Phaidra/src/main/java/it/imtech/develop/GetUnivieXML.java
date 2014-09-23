package it.imtech.develop;

import it.imtech.globals.Globals;
import it.imtech.upload.SelectedServer;
import it.imtech.utility.Utility;
import it.imtech.xmltree.XMLUtil;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
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
            Logger.getLogger(GetUnivieXML.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(GetUnivieXML.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(GetUnivieXML.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SAXException ex) {
            Logger.getLogger(GetUnivieXML.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static void main(String[] args){
        SwingUtilities.invokeLater(new Runnable(){
            @Override
            public void run(){
                   new GetUnivieXML();
            }
        });
    };
}
