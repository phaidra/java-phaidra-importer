/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package it.imtech.vocabularies;

/**
 *
 * @author mauro
 */
public class VocEntry{
    public String ID;
    public String description;
    
    public VocEntry(String ID, String description){
        this.ID = ID;
        this.description = description;
    }

    public int compare(VocEntry x, VocEntry y){
        return x.description.toString().compareTo(y.description.toString());
    }
}
