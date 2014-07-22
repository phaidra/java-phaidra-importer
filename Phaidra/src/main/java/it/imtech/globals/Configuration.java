package it.imtech.globals;

import it.imtech.upload.SelectedServer;
import java.util.ResourceBundle;
import java.util.TreeMap;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;

/**
 *
 * @author mauro
 */
public final class Configuration {
    //Gestore dei log
    public final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(Configuration.class);
            
    public SelectedServer servers;
    
    public String updater;
    
    public TreeMap<String, String> languages;
    
    public Configuration(XMLConfiguration config){
        ResourceBundle bundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);
       
        getUpdaterString(config);
    }
    
    /**
     * Aggiorna il link per il file di configurazione dell'aggiornamento
     * dell'applicazione.
     * @param config 
     */
    public void getUpdaterString(XMLConfiguration config){
         try {
            XMLConfiguration internalConf = new XMLConfiguration(Globals.INTERNAL_CONFIG);
            if(!internalConf.getString("urlupdater.descrurl").equals(config.getString("urlupdater.descrurl"))){
                internalConf.setAutoSave(true);
                internalConf.setProperty("urlupdater.descrurl", config.getString("urlupdater.descrurl"));
            }
        }
        catch (ConfigurationException ex) {
            logger.error(ex.getMessage());
        }
    }
}
