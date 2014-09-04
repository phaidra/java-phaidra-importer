package it.imtech.xmltree;

/*
 * La classe definisce un oggetto immagine, definisce id e path 
 * di uno degli oggetti della struttura del libro e/o collezzione
 */
public class XMLPage {
    String pid;
    String href;
    
    public XMLPage(String myPid, String myHref){
        this.pid = myPid;
        this.href = myHref;
    }
    
    public String getPid(){
        return this.pid;
    }
    
    public String getHref(){
        return this.href;
    }
    
    @Override
    public String toString(){
        return this.pid;
    }
}
