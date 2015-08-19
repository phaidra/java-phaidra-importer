/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package it.imtech.upload;

import it.imtech.utility.Server;

/**
 *
 * @author mauro
 */
public class SelectedServer {

    String username         = null;
    String password         = null;
    String baseurl          = null;
    String staticBaseURL    = null;
    String stylesheetURL    = null;
    String oaiIdentifier    = null;
    String phaidraUrl       = null;
    String serverName       = null;
    
    private static SelectedServer istance;
    
    public static SelectedServer getInstance(Server s){
        if (istance == null){
            if(s!=null)
                istance = new SelectedServer(s);
            else
                istance = null;
        }
        
        return istance;
    }
    
    public void makeEmpty(){
        this.istance = null;
    }
    
    public SelectedServer() {}

    public SelectedServer(Server s){
        this.baseurl         = s.getFedoraUrl();
        this.staticBaseURL   = s.getStaticurl();
        this.stylesheetURL   = s.getStylesheetURLl();
        this.oaiIdentifier   = s.getOaiIdentifier();
        this.phaidraUrl      = s.getPhaidraurl();
        this.serverName      = s.getServername();
    }
    
    public String getUsername() {
        return this.username;
    }

    public String setUsername() {
        return this.username;
    }
    
    public void setPassword(String value) {
        this.password = value;
    }

    public String getPassword() {
        return this.password;
    }

    public void setBaseUrl(String value) {
        this.baseurl = value;
    }

    public String getBaseUrl() {
        return this.baseurl;
    }
    
    public void setStaticBaseURL(String value) {
        this.staticBaseURL = value;
    }

    public String getStaticBaseURL() {
        return this.staticBaseURL;
    }
    
    public String getHTTPStaticBaseURL() {
       if(this.staticBaseURL.startsWith("http://"))
           return this.staticBaseURL;
       else
           return "http://"+this.staticBaseURL;
    }

    public void setStyleSheetURL(String value) {
        this.stylesheetURL = value;
    }

    public String getStyleSheetURL() {
        return this.stylesheetURL;
    }
    
    public void setOaiIdentifier(String value) {
        this.oaiIdentifier = value;
    }

    public String getOaiIdentifier() {
        return this.oaiIdentifier;
    }

    public void setPhaidraURL(String value) {
        this.phaidraUrl = value;
    }

    public String getPhaidraURL() {
        return this.phaidraUrl;
    }
    public void setPserverName(String value) {
        this.serverName = value;
    }

    public String getserverName() {
        return this.serverName;
    }
}
