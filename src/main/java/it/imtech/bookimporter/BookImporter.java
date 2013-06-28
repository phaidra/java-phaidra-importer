package it.imtech.bookimporter;

import it.imtech.about.About;
import it.imtech.certificate.AddToStoreKey;
import it.imtech.globals.Globals;
import it.imtech.metadata.MetaUtility;
import it.imtech.metadata.Metadata;
import it.imtech.pdfepub.Epub;
import it.imtech.pdfepub.PdfCreateMonitor;
import it.imtech.upload.PhaidraUtils;
import it.imtech.upload.UploadSettings;
import it.imtech.utility.Server;
import it.imtech.utility.Utility;
import it.imtech.xmltree.XMLNode;
import it.imtech.xmltree.XMLTree;
import java.awt.*;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import javax.net.ssl.SSLHandshakeException;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.xml.DOMConfigurator;
import org.jdesktop.swingx.JXDatePicker;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Classe principale di gestione del frame PhaidraImporter
 *
 * @author Medelin Mauro
 */
public class BookImporter extends javax.swing.JFrame {
    
    //Metadati remoti book o class
    public static String URL_METADATA = Globals.FOLD_XML + "uwmetadata.xml";
    
    //Link alle risorse di lingua
    public static String RESOURCES = "resources" + Utility.getSep() + "messages";
    
    //Definisce se si sta lavorando su un libro o una collection
    public static char TYPEBOOK = 'X';
    
    //Lingua corrente usata per visualizzare le interfacce
    public static Locale currentLocale = null;
 
    //Istanza della classe SingleTon BookImporter
    private static BookImporter instance = null;

    //Contiene la mappatura del file dei metadati preso da URL
    protected TreeMap<Object, Metadata> metadata = new TreeMap<Object, Metadata>();
    
    //Contiene una mappatura gli elementi dell'interfaccia dei metadati con i quali l'utente puo interagire
    protected HashMap componentMap;
   
    //Oggetto che identifica l'albero del libro/collezione
    public static XMLTree xmlTree;
    
    //Path assoluto alla cartella di lavoro corrente
    public static String selectedFolder = null;
    
    //Path assoluto alla cartella di lavoro corrente con separatore
    public static String selectedFolderSep = null;
    
    //URL del File di configurazione su server
    public static URL urlConfig = null;
            
    //XMLConfiguration del File di configurazione Su server
    public static XMLConfiguration config = null;
    
    //File di configurazione interno (Lingua default, url config url updater e versione)
    public static XMLConfiguration internalConf = null;
    
    //file di configurazione delle classificazioni valido per progetto
    public static XMLConfiguration classifConf = null;
    
    //Path della cartella che contiene l'eseguibile dell'applicazione
    public static String jrPath = null;

    public static boolean folderWritable = true;
     
    public static Locale localConst = null;
    /**
     * Metodo di gestione del Singleton
     *
     * @return
     */
    public static BookImporter getInstance() {
        if (instance == null) {
            instance = new BookImporter();

            if (new File(selectedFolderSep + Globals.IMP_EXP_METADATA).isFile()) {
                instance.importMetadata();
            }
        }
        return instance;
    }

    /**
     * Costruttore della classe BookImporter
     */
    public BookImporter() {
        Globals.setDebug();
        setCurrentJarDirectory();
        
        cleanUndoDir();
        initComponents();

        try {
            DOMConfigurator.configure(Globals.LOG4J);
            urlConfig = setConfigurationPath(false);
            
            try{
                if(Globals.DEBUG)
                    config = new XMLConfiguration(new File(jrPath+Globals.DEBUG_XML));
                else
                    config = new XMLConfiguration(urlConfig);
            } catch (ConfigurationException ex) {
                Globals.logger.error(ex.getMessage());
                JOptionPane.showMessageDialog(new Frame(), ex.getMessage());
            }
            
            try{
                this.setTitle("Phaidra Importer v." + internalConf.getString("version[@current]"));
                String locale = internalConf.getString("locale.current[@value]");
                currentLocale = new Locale(locale);
                localConst = new Locale(locale);
            }
            catch (Exception ex) {
                Globals.logger.error(ex.getMessage());
                currentLocale = new Locale("en");
            }
            
            //Gestione lingue e server
            createLanguageMenu();
            configureLanguage();
            configureServer();
            configureUrlUpdater();
            
            if (PhaidraUtils.getInstance(null) != null) {
               
                MetaUtility.getInstance().preInitializeData();
            
                if (testServerConnection(PhaidraUtils.getInstance(null).getBaseUrl())) {
                    updateLanguageLabel(currentLocale);

                    while(TYPEBOOK == Globals.NOT_EXISTS){
                        setSelectedFolder();

                        if (selectedFolder != null) {
                            TYPEBOOK = XMLTree.getTypeFileBookstructure(selectedFolderSep);

                            if (TYPEBOOK == Globals.NOT_EXISTS) {
                                Utility.chooseBookOrCollection();
                            }
                        }
                        else {
                             Globals.logger.info("Folder non selezionato");
                            System.exit(0);
                        }
                    }
                    
                    //Inizializzazione classificazione e creazione interfaccia dinamica    
                    initializeData();

                    boolean fromFile = askForStructure();
                    initializeXmlTree(fromFile,false);

                        if (TYPEBOOK == Globals.COLLECTION) {
                            hideBookElements();
                        }

                        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
                        int x = (dim.width - getSize().width) / 2;
                        int y = (dim.height - getSize().height) / 2;
                        setLocation(x, y);
                        setVisible(true);
                }
            } else {
                Globals.logger.info("Server non selezionato");
                System.exit(0);
            }
        } catch (ConfigurationException ex) {
            Globals.logger.error(ex.getMessage());
            JOptionPane.showMessageDialog(new Frame(), ex.getMessage());
            System.exit(0);
        } catch (Exception ex) {
            Globals.logger.error(ex.getMessage());
            JOptionPane.showMessageDialog(new Frame(), ex.getMessage());
            System.exit(0);
        }
    }
    
    public TreeMap<Object, Metadata> getMetadata(){
        return metadata;
    }
    
    public static Locale getCurrentLocale() {
        return currentLocale;
    }
    
    public JLabel getLabelPreviewImage() {
        return jLabel3;
    }
    
    public boolean ocrBoxIsChecked() {
        return jCheckBox1.isSelected();
    }
     
    public String getPathPdf() {
        return jTextField2.getText();
    }
    
    /**
     * Restituisce un componente contenuto nella struttura dati che mappa gli
     * oggetti dell'interfaccia dei metadati in base al nome passato come
     * parametro.
     *
     * @param name
     * @return Component
     */
    public Component getComponentByName(String name) {
        if (componentMap.containsKey(name)) {
            return (Component) componentMap.get(name);
        } else {
            return null;
        }
    }

    /**
     * Restituisce un valore booleano (Libro/Collezione) in base al valore di
     * TYPEBOOK
     *
     * @return
     */
    public boolean isBook() {
        if (TYPEBOOK == Globals.BOOK) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Resetta i componenti dell'interfaccia dei metadati
     */
    public void createComponentMap() {
        componentMap = new HashMap<String, Component>();
        createComponentMap(main_panel);
    }

    public void initializeXmlTree(boolean fromFile,boolean refresh) {
        xmlTree = new XMLTree(selectedFolderSep, fromFile, false,refresh);

        if (XMLTree.getRoot() == null) {
             Globals.logger.info("Errore inizializzazione file bookstructure");
            JOptionPane.showMessageDialog(new Frame(), "Errore inizializzazione file bookstructure");
            System.exit(0);
        }

        setViewXmltree(xmlTree);
    }

    public void initializeUndoXmlTree(String file) {
        xmlTree = new XMLTree(file, true, true,false);

        if (XMLTree.getRoot() == null) {
             Globals.logger.info("Errore inizializzazione file bookstructure");
            JOptionPane.showMessageDialog(new Frame(), "Errore inizializzazione file bookstructure");
            System.exit(0);
        }

        setViewXmltree(xmlTree);
    }

    public void setViewXmltree(XMLTree xmlTree) {
        XMLTree.expandAll(xmlTree);
        jScrollPane1.setViewportView(xmlTree);
    }
    
    /**
     * Setta la directory dell'eseguibile dell'applicazione
     */
    protected final void setCurrentJarDirectory() {
        //Get Current jar Directory
        final URL url = BookImporter.class.getProtectionDomain().getCodeSource().getLocation();
        try {
            final File jarPath = new File(url.toURI()).getParentFile();
            jrPath = jarPath.getAbsolutePath() + Utility.getSep();
            RESOURCES = jrPath + RESOURCES;
        } catch (final URISyntaxException ex) {
            Globals.logger.error(ex.getMessage());
            JOptionPane.showMessageDialog(new Frame(), ex.getMessage());
        }
    }

    /**
     * Effettua il reload dell'interfaccia dinamica dei metadati in base al file
     * uwmetadata.xml contenuto nella cartella di lavoro corrente.
     *
     */
    protected void importMetadata() {
        ResourceBundle bundle = ResourceBundle.getBundle(RESOURCES, currentLocale, Globals.loader);

        try {
            if (new File(selectedFolderSep + Globals.IMP_EXP_METADATA).isFile()) {
                Object[] options = {Utility.getBundleString("voc1", bundle), Utility.getBundleString("voc2", bundle)};
                int n = JOptionPane.showOptionDialog(this, Utility.getBundleString("loadUwmetadataText", bundle),
                        Utility.getBundleString("loadUwmetadata", bundle),
                        JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

                if (n == JOptionPane.YES_OPTION) {
                    //Leggi il file uwmetadata.xml
                    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    MetaUtility.getInstance().read_uwmetadata(selectedFolderSep + Globals.IMP_EXP_METADATA);

                    jLayeredPane1.removeAll();
                    jLayeredPane1.revalidate();

                    //Ridisegna l'interfaccia
                    this.setMetadataTab();
                    setCursor(null);
                    JOptionPane.showMessageDialog(this, Utility.getBundleString("import4", bundle));
                }
            } else {
                setCursor(null);
                JOptionPane.showMessageDialog(this, Utility.getBundleString("errorloadUwmetadataText", bundle));
            }
        } catch (Exception ex) {
            setCursor(null);
            JOptionPane.showMessageDialog(this, Utility.getBundleString("errorloadUwmetadataText", bundle) + ": " + ex.getMessage());
        }
    }
    
    /**
     * Richiede l'import della struttura del libro/collezione se esistente
     * @return 
     */
    private boolean askForStructure(){ 
        boolean response = false;
        
        if(new File(selectedFolderSep+Globals.IMP_EXP_BOOK).isFile()){
            ResourceBundle bundle = ResourceBundle.getBundle(RESOURCES, currentLocale, Globals.loader);

            Object[] options = {Utility.getBundleString("voc1", bundle), Utility.getBundleString("voc2", bundle)};
            int n = JOptionPane.showOptionDialog(this, Utility.getBundleString("loadStructureText", bundle),
                    Utility.getBundleString("loadStructure", bundle),
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

                if (n == JOptionPane.YES_OPTION) {
                   response = true;
                }
          }
          return response;
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
    private URL setConfigurationPath(boolean change) {
        URL result = null;
        try{
            internalConf = new XMLConfiguration(Globals.internalConfig);
            String text = "Set configuration file path";
            String title = "Configuration file";

            if (this.currentLocale != null) {
                ResourceBundle bundle = ResourceBundle.getBundle(RESOURCES, currentLocale, Globals.loader);
                text = Utility.getBundleString("setconf", bundle);
                title = Utility.getBundleString("setconf2", bundle);
            }

            internalConf.setAutoSave(true);

            String n = internalConf.getString("configurl[@path]");

            if (n.isEmpty()) {
                String s = (String) JOptionPane.showInputDialog(new Frame(), text, title, JOptionPane.PLAIN_MESSAGE,
                        null, null, "http://phaidrastatic.cab.unipd.it/xml/config.xml");

                //If a string was returned, say so.
                if ((s != null) && (s.length() > 0)) {
                    internalConf.setProperty("configurl[@path]", s);
                    result = new URL(s);
                    
                } else {
                    Globals.logger.info("File di configurazione non settato");
                }
            } else {
                result = new URL(n);
            }
        } catch (final MalformedURLException ex) {
             Globals.logger.error(ex.getMessage());
            JOptionPane.showMessageDialog(new Frame(), ex.getMessage());
        }
        catch (final ConfigurationException  ex) {
             Globals.logger.error(ex.getMessage());
            JOptionPane.showMessageDialog(new Frame(), ex.getMessage());
        }
        return result;
    }

    /**
     * Seleziona la cartella di lavoro iniziale e setta i due valori
     *
     */
    private void setSelectedFolder() {
        ResourceBundle bundle = ResourceBundle.getBundle(RESOURCES, currentLocale, Globals.loader);
        
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setAcceptAllFileFilterUsed(true);

        // Set the text
        fileChooser.setDialogTitle(Utility.getBundleString("selectfolder", bundle));
        fileChooser.setApproveButtonText(Utility.getBundleString("select", bundle));
        fileChooser.setApproveButtonMnemonic('e');
        fileChooser.setApproveButtonToolTipText(Utility.getBundleString("tooltipselect", bundle));
        
        fileChooser.setLocale(currentLocale);
        fileChooser.updateUI();
        
        if (fileChooser.showOpenDialog(BookImporter.this) == JFileChooser.APPROVE_OPTION) {
            selectedFolder = fileChooser.getSelectedFile().toString();
            selectedFolderSep = selectedFolder + Utility.getSep();
        }
        
        try{
            File testFile = Utility.getUniqueFileName(BookImporter.selectedFolderSep+"testfile", "txt");
            Writer output = null;
            output = new BufferedWriter(new FileWriter(testFile));
            output.close();
            
            testFile.delete();
        }
        catch (Exception ex) {
            folderWritable = false;
        }
    }

    /**
     * Nasconde gli elementi dell'interfaccia riguardanti il libro nel caso si
     * tratti di una collezione e viceversa.
     */
    private void hideBookElements() {
        jPanel2.setVisible(false);
        jMenuItem4.setVisible(false);
        jMenuItem7.setVisible(false);
        jLayeredPane2.revalidate();

        ResourceBundle bundle = ResourceBundle.getBundle(RESOURCES, currentLocale, Globals.loader);
        jMenuItem8.setText(Utility.getBundleString("cimportcollection", bundle));
        jMenuItem2.setText(Utility.getBundleString("cimportcollection", bundle));
    }

    /**
     * Crea dinamicamente il menu delle lingue a partire dal file di
     * configurazione aggiunge gli eventi agli Items del menu e setta la lingua
     * di default
     *
     * @param config
     * @throws Exception
     */
    private void createLanguageMenu() throws Exception {
        ResourceBundle bundle = ResourceBundle.getBundle(RESOURCES, currentLocale, Globals.loader);

        //Per ogni lingua trovata
        boolean hasResources = true;
        int i = 0;
        
        while(hasResources){
            try {
            final String language = config.getString("resources.resource(" + i + ")");
            String descr = config.getString("resources.resource(" + i + ")[@descr]");

            final Locale local = new Locale(language);

            //Crea un nuovo menu item
            final JCheckBoxMenuItem lang = new JCheckBoxMenuItem();
            lang.setName(descr);
            lang.setText(Utility.getBundleString(descr, bundle));
            lang.setToolTipText(language);

            //Aggiungi l'evento
            lang.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    try {
                        currentLocale = local;
                        Locale.setDefault(currentLocale);
                        updateLanguageLabel(local);

                        if (instance != null) {
                            setDefaultCurrent();
                        }

                        
                        int items = jMenu1.getItemCount();
                        for (int z = 0; z < items; z++) {
                            JCheckBoxMenuItem item = (JCheckBoxMenuItem) jMenu1.getItem(z);
                            //Setta il default
                            if (item.getToolTipText().equals(language)) {
                                item.setSelected(true);
                            } else {
                                item.setSelected(false);
                            }
                        }
                    }   catch (Exception ex) {
                            Globals.logger.error(ex.getMessage());
                        JOptionPane.showMessageDialog(new Frame(), ex.getMessage());
                        } 
                }
            });
            int count = jMenu1.getItemCount();
            jMenu1.add(lang);
            jMenu1.getItem(count).setSelected(true);
         i++;
            } catch (Exception ex) {
                hasResources=false;
            }

        }
    }

    /**
     * Attiva l'evento click per la lingua settata come default nel file di
     * configurazione
     *
     * @param config
     * @throws Exception
     */
    private void configureLanguage() throws Exception {
        int totalLangs = jMenu1.getItemCount();

        for (int i = 0; i < totalLangs; i++) {
            JCheckBoxMenuItem item = (JCheckBoxMenuItem) jMenu1.getItem(i);

            if (item.getToolTipText().equals(currentLocale.getLanguage())) {
                item.doClick();
            }
        }
    }

    /**
     * Aggiorna il link per il file di configurazione dell'aggiornamento
     * dell'applicazione.
     * @param config 
     */
    private void configureUrlUpdater(){ 
        try {
            if(!internalConf.getString("urlupdater.descrurl").equals(config.getString("urlupdater.descrurl"))){
                internalConf.setAutoSave(true);
                internalConf.setProperty("urlupdater.descrurl", config.getString("urlupdater.descrurl"));
            }
            
        }
        catch (Exception ex) {
             Globals.logger.error(ex.getMessage());
        }
    }
    /**
     * Crea una select di oggetti Server, e al termine della scelta del server
     * setta l'oggetto PhaidraUtils (Singleton) con i parametri del server
     * scelto
     *
     * @param config
     * @throws Exception
     */
    private void configureServer() throws Exception {
        ResourceBundle bundle = ResourceBundle.getBundle(RESOURCES, currentLocale, Globals.loader);
        if(config!=null){
            ArrayList<Server> possibility = new ArrayList<Server>();
            boolean hasServers = true;
            int i = 0;

            while(hasServers){
                try {
                    String servername = config.getString("servers.server(" + i + ").servername");
                    String fedoraurl = config.getString("servers.server(" + i + ").fedoraurl");
                    String phaidraurl = config.getString("servers.server(" + i + ").phaidraurl");
                    String staticurl = config.getString("servers.server(" + i + ").staticurl");
                    String stylesheeturl = config.getString("servers.server(" + i + ").stylesheeturl");
                    String oaiIdentifier = config.getString("servers.server(" + i + ").oaiIdentifier");

                    if(servername==null || fedoraurl==null || phaidraurl==null)
                        hasServers=false;
                    else{
                        Server sv = new Server(servername, fedoraurl, phaidraurl, staticurl, stylesheeturl, oaiIdentifier);
                        possibility.add(sv);
                    }
                    i++;
                } 
                catch (Exception ex) {
                    hasServers=false;
                }
            }

            Server[] possibilities = possibility.toArray(new Server[possibility.size()]);

            Object[] options = {Utility.getBundleString("ok", bundle), Utility.getBundleString("annulla", bundle)};
            Server s = (Server) JOptionPane.showInputDialog(this,
                    Utility.getBundleString("PhaidraLocationLabel", bundle), Utility.getBundleString("PhaidraLocationLabel", bundle),
                    JOptionPane.PLAIN_MESSAGE, null, possibilities, options[0]);

            //Aggiungi i parametri a PhaidraUtils
            if ((s != null) && s.allParametersDefined()) {
                PhaidraUtils.getInstance(s);
            }
        }
        else{
            Server sv = new Server("dummy", "dummy", "dummy", "dummy", "dummy", "dummy");
            PhaidraUtils.getInstance(sv);
        }
    }

    /**
     * Test della connessione al server per l'acquisizione del certificato
     * Acquisice il certificato e restart dell'applicazione
     * @param uri
     * @return 
     */
    private boolean testServerConnection(String uri) {
        boolean result = true;
        String outputFile = Globals.USER_DIR + "certs" + Utility.getSep() + "jssecacerts.jks";
        
        //Aggiungo Keystore Temporaneo
        if (new File(outputFile).isFile()) {
            System.setProperty("javax.net.ssl.keyStore", outputFile);
            System.setProperty("javax.net.ssl.keyStorePassword", "changeit");
            System.setProperty("javax.net.ssl.trustStore", outputFile);
            System.setProperty("javax.net.ssl.trustStorePassword", "changeit");
             Globals.logger.info(System.getProperty("javax.net.ssl.trustStore"));
        }
        
        try {
            if(config!=null){
                URL url = new URL("https://" + uri);
                URLConnection con = url.openConnection();
                Reader reader = new InputStreamReader(con.getInputStream());
            }
        } 
        catch (SSLHandshakeException ex){
            ResourceBundle bundle = ResourceBundle.getBundle(RESOURCES, currentLocale, Globals.loader);
            Object[] options = {Utility.getBundleString("voc1", bundle), Utility.getBundleString("voc2", bundle)};

            int n = JOptionPane.showOptionDialog(this, Utility.getBundleString("phcertadd", bundle),
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
     * Setta la lingua corrente come lingua di default nel file di
     * configurazione
     *
     * @throws ConfigurationException
     */
    private void setDefaultCurrent() throws ConfigurationException {
        internalConf.setAutoSave(true);

        ResourceBundle bundle = ResourceBundle.getBundle(RESOURCES, currentLocale, Globals.loader);

        Object[] options = {Utility.getBundleString("voc1", bundle), Utility.getBundleString("voc2", bundle)};
        int n = JOptionPane.showOptionDialog(this, Utility.getBundleString("setasdefaultlang", bundle),
                Utility.getBundleString("setasdefaultlangtitle", bundle),
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

        if (n == JOptionPane.YES_OPTION) {
            internalConf.setProperty("locale.current[@value]", currentLocale.getLanguage());
        }
    }

    /**
     * Crea un file nella cartella di lavoro che definisce la classificazione scelta
     */
    private void setDefaultClassificationFile() {
        try {
            if (!new File(selectedFolderSep + Globals.classifConfig).isFile()) {
                DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
                Document doc = docBuilder.newDocument();

                Element rootElement = doc.createElement("classification");
                rootElement.setAttribute("default", "");
                rootElement.setTextContent("File di Classificazione");
                doc.appendChild(rootElement);

                Transformer t = TransformerFactory.newInstance().newTransformer();
                t.setOutputProperty(OutputKeys.INDENT, "yes");
                t.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
                t.setOutputProperty(OutputKeys.STANDALONE, "");
                t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

                it.imtech.xmltree.XMLUtil.xmlWriter(doc, selectedFolderSep + Globals.classifConfig);
            }

            classifConf = new XMLConfiguration(selectedFolderSep + Globals.classifConfig);

        } catch (TransformerConfigurationException ex) {
             Globals.logger.error(ex.getMessage());
        } catch (ParserConfigurationException ex) {
             Globals.logger.error(ex.getMessage());
        } catch (ConfigurationException ex) {
             Globals.logger.error(ex.getMessage());
        }
    }

    /**
     * Setta tutte le strutture dati necessarie per la creazione
     * dell'interfaccia dei metadati
     *
     */
    private void initializeData() {
        //Scelgo i metadati
        URL_METADATA = (TYPEBOOK == Globals.COLLECTION) ? Globals.URL_METADATA_COLL : URL_METADATA;

        try {
            //Inserisce tutti i campi dinamici visibili nella struttura dati metadata
            this.metadata = MetaUtility.getInstance().metadata_reader();

            //Setta il file di classificazione scelto
            if(folderWritable)
                setDefaultClassificationFile();

            //Inserisce tutte le classificazione nella struttura dati oefos
            MetaUtility.getInstance().classifications_reader();

            //Crea Interfaccia dei metadatai
            this.setMetadataTab();
        } catch (Exception ex) {
            ResourceBundle bundle = ResourceBundle.getBundle(RESOURCES, currentLocale, Globals.loader);
             Globals.logger.info(Utility.getBundleString("ExceptionMetadata", bundle) + ex);
            JOptionPane.showMessageDialog(this, Utility.getBundleString("ExceptionMetadata", bundle) + "\n" + ex);
        }
    }

    /**
     * Crea interfaccia dei metadati a partire dal main_panel
     *
     * @throws Exception
     */
    private void setMetadataTab() throws Exception {
        ResourceBundle bundle = ResourceBundle.getBundle(RESOURCES, currentLocale, Globals.loader);
        
        Dimension iDim = new Dimension(840, 0);
        int p_x = 10;
        int p_y = 10;

        javax.swing.JScrollPane main_scroller = new javax.swing.JScrollPane();
        main_panel = new JPanel(new MigLayout());
        setName("main_panel");

        main_scroller.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        main_scroller.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        main_scroller.setPreferredSize(new java.awt.Dimension(100, 550));
        main_scroller.setViewportView(main_panel);
        main_scroller.setBorder(null);
        main_scroller.setBounds(5, 5, 750, 650);

        JPanel innerPanel = new JPanel(new MigLayout());
        innerPanel.setName("pannello000");
        JLabel label = new JLabel();
        label.setText(Utility.getBundleString("obblfield", bundle));
        label.setPreferredSize(new Dimension(690, 12));
        label.setHorizontalAlignment(SwingConstants.RIGHT);
        label.setFont(new Font("MS Sans Serif", Font.PLAIN, 12));
                
        main_panel.add(label, "wrap");
            
        //Metodo che si occupa della costruzione dell'interfaccia a partire dalle strutture dati
        MetaUtility.getInstance().create_metadata_view(metadata,  main_panel, 0);

        jLayeredPane1.add(main_scroller, "wrap");

        setCursor(null);
    }

    /**
     * Inserisce in una struttura dati solo i campi che si possono editare tra
     * tutti i campi di un JPanel
     *
     * @param panel
     */
    private void createComponentMap(JPanel panel) {
        Component[] components = panel.getComponents();

        for (int i = 0; i < components.length; i++) {
            if (components[i] instanceof javax.swing.JTextField) {
                componentMap.put(components[i].getName(), components[i]);
            }

            if (components[i] instanceof javax.swing.JTextArea) {
                componentMap.put(components[i].getName(), components[i]);
            }

            if (components[i] instanceof javax.swing.JComboBox) {
                componentMap.put(components[i].getName(), components[i]);
            }

            if (components[i] instanceof JXDatePicker) {
                componentMap.put(components[i].getName(), components[i]);
            }

            if (components[i] instanceof JTree) {
                componentMap.put(components[i].getName(), components[i]);
            }

            if (components[i] instanceof JLabel) {
                componentMap.put(components[i].getName(), components[i]);
            }

            if (components[i] instanceof javax.swing.JScrollPane) {
                if (components[i].getName() != null) {
                    if (components[i].getName().equals("langStringScroll")) {
                        JScrollPane scroller = (JScrollPane) components[i];
                        
                        Component[] textareas = scroller.getComponents();
                        for (int j = 0; j < textareas.length; j++) {
                            if (textareas[j] instanceof javax.swing.JViewport) {
                                JViewport viewport = (JViewport) textareas[j];
                                Component[] viewports = viewport.getComponents();
                                
                                for (int z = 0; z < viewports.length; z++) {
                                    if (viewports[z] instanceof JTextArea) {
                                        componentMap.put(viewports[z].getName(), viewports[z]);
                                    }
                                }
                            }
                        } 
                    } 
                }
            }
            
            if (components[i] instanceof javax.swing.JPanel) {
                if (components[i].getName() != null) {
                    if (components[i].getName().equals("ImPannelloClassif")) {
                        componentMap.put(components[i].getName(), components[i]);
                    }
                }

                this.createComponentMap((JPanel) components[i]);
            }
        }
    }

    /**
     * Restituisce il path del file scelto per l'esportazione o l'importazione
     * di un file pdf, Null se non viene scelto un file
     *
     * @param exp
     * @return
     */
    private String chooseFileImpExportPdf(boolean exp) {
        ResourceBundle bundle = ResourceBundle.getBundle(RESOURCES, currentLocale, Globals.loader);

        JFileChooser saveFile = new JFileChooser();//new save dialog  
        
        // Set the text
        saveFile.setDialogTitle(Utility.getBundleString("selectfolder", bundle));
        saveFile.setApproveButtonText(Utility.getBundleString("savepdf", bundle));
        saveFile.setApproveButtonMnemonic('a');
        saveFile.setApproveButtonToolTipText(Utility.getBundleString("tooltipsavepdf", bundle));
        
        saveFile.setLocale(currentLocale);
        saveFile.updateUI();
        
        FileFilter filter = new FileNameExtensionFilter("pdf", "pdf");
        saveFile.addChoosableFileFilter(filter);
        
        int ret = saveFile.showSaveDialog(BookImporter.getInstance());

        boolean selected = false;
        String pdfLocation = null;

        if (ret == JFileChooser.APPROVE_OPTION) {
            File f = saveFile.getSelectedFile();

            String filePath = f.getAbsolutePath();
            if (!filePath.toLowerCase().endsWith(".pdf")) {
                f = new File(filePath + ".pdf");
            }

            pdfLocation = f.getAbsolutePath();

            if (new File(pdfLocation).isFile() && exp) {
                Object[] options = {Utility.getBundleString("voc1", bundle), Utility.getBundleString("voc2", bundle)};
                int n = JOptionPane.showOptionDialog(this, Utility.getBundleString("fileexists", bundle),
                        Utility.getBundleString("fileexists", bundle),
                        JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

                if (n == JOptionPane.YES_OPTION) {
                    selected = true;
                }
            } else {
                selected = true;
            }
        }

        if (selected == true) {
            return pdfLocation;
        } else {
            return null;
        }
    }

    /**
     * Effettua il refresh di tutti i campi dell'interfaccia al momento della
     * scelta di una nuova lingua
     *
     * @param localizacion
     */
    private void updateLanguageLabel(Locale localizacion) {

        ResourceBundle bundle = ResourceBundle.getBundle(RESOURCES, localizacion, Globals.loader);
        jMenu2.setText(Utility.getBundleString("menuLoadingLabel", bundle) + " ");
        jMenu1.setText(Utility.getBundleString("menuSettingLabel", bundle) + " ");
        jMenu3.setText(Utility.getBundleString("menuImportLabel", bundle) + " ");
        jMenu4.setText(Utility.getBundleString("menuExportLabel", bundle) + " ");
        jMenu5.setText(Utility.getBundleString("menuHelpLabel", bundle) + " ");

        jCheckBox1.setText(Utility.getBundleString("bocravailable", bundle));


        jPanel2.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), Utility.getBundleString("bdatilibro", bundle), TitledBorder.LEFT, TitledBorder.TOP));
        jPanel3.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), Utility.getBundleString("banteprima", bundle), TitledBorder.LEFT, TitledBorder.TOP));

        jButton2.setText(Utility.getBundleString("bselect", bundle));

        jMenuItem1.setText(Utility.getBundleString("bimportmeta", bundle));
        jMenuItem2.setText(Utility.getBundleString("bimportbook", bundle));
        jMenuItem3.setText(Utility.getBundleString("bimportmeta", bundle));
        jMenuItem4.setText(Utility.getBundleString("bexportbook", bundle));
        jMenuItem5.setText(Utility.getBundleString("buploaddata", bundle));
        jMenuItem6.setText(Utility.getBundleString("bimportpdf", bundle));
        jMenuItem7.setText(Utility.getBundleString("bexportepub", bundle));
        jMenuItem8.setText(Utility.getBundleString("bimportbook", bundle));
        //jMenuItem9.setText(Utility.getBundleString("phcert"));
        jMenuItem10.setText(Utility.getBundleString("phinfo", bundle));
        //jMenuItem11.setText(Utility.getBundleString("phguida"));
        //jMenuItem12.setText(Utility.getBundleString("phconf"));

        jLabel1.setText(Utility.getBundleString("blabellocal", bundle));

        jTabbedPane2.setTitleAt(0, Utility.getBundleString("bpane2", bundle));
        jTabbedPane2.setTitleAt(1, Utility.getBundleString("bmetadati", bundle));

        int items = jMenu1.getItemCount();
        for (int z = 0; z < items; z++) {
            JCheckBoxMenuItem item = (JCheckBoxMenuItem) jMenu1.getItem(z);
            item.setText(Utility.getBundleString(item.getName(), bundle));
        }

        if (TYPEBOOK == Globals.COLLECTION) {
            jMenuItem8.setText(Utility.getBundleString("cimportcollection", bundle));
            jMenuItem2.setText(Utility.getBundleString("cimportcollection", bundle));
            jTabbedPane2.setTitleAt(0, Utility.getBundleString("cpane2", bundle));
        }
    }

    private void cleanUndoDir() {
        try {

            boolean exists = (new File(Globals.UNDO_DIR)).exists();
            if (!exists) {
                boolean success = (new File(Globals.UNDO_DIR)).mkdir();
                if (!success) {
                     Globals.logger.error("Impossibile creare directory: " + Globals.UNDO_DIR);
                }
                return;
            }

            File dir = new File(Globals.UNDO_DIR);
            File[] listOfFiles = dir.listFiles();

            if (listOfFiles.length != 0) {

                for (int i = 0; i < listOfFiles.length; i++) {
                    File file = new File(listOfFiles[i].getCanonicalPath());
                    file.delete();
                }

            }

        } catch (Exception ex) {
             Globals.logger.error(ex.getMessage());
        }
    }

    /*
     * private static void expandAll(XMLTree tree) { int row = 0; while (row <
     * tree.getRowCount()) { tree.expandRow(row); row++; } }
     */
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 32767));
        filler2 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 32767));
        jSplitPane1 = new javax.swing.JSplitPane();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTree2 = new javax.swing.JTree();
        jTabbedPane2 = new javax.swing.JTabbedPane();
        jLayeredPane2 = new javax.swing.JLayeredPane();
        jPanel2 = new javax.swing.JPanel();
        jCheckBox1 = new javax.swing.JCheckBox();
        jButton2 = new javax.swing.JButton();
        jTextField2 = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jLayeredPane1 = new javax.swing.JLayeredPane();
        jPanel1 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenu3 = new javax.swing.JMenu();
        jMenuItem6 = new javax.swing.JMenuItem();
        jMenuItem3 = new javax.swing.JMenuItem();
        jMenuItem8 = new javax.swing.JMenuItem();
        jMenu4 = new javax.swing.JMenu();
        jMenuItem4 = new javax.swing.JMenuItem();
        jMenuItem7 = new javax.swing.JMenuItem();
        jMenuItem1 = new javax.swing.JMenuItem();
        jMenuItem2 = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();
        jMenuItem5 = new javax.swing.JMenuItem();
        jMenu5 = new javax.swing.JMenu();
        jMenuItem10 = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jSplitPane1.setPreferredSize(new java.awt.Dimension(232, 414));

        jScrollPane1.setViewportView(jTree2);

        jSplitPane1.setLeftComponent(jScrollPane1);

        jTabbedPane2.setAutoscrolls(true);

        jLayeredPane2.setAutoscrolls(true);

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Dati estesi su libro"));

        jCheckBox1.setText("Dati OCR disponibili");

        jButton2.setText("Scegli");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jLabel1.setText("jLabel1");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(jCheckBox1)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, 126, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, 433, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 105, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, 24, Short.MAX_VALUE)
                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jButton2)
                        .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 18, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(jCheckBox1)
                .addContainerGap())
        );

        jPanel2.setBounds(10, 20, 710, 110);
        jLayeredPane2.add(jPanel2, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("Anteprima"));

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(116, 116, 116)
                .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 427, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(155, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap(32, Short.MAX_VALUE)
                .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 440, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jPanel3.setBounds(10, 150, 710, 510);
        jLayeredPane2.add(jPanel3, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jTabbedPane2.addTab("Sezioni e pagine", jLayeredPane2);

        jLayeredPane1.setAutoscrolls(true);

        jPanel1.setAutoscrolls(true);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
        );

        jPanel1.setBounds(20, 1000, 100, 100);
        jLayeredPane1.add(jPanel1, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jTabbedPane2.addTab("METADATI", jLayeredPane1);

        jSplitPane1.setRightComponent(jTabbedPane2);

        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel2.setText("Powered by: I.M. Technologies");

        jMenuBar1.setAlignmentY(0.5F);

        jMenu1.setText("Lingua");
        jMenuBar1.add(jMenu1);

        jMenu3.setText("Importa nel Phaidra Importer");
        jMenu3.setContentAreaFilled(false);
        jMenu3.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jMenu3.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        jMenuItem6.setText("Import PDF");
        jMenuItem6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem6ActionPerformed(evt);
            }
        });
        jMenu3.add(jMenuItem6);

        jMenuItem3.setText("Metadati in XML");
        jMenuItem3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem3ActionPerformed(evt);
            }
        });
        jMenu3.add(jMenuItem3);

        jMenuItem8.setText("Struttura del libro in XML");
        jMenuItem8.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem8ActionPerformed(evt);
            }
        });
        jMenu3.add(jMenuItem8);

        jMenuBar1.add(jMenu3);

        jMenu4.setText("Esporta nel Phaidra Importer");

        jMenuItem4.setText("Export PDF Book");
        jMenuItem4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem4ActionPerformed(evt);
            }
        });
        jMenu4.add(jMenuItem4);

        jMenuItem7.setText("Export Epub Book");
        jMenuItem7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem7ActionPerformed(evt);
            }
        });
        jMenu4.add(jMenuItem7);

        jMenuItem1.setText("Metadata");
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem1ActionPerformed(evt);
            }
        });
        jMenu4.add(jMenuItem1);

        jMenuItem2.setText("Bookstructure");
        jMenuItem2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem2ActionPerformed(evt);
            }
        });
        jMenu4.add(jMenuItem2);

        jMenuBar1.add(jMenu4);

        jMenu2.setText("Caricamento");
        jMenu2.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);

        jMenuItem5.setText("Upload Data");
        jMenuItem5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem5ActionPerformed(evt);
            }
        });
        jMenu2.add(jMenuItem5);

        jMenuBar1.add(jMenu2);

        jMenu5.setLabel("Aiuto");

        jMenuItem10.setText("Check for updates");
        jMenuItem10.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem10ActionPerformed(evt);
            }
        });
        jMenu5.add(jMenuItem10);

        jMenuBar1.add(jMenu5);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jSplitPane1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 1074, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(0, 1074, Short.MAX_VALUE)
                        .addComponent(filler1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(filler2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 299, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(109, 109, 109))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(478, 478, 478)
                        .addComponent(filler1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(219, 219, 219)
                        .addComponent(filler2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 13, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jSplitPane1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Gestisce l'esportazione dei metadati sul file uwmetadata.xml nella
     * cartella locale di lavoro
     *
     */
    private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem1ActionPerformed
        ResourceBundle bundle = ResourceBundle.getBundle(RESOURCES, currentLocale, Globals.loader);

        if (folderWritable) {
            componentMap = new HashMap<String, Component>();
            createComponentMap(main_panel);

            String xmlFile = selectedFolderSep + Globals.IMP_EXP_METADATA;

            String error = MetaUtility.getInstance().check_and_save_metadata(xmlFile,true);
            
            if (error.length() > 0) {
                JOptionPane.showMessageDialog(this, error);
            } else {
                JOptionPane.showMessageDialog(this, Utility.getBundleString("export4", bundle));
            }
        } else {
            JOptionPane.showMessageDialog(this, Utility.getBundleString("opnotpermitted", bundle));
        }
    }//GEN-LAST:event_jMenuItem1ActionPerformed

    private void jMenuItem2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem2ActionPerformed
        ResourceBundle bundle = ResourceBundle.getBundle(RESOURCES, currentLocale, Globals.loader);
        
        if (folderWritable) {
            XMLTree.exportBookStructureToFile(selectedFolderSep);
            String msg = (this.isBook())?"bstructexppre":"cstructexppre";
            JOptionPane.showMessageDialog(this, Utility.getBundleString(msg, bundle));  
        } else {
            JOptionPane.showMessageDialog(this, Utility.getBundleString("opnotpermitted", bundle));
        }
    }//GEN-LAST:event_jMenuItem2ActionPerformed

    /**
     * Gestisce l'import dei metadati da file
     *
     * @param evt
     */
    private void jMenuItem3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem3ActionPerformed
        importMetadata();
    }//GEN-LAST:event_jMenuItem3ActionPerformed

    /**
     * Gestisce la creazione di un documento pdf a partire dalle immagini
     * contenute nel bookstructure
     *
     * @param evt
     */
    private void jMenuItem4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem4ActionPerformed
        ResourceBundle bundle = ResourceBundle.getBundle(RESOURCES, currentLocale, Globals.loader);

        try {
            XMLTree.exportBookstructure(selectedFolderSep);

            String location = chooseFileImpExportPdf(true);

            if (location != null) {
                PdfCreateMonitor.createAndShowGUI(true, location);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, Utility.getBundleString("errorloadUwmetadataText", bundle) + ": " + ex.getMessage());
        }
    }//GEN-LAST:event_jMenuItem4ActionPerformed

    /**
     * Setta il path del pdf da uploadare nel caso del libro
     *
     * @param evt
     */
    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        ResourceBundle bundle = ResourceBundle.getBundle(RESOURCES, currentLocale, Globals.loader);
        
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setAcceptAllFileFilterUsed(false);
        
        // Set the text
        fileChooser.setDialogTitle(Utility.getBundleString("selectfolder", bundle));
        fileChooser.setApproveButtonText(Utility.getBundleString("select", bundle));
        fileChooser.setApproveButtonMnemonic('e');
        fileChooser.setApproveButtonToolTipText(Utility.getBundleString("tooltipselect", bundle));
        
        fileChooser.setLocale(currentLocale);
        fileChooser.updateUI();
        
        FileNameExtensionFilter filter = new FileNameExtensionFilter(".pdf", "pdf");
        fileChooser.setFileFilter(filter);

        if (fileChooser.showOpenDialog(BookImporter.this) == JFileChooser.APPROVE_OPTION) {
            jTextField2.setText(fileChooser.getSelectedFile().toString());
        } else {
            jTextField2.setText("");
        }
    }//GEN-LAST:event_jButton2ActionPerformed

    /**
     * Gestisce l'evento di upload del libro/collezione
     *
     * @param evt
     */
    private void jMenuItem5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem5ActionPerformed
        UploadSettings.getInstance(currentLocale).setVisible(true);
        this.setVisible(false);
    }//GEN-LAST:event_jMenuItem5ActionPerformed

    /**
     * Gestisce l'importazione di immagini nel bookstructure a partire da un
     * file pdf
     *
     * @param evt por
     */
    private void jMenuItem6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem6ActionPerformed
        ResourceBundle bundle = ResourceBundle.getBundle(RESOURCES, currentLocale, Globals.loader);
        
        try {
            if (folderWritable) {
                if (xmlTree.getSelectionPath() != null) {
                    XMLNode nd = (XMLNode) xmlTree.getSelectionPath().getLastPathComponent();

                    if (nd.getLevel() != 0 && nd.getLevel() != 1) {
                        XMLTree.exportBookstructure(selectedFolderSep);

                        String location = chooseFileImpExportPdf(false);

                        if (location != null) {
                            PdfCreateMonitor.createAndShowGUI(false, location);
                        }
                    } else {
                        String message = (this.isBook()) ? Utility.getBundleString("importtoroot", bundle) : Utility.getBundleString("cimporttoroot,bundle", bundle);
                        JOptionPane.showMessageDialog(this, message);
                    }
                } else {
                    JOptionPane.showMessageDialog(this, Utility.getBundleString("selectImportNode", bundle));
                }

                XMLTree.exportBookStructureToFile(selectedFolderSep);
            } else {
                JOptionPane.showMessageDialog(this, Utility.getBundleString("opnotpermitted", bundle));
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, Utility.getBundleString("errorloadUwmetadataText", bundle) + ": " + ex.getMessage());
        }
    }//GEN-LAST:event_jMenuItem6ActionPerformed

    /**
     * Gestisce l'esportazione in formato Epub del bookstructure
     *
     * @param evt
     */
    private void jMenuItem7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem7ActionPerformed
        Epub.createAndShowGUI();
    }//GEN-LAST:event_jMenuItem7ActionPerformed

    /**
     * Import BookStructure
     *
     * @param evt
     */
    private void jMenuItem8ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem8ActionPerformed
        ResourceBundle bundle = ResourceBundle.getBundle(RESOURCES, currentLocale, Globals.loader);
        if (folderWritable) {
            initializeXmlTree(true,false);
            cleanUndoDir();
        } 
        else {
            JOptionPane.showMessageDialog(this, Utility.getBundleString("opnotpermitted", bundle));
        }
    }//GEN-LAST:event_jMenuItem8ActionPerformed

    private void jMenuItem10ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem10ActionPerformed
        new About().setVisible(true);
    }//GEN-LAST:event_jMenuItem10ActionPerformed

    private void jMenu3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenu3ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jMenu3ActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /*
         * Set the Nimbus look and feel
         */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /*
         * If Nimbus (introduced in Java SE 6) is not available, stay with the
         * default look and feel. For details see
         * http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(BookImporter.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(BookImporter.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(BookImporter.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(BookImporter.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /*
         * Create and display the form
         */
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                getInstance();



                /*
                 * JFrame fm = new JFrame("System file properties of tree ");
                 *
                 * Properties p = System.getProperties(); JTree tree = new
                 * JTree(p); tree.setRootVisible(true); JScrollPane scrollpane =
                 * new JScrollPane(tree); fm.getContentPane().add(scrollpane,
                 * "Center"); fm.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                 * fm.setSize(450, 400); fm.setVisible(true);
                 */
            }
        });
    }
    private javax.swing.JPanel main_panel;
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.Box.Filler filler1;
    private javax.swing.Box.Filler filler2;
    private javax.swing.JButton jButton2;
    private javax.swing.JCheckBox jCheckBox1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLayeredPane jLayeredPane1;
    private javax.swing.JLayeredPane jLayeredPane2;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenu jMenu3;
    private javax.swing.JMenu jMenu4;
    private javax.swing.JMenu jMenu5;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem10;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JMenuItem jMenuItem3;
    private javax.swing.JMenuItem jMenuItem4;
    private javax.swing.JMenuItem jMenuItem5;
    private javax.swing.JMenuItem jMenuItem6;
    private javax.swing.JMenuItem jMenuItem7;
    private javax.swing.JMenuItem jMenuItem8;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JTabbedPane jTabbedPane2;
    private javax.swing.JTextField jTextField2;
    private javax.swing.JTree jTree2;
    // End of variables declaration//GEN-END:variables
}
