/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package it.imtech.metadata;

import it.imtech.bookimporter.*;
import it.imtech.globals.Globals;
import it.imtech.upload.SelectedServer;
import it.imtech.utility.Utility;
import it.imtech.vocabularies.VocEntry;
import it.imtech.vocabularies.Vocabulary;
import it.imtech.xmltree.XMLUtil;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.*;
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
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.io.FileUtils;
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
    private TreeMap<String, TreeMap<Integer, Integer>> oefos_path = new TreeMap<String, TreeMap<Integer, Integer>>();
    private TreeMap<String, TreeMap<Object, Taxon>> oefos = new TreeMap<String, TreeMap<Object, Taxon>>();
    
    //Contiene coppie di valori link classificazioni -> ID (source)
    private TreeMap<String, String> classificationIDS  = new TreeMap<String, String> ();
    
    //Contiene coppie di valori link classificazioni -> nome da visualizzare nel combo
    private LinkedHashMap<String, String> availableClassifications = null;    
    
    //Contiene coppie di valori sequenza della classificazione -> link della classificazione
    private TreeMap<String, String> selectedClassificationList;
    
    //Il nodo selezionato dall'utente durante l'importazione dei metadati
    private DefaultMutableTreeNode selected = null;
    
    //private String selectedClassif = null;
    private Integer classificationMID = null;
    
    public  JButton classificationAddButton = null;
    //Mappatura del file delle classificazioni preso da URL
    
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
    
    public void setSelectedClassificationList(){
        
    }
    
    private JComboBox addClassificationChoice(JPanel choice, final String sequence, final String panelname){
        
        int selected = 0;
        int index = 0;
        int count = 1;
        
        for (Map.Entry<String, String> vc : availableClassifications.entrySet()) {
            if(count==1 && !selectedClassificationList.containsKey(sequence)){
                selected = index;
                selectedClassificationList.put(sequence,vc.getKey());
            }
            
            if(selectedClassificationList.containsKey(sequence)){
                if (selectedClassificationList.get(sequence).equals(vc.getKey())) {
                    selected = index;
                }
            }
            index++;
        }

        final ComboMapImpl model = new ComboMapImpl();
        model.putAll(availableClassifications);

        JComboBox result = new javax.swing.JComboBox(model);

        result.setSelectedIndex(selected);
        model.specialRenderCombo(result);

        result.addActionListener(new ActionListener() {
           
            public void actionPerformed(ActionEvent event) {
                BookImporter.getInstance().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        
                JComboBox comboBox = (JComboBox) event.getSource();
                Map.Entry<String,String> c = (Map.Entry<String,String>) comboBox.getSelectedItem();
                
                selectedClassificationList.put(sequence, c.getKey());      
               
                BookImporter.getInstance().createComponentMap(BookImporter.getInstance().metadatapanels.get(BookImporter.mainpanel).getPanel());
                JPanel innerPanel = (JPanel) BookImporter.getInstance().getComponentByName("ImPannelloClassif---"+sequence);
                innerPanel.removeAll();
                
                try {
                    classifications_reader(sequence);
                    addClassification(innerPanel, classificationMID, sequence, panelname);
                } 
                catch (Exception ex) {
                    logger.error(ex.getMessage());
                }
                
                innerPanel.revalidate();
                BookImporter.getInstance().setCursor(null);
            }
        });

        return result;
    }    
    
    private void setClassificationChoice() throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
        Document doc = Utility.getDocument(Globals.URL_CLASS_LIST,false);
        
        String fl ="";
        
        availableClassifications = new LinkedHashMap <String, String>();
        
        String name = (Globals.TYPE_BOOK==Globals.BOOK)?"book":"coll";
        NodeList nList = doc.getElementsByTagName(name+"classification");

        int count = 1;
        for (int z = 0; z < nList.getLength(); z++) {
            if (nList.item(z).getNodeType() == Node.ELEMENT_NODE) {
                Element c = (Element) nList.item(z);
                
                if (Globals.ONLINE){
                    fl = SelectedServer.getInstance(null).getHTTPStaticBaseURL() + Globals.FOLD_XML + c.getTextContent();
                }
                else{
                    fl =  Globals.FOLD_XML + c.getTextContent();
                }
                String cla_nome = "Classification "+count;
                
                try{
                    cla_nome = c.getAttribute("name");
                } catch (Exception ex) {}  
                
                availableClassifications.put(fl,cla_nome);
                count++;
            }
        }
    }
    
    
    /**
     * Metodo adibito alla lettura ricorsiva dei file delle classificazioni
     *
     * @return TreeMap<Object, Taxon>
     * @throws Exception
     */
    public void classifications_reader(String sequence) throws Exception {
        setClassificationChoice();

        ResourceBundle bundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);
        String currentlink = "";
        
        try {
            boolean defaultclassification = true;
            boolean alreadyread = false;
            
            if (!sequence.isEmpty()){
                defaultclassification = false;
                currentlink = selectedClassificationList.get(sequence);
                
                if (!currentlink.isEmpty()){
                    if (oefos.containsKey(currentlink))
                        alreadyread = true;
                }
            }
            
            for (String classificationLink : this.availableClassifications.keySet())
            {
                if (!alreadyread && (defaultclassification || (!currentlink.isEmpty() && classificationLink.equals(currentlink)))){
                    defaultclassification = false;

                    TreeMap<Object, Taxon> rval = new TreeMap<Object, Taxon>();
                    Document doc = Utility.getDocument(classificationLink,true);
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
                                        classificationIDS.put(classificationLink, classification.getAttribute("ID"));
                                    }
                                }
                                s++;
                            } while (s < nList.getLength());

                            if (classification == null) {
                                throw new Exception("Classification 1 not found");
                            }

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

                        oefos.put(classificationLink, rval);
                    }
                }
            }
            
        } catch (Exception ex) {
            throw new Exception(Utility.getBundleString("error4",bundle) + ": " + ex.getMessage());
        }
    }

    /**
     * Metodo adibito alla lettura ricorsiva del file dei metadati per la
     * costruzione di una struttura dati che li contenga.
     *
     * @return TreeMap<Object, Metadata>
     * @throws Exception
     */
    public TreeMap<Object, Metadata> metadata_reader(String filepath) throws Exception {
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
        forceAddMID.put("123", 1); //identifiers
        forceAddMID.put("124", 1); //identifiers
        forceAddMID.put("125", 1); //identifiers
        forceAddMID.put("5", 1); //keywords
	forceAddMID.put("96", 1); // GPS
        forceAddMID.put("6", 1); // Copertura
        forceAddMID.put("84", 1); // Dimensioni
        forceAddMID.put("83", 1); // Descrizione
        forceAddMID.put("93", 1); // Tipo Materiale
        forceAddMID.put("88", 1); // Unita di misura
        forceAddMID.put("85", 1); // Lunghezza
        forceAddMID.put("86", 1); // Larghezza
        forceAddMID.put("87", 1); // Altezza
        forceAddMID.put("92", 1); // Diametro
        
        forceAddMID.put("11", 1); // Contributo
        forceAddMID.put("12", 1); // Ruolo
        //forceAddMID.put("126", 1); // Altro Ruolo
        forceAddMID.put("13", 1); // Dati Personali
        forceAddMID.put("14", 1); // Nome
        forceAddMID.put("15", 1); // Cognome 
        forceAddMID.put("63", 1); // Ente
        forceAddMID.put("64", 1); // Titolo
        forceAddMID.put("65", 1); // Titolo
        forceAddMID.put("66", 1); // Tipo
        forceAddMID.put("148", 1); // Numero Matricola
        
        try {
            selectedClassificationList = new TreeMap<String, String>();
            
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc;
           
            File urlf = new File(filepath);
            doc = dBuilder.parse(urlf);

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
    
    public void setSessionMetadataFile(String panelname){
         try {
            DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc;
            
            File f = new File(Globals.BACKUP_METADATA);
            //File s = new File(Globals.SESSION_METADATA);
            File s = new File(Globals.DUPLICATION_FOLDER_SEP + panelname);
            FileUtils.copyFile(f, s);
            
            XPathFactory factory = XPathFactory.newInstance();
	    XPath xpath = factory.newXPath();
            
            File impmetadata = new File(Globals.SELECTED_FOLDER_SEP + Globals.IMP_EXP_METADATA);
            doc = dBuilder.parse(impmetadata);
                      
            String expression = "//*[local-name()='contribute']";
            NodeList nodeList = (NodeList) xpath.evaluate(expression, doc, XPathConstants.NODESET);
            
            for (int i=0;i<nodeList.getLength()-1;i++){
                addContributorToMetadata(panelname);
            }
            
            expression = "//*[local-name()='taxonpath']";
            nodeList = (NodeList) xpath.evaluate(expression, doc, XPathConstants.NODESET);
            
            for (int i=0;i<nodeList.getLength()-1;i++){
                addClassificationToMetadata(panelname);
            }
        } 
        catch (ParserConfigurationException ex) {
            logger.error(ex.getMessage());
        } catch (XPathExpressionException ex) {
            logger.error(ex.getMessage());
        } catch (SAXException ex) {
            logger.error(ex.getMessage());
        } catch (IOException ex) {
            logger.error(ex.getMessage());
        } 
    }
    
    private void addContributorToMetadata(String panelname){
        try {
            DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc;
            
            XPathFactory factory = XPathFactory.newInstance();
	    XPath xpath = factory.newXPath();
            File backupmetadata = new File(Globals.DUPLICATION_FOLDER_SEP + "session" + panelname);
            //File backupmetadata = new File(Globals.SESSION_METADATA);
            doc = dBuilder.parse(backupmetadata);
            
            String expression = "//*[@ID='11']";
            NodeList nodeList = (NodeList) xpath.evaluate(expression, doc, XPathConstants.NODESET);
            int maxseq = 0;
            int tmpseq = 0;
            
            for (int i = 0; i < nodeList.getLength(); i++) {
               NamedNodeMap attr = nodeList.item(i).getAttributes();
               Node nodeAttr = attr.getNamedItem("sequence");
               tmpseq = Integer.parseInt(nodeAttr.getNodeValue());
            
               if (tmpseq > maxseq){
                   maxseq = tmpseq;
               }
	    }
            maxseq++;
            
            Node newNode = nodeList.item(0).cloneNode(true);
            Element nodetocopy = (Element) newNode;
            NamedNodeMap attr = nodeList.item(0).getAttributes();
            Node nodeAttr = attr.getNamedItem("sequence");
            nodeAttr.setTextContent(Integer.toString(maxseq));
            
            Node copyOfn = doc.importNode(nodetocopy, true);
            nodeList.item(0).getParentNode().appendChild(copyOfn);
            
            XMLUtil.xmlWriter(doc, Globals.DUPLICATION_FOLDER_SEP + "session" + panelname);
        } catch (ParserConfigurationException ex) {
            logger.error(ex.getMessage());
        } catch (SAXException ex) {
            logger.error(ex.getMessage());
        } catch (IOException ex) {
            logger.error(ex.getMessage());
        } catch (XPathExpressionException ex) {
            logger.error(ex.getMessage());
        }
    }
    
    
    private void addClassificationToMetadata(String panelname){
        try {
            DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc;
            
            XPathFactory factory = XPathFactory.newInstance();
	    XPath xpath = factory.newXPath();
            
            //File backupmetadata = new File(Globals.SESSION_METADATA);
            File backupmetadata = new File(Globals.DUPLICATION_FOLDER_SEP + "session" +panelname);
            doc = dBuilder.parse(backupmetadata);
            
            String expression = "//*[@ID='22']";
            NodeList nodeList = (NodeList) xpath.evaluate(expression, doc, XPathConstants.NODESET);
            int maxseq = 0;
            int tmpseq;
            
            for (int i = 0; i < nodeList.getLength(); i++) {
               NamedNodeMap attr = nodeList.item(i).getAttributes();
               Node nodeAttr = attr.getNamedItem("sequence");
               tmpseq = Integer.parseInt(nodeAttr.getNodeValue());
            
               if (tmpseq > maxseq){
                   maxseq = tmpseq;
               }
	    }
            maxseq++;
            
            Node newNode = nodeList.item(0).cloneNode(true);
            Element nodetocopy = (Element) newNode;
            NamedNodeMap attr = nodeList.item(0).getAttributes();
            Node nodeAttr = attr.getNamedItem("sequence");
            nodeAttr.setTextContent(Integer.toString(maxseq));
            
            Node copyOfn = doc.importNode(nodetocopy, true);
            nodeList.item(0).getParentNode().appendChild(copyOfn);
            
            Element root = doc.getDocumentElement();
            NodeList firstlevelnodes = root.getChildNodes();
            
            for (int i=0; i<firstlevelnodes.getLength();i++){
                if (firstlevelnodes.item(i).getNodeType() == Node.ELEMENT_NODE) {
                    Element node = (Element) firstlevelnodes.item(i);
                    Integer sequence = Integer.parseInt(node.getAttribute("sequence"));
                    if (!node.getAttribute("ID").equals("22") && sequence>=maxseq){
                        node.setAttribute("sequence", Integer.toString(sequence +1));
                    }
                }
            }

            XMLUtil.xmlWriter(doc, Globals.DUPLICATION_FOLDER_SEP + "session" + panelname);
        } catch (ParserConfigurationException ex) {
            logger.error(ex.getMessage());
        } catch (SAXException ex) {
            logger.error(ex.getMessage());
        } catch (IOException ex) {
            logger.error(ex.getMessage());
        } catch (XPathExpressionException ex) {
            logger.error(ex.getMessage());
        }
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
    public void create_metadata_view(Map<Object, Metadata> submetadatas, JPanel parent, int level, final String panelname) throws Exception {
        ResourceBundle bundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);
        int lenght = submetadatas.size();
        int labelwidth = 220;
        int i = 0;
        JButton addcontribute = null;
        
        
        for (Map.Entry<Object, Metadata> kv : submetadatas.entrySet()) {
            ArrayList<Component> tabobjects = new ArrayList<Component>();
            
            if (kv.getValue().MID == 17 || kv.getValue().MID == 23 || kv.getValue().MID == 18 || kv.getValue().MID == 137) {
                continue;
            }

            //Crea un jpanel nuovo e fa appen su parent
            JPanel innerPanel = new JPanel(new MigLayout("fillx, insets 1 1 1 1"));
            innerPanel.setName("pannello" + level + i);
            
            i++;
            String datatype = kv.getValue().datatype.toString();

            if (kv.getValue().MID == 45) {
                JPanel choice = new JPanel(new MigLayout());
                JComboBox combo = addClassificationChoice(choice, kv.getValue().sequence, panelname);
                
                JLabel labelc = new JLabel();
                labelc.setText(Utility.getBundleString("selectclassif",bundle));
                labelc.setPreferredSize(new Dimension(100, 20));
                
                choice.add(labelc);
    
                if (classificationAddButton == null){
                    choice.add(combo, "width 100:600:600");
                
                    classificationAddButton = new JButton("+");
          
                    classificationAddButton.addActionListener(new ActionListener()
                    {
                        @Override
                        public void actionPerformed(ActionEvent event)
                        {
                            BookImporter.getInstance().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                            addClassificationToMetadata(panelname);
                            BookImporter.getInstance().refreshMetadataTab(false, panelname);
                            BookImporter.getInstance().setCursor(null);
                        }
                    });
    
                    choice.add(classificationAddButton, "width :50:");
                }
                else{
                    choice.add(combo, "wrap,width 100:700:700");
                }
                parent.add(choice, "wrap,width 100:700:700");
                classificationMID = kv.getValue().MID;
                
                innerPanel.setName("ImPannelloClassif---"+kv.getValue().sequence);
                try{
                   addClassification(innerPanel, classificationMID, kv.getValue().sequence, panelname);
                }
                catch (Exception ex) {
                    logger.error("Errore nell'aggiunta delle classificazioni");
                }
                parent.add(innerPanel, "wrap, growx");
                BookImporter.policy.addIndexedComponent(combo);
                continue;
            }

            if (datatype.equals("Node")) {
                JLabel label = new JLabel();
                label.setText(kv.getValue().description);
                label.setPreferredSize(new Dimension(100, 20));

                int size = 16 - (level * 2);
                Font myFont = new Font("MS Sans Serif", Font.PLAIN, size);
                label.setFont(myFont);
                
                if (Integer.toString(kv.getValue().MID).equals("11") && addcontribute == null){
                    addcontribute = new JButton("+");
          
                    addcontribute.addActionListener(new ActionListener()
                    {
                        @Override
                        public void actionPerformed(ActionEvent event)
                        {
                            BookImporter.getInstance().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                            addContributorToMetadata(panelname);
                            BookImporter.getInstance().refreshMetadataTab(false, panelname);
                            BookImporter.getInstance().setCursor(null);
                        }
                    });
                    
                    JPanel temppanel = new JPanel(new MigLayout());
                    temppanel.add(label, " width :200:");
                    temppanel.add(addcontribute, "width :50:");
                    innerPanel.add(temppanel, "wrap, growx");
                    
                }
                else{
                    innerPanel.add(label, "wrap, growx");
                }
            } else {
                String title = "";

                if (kv.getValue().mandatory.equals("Y") || kv.getValue().MID == 14 || kv.getValue().MID == 15) {
                    title = kv.getValue().description + " *";
                } else {
                    title = kv.getValue().description;
                }

                innerPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), title, TitledBorder.LEFT, TitledBorder.TOP));

                if (datatype.equals("Vocabulary")) {
                    TreeMap<String, String> entryCombo = new TreeMap<String, String>();
                    int index = 0;
                    String selected = null;
                    
                    if(!Integer.toString(kv.getValue().MID).equals("8"))
                        entryCombo.put(Utility.getBundleString("comboselect",bundle),Utility.getBundleString("comboselect",bundle));
                    
                    for (Map.Entry<String,TreeMap<String,VocEntry>> vc : vocabularies.entrySet()) {
                        String tempmid = Integer.toString(kv.getValue().MID);
                        
                        if (Integer.toString(kv.getValue().MID_parent).equals("11") || Integer.toString(kv.getValue().MID_parent).equals("13")){
                            String[] testmid = tempmid.split("---");
                            tempmid = testmid[0];
                        }
                        
                        if (vc.getKey().equals(tempmid)) {
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
                    
                    if (Integer.toString(kv.getValue().MID_parent).equals("11") || Integer.toString(kv.getValue().MID_parent).equals("13")){
                        voc.setName("MID_" + Integer.toString(kv.getValue().MID)+"---"+kv.getValue().sequence);
                    }
                    else{
                        voc.setName("MID_" + Integer.toString(kv.getValue().MID));
                    }
                    
                    if(Integer.toString(kv.getValue().MID).equals("8") && selected==null)
                        selected = "44";
                                        
                    selected = (selected==null)?Utility.getBundleString("comboselect",bundle):selected;
                    
                    for (int k = 0; k < voc.getItemCount(); k++) {
                        Map.Entry<String, String> el = (Map.Entry<String, String>) voc.getItemAt(k);
                        if(el.getValue().equals(selected))
                            voc.setSelectedIndex(k);
                    }
                    
                    voc.setPreferredSize(new Dimension(150, 30));
                    innerPanel.add(voc, "wrap, width :400:");
                    tabobjects.add(voc);
                } else if (datatype.equals("CharacterString") || datatype.equals("GPS")) {
                    final JTextArea textField = new javax.swing.JTextArea();
                    
                    if (Integer.toString(kv.getValue().MID_parent).equals("11") || Integer.toString(kv.getValue().MID_parent).equals("13")){
                        textField.setName("MID_" + Integer.toString(kv.getValue().MID)+"---"+kv.getValue().sequence);
                    }
                    else{
                        textField.setName("MID_" + Integer.toString(kv.getValue().MID));
                    }
                    
                    textField.setPreferredSize(new Dimension(230, 0));
                    textField.setText(kv.getValue().value);
                    textField.setLineWrap(true);
                    textField.setWrapStyleWord(true);
                    
                    innerPanel.add(textField, "wrap, width :300:");
                    
                    textField.addKeyListener(new KeyAdapter() {
                        @Override
                        public void keyPressed(KeyEvent e) {
                            if (e.getKeyCode() == KeyEvent.VK_TAB) {
                                if (e.getModifiers() > 0) {
                                    textField.transferFocusBackward();
                                } else {
                                    textField.transferFocus();
                                }
                                e.consume();
                            }
                        }
                    });
                    
                    tabobjects.add(textField);
                } else if (datatype.equals("LangString")) {
                    JScrollPane inner_scroll = new javax.swing.JScrollPane();
                    inner_scroll.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
                    inner_scroll.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
                    inner_scroll.setPreferredSize(new Dimension(240, 80));
                    inner_scroll.setName("langStringScroll");
                    final JTextArea jTextArea1 = new javax.swing.JTextArea();
                    jTextArea1.setName("MID_" + Integer.toString(kv.getValue().MID));
                    jTextArea1.setText(kv.getValue().value);
                    
                    jTextArea1.setSize(new Dimension(350, 70));
                    jTextArea1.setLineWrap(true);
                    jTextArea1.setWrapStyleWord(true);

                    inner_scroll.setViewportView(jTextArea1);
                    innerPanel.add(inner_scroll, "width :300:");

                    //Add combo language box
                    JComboBox voc = getComboLangBox(kv.getValue().language);
                    voc.setName("MID_" + Integer.toString(kv.getValue().MID) + "_lang");

                    voc.setPreferredSize(new Dimension(200, 20));
                    innerPanel.add(voc, "wrap, width :300:");
                    
                    jTextArea1.addKeyListener(new KeyAdapter() {
                        @Override
                        public void keyPressed(KeyEvent e) {
                            if (e.getKeyCode() == KeyEvent.VK_TAB) {
                                if (e.getModifiers() > 0) {
                                    jTextArea1.transferFocusBackward();
                                } else {
                                    jTextArea1.transferFocus();
                                }
                                e.consume();
                            }
                        }
                    });
                    tabobjects.add(jTextArea1);
                    tabobjects.add(voc);
                } else if (datatype.equals("Language")) {
                    final JComboBox voc = getComboLangBox(kv.getValue().value);
                    voc.setName("MID_" + Integer.toString(kv.getValue().MID));

                    
                    voc.setPreferredSize(new Dimension(150, 20));
                    voc.setBounds(5, 5, 150, 20);
                    innerPanel.add(voc, "wrap, width :500:");
                    
                    //BookImporter.policy.addIndexedComponent(voc);
                    tabobjects.add(voc);
                            
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
                    innerPanel.add(voc, "wrap, width :300:");
                    //BookImporter.policy.addIndexedComponent(voc);
                    tabobjects.add(voc);
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
                    innerPanel.add(voc, "wrap, width :500:");
                    //BookImporter.policy.addIndexedComponent(voc);
                    tabobjects.add(voc);
                } else if (datatype.equals("DateTime")) {
                    final JXDatePicker datePicker = new JXDatePicker();
                    datePicker.setName("MID_" + Integer.toString(kv.getValue().MID));
                    
                    JPanel test = new JPanel(new MigLayout());
                    JLabel lbefore = new JLabel(Utility.getBundleString("beforechristlabel",bundle));
                    JCheckBox beforechrist = new JCheckBox();
                    beforechrist.setName("MID_" + Integer.toString(kv.getValue().MID) + "_check");
                    
                    if (kv.getValue().value != null) {
                        try {
                            if (kv.getValue().value.charAt(0) == '-'){
                                beforechrist.setSelected(true);
                            }
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                            Date date1 = sdf.parse(kv.getValue().value);
                            datePicker.setDate(date1);
                        } catch (Exception e) {
                            //Console.WriteLine("ERROR import date:" + ex.Message);
                        }
                    }
                    
                    test.add(datePicker, "width :200:");
                    test.add(lbefore, "gapleft 30");
                    test.add(beforechrist, "wrap");
                    
                    
                    innerPanel.add(test,"wrap");
                }
            }

            //Recursive call
            create_metadata_view(kv.getValue().submetadatas, innerPanel, level + 1, panelname);

            if (kv.getValue().editable.equals("Y") || (datatype.equals("Node") && kv.getValue().hidden.equals("0"))) {
                parent.add(innerPanel, "wrap, growx");
                
                for (Component tabobject : tabobjects) {
                    BookImporter.policy.addIndexedComponent(tabobject); 
                }
            }
        }
    }

    /**
     * Metodo adibito all'esportazione dei metadati sul file definito dal
     * parametro
     *
     * @param xmlFile File in cui esportare i metadati
     * @param checkMandatory
     * @return
     */
    public String check_and_save_metadata(String xmlFile,Boolean toFile, boolean checkMandatory) {
        String result = "";

        result = check_and_save_metadata_recursive(BookImporter.getInstance().getMetadata(), checkMandatory);

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
    private void setOEFOS(JTree e, String sequence, String panelname) throws Exception {
        try {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.getLastSelectedPathComponent();
            String completePath = "";

            BookImporter.getInstance().createComponentMap(BookImporter.getInstance().metadatapanels.get(panelname).getPanel());
            Component controls = BookImporter.getInstance().getComponentByName("classification_path---"+sequence);

            //Se e' una foglia aggiorna il path nell'interfaccia e oefos_path
            if (node != null) {
                TreePath selpath = new TreePath(node.getPath());
                e.setSelectionPath(selpath);

                Object[] nodes = selpath.getPath();
                
                TreeMap<Integer,Integer> single_path = new TreeMap<Integer,Integer>();
                
                for (int i = 1; i < nodes.length; i++) {
                    Object nodeInfo = nodes[i];
                    DefaultMutableTreeNode nodeC = (DefaultMutableTreeNode) nodeInfo;
                    ClassNode c = (ClassNode) nodeC.getUserObject();
                    single_path.put(i, Integer.parseInt(c.getKey().toString()));

                    completePath += nodes[i].toString();
                    completePath += (i != nodes.length - 1) ? "/" : "";
                }
                
                this.oefos_path.put(sequence, single_path);
            } else {
                this.oefos_path.put(sequence, null);
            }

            JLabel label = (JLabel) controls;
            if (completePath.length() > 120){
                label.setText(completePath.substring(0, 120));
            }
            else{
                label.setText(completePath);
            }
                    
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
    
    private void recursiveOefosTreeviewBuild(DefaultMutableTreeNode nodes, TreeMap<Object, Taxon> taxons, String sequence) throws Exception {
        try {
            for (Map.Entry<Object, Taxon> kv : taxons.entrySet()) {
                ClassNode iNode = new ClassNode("" + kv.getKey(), kv.getValue().upstream_identifier + ": " + kv.getValue().description);
              
                ClassMutableNode inner = new ClassMutableNode(iNode);
                nodes.add(inner);

                if (this.oefos_path.get(sequence) != null) {
                    if (this.oefos_path.get(sequence).containsValue(kv.getValue().TID)) {
                        selected = inner;
                    }
                }

                recursiveOefosTreeviewBuild(inner, kv.getValue().subtaxons, sequence);
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
    private void addClassification(JPanel innerPanel, Integer kv, final String sequence, final String panelname) throws Exception {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);

            DefaultMutableTreeNode hierarchy = new DefaultMutableTreeNode("root");
            selected = null;
            String selectedPath = "";
            
            String link = selectedClassificationList.get(sequence);
            
            recursiveOefosTreeviewBuild(hierarchy, oefos.get(link), sequence);
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

            tree.setName("MID_" + Integer.toString(kv)+"---"+sequence);
            tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

            tree.addTreeSelectionListener(new TreeSelectionListener() {

                public void valueChanged(TreeSelectionEvent e) {
                    try {
                        setOEFOS(tree, sequence, panelname);
                    } catch (Exception ex) {
                        logger.error(ex.getMessage());
                    }
                }
            });

            javax.swing.JScrollPane tree_scroller = new javax.swing.JScrollPane();
            tree_scroller.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            tree_scroller.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
            tree_scroller.setViewportView(tree);
            tree_scroller.setBorder(null);

            innerPanel.add(tree_scroller, "wrap, growx");

            JPanel iPanel = new JPanel(new MigLayout());
            iPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), Utility.getBundleString("choose2",bundle), TitledBorder.LEFT, TitledBorder.TOP));
            JLabel label = new JLabel();
            label.setName("classification_path"+"---"+sequence);
            label.setText(selectedPath);
            iPanel.add(label, "wrap, growx, height 30:30:30");

            innerPanel.add(iPanel, "wrap, width 100:700:700");
        } catch (Exception ex) {
            logger.error(ex.getMessage());
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
                            
                            String sequencemulti = "";
                            
                            if (MID_parent.equals("22") || MID_parent.equals("45")){
                                Node searchparent = iInnerNode;
                                boolean found = false;
                                
                                while (!found){
                                    Element x = (Element) searchparent.getParentNode();
                                    if (x.getAttribute("ID").equals("22")){
                                        sequencemulti = x.getAttribute("sequence");
                                        found = true;
                                    }   
                                    else{
                                        searchparent = searchparent.getParentNode();
                                    }
                                }
                            }
                            
                            //Add contributors management
                            if (MID_parent.equals("11") || MID_parent.equals("13")){
                                Node searchparent = iInnerNode;
                                boolean found = false;
                                
                                while (!found){
                                    Element x = (Element) searchparent.getParentNode();
                                    if (x.getAttribute("ID").equals("11")){
                                        sequencemulti = x.getAttribute("sequence");
                                        found = true;
                                    }   
                                    else{
                                        searchparent = searchparent.getParentNode();
                                    }
                                }
                            }
                                  
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
                                
                                Metadata t = new Metadata(Integer.parseInt(MID), mid_parent, description, datatype, editable, foxmlname, null, foxmlnamespace, mandatory, hidden,  sequencemulti);
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
            logger.error(ex.getMessage());
        }
    }

    private String check_and_save_metadata_recursive(Map<Object, Metadata> submetadatas, boolean checkMandatory) {
        ResourceBundle bundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);
        String error = "";

        for (Map.Entry<Object, Metadata> field : submetadatas.entrySet()) {
            if (!field.getValue().datatype.equals("Node") && field.getValue().editable.equals("Y")) {
                Component element = null;
                String midp = Integer.toString(field.getValue().MID_parent);
                
                if (midp.equals("11") || midp.equals("13"))
                    element = BookImporter.getInstance().getComponentByName("MID_" + Integer.toString(field.getValue().MID)+"---"+field.getValue().sequence);
                else{
                    element = BookImporter.getInstance().getComponentByName("MID_" + Integer.toString(field.getValue().MID));
                }
                
                if (element != null) {
                    if (field.getValue().datatype.equals("CharacterString") || field.getValue().datatype.equals("LangString") || field.getValue().datatype.equals("GPS")) {
                        JTextArea textTemp = (JTextArea) element;
                        field.getValue().value = textTemp.getText();
                        if (checkMandatory && field.getValue().value.length() < 1 && (field.getValue().mandatory.equals("Y") || field.getValue().MID == 14 || field.getValue().MID == 15)) {
                            error += Utility.getBundleString("error10",bundle) + " " + field.getValue().description.toString() + " " + Utility.getBundleString("error11",bundle) + "!\n";
                        }
                    }
                    if (field.getValue().datatype.equals("LangString")) {
                        Component combobox = BookImporter.getInstance().getComponentByName("MID_" + Integer.toString(field.getValue().MID) + "_lang");
                        JComboBox tmp = (JComboBox) combobox;

                        Map.Entry tmp2 = (Map.Entry) tmp.getSelectedItem();
                        field.getValue().language = tmp2.getKey().toString();
                    } else if (field.getValue().datatype.equals("DateTime")) {
                        Component combobox = BookImporter.getInstance().getComponentByName("MID_" + Integer.toString(field.getValue().MID) + "_check");
                        JCheckBox beforechrist = (JCheckBox) combobox;
                        
                        JXDatePicker datePicker = (JXDatePicker) element;
                        Date data = datePicker.getDate();

                        field.getValue().value = "";

                        if (data != null) {
                            Format formatter = new SimpleDateFormat("yyyy-MM-dd");
                            String stDate = formatter.format(data);
                            
                            if (!stDate.equals("")) {
                                if (beforechrist.isSelected()){
                                    stDate = "-"+stDate;
                                }
                                
                                field.getValue().value = stDate;
                            }
                        }
                    } else if (field.getValue().datatype.equals("Language") || field.getValue().datatype.equals("Boolean") || field.getValue().datatype.equals("License") || field.getValue().datatype.equals("Vocabulary")) {
                        Component combobox = null;
                        
                        if (midp.equals("11") || midp.equals("13"))
                            combobox = BookImporter.getInstance().getComponentByName("MID_" + Integer.toString(field.getValue().MID)+"---"+field.getValue().sequence);
                        else
                            combobox = BookImporter.getInstance().getComponentByName("MID_" + Integer.toString(field.getValue().MID));
                                                     
                        JComboBox tmp = (JComboBox) combobox;

                        Map.Entry tmp2 = (Map.Entry) tmp.getSelectedItem();
                        
                        if(field.getValue().datatype.equals("License") || field.getValue().datatype.equals("Vocabulary")){
                            //ResourceBundle tmpBundle = ResourceBundle.getBundle(Globals.RESOURCES, BookImporter.localConst, Globals.loader); 
                            ResourceBundle tmpBundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader); 
                            
                            if(checkMandatory && tmp2.getValue().toString().equals(Utility.getBundleString("comboselect",tmpBundle)) && field.getValue().mandatory.equals("Y"))
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

            error += check_and_save_metadata_recursive(field.getValue().submetadatas, checkMandatory);
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

    private void read_uwmetadata_recursive(Map<Object, Metadata> submetadatas, String xpath, NamespaceContextImpl nsmgr, Document nav) throws XPathExpressionException {
        for (Map.Entry<Object, Metadata> field : submetadatas.entrySet()) {
            String actXpath = "";
            if (field.getValue().MID_parent==11 || field.getValue().MID_parent==13){
                actXpath = xpath +"[@seq='"+field.getValue().sequence+"']/" + nsmgr.getPrefix(field.getValue().foxmlnamespace) + ":" + field.getValue().foxmlname;
            }
            else{
                actXpath = xpath + "/" + nsmgr.getPrefix(field.getValue().foxmlnamespace) + ":" + field.getValue().foxmlname;
            }

            if (!field.getValue().datatype.equals("Node") && field.getValue().editable.equals("Y")) {
                
                Element node = Utility.getXPathNode(actXpath, nsmgr, nav);

                if (node != null) {
                    if (field.getValue().datatype.equals("LangString")) {
                        field.getValue().value = node.getTextContent();
                        field.getValue().language = node.getAttribute("language");
                    } else if (field.getValue().datatype.equals("ClassificationSource")) {
                        XPath taxonpath = XPathFactory.newInstance().newXPath();
                        String expression = "//*[local-name()='taxonpath'][@seq='"+field.getValue().sequence+"']";;
                        NodeList nodeList = (NodeList) taxonpath.evaluate(expression, nav, XPathConstants.NODESET);
                        TreeMap<Integer, Integer> single_path = new TreeMap<Integer, Integer>();
                        
                        int countpaths = 1;
                        for (int j=0;j<nodeList.getLength();j++){
                            NodeList children = nodeList.item(j).getChildNodes();
                            
                            for (int i=0; i<children.getLength();i++){
                                Node nodetaxon = (Node) children.item(i);
                                if (nodetaxon.getNodeName().equals("ns7:source")){
                                    String link = Utility.getValueFromKey(this.classificationIDS, nodetaxon.getTextContent());
                                    this.selectedClassificationList.put(field.getValue().sequence,link);
                                }
                                if (nodetaxon.getNodeName().equals("ns7:taxon")){
                                    single_path.put(countpaths, Integer.parseInt(nodetaxon.getTextContent()));
                                    countpaths++;
                                }
                            }
                            this.oefos_path.put(field.getValue().sequence, single_path);
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
            boolean classificationtag = false;
            String expression = "//*[local-name()='classification']";
            XPath xpath = XPathFactory.newInstance().newXPath();
	 
            for (Map.Entry<Object, Metadata> field : submetadatas.entrySet()) {
                if (field.getValue().MID == 2) {
                    this.objectTitle = field.getValue().value;
                }
                if (field.getValue().datatype.equals("Node")) {
                    if (field.getValue().MID == 45 && this.oefos_path.get(field.getValue().sequence) == null) {
                        continue;
                    } else if (field.getValue().MID == 45 && this.oefos_path.get(field.getValue().sequence).size() < 1) {
                        continue;
                    } else {
                        //Set classification tag
                        NodeList nodeList = (NodeList) xpath.evaluate(expression, w, XPathConstants.NODESET);
                       
                        String book = Utility.getValueFromKey(metadata_namespaces, field.getValue().foxmlnamespace);
                        Element link = w.createElement(book + ":" + field.getValue().foxmlname);
                        if (!field.getValue().sequence.equals("") && field.getValue().MID != 45)
                            e.setAttribute("seq", field.getValue().sequence);
                        
                        if ((field.getValue().MID != 22 && field.getValue().MID != 45) || nodeList.getLength()<=0)
                            e.appendChild(link);
                        else{
                            if (field.getValue().MID == 45){
                                nodeList.item(0).appendChild(link);
                            }
                        }
                        create_uwmetadata_recursive(w, link, field.getValue().submetadatas, defValues, pagenum, collTitle);
                    }
                } else if (field.getValue().MID == 46) {
                    //Set source tag
                    if (this.oefos_path.get(field.getValue().sequence).size() > 0) {
                        String book = Utility.getValueFromKey(metadata_namespaces, field.getValue().foxmlnamespace);
                        Element link = w.createElement(book + ":" + field.getValue().foxmlname);
                        
                        String classiflink = this.selectedClassificationList.get(field.getValue().sequence);
                        String CID = this.classificationIDS.get(classiflink);
                        
                        link.setTextContent(CID);
                        
                        e.appendChild(link);
                    }
                } else if (field.getValue().MID == 47 && this.oefos_path.get(field.getValue().sequence).size() > 0) {
                    //Set taxonpaths tags
                    for (Map.Entry<Integer, Integer> iField : this.oefos_path.get(field.getValue().sequence).entrySet()) {
                        String book = Utility.getValueFromKey(metadata_namespaces, field.getValue().foxmlnamespace);
                        Element link = w.createElement(book + ":" + field.getValue().foxmlname);
                      
                        link.setTextContent(Integer.toString(iField.getValue()));
                        link.setAttribute("seq", Integer.toString(iField.getKey() - 1));
                        e.setAttribute("seq", field.getValue().sequence);
                        e.appendChild(link);
                    }
                } else if (field.getValue().editable.equals("Y")) {
                    String value = "";

                    if (field.getValue().value != null) {

                        if(field.getValue().MID==2 && !collTitle.equals(""))
                            value = collTitle;
                        else{
                            if (field.getValue().MID == 2 && pagenum > 0) {
                                value = field.getValue().value + " - " + Utility.getBundleString("seite",bundle) + " " + Integer.toString(pagenum);
                            } else {
                                value = field.getValue().value;
                            }
                        }
                    } else if (defValues != null) {
                        if (defValues.containsKey(field.getValue().foxmlname)) {
                            value = defValues.get(field.getValue().foxmlname);
                        } else {
                            value = "en";
                        }
                    }
               
                    if (value.length() > 0) {
                        String book = Utility.getValueFromKey(metadata_namespaces, field.getValue().foxmlnamespace);
                        Element link = w.createElement(book + ":" + field.getValue().foxmlname);

                        if (field.getValue().datatype.equals("LangString")) {
                            link.setAttribute("language", field.getValue().language);
                        }

                        link.setTextContent(value);
                        if (!field.getValue().sequence.equals(""))
                            e.setAttribute("seq", field.getValue().sequence);
                        
                        e.appendChild(link);
                    }
                } else if (field.getValue().editable.equals("N") && defValues != null) {
                    String value = null;
                    if (field.getValue().foxmlname.equals("location")) {
                        value = SelectedServer.getInstance(null).getPhaidraURL() + "/" + defValues.get("identifier");
                        value = defValues.get("identifier");
                    } else {
                        value = defValues.get(field.getValue().foxmlname);
                    }

                    String book = Utility.getValueFromKey(metadata_namespaces, field.getValue().foxmlnamespace);
                    Element link = w.createElement(book + ":" + field.getValue().foxmlname);
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
        LinkedHashMap<String, String> comboLang = new LinkedHashMap<String, String>();
        int sel_index = 0;
        int index = 0;

        for (Map.Entry<String, String> vc : languages.entrySet()) {
            comboLang.put(vc.getValue(), vc.getKey());

            if (Globals.CURRENT_LOCALE.getLanguage().equals(vc.getValue()) && value == null) {
                sel_index = index;
            } else if (value != null) {
                if (value.equals(vc.getValue())) {
                    sel_index = index;
                }
            }
            index++;
        }

        final ComboMapImpl model = new ComboMapImpl();
        model.putAllLinked(comboLang);

        JComboBox result = new javax.swing.JComboBox(model);

        result.setSelectedIndex(sel_index);
        model.specialRenderCombo(result);

        return result;
    }
}
