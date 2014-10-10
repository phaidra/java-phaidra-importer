/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package it.imtech.bookimporter;

//import it.imtech.globals.CustomClassLoader;
import it.imtech.globals.CustomClassLoader;
import java.io.File;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.TreeMap;
import junit.framework.TestCase;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;

/**
 *
 * @author mede318
 */
public class BookUtilityTest extends TestCase {
    public final static String DEBUG_XML = "xml"+File.separator+"config.xml";
    public static String RESOURCES = "resources"+File.separator+"messages";
    
    public BookUtilityTest(String testName) {
        super(testName);
    }
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }
    
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test of getOrderedLanguages method, of class BookUtility.
     */
    public void testGetOrderedLanguages() {
        System.out.println("Test Ordering configuration languages: method -> getOrderedLanguages");
        TreeMap<String, String> en = new TreeMap<String, String>();
        en.put("English", "en");
        en.put("German", "de");
        en.put("Italian", "it");
        
        TreeMap<String, String> it = new TreeMap<String, String>();
        it.put("Inglese", "en");
        it.put("Italiano", "it");
        it.put("Tedesco", "de");
 
        TreeMap<String, String> de = new TreeMap<String, String>();
        de.put("Deutsch", "de");
        de.put("English", "en");
        de.put("Italienisch", "it");
 
        try{
            CustomClassLoader loader = new CustomClassLoader();
         
            Locale locale = new Locale("en");
            ResourceBundle bundle = ResourceBundle.getBundle(RESOURCES, locale, loader);
            XMLConfiguration config = new XMLConfiguration(new File(DEBUG_XML));
            TreeMap<String, String> result = BookUtility.getOrderedLanguages(config, bundle);
            assertEquals(en, result);
            
            locale = new Locale("it");
            bundle = ResourceBundle.getBundle(RESOURCES, locale, loader);
            config = new XMLConfiguration(new File(DEBUG_XML));
            result = BookUtility.getOrderedLanguages(config, bundle);
            assertEquals(it, result);
            
            locale = new Locale("de");
            bundle = ResourceBundle.getBundle(RESOURCES, locale, loader);
            config = new XMLConfiguration(new File(DEBUG_XML));
            result = BookUtility.getOrderedLanguages(config, bundle);
            assertEquals(de, result);
        }
        catch(ConfigurationException e){
            fail("Ordering Language Test failed: ConfigurationException " + e.getMessage());
        }
    }
}
