package it.imtech.xmltree;

import javax.swing.tree.DefaultMutableTreeNode;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Element;

/**
 *
 * @author luigi
 */
public class XMLNode extends DefaultMutableTreeNode {

    private String name;
    private String iconName;
    private boolean isFile;
    private boolean loaded;
    //private boolean isFirstpage;

    XMLNode(Object nodeObject) {
        super(nodeObject);

        String tagName = ((Element) userObject).getTagName();
        if ("book:book".equals(tagName)) {
            name = "root";
            iconName = "/";
            isFile = false;
        } else if ("coll:pages".equals(tagName)) {
            name = "Collection";
            iconName = "collection";
            isFile = false;
        } else if ("book:pages".equals(tagName)) {
            name = "Book";
            iconName = "book";
            isFile = false;
        } else if ("book:structure".equals(tagName)) {
            name = ((Element) userObject).getAttribute("name");
            iconName = "page";
            isFile = false;
        } else if ("book:page".equals(tagName)) {
            name = ((Element) userObject).getAttribute("pid");
            iconName = StringUtils.substringAfter(name, ".").toLowerCase();
            isFile = true;
            if(((Element) userObject).getAttribute("firstpage").equals("true"))
                iconName="firstpage";

        }
    }

    public boolean isLoaded() {
        return loaded;
    }

    public void setLoaded(boolean loaded) {
        this.loaded = loaded;
    }

    public String getIconName() {
        return iconName;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean isLeaf() {
        return isFile;
    }

    public void setName(String name) {
        this.name = name;

    }

    public String getName() {
        return this.name;

    }
}
