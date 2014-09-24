/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Template
 * and open the template in the editor.
 */

package it.imtech.metadata;

import it.imtech.globals.Globals;

/**
 *
 * @author mauro
 */
public class Template {
    String filetitle;
    String filename;
    String filetype;
    
    public Template(String title, String name){
        this.filename = name;
        this.filetitle = title;
        this.filetype = Character.toString(Globals.TYPE_BOOK);
    }
    
    @Override
    public String toString() {  
        return this.filetitle;
    }
    
    public String getFileTitle(){
        return this.filetitle;
    }
    
     public String getFileName(){
        return this.filename;
    }
     
    public String getFileType(){
        return this.filetype;
    }
}
