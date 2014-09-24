/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | TemplatesUtility
 * and open the template in the editor.
 */

package it.imtech.metadata;

import it.imtech.globals.Globals;
import it.imtech.xmltree.XMLUtil;
import java.io.File;
import java.io.IOException;
import java.util.ResourceBundle;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author mauro
 */
public  class TemplatesUtility {
    public final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(TemplatesUtility.class);
    
    public static boolean deleteTemplateXML(String filetitle,String filename){
        File templatexml = new File(Globals.TEMPLATES_XML);
        boolean result = false;
        
        if (templatexml.isFile()){
            try {
                DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                Document doc = dBuilder.newDocument();
                
                doc = dBuilder.parse(templatexml);
                
                NodeList template = doc.getElementsByTagName("template");
                
                for (int i=0;i<template.getLength();i++){
                    if (template.item(i).getNodeType() == Node.ELEMENT_NODE) {
                        Element node = (Element) template.item(i);
                        String type = node.getAttribute("type");
                        
                        if (Character.toString(Globals.TYPE_BOOK).equals(type)){
                            if (node.getAttribute("title").equals(filetitle) && 
                                node.getAttribute("file").equals(filename)){
                                
                                node.getParentNode().removeChild(node);
                                XMLUtil.xmlWriter(doc, Globals.TEMPLATES_XML);
                                
                                File temp = new File(Globals.TEMPLATES_FOLDER_SEP + filename);
                                if (temp.isFile()){
                                    temp.delete();
                                    result = true;
                                }
                            }
                        }
                    }
                }
            } catch (SAXException ex) {
                Logger.getLogger(TemplatesUtility.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(TemplatesUtility.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ParserConfigurationException ex) {
                Logger.getLogger(TemplatesUtility.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return result;
    }
    
    
    public static TreeMap<String, String> getTemplatesList(){
        TreeMap<String, String> templatelist = new TreeMap<String, String>();
        
        File templatexml = new File(Globals.TEMPLATES_XML);
        
        if (templatexml.isFile()){
            try {
                DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                Document doc = dBuilder.newDocument();
                
                doc = dBuilder.parse(templatexml);
                
                NodeList template = doc.getElementsByTagName("template");
                
                for (int i=0;i<template.getLength();i++){
                    if (template.item(i).getNodeType() == Node.ELEMENT_NODE) {
                        Element node = (Element) template.item(i);
                        String type = node.getAttribute("type");
                        
                        if (Character.toString(Globals.TYPE_BOOK).equals(type)){
                            templatelist.put(node.getAttribute("title"),node.getAttribute("file"));
                        }
                    }
                }
                
                
            } catch (SAXException ex) {
                Logger.getLogger(TemplatesUtility.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(TemplatesUtility.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ParserConfigurationException ex) {
                Logger.getLogger(TemplatesUtility.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return templatelist;
    }
    
    
    public static boolean templateExists(String filetitle){
        File templatexml = new File(Globals.TEMPLATES_XML);
        boolean result = false;
        
        if (templatexml.isFile()){
            try {
                DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                Document doc = dBuilder.newDocument();
                
                doc = dBuilder.parse(templatexml);
                
                NodeList template = doc.getElementsByTagName("template");
                
                for (int i=0; !result && i<template.getLength();i++){
                    if (template.item(i).getNodeType() == Node.ELEMENT_NODE) {
                        Element node = (Element) template.item(i);
                        String title = node.getAttribute("title");
                        
                        if (title != null && title.equals(filetitle)){
                            result = true;
                        }
                    }
                }
            } catch (SAXException ex) {
                Logger.getLogger(TemplatesUtility.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(TemplatesUtility.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ParserConfigurationException ex) {
                Logger.getLogger(TemplatesUtility.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        return result;
    }
    
    
    public static boolean addTemplateXML(String filename, String filetitle){
        ResourceBundle bundle = ResourceBundle.getBundle(Globals.RESOURCES,Globals.CURRENT_LOCALE, Globals.loader);
        TreeMap<String, String> templates = new TreeMap<String, String>();
        
        boolean result = false;
        
        Document doc = null;
        Element root = null;   
        
        File templatexml = new File(Globals.TEMPLATES_XML);
            
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

                if (roots != null && roots.getLength()==1){
                    root = (Element) roots.item(0);
                }
            }
            
            if (root != null){
                Element template = doc.createElement("template");
                template.setAttribute("title", filetitle);
                template.setAttribute("file", filename);
                template.setAttribute("type", Character.toString(Globals.TYPE_BOOK));
                
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
