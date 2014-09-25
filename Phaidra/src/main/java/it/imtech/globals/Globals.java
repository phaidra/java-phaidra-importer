/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package it.imtech.globals;

import it.imtech.utility.Language;
import it.imtech.utility.Utility;
import java.io.File;
import java.net.URL;
import java.util.Locale;

/**
 *
 * @author Mauro
 */
public class Globals {
    public final static boolean DEBUG = false;
    public final static String DEBUG_FOLDER = "xml";
    public final static String DEBUG_XML = Utility.getSep() + "xml" + Utility.getSep() + "config.xml";
    
    //Local files import/export metadata & bookstructure
    public static String IMP_EXP_METADATA = "uwmetadata.xml";
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

    public static boolean ONLINE = false;
    
    //Book Collection definition
    public final static char BOOK = 'B';
    public final static char COLLECTION = 'C';
    public final static char NOT_EXISTS = 'X';
    
    public final static String HIDDEN_FOLDER = ".imphaidraimportertwo";
    public static String USER_DIR  = System.getProperty("user.home") + Utility.getSep() + HIDDEN_FOLDER + Utility.getSep();
    
    public static String UNDO_DIR  = USER_DIR + "undo" +Utility.getSep();
    public static String LOG4J     = USER_DIR + "config" + Utility.getSep() + "log4j.xml";
    public static String BLANKPAGE = USER_DIR + "config" + Utility.getSep() + "blankpage.jpg";
    public static String INTERNAL_CONFIG = USER_DIR +"config" + Utility.getSep() + "config.xml";
     
       
    //BACKUP_METADATA
    public static String DUPLICATION_FOLDER_SEP = USER_DIR + Utility.getSep() + "duplication" + Utility.getSep();
    public static String SESSION_METADATA = DUPLICATION_FOLDER_SEP + "sessionuwmetadata.xml";
    public static String BACKUP_METADATA  = "";
    public static String EXPORT_METADATA  = DUPLICATION_FOLDER_SEP + "exportuwmetadata.xml";
    public static String BACKUP_INIT = "backupuwmetadata.xml";
    
    //GESTIONE TEMPLATES
    public static String TEMPLATES_FOLDER_SEP = USER_DIR + Utility.getSep() + "templates" + Utility.getSep();
    public static String TEMPLATES_XML = TEMPLATES_FOLDER_SEP + "templates_list.xml";
    
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
        
        File backup = new File(Globals.DUPLICATION_FOLDER_SEP);
        
        if (backup.isDirectory()){
            for(File file: backup.listFiles()) {
                if (file.getName().startsWith("sessionuwmetadata")){
                    file.delete();
                }
                if (file.getName().startsWith("exportuwmetadata")){
                    file.delete();
                }
            }
        }
        
        if(DEBUG){
            USER_DIR = "";
            CURRENT_VERSION = "2.0";
            UNDO_DIR = Globals.JRPATH + "appdata"+ Utility.getSep() + "undo"+Utility.getSep();
            LOG4J = Globals.JRPATH + "appdata"+ Utility.getSep() + "config" + Utility.getSep() + "log4j.xml";
            BLANKPAGE = Globals.JRPATH + "appdata"+ Utility.getSep() + "config" + Utility.getSep() + "blankpage.jpg";
            INTERNAL_CONFIG = Globals.JRPATH + "appdata"+ Utility.getSep() + "config" + Utility.getSep() + "config.xml";
            
            //BACKUP_METADATA = Globals.JRPATH + "appdata"+ Utility.getSep() + "uploads" + Utility.getSep() + "backupuwmetadata.xml";
            //EXPORT_METADATA = Globals.JRPATH + "appdata"+ Utility.getSep() + "uploads" + Utility.getSep() + "exportuwmetadata.xml";
            //SESSION_METADATA = Globals.JRPATH + "appdata"+ Utility.getSep() + "uploads" + Utility.getSep() + "sessionuwmetadata.xml";
        }    
    }
}
