/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package it.imtech.upload;

import at.ac.univie.phaidra.api.Phaidra;
import at.ac.univie.phaidra.api.objekt.Book;
import at.ac.univie.phaidra.api.objekt.Collection;
import it.imtech.bookimporter.BookImporter;
import it.imtech.dialogs.ConfirmDialog;
import it.imtech.globals.Globals;
import it.imtech.metadata.MetaUtility;
import it.imtech.utility.Utility;
import it.imtech.xmltree.XMLTree;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.ResourceBundle;
import java.util.logging.Level;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author mauro
 */
public class UploadProgress extends javax.swing.JPanel implements java.beans.PropertyChangeListener {
    
    //Gestore dei log
    private final static Logger logger = Logger.getLogger(UploadProgress.class);
    
    private UplTask task;
    protected static JFrame frame = null;
    protected static Phaidra phaidra = null;
    protected static String lock = null;
    protected static String path = null;
    protected static String user = null;
    protected String type = null;

    protected String uploadObjPID = null;
    class UplTask extends SwingWorker<Void, Void> {

        ResourceBundle bundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);

        /**
         * Task eseguito in background, crea una collezione o un libro.
         *
         * @return
         */
        @Override
        public Void doInBackground() {
            try {
                logger.info("Starting upload...");
                if (Utility.countXmlTreeLeaves() > 0) {
                    //Crea oggetto ImObject per interfacciarsi a Phaidra
                    ImObject obj = new ImObject(phaidra, lock, user);

                    //Conta i passi totali da compiere
                    int total = obj.countTotal();

                    //Lista dei nodi dell'albero (oggetti da uploadare)
                    NodeList nodeLst = getNodeList();

                    if (Globals.TYPE_BOOK == Globals.BOOK) {
                        logger.info("Create a book");
                        createBook(obj, total, nodeLst);
                    } else if(Globals.TYPE_BOOK == Globals.COLLECTION){
                        logger.info("Create a collection");
                        if (XMLTree.getSingleMetadataFiles().size()>0){
                            XMLTree.exportBookStructureToFile(Globals.SELECTED_FOLDER_SEP);
                        }
                        
                        createCollection(obj, total, nodeLst);
                    } else if(Globals.TYPE_BOOK == Globals.SINGLE_VIDEO){
                        logger.info("Create a single video");
                        jButton2.setText(Utility.getBundleString("openvideourl",bundle));
                        createSingleVideo(XMLTree.getVideoPath(), Utility.getMimeType(XMLTree.getVideoPath()), this, obj);
                    }
                } else {
                    String pbundle = Globals.TYPE_BOOK == Globals.BOOK ? "book" : "coll";
                    addUploadInfoText(Utility.getBundleString("uperror" + pbundle + "1",bundle));
                    jProgressBar1.setValue(100);
                    setCursor(null);
                }
            } catch (Exception ex) {
                addUploadInfoText(Utility.getBundleString("uplexception",bundle) + ":" + ex.getMessage());
                logger.error(Utility.getBundleString("uplexception",bundle) + ":" + ex.getMessage());
                JOptionPane.showMessageDialog(new Frame(), Utility.getBundleString("uplexception",bundle));
            }

            return null;
        }

        /**
         * Metodo adibito alla creazione di una collezione a partire da un
         * insieme di nodi del collection structure
         *
         * @param obj Oggetto interfaccia per Phaidra
         * @param total Totale oggetti da caricare
         * @param nodeLst Lista di nodi da caricare
         * @throws Exception
         */
        private void createCollection(ImObject obj, int total, NodeList nodeLst) throws Exception {
            int progress = 0;
            int percent = 0;
            Collection coll;
            
            //Creo la collezione principale
            try{
                coll = obj.createCollection("Java Collection");
                uploadObjPID = coll.getPID();
            }
            catch (Exception ex) {
                String exc = (ex.getMessage()!=null)?ex.getMessage():"";
                throw new Exception(Utility.getBundleString("capigetpid",bundle)+" "+exc);
            }
            
            updateProgress(progress, total, Utility.getBundleString("clogging10",bundle) + ":" + uploadObjPID);
            
            //Aggiungo Metadati
            if (uploadObjPID != null) {
                setTextField(path);

                try{
                    BookImporter.getInstance().metadata = MetaUtility.getInstance().metadata_reader(Globals.DUPLICATION_FOLDER_SEP + "session" + BookImporter.mainpanel);
                    BookImporter.getInstance().createComponentMap(BookImporter.getInstance().metadatapanels.get(BookImporter.mainpanel).getPanel());
                    MetaUtility.getInstance().check_and_save_metadata(Globals.SELECTED_FOLDER_SEP + BookImporter.mainpanel, false, true, BookImporter.mainpanel);
                
                    coll.addMetadata(obj.addPhaidraMetadata(uploadObjPID,"", BookImporter.mainpanel));
                
                
                }
                catch (Exception ex) {
                    String exc = (ex.getMessage()!=null)?ex.getMessage():"";
                    throw new Exception(Utility.getBundleString("cpiaddmeta",bundle)+" "+exc);
                }
                
                updateProgress(progress, total, Utility.getBundleString("clogging6",bundle) + ":" + uploadObjPID);

                //Creo Sottocollezioni ed Elementi
                try{
                     ArrayList<String> members = createElements(obj, progress, total, nodeLst);
                     boolean result = obj.addCollMembers(coll, members);
                }
                catch (Exception ex) {
                    String exc = (ex.getMessage()!=null)?ex.getMessage():"";
                    throw new Exception(Utility.getBundleString("cpiaddpages",bundle)+" "+exc);
                }
             
                addUploadInfoText(Utility.getBundleString("clogging4",bundle) + Utility.getBundleString("clogging5",bundle) + ":" + uploadObjPID);

                
                //Nel caso di pubblicazione postposta
                try{
                    if (lock != null) {
                        if (!lock.equals("")) {
                            //coll.addDatastreamContent("RIGHTS", "text/xml", obj.create_rights(lock), "RIGHTS", "X");
                            coll.grantUsername(user, lock);
                            progress = updateProgress(progress, total, Utility.getBundleString("logging9",bundle) + ":" + uploadObjPID);
                        }
                    }
                }
                catch (Exception ex) {
                    String exc = (ex.getMessage()!=null)?ex.getMessage():"";
                    throw new Exception(Utility.getBundleString("cpiaddblock",bundle)+" "+exc);
                }
                
                //Salvo collezione principale
                try{
                    coll.save();
                }
                catch (Exception ex) {
                    String exc = (ex.getMessage()!=null)?ex.getMessage():"";
                    throw new Exception(Utility.getBundleString("cpisavebook",bundle)+" "+exc);
                }
                
                if (task.isCancelled()){
                    String message = Utility.getBundleString("uploadProgress11",bundle) + ": " + uploadObjPID;
                    JOptionPane.showMessageDialog(new Frame(), message);
                    setCursor(null);
                    jButton1.setEnabled(true);
                }
                else{
                    addUploadInfoText(Utility.getBundleString("logging12",bundle) + ":" + uploadObjPID);
                    jButton2.setEnabled(true);
                    JOptionPane.showMessageDialog(new Frame(), Utility.getBundleString("cuploadterm",bundle) + ": " + uploadObjPID);
                }
                jButton1.setEnabled(true);
            }
        }
        
        private void createSingleVideo(String path, String mimetype, UploadProgress.UplTask task, ImObject obj) throws Exception{
            File file = new File(path);
            
            if(file.isFile()) {
                
                logger.info("Start upload video with PID: " + uploadObjPID);
                addUploadInfoText(Utility.getBundleString("logging15",bundle));
                
                setTextField(path);
                
                //call api method
                uploadObjPID = obj.createVideo(path, mimetype, task, BookImporter.mainpanel);
                
                jButton2.setEnabled(true);
                addUploadInfoText(Utility.getBundleString("logging17",bundle) + uploadObjPID);
                String message = Utility.getBundleString("uploadProgress12",bundle) + ": " 
                                 + uploadObjPID + " " + Utility.getBundleString("uploadProgress2",bundle);
                JOptionPane.showMessageDialog(new Frame(), message);
            } else {
                addUploadInfoText(Utility.getBundleString("logging16",bundle));
                logger.error("The file in this path is not valid");
            }
        }

        /**
         * Carica un libro in Phaidra utilizzando l'oggetto ImObject
         * @param obj Oggetto interfaccia per Phaidra 
         * @param total Numero di passi da compiere per la progress bar
         * @param nodeLst Nodi (Capitoli, Pagine) da caricare in Phaidra
         * @throws Exception 
         */
        private void createBook(ImObject obj, int total, NodeList nodeLst) throws Exception {
            int progress = 0;
            int percent = 0;
            Book book;
            
            try{
            //Creo il libro mediante API phaidra
            try{
                book = obj.createBook();
                uploadObjPID = book.getPID();
                addUploadInfoText(Utility.getBundleString("uploadProgress4", bundle) + ": " + uploadObjPID);
            }
            catch (Exception ex) {
                String exc = (ex.getMessage()!=null)?ex.getMessage():"";
                throw new Exception(Utility.getBundleString("apigetpid",bundle)+" "+exc);
            }
            
            //Aggiungo il file pdf
            if (uploadObjPID != null) {
                setTextField(path);
                
                try{
                    addUploadInfoText(Utility.getBundleString("uploadProgress5", bundle));
                    book.addPDF(path);
                }
                catch (Exception ex) {
                    String exc = (ex.getMessage()!=null)?ex.getMessage():"";
                    throw new Exception(Utility.getBundleString("apiaddpdf",bundle)+" "+exc);
                }
                
                progress = updateProgress(progress, total, Utility.getBundleString("logging10",bundle) + ":" + uploadObjPID);

                //Aggiungo i metadati
                try{
                        addUploadInfoText(Utility.getBundleString("uploadProgress6", bundle));
                        book.addMetadata(obj.addPhaidraMetadata(uploadObjPID,"", BookImporter.mainpanel));
                }
                catch (Exception ex) {
                    String exc = (ex.getMessage()!=null)?ex.getMessage():"";
                    throw new Exception(Utility.getBundleString("apiaddmeta",bundle)+" "+exc);
                }
                
                progress = updateProgress(progress, total, Utility.getBundleString("logging3",bundle) + ":" + uploadObjPID);

                //Aggiungo pagine e capitoli
                try{
                        addUploadInfoText(Utility.getBundleString("uploadProgress7", bundle));
                        progress = createPages(uploadObjPID, obj, progress, total, nodeLst, book);
                }
                catch (Exception ex) {
                    String exc = (ex.getMessage()!=null)?ex.getMessage():"";
                    throw new Exception(Utility.getBundleString("apiaddpages",bundle)+" "+exc);
                }
                
                //Aggiungo contenuto OCR per il libro
                try{
                    if (BookImporter.getInstance().ocrBoxIsChecked()) {
                    	addUploadInfoText(Utility.getBundleString("uploadProgress8", bundle));
                        String ocr = obj.getFulltext();
                        book.addDatastreamContent("FULLTEXT", "text/plain", ocr, "Fulltext of the Book", "M");

                        progress = updateProgress(progress, total, Utility.getBundleString("logging7",bundle) + ":" + uploadObjPID);
                    }
                }
                catch (Exception ex) {
                    String exc = (ex.getMessage()!=null)?ex.getMessage():"";
                    throw new Exception(Utility.getBundleString("apiaddocr",bundle)+" "+exc);
                }
                
                //Nel caso di pubblicazione postposta, aggiungo il blocco
                try{
                    if (lock != null) {
                        if (lock != "") {
                                addUploadInfoText(Utility.getBundleString("uploadProgress9", bundle));
                                book.grantUsername(user, lock);
                                progress = updateProgress(progress, total, Utility.getBundleString("logging9",bundle) + ":" + uploadObjPID);
                        }
                    }
                }
                catch (Exception ex) {
                    String exc = (ex.getMessage()!=null)?ex.getMessage():"";
                    throw new Exception(Utility.getBundleString("apiaddblock",bundle)+" "+exc);
                }
                
                //Salvo il libro
                try{
                    addUploadInfoText(Utility.getBundleString("uploadProgress10", bundle));
                    book.save();
                }
                catch (Exception ex) {
                    String exc = (ex.getMessage()!=null)?ex.getMessage():"";
                    throw new Exception(Utility.getBundleString("apisavebook",bundle)+" "+exc);
                }

                if (task.isCancelled()){
                    String message = Utility.getBundleString("uploadProgress11",bundle) + ": " + uploadObjPID;
                    JOptionPane.showMessageDialog(new Frame(), message);
                    setCursor(null);
                    jButton1.setEnabled(true);
                }
                else{
                    addUploadInfoText(Utility.getBundleString("logging12",bundle) + ":" + uploadObjPID);
                    String message = Utility.getBundleString("buploadterm",bundle) + ": " + uploadObjPID;
                    jButton2.setEnabled(true);
                    JOptionPane.showMessageDialog(new Frame(), message);
                }
                jButton1.setEnabled(true);
                }
            }
            catch (Exception ex) {
                jButton1.setEnabled(true);
                setCursor(null);
                throw new Exception(ex.getMessage());
            }
        }
        
        private ArrayList<String> createElements(ImObject coll, int progress, int total, NodeList nodeLst) throws Exception {
            ArrayList<String> firsLevelEl = new ArrayList<String>();

            String flPID = null;
            String slPID = null;

            //Scorri collstructure per trovare coppie(Nome Capitolo, Nome File pagina)
            for (int s = 0; s < nodeLst.getLength() && !isCancelled(); s++) {
                if (nodeLst.item(s).getNodeType() == Node.ELEMENT_NODE) {
                    Element container = (Element) nodeLst.item(s);

                    //Se e' una foglia aggiungila alla collezione
                    if (!container.hasChildNodes()) {
                        if (container.hasAttribute("pid")) {
                            slPID = addElementToCollection(container, coll);

                            if (slPID != null) {
                                progress = updateProgress(progress, total, Utility.getBundleString("clogging1",bundle) + " " + type + ":" + slPID);
                                firsLevelEl.add(slPID);
                            } else {
                                progress = updateProgress(progress, total, Utility.getBundleString("clogging15",bundle) + ":" + slPID);
                            }
                        }
                        else
                            if(container.hasAttribute("name"))
                                addUploadInfoText(Utility.getBundleString("collempty",bundle)+": ("+container.getAttribute("name")+") "+ Utility.getBundleString("collnotup",bundle));
                            else
                                addUploadInfoText(Utility.getBundleString("collempty",bundle)+" "+Utility.getBundleString("collnotup",bundle));
                    } else {
                        ArrayList<String> SecondLevelEl = new ArrayList<String>();

                        //Crea collection con il nome definito nel collstructure
                        Collection flColl = coll.createCollection(container.getAttribute("name"));
                        flPID = flColl.getPID();

                        updateProgress(progress, total, Utility.getBundleString("clogging10",bundle) + ":" + flPID);

                        //Aggiungo i metadati per questa collezione
                        BookImporter.getInstance().metadata = MetaUtility.getInstance().metadata_reader(Globals.DUPLICATION_FOLDER_SEP + "session" + BookImporter.mainpanel);
                        BookImporter.getInstance().createComponentMap(BookImporter.getInstance().metadatapanels.get(BookImporter.mainpanel).getPanel());
                        MetaUtility.getInstance().check_and_save_metadata(Globals.SELECTED_FOLDER_SEP + BookImporter.mainpanel, false, true, BookImporter.mainpanel);
                
                        flColl.addMetadata(coll.addPhaidraMetadata(flPID, container.getAttribute("name"), BookImporter.mainpanel));

                        updateProgress(progress, total, Utility.getBundleString("clogging6",bundle) + ":" + flPID);

                        if (flPID != null) {
                            //Per ogni elemento del container esegui l'upload
                            NodeList leaves = container.getChildNodes();
                            for (int z = 0; z < leaves.getLength() && !isCancelled(); z++) {
                                if (leaves.item(z).getNodeType() == Node.ELEMENT_NODE) {
                                    Element page = (Element) leaves.item(z);

                                    slPID = addElementToCollection(page, coll);

                                    if (slPID != null) {
                                        progress = updateProgress(progress, total, Utility.getBundleString("clogging1",bundle) + " " + type + ":" + slPID);

                                        //Aggiungo oggetto alla sottocollezione
                                        SecondLevelEl.add(slPID);
                                    } else {
                                        progress = updateProgress(progress, total, Utility.getBundleString("clogging15",bundle) + ":" + slPID);
                                    }
                                }
                            }

                            //Aggiungo sottocollezione ad array di collezioni
                            firsLevelEl.add(flPID);

                            //Nel caso di pubblicazione postposta
                            if (lock != null) {
                                if (lock != "") {
                                    //flColl.addDatastreamContent("RIGHTS", "text/xml", coll.create_rights(lock), "RIGHTS", "X");
                                    flColl.grantUsername(user, lock);
                                    progress = updateProgress(progress, total, Utility.getBundleString("logging9",bundle) + ":" + flPID);
                                }
                            }

                            updateProgress(progress, total, Utility.getBundleString("clogging6",bundle) + ":" + flPID);

                            coll.addCollMembers(flColl, SecondLevelEl);
                            flColl.save();

                            addUploadInfoText(Utility.getBundleString("clogging4",bundle) + Utility.getBundleString("clogging5",bundle) + ":" + flPID);
                        }
                    }
                }
            }
            return firsLevelEl;
        }

        public void addUploadInfoInnerText(String message){
            addUploadInfoText(message);
        }
        
        /**
         * metodo che capisce l'elemento che ha in mano è un capitolo o una pagina
         * @param el
         * @param panelexists 
         */
        private void prepareMetadataUpload(Element el, boolean panelexists, String metadata, String panelname) {
            
            panelname = "";
            metadata = el.getAttribute("metadata");
            String pid = el.getAttribute("pid");
            
            if (metadata != null && !metadata.isEmpty()){
                
                if(pid.isEmpty()) {
                    pid = el.getAttribute("name");
                    pid += "-" + el.getAttribute("seq");
                }
                
                try {
                    panelexists = XMLTree.setPanelIfNotExists(metadata, pid);
                    panelname = BookImporter.getInstance().metadatapanels.get(metadata).getPanel().getName();
                    BookImporter.getInstance().metadata = MetaUtility.getInstance().metadata_reader(Globals.DUPLICATION_FOLDER_SEP + "session" + panelname);
                    
                    BookImporter.getInstance().createComponentMap(BookImporter.getInstance().metadatapanels.get(metadata).getPanel());
                    MetaUtility.getInstance().check_and_save_metadata(Globals.SELECTED_FOLDER_SEP + metadata, false, true, panelname);
                } catch (Exception ex) {
                    logger.error(ex.getMessage());
                }
            }
            else{
                panelname = BookImporter.mainpanel;
            }
        }
        
        private String addElementToCollection(Element el, ImObject coll) throws Exception {
            type = null;
            boolean panelexists = false;
            String metadata = el.getAttribute("metadata");
            String panelname = "";
            String pid = el.getAttribute("pid");
            
            if (metadata != null && !metadata.isEmpty()){
                panelexists = XMLTree.setPanelIfNotExists(el.getAttribute("metadata"), el.getAttribute("pid"));
                panelname = BookImporter.getInstance().metadatapanels.get(metadata).getPanel().getName();
                BookImporter.getInstance().metadata = MetaUtility.getInstance().metadata_reader(Globals.DUPLICATION_FOLDER_SEP + "session" + panelname);
                
                BookImporter.getInstance().createComponentMap(BookImporter.getInstance().metadatapanels.get(metadata).getPanel());
                MetaUtility.getInstance().check_and_save_metadata(Globals.SELECTED_FOLDER_SEP + metadata, false, true, panelname);
            } else {
                panelname = BookImporter.mainpanel; //metadati generici
                
                    //Altrimenti prende i metadati del padre
                    //pid = el.getAttribute("name");
                    //pid += "-" + el.getAttribute("seq");
                    //
                    //panelexists = XMLTree.setPanelIfNotExists(el.getAttribute("metadata"), pid);
                    //panelname = BookImporter.getInstance().metadatapanels.get(metadata).getPanel().getName();
                    //BookImporter.getInstance().metadata = MetaUtility.getInstance().metadata_reader(Globals.DUPLICATION_FOLDER_SEP + "session" + panelname);
                    //
                    //D) BookImporter.getInstance().createComponentMap(BookImporter.getInstance().metadatapanels.get(metadata).getPanel());
                    //M) BookImporter.getInstance().createComponentMap(BookImporter.getInstance().metadatapanels.get(panel name).getPanel());
                    //MetaUtility.getInstance().check_and_save_metadata(Globals.SELECTED_FOLDER_SEP + metadata, false, true, panelname);
            }
            
            //prepareMetadataUpload(el, panelexists, metadata, panelname);
           
            //Ricava nome file e path 
            String file = Globals.SELECTED_FOLDER_SEP + el.getAttribute("folder") + Utility.getSep() + el.getAttribute("href");

            setTextField(el.getAttribute("pid"));

            String mimetype = Utility.getMimeType(file);

            String slPID = null;
            
            if (Utility.fileIsPicture(mimetype)) {
                slPID = coll.createPicture(file, mimetype, this, panelname);
                type = Utility.getBundleString("clogging2",bundle);
            } else if (Utility.fileIsPdf(file)) {
                slPID = coll.createDocument(file, mimetype, this, panelname);
                type = Utility.getBundleString("clogging3",bundle);
            } else if (Utility.fileIsVideo(mimetype)) {
                slPID = coll.createVideo(file, mimetype, this, panelname);
                type = Utility.getBundleString("clogging13",bundle);
            }
            
            if (metadata != null && !metadata.isEmpty()){
                if (!panelexists){
                    BookImporter.getInstance().metadatapanels.get(metadata).getPane().getParent().remove(BookImporter.getInstance().metadatapanels.get(metadata).getPane());
                    BookImporter.getInstance().metadatapanels.remove(metadata);
                }
                
                BookImporter.getInstance().createComponentMap(BookImporter.getInstance().metadatapanels.get(BookImporter.mainpanel).getPanel());
                BookImporter.getInstance().metadata = MetaUtility.getInstance().metadata_reader(Globals.DUPLICATION_FOLDER_SEP + "session" + panelname);                
                MetaUtility.getInstance().check_and_save_metadata(Globals.SELECTED_FOLDER_SEP + Globals.IMP_EXP_METADATA, false, true, panelname);
            }
            
            return slPID;
        }

        /**
         * Metodo adibito alla creazione di tutte le pagine e capitoli di un
         * libro
         *
         * @param bookPID ID del libro nel quale aggiungere i capitoli/pagine
         * @param book Oggetto Libro su cui caricare i capitoli
         * @param meta Metadati in formato Stringa
         * @return current progress
         */
        private int createPages(String bookPID, ImObject obj, int progress, int total, NodeList nodeLst, Book book) throws Exception {
            ResourceBundle bundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);
            int pagenum = 1;
            int abspagenum = Utility.countXmlTreeLeaves();
            int percent = 0;

            boolean addChapter = true;
            boolean firstPage  = false;
            //Scorri bookstructure per trovare coppie(Nome Capitolo, Nome File pagina)
            for (int s = 0; s < nodeLst.getLength() && !isCancelled(); s++) {
                if (nodeLst.item(s).getNodeType() == Node.ELEMENT_NODE) {
                    Element chapter = (Element) nodeLst.item(s);
                    Book.Chapter capitolo = null;

                    addChapter = true;

                    //Per ogni pagina del capitolo esegui l'upload
                    NodeList leaves = chapter.getChildNodes();
                    for (int z = 0; z < leaves.getLength() && !isCancelled(); z++) {
                        if (leaves.item(z).getNodeType() == Node.ELEMENT_NODE) {
                            
                            //Se il capitolo ha una foglia aggiungilo
                            if (addChapter) {
                                addChapter = false;
                                capitolo = book.addChapter(chapter.getAttribute("name"));
                            }

                            Element page = (Element) leaves.item(z);

                            String flnm = page.getAttribute("href");

                            firstPage = (page.getAttribute("firstpage").equals("true"))?true:false;
                            
                            setTextField(flnm);
                            String pagePid = obj.createPage(flnm, bookPID, chapter.getAttribute("name"), pagenum, capitolo, book, firstPage, task);

                            pagenum++;
                            progress = updateProgress(progress, total, Utility.getBundleString("logging1",bundle) + ":" + pagePid);
                        }
                    }
                    
                    if (isCancelled()){
                        addUploadInfoText(Utility.getBundleString("suspendupload1", bundle) + ":" + uploadObjPID);
                        addUploadInfoText(Utility.getBundleString("suspendupload4", bundle));
                    }
                }
            }

            return progress;
        }

        private int updateProgress(int progress, int total, String field) {
            progress++;
            int percent = progress * 100 / total;
            jProgressBar1.setValue(percent);

            if (field != "") {
                addUploadInfoText(field);
            }

            return progress;
        }

        /*
         * Executed in event dispatching thread
         */
        @Override
        public void done() {
            if (!task.isCancelled()){
                jProgressBar1.setValue(100);
                setCursor(null);
                jButton1.setEnabled(true);
            }
        }
    }

    private NodeList getNodeList() throws ParserConfigurationException, SAXException, IOException {
        XMLTree.savedXmlDoc.getDocumentElement().normalize();

        NodeList result = null;
        if (Globals.TYPE_BOOK == Globals.BOOK) {
            result = XMLTree.savedXmlDoc.getElementsByTagName("book:structure");
        } else {
            NodeList list = XMLTree.savedXmlDoc.getElementsByTagName("coll:pages");
            if (list.getLength() == 1) {
                Node nodo = list.item(0);
                result = nodo.getChildNodes();
            }
        }

        return result;
    }

    public void setTextField(String file) {
        jTextField1.setText(file);
    }

    /**
     * Invoked when task's progress property changes.
     */
    public void propertyChange(PropertyChangeEvent evt) {
        if ("progress".equals(evt.getPropertyName())) {
            int progress = (Integer) evt.getNewValue();
            jProgressBar1.setValue(progress);
        }
    }

    /**
     * Creates new form UploadProgress
     */
    public UploadProgress(Phaidra phai, String locks, String pdfPth, String usernm) {
        phaidra = phai;
        lock = locks;
        path = pdfPth;
        user = usernm;
        BookImporter.getInstance().setVisible(false);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHms");
        System.setProperty("current.date", dateFormat.format(new Date()));
        DOMConfigurator.configure(Globals.UPLLOG4J);
        
        initComponents();
        jProgressBar1.setMaximum(100);
        jButton1.setMinimumSize(new Dimension(120,20));
        jButton2.setMinimumSize(new Dimension(120,20));
        
        jTextField1.setEditable(false);
        jTextPane1.setEditable(false);
        
        updateLanguageLabel();

        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        task = new UploadProgress.UplTask();
        task.addPropertyChangeListener(this);
        task.execute();

        jButton1.setEnabled(false);
        jButton1.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                BookImporter.getInstance().setVisible(true);
                frame.dispose();
            }
        });

        //Gestione della visualizzazione del URI del libro caricato
        jButton2.setEnabled(false);
        jButton2.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    ResourceBundle bundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);
                    String uploaduriprotocol = Utility.getBundleString("uploaduriprotocol",bundle);
                    String uploaduristatic = Utility.getBundleString("uploaduristatic",bundle);

                    String uploaduri = uploaduriprotocol + SelectedServer.getInstance(null).getPhaidraURL();
                    uploaduri += uploaduristatic + uploadObjPID;
                              
                    URI link = new URI(uploaduri);
                    openWebpage(link);
                } catch (URISyntaxException ex) {
                    logger.error(ex.getMessage());
                }
            }
        });
        
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                ResourceBundle bundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);
                   String title = Utility.getBundleString("stopupload_title", bundle);
                   String text = Utility.getBundleString("stopupload", bundle);
                   
                   ConfirmDialog confirm = new ConfirmDialog(frame, true, title, text, Utility.getBundleString("confirm", bundle), Utility.getBundleString("back", bundle));
                   
                   confirm.setVisible(true);
                   boolean close = confirm.getChoice();
                   confirm.dispose();

                   if (close == true){
                        task.cancel(true);
                   }
            }
        });
        
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (dim.width - getSize().width) / 2;
        int y = (dim.height - getSize().height) / 2;
        setLocation(x, y);
    }

    private void updateLanguageLabel() {
        ResourceBundle bundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);
        jButton1.setText(Utility.getBundleString("uplprbutton",bundle));
        
        if (Globals.COLLECTION == Globals.TYPE_BOOK){
            jButton2.setText(Utility.getBundleString("opencollurl",bundle));
        }
        else{
            jButton2.setText(Utility.getBundleString("openbookurl",bundle));
        }
        
        jLabel1.setText(Utility.getBundleString("uplprlbl1",bundle));
        jLabel2.setText(Utility.getBundleString("uplprlbl2",bundle));
        
        frame.setTitle(Utility.getBundleString("uplstatus",bundle));
        
        if(Globals.TYPE_BOOK == Globals.BOOK)
            jPanel1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), Utility.getBundleString("uplprtitle1",bundle), TitledBorder.LEFT, TitledBorder.TOP));
        else
            jPanel1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), Utility.getBundleString("cplprtitle1",bundle), TitledBorder.LEFT, TitledBorder.TOP));
        
        jPanel2.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), Utility.getBundleString("uplprtitle2",bundle), TitledBorder.LEFT, TitledBorder.TOP));
        jPanel3.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), Utility.getBundleString("uplprtitle3",bundle), TitledBorder.LEFT, TitledBorder.TOP));
        jTextPane1.setText(Utility.getBundleString("upldprfirst",bundle));
    }

    /**
     * Create the GUI and show it. As with all GUI code, this must run on the
     * event-dispatching thread.
     */
    protected static void createAndShowGUI(Phaidra phaidra, String lockObjectsUntil, String pdfPath, String username) {
        //Create and set up the window.
        frame = new JFrame("Upload Book");
        frame.setDefaultCloseOperation(frame.DO_NOTHING_ON_CLOSE);

        //Create and set up the content pane.
        JComponent newContentPane = new UploadProgress(phaidra, lockObjectsUntil, pdfPath, username);

        newContentPane.setOpaque(true); //content panes must be opaque
        frame.setContentPane(newContentPane);

        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                BookImporter.getInstance().setVisible(true);
                frame.dispose();
            }
        });

        //Display the window.
        frame.pack();
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (dim.width - frame.getSize().width) / 2;
        int y = (dim.height - frame.getSize().height) / 2;
        frame.setLocation(x, y);
            
        frame.setVisible(true);
    }

    public void addUploadInfoText(String add) {
        String text = jTextPane1.getText();
        text += "\n" + add;
        jTextPane1.setText(text);
        jTextPane1.revalidate();
        logger.info(add);
    }

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {
        frame.dispose();
    }

        
    private static void openWebpage(URI uri) {
        Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
        if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
            try {
                desktop.browse(uri);
            } catch (Exception ex) {
                logger.error(ex.toString());
            }
        }
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jProgressBar1 = new javax.swing.JProgressBar();
        jPanel2 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jTextField1 = new javax.swing.JTextField();
        jPanel3 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTextPane1 = new javax.swing.JTextPane();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(""));

        jLabel1.setText("Avanzamento");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(22, 22, 22)
                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 107, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(27, 27, 27)
                .addComponent(jProgressBar1, javax.swing.GroupLayout.PREFERRED_SIZE, 521, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jProgressBar1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder(""));

        jLabel2.setText("File");

        jTextField1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(23, 23, 23)
                .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 101, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(34, 34, 34)
                .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 518, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder(""));

        jScrollPane2.setViewportView(jTextPane1);

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 596, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(18, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 235, Short.MAX_VALUE)
                .addContainerGap())
        );

        jButton1.setText("jButton1");

        jButton2.setText("jButton2");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(19, 19, 19)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jButton2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jButton1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(23, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(18, 18, 18)
                        .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(54, 54, 54)
                        .addComponent(jButton2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButton1)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jTextField1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextField1ActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JProgressBar jProgressBar1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextPane jTextPane1;
    // End of variables declaration//GEN-END:variables
}
