/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.imtech.develop;

import it.imtech.bookimporter.BookImporter;
import it.imtech.globals.Globals;
import it.imtech.upload.SelectedServer;
import it.imtech.upload.UploadSettings;
import it.imtech.utility.Language;
import it.imtech.utility.Server;
import java.util.ArrayList;
import java.util.Locale;
import javax.swing.SwingUtilities;
import org.apache.log4j.Logger;

/**
 *
 * @author mede318
 */
public class BookImporterDevelop {
    
    private final static Logger logger = Logger.getLogger(BookImporterDevelop.class);
    
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
    
        Globals.SELECTED_FOLDER = "C:\\Users\\mauro\\Documents\\testing";
        Globals.SELECTED_FOLDER_SEP = "C:\\Users\\mauro\\Documents\\testing\\";
        
        //Globals.SELECTED_FOLDER = "/Users/mede318/testphaidra";
        //Globals.SELECTED_FOLDER_SEP = "/Users/mede318/testphaidra/";
        
        Globals.CURRENT_LOCALE = new Locale("it");
        
        ArrayList<Language> langs = new ArrayList<Language>();
        Language lang = new Language("en", "binglese", "Inglese");
        langs.add(lang);
        lang = new Language("it", "bitaliano", "Italiano");
        langs.add(lang);
        lang = new Language("de", "btedesco", "Tedesco");
        langs.add(lang);

        Globals.LANGUAGES = langs.toArray(new Language[langs.size()]);
        
            
        Globals.setGlobalVariables();
        Globals.TYPE_BOOK = Globals.COLLECTION;
        BookImporter x = BookImporter.getInstance();
    }
    
    public static void main(String[] args){
        SwingUtilities.invokeLater(new Runnable(){
            @Override
            public void run(){
                try {
                    for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                        if ("Nimbus".equals(info.getName())) {
                            javax.swing.UIManager.setLookAndFeel(info.getClassName());
                            break;
                        }
                    }
                } catch (ClassNotFoundException ex) {
                    logger.error(ex.getMessage());
                } catch (InstantiationException ex) {
                    logger.error(ex.getMessage());        
                } catch (IllegalAccessException ex) {
                    logger.error(ex.getMessage());
                } catch (javax.swing.UnsupportedLookAndFeelException ex) {
                    logger.error(ex.getMessage());
                }
                
                new BookImporterDevelop();
            }
        });
    };
}
