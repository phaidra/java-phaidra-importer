/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package it.imtech.metadata;

import it.imtech.utility.Utility;
import it.imtech.upload.SelectedServer;
import it.imtech.bookimporter.*;
import it.imtech.globals.Globals;
import it.imtech.upload.UploadSettings;
import it.imtech.vocabularies.VocEntry;
import it.imtech.vocabularies.Vocabulary;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import net.miginfocom.swing.MigLayout;
import org.apache.ws.commons.util.NamespaceContextImpl;
import org.jdesktop.swingx.JXDatePicker;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

/**
 * Classe di utility per la creazione, lettura, importazione ed esportazione di
 * metadati
 *
 * @author Medelin Mauro
 */
public class MetaUtility {
    //Gestore dei log
    public final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(MetaUtility.class);
    
    //Istanza singleton dell'oggetto MetaUtility
    private static MetaUtility istance;
    //Valori di default da inserire nei metadati al momento dell'upload
    private HashMap objectDefaultValues;
    private String objectTitle = "";
    //Contiene i nodi scelti dall'utente per la classification
    private TreeMap<Integer, Integer> oefos_path = null;
    //Il nodo selezionato dall'utente durante l'importazione dei metadati
    private DefaultMutableTreeNode selected = null;
    
    private TreeMap<String, String> classifica = null;
    private String selectedClassif = null;
    private Integer classificationMID = null;
    String CID = "";
    
    //Mappatura del file delle classificazioni preso da URL
    protected TreeMap<Object, Taxon> oefos = null;
    //Mappatura dei namespace contenuti nel file dei metadati
    protected Map<String, String> metadata_namespaces = new HashMap<String, String>();
    //Contiene le lingue statiche
    protected TreeMap<String, String> languages = null;
    //Mappatura del file vocabularies.xml preso da URL
    public HashMap<String, TreeMap<String,VocEntry>> vocabularies = null;
    
    private MetaUtility() {}

    /**
     * Singleton MetaUtility
     *
     * @return
     */
    public static MetaUtility getInstance() {
        if (istance == null) {
            istance = new MetaUtility();
        }

        return istance;
    }

    /**
     * Restituisce titolo del Libro/Collezione da esportare
     *
     * @return
     */
    public String getObjectTitle() {
        return objectTitle;
    }

    /**
     * Setta titolo del Libro/Collezione da esportare
     *
     * @param newObj Valore da settare per il campo objectTitle
     */
    public void setObjectTitle(String newObj) {
        this.objectTitle = newObj;
    }

    private JComboBox addClassificationChoice(JPanel choice){
        
        int selected = 0;
        int index = 0;
        int count = 1;
        
        for (Map.Entry<String, String> vc : classifica.entrySet()) {
            if(count==1 && selectedClassif == null){
                selected = index;
                selectedClassif = vc.getKey();
            }
            
            if(selectedClassif != null){
                if (selectedClassif.equals(vc.getKey())) {
                    selected = index;
                }
            }
            index++;
        }

        final ComboMapImpl model = new ComboMapImpl();
        model.putAll(classifica);

        JComboBox result = new javax.swing.JComboBox(model);

        result.setSelectedIndex(selected);
        model.specialRenderCombo(result);

        result.addActionListener(new ActionListener() {
           
            public void actionPerformed(ActionEvent event) {
                BookImporter.getInstance().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        
                JComboBox comboBox = (JComboBox) event.getSource();
                Map.Entry<String,String> c = (Map.Entry<String,String>) comboBox.getSelectedItem();
                
                if(Globals.FOLDER_WRITABLE){
                    BookImporter.classifConf.setAutoSave(true);
                    BookImporter.classifConf.setProperty("classification[@default]", c.getKey());
                }
                
                selectedClassif = c.getKey();      
               
                BookImporter.getInstance().createComponentMap();
                JPanel innerPanel = (JPanel) BookImporter.getInstance().getComponentByName("ImPannelloClassif");
                innerPanel.removeAll();
                
                try {
                    classifications_reader();
                    addClassification(innerPanel, classificationMID);
                } 
                catch (Exception ex) {
                    Logger.getLogger(MetaUtility.class.getName()).log(Level.SEVERE, null, ex);
                }
                
                innerPanel.revalidate();
                BookImporter.getInstance().setCursor(null);
            }
        });

        return result;
    }    
    
    private void setClassificationChoice() throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
        Document doc = Utility.getDocument(Globals.URL_CLASS_LIST,false);
        
        String defaultc = "";
        String fl ="";
        
        if(new File(Globals.SELECTED_FOLDER_SEP + "classification.xml").isFile()){
           selectedClassif = BookImporter.classifConf.getString("classification[@default]");
        }
        
        classifica = new TreeMap<String, String>();
        
        String name = (Globals.TYPE_BOOK==Globals.BOOK)?"book":"coll";
        NodeList nList = doc.getElementsByTagName(name+"classification");

        int count = 1;
        for (int z = 0; z < nList.getLength(); z++) {
            if (nList.item(z).getNodeType() == Node.ELEMENT_NODE) {
                Element c = (Element) nList.item(z);
                fl = SelectedServer.getInstance(null).getHTTPStaticBaseURL() + Globals.FOLD_XML + c.getTextContent();
                
                defaultc = (z==0)?fl:defaultc;
                String cla_nome = "Classification "+count;
                
                try{
                    cla_nome = c.getAttribute("name");
                } catch (Exception ex) {}  
                
                classifica.put(fl,cla_nome);
                count++;
            }
        }
        
        if(selectedClassif == null){
            selectedClassif = defaultc;
            
            if(Globals.FOLDER_WRITABLE){
              BookImporter.classifConf.setAutoSave(true);
              BookImporter.classifConf.setProperty("classification[@default]", selectedClassif);
            }
        }
    }

    /**
     * Metodo adibito alla lettura ricorsiva del file delle classificazioni
     *
     * @return TreeMap<Object, Taxon>
     * @throws Exception
     */
    public void classifications_reader() throws Exception {
        setClassificationChoice();

        ResourceBundle bundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);
        TreeMap<Object, Taxon> rval = new TreeMap<Object, Taxon>();

        try {
            Document doc = Utility.getDocument(selectedClassif,true);
            
            Node n = doc.getFirstChild();
            Element classification = null;

            if (n.getNodeType() == Node.ELEMENT_NODE) {
                Element classifications = (Element) n;

                if (classifications.getTagName().equals("classifications")) {
                    NodeList nList = classifications.getChildNodes();
                    int s = 0;

                    do {
                        if (nList.item(s).getNodeType() == Node.ELEMENT_NODE) {
                            classification = (Element) nList.item(s);

                            if (classification.getTagName().equals("classification")) {
                                CID = classification.getAttribute("ID");
                            }
                        }
                        s++;
                    } while (s < nList.getLength());

                    if (classification == null) {
                        throw new Exception("Classification 1 not found");
                    }

                    //Element iNode = (Element) nList.item(s);
                    NodeList tList = classification.getChildNodes();
                    for (int z = 0; z < tList.getLength(); z++) {
                        if (tList.item(z).getNodeType() == Node.ELEMENT_NODE) {
                            Element taxons = (Element) tList.item(z);

                            if (taxons.getTagName().equals("taxons")) {
                                classifications_reader_taxons(taxons, rval);
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            throw new Exception(Utility.getBundleString("error4",bundle) + ": " + ex.getMessage());
        }
        oefos=rval;
       
    }

    /**
     * Metodo adibito alla lettura ricorsiva del file dei metadati per la
     * costruzione di una struttura dati che li contenga.
     *
     * @return TreeMap<Object, Metadata>
     * @throws Exception
     */
    public TreeMap<Object, Metadata> metadata_reader() throws Exception {
        String lang = Globals.CURRENT_LOCALE.getLanguage();
        TreeMap<Object, Metadata> metadatas = new TreeMap<Object, Metadata>();

        //Hash dei MIDs dei nodi obbligatori
        TreeMap forceAddMID = new TreeMap();
        forceAddMID.put("98", 1);
        forceAddMID.put("14", 1);
        forceAddMID.put("15", 1);
        forceAddMID.put("64", 1);
        forceAddMID.put("65", 1);
        forceAddMID.put("122", 1);
        forceAddMID.put("105", 1);
        forceAddMID.put("23", 1); //format
        forceAddMID.put("24", 1); //location
        forceAddMID.put("25", 1); //filesize
        forceAddMID.put("17", 1); //technical
        forceAddMID.put("22", 1); //Classification
        forceAddMID.put("45", 1); //Taxonpath
        forceAddMID.put("46", 1); //Source
        forceAddMID.put("47", 1); //Taxon
        forceAddMID.put("82", 1); //Kontextuelle Angaben
        forceAddMID.put("89", 1); //REFERENZ
        forceAddMID.put("94", 1); //Referenz
        forceAddMID.put("95", 1); //Referenznummer
        forceAddMID.put("137", 1); //Referenznummer

        try {
            Document doc = Utility.getDocument(Globals.URL_METADATA,false);

            //File xmlFile = new File(BookImporter.selectedFolderSep + "uwmetadata.xml");
            //Document doc = dBuilder.parse(xmlFile);

            Node n = doc.getFirstChild();

            if (n.getNodeType() == Node.ELEMENT_NODE) {
                Element iENode = (Element) n;
                metadata_reader_metadatas(iENode, metadatas, false, forceAddMID, lang);
            }
        } catch (Exception ex) {
            throw ex;
        }

        return metadatas;
    }

    /**
     * Metodo adibito alla creazione dinamica ricorsiva dell'interfaccia dei
     * metadati
     *
     * @param submetadatas Map contente i metadati e i sottolivelli di metadati
     * @param vocabularies Map contenente i dati contenuti nel file xml
     * vocabulary.xml
     * @param parent Jpanel nel quale devono venir inseriti i metadati
     * @param level Livello corrente
     */
    public void create_metadata_view(Map<Object, Metadata> submetadatas, JPanel parent, int level) throws Exception {
        ResourceBundle bundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);
        int lenght = submetadatas.size();
        int labelwidth = 220;
        int i = 0;

        for (Map.Entry<Object, Metadata> kv : submetadatas.entrySet()) {
            if (kv.getValue().MID == 17 || kv.getValue().MID == 23 || kv.getValue().MID == 18 || kv.getValue().MID == 137) {
                continue;
            }

            //Crea un jpanel nuovo e fa appen su parent
            JPanel innerPanel = new JPanel(new MigLayout());
            innerPanel.setName("pannello" + level + i);

            i++;
            String datatype = kv.getValue().datatype.toString();

            if (kv.getValue().MID == 45) {
                JPanel choice = new JPanel(new MigLayout());
                JComboBox combo = addClassificationChoice(choice);
                
                JLabel labelc = new JLabel();
                labelc.setText(Utility.getBundleString("selectclassif",bundle));
                labelc.setPreferredSize(new Dimension(100, 20));
                
                choice.add(labelc);
                choice.add(combo, "wrap,width :700:");
                parent.add(choice, "wrap,width :700:");
                classificationMID = kv.getValue().MID;
                
                innerPanel.setName("ImPannelloClassif");
                try{
                    addClassification(innerPanel, classificationMID);
                }
                catch (Exception ex) {
                    logger.error("Errore nell'aggiunta delle classificazioni");
                }
                
                parent.add(innerPanel, "wrap,width :700:");
                continue;
            }

            if (datatype.equals("Node")) {
                JLabel label = new JLabel();
                label.setText(kv.getValue().description.toString());
                label.setPreferredSize(new Dimension(100, 20));

                int size = 16 - (level * 2);
                Font myFont = new Font("MS Sans Serif", Font.PLAIN, size);
                label.setFont(myFont);
                
                innerPanel.add(label, "wrap");
            } else {
                String title = "";

                if (kv.getValue().mandatory.equals("Y") || kv.getValue().MID == 14 || kv.getValue().MID == 15) {
                    title = kv.getValue().description.toString() + " *";
                } else {
                    title = kv.getValue().description.toString();
                }

                innerPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), title, TitledBorder.LEFT, TitledBorder.TOP));

                if (datatype.equals("Vocabulary")) {
                    TreeMap<String, String> entryCombo = new TreeMap<String, String>();
                    int index = 0;
                    String selected = null;
                    
                    if(!Integer.toString(kv.getValue().MID).equals("8"))
                        entryCombo.put(Utility.getBundleString("comboselect",bundle),Utility.getBundleString("comboselect",bundle));
                    
                    for (Map.Entry<String,TreeMap<String,VocEntry>> vc : vocabularies.entrySet()) {
                        if (vc.getKey().equals(Integer.toString(kv.getValue().MID))) {
                            TreeMap<String,VocEntry> iEntry = vc.getValue();
                
                            for (Map.Entry<String,VocEntry> ivc : iEntry.entrySet()) {
                                entryCombo.put(ivc.getValue().description,ivc.getValue().ID);
                                if (kv.getValue().value != null) {
                                    if (kv.getValue().value.equals(ivc.getValue().ID)) {
                                        selected = ivc.getValue().ID;
                                    }
                                }
                                index++;
                            }
                        }
                    }
                    
                    final ComboMapImpl model = new ComboMapImpl();
                    model.setVocabularyCombo(true);
                    model.putAll(entryCombo);

                    final JComboBox voc = new javax.swing.JComboBox(model);
                    model.specialRenderCombo(voc);

                    voc.setName("MID_" + Integer.toString(kv.getValue().MID));
                     
                    if(Integer.toString(kv.getValue().MID).equals("8") && selected==null)
                        selected = "44";
                                        
                    selected = (selected==null)?Utility.getBundleString("comboselect",bundle):selected;
                    
                    for (int k = 0; k < voc.getItemCount(); k++) {
                        Map.Entry<String, String> el = (Map.Entry<String, String>) voc.getItemAt(k);
                        if(el.getValue().toString().equals(selected))
                            voc.setSelectedIndex(k);
                    }
                    
                    voc.setPreferredSize(new Dimension(150, 30));
                    innerPanel.add(voc, "wrap");
                } else if (datatype.equals("CharacterString")) {
                    JTextArea textField = new javax.swing.JTextArea();
                    textField.setName("MID_" + Integer.toString(kv.getValue().MID));
                    textField.setPreferredSize(new Dimension(230, 0));
                    textField.setText(kv.getValue().value);
                    textField.setLineWrap(true);
                    textField.setWrapStyleWord(true);
                    
                    innerPanel.add(textField, "wrap");
                } else if (datatype.equals("LangString")) {
                    JScrollPane inner_scroll = new javax.swing.JScrollPane();
                    inner_scroll.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
                    inner_scroll.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
                    inner_scroll.setPreferredSize(new Dimension(240, 80));
                    inner_scroll.setName("langStringScroll");
                    JTextArea jTextArea1 = new javax.swing.JTextArea();
                    jTextArea1.setName("MID_" + Integer.toString(kv.getValue().MID));
                    jTextArea1.setText(kv.getValue().value);
                    
                    jTextArea1.setSize(new Dimension(240, 70));
                    jTextArea1.setLineWrap(true);
                    jTextArea1.setWrapStyleWord(true);

                    inner_scroll.setViewportView(jTextArea1);
                    innerPanel.add(inner_scroll);

                    //Add combo language box
                    JComboBox voc = getComboLangBox(kv.getValue().language);
                    voc.setName("MID_" + Integer.toString(kv.getValue().MID) + "_lang");

                    voc.setPreferredSize(new Dimension(200, 20));
                    innerPanel.add(voc, "wrap");
                } else if (datatype.equals("Language")) {
                    final JComboBox voc = getComboLangBox(kv.getValue().value);
                    voc.setName("MID_" + Integer.toString(kv.getValue().MID));

                    voc.setPreferredSize(new Dimension(150, 20));
                    voc.setBounds(5, 5, 150, 20);
                    innerPanel.add(voc, "wrap");
                } else if (datatype.equals("Boolean")) {
                    int selected = 0;
                    TreeMap bin = new TreeMap<String, String>();
                    bin.put("yes", Utility.getBundleString("voc1",bundle));
                    bin.put("no", Utility.getBundleString("voc2",bundle));

                    if (kv.getValue().value == null) {
                        switch (kv.getValue().MID) {
                            case 35:
                                selected = 0;
                                break;
                            case 36:
                                selected = 1;
                                break;
                        }
                    } else if (kv.getValue().value.equals("yes")) {
                        selected = 1;
                    } else {
                        selected = 0;
                    }

                    final ComboMapImpl model = new ComboMapImpl();
                    model.putAll(bin);

                    final JComboBox voc = new javax.swing.JComboBox(model);
                    model.specialRenderCombo(voc);

                    voc.setName("MID_" + Integer.toString(kv.getValue().MID));
                    voc.setSelectedIndex(selected);

                    voc.setPreferredSize(new Dimension(150, 20));
                    voc.setBounds(5, 5, 150, 20);
                    innerPanel.add(voc, "wrap");
                } else if (datatype.equals("License")) {
                    String selectedIndex = null;
                    int vindex = 0;
                    int defaultIndex = 0;
                    
                    TreeMap<String, String> entryCombo = new TreeMap<String, String>();

                    for (Map.Entry<String,TreeMap<String,VocEntry>> vc : vocabularies.entrySet()) {
                        if (vc.getKey().equals(Integer.toString(kv.getValue().MID))) {
                            TreeMap<String,VocEntry> iEntry = vc.getValue();

                            for (Map.Entry<String,VocEntry> ivc : iEntry.entrySet()) {
                                entryCombo.put(ivc.getValue().description,ivc.getValue().ID);
                                
                                if(ivc.getValue().ID.equals("1"))
                                    defaultIndex = vindex;
                                
                                if (kv.getValue().value != null) {
                                    if (ivc.getValue().ID.equals(kv.getValue().value)) {
                                        selectedIndex = Integer.toString(vindex);
                                    }
                                }
                                vindex++;
                            }
                        }
                    }

                    if(selectedIndex == null)
                        selectedIndex = Integer.toString(defaultIndex);
                    
                    ComboMapImpl model = new ComboMapImpl();
                    model.putAll(entryCombo);
                    model.setVocabularyCombo(true);
                    
                    JComboBox voc = new javax.swing.JComboBox(model);
                    model.specialRenderCombo(voc);

                    voc.setName("MID_" + Integer.toString(kv.getValue().MID));
                    voc.setSelectedIndex(Integer.parseInt(selectedIndex));
                    voc.setPreferredSize(new Dimension(150, 20));

                    voc.setBounds(5, 5, 150, 20);
                    innerPanel.add(voc, "wrap");
                } else if (datatype.equals("DateTime")) {
                    JXDatePicker datePicker = new JXDatePicker();
                    datePicker.setName("MID_" + Integer.toString(kv.getValue().MID));

                    if (kv.getValue().value != null) {
                        try {
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                            Date date1 = sdf.parse(kv.getValue().value);
                            datePicker.setDate(date1);
                        } catch (Exception e) {
                            //Console.WriteLine("ERROR import date:" + ex.Message);
                        }
                    }

                    innerPanel.add(datePicker, "wrap");
                }
            }

            //Recursive call
            create_metadata_view(kv.getValue().submetadatas, innerPanel, level + 1);

            if (kv.getValue().editable.equals("Y") || (datatype.equals("Node") && kv.getValue().hidden.equals("0"))) {
                parent.add(innerPanel, "wrap,width :700:");
            }
        }
    }

    /**
     * Metodo adibito all'esportazione dei metadati sul file definito dal
     * parametro
     *
     * @param xmlFile File in cui esportare i metadati
     * @return
     */
    public String check_and_save_metadata(String xmlFile,Boolean toFile) {
        String result = "";

        result = check_and_save_metadata_recursive(BookImporter.getInstance().getMetadata());

        if (result.length() < 1) {
            try {
                this.objectDefaultValues = new HashMap<String, String>();
                this.objectDefaultValues.put("identifier", "o:DEFAULT");
                this.objectDefaultValues.put("upload_date", "DEFAULT");
                this.objectDefaultValues.put("size", "DEFAULT");
                this.objectDefaultValues.put("format", "DEFAULT");
                this.objectDefaultValues.put("peer_reviewed", "no");
                
                Document savedXmlMetadata = create_uwmetadata(null, -1, null,"");
                
                if(toFile)
                    it.imtech.xmltree.XMLUtil.xmlWriter(savedXmlMetadata, xmlFile);
            } 
            catch (Exception ex) {
                result = ex.getMessage().substring(0, 75)+"...";
            }
        }

        return result;
    }

    /**
     * Metodo adibito all'importazione dei metadati da file locale
     *
     * @param filePath File da cui importare i metadati
     * @throws Exception
     */
    public void read_uwmetadata(String filePath) throws Exception {
        try {
            File xmlFile = new File(filePath);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            dbFactory.setNamespaceAware(true);
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(xmlFile);

            NamespaceContextImpl nsContext = new NamespaceContextImpl();
            nsContext.startPrefixMapping("phaidra0", "http://phaidra.univie.ac.at/XML/metadata/V1.0");

            int count = 1;
            //Add namespaces in XML Root
            for (Map.Entry<String, String> field : metadata_namespaces.entrySet()) {
                nsContext.startPrefixMapping("phaidra" + count, field.getValue().toString());
                count++;
            }
            
            this.read_uwmetadata_recursive(BookImporter.getInstance().getMetadata(), "//phaidra0:uwmetadata", nsContext, doc);
        } catch (Exception ex) {
            ResourceBundle ex_bundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);
            throw new Exception(Utility.getBundleString("error22",ex_bundle) + ": " + ex.getMessage());
        }
    }

    /**
     * Metodo adibito all'esportazione dei metadati su oggetto Document a
     * partire dai valori dell'interfaccia
     *
     * @param pid PID del libro durante upload
     * @param pagenum Numero pagina corrente di esportazione
     * @param objectDefaultValues Valori di default durante upload
     * @return Oggetto xml contenente i metadati inseriti nei campi
     * dell'interfaccia
     * @throws Exception
     */
    public Document create_uwmetadata(String pid, int pagenum, HashMap<String, String> objectDefaultValues,String collTitle) throws Exception {
        String xmlFile = "";

        if (objectDefaultValues != null) {
            this.objectDefaultValues = objectDefaultValues;
        }

        StreamResult result = new StreamResult(new StringWriter());

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        // set the factory to be namespace aware
        factory.setNamespaceAware(true);

        // create the xml document builder object and get the DOMImplementation object
        DocumentBuilder builder = factory.newDocumentBuilder();
        DOMImplementation domImpl = builder.getDOMImplementation();
        Document xmlDoc = domImpl.createDocument("http://phaidra.univie.ac.at/XML/metadata/V1.0", "ns0:uwmetadata", null);

        try {
            Transformer t = TransformerFactory.newInstance().newTransformer();
            t.setOutputProperty(OutputKeys.INDENT, "yes");
            t.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            t.setOutputProperty(OutputKeys.STANDALONE, "");
            t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

            // get the root element
            Element rootElement = xmlDoc.getDocumentElement();

            //Add namespaces in XML Root
            for (Map.Entry<String, String> field : metadata_namespaces.entrySet()) {
                rootElement.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:" + field.getKey().toString(), field.getValue().toString());
            }
            create_uwmetadata_recursive(xmlDoc, rootElement, BookImporter.getInstance().getMetadata(), this.objectDefaultValues, pagenum, collTitle);
        } catch (TransformerConfigurationException e) {
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }

        return xmlDoc;
    }

    public HashMap<String, TreeMap<String,VocEntry>> getVocabularies(){
        return vocabularies;
    }
    
    /**
     * Setta le strutture dati che non necessitano della scelta della cartella
     * di lavoro
     */
     public void preInitializeData() throws Exception {
        try {
            //Inserisce tutti i campi dinamici del vocabolario nella struttura dati vocabularies
            vocabularies = Vocabulary.getInstance().getVocabularies();

            //Inserisce tutti le lingue statiche nella struttura dati languages
            languages = Utility.getLanguages();
        }catch (Exception ex) {
            ResourceBundle ex_bundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);
            throw new Exception(Utility.getBundleString("error22",ex_bundle) + ": " + ex.getMessage());
        } 
    }
     
    /**
     * Metodo che setta il TreeMap oefos_path con i valori del nodo selezionato
     * e aggiorna il label che descrive il path dei nodi selezionati
     *
     * @param e L'albero sul quale ricercare il path selezionato
     */
    private void setOEFOS(JTree e) throws Exception {
        try {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.getLastSelectedPathComponent();
            String completePath = "";

            BookImporter.getInstance().createComponentMap();
            Component controls = BookImporter.getInstance().getComponentByName("classification_path");

            //Se e' una foglia aggiorna il path nell'interfaccia e oefos_path
            if (node != null) {
                TreePath selpath = new TreePath(node.getPath());
                e.setSelectionPath(selpath);

                Object[] nodes = selpath.getPath();
                this.oefos_path = new TreeMap<Integer, Integer>();

                for (int i = 1; i < nodes.length; i++) {
                    Object nodeInfo = nodes[i];
                    DefaultMutableTreeNode nodeC = (DefaultMutableTreeNode) nodeInfo;
                    ClassNode c = (ClassNode) nodeC.getUserObject();
                    this.oefos_path.put(i, Integer.parseInt(c.getKey().toString()));

                    completePath += nodes[i].toString();
                    completePath += (i != nodes.length - 1) ? "/" : "";
                }
            } else {
                this.oefos_path = null;
            }

            JLabel label = (JLabel) controls;
            label.setText(completePath);
            label.revalidate();
        } catch (Exception ex) {
            throw new Exception("Exception in setOEFOS: " + ex.getStackTrace() + "\n");
        }
    }

    
    /**
     * Metodo che costruisce l'albero delle classificazione e setta il nodo
     * selezionato se esistente
     *
     * @param nodes Il nodo nel quale aggiungere i nuovi nodi (Nodo Padre)
     * @param taxons La lista di nodi per livello
     */
    
    private void recursiveOefosTreeviewBuild(DefaultMutableTreeNode nodes, TreeMap<Object, Taxon> taxons) throws Exception {
        try {
            for (Map.Entry<Object, Taxon> kv : taxons.entrySet()) {
                ClassNode iNode = new ClassNode("" + kv.getKey(), kv.getValue().upstream_identifier + ": " + kv.getValue().description);
              
                ClassMutableNode inner = new ClassMutableNode(iNode);
                nodes.add(inner);

                if (this.oefos_path != null) {
                    if (this.oefos_path.containsValue(kv.getValue().TID)) {
                        selected = inner;
                    }
                }

                recursiveOefosTreeviewBuild(inner, kv.getValue().subtaxons);
            }

 //          Utility.sortTreeChildren(nodes);
        } catch (Exception ex) {
            throw new Exception("Exception in recursiveOefosTreeviewBuild: " + ex.getStackTrace() + "\n");
        }
    }

    /**
     * Aggiunge il pannello delle classificazioni all'interfaccia dei metadati
     *
     * @param innerPanel Pannello sul quale aggiungere i metadati
     * @param kv Valori dei metadati
     */
    private void addClassification(JPanel innerPanel, Integer kv) throws Exception {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);

            DefaultMutableTreeNode hierarchy = new DefaultMutableTreeNode("root");
            selected = null;
            String selectedPath = "";

            recursiveOefosTreeviewBuild(hierarchy, oefos);
            DefaultTreeModel model = new DefaultTreeModel(hierarchy);

            final JTree tree = new JTree(model);
            tree.setRootVisible(false);

            if (selected != null) {
                TreePath selpath = new TreePath(selected.getPath());
                tree.setSelectionPath(selpath);

                Object[] nodes = selpath.getPath();

                for (int i = 1; i < nodes.length; i++) {
                    selectedPath += nodes[i].toString();
                    selectedPath += (i != nodes.length - 1) ? "/" : "";
                }
            }

            tree.setName("MID_" + Integer.toString(kv));
            tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

            tree.addTreeSelectionListener(new TreeSelectionListener() {

                public void valueChanged(TreeSelectionEvent e) {
                    try {
                        setOEFOS(tree);
                    } catch (Exception ex) {
                        Logger.getLogger(MetaUtility.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            });

            javax.swing.JScrollPane tree_scroller = new javax.swing.JScrollPane();
            tree_scroller.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            tree_scroller.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
            tree_scroller.setPreferredSize(new Dimension(700, 600));
            tree_scroller.setViewportView(tree);
            tree_scroller.setBorder(null);

            innerPanel.add(tree_scroller, "wrap");

            JPanel iPanel = new JPanel(new MigLayout());
            iPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), Utility.getBundleString("choose2",bundle), TitledBorder.LEFT, TitledBorder.TOP));
            JLabel label = new JLabel();
            label.setName("classification_path");

            label.setPreferredSize(new Dimension(700, 30));
            label.setText(selectedPath);
            iPanel.add(label, "wrap");

            innerPanel.add(iPanel, "wrap,width :700:");
        } catch (Exception ex) {
            Logger.getLogger(MetaUtility.class.getName()).log(Level.SEVERE, "addClassification", ex);
            throw new Exception("Exception in addClassification: " + ex.getStackTrace() + "\n");
        }
    }

    private void metadata_reader_metadatas(Element iENode, TreeMap<Object, Metadata> metadatas, boolean forceAdd, TreeMap forceAddMID, String sLang) {
        try {
            if (iENode.getTagName().equals("metadatas")) {
                NodeList nList = iENode.getChildNodes();

                for (int s = 0; s < nList.getLength(); s++) {
                    if (nList.item(s).getNodeType() == Node.ELEMENT_NODE) {
                        Element iInnerNode = (Element) nList.item(s);

                        if (iInnerNode.getTagName().equals("metadata")) {
                            String MID = iInnerNode.getAttribute("ID");

                            //Se è hidden rimuovo l'elemento dai forzati
                            String hidden = "0";
                            if(iInnerNode.hasAttribute("hidden")){
                                hidden = iInnerNode.getAttribute("hidden");
                                forceAddMID.remove(iInnerNode.getAttribute("ID"));
                            }
                            
                            if (forceAddMID.containsKey(MID)) {
                                forceAdd = true;
                           }

                            String MID_parent = iInnerNode.getAttribute("mid_parent");
                            String mandatory = iInnerNode.getAttribute("mandatory");
                            String datatype = iInnerNode.getAttribute("datatype");
                            String editable = iInnerNode.getAttribute("editable");
                            String foxmlname = iInnerNode.getAttribute("forxmlname");
                            String sequence = iInnerNode.getAttribute("sequence");
                            String foxmlnamespace = iInnerNode.getAttribute("fornamespace");
                            
                            if (!metadata_namespaces.containsValue(foxmlnamespace)) {
                                int count = metadata_namespaces.size();
                                count++;
                                metadata_namespaces.put("ns" + count, foxmlnamespace);
                            }

                            String description = null;
                            String DESCRIPTION_DE = null;

                            TreeMap<Object, Metadata> submetadatas = new TreeMap<Object, Metadata>();

                            NodeList innerList = iInnerNode.getChildNodes();
                            for (int z = 0; z < innerList.getLength(); z++) {
                                if (innerList.item(z).getNodeType() == Node.ELEMENT_NODE) {
                                    Element iDescrNode = (Element) innerList.item(z);

                                    if (iDescrNode.getAttribute("isocode").equals(sLang)) {
                                        description = iDescrNode.getTextContent();
                                    } else if (iDescrNode.getAttribute("isocode").equals("de")) {
                                        DESCRIPTION_DE = iDescrNode.getTextContent();
                                    }

                                    if (iDescrNode.getTagName().equals("metadatas")) {
                                        if (MID.equals("98")) {
                                            metadata_reader_metadatas(iDescrNode, submetadatas, true, forceAddMID, sLang);
                                        } else {
                                            metadata_reader_metadatas(iDescrNode, submetadatas, false, forceAddMID, sLang);
                                        }
                                    }
                                }
                            }

                            //Fallback DE
                            if (description == null) {
                                description = DESCRIPTION_DE;
                            }
                            if (description == null && !iInnerNode.getTagName().equals("metadata")) {
                                throw new Exception("Can't find description for metadata " + iInnerNode.getTagName());
                            }

                            if ((mandatory.equals("Y") || forceAdd == true)) {
                                int mid_parent = 0;
                                if (!MID_parent.equals("")) {
                                    mid_parent = Integer.parseInt(MID_parent);
                                }

                                Metadata t = new Metadata(Integer.parseInt(MID), mid_parent, description, datatype, editable, foxmlname, null, foxmlnamespace, mandatory,hidden);
                                t.submetadatas = submetadatas;

                                String index = sequence;
                                if (index == null || index.equals("")) {
                                    index = MID;
                                }
                                int param = Integer.parseInt(index);
                                metadatas.put(param, t);
                            }

                            if (forceAddMID.containsKey(MID.toString())) {
                                forceAdd = false;
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(BookImporter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private String check_and_save_metadata_recursive(Map<Object, Metadata> submetadatas) {
        ResourceBundle bundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);
        String error = "";

        for (Map.Entry<Object, Metadata> field : submetadatas.entrySet()) {
            if (!field.getValue().datatype.equals("Node") && field.getValue().editable.equals("Y")) {
                Component element = BookImporter.getInstance().getComponentByName("MID_" + Integer.toString(field.getValue().MID));

                if (element != null) {
                    if (field.getValue().datatype.equals("CharacterString") || field.getValue().datatype.equals("LangString")) {
                        JTextArea textTemp = (JTextArea) element;
                        field.getValue().value = textTemp.getText();
                        if (field.getValue().value.length() < 1 && (field.getValue().mandatory.equals("Y") || field.getValue().MID == 14 || field.getValue().MID == 15)) {
                            error += Utility.getBundleString("error10",bundle) + " " + field.getValue().description.toString() + " " + Utility.getBundleString("error11",bundle) + "!\n";
                        }
                    }
                    if (field.getValue().datatype.equals("LangString")) {
                        Component combobox = BookImporter.getInstance().getComponentByName("MID_" + Integer.toString(field.getValue().MID) + "_lang");
                        JComboBox tmp = (JComboBox) combobox;

                        Map.Entry tmp2 = (Map.Entry) tmp.getSelectedItem();
                        field.getValue().language = tmp2.getKey().toString();
                    } else if (field.getValue().datatype.equals("DateTime")) {
                        JXDatePicker datePicker = (JXDatePicker) element;
                        Date data = datePicker.getDate();

                        field.getValue().value = "";

                        if (data != null) {
                            Format formatter = new SimpleDateFormat("yyyy-MM-dd");
                            String stDate = formatter.format(data);

                            if (!stDate.equals("")) {
                                field.getValue().value = stDate;
                            }
                        }
                    } else if (field.getValue().datatype.equals("Language") || field.getValue().datatype.equals("Boolean") || field.getValue().datatype.equals("License") || field.getValue().datatype.equals("Vocabulary")) {
                        Component combobox = BookImporter.getInstance().getComponentByName("MID_" + Integer.toString(field.getValue().MID));
                        JComboBox tmp = (JComboBox) combobox;

                        Map.Entry tmp2 = (Map.Entry) tmp.getSelectedItem();
                        
                        if(field.getValue().datatype.equals("License") || field.getValue().datatype.equals("Vocabulary")){
                            //ResourceBundle tmpBundle = ResourceBundle.getBundle(Globals.RESOURCES, BookImporter.localConst, Globals.loader); 
                            ResourceBundle tmpBundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader); 
                            
                            if(tmp2.getValue().toString().equals(Utility.getBundleString("comboselect",tmpBundle)) && field.getValue().mandatory.equals("Y"))
                                error += Utility.getBundleString("error10",bundle) + " " + field.getValue().description.toString() + " " + Utility.getBundleString("error11",bundle) + "!\n";
                             else
                                if(tmp2.getValue().toString().equals(Utility.getBundleString("comboselect",tmpBundle)))
                                    field.getValue().value = "";
                                else
                                    field.getValue().value = tmp2.getValue().toString();
                        }                        
                        else
                            field.getValue().value = tmp2.getKey().toString();
                    }
                }
            }

            error += check_and_save_metadata_recursive(field.getValue().submetadatas);
        }
        return error;
    }

    private void classifications_reader_taxons(Element iTaxons, TreeMap<Object, Taxon> taxons) throws Exception {
        String TAXON_DE = null;
        String sLang = Globals.CURRENT_LOCALE.getLanguage();

        NodeList tList = iTaxons.getChildNodes();
        for (int z = 0; z < tList.getLength(); z++) {
            if (tList.item(z).getNodeType() == Node.ELEMENT_NODE) {
                Element taxon = (Element) tList.item(z);

                if (taxon.getTagName().equals("taxon")) {
                    String TID = taxon.getAttribute("ID");
                        
                    String upstream_identifier = taxon.getAttribute("upstream_identifier");
                    String description = null;

                    TreeMap<Object, Taxon> subtaxons = new TreeMap<Object, Taxon>();

                    NodeList dList = taxon.getChildNodes();
                    for (int d = 0; d < dList.getLength(); d++) {
                        if (dList.item(d).getNodeType() == Node.ELEMENT_NODE) {
                            Element iTaxon = (Element) dList.item(d);
                            if (iTaxon.getTagName().equals("description")) {
                                if (iTaxon.getAttribute("isocode").equals(sLang)) {
                                    description = iTaxon.getTextContent();
                                } else if (iTaxon.getAttribute("isocode").equals("de")) {
                                    TAXON_DE = iTaxon.getTextContent();
                                }
                            } else if (iTaxon.getTagName().equals("taxons")) {
                                classifications_reader_taxons(iTaxon, subtaxons);
                            }
                        }
                    }
                    //Fallback DE
                    if (description == null) {
                        description = TAXON_DE;
                    }
                    if (description == null) {
                        throw new Exception("Can't find description for taxon (2)");
                    }

                    // In die Liste einfuegen
                    Taxon t = new Taxon(Integer.parseInt(TID), upstream_identifier, description);
                    t.subtaxons = subtaxons;
                    taxons.put(Integer.parseInt(TID), t);
                }
            }
        }
    }

    private void read_uwmetadata_recursive(Map<Object, Metadata> submetadatas, String xpath, NamespaceContextImpl nsmgr, Document nav) {
        for (Map.Entry<Object, Metadata> field : submetadatas.entrySet()) {
            String actXpath = xpath + "/" + nsmgr.getPrefix(field.getValue().foxmlnamespace) + ":" + field.getValue().foxmlname;

            if (!field.getValue().datatype.equals("Node") && field.getValue().editable.equals("Y")) {
                XPathFactory factory = XPathFactory.newInstance();
                Element node = Utility.getXPathNode(actXpath, nsmgr, nav);

                if (node != null) {
                    if (field.getValue().datatype.equals("LangString")) {
                        field.getValue().value = node.getTextContent();
                        field.getValue().language = node.getAttribute("language");
                    } else if (field.getValue().datatype.equals("ClassificationSource")) {
                        this.oefos_path = new TreeMap<Integer, Integer>();
                        
                        boolean sent = true;
                        for (int i = 0; sent==true; i++) {
                            String tpath = xpath + "/" + nsmgr.getAttributePrefix(field.getValue().foxmlnamespace) + ":taxon[@seq=" + i + "]";
                            Element taxon = Utility.getXPathNode(tpath, nsmgr, nav);
                            if (taxon != null) {
                                this.oefos_path.put(i + 1, Integer.parseInt(taxon.getTextContent()));
                            }
                            else
                                sent=false;
                        }
                    } else {
                        field.getValue().value = node.getTextContent();
                    }
                }
            }
            if (field.getValue().submetadatas != null) {
                read_uwmetadata_recursive(field.getValue().submetadatas, actXpath, nsmgr, nav);
            }
        }
    }

    private void create_uwmetadata_recursive(Document w, Element e, Map<Object, Metadata> submetadatas, HashMap<String, String> defValues, int pagenum, String collTitle) throws Exception {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);
            for (Map.Entry<Object, Metadata> field : submetadatas.entrySet()) {
                if (field.getValue().MID == 2) {
                    this.objectTitle = field.getValue().value;
                }
                if (field.getValue().datatype.equals("Node")) {
                    if (field.getValue().MID == 45 && this.oefos_path == null) {
                        continue;
                    } else if (field.getValue().MID == 45 && this.oefos_path.size() < 1) {
                        continue;
                    } else {
                        String book = Utility.getValueFromKey(metadata_namespaces, field.getValue().foxmlnamespace.toString());
                        Element link = w.createElement(book + ":" + field.getValue().foxmlname.toString());
                        e.appendChild(link);
                        create_uwmetadata_recursive(w, link, field.getValue().submetadatas, defValues, pagenum,collTitle);
                    }
                } else if (field.getValue().MID == 46) {
                    if (this.oefos_path.size() > 0) {
                        String book = Utility.getValueFromKey(metadata_namespaces, field.getValue().foxmlnamespace.toString());
                        Element link = w.createElement(book + ":" + field.getValue().foxmlname.toString());
                        link.setTextContent(CID);
                        e.appendChild(link);
                    }
                } else if (field.getValue().MID == 47 && this.oefos_path.size() > 0) {
                    for (Map.Entry<Integer, Integer> iField : this.oefos_path.entrySet()) {
                        String book = Utility.getValueFromKey(metadata_namespaces, field.getValue().foxmlnamespace.toString());
                        Element link = w.createElement(book + ":" + field.getValue().foxmlname.toString());
                        link.setAttribute("seq", Integer.toString(iField.getKey() - 1));
                        link.setTextContent(Integer.toString(iField.getValue()));
                        e.appendChild(link);
                    }
                } else if (field.getValue().editable.equals("Y")) {
                    String value = "";

                    if (field.getValue().value != null) {

                        if(field.getValue().MID==2 && !collTitle.equals(""))
                            value = collTitle;
                        else{
                            if (field.getValue().MID == 2 && pagenum > 0) {
                                value = field.getValue().value.toString() + " - " + Utility.getBundleString("seite",bundle) + " " + Integer.toString(pagenum);
                            } else {
                                value = field.getValue().value.toString();
                            }
                        }
                    } else if (defValues != null) {
                        if (defValues.containsKey(field.getValue().foxmlname)) {
                            value = defValues.get(field.getValue().foxmlname).toString();
                        } else {
                            value = "en";
                        }
                    }
                    //throw new Exception("\n"+Utility.getBundleString("error8")+ " "+ field.getValue().foxmlname.toString());

                    if (value.length() > 0) {
                        String book = Utility.getValueFromKey(metadata_namespaces, field.getValue().foxmlnamespace.toString());
                        Element link = w.createElement(book + ":" + field.getValue().foxmlname.toString());

                        if (field.getValue().datatype.equals("LangString")) {
                            link.setAttribute("language", field.getValue().language.toString());
                        }

                        link.setTextContent(value);
                        e.appendChild(link);
                    }
                } else if (field.getValue().editable.equals("N") && defValues != null) {
                    String value = null;
                    if (field.getValue().foxmlname.equals("location")) {
                        value = SelectedServer.getInstance(null).getPhaidraURL() + "/" + defValues.get("identifier");
                    } else {
                        value = defValues.get(field.getValue().foxmlname).toString();
                    }

                    String book = Utility.getValueFromKey(metadata_namespaces, field.getValue().foxmlnamespace.toString());
                    Element link = w.createElement(book + ":" + field.getValue().foxmlname.toString());
                    link.setTextContent(value);
                    e.appendChild(link);
                }
            }
        } catch (Exception ex) {
            ResourceBundle bundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);
            throw new Exception(Utility.getBundleString("error7",bundle) + ": " + ex.getMessage());
        }
    }

    private JComboBox getComboLangBox(String value) {
        HashMap<String, String> comboLang = new HashMap<String, String>();
        int selected = 0;
        int index = 0;

        for (Map.Entry<String, String> vc : languages.entrySet()) {
            comboLang.put(vc.getKey(), vc.getValue());

            if (Globals.CURRENT_LOCALE.getLanguage().equals(vc.getKey()) && value == null) {
                selected = index;
            } else if (value != null) {
                if (value.equals(vc.getKey())) {
                    selected = index;
                }
            }
            index++;
        }

        final ComboMapImpl model = new ComboMapImpl();
        model.putAll(comboLang);

        JComboBox result = new javax.swing.JComboBox(model);

        result.setSelectedIndex(selected);
        model.specialRenderCombo(result);

        return result;
    }
}
