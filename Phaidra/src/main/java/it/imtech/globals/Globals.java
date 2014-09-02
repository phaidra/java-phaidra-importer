/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package it.imtech.globals;

import it.imtech.utility.Language;
import it.imtech.utility.Utility;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;

/**
 *
 * @author Mauro
 */
public class Globals {
    public final static boolean DEBUG = true;
    public final static String DEBUG_FOLDER = "remote";
    public final static String DEBUG_XML = Globals.DEBUG_FOLDER + Utility.getSep() + "xml" + Utility.getSep() + "config.xml";
    
    //Local files import/export metadata & bookstructure
    public final static String IMP_EXP_METADATA = "uwmetadata.xml";
    public final static String IMP_EXP_BOOK = "phaidraimporterstructure.xml";
    
    public final static int UNDO_MAX_FILE = 10;
    
    //XML Files (remote or local)
    public final static String FOLD_XML = "/xml/";
    public final static String URL_CLASSIFICATION = FOLD_XML + "classifications_1.xml";
    public final static String URL_METADATA_COLL = FOLD_XML + "colluwmetadata.xml";
    public static String URL_METADATA = FOLD_XML + "uwmetadata.xml";
    public final static String URL_VOCABULARY = FOLD_XML + "vocabulary.xml";
    public final static String URL_CLASS_LIST = FOLD_XML + "classification_list.xml";
    public final static String CLASSIF_CONFIG = "classification.xml";
   
    //Book Collection definition
    public final static char BOOK = 'B';
    public final static char COLLECTION = 'C';
    public final static char NOT_EXISTS = 'X';
    
    public static String USER_DIR = System.getProperty("user.home")+Utility.getSep()+".phaidraimporter"+Utility.getSep();
    public static String UNDO_DIR = System.getProperty("user.home")+Utility.getSep()+".phaidraimporter"+Utility.getSep()+"undo"+Utility.getSep();
    public static String LOG4J = System.getProperty("user.home")+Utility.getSep()+".phaidraimporter"+Utility.getSep()+"config" + Utility.getSep() + "log4j.xml";
    public static String BLANKPAGE = System.getProperty("user.home")+Utility.getSep()+".phaidraimporter"+Utility.getSep()+"config" + Utility.getSep() + "blankpage.jpg";
    public static String INTERNAL_CONFIG = System.getProperty("user.home")+Utility.getSep()+".phaidraimporter"+Utility.getSep()+"config" + Utility.getSep() + "config.xml";
    
    //Path della cartella che contiene l'eseguibile dell'applicazione
    public static String JRPATH = null;
    public static String BASE_RESOURCES = "resources" +Utility.getSep()+ "messages";
    public static String RESOURCES;
    
    //Loader delle resources
    public final static CustomClassLoader loader = new CustomClassLoader();
    
    //Variabili globali definite a runtime
    
    //Lingua corrente
    public static Locale CURRENT_LOCALE;
    //Versione corrente
    public static String CURRENT_VERSION;
    //Path assoluto alla cartella di lavoro corrente
    public static String SELECTED_FOLDER = null;
    //Path assoluto alla cartella di lavoro corrente con separatore
    public static String SELECTED_FOLDER_SEP = null;
    //La cartella di output e scrivibile
    public static boolean FOLDER_WRITABLE = true;
    //E' stato scelto di creare un libro o una collezzione
    public static char TYPE_BOOK = 'X';
    //Lingue possibili
    public static Language[] LANGUAGES;
    
    public static URL URL_CONFIG = null;
    
    public static void setGlobalVariables(){
        Globals.JRPATH = Utility.getCurrentJarDirectory();
        Globals.RESOURCES = JRPATH + BASE_RESOURCES;
        Globals.CURRENT_LOCALE = new Locale("en");
        
        if(DEBUG){
            
            USER_DIR = "";
            UNDO_DIR = "appdata"+ Utility.getSep() + "undo"+Utility.getSep();
            LOG4J = "appdata"+ Utility.getSep() + "config" + Utility.getSep() + "log4j.xml";
            BLANKPAGE = "appdata"+ Utility.getSep() + "config" + Utility.getSep() + "blankpage.jpg";
            INTERNAL_CONFIG = "appdata"+ Utility.getSep() + "config" + Utility.getSep() + "config.xml";
            /*
            SELECTED_FOLDER = "working_dir";
            SELECTED_FOLDER_SEP = "working_dir"+Utility.getSep();
            */
            CURRENT_VERSION = "2.0";
            
          
            ArrayList<Language> langs = new ArrayList<Language>();
            Language lang = new Language("en", "binglese", "Inglese");
            langs.add(lang);
            lang = new Language("it", "bitaliano", "Italiano");
            langs.add(lang);
            lang = new Language("de", "btedesco", "Tedesco");
            langs.add(lang);
            
            LANGUAGES = langs.toArray(new Language[langs.size()]);
            
            //Server sv = new Server("test", "test", "test", "test", "test", "test");
            //SelectedServer.getInstance(sv);
        }    
    }
    
    
}
