/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package it.imtech.utility;

import it.imtech.globals.Globals;
import java.util.ResourceBundle;

/**
 *
 * @author mauro
 */
public class Language {
    private final String code;
    private final String key;
    private final String descr;
    
    public Language(String cd, String ke, String de){
        code = cd;
        key = ke;
        descr = de;
    }
    
    public String getCode(){
        return code;
    }
    
    public String getKey(){
        return key;
    }
    
    public String getDescr(){
        return descr;
    }
    
    @Override
    public String toString() {  
        ResourceBundle bundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);
        return Utility.getBundleString(key,bundle);
    }
}
