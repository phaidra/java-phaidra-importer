/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package it.imtech.develop;

import java.io.File;
import java.io.IOException;
import javax.swing.SwingUtilities;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author mauro
 */
public class MetadataParser {
    private static final Logger logger = Logger.getLogger(MetadataParser.class);
    
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
            logger.error(ex.getMessage());
        } catch (XPathExpressionException ex) {
            logger.error(ex.getMessage());
        } catch (SAXException ex) {
            logger.error(ex.getMessage());
        } catch (IOException ex) {
            logger.error(ex.getMessage());
        } 
    }
    
    public static void main(String[] args){
        SwingUtilities.invokeLater(new Runnable(){
            
            public void run(){
                   new MetadataParser();
            }
        });
    };
}
