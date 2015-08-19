package it.imtech.xmltree;

import java.awt.Component;
import java.util.Hashtable;
import javax.swing.Icon;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

/**
 * 
 * @author luigi
 */
public class XMLTreeCellRenderer extends DefaultTreeCellRenderer{

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {

        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf,
                row, hasFocus);
        Icon icon = null;
        //Retrieve the 'icons' clientProperty
        Hashtable icons = (Hashtable) tree.getClientProperty("icons");
        //get the type of node
        String name = ((XMLNode) value).getIconName();

        if ((name != null)) {
            //get the icon for this type of node
            icon = (Icon) icons.get(name);
            if(icon == null)
                //if we could'nt find anything, get the 'unknown' icon
                icon = (Icon)icons.get("unknown");
            //set the icon
            setIcon(icon);
        }
        //set the tooltip
        setToolTipText(((XMLNode) value).toString());
        // return back this component
        return this;
    }


}
