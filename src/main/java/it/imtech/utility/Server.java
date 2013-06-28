/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package it.imtech.utility;

import it.imtech.bookimporter.BookImporter;
import it.imtech.globals.Globals;
import java.util.ResourceBundle;


/**
 *
 * @author mauro
 */
public class Server {
   
    String servername     =   null;
    String fedoraurl      =   null;
    String phaidraurl     =   null;
    String staticurl      =   null;
    String oaiIdentifier  =   null;
    String stylesheetURLl =   null;
    
    public Server(String sn, String fu, String pu, String su,String sr,String oi){
        this.servername     = sn;
        this.fedoraurl      = fu;
        this.phaidraurl     = pu;
        this.staticurl      = su;
        this.stylesheetURLl = sr;
        this.oaiIdentifier  = oi;
    }
    
    public String getServername(){
        return this.servername;
    }
    
    public String getOaiIdentifier(){
        return this.oaiIdentifier;
    }
    
    public String getStylesheetURLl(){
        return this.stylesheetURLl;
    }
    
    public String getFedoraUrl(){
        return this.fedoraurl;
    }
    
    public String getPhaidraurl(){
        return this.phaidraurl;
    }
    
    public String getStaticurl(){
        return this.staticurl;
    }
    
    public boolean allParametersDefined(){
        if(this.servername==null || this.servername.equals(""))return false;
        if(this.fedoraurl==null || this.fedoraurl.equals(""))  return false;
        if(this.phaidraurl==null || this.phaidraurl.equals(""))return false;
        if(this.staticurl==null || this.staticurl.equals(""))  return false;
        if(this.oaiIdentifier==null || this.oaiIdentifier.equals(""))return false;
        if(this.stylesheetURLl==null || this.stylesheetURLl.equals(""))return false;
        return true;
    }
    
    @Override
    public String toString() {  
        ResourceBundle bundle = ResourceBundle.getBundle(BookImporter.RESOURCES, BookImporter.currentLocale, Globals.loader);
        return Utility.getBundleString(servername,bundle);
    }
}


