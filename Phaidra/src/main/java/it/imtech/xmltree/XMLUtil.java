package it.imtech.xmltree;

import it.imtech.bookimporter.BookImporter;
import it.imtech.globals.Globals;
import java.io.*;
import java.util.Enumeration;
import java.util.HashMap;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import javax.swing.tree.MutableTreeNode;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.log4j.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 *
 * @author luigi
 */
public class XMLUtil {

    public final static Logger logger = Logger.getLogger(XMLTree.class.getName());

    /**
     * xml writer document on console
     *
     * @param doc
     * @param xmlfile
     */
    public static void xmlWriter(Document doc) {

        StreamResult result = xmlWriterDoWork(doc, new StreamResult(new StringWriter()));

        // print xml
        logger.info("xmlString=" + result.getWriter().toString());


    }

    /**
     * xml writer document on file
     *
     * @param doc
     * @param xmlfile
     */
    public static void xmlWriter(Document doc, String xmlfile) {

        try {

            StreamResult result = xmlWriterDoWork(doc, new StreamResult(new FileOutputStream(xmlfile)));
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("xmlfile=" + xmlfile + ", ex=" + e.getStackTrace());
        }

    }

    private static StreamResult xmlWriterDoWork(Document doc, StreamResult result) {

        try {
            Transformer t = TransformerFactory.newInstance().newTransformer();
            t.setOutputProperty(OutputKeys.INDENT, "yes");
            t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

            DOMSource source = new DOMSource(doc);
            t.transform(source, result);

        } catch (TransformerConfigurationException e) {
            logger.error(e.getStackTrace());
        } catch (Exception e) {
            logger.error(e.getStackTrace());
        }
        return result;

    }

    public static HashMap<Integer, MutableTreeNode> getIndexNode(XMLNode node) {
        HashMap<Integer, MutableTreeNode> hm = new HashMap<Integer, MutableTreeNode>();
        MutableTreeNode parent = null;
        try {
            int index = 0;
            if (node.isLeaf()) {
                Enumeration<XMLNode> children = (Enumeration<XMLNode>) node.getParent().children();
                while (children.hasMoreElements()) {
                    XMLNode child = children.nextElement();
                    index++;
                    if (node.getName().equals(child.getName())) {
                        break;
                    }

                }
                parent = (MutableTreeNode) node.getParent();
            } else {
                //Enumeration<XMLNode> children = (Enumeration<XMLNode>) node.children();
                String tagName = ((Node) node.getUserObject()).getNodeName();
                if ("book:structure".equals(tagName) || "coll:pages".equals(tagName)) {
                    //if(children.hasMoreElements() && children.nextElement().isLeaf()) {
                    parent = (MutableTreeNode) node;
                    index = 0;
                }


            }

            if (parent != null) {
                hm.put(new Integer(index), parent);
            }


        } catch (Exception e) {
            logger.error(e.getStackTrace());
        }
        return hm;

    }

    public static HashMap<Integer, MutableTreeNode> getIndexNodes(XMLNode node) {
        HashMap<Integer, MutableTreeNode> hm = new HashMap<Integer, MutableTreeNode>();
        MutableTreeNode parent = null;
        try {
            int index = 0;
            if (node.isLeaf()) {
                Enumeration<XMLNode> children = (Enumeration<XMLNode>) node.getParent().children();
                while (children.hasMoreElements()) {
                    XMLNode child = children.nextElement();
                    index++;
                    if (node.getName().equals(child.getName())) {
                        break;
                    }

                }
                parent = (MutableTreeNode) node.getParent();
            } else {
                //Enumeration<XMLNode> children = (Enumeration<XMLNode>) node.children();
                String tagName = ((Node) node.getUserObject()).getNodeName();
                if ("book:structure".equals(tagName)) {
                    //if(children.hasMoreElements() && children.nextElement().isLeaf()) {
                    parent = (MutableTreeNode) node;
                    index = 0;
                }


            }

            if (parent != null) {
                hm.put(new Integer(index), parent);
            }


        } catch (Exception e) {
            logger.error(e.getStackTrace());
        }
        return hm;

    }

    public static XMLNode createXMLNode(String name) {
        XMLNode xmlnode = null;
        try {

            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = builder.newDocument();
            Element page = doc.createElement("book:page");

            page.setAttribute("pid", name);
            page.setAttribute("firstpage", "false");
            page.setAttribute("abspagenum", "-1");
            page.setAttribute("pagenum", "-1");
            xmlnode = new XMLNode(page);

        } catch (Exception e) {
            logger.error(e.getStackTrace());
        }
        return xmlnode;

    }

    public static void copyfile(String srFile, String dtFile) {
        try {
            File f1 = new File(srFile);
            File f2 = new File(dtFile);
            InputStream in = new FileInputStream(f1);

            //For Overwrite the file.
            OutputStream out = new FileOutputStream(f2);

            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        } catch (FileNotFoundException ex) {
            logger.error(ex.getStackTrace());
        } catch (IOException e) {
            logger.error(e.getStackTrace());
        }
    }

    /**
     * Restituisce il path del file scelto per l'esportazione o l'importazione
     * di un file pdf, Null se non viene scelto un file
     *
     * @param exp
     * @return
     * @author Medelin Mauro
     */
    public static String[] chooseFileImage() {
        String[] nomeFiles = null;
        JFileChooser chooser = new JFileChooser();//new save dialog  
        chooser.setMultiSelectionEnabled(true);

        javax.swing.filechooser.FileFilter filter = new FileNameExtensionFilter("image file", "jpeg", "jpg", "png", "tif", "tiff");
        chooser.addChoosableFileFilter(filter);

        int ret = chooser.showDialog(BookImporter.getInstance(), "Select File");

        boolean selected = false;

        if (ret == JFileChooser.APPROVE_OPTION) {
            File[] files = chooser.getSelectedFiles();
            nomeFiles=new String[files.length];
            for (int i=0; i < files.length; i++) {
                File f=files[i];
                nomeFiles[files.length-1-i]=f.getName();
                if (!new File(Globals.SELECTED_FOLDER_SEP + f.getName()).exists()) {
                    copyfile(f.getAbsolutePath(), Globals.SELECTED_FOLDER_SEP + f.getName());
                }

            }
            selected = true;
        }

        if (selected == true) {
            return nomeFiles;
        } else {
            return null;
        }
    }
    
}
