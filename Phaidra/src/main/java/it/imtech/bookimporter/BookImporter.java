package it.imtech.bookimporter;

import com.toedter.calendar.JDateChooser;
import it.imtech.about.About;
import it.imtech.configuration.StartWizard;
import it.imtech.dialogs.AlertDialog;
import it.imtech.dialogs.ConfirmDialog;
import it.imtech.dialogs.InputDialog;
import it.imtech.dialogs.TemplateDialog;
import it.imtech.globals.Globals;
import it.imtech.helper.Helper;
import it.imtech.metadata.MetaPanels;
import it.imtech.metadata.MetaUtility;
import static it.imtech.metadata.MetaUtility.logger;
import it.imtech.metadata.Metadata;
import it.imtech.metadata.Template;
import it.imtech.metadata.TemplatesUtility;
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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.util.*;
import java.util.logging.Level;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.jdesktop.swingx.JXDatePicker;
import static org.jpedal.examples.viewer.Viewer.file;

/**
 * Classe principale di gestione del frame PhaidraImporter
 *
 * @author Medelin Mauro
 */
public class BookImporter extends javax.swing.JFrame {
    //Gestore dei log
    private final static Logger logger = Logger.getLogger(BookImporter.class);
    
    //Istanza della classe SingleTon BookImporter
    private static BookImporter instance = null;

    //Contiene la mappatura del file dei metadati preso da URL
    public TreeMap<Object, Metadata> metadata = new TreeMap<Object, Metadata>();
    
    //Contiene una mappatura gli elementi dell'interfaccia dei metadati con i quali l'utente puo interagire
    protected HashMap componentMap;
   
    //Oggetto che identifica l'albero del libro/collezione
    public static XMLTree xmlTree;
     
    private JLabel jLabel1 = new JLabel();
    private JLabel jLabel3 = new JLabel();
  
    public JPanel book_panel = new JPanel();    
    private JPanel jPanel3 = new JPanel();
    
    private JTextField jTextField2 = new JTextField();
    private JCheckBox jCheckBox1 = new JCheckBox();
    
    private JButton jButton2 = new JButton();
    
    public TreeMap<String, MetaPanels> metadatapanels = new TreeMap<String, MetaPanels>(); 
    
    public static IndexedFocusTraversalPolicy policy;
    
    public static String mainpanel = "uwmetadata.xml";
    
    private  void manageSingleCollectionMetadataFiles(){
        ResourceBundle bundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);
            
        ArrayList<String> metadatafiles = XMLTree.getSingleMetadataFiles();
                    
        if(metadatafiles.size()>0){
            String text = Utility.getBundleString("singlemetadata1", bundle);
            String title= Utility.getBundleString("singlemetadata1title", bundle);
            ConfirmDialog confirm = new ConfirmDialog(this, true, title, text, 
                                                      Utility.getBundleString("confirm", bundle),
                                                      Utility.getBundleString("annulla", bundle));

            confirm.setVisible(true);
            boolean close = confirm.getChoice();
            confirm.dispose();

            if (close == true){
                for (String metadatafile : metadatafiles) {
                    try {
                        File metadatasingle = new File(metadatafile);
                        File sessionmeta = new File(Globals.DUPLICATION_FOLDER_SEP + "session" + metadatasingle.getName());
                        FileUtils.copyFile(new File(Globals.BACKUP_METADATA), sessionmeta);
                        importSingleMetadata(metadatafile, "Test", true, true);
                    }catch (IOException ex) {
                        logger.error(ex.getMessage());
                    }
                }                
            }
        }
    }
    
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
                instance.importMetadata(Globals.SELECTED_FOLDER_SEP + Globals.IMP_EXP_METADATA, mainpanel, true, true, false);                
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
            
            //Create frontal panel
            createFrontalPane();
            
            jTextField2.setText("");
            
            
            this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        
            this.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                    ResourceBundle bundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);
                    String title = Utility.getBundleString("dialog_1_title", bundle);
                    String text = Utility.getBundleString("dialog_1", bundle);

                    ConfirmDialog confirm = new ConfirmDialog(BookImporter.getInstance(), true, title, text, Utility.getBundleString("confirm", bundle),Utility.getBundleString("annulla", bundle));

                    confirm.setVisible(true);
                    boolean close = confirm.getChoice();
                    confirm.dispose();

                    if (close == true){
                        dispose();
                        System.exit(0);
                    }
                }
            });
            
            //Crea Menu delle lingue
            createLanguageMenu(bundle);
            
            //Aggiorna tutti i label in base alla lingua di default
            updateLanguageLabel(Globals.CURRENT_LOCALE);
            
            
            
            //Creazione interfaccia metadati
            if (Globals.TYPE_BOOK == Globals.COLLECTION){
                Utility.makeBackupMetadata(Globals.URL_METADATA_COLL, Globals.SESSION_METADATA);
            }
            else{
                Utility.makeBackupMetadata(Globals.URL_METADATA, Globals.SESSION_METADATA);
            }
            
            //Inizializzazione dell'albero di struttura
            boolean fromFile = askForStructure();
            
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            
            initializeXmlTree(fromFile, false);
            
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            
            MetaUtility.getInstance().setClassificationChoice();
            
            initializeData("");
            
            setCursor(null);
            
            //Posizionamento interfaccia
            Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
            int x = (dim.width - getSize().width) / 2;
            int y = (dim.height - getSize().height) / 2;
            setLocation(x, y);
            setVisible(true);
        }  catch (Exception ex) {
            logger.error(ex.getMessage());
            JOptionPane.showMessageDialog(new Frame(), ex.getMessage());
            System.exit(0);
        }
    }
       
    TreeMap<String, JComboBox> templatelists = new TreeMap<String, JComboBox>();
    TreeMap<String, JButton> templatebuttons = new TreeMap<String, JButton>();
     
    public void redrawTemplatePanels(){
        ArrayList<Template> combolist = new ArrayList<Template>();
        TreeMap<String, String> templates = TemplatesUtility.getTemplatesList();
        Template[] combo = null;
        
        if (!templates.isEmpty()){
            for(Map.Entry<String, String> entry : templates.entrySet()) {
                combolist.add(new Template(entry.getKey(), entry.getValue()));
            }  
        }
        
        if (!templates.isEmpty()){
            combo = combolist.toArray(new Template[combolist.size()]);
        }
        
        for(Map.Entry<String, JComboBox> combos: this.templatelists.entrySet()){
            combos.getValue().removeAllItems();
            combos.getValue().setName("IMTemplateCombo");
            if(!templates.isEmpty()){
                combos.getValue().setModel(new javax.swing.DefaultComboBoxModel(combo));
                combos.getValue().setSelectedItem(combo[0]);
                combos.getValue().setMinimumSize(new Dimension(250,20));
                combos.getValue().setEnabled(true);
                
                this.templatebuttons.get(combos.getKey()+"_import").setEnabled(true);
                this.templatebuttons.get(combos.getKey()+"_delete").setEnabled(true);
                
                //AddImportHandler(combos.getKey());
            }
            else{
                combos.getValue().setEnabled(false);
                this.templatebuttons.get(combos.getKey()+"_import").setEnabled(false);
                this.templatebuttons.get(combos.getKey()+"_delete").setEnabled(false);
            }
            
            combos.getValue().repaint();
            combos.getValue().revalidate();
        }
    }
    
    public void AddImportHandler(final String panelname) {
        if (this.templatebuttons.get(panelname+"_import") != null){

            this.templatebuttons.get(panelname+"_import").addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent event)
                {
                    ResourceBundle bundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);
                    String text = Utility.getBundleString("templateimporttext", bundle);
                    String title = Utility.getBundleString("templateimporttitle", bundle);
                    String buttonok = Utility.getBundleString("voc1", bundle);
                    String buttonko = Utility.getBundleString("voc2", bundle);
                    ConfirmDialog confirm = new ConfirmDialog(BookImporter.getInstance(), true, title, text, buttonok, buttonko);

                    confirm.setVisible(true);
                    boolean response = confirm.getChoice();
                    confirm.dispose();

                    if (response==true) {
                        JComboBox combo = (JComboBox) templatelists.get(panelname);
                        Template selected = (Template) combo.getSelectedItem();
                        //importMetadataSilent(Globals.TEMPLATES_FOLDER_SEP + selected.getFileName(), panelname);
                        String xmlFile = Globals.TEMPLATES_FOLDER_SEP + selected.getFileName();
                        importMetadata(xmlFile, panelname, false, false, true);
                    }
                }
            });
        }
    }
   
    public JPanel drawTemplatePanel(final String panelname){
        ResourceBundle bundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);
        JPanel templatepanel = new JPanel(new MigLayout("fillx,insets 5 5 5 5"));
        final JComboBox choose_template = new JComboBox();
        
        templatepanel.removeAll();
        templatepanel.revalidate();
        TreeMap<String, String> templates = TemplatesUtility.getTemplatesList();
        
        templatepanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), Utility.getBundleString("paneltemplate", bundle), TitledBorder.LEFT, TitledBorder.TOP));
        
        ArrayList<Template> combolist = new ArrayList<Template>();
        JButton templateimport = new JButton(Utility.getBundleString("templateimportbutton",bundle));
        
        if (!templates.isEmpty()){
            choose_template.setEnabled(true);
            for(Map.Entry<String, String> entry : templates.entrySet()) {
                combolist.add(new Template(entry.getKey(), entry.getValue()));
            }
        
            Template[] combo = combolist.toArray(new Template[combolist.size()]);
            choose_template.setModel(new javax.swing.DefaultComboBoxModel(combo));
            choose_template.setSelectedItem(combo[0]);
            choose_template.setMinimumSize(new Dimension(250,20));
            choose_template.setName("IMTemplateCombo");
            templateimport.setName("IMTemplateImportButton");
            templateimport.setMinimumSize(new Dimension(120,10));
            logger.info("Adding a new template");
            
            
            templatepanel.add(choose_template, "width 100:250:250");
          
        }
        else{
            choose_template.setMinimumSize(new Dimension(250,20));
            choose_template.setEnabled(false);
            
            templateimport.setName("IMTemplateImportButton");
            templateimport.setMinimumSize(new Dimension(120,10));
            templateimport.setEnabled(false);
            
            templatepanel.add(choose_template, "width 100:250:250");
            
        }
        
        templatelists.put(panelname, choose_template);
        templatebuttons.put(panelname+"_import", templateimport);
        AddImportHandler(panelname);
        templatepanel.add(templateimport);
        
        JButton templateexport = new JButton(Utility.getBundleString("templateexportbutton",bundle));
        templateexport.setName("IMTemplateExportButton");
        templateexport.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent event)
            {
                exportTemplate(panelname);
                redrawTemplatePanels();
            }
        });
    
        templatebuttons.put(panelname+"_export", templateexport);
        
        JButton templatedelete = new JButton(Utility.getBundleString("templatedeletebutton",bundle));
        templatedelete.setName("IMTemplateDeleteButton");
        templatedelete.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent event)
            {
                deleteTemplate(panelname);
                redrawTemplatePanels();
            }
        });
        templatebuttons.put(panelname+"_delete", templatedelete);
        if (templates.isEmpty()){
            templatedelete.setEnabled(false);
        }
          
        templatepanel.add(templateexport);
        templatepanel.add(templatedelete);
        templatepanel.repaint();
        templatepanel.revalidate();
        
        return templatepanel;
    }
    
    public final void createFrontalPane(){
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

            jLayeredPane2.add(book_panel, "wrap, grow, height 80:80:80");
            jLayeredPane2.add(jPanel3, "wrap, grow, height 100:540:540, align center");
        }
        else{
            MigLayout path_layout = new MigLayout("fillx, inset 10 10 10 10");
            jLayeredPane2.setLayout(path_layout);

            jLayeredPane2.add(jPanel3, "height 100:620:620, align center, growx");
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
    public void createComponentMap(JPanel panel) {
        componentMap = new HashMap<String, Component>();
        createComponentMap(panel, true);
    }

    public void initializeXmlTree(boolean fromFile, boolean refresh) {
        ResourceBundle bundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);
        xmlTree = new XMLTree(Globals.SELECTED_FOLDER_SEP, fromFile, false, refresh);
        
        if (XMLTree.getRoot() == null) {
            logger.info("Error during bookstructure initialization!");
            JOptionPane.showMessageDialog(new Frame(), Utility.getBundleString("xmltreeinitialization", bundle));
            System.exit(0);
        }
        
        setViewXmltree(xmlTree);
    }

    public void initializeUndoXmlTree(String file) { 
       ResourceBundle bundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);
       xmlTree = new XMLTree(file, true, true,false);

        if (XMLTree.getRoot() == null) {
            logger.info("Error during bookstructure initialization!");
            JOptionPane.showMessageDialog(new Frame(), Utility.getBundleString("xmltreeinitialization", bundle));
            System.exit(0);
        }

        setViewXmltree(xmlTree);
    }

    public void setViewXmltree(XMLTree xmlTree) {
        XMLTree.expandAll(xmlTree);
        jScrollPane1.setViewportView(xmlTree);
    }
    
    public void importSingleMetadata(String filename, String PID, boolean view, boolean visible){
        ResourceBundle bundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);
        
        try {
            JLayeredPane singlemetadatapane = new JLayeredPane();
            singlemetadatapane.setName(PID);
            singlemetadatapane.setVisible(visible);
            String xmlFile = Globals.SELECTED_FOLDER_SEP + filename;
            
            javax.swing.JScrollPane main_scroller = new javax.swing.JScrollPane();
            final JPanel main_panel = new JPanel(new MigLayout("fillx, insets 10 10 10 10"));
            main_panel.setName(filename);
            MetaPanels item = new MetaPanels(main_panel, singlemetadatapane);
            this.metadatapanels.put(main_panel.getName(), item);
            singlemetadatapane.add(main_scroller, "wrap, growx");
   
            jTabbedPane2.add(singlemetadatapane);
            
            if (view == true){
                this.importMetadata(xmlFile, filename, false, false, false);
            }
            else{
                this.metadata = MetaUtility.getInstance().metadata_reader(Globals.DUPLICATION_FOLDER_SEP + "session" + filename);
                this.setMetadataTab(singlemetadatapane, filename);
                this.exportMetadataSilent(xmlFile, filename);
            }
        } catch (Exception ex) {
            setCursor(null);
            JOptionPane.showMessageDialog(this, Utility.getBundleString("errorloadUwmetadataText", bundle) + ": " + ex.getMessage());
        }
    }
    
    public void importMetadataSilent(String xmlFile, String panelname){
        ResourceBundle bundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);
        try {
            //Leggi il file uwmetadata.xml
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            MetaUtility.getInstance().read_uwmetadata(xmlFile, panelname);

            this.metadatapanels.get(panelname).getPane().removeAll();
            this.metadatapanels.get(panelname).getPane().revalidate();

            //Ridisegna l'interfaccia
            this.setMetadataTab(this.metadatapanels.get(panelname).getPane(), panelname);
            
            setCursor(null);
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
    protected void importMetadata(String xmlFile, String panelname, boolean showMessageOne, boolean showMessageTwo, boolean isTemplate) {
        ResourceBundle bundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);

        try {
            //if (new File(Globals.SELECTED_FOLDER_SEP + Globals.IMP_EXP_METADATA).isFile()) {
            if (new File(xmlFile).isFile()) {
                boolean importmetadata = false;
                
                if (showMessageOne == true){
                    String text = Utility.getBundleString("loadUwmetadataText", bundle);
                    String title = Utility.getBundleString("loadUwmetadata", bundle);
                    String buttonok = Utility.getBundleString("voc1", bundle);
                    String buttonko = Utility.getBundleString("voc2", bundle);
                    ConfirmDialog confirm = new ConfirmDialog(this, true, title, text, buttonok, buttonko);

                    confirm.setVisible(true);
                    importmetadata = confirm.getChoice();
                    confirm.dispose();
                }
                else{
                    importmetadata = true;
                }
                
                if (importmetadata){
                    //Leggi il file uwmetadata.xml
                    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    if (isTemplate){
                        MetaUtility.getInstance().setSessionMetadataFile(xmlFile, panelname);
                    }
                    else{
                        MetaUtility.getInstance().setSessionMetadataFile(Globals.SELECTED_FOLDER_SEP + panelname, panelname);
                    }
                    
                    //Ridisegna l'interfaccia
                    this.metadata = MetaUtility.getInstance().metadata_reader(Globals.DUPLICATION_FOLDER_SEP + "session" + panelname);
                    
                    MetaUtility.getInstance().read_uwmetadata(xmlFile, panelname);
                    
                    //Ridisegna l'interfaccia
                    this.setMetadataTab(this.metadatapanels.get(panelname).getPane(), panelname);
                    
                    setCursor(null);
                    
                    if (showMessageTwo==true){
                        JOptionPane.showMessageDialog(this, Utility.getBundleString("import4", bundle));
                    }
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
            
            String text = Utility.getBundleString("loadStructureText", bundle);
            String title = Utility.getBundleString("loadStructure", bundle);
            String buttonok = Utility.getBundleString("voc1", bundle);
            String buttonko = Utility.getBundleString("voc2", bundle);
            ConfirmDialog confirm = new ConfirmDialog(StartWizard.getInstance().mainFrame, true, title, text, buttonok, buttonko);

            confirm.setVisible(true);
            response = confirm.getChoice();
            confirm.dispose();
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
    
    public void refreshMetadataTab(boolean updateLang, String panelname){
        try {
            this.createComponentMap(this.metadatapanels.get(panelname).getPanel());
            boolean exported = this.exportMetadataSilent(Globals.DUPLICATION_FOLDER_SEP + "export" + panelname, panelname);
            this.metadatapanels.get(panelname).getPane().removeAll();
            
            if (updateLang){
                MetaUtility.getInstance().preInitializeData();
            }
            
            initializeData(panelname);
            
            if(exported){
                this.importMetadataSilent(Globals.DUPLICATION_FOLDER_SEP + "export"+panelname, panelname);
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage());
        }
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
                            for (Map.Entry<String, MetaPanels> entry : this.metadatapanels.entrySet()) {
                                boolean exported = this.exportMetadataSilent(backup_metadata, entry.getValue().getPanel().getName());
                                entry.getValue().getPane().removeAll();
                                
                                MetaUtility.getInstance().preInitializeData();
                                initializeData(entry.getValue().getPanel().getName());
                                
                                if(exported){
                                    this.importMetadataSilent(backup_metadata, entry.getValue().getPanel().getName());
                                }
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
        boolean response = false;
        
        String text = Utility.getBundleString("setasdefaultlang", bundle);
        String title = Utility.getBundleString("setasdefaultlangtitle", bundle);
        String buttonok = Utility.getBundleString("voc1", bundle);
        String buttonko = Utility.getBundleString("voc2", bundle);
        ConfirmDialog confirm = new ConfirmDialog(this, true, title, text, buttonok, buttonko);

        confirm.setVisible(true);
        response = confirm.getChoice();
        confirm.dispose();
        
        if (response==true) {
            Utility.setDefaultLangCurrent();
        }
    }

    /**
     * Setta tutte le strutture dati necessarie per la creazione
     * dell'interfaccia dei metadati
     *
     */
    private void initializeData(String panelname) {
       try {
            //Inserisce tutte le classificazione nella struttura dati oefos
            //MetaUtility.getInstance().classifications_reader("", panelname);
            
            //Crea Interfaccia dei metadatai
            if (this.metadatapanels.isEmpty()){
                //Inserisce tutti i campi dinamici visibili nella struttura dati metadata
                this.metadata = MetaUtility.getInstance().metadata_reader(Globals.DUPLICATION_FOLDER_SEP+ Globals.BACKUP_INIT);
                
                this.setMetadataTab(jLayeredPane1, mainpanel);
            }
            else{
               this.metadata = MetaUtility.getInstance().metadata_reader(Globals.DUPLICATION_FOLDER_SEP + "session" + panelname);

               this.setMetadataTab(this.metadatapanels.get(panelname).getPane(), this.metadatapanels.get(panelname).getPanel().getName());
            }
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
    private void setMetadataTab(JLayeredPane metadatapane, String panelname) throws Exception {
        ResourceBundle bundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);
        
        metadatapane.removeAll();
        metadatapane.revalidate();
                    
        Dimension iDim = new Dimension(840, 0);
        int p_x = 10;
        int p_y = 10;
            
        javax.swing.JScrollPane main_scroller = new javax.swing.JScrollPane();
        final JPanel main_panel = new JPanel(new MigLayout("fillx, insets 10 10 10 10"));
        main_panel.setName(panelname);
        
        main_scroller.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        main_scroller.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        main_scroller.setPreferredSize(new java.awt.Dimension(100, 730));
        main_scroller.setViewportView(main_panel);
        main_scroller.setBorder(null);
        main_scroller.setBounds(5, 5, 730, 730);
        main_scroller.getVerticalScrollBar().setUnitIncrement(16);
        JPanel templatepanel = drawTemplatePanel(panelname);

        main_panel.add(templatepanel, "wrap, growx");
        
        JPanel innerPanel = new JPanel(new MigLayout());
        innerPanel.setName("pannello000");
        JLabel label = new JLabel();
        label.setText(Utility.getBundleString("obblfield", bundle));
        label.setHorizontalAlignment(SwingConstants.RIGHT);
        label.setFont(new Font("MS Sans Serif", Font.PLAIN, 12));
        main_panel.add(label, "wrap");
          
        policy = new IndexedFocusTraversalPolicy();
        
        //Metodo che si occupa della costruzione dell'interfaccia a partire dalle strutture dati
        MetaUtility.getInstance().classificationAddButton = null;
        MetaUtility.getInstance().classificationRemoveButton = null;
        MetaUtility.getInstance().removeContribute = null;
        MetaUtility.getInstance().create_metadata_view(metadata,  main_panel, 0, panelname);
        
        MetaPanels item = new MetaPanels(main_panel, metadatapane);
        this.metadatapanels.put(main_panel.getName(), item);
        
        metadatapane.setLayout(new MigLayout("fillx, insets 10 10 10 10"));
        metadatapane.add(main_scroller, "wrap, growx");
        
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                            .addPropertyChangeListener("focusOwner", 
                     new PropertyChangeListener() {

          @Override
          public void propertyChange(PropertyChangeEvent evt) {
            if (!(evt.getNewValue() instanceof JComponent)) {
              return;
            }
            JComponent focused = (JComponent) evt.getNewValue();
            if (main_panel.isAncestorOf(focused)) {
              focused.scrollRectToVisible(focused.getBounds());
            }
          }
        });
        
        setFocusTraversalPolicy(policy);        
        setCursor(null);
    }
    
    public class IndexedFocusTraversalPolicy extends 
        FocusTraversalPolicy {

         private ArrayList<Component> components = 
            new ArrayList<Component>();

         public void addIndexedComponent(Component component) {
              components.add(component);
         }

         @Override
         public Component getComponentAfter(Container aContainer, 
                     Component aComponent) {
              int atIndex = components.indexOf(aComponent);              
              int nextIndex = (atIndex + 1) % components.size();
              return components.get(nextIndex);
         }

         @Override
         public Component getComponentBefore(Container aContainer,
               Component aComponent) {
              int atIndex = components.indexOf(aComponent);
              int nextIndex = (atIndex + components.size() - 1) %
                      components.size();
              return components.get(nextIndex);
         }

         @Override
         public Component getFirstComponent(Container aContainer) {
              return components.get(0);
         }
         
         @Override
         public Component getLastComponent(Container aContainer) {
              return components.get(0);
         }
         
         @Override
         public Component getDefaultComponent(Container aContainer) {
              return components.get(0);
         }
      }
    
    /**
     * Inserisce in una struttura dati solo i campi che si possono editare tra
     * tutti i campi di un JPanel
     *
     * @param panel
     */
    private void createComponentMap(JPanel panel, boolean x) {
        
        Component[] components = panel.getComponents();
        try{
            for (int i = 0; i < components.length; i++) {
                if (components[i] != null){
                    try{
                        if (components[i] instanceof javax.swing.JTextField) {
                            if (components[i].getName() != null){
                                componentMap.put(components[i].getName(), components[i]);
                            }
                        }
                    }
                    catch(Exception ex){
                        logger.error(ex.getMessage());
                    }
                     
                    try{
                        if (components[i] instanceof javax.swing.JTextArea) {
                            if (components[i].getName() != null){
                                componentMap.put(components[i].getName(), components[i]);
                            }
                        }
                    }
                    catch(Exception ex){
                        logger.error(ex.getMessage());
                    }
                    
                    try{
                        if (components[i] instanceof javax.swing.JComboBox) {
                            if (components[i].getName() != null && !components[i].getName().equals("IMTemplateCombo")){
                                componentMap.put(components[i].getName(), components[i]);
                            }
                        }
                    }
                    catch(Exception ex){
                        logger.error(ex.getMessage());
                    }
                    
                    try{
                        if (components[i] instanceof JXDatePicker) {
                            if (components[i].getName() != null){
                                componentMap.put(components[i].getName(), components[i]);
                            }
                        }
                    }
                    catch(Exception ex){
                        logger.error(ex.getMessage());
                    }
                    
                    try{
                        if (components[i] instanceof JDateChooser) {
                            if (components[i].getName() != null){
                                componentMap.put(components[i].getName(), components[i]);
                            }
                        }
                    }
                    catch(Exception ex){
                        logger.error(ex.getMessage());
                    }
                    
                    try{
                        if (components[i] instanceof JTree) {
                            if (components[i].getName() != null){
                                componentMap.put(components[i].getName(), components[i]);
                            }
                        }
                    }
                    catch(Exception ex){
                        logger.error(ex.getMessage());
                    }
                    
                    try{
                        if (components[i] instanceof JCheckBox) {
                            if (components[i].getName() != null){
                                componentMap.put(components[i].getName(), components[i]);
                            }
                        }
                    }
                    catch(Exception ex){
                        logger.error(ex.getMessage());
                    }
                    
                    try{
                        if (components[i] instanceof JLabel) {
                            if (components[i].getName() != null){
                                componentMap.put(components[i].getName(), components[i]);
                            }
                        }
                    }
                    catch(Exception ex){
                        logger.error(ex.getMessage());
                    }
                    
                    try{
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
                    }
                    catch(Exception ex){
                        logger.error(ex.getMessage());
                    }
                    
                    try{
                        if (components[i] instanceof javax.swing.JPanel) {
                            if (components[i].getName() != null) {
                                if (components[i].getName().contains("---ImPannelloClassif---")) {
                                    componentMap.put(components[i].getName(), components[i]);
                                }
                            }

                            this.createComponentMap((JPanel) components[i], true);
                        }
                    }
                    catch(Exception ex){
                        logger.error(ex.getMessage());
                    }
                }
            }
        }
        catch(Exception ex){
            logger.error(ex.getMessage());
        }
    }

    /**
     * Restituisce il path del file scelto per l'esportazione o l'importazione
     * di un file pdf, Null se non viene scelto un file
     *
     * @param exp
     * @return
     */
    private String chooseFileImpExport(boolean exp, String type,String extension) {
        ResourceBundle bundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);

        JFileChooser saveFile = new JFileChooser();//new save dialog  
        
        // Set the text
        saveFile.setDialogTitle(Utility.getBundleString("selectfolder", bundle));
        saveFile.setApproveButtonText(Utility.getBundleString("save"+type, bundle));
        saveFile.setApproveButtonMnemonic('a');
        saveFile.setApproveButtonToolTipText(Utility.getBundleString("tooltipsave"+type, bundle));
        
        saveFile.setLocale(Globals.CURRENT_LOCALE);
        saveFile.updateUI();
        
        FileFilter filter = new FileNameExtensionFilter(extension, extension);
        saveFile.addChoosableFileFilter(filter);
        
        int ret = saveFile.showSaveDialog(BookImporter.getInstance());

        boolean selected = false;
        String pdfLocation = null;

        if (ret == JFileChooser.APPROVE_OPTION) {
            File f = saveFile.getSelectedFile();

            String filePath = f.getAbsolutePath();
            if (!filePath.toLowerCase().endsWith("."+extension)) {
                f = new File(filePath + "."+extension);
            }

            pdfLocation = f.getAbsolutePath();

            if (new File(pdfLocation).isFile() && exp) {
                String text = Utility.getBundleString("fileexists", bundle);
                String title = Utility.getBundleString("fileexists", bundle);
                String buttonok = Utility.getBundleString("voc1", bundle);
                String buttonko = Utility.getBundleString("voc2", bundle);
                ConfirmDialog confirm = new ConfirmDialog(this, true, title, text, buttonok, buttonko);

                boolean response = false;
                
                confirm.setVisible(true);
                response = confirm.getChoice();
                confirm.dispose();
            
                if (response==true) {
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

        jMenuItem14.setText(Utility.getBundleString("uploadsinglevideo", bundle));
        jMenuItem14.setVisible(false);
        
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
            jMenuItem14.setVisible(true);
        } else if(Globals.TYPE_BOOK == Globals.BOOK) {
            jMenuItem14.setVisible(false);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jMenuItem13 = new javax.swing.JMenuItem();
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
        jMenuItem11 = new javax.swing.JMenuItem();
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
        jMenuItem14 = new javax.swing.JMenuItem();

        jMenuItem13.setText("jMenuItem13");

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new java.awt.Dimension(1145, 600));
        setPreferredSize(new java.awt.Dimension(1137, 735));

        jSplitPane1.setPreferredSize(new java.awt.Dimension(232, 414));

        jTree2.setPreferredSize(new java.awt.Dimension(300, 76));
        jScrollPane1.setViewportView(jTree2);
        jScrollPane1.createVerticalScrollBar();
        jScrollPane1.createHorizontalScrollBar();

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

        jMenuItem11.setText("Help");
        jMenuItem11.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem11ActionPerformed(evt);
            }
        });
        jMenu6.add(jMenuItem11);

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

        jMenuItem14.setLabel("Upload Single Video");
        jMenuItem14.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem14ActionPerformed(evt);
            }
        });
        jMenu2.add(jMenuItem14);

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
                        .addGap(507, 507, 507)
                        .addComponent(filler1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jSplitPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 703, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(filler2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
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
    
    public boolean exportMetadataSilent(String xmlFile, String panelname){
        String error = "";
        try {
            componentMap = new HashMap<String, Component>();
            
            createComponentMap(this.metadatapanels.get(panelname).getPanel());
            this.metadata = MetaUtility.getInstance().metadata_reader(Globals.DUPLICATION_FOLDER_SEP + "session" + panelname);
            error = MetaUtility.getInstance().check_and_save_metadata(xmlFile, true, false, panelname);
        } catch (Exception ex) {
            logger.error(ex.getMessage());
        }
        
        if (error.length() > 0) {
                return false;
        }
        else{
            return true;
        }
    }
    
    public String exportError = "";
    
    public boolean exportMetadata(String location, String panelname, boolean alertresult, String panename){
        boolean result = false;
        
        ResourceBundle bundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);
        if (Globals.FOLDER_WRITABLE) {
            try {
                componentMap = new HashMap<String, Component>();
                createComponentMap(this.metadatapanels.get(panelname).getPanel());
                this.metadata = MetaUtility.getInstance().metadata_reader(Globals.DUPLICATION_FOLDER_SEP + "session" + panelname);
                
                if (location.isEmpty())
                    location = Globals.SELECTED_FOLDER_SEP + Globals.IMP_EXP_METADATA;
                
                String error = MetaUtility.getInstance().check_and_save_metadata(location, true, true, panelname);
                
                if (error.length() > 0) {
                    this.exportError += "\nMetadata TAB: " + panename + "\n"+ error;
                }
                else {
                    if (alertresult && this.exportError.isEmpty()){
                        JOptionPane.showMessageDialog(this, Utility.getBundleString("export4", bundle));
                    }
                    result = true;
                }
            } catch (Exception ex) {
                logger.error(ex.getMessage()); 
            }
        } else {
            JOptionPane.showMessageDialog(this, Utility.getBundleString("opnotpermitted", bundle));
        }
        return result;
    }
    
    public boolean exportAllMetadatas(boolean showokmessage){
        boolean alertexport = false;
        int i = 1;
        boolean result = false;
        
        if (this.metadatapanels.size() >1){
            logger.info("Export multiple metadata tabs");
        }
        
        for(Map.Entry<String,MetaPanels> entry : this.metadatapanels.entrySet()) {
            String namepanel = entry.getValue().getPanel().getName();
            String namepane = entry.getValue().getPane().getName();
            logger.info("Exporting metadata tab:" + namepanel);
            
            if (namepane==null){
                namepane = "General";
            }
            
            if (i == this.metadatapanels.size() && showokmessage){
                alertexport = true;
            }
            
            exportMetadata(Globals.SELECTED_FOLDER_SEP + namepanel, namepanel, alertexport, namepane);
            i++;
        }
        
        if (!this.exportError.isEmpty()){
            JOptionPane.showMessageDialog(this, this.exportError);
            this.exportError = "";
        }
        else{
            result = true;
        }
        return result;
    }
    /**
     * Gestisce l'esportazione dei metadati sul file uwmetadata.xml nella
     * cartella locale di lavoro
     */
    private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem1ActionPerformed
        logger.info("Export metadata required!");
        exportAllMetadatas(true);
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
        boolean showMessageOne = true;
        boolean showMessageTwo = true;
        int i = this.metadatapanels.size();
        logger.info("Metadata import required!");
        if (i>1){
            logger.info("Multiple tab metadata import");
            
            int j = 1;
            for(Map.Entry<String, MetaPanels> entry : this.metadatapanels.entrySet()){
                try {
                    showMessageOne = (j==1)?true:false;
                    showMessageTwo = (j==i)?true:false;
  
                    j++;
                    String panelname = entry.getValue().getPanel().getName();
                    logger.info("Multiple metadata tab: "+panelname);
                    importMetadata(Globals.SELECTED_FOLDER_SEP + panelname, panelname, showMessageOne, showMessageTwo, false);
                } catch (Exception ex) {
                    logger.error(ex.getMessage());
                }
            }
        }
        else{
            logger.info("Multiple single metadata tab: "+mainpanel);
            importMetadata(Globals.SELECTED_FOLDER_SEP + Globals.IMP_EXP_METADATA, mainpanel, showMessageOne, showMessageTwo, false);
        }
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

            String location = chooseFileImpExport(true, "pdf", "pdf");

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
        ResourceBundle bundle = ResourceBundle.getBundle(Globals.RESOURCES,Globals.CURRENT_LOCALE, Globals.loader);
        boolean canupload = false;    
        
        logger.info("Upload: check internet connection"); 
        
        if (Globals.ONLINE){
            if(XMLTree.getSingleMetadataFiles().size()>0){
                if(!this.exportAllMetadatas(false)){
                    JOptionPane.showMessageDialog(this, Utility.getBundleString("multimetadataerror", bundle));
                    canupload=false;
                }
                else{
                    canupload = true;
                }
            }
            else{
                canupload = true;
            }
        }
        else{
            JOptionPane.showMessageDialog(this, Utility.getBundleString("offline_upload", bundle));
            logger.info("Upload: connection [X]");
        }
        
        if(canupload){
            logger.info("Upload: connection [OK]");
            this.setVisible(false);
            UploadSettings.getInstance(Globals.CURRENT_LOCALE).setVisible(true);
        }
        DOMConfigurator.configure(Globals.LOG4J);
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

                        String location = chooseFileImpExport(false,"pdf", "pdf");

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
        ResourceBundle bundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);
        boolean response = false;
        
        if (Globals.FOLDER_WRITABLE) {
            String text = Utility.getBundleString("loadstructuremanual", bundle);
            String title = Utility.getBundleString("loadStructure", bundle);
            String buttonok = Utility.getBundleString("voc1", bundle);
            String buttonko = Utility.getBundleString("voc2", bundle);
            ConfirmDialog confirm = new ConfirmDialog(this, true, title, text, buttonok, buttonko);

            confirm.setVisible(true);
            response = confirm.getChoice();
            confirm.dispose();
            
            if (response==true){
                initializeXmlTree(true,false);
                it.imtech.utility.Utility.cleanUndoDir();
                JOptionPane.showMessageDialog(this, Utility.getBundleString("loadstructuremanualok", bundle));
            }
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

    private void jMenuItem9ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem9ActionPerformed
        ResourceBundle bundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);
        String title = Utility.getBundleString("dialog_1_title", bundle);
        String text = Utility.getBundleString("dialog_1", bundle);

        ConfirmDialog confirm = new ConfirmDialog(this, true, title, text,Utility.getBundleString("confirm", bundle),Utility.getBundleString("annulla", bundle));

        confirm.setVisible(true);
        boolean close = confirm.getChoice();
        confirm.dispose();

        if (close == true){
            this.dispose();
        }
    }//GEN-LAST:event_jMenuItem9ActionPerformed

    private void jMenuItem11ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem11ActionPerformed
        // TODO add your handling code here:
        Helper helper = new Helper();

        helper.openHelp();
    }//GEN-LAST:event_jMenuItem11ActionPerformed

    private void jMenuItem14ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem14ActionPerformed
        // TODO add your handling code here:
        ResourceBundle bundle = ResourceBundle.getBundle(Globals.RESOURCES,Globals.CURRENT_LOCALE, Globals.loader);
        boolean canupload = false;    
        
        logger.info("Upload: check internet connection...");

        if (Globals.ONLINE){
            canupload = true;
            logger.info("Upload: connection [OK]");
            
            XMLTree.exportBookstructure(Globals.SELECTED_FOLDER_SEP);
            AlertDialog alert = null;
            switch(XMLTree.getVideoFromStructure()) {
                case 0:
                    logger.error("No videos founded!");
                    canupload = false;
                    bundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);
                    alert = new AlertDialog(null, true, 
                            Utility.getBundleString("dialog_4_title", bundle), 
                            Utility.getBundleString("dialog_4", bundle)+" "+file, "Ok");
                    break;
                case 1:
                    logger.info("One video founded");
                    break;
                default:
                    logger.error("Too many videos founded, only one is accepted!");
                    canupload = false;
                    bundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);
                    alert = new AlertDialog(null, true, 
                            Utility.getBundleString("dialog_3_title", bundle), 
                            Utility.getBundleString("dialog_3", bundle)+" "+file, "Ok");
                    break;
            }
            
        } else {
            JOptionPane.showMessageDialog(this, Utility.getBundleString("offline_upload", bundle));
            logger.info("Upload: connection [X]");
        }
        
        if(canupload){
            Globals.TYPE_BOOK = Globals.SINGLE_VIDEO;
            this.setVisible(false);
            UploadSettings.getInstance(Globals.CURRENT_LOCALE).setVisible(true);
        }
    }//GEN-LAST:event_jMenuItem14ActionPerformed
   
    private void deleteTemplate(String panelname){
        
        ResourceBundle bundle = ResourceBundle.getBundle(Globals.RESOURCES,Globals.CURRENT_LOCALE, Globals.loader);

        try {
            String title = Utility.getBundleString("deletetemplatetitle", bundle);
            String text = Utility.getBundleString("deletetemplatetext", bundle);
            String buttonok = Utility.getBundleString("ok", bundle);
            
            TemplateDialog inputdialog = new TemplateDialog(this, true, title, text, buttonok, panelname);

            inputdialog.setVisible(true);
            boolean close = inputdialog.getChoice();
            String filetitle = inputdialog.getInputText();
            logger.info("Delete template: " + filetitle);
            inputdialog.dispose();
        } 
        catch (HeadlessException ex) {
            JOptionPane.showMessageDialog(this, Utility.getBundleString("errorloadUwmetadataText", bundle) + ": " + ex.getMessage());
        }  
    }
    
    
    private void exportTemplate(String panelname){
        ResourceBundle bundle = ResourceBundle.getBundle(Globals.RESOURCES,Globals.CURRENT_LOCALE, Globals.loader);

        try {
            String title = Utility.getBundleString("exporttemplatetitle", bundle);
            String text = Utility.getBundleString("exporttemplatetext1", bundle)+"<br/><br/>"+Utility.getBundleString("exporttemplatetext2", bundle);
            String buttonok = Utility.getBundleString("yesexport", bundle);
            String buttoncancel = Utility.getBundleString("noexport", bundle);
            
            InputDialog inputdialog = new InputDialog(this, true, title, text, buttonok, buttoncancel);

            inputdialog.setVisible(true);
            boolean close = inputdialog.getChoice();
            String filetitle = inputdialog.getInputText();
            logger.info("Export template: " + filetitle);
            
            inputdialog.dispose();

            if (close == true){
                if (filetitle != null && !filetitle.isEmpty()) {
                    String filename = filetitle.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
                    filename = filename.toLowerCase();
                    
                    File template = Utility.getUniqueFileName(Globals.TEMPLATES_FOLDER_SEP + filename, "xml");
                    
                    if(this.exportMetadataSilent(template.getAbsolutePath(), panelname)){
                        if(TemplatesUtility.addTemplateXML(template.getName(), filetitle)){
                            //BookImporter.getInstance().redrawTemplatePanels();
                            JOptionPane.showMessageDialog(this, Utility.getBundleString("exporttemplateok", bundle));
                        }
                    }
                    else{
                        template.delete();
                        JOptionPane.showMessageDialog(this, Utility.getBundleString("exporttemplateerror", bundle));
                    }
                }
            }
            
        } catch (HeadlessException ex) {
            JOptionPane.showMessageDialog(this, Utility.getBundleString("errorloadUwmetadataText", bundle) + ": " + ex.getMessage());
        }  
    }
    
    
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /*
         * Create and display the form
         */
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
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
    private javax.swing.JMenuItem jMenuItem11;
    private javax.swing.JMenuItem jMenuItem13;
    private javax.swing.JMenuItem jMenuItem14;
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
