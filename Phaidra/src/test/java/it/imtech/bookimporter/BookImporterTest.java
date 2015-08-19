/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package it.imtech.bookimporter;

import it.imtech.globals.CustomClassLoader;
import it.imtech.utility.Utility;
import java.io.File;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.TreeMap;
import junit.framework.TestCase;
import org.apache.commons.configuration.XMLConfiguration;


/**
 *
 * @author mede318
 */
public class BookImporterTest extends TestCase {
   
    public BookImporterTest(String testName) {
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

     public void testCreateLanguageMenu() {
        System.out.println("Test Creating language menu: method -> CreateLanguageMenu");
       /*
        try{
            BookImporter test = new BookImporter();
            
            CustomClassLoader loader = new CustomClassLoader();
         
            Locale locale = new Locale("en");
            ResourceBundle bundle = ResourceBundle.getBundle(UtilityTest.RESOURCES, locale, loader);
            XMLConfiguration config = new XMLConfiguration(new File(UtilityTest.DEBUG_XML));
            
            test.createLanguageMenu(bundle, config);
            TreeMap<String, String> result = Utility.getOrderedLanguages(config, bundle);
            
        }
        catch(Exception e){
            fail("Creating Language Menu Test failed: General Exception: " + e.getMessage());
        }
        
        //result = Utility.getOrderedLanguages(config, bundle);
        assertEquals("1", "1");*/
     }
}
