package it.imtech.xmltree;

import it.imtech.bookimporter.BookImporter;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.apache.log4j.Logger;
/**
 *
 * @author luigi
 */
public class XMLTreeModelListener implements TreeModelListener {
    public final static Logger logger = Logger.getLogger(XMLTreeModelListener.class.getName());

    public void treeNodesChanged(TreeModelEvent e) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) (e.getTreePath().getLastPathComponent());
        Element element;
        
        try {
            int index = e.getChildIndices()[0];
            XMLNode xmlNode = (XMLNode) (node.getChildAt(index));
            String newName = xmlNode.getUserObject().toString();
            
            xmlNode.setName(newName);
            final DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            final Document doc = builder.newDocument();
                    
            if (xmlNode.isLeaf()){
                element = doc.createElement("book:page");
                element.setAttribute("pid", newName);
                element.setAttribute("href", xmlNode.getHref());
                element.setAttribute("firstpage", "false");
                element.setAttribute("abspagenum", "-1");
                element.setAttribute("pagenum", "-1");
            }
            else{
                element = doc.createElement("book:structure");
                element.setAttribute("name", newName);
                element.setAttribute("seq", "1");
                element.setAttribute("id", xmlNode.getId());
            }
            
            xmlNode.setUserObject(element);
            
            XMLTree.getXmlTreeModel().reload((DefaultMutableTreeNode) XMLTree.getXmlTreeModel().getRoot());
            XMLTree.expandAll(BookImporter.xmlTree);
        } catch (NullPointerException ex) {
            logger.error(ex.getMessage());
        } catch (ParserConfigurationException ex) {
            logger.error(ex.getMessage());
        }
    }

    public void treeNodesInserted(TreeModelEvent e) {
    	//logger.debug("treeNodesInserted");
    }

    public void treeNodesRemoved(TreeModelEvent e) {
    	//logger.debug("treeNodesRemoved");
    }

    public void treeStructureChanged(TreeModelEvent e) {
    	//logger.debug("treeStructureChanged");
    }
}
