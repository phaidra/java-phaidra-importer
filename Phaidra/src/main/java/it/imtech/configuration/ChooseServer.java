/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package it.imtech.configuration;

import it.imtech.certificate.AddToStoreKey;
import it.imtech.globals.Globals;
import it.imtech.upload.SelectedServer;
import it.imtech.utility.Language;
import it.imtech.utility.Server;
import it.imtech.utility.Utility;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Locale;
import java.util.ResourceBundle;
import javax.net.ssl.SSLHandshakeException;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;

/**
 *
 * @author mede318
 */
public class ChooseServer extends javax.swing.JPanel {
    
    public final Logger logger = Logger.getLogger(ChooseServer.class);
    
    private ResourceBundle bundle;
    
    private Language selected_lang;
    
    JLabel label_server_1 = new JLabel();
    JLabel label_server_2 = new JLabel();
    JLabel label_server_3 = new JLabel();
    JLabel label_server_4 = new JLabel();
    
    static JComboBox choose_server = new JComboBox();
    JComboBox choose_language = new JComboBox();
    
    JPanel main_panel;
    
    /**
     * Creates new form ChooseServer
     */
    public ChooseServer(XMLConfiguration config) {
        initComponents();
        //this.setBackground(Color.WHITE); 
        
        bundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);
        
        Server[] servers = getServersFromConfig(config, bundle);
        choose_server.setModel(new javax.swing.DefaultComboBoxModel(servers));
        choose_server.setSelectedItem(servers[0]);
        choose_server.setMinimumSize(new Dimension(400,20));
        
        Server s = (Server) choose_server.getSelectedItem();
        SelectedServer.getInstance(s);
                
        getLanguagesFromConfig(config, bundle);
        choose_language.setModel(new javax.swing.DefaultComboBoxModel(Globals.LANGUAGES));
        choose_language.setSelectedItem(this.selected_lang);
        choose_language.setMinimumSize(new Dimension(400,20));
        
        updateLanguage();
        MigLayout choose_layout = new MigLayout("fillx, insets 10 20 10 50");  
        main_panel = new JPanel(choose_layout);
        //main_panel.setBackground(Color.WHITE);
        main_panel.add(label_server_1, "wrap 20");
        main_panel.add(label_server_2, "wrap 30");
        main_panel.add(label_server_3, "wrap 5");
        main_panel.add(choose_server, "wrap 10");
        main_panel.add(label_server_4, "wrap 5");
        main_panel.add(choose_language, "wrap 10");
        
        this.setLayout(new BorderLayout());
        this.add(BorderLayout.CENTER, main_panel);
        
        //Gestione eventi
        choose_language.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                JComboBox comboBox = (JComboBox) event.getSource();
                Language selected = (Language) comboBox.getSelectedItem();
                selected_lang = selected;
                
                Globals.CURRENT_LOCALE = new Locale(selected.getCode());
                updateLanguage();
                Utility.setDefaultLangCurrent();
            }
        });
        

        this.setPreferredSize(this.getPreferredSize());
        this.validate();
        this.repaint();
    }
    
    /**
     * Test della connessione al server per l'acquisizione del certificato
     * Acquisice il certificato e restart dell'applicazione
     * @param uri
     * @return 
     */
    public static boolean testServerConnection(String uri) {
        Server s = (Server) choose_server.getSelectedItem();
        SelectedServer.getInstance(s);
                
        boolean result = true;
        String outputFile = Globals.USER_DIR + "certs" + Utility.getSep() + "jssecacerts.jks";
        
        //Aggiungo Keystore Temporaneo
        if (new File(outputFile).isFile()) {
            System.setProperty("javax.net.ssl.keyStore", outputFile);
            System.setProperty("javax.net.ssl.keyStorePassword", "changeit");
            System.setProperty("javax.net.ssl.trustStore", outputFile);
            System.setProperty("javax.net.ssl.trustStorePassword", "changeit");
        }
        
        try {
            URL url = new URL("https://" + uri);
            URLConnection con = url.openConnection();
            Reader reader = new InputStreamReader(con.getInputStream());
        } 
        catch (SSLHandshakeException ex){
            ResourceBundle bundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);
            Object[] options = {Utility.getBundleString("voc1", bundle), Utility.getBundleString("voc2", bundle)};
 
            int n = JOptionPane.showOptionDialog(null, Utility.getBundleString("phcertadd", bundle),
                    Utility.getBundleString("phcertadd", bundle),
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

            if (n == JOptionPane.YES_OPTION) {
                String[] run = new String[1];
                run[0] = uri + ":443";
                AddToStoreKey.createAndShowGUI(run);
                result = false;
            }
        }
        catch (Exception ex) {}

        return result;
    }
    
    /**
     * Aggiorna tutte i labels
     */
    private void updateLanguage(){
        bundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);
        
        label_server_1.setText("<html>"+Utility.getBundleString("server_1", bundle)+"</html>");
        label_server_2.setText("<html>"+Utility.getBundleString("server_2", bundle)+"</html>");
        label_server_3.setText("<html>"+Utility.getBundleString("server_3", bundle)+"</html>");
        label_server_4.setText("<html>"+Utility.getBundleString("server_4", bundle)+"</html>");
        
        JButton nextButton = (JButton) StartWizard.footerPanel.getComponentByName("next_button");
        JButton prevButton = (JButton) StartWizard.footerPanel.getComponentByName("prev_button");
        
        nextButton.setText(Utility.getBundleString("next", bundle));
        prevButton.setText(Utility.getBundleString("back", bundle));
    }
    
     /**
     * Carica le lingue del xml remoto
     * @param config
     * @param bundle 
     * @return  
     */
    public final void getLanguagesFromConfig(XMLConfiguration config, ResourceBundle bundle){
        String lang_name;
        String key;
        ArrayList<Language> langs = new ArrayList<Language>();
        
        java.util.List<HierarchicalConfiguration> resources = config.configurationsAt("resources.resource");
        for(HierarchicalConfiguration resource : resources) {
            lang_name = resource.getString("[@descr]");
            key = it.imtech.utility.Utility.getBundleString(lang_name, bundle);
            Language lang = new Language(resource.getString(""), lang_name, key);
            
            langs.add(lang);
            
            //Setto la descrizione di default
            if (resource.getString("").equals(Globals.CURRENT_LOCALE.getLanguage())){
                selected_lang = lang;
            }  
        }
        
        Globals.LANGUAGES = langs.toArray(new Language[langs.size()]);
    }
    
    /**
     * Setta il percorso del file di configurazione se è la prima volta che
     * viene avviata l'applicazione
     *
     * @param change Definisce se è l'avvio dell'applicazione o se si vuole
     * modificare il percorso
     * @throws MalformedURLException
     * @throws ConfigurationException
     */
    private XMLConfiguration setConfigurationPaths(boolean change, XMLConfiguration internalConf, ResourceBundle bundle) {
        URL urlConfig = null;
        XMLConfiguration configuration = null;
        
        try{
            String text = Utility.getBundleString("setconf", bundle);
            String title = Utility.getBundleString("setconf2", bundle);
            
            internalConf.setAutoSave(true);

            String n = internalConf.getString("configurl[@path]");

            if (n.isEmpty()) {
                String s = (String) JOptionPane.showInputDialog(new Frame(), text, title, JOptionPane.PLAIN_MESSAGE,
                        null, null, "http://phaidrastatic.cab.unipd.it/xml/config.xml");

                //If a string was returned, say so.
                if ((s != null) && (s.length() > 0)) {
                    internalConf.setProperty("configurl[@path]", s);
                    urlConfig = new URL(s);                   
                } else {
                    logger.info("File di configurazione non settato");
                }
            } else {
                urlConfig = new URL(n);
            }
            
            if (urlConfig != null){
                if(Globals.DEBUG)
                    configuration = new XMLConfiguration(new File(Globals.JRPATH+Globals.DEBUG_XML));
                else
                    configuration = new XMLConfiguration(urlConfig);
            }
        } catch (final MalformedURLException ex) {
            logger.error(ex.getMessage());
            JOptionPane.showMessageDialog(new Frame(), Utility.getBundleString("exc_conf_1", bundle));
        }
        catch (final ConfigurationException  ex) {
            logger.error(ex.getMessage());
            JOptionPane.showMessageDialog(new Frame(), Utility.getBundleString("exc_conf_2", bundle));
        }
        
        return configuration;
    }
    
    /**
     * Crea una array di oggetti Server contenuti nel file di configurazione
     *
     * @param config
     * @throws Exception
     */
    private Server[] getServersFromConfig(XMLConfiguration config, ResourceBundle bundle) {
        //Parametri di un server
        String servername;
        String fedoraurl;
        String phaidraurl;
        String staticurl;
        String stylesheeturl;
        String oaiIdentifier;
        
        ArrayList<Server> possibility = new ArrayList<Server>();
        java.util.List<HierarchicalConfiguration> servs = config.configurationsAt("servers.server");
        
        for(HierarchicalConfiguration server : servs) {
            servername = server.getString("servername");
            fedoraurl = server.getString("fedoraurl");
            phaidraurl = server.getString("phaidraurl");
            staticurl = server.getString("staticurl");
            stylesheeturl = server.getString("stylesheeturl");
            oaiIdentifier = server.getString("oaiIdentifier");
            
            Server sv = new Server(servername, fedoraurl, phaidraurl, staticurl, stylesheeturl, oaiIdentifier);
            possibility.add(sv);
        }
            
        return possibility.toArray(new Server[possibility.size()]);
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 374, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 238, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
