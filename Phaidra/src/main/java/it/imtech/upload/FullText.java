/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package it.imtech.upload;

/**
 *
 * @author mauro
 */
public class FullText {
    private String fulltext = "";

    public FullText(){}

    public void addText(String text){
        if (this.fulltext.length() < 1)
            this.fulltext += text;
        else
            this.fulltext += " " + text;
    }

    public String getFulltext(){
        return this.fulltext;
    }
}
