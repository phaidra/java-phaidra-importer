/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package it.imtech.globals;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

public class CustomClassLoader extends ClassLoader {

   
    @Override
    protected URL findResource(String name) {
        File f = new File(name);
        try {
            return f.toURI().toURL();
        } 
        catch (MalformedURLException meu) {
        }
        
        return super.findResource(name);
    }
}
