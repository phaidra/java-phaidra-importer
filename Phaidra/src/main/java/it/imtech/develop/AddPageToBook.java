package it.imtech.develop;

import at.ac.univie.phaidra.api.Phaidra;
import at.ac.univie.phaidra.api.objekt.Book;
import at.ac.univie.phaidra.api.objekt.Page;
import it.imtech.upload.SelectedServer;
import it.imtech.utility.Server;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author mauro
 */
public class AddPageToBook {
    String username = "testphaidra";
    String password = "H83HC5Zq";
    String baseurl = "fedoradev.cab.unipd.it";
    String phaidraURL = "phaidradev.cab.unipd.it";
    String staticBaseURL = "phaidrastaticdev.cab.unipd.it";
    String stylesheetURL = "http://phaidrastaticdev.cab.unipd.it/stylesheets/externalview.xsl";
    String oaiIdentifier = "cab.unipd.it";
    HashSet<String> x = null;
    Book book = null;
    Page page = null;
    
    public AddPageToBook(){
        try {
            Phaidra phaidra = new Phaidra(baseurl, staticBaseURL, stylesheetURL, oaiIdentifier, username, password);
            
            book = phaidra.loadBook("o:8754");
            book.addPage(null, page);
            
            //x = book.members;
        } catch (Exception ex) {
            Logger.getLogger(AddPageToBook.class.getName()).log(Level.SEVERE, null, ex);
        }
        String y = "y";
    }
    
    public static void main(String[] args){
        SwingUtilities.invokeLater(new Runnable(){
            @Override
            public void run(){
                   new AddPageToBook();
            }
        });
    };
}
