package it.imtech.develop;

import it.imtech.globals.Globals;
import it.imtech.helper.Helper;
import java.awt.Dimension;
import java.net.URL;
import java.util.Locale;
import javax.help.HelpBroker;
import javax.help.HelpSet;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.xml.ws.Action;


public class JavaHelp extends JFrame{
    /***This method opens up the help viewer for specified help set 
     * and displays the home ID of that help set
     */  
    private HelpSet hs;
    private HelpBroker hb;
    private URL hsURL;
    
    public JavaHelp(){
        this.setSize(new Dimension(400,400));
        this.setVisible(true);
    }
    
    @Action
    public void openHelp() {
        // Identify the location of the .hs file 
        String pathToHS = "/appwithhelp/docs/appwithhelp-hs.xml";
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
    
    public static void main(String[] args){
        SwingUtilities.invokeLater(new Runnable(){
            
            public void run(){
                   //new JavaHelp();
                Globals.setGlobalVariables();
                Globals.CURRENT_LOCALE = new Locale("it");
                Helper helper = new Helper();
                
                helper.openHelp();
            }
        });
    };

}