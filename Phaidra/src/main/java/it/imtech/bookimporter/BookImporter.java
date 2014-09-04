package it.imtech.bookimporter;

import it.imtech.about.About;
import it.imtech.globals.ConfirmDialog;
import it.imtech.globals.Globals;
import it.imtech.metadata.MetaUtility;
import it.imtech.metadata.Metadata;
import it.imtech.pdfepub.Epub;
import it.imtech.pdfepub.PdfCreateMonitor;
import it.imtech.upload.UploadSettings;
import it.imtech.utility.Language;
import it.imtech.utility.Utility;
import it.imtech.xmltree.XMLNode;
import it.imtech.xmltree.XMLTree;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.*;
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
import org.apache.log4j.Logger;
import org.jdesktop.swingx.JXDatePicker;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Classe principale di gestione del frame PhaidraImporter
 *
 * @author Medelin Mauro
 */
public class BookImporter extends javax.swing.JFrame {
    //Gestore dei log
    public final static Logger logger = Logger.getLogger(BookImporter.class);
    
    //Istanza della classe SingleTon BookImporter
    private static BookImporter instance = null;

    //Contiene la mappatura del file dei metadati preso da URL
    protected TreeMap<Object, Metadata> metadata = new TreeMap<Object, Metadata>();
    
    //Contiene una mappatura gli elementi dell'interfaccia dei metadati con i quali l'utente puo interagire
    protected HashMap componentMap;
   
    //Oggetto che identifica l'albero del libro/collezione
    public static XMLTree xmlTree;
    
    //Mappatura del file file di configurazione delle classificazioni contenuto nella cartella di output del libro
    public static XMLConfiguration classifConf = null;
     
    private JLabel jLabel1 = new JLabel();
    private JLabel jLabel3 = new JLabel();
  
    public JPanel book_panel = new JPanel();    
    private JPanel jPanel3 = new JPanel();
    
    private JTextField jTextField2 = new JTextField();
    private JCheckBox jCheckBox1 = new JCheckBox();
    
    private JButton jButton2 = new JButton();
    
    //public static Locale localConst = null;
    /**
     * Metodo di gestione del Singleton
     *
     * @return
     */
    public static BookImporter getInstance() {
        if (instance == null) {
            instance = new BookImporter();
            
            if (new File(Globals.SELECTED_FOLDER_SEP + Globals.IMP_EXP_METADATA).isFile()) {
                instance.importMetadata();
            }
        }
        
        return instance;
    }

    /**
     * Costruttore della classe BookImporter
     */
    public BookImporter() {
        try {
            initComponents();

            ResourceBundle bundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);
            this.setTitle("Phaidra Importer v." + Globals.CURRENT_VERSION);
            jTextField2.setText("");
            
            //Crea pannello frontale
            createFrontalPane();
            jTextField2.setText("");
            
            this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        
            this.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                    ResourceBundle bundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);
                    String title = Utility.getBundleString("dialog_1_title", bundle);
                    String text = Utility.getBundleString("dialog_1", bundle);

                    ConfirmDialog confirm = new ConfirmDialog(null, true, title, text, "test");

                    confirm.setVisible(true);
                    boolean close = confirm.getChoice();
                    confirm.dispose();

                    if (close == true){
                        dispose();
                    }
                }
            });
            
            //Crea Menu delle lingue
            createLanguageMenu(bundle);
            
            //Aggiorna tutti i label in base alla lingua di default
            updateLanguageLabel(Globals.CURRENT_LOCALE);
            
            //Inizializzazione classificazione e vocabolario    
            MetaUtility.getInstance().preInitializeData();
            
            //Inizializzazione dell'albero di struttura
            boolean fromFile = askForStructure();
            initializeXmlTree(fromFile, false);
            
            //Creazione interfaccia metadati
            initializeData();
            
            //Posizionamento interfaccia
            Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
            int x = (dim.width - getSize().width) / 2;
            int y = (dim.height - getSize().height) / 2;
            setLocation(x, y);
            setVisible(true);
        } catch (ConfigurationException ex) {
            logger.error(ex.getMessage());
            JOptionPane.showMessageDialog(new Frame(), ex.getMessage());
            System.exit(0);
        } catch (Exception ex) {
            logger.error(ex.getMessage());
            JOptionPane.showMessageDialog(new Frame(), ex.getMessage());
            System.exit(0);
        }
    }
    
    public void createFrontalPane(){
        jLayeredPane2.removeAll();
        MigLayout miglayout = new MigLayout(); 
               
        jPanel3.setLayout(new BorderLayout());
        jLabel3.setMinimumSize(new Dimension(300, 540));
        jLabel3.setHorizontalAlignment(SwingConstants.CENTER);
        jPanel3.add(BorderLayout.CENTER, jLabel3);
        
        if (Globals.TYPE_BOOK == Globals.BOOK){
            MigLayout miglayout_test = new MigLayout("fillx, insets 10 10 10 10"); 
               
            jLayeredPane2.setLayout(miglayout_test);
                
            jButton2.addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent event)
                {
                   manageSelectFolder();
                }
            });

            book_panel.setLayout(miglayout);
           
            book_panel.add(jLabel1);
            book_panel.add(jTextField2, "width 100:450:450");
            book_panel.add(jButton2);
            book_panel.add(jCheckBox1, "gapleft 50");
            
            jLayeredPane2.add(book_panel, "wrap, grow, height 60:60:60");
            jLayeredPane2.add(jPanel3, "wrap, grow, height 100:600:600, align center");
        }
        else{
            MigLayout path_layout = new MigLayout("fillx, inset 10 10 10 10"); 
            jLayeredPane2.setLayout(path_layout);
        
            jLayeredPane2.add(jPanel3, "h 100%, align center, growx");
        }
        
        jLayeredPane2.validate();
        jLayeredPane2.repaint();
    }
    
    public TreeMap<Object, Metadata> getMetadata(){
        return metadata;
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
     * Resetta i componenti dell'interfaccia dei metadati
     */
    public void createComponentMap() {
        componentMap = new HashMap<String, Component>();
        createComponentMap(main_panel);
    }

    public void initializeXmlTree(boolean fromFile, boolean refresh) {
        xmlTree = new XMLTree(Globals.SELECTED_FOLDER_SEP, fromFile, false, refresh);

        if (XMLTree.getRoot() == null) {
            logger.info("Errore inizializzazione file bookstructure");
            JOptionPane.showMessageDialog(new Frame(), "Errore inizializzazione file bookstructure");
            System.exit(0);
        }
        
        setViewXmltree(xmlTree);
    }

    public void initializeUndoXmlTree(String file) {
        xmlTree = new XMLTree(file, true, true,false);

        if (XMLTree.getRoot() == null) {
            logger.info("Errore inizializzazione file bookstructure");
            JOptionPane.showMessageDialog(new Frame(), "Errore inizializzazione file bookstructure");
            System.exit(0);
        }

        setViewXmltree(xmlTree);
    }

    public void setViewXmltree(XMLTree xmlTree) {
        XMLTree.expandAll(xmlTree);
        jScrollPane1.setViewportView(xmlTree);
    }
    
    
    protected void importMetadataSilent(String xmlFile){
        ResourceBundle bundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);
        try {
            //Leggi il file uwmetadata.xml
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            MetaUtility.getInstance().read_uwmetadata(xmlFile);

            jLayeredPane1.removeAll();
            jLayeredPane1.revalidate();

            //Ridisegna l'interfaccia
            this.setMetadataTab();
            setCursor(null);

            //JOptionPane.showMessageDialog(this, Utility.getBundleString("import4", bundle));
        } catch (Exception ex) {
            setCursor(null);
            JOptionPane.showMessageDialog(this, Utility.getBundleString("errorloadUwmetadataText", bundle) + ": " + ex.getMessage());
        }
    }
	
    /**
     * Effettua il reload dell'interfaccia dinamica dei metadati in base al file
     * uwmetadata.xml contenuto nella cartella di lavoro corrente.
     *
     */
    protected void importMetadata() {
        ResourceBundle bundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);

        try {
            if (new File(Globals.SELECTED_FOLDER_SEP + Globals.IMP_EXP_METADATA).isFile()) {
                Object[] options = {Utility.getBundleString("voc1", bundle), Utility.getBundleString("voc2", bundle)};
                int n = JOptionPane.showOptionDialog(this, Utility.getBundleString("loadUwmetadataText", bundle),
                        Utility.getBundleString("loadUwmetadata", bundle),
                        JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

                if (n == JOptionPane.YES_OPTION) {
                    //Leggi il file uwmetadata.xml
                    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    MetaUtility.getInstance().read_uwmetadata(Globals.SELECTED_FOLDER_SEP + Globals.IMP_EXP_METADATA);

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
        
        if(new File(Globals.SELECTED_FOLDER_SEP+Globals.IMP_EXP_BOOK).isFile()){
            ResourceBundle bundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);

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
    
    private TreeMap<String, String> getOrderedLanguages(ResourceBundle bundle){
        TreeMap<String, String> ordered_res = new TreeMap<String, String>();
        String lang;
        String code;
        
        for (Language LANGUAGE : Globals.LANGUAGES) {
            lang = Utility.getBundleString(LANGUAGE.getKey(), bundle);
            code = LANGUAGE.getCode();
            ordered_res.put(lang,code);  
        }
        
        return ordered_res;
    }
    
    /**
     * Crea dinamicamente il menu delle lingue a partire dal file di
     * configurazione aggiunge gli eventi agli Items del menu e setta la lingua
     * di default
     *
     * @param bundle
     */
    protected final void createLanguageMenu(ResourceBundle bundle) {
        try {
            jMenu1.removeAll();
            TreeMap<String, String> resources = getOrderedLanguages(bundle);
            
            for(String key: resources.keySet()){
                final String language = resources.get(key);
                final Locale local = new Locale(language);

                //Crea un nuovo menu item
                final JCheckBoxMenuItem lang = new JCheckBoxMenuItem();
                lang.setName(key);
                lang.setText(Utility.getBundleString(key, bundle));
                lang.setToolTipText(language);
                
                //Associo l'evento al nuovo menu item
                lang.addActionListener(new java.awt.event.ActionListener() {
                    public void actionPerformed(java.awt.event.ActionEvent evt) {
                        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                        Globals.CURRENT_LOCALE = local;
                        Locale.setDefault(Globals.CURRENT_LOCALE);
                        
                        ResourceBundle bundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);
                        xmlTree.updateLanguage();
                        createLanguageMenu(bundle);
                        setCursor(null);
                    }
                });

                //Aggiungo il sottomenu e lo attivo in caso di lingua corrente
                jMenu1.add(lang);
                if (language.equals(bundle.getLocale().getLanguage())) {
                    try {
                        String backup_metadata = Globals.USER_DIR+"metadata_backup.xml";
                                                
                        updateLanguageLabel(local);

                        if (instance != null) {
                           setDefaultCurrent();
                           boolean exported = this.exportMetadataSilent(backup_metadata);
                           
                           jLayeredPane1.removeAll();
                           MetaUtility.getInstance().preInitializeData();
                           initializeData();
                           if(exported){
                               this.importMetadataSilent(backup_metadata);
                           }
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
                    }   
                    catch (ConfigurationException ex) {
                        logger.error(ex.getMessage());
                        JOptionPane.showMessageDialog(new Frame(), ex.getMessage());
                    } 
                     catch (Exception ex) {
                        logger.error(ex.getMessage());
                        JOptionPane.showMessageDialog(new Frame(), ex.getMessage());
                    } 
                }
            }
        } catch (HeadlessException ex) {
            logger.error(ex.getMessage());
        }
    }


    /**
     * Setta la lingua corrente come lingua di default nel file di
     * configurazione
     *
     * @throws ConfigurationException
     */
    private void setDefaultCurrent() throws ConfigurationException {
        ResourceBundle bundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);

        Object[] options = {Utility.getBundleString("voc1", bundle), Utility.getBundleString("voc2", bundle)};
        int n = JOptionPane.showOptionDialog(this, Utility.getBundleString("setasdefaultlang", bundle),
                Utility.getBundleString("setasdefaultlangtitle", bundle),
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

        if (n == JOptionPane.YES_OPTION) {
            Utility.setDefaultLangCurrent();
        }
    }

    /**
     * Crea un file nella cartella di lavoro che definisce la classificazione scelta
     */
    private void setDefaultClassificationFile() {
        try {
            if (!new File(Globals.SELECTED_FOLDER_SEP + Globals.CLASSIF_CONFIG).isFile()) {
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

                it.imtech.xmltree.XMLUtil.xmlWriter(doc, Globals.SELECTED_FOLDER_SEP + Globals.CLASSIF_CONFIG);
            }

            classifConf = new XMLConfiguration(Globals.SELECTED_FOLDER_SEP + Globals.CLASSIF_CONFIG);

        } catch (TransformerConfigurationException ex) {
             logger.error(ex.getMessage());
        } catch (ParserConfigurationException ex) {
             logger.error(ex.getMessage());
        } catch (ConfigurationException ex) {
             logger.error(ex.getMessage());
        }
    }

    /**
     * Setta tutte le strutture dati necessarie per la creazione
     * dell'interfaccia dei metadati
     *
     */
    private void initializeData() {
        //Scelgo i metadati
        Globals.URL_METADATA = (Globals.TYPE_BOOK == Globals.COLLECTION) ? Globals.URL_METADATA_COLL : Globals.URL_METADATA;

        try {
            //Inserisce tutti i campi dinamici visibili nella struttura dati metadata
            this.metadata = MetaUtility.getInstance().metadata_reader();

            //Setta il file di classificazione scelto
            if(Globals.FOLDER_WRITABLE)
                setDefaultClassificationFile();

            //Inserisce tutte le classificazione nella struttura dati oefos
            MetaUtility.getInstance().classifications_reader();

            //Crea Interfaccia dei metadatai
            this.setMetadataTab();
        } catch (Exception ex) {
            ResourceBundle bundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);
            logger.info(Utility.getBundleString("ExceptionMetadata", bundle) + ex);
            JOptionPane.showMessageDialog(this, Utility.getBundleString("ExceptionMetadata", bundle) + "\n" + ex);
        }
    }

    /**
     * Crea interfaccia dei metadati a partire dal main_panel
     *
     * @throws Exception
     */
    private void setMetadataTab() throws Exception {
        ResourceBundle bundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);
        
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
        ResourceBundle bundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);

        JFileChooser saveFile = new JFileChooser();//new save dialog  
        
        // Set the text
        saveFile.setDialogTitle(Utility.getBundleString("selectfolder", bundle));
        saveFile.setApproveButtonText(Utility.getBundleString("savepdf", bundle));
        saveFile.setApproveButtonMnemonic('a');
        saveFile.setApproveButtonToolTipText(Utility.getBundleString("tooltipsavepdf", bundle));
        
        saveFile.setLocale(Globals.CURRENT_LOCALE);
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

        ResourceBundle bundle = ResourceBundle.getBundle(Globals.RESOURCES, localizacion, Globals.loader);
        jMenu2.setText(Utility.getBundleString("menuLoadingLabel", bundle) + " ");
        jMenu1.setText(Utility.getBundleString("menuSettingLabel", bundle) + " ");
        jMenu3.setText(Utility.getBundleString("menuImportLabel", bundle) + " ");
        jMenu4.setText(Utility.getBundleString("menuExportLabel", bundle) + " ");
        jMenu6.setText(Utility.getBundleString("menuFileLabel", bundle) + " ");

        jCheckBox1.setText(Utility.getBundleString("bocravailable", bundle));


        book_panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), Utility.getBundleString("bdatilibro", bundle), TitledBorder.LEFT, TitledBorder.TOP));
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
        jMenuItem9.setText(Utility.getBundleString("bexit", bundle));       
        jMenuItem10.setText(Utility.getBundleString("phinfo", bundle));

        jLabel1.setText(Utility.getBundleString("blabellocal", bundle));

        jTabbedPane2.setTitleAt(0, Utility.getBundleString("bpane2", bundle));
        jTabbedPane2.setTitleAt(1, Utility.getBundleString("bmetadati", bundle));

        int items = jMenu1.getItemCount();
        for (int z = 0; z < items; z++) {
            JCheckBoxMenuItem item = (JCheckBoxMenuItem) jMenu1.getItem(z);
            item.setText(Utility.getBundleString(item.getName(), bundle));
        }

        if (Globals.TYPE_BOOK == Globals.COLLECTION) {
            jMenuItem8.setText(Utility.getBundleString("cimportcollection", bundle));
            jMenuItem2.setText(Utility.getBundleString("cimportcollection", bundle));
            jTabbedPane2.setTitleAt(0, Utility.getBundleString("cpane2", bundle));
            jMenuItem4.setVisible(false);
            jMenuItem7.setVisible(false);
        }        
    }

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
        jLayeredPane1 = new javax.swing.JLayeredPane();
        jPanel1 = new javax.swing.JPanel();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu6 = new javax.swing.JMenu();
        jMenuItem10 = new javax.swing.JMenuItem();
        jMenuItem9 = new javax.swing.JMenuItem();
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

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jSplitPane1.setPreferredSize(new java.awt.Dimension(232, 414));

        jTree2.setMaximumSize(new java.awt.Dimension(300, 76));
        jTree2.setMinimumSize(new java.awt.Dimension(300, 76));
        jTree2.setPreferredSize(new java.awt.Dimension(300, 76));
        jScrollPane1.setViewportView(jTree2);

        jSplitPane1.setLeftComponent(jScrollPane1);

        jTabbedPane2.setAutoscrolls(true);
        jTabbedPane2.setMaximumSize(new java.awt.Dimension(1000, 32767));
        jTabbedPane2.setMinimumSize(new java.awt.Dimension(500, 72));
        jTabbedPane2.setPreferredSize(new java.awt.Dimension(500, 72));

        jLayeredPane2.setAutoscrolls(true);
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

        jLayeredPane1.add(jPanel1);
        jPanel1.setBounds(20, 1000, 100, 100);

        jTabbedPane2.addTab("METADATI", jLayeredPane1);

        jSplitPane1.setRightComponent(jTabbedPane2);

        jMenuBar1.setAlignmentY(0.5F);

        jMenu6.setText("File");

        jMenuItem10.setText("Check for updates");
        jMenuItem10.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem10ActionPerformed(evt);
            }
        });
        jMenu6.add(jMenuItem10);

        jMenuItem9.setText("Exit");
        jMenuItem9.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem9ActionPerformed(evt);
            }
        });
        jMenu6.add(jMenuItem9);

        jMenuBar1.add(jMenu6);

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

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jSplitPane1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 1119, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(0, 1119, Short.MAX_VALUE)
                        .addComponent(filler1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(filler2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
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
                        .addGap(0, 33, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jSplitPane1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void manageSelectFolder() {                                         
        ResourceBundle bundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);
        
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setAcceptAllFileFilterUsed(false);
        
        // Set the text
        fileChooser.setDialogTitle(Utility.getBundleString("selectfolder", bundle));
        fileChooser.setApproveButtonText(Utility.getBundleString("select", bundle));
        fileChooser.setApproveButtonMnemonic('e');
        fileChooser.setApproveButtonToolTipText(Utility.getBundleString("tooltipselect", bundle));
        
        fileChooser.setLocale(Globals.CURRENT_LOCALE);
        fileChooser.updateUI();
        
        FileNameExtensionFilter filter = new FileNameExtensionFilter(".pdf", "pdf");
        fileChooser.setFileFilter(filter);

        if (fileChooser.showOpenDialog(BookImporter.this) == JFileChooser.APPROVE_OPTION) {
            jTextField2.setText(fileChooser.getSelectedFile().toString());
        } else {
            jTextField2.setText("");
        }
    }                          
    
    private boolean exportMetadataSilent(String xmlFile){
        componentMap = new HashMap<String, Component>();
        createComponentMap(main_panel);
        
        String error = MetaUtility.getInstance().check_and_save_metadata(xmlFile, true, false);
        
        if (error.length() > 0) {
            return false;
        } 
        else{
            return true;
        }
    }
    
    
    private boolean exportMetadata(){
        boolean result = false;
        
        ResourceBundle bundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);
        if (Globals.FOLDER_WRITABLE) {
            componentMap = new HashMap<String, Component>();
            createComponentMap(main_panel);

            String xmlFile = Globals.SELECTED_FOLDER_SEP + Globals.IMP_EXP_METADATA;
          
            String error = MetaUtility.getInstance().check_and_save_metadata(xmlFile,true, true);
           
            if (error.length() > 0) {
                JOptionPane.showMessageDialog(this, error);
            } 
            else {
                JOptionPane.showMessageDialog(this, Utility.getBundleString("export4", bundle));
                result = true;
            }
        } else {
            JOptionPane.showMessageDialog(this, Utility.getBundleString("opnotpermitted", bundle));
        }
        return result;
    }
    
    /**
     * Gestisce l'esportazione dei metadati sul file uwmetadata.xml nella
     * cartella locale di lavoro
     *
     */
    private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem1ActionPerformed
        exportMetadata();
    }//GEN-LAST:event_jMenuItem1ActionPerformed

    private void jMenuItem2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem2ActionPerformed
        ResourceBundle bundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);
        
        if (Globals.FOLDER_WRITABLE) {
            XMLTree.exportBookStructureToFile(Globals.SELECTED_FOLDER_SEP);
            String msg = (Globals.TYPE_BOOK == Globals.BOOK)?"bstructexppre":"cstructexppre";
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
        ResourceBundle bundle = ResourceBundle.getBundle(Globals.RESOURCES,Globals.CURRENT_LOCALE, Globals.loader);

        try {
            XMLTree.exportBookstructure(Globals.SELECTED_FOLDER_SEP);

            String location = chooseFileImpExportPdf(true);

            if (location != null) {
                PdfCreateMonitor.createAndShowGUI(true, location);
                jTextField2.setText(location);
                
                
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, Utility.getBundleString("errorloadUwmetadataText", bundle) + ": " + ex.getMessage());
        }
    }//GEN-LAST:event_jMenuItem4ActionPerformed

    /**
     * Gestisce l'evento di upload del libro/collezione
     *
     * @param evt
     */
    private void jMenuItem5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem5ActionPerformed
        UploadSettings.getInstance(Globals.CURRENT_LOCALE).setVisible(true);
        //this.setVisible(false);
    }//GEN-LAST:event_jMenuItem5ActionPerformed

    /**
     * Gestisce l'importazione di immagini nel bookstructure a partire da un
     * file pdf
     *
     * @param evt por
     */
    private void jMenuItem6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem6ActionPerformed
        ResourceBundle bundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);
        
        try {
            if (Globals.FOLDER_WRITABLE) {
                if (xmlTree.getSelectionPath() != null) {
                    XMLNode nd = (XMLNode) xmlTree.getSelectionPath().getLastPathComponent();

                    if (nd.getLevel() != 0 && nd.getLevel() != 1) {
                        XMLTree.exportBookstructure(Globals.SELECTED_FOLDER_SEP);

                        String location = chooseFileImpExportPdf(false);

                        if (location != null) {
                            PdfCreateMonitor.createAndShowGUI(false, location);
                        }
                    } else {
                        String message = (Globals.TYPE_BOOK == Globals.BOOK) ? Utility.getBundleString("importtoroot", bundle) : Utility.getBundleString("cimporttoroot",bundle);
                        JOptionPane.showMessageDialog(this, message);
                    }
                } else {
                    JOptionPane.showMessageDialog(this, Utility.getBundleString("selectImportNode", bundle));
                }

                XMLTree.exportBookStructureToFile(Globals.SELECTED_FOLDER_SEP);
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
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        ResourceBundle bundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);
        if (Globals.FOLDER_WRITABLE) {
            initializeXmlTree(true,false);
            it.imtech.utility.Utility.cleanUndoDir();
        } 
        else {
            JOptionPane.showMessageDialog(this, Utility.getBundleString("opnotpermitted", bundle));
        }
        setCursor(null);
    }//GEN-LAST:event_jMenuItem8ActionPerformed

    private void jMenuItem10ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem10ActionPerformed
        new About().setVisible(true);
    }//GEN-LAST:event_jMenuItem10ActionPerformed

    private void jMenu3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenu3ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jMenu3ActionPerformed

    private void jMenuItem9ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem9ActionPerformed
        ResourceBundle bundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);
        String title = Utility.getBundleString("dialog_1_title", bundle);
        String text = Utility.getBundleString("dialog_1", bundle);

        ConfirmDialog confirm = new ConfirmDialog(this, true, title, text, "test");

        confirm.setVisible(true);
        boolean close = confirm.getChoice();
        confirm.dispose();

        if (close == true){
            this.dispose();
        }
    }//GEN-LAST:event_jMenuItem9ActionPerformed

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
    private javax.swing.JLayeredPane jLayeredPane1;
    private javax.swing.JLayeredPane jLayeredPane2;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenu jMenu3;
    private javax.swing.JMenu jMenu4;
    private javax.swing.JMenu jMenu6;
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
    private javax.swing.JMenuItem jMenuItem9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JTabbedPane jTabbedPane2;
    private javax.swing.JTree jTree2;
    // End of variables declaration//GEN-END:variables
}
