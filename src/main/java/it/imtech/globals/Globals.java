/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package it.imtech.globals;

import it.imtech.bookimporter.BookImporter;
import it.imtech.utility.Utility;
import org.apache.log4j.Logger;

/**
 *
 * @author mads
 */
public class Globals {
    public final static boolean DEBUG = false;
    public final static String DEBUG_FOLDER = "remote";
    public final static String DEBUG_XML = DEBUG_FOLDER+Utility.getSep()+"xml"+Utility.getSep()+"config.xml";
    
    public final static int UNDO_MAX_FILE = 10;
    public final static String IMP_EXP_METADATA = "uwmetadata.xml";
    public final static String IMP_EXP_BOOK = "phaidraimporterstructure.xml";
    public final static String FOLD_XML = "/xml/";
    public final static String URL_CLASSIFICATION = FOLD_XML + "classifications_1.xml";
    public final static String URL_METADATA_COLL = FOLD_XML + "colluwmetadata.xml";
    public final static String URL_VOCABULARY = FOLD_XML + "vocabulary.xml";
    public final static String URL_CLASS_LIST = FOLD_XML + "classification_list.xml";
    public final static String classifConfig = "classification.xml";
    
    public final static char BOOK = 'B';
    public final static char COLLECTION = 'C';
    public final static char NOT_EXISTS = 'X';
    
    public static String USER_DIR = System.getProperty("user.home")+Utility.getSep()+".phaidraimporter"+Utility.getSep();
    public static String UNDO_DIR = System.getProperty("user.home")+Utility.getSep()+".phaidraimporter"+Utility.getSep()+"undo"+Utility.getSep();
    public static String LOG4J = System.getProperty("user.home")+Utility.getSep()+".phaidraimporter"+Utility.getSep()+"config" + Utility.getSep() + "log4j.xml";
    public static String BLANKPAGE = System.getProperty("user.home")+Utility.getSep()+".phaidraimporter"+Utility.getSep()+"config" + Utility.getSep() + "blankpage.jpg";
    public static String internalConfig = System.getProperty("user.home")+Utility.getSep()+".phaidraimporter"+Utility.getSep()+"config" + Utility.getSep() + "config.xml";
    
    //Gestore dei log
    public final static Logger logger = Logger.getLogger(BookImporter.class);
    
    //Loader delle resources
    public final static CustomClassLoader loader = new CustomClassLoader();
    
    public static void setDebug(){
        if(DEBUG){
            USER_DIR = "";
            UNDO_DIR = "undo"+Utility.getSep();
            LOG4J = "config" + Utility.getSep() + "log4j.xml";
            BLANKPAGE = "config" + Utility.getSep() + "blankpage.jpg";
            internalConfig = "config" + Utility.getSep() + "config.xml";
        }   
    }
}
