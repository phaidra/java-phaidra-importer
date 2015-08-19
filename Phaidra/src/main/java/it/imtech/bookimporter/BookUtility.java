/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package it.imtech.bookimporter;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import java.util.ResourceBundle;
import java.util.TreeMap;


/**
 *
 * @author IM-Technologies
 */
public class BookUtility {
    
    /**
     * Restituisce le lingue disponibili in ordine alfabetico
     * @param config
     * @param bundle
     * @return TreeMap<String, String>
     */
    protected static TreeMap<String, String> getOrderedLanguages(XMLConfiguration config, ResourceBundle bundle){
        TreeMap<String, String> ordered_res = new TreeMap<String, String>();
        
        String lang_name;
        String key;
        
        java.util.List<HierarchicalConfiguration> resources = config.configurationsAt("resources.resource");
        for(HierarchicalConfiguration resource : resources) {
            lang_name = resource.getString("[@descr]");
            key = it.imtech.utility.Utility.getBundleString(lang_name, bundle);
  
            ordered_res.put(key,resource.getString(""));  
        }
        
        return ordered_res;
    }
    
    
    
}
