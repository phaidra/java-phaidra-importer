/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package it.imtech.configuration;

import it.imtech.bookimporter.BookImporter;
import it.imtech.dialogs.ConfirmDialog;
import it.imtech.globals.Globals;
import it.imtech.upload.SelectedServer;
import it.imtech.utility.Server;
import it.imtech.utility.Utility;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;
import java.util.ResourceBundle;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

public class StartWizard  {
    private static Logger logger = Logger.getLogger(StartWizard.class);
    
    CardLayout c1 = new CardLayout();
    
    HeaderPanel headerPanel;
    static FooterPanel footerPanel;
    
    JPanel cardsPanel;
    JPanel mainPanel;
   
    ChooseServer chooseServer;
    ChooseFolder chooseFolder;
    
    JFrame mainFrame;
    
    ResourceBundle bundle;
     
    private void backupOnlineFiles(){
        
    }
    
    /**
     * Creates new form Main
     */
    public StartWizard() {
        mainFrame = new JFrame();
        logger.info("Start Wizard");
        
        if(Utility.internetConnectionAvailable()){
            Globals.ONLINE = true;
        }
        
        Globals.setGlobalVariables();
        
        bundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);
        
        if(!checkAppDataFiles()){
            JOptionPane.showMessageDialog(null, Utility.getBundleString("copy_appdata", bundle), 
            Utility.getBundleString("copy_appdata_title", bundle), JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
        
        DOMConfigurator.configure(Globals.LOG4J);
        logger = Logger.getLogger(StartWizard.class);
        
        logger.info("Starting Application Phaidra Importer");
        
        it.imtech.utility.Utility.cleanUndoDir();
        
        XMLConfiguration internalConf = setConfiguration();

        XMLConfiguration config = setConfigurationPaths(false, internalConf, bundle);
        
        //Creazione Header
        headerPanel = new HeaderPanel();
        footerPanel = new FooterPanel();
        
        JButton nextButton = (JButton) footerPanel.getComponentByName("next_button");
        JButton prevButton = (JButton) footerPanel.getComponentByName("prev_button");
        
        nextButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent event)
            {
                if(getCurrentCard() instanceof ChooseServer){
                    mainFrame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    
                    Server selected = chooseServer.getSelectedServer();
                    SelectedServer.getInstance(null).makeEmpty();
                    SelectedServer.getInstance(selected);
                    
                    if (Globals.ONLINE){
                        ChooseServer.testServerConnection(SelectedServer.getInstance(null).getBaseUrl());
                    }
                    chooseFolder.updateLanguage();
                    
                    c1.next(cardsPanel);
                    mainFrame.setCursor(null);
                }
                else if(getCurrentCard() instanceof ChooseFolder){
                    mainFrame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    boolean error = chooseFolder.checkFolderSelectionValidity();
                
                    if (error==false){
                        BookImporter x = BookImporter.getInstance();
                        mainFrame.setCursor(null);
                        mainFrame.dispose();
                    }
                }
            }
        });
        
        prevButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent event)
            {
               if(getCurrentCard() instanceof ChooseServer){
                   ResourceBundle bundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);
                   String title = Utility.getBundleString("dialog_1_title", bundle);
                   String text = Utility.getBundleString("dialog_1", bundle);
                   
                   ConfirmDialog confirm = new ConfirmDialog(mainFrame, true, title, text, Utility.getBundleString("confirm", bundle), Utility.getBundleString("confirm", bundle));
                   
                   confirm.setVisible(true);
                   boolean close = confirm.getChoice();
                   confirm.dispose();

                   if (close == true){
                       mainFrame.dispose();
                   }
               }
               else{
                    c1.previous(cardsPanel);
               }
            }
        });        
        
        cardsPanel = new JPanel(new CardLayout());
        cardsPanel.setBackground(Color.WHITE);
        
        chooseServer = new ChooseServer(config);
        chooseFolder = new ChooseFolder();
       
        cardsPanel.add(chooseServer, "1");
        cardsPanel.add(chooseFolder, "2");
    
        cardsPanel.setLayout(c1);
        c1.show(cardsPanel, "1");
        
        //Composizione pannello principale
        mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(BorderLayout.NORTH, headerPanel);
        mainPanel.add(BorderLayout.CENTER, cardsPanel);
        mainPanel.add(BorderLayout.PAGE_END, footerPanel);
        mainFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        
        mainFrame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                ResourceBundle bundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);
                String title = Utility.getBundleString("dialog_1_title", bundle);
                String text = Utility.getBundleString("dialog_1", bundle);
                   
                ConfirmDialog confirm = new ConfirmDialog(mainFrame, true, title, text, Utility.getBundleString("confirm", bundle), Utility.getBundleString("confirm", bundle));
                   
                confirm.setVisible(true);
                boolean close = confirm.getChoice();
                confirm.dispose();

                if (close == true){
                    mainFrame.dispose();
                }
            }
        });
        
        //Stile interfaccia
        mainFrame.getContentPane().setBackground(Color.white);
        mainFrame.getContentPane().setLayout(new BorderLayout());
        mainFrame.getContentPane().setPreferredSize(new Dimension(640, 400));
        mainFrame.getContentPane().add(BorderLayout.CENTER, mainPanel);
        //mainFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        mainFrame.pack();
        
        //Centra il frame in mezzo allo schermo
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (dim.width - mainFrame.getSize().width) / 2;
        int y = (dim.height - mainFrame.getSize().height) / 2;
        mainFrame.setLocation(x, y);
        
        mainFrame.setVisible(true);
    }
    
    public JPanel getCurrentCard()
    {
        JPanel card = null;

        for (Component comp : cardsPanel.getComponents() ) {
            if (comp.isVisible() == true) {
               card = (JPanel)comp;
            }
        }
        
        return card;
    }

    private boolean checkAppDataFiles(){
        boolean result = false;
        
        try{
            File appdata = new File(Globals.USER_DIR);
            String currentpath = Globals.JRPATH + "appdata" + Utility.getSep();
            File blank = new File(currentpath + "config" + Utility.getSep() + "blankpage.jpg");
            File xmlconfnew = new File(currentpath + "config" + Utility.getSep() + "config.xml");
            File logforj = new File(currentpath + "config" + Utility.getSep() + "log4j.xml");
            
            if (!appdata.exists()){
                appdata.mkdir();
            }
            
            File backupxml = new File(currentpath + "xml");
            File remotexml = new File(Globals.USER_DIR + "xml");
            
            File templates = new File (Globals.USER_DIR + "templates");
            if (!templates.exists()){
                templates.mkdir();
            }
            
            if (!remotexml.exists()){
                FileUtils.copyDirectory(backupxml, remotexml);
            }
            
            File config = new File(Globals.USER_DIR + "config");
            if (!config.exists()){
                config.mkdir();
            }
            
            File undo = new File(Globals.USER_DIR + "undo");
            if (!undo.exists()){
                undo.mkdir();
            }
            
            File certs = new File(Globals.USER_DIR + "certs");
            if (!certs.exists()){
                certs.mkdir();
            }
            
            File uploads = new File(Globals.USER_DIR + "duplication");
            if (!uploads.exists()){
                uploads.mkdir();
            }
            
            File xmlconfold = new File(Globals.USER_DIR + "config" + Utility.getSep() +"config.xml");
            if (xmlconfold.exists()){
                try{
                    XMLConfiguration configureold = new XMLConfiguration(xmlconfold);
                    XMLConfiguration configurenew = new XMLConfiguration(xmlconfnew);

                    String versioneold = configureold.getString("version[@current]");
                    String versionenew = configurenew.getString("version[@current]");

                    String urlold = configureold.getString("configurl[@path]");
                    String urlnew = configurenew.getString("configurl[@path]");
                    if ((!versioneold.equals(versionenew)) || (!urlold.equals(urlnew)))
                    {
                      xmlconfold.delete();
                      FileUtils.copyFile(xmlconfnew, xmlconfold);
                    }
                  }
                  catch (ConfigurationException ex)
                  {
                    logger.error("ERR:0002 Cannot copy configuration application data");
                    logger.error(ex.getMessage());
                    result = false;
                  }
            } else {
                FileUtils.copyFile(xmlconfnew, xmlconfold);
            }

            File logforjnew = new File(Globals.USER_DIR + "config" + Utility.getSep() +"log4j.xml");
            if (!logforjnew.exists()){
                FileUtils.copyFile(logforj, logforjnew);
            }
            
            File blanknew = new File(Globals.USER_DIR + "config" + Utility.getSep() +"blankpage.jpg");
            if (!blanknew.exists()){
                FileUtils.copyFile(blank, blanknew);
            }
            
            result = true;
        } catch (IOException ex) {
            logger.error("ERR:0002 Cannot copy application data");
            logger.error(ex.getMessage());
            result = false;
        }
        return result;
    }
    
    private XMLConfiguration setConfiguration(){
        XMLConfiguration config = null;
        
        try{
            config = new XMLConfiguration(Globals.INTERNAL_CONFIG);
            Globals.CURRENT_VERSION = config.getString("version[@current]");

            mainFrame.setTitle("Phaidra Importer v." + config.getString("version[@current]"));
            String locale = config.getString("locale.current[@value]");
            Globals.CURRENT_LOCALE = new Locale(locale);
        }
        catch (ConfigurationException ex) {
            logger.error(ex.getMessage());
            Globals.CURRENT_LOCALE = new Locale("en");
        } 
        
        return config;
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
                    Globals.URL_CONFIG = new URL(s);                   
                } else {
                    logger.info("File di configurazione non settato");
                }
            } else {
                if (Globals.ONLINE){
                    Globals.URL_CONFIG = new URL(n);
                }   
            }
            
            if (Globals.URL_CONFIG != null){
                if(Globals.DEBUG)
                    configuration = new XMLConfiguration(new File(Globals.JRPATH + Globals.DEBUG_XML));
                else
                    configuration = new XMLConfiguration(Globals.URL_CONFIG);
            }
            else{
                if (!Globals.ONLINE){
                    configuration = new XMLConfiguration(new File(Globals.USER_DIR + Utility.getSep() + Globals.FOLD_XML + "config.xml"));
                }
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

                new StartWizard();
            }
        });
    }
}