package it.imtech.xmltree;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;


/**
 * 
 * @author luigi
 */
public class NodesTransferable implements Transferable {

    //specify the data flavor
    final public static DataFlavor INFO_FLAVOR =
            new DataFlavor(XMLNode[].class, "application/x-java-serialized-object");
    static DataFlavor flavors[] = {INFO_FLAVOR};
    XMLNode[] nodes;

    //an array of XMLNodes to support DnD for more than one node
    public NodesTransferable(XMLNode[] nodes) {
        this.nodes = nodes;
    }

    /**
     *
     * @param flavor
     * @return NodesTransferable object
     * @throws UnsupportedFlavorException
     */
    public Object getTransferData(DataFlavor flavor)
            throws UnsupportedFlavorException {
        if (!isDataFlavorSupported(flavor)) {
            throw new UnsupportedFlavorException(flavor);
        }
        return nodes;
    }

    //return the data flavors
    public DataFlavor[] getTransferDataFlavors() {
        return flavors;
    }

    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return flavor.equals(INFO_FLAVOR);
    }
}
