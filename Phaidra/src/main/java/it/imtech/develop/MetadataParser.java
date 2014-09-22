/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package it.imtech.develop;

import it.imtech.bookimporter.BookImporter;
import it.imtech.metadata.MetaUtility;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import javax.swing.SwingUtilities;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author mauro
 */
public class MetadataParser {
    public MetadataParser(){
        String filetoparse = "/Users/mede318/testphaidra/uwmetadata.xml";
              
        try {
            DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc;
            
            XPathFactory factory = XPathFactory.newInstance();
	    XPath xpath = factory.newXPath();
            
            File impmetadata = new File(filetoparse);
            doc = dBuilder.parse(impmetadata);
            
            String expression = "//ns1:contribute";
            NodeList nodeList = (NodeList) xpath.evaluate(expression, doc, XPathConstants.NODESET);
            System.out.println("Expression: " + expression + " --> Size: "+Integer.toString(nodeList.getLength()));
            
            expression = "//contribute";
            nodeList = (NodeList) xpath.evaluate(expression, doc, XPathConstants.NODESET);
            System.out.println("Expression: " + expression + " --> Size: "+Integer.toString(nodeList.getLength()));
            
            expression = "//*[local-name()='contribute']";
            nodeList = (NodeList) xpath.evaluate(expression, doc, XPathConstants.NODESET);
            System.out.println("Expression: " + expression + " --> Size: "+Integer.toString(nodeList.getLength()));
            
            expression = "//*[local-name()='taxonpath']";
            nodeList = (NodeList) xpath.evaluate(expression, doc, XPathConstants.NODESET);
            System.out.println("Expression: " + expression + " --> Size: "+Integer.toString(nodeList.getLength()));
            
            expression = "//*[local-name()='taxonpath'][@seq='8']";
            nodeList = (NodeList) xpath.evaluate(expression, doc, XPathConstants.NODESET);
            System.out.println("Expression: " + expression + " --> Size: "+Integer.toString(nodeList.getLength()));
            
            
        } catch (ParserConfigurationException ex) {
            java.util.logging.Logger.getLogger(MetaUtility.class.getName()).log(Level.SEVERE, null, ex);
        } catch (XPathExpressionException ex) {
            java.util.logging.Logger.getLogger(BookImporter.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SAXException ex) {
            java.util.logging.Logger.getLogger(BookImporter.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(BookImporter.class.getName()).log(Level.SEVERE, null, ex);
        } 
    }
    
    public static void main(String[] args){
        SwingUtilities.invokeLater(new Runnable(){
            @Override
            public void run(){
                   new MetadataParser();
            }
        });
    };
}
