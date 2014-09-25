/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.imtech.helper;

import java.net.URL;
import javax.help.HelpBroker;
import javax.help.HelpSet;
import javax.xml.ws.Action;

/**
 *
 * @author mede318
 */
public class Helper {
    private HelpSet hs;
    private HelpBroker hb;
    private URL hsURL;
    
    @Action
    public void openHelp() {
        // Identify the location of the .hs file 
        String pathToHS = "javahelp/appwithhelp/docs/hierarchy_helpset.hs";
        //Create a URL for the location of the help set
        try {
            URL hsURL = getClass().getResource(pathToHS);
            hs = new HelpSet(null, hsURL);
        } catch (Exception ee) {
            // Print info to the console if there is an exception
            System.out.println( "HelpSet " + ee.getMessage());
            System.out.println("Help Set "+ pathToHS +" not found");
            return;
        }

        // Create a HelpBroker object for manipulating the help set
        hb = hs.createHelpBroker();
        //Display help set
        hb.setDisplayed(true);
    }
}
