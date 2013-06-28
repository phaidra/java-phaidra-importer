/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package it.imtech.metadata;

import java.util.Collections;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

/**
 * Classe adibita all'inplementazione di un JTree ordinato alfabeticamente
 * @author mauro
 */
class ClassMutableNode extends DefaultMutableTreeNode implements Comparable {
    public ClassMutableNode(ClassNode name) {
        super(name);
    }
    
    @Override
    public void add(final MutableTreeNode newChild) {
        super.add(newChild);
    }
    
    @Override
    public void insert(final MutableTreeNode newChild, final int childIndex) {
        super.insert(newChild, childIndex);
        Collections.sort(this.children);
    }
    
    public int compareTo(final Object o) {
        return this.toString().compareToIgnoreCase(o.toString());
    }
}