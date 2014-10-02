/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package it.imtech.configuration;

import it.imtech.bookimporter.BookImporter;
import it.imtech.dialogs.ConfirmDialog;
import it.imtech.globals.Globals;
import it.imtech.metadata.MetaUtility;
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

/**
 * Represents the starting wizard with two cards 
 * (Select Folder and Select Server/Language)
 * @author I.M. Technologies
 */
public class StartWizard  {
    /**
     * Represents the log configuration for this class
     */
    private static Logger logger = Logger.getLogger(StartWizard.class);
    
    CardLayout c1 = new CardLayout();
    
    HeaderPanel headerPanel;
    static FooterPanel footerPanel;
    
    JPanel cardsPanel;
    JPanel mainPanel;
   
    ChooseServer chooseServer;
    ChooseFolder chooseFolder;
    
    JFrame mainFrame;
    
    /**
     * Creates a new wizard with active card (Select Language/Server)
     */
    public StartWizard() {
        Globals.setGlobalVariables();
        ResourceBundle bundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);
        
        if(!checkAppDataFiles()){
            JOptionPane.showMessageDialog(null, Utility.getBundleString("copy_appdata", bundle), 
            Utility.getBundleString("copy_appdata_title", bundle), JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
        
        logger = Logger.getLogger(StartWizard.class);
        logger.info("Starting Application Phaidra Importer");
        
        mainFrame = new JFrame();
        
        if(Utility.internetConnectionAvailable()){
            Globals.ONLINE = true;
        }
        
        it.imtech.utility.Utility.cleanUndoDir();
        
        XMLConfiguration internalConf = setConfiguration();
        logger.info("Configuration path estabilished");
        
        XMLConfiguration config = setConfigurationPaths(internalConf, bundle);
        
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
                    ResourceBundle bundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);
        
                    try {
                        mainFrame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                        
                        Server selected = chooseServer.getSelectedServer();
                        logger.info("Selected Server = "+selected.getServername());
                        SelectedServer.getInstance(null).makeEmpty();
                        SelectedServer.getInstance(selected);
                        
                        if (Globals.ONLINE){
                            logger.info("Testing server connection...");
                            ChooseServer.testServerConnection(SelectedServer.getInstance(null).getBaseUrl());
                        }
                        chooseFolder.updateLanguage();
                        
                        MetaUtility.getInstance().preInitializeData();
                        logger.info("Preinitialization done (Vocabulary and Languages");
                        
                        c1.next(cardsPanel);
                        mainFrame.setCursor(null);
                    } catch (Exception ex) {
                        logger.error(ex.getMessage());
                        JOptionPane.showMessageDialog(new Frame(), Utility.getBundleString("preinitializemetadataex",bundle));
                    }
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
                   
                   ConfirmDialog confirm = new ConfirmDialog(mainFrame, true, title, text, Utility.getBundleString("confirm", bundle), Utility.getBundleString("back", bundle));
                   
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
        
        //Main Panel style
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
                   
                ConfirmDialog confirm = new ConfirmDialog(mainFrame, true, title, text, Utility.getBundleString("confirm", bundle), Utility.getBundleString("back", bundle));
                   
                confirm.setVisible(true);
                boolean close = confirm.getChoice();
                confirm.dispose();

                if (close == true){
                    mainFrame.dispose();
                }
            }
        });
        
        //Add Style 
        mainFrame.getContentPane().setBackground(Color.white);
        mainFrame.getContentPane().setLayout(new BorderLayout());
        mainFrame.getContentPane().setPreferredSize(new Dimension(640, 400));
        mainFrame.getContentPane().add(BorderLayout.CENTER, mainPanel);
        mainFrame.pack();
        
        //Center frame in the screen
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (dim.width - mainFrame.getSize().width) / 2;
        int y = (dim.height - mainFrame.getSize().height) / 2;
        mainFrame.setLocation(x, y);
        
        mainFrame.setVisible(true);
    }
    
    /**
     * Returns the current active card
     * @return 
     */
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

    /**
     * Creates necessary folders in home user menu
     * @return 
     */
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
            
            File templates = new File (Globals.USER_DIR + "templates");
            if (!templates.exists()){
                templates.mkdir();
            }
            
            File backupxml = new File(currentpath + "xml");
            File remotexml = new File(Globals.USER_DIR + "xml");
            FileUtils.copyDirectory(backupxml, remotexml);
            
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
            FileUtils.copyFile(logforj, logforjnew);
            
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
    
    /**
     * Sets the version and default locale
     * @return 
     */
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
     * Retrieve online configuration filepath
     *
     * @throws MalformedURLException
     * @throws ConfigurationException
     */
    private XMLConfiguration setConfigurationPaths(XMLConfiguration internalConf, ResourceBundle bundle) {
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