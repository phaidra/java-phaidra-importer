package it.imtech.xmltree;

import java.util.Vector;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import org.w3c.dom.*;

/**
 * 
 * @author luigi
 */
public class XMLTreeModel extends DefaultTreeModel implements
        TreeWillExpandListener {

    /**
     *
     * @param parentNode
     * @return
     * @see http://www.developer.com/xml/article.php/
     * 10929_3731356_2/Displaying-XML-in-a-Swing-JTree.htm
     */
    public static Vector<Element> getChildElements(Node parentNode) {
        NodeList list = parentNode.getChildNodes();
        Vector<Element> childNodes = new Vector<Element>();
        for (int i = 0; i < list.getLength(); i++) {
            if (list.item(i).getNodeType() == Node.ELEMENT_NODE) {
                childNodes.add((Element) list.item(i));// = new XMLNode((Element) list.item(i));
            }
        }
        return childNodes;
    }

    /**
     *
     * @param root
     */
    public XMLTreeModel(XMLNode root) {
        super(root);
        setChildren(root, XMLTreeModel.getChildElements((Node) root.getUserObject()));
    }

    /**
     *
     * @param parentNode
     * @param childElements
     */
    public void setChildren(XMLNode parentNode, Vector<Element> childElements) {
        if (childElements == null) {
            return;
        }
        // get the chld count
        int childCount = parentNode.getChildCount();
        // set the node as loaded
        parentNode.setLoaded(true);
        // remove all old nodes from the parent
        if (childCount > 0) {
            for (int i = 0; i < childCount; i++) {
                removeNodeFromParent((DefaultMutableTreeNode) parentNode.getChildAt(0));
            }
        }
        XMLNode node;
        // insert the nodes in the parent node
        for (int i = 0; i < childElements.size(); i++) {
            node = new XMLNode(childElements.get(i));
            insertNodeInto(node, parentNode, i);
        }
    }

    /**
     *
     * @param event
     * @throws ExpandVetoException
     */
    public void treeWillExpand(TreeExpansionEvent event)
            throws ExpandVetoException {
        // get the lazy node
        XMLNode lazyNode = (XMLNode) event.getPath().getLastPathComponent();
        // node is already loaded, does'nt have to do it again. return.
        if (lazyNode.isLoaded()) {
            return;
        }
        // add the child nodes to the parent
        setChildren(lazyNode,
                XMLTreeModel.getChildElements((Node) lazyNode.getUserObject()));

    }

    public void treeWillCollapse(TreeExpansionEvent event)
            throws ExpandVetoException {
    }
}
