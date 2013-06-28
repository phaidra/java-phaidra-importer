/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package it.imtech.metadata;

/**
 *
 * @author Administrator
 */
public class ClassNode {
    String key;
    String value;
    
    public ClassNode(String k, String v){
        this.key = k;
        this.value = v;
    }
    
    public String getKey(){
        return this.key;
    }
    
    public String getValue(){
        return this.value;
    }
    
    @Override
    public String toString() {  
        return this.value;
    }
}
