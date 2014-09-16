/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.imtech.develop;

import it.imtech.bookimporter.BookImporter;
import it.imtech.globals.Globals;
import it.imtech.upload.SelectedServer;
import it.imtech.utility.Server;
import java.util.Locale;
import javax.swing.SwingUtilities;

/**
 *
 * @author mede318
 */
public class BookImporterDevelop {
    
    public BookImporterDevelop(){
        testBookLayout();
    }
    
    private Server getDevelopmentServer(){
        String servername = "TEST MAURO";
        String fedoraurl = "fedoradev.cab.unipd.it";
        String phaidraurl = "phaidradev.cab.unipd.it";
        String staticurl = "phaidrastaticdev.cab.unipd.it";
        String stylesheeturl = "http://phaidrastaticdev.cab.unipd.it/stylesheets/externalview.xsl";
        String oaiIdentifier = "cab.unipd.it";
            
        return new Server(servername, fedoraurl, phaidraurl, staticurl, stylesheeturl, oaiIdentifier);

    }
    
    public final void testBookLayout(){
        Server selected = getDevelopmentServer();
        SelectedServer.getInstance(selected);
    
        Globals.SELECTED_FOLDER = "/Users/mede318/testphaidra";
        Globals.SELECTED_FOLDER_SEP = "/Users/mede318/testphaidra/";
        Globals.CURRENT_LOCALE = new Locale("it");
        
        Globals.setGlobalVariables();
        Globals.TYPE_BOOK = Globals.BOOK;
        BookImporter x = BookImporter.getInstance();
    }
    
    public static void main(String[] args){
        SwingUtilities.invokeLater(new Runnable(){
            @Override
            public void run(){
                   new BookImporterDevelop();
            }
        });
    };
}
