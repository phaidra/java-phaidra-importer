/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package it.imtech.metadata;
import java.util.TreeMap;

/**
 *
 * @author Medelin Mauro
 */
public class Taxon {
    public TreeMap<Object, Taxon> subtaxons = new TreeMap<Object, Taxon>();
    
    public int TID;
    public String upstream_identifier;
    public String description;
    
    public Taxon(int TID,String upstream_identifier,String description){
        this.TID = TID;
        this.description = description;
        this.upstream_identifier = upstream_identifier;

    }
}
