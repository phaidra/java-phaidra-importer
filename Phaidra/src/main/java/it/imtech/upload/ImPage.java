/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package it.imtech.upload;

/**
 * 
 * @author Medelin Mauro
 */
public class ImPage{
    private String ocrXML = "";
    private String fulltext = "";

    public ImPage(){}

    public void setOcrXML(String xml){
        this.ocrXML = xml;
    }

    public void addToFulltext(String text){
        if (this.fulltext.length() < 1)
            this.fulltext += text;
        else
            this.fulltext += " " + text;
    }

    public String getOcrXML(){
        return this.ocrXML;
    }

    public String getFulltext(){
        return this.fulltext;
    }
}
