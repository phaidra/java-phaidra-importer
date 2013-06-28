package it.imtech.xmltree;

import it.imtech.bookimporter.BookImporter;
import it.imtech.globals.Globals;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.tree.TreePath;
import org.w3c.dom.Node;

/**
 *
 * @author luigi
 */
class TreeTransferHandler extends TransferHandler {

    private XMLNode[] nodesToRemove;

    public TreeTransferHandler() {
    }

    @Override
    public boolean canImport(TransferHandler.TransferSupport support) {

        if (!support.isDrop()) {
            return false;
        }

        support.setShowDropLocation(true);

        if (!support.isDataFlavorSupported(NodesTransferable.INFO_FLAVOR)) {
            return false;
        }

        // Do not allow a drop on the drag source selections.
        JTree.DropLocation dl = (JTree.DropLocation) support.getDropLocation();
        TreePath dest = dl.getPath();
        XMLNode target = (XMLNode) dest.getLastPathComponent();
        
        if (!target.isLeaf()) {
            Enumeration<XMLNode> children = (Enumeration<XMLNode>) target.children();
            
            switch (BookImporter.TYPEBOOK) {
                case Globals.BOOK:
                    if (children.hasMoreElements()
                            && !children.nextElement().isLeaf()) {
                        return false;
                    }
                    break;
                case Globals.COLLECTION:
                    if ("root".equals(target.getName())) {
                        return false;
                    }
            }

        }


        JTree tree = (JTree) support.getComponent();
        int dropRow = tree.getRowForPath(dl.getPath());
        int[] selRows = tree.getSelectionRows();
        for (int i = 0; i < selRows.length; i++) {
            if (selRows[i] == dropRow) {
                return false;
            }
        }

        if (target.isLeaf()) {
            return false;
        }
        // Do not allow MOVE-action drops if a non-leaf node is
        // selected unless all of its children are also selected.
        int action = support.getDropAction();
        if (action == MOVE) {
            return haveCompleteNode(tree);
        }
        // Do not allow a non-leaf node to be copied to a level
        // which is less than its source level.

        TreePath path = tree.getPathForRow(selRows[0]);
        XMLNode firstNode = (XMLNode) path.getLastPathComponent();
        if (firstNode.getChildCount() > 0
                && target.getLevel() < firstNode.getLevel()) {

            return false;
        }
        return true;
    }

    private boolean haveCompleteNode(JTree tree) {
        int[] selRows = tree.getSelectionRows();
        TreePath path = tree.getPathForRow(selRows[0]);
        XMLNode first = (XMLNode) path.getLastPathComponent();
        int childCount = first.getChildCount();
        // first has children and no children are selected.
        if (childCount > 0 && selRows.length == 1) {
            return false;
        }
        // first may have children.
        for (int i = 1; i < selRows.length; i++) {
            path = tree.getPathForRow(selRows[i]);
            XMLNode next = (XMLNode) path.getLastPathComponent();
            if (first.isNodeChild(next)) {
                // Found a child of first.
                if (childCount > selRows.length - 1) {
                    // Not all children of first are selected.
                    return false;
                }

            }
        }
        return true;
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        JTree tree = (JTree) c;
        TreePath[] paths = tree.getSelectionPaths();
        if (paths != null) {

            //save tree for undo
            XMLTree.exportBookstructureXUndo();

            // Make up a node array of copies for transfer and
            // another for/of the nodes that will be removed in
            // exportDone after a successful drop.
            List<XMLNode> copies = new ArrayList<XMLNode>();
            List<XMLNode> toRemove = new ArrayList<XMLNode>();
            XMLNode node = (XMLNode) paths[0].getLastPathComponent();
            if (!node.isLoaded()) {
                XMLTreeModel model = (XMLTreeModel) tree.getModel();
                model.setChildren(node, XMLTreeModel.getChildElements((Node) node.getUserObject()));
            }

            // XMLNode will loose its loaded property after
            // making a copy of it.
            // so reading the property of the node before making
            // a copy and setting the property back after copying.

            boolean loaded = node.isLoaded();
            XMLNode copy = copy(node);
            copy.setLoaded(loaded);
            copies.add(copy);
            toRemove.add(node);
            for (int i = 1; i < paths.length; i++) {
                XMLNode next = (XMLNode) paths[i].getLastPathComponent();
                // Do not allow higher level nodes to be added to list.
                if (next.getLevel() < node.getLevel()) {
                    break;
                } else if (next.getLevel() > node.getLevel()) { // child node
                    copy.add(copy(next));
                    // node already contains child
                } else { // sibling
                    copies.add(copy(next));
                    toRemove.add(next);
                }
            }
            XMLNode[] nodes = copies.toArray(new XMLNode[copies.size()]);
            nodesToRemove = toRemove.toArray(new XMLNode[toRemove.size()]);
            return new NodesTransferable(nodes);
        }
        return null;
    }

    /**
     * Defensive copy used in createTransferable.
     */
    private XMLNode copy(XMLNode node) {
        return new XMLNode(node.getUserObject());
    }

    protected void exportDone(JComponent source, Transferable data, int action) {
        if ((action & MOVE) == MOVE) {

            JTree tree = (JTree) source;
            XMLTreeModel model = (XMLTreeModel) tree.getModel();
            // Remove nodes saved in nodesToRemove in createTransferable.
            for (int i = 0; i < nodesToRemove.length; i++) {
                model.removeNodeFromParent(nodesToRemove[i]);
            }
        }
    }

    public int getSourceActions(JComponent c) {
        return COPY_OR_MOVE;
    }

    public boolean importData(TransferHandler.TransferSupport support) {
        if (!canImport(support)) {
            return false;
        }
        // Extract transfer data.
        XMLNode[] nodes = null;
        try {
            Transferable t = support.getTransferable();
            nodes = (XMLNode[]) t.getTransferData(NodesTransferable.INFO_FLAVOR);
        } catch (UnsupportedFlavorException ufe) {
            System.out.println("UnsupportedFlavor: " + ufe.getMessage());
        } catch (java.io.IOException ioe) {
            System.out.println("I/O error: " + ioe.getMessage());
        }
        // Get drop location info.
        JTree.DropLocation dl = (JTree.DropLocation) support.getDropLocation();
        int childIndex = dl.getChildIndex();
        TreePath dest = dl.getPath();
        XMLNode parent = (XMLNode) dest.getLastPathComponent();
        JTree tree = (JTree) support.getComponent();
        XMLTreeModel model = (XMLTreeModel) tree.getModel();
        if (!parent.isLoaded()) {
            model.setChildren(parent, XMLTreeModel.getChildElements((Node) parent.getUserObject()));
        }
        // Configure for drop mode.
        int index = childIndex; // DropMode.INSERT
        if (childIndex == -1) { // DropMode.ON
            index = parent.getChildCount();
        }
        // Add data to model.
        for (int i = 0; i < nodes.length; i++) {
            model.insertNodeInto(nodes[i], parent, index++);
        }
        return true;
    }

    public String toString() {
        return getClass().getName();
    }
}
