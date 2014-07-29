/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package it.imtech.upload;

import at.ac.univie.phaidra.api.Phaidra;
import at.ac.univie.phaidra.api.objekt.Collection;
import at.ac.univie.phaidra.api.objekt.*;
import it.imtech.bookimporter.BookImporter;
import it.imtech.metadata.MetaUtility;
import it.imtech.utility.Utility;
import it.imtech.globals.Globals;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
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
import org.apache.log4j.Logger;
import org.apache.ws.commons.util.NamespaceContextImpl;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Classe adibita all'upload di un oggetto su server (Collezione/Libro)
 * @author mauro
 */
public class ImObject {
    //Gestore dei log
    public final static Logger logger = Logger.getLogger(ImObject.class);
    
    FullText bookFulltext = new FullText();
    String title = "";
    String username = "";
    Phaidra phaidra = null;
    String lockObjectsUntil = null;
    ResourceBundle bundle = null;

    /**
     * Costruttore dell'oggetto 
     * @param ImPhaidra Oggetto Phaidra dal quale richiamare i metodi delle API
     * @param lockUntil Stringa di blocco oggetto su server
     * @param user Username per l'accesso al server
     */
    public ImObject(Phaidra ImPhaidra, String lockUntil, String user) {
        this.phaidra = ImPhaidra;
        this.lockObjectsUntil = lockUntil;
        this.username = user;
        bundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);
    }

    /**
     * Conta il numero totale di operazioni da eseguire per raggiungere
     * il 100% dell'upload di un oggetto
     * @return 
     */
    public int countTotal() {
        int total = Utility.countXmlTreeLeaves();
        
        if (BookImporter.getInstance().ocrBoxIsChecked() && Globals.TYPE_BOOK == Globals.BOOK) {
            total++;
        }

        if (lockObjectsUntil != null) {
            if (lockObjectsUntil != "") {
                total++;
            }
        }

        return total + 2;
    }

    /**
     * Restituisce una collezione create mediante API di Phaidra
     * @param name Label della collezione
     * @return L'oggetto Collezione
     * @throws Exception 
     */
    protected Collection createCollection(String name) throws Exception {
        return phaidra.createCollection(name);
    }

    /**
     * Crea un' Oggetto picture di Fedora con relativi metadati e blocco
     * @param path Path del file 
     * @param mimetype Tipo di file
     * @return Identificativo dell'oggetto
     * @throws Exception 
     */
    protected String createPicture(String path, String mimetype) throws Exception {
        Picture picture = phaidra.createPicture("Picture Java");
        picture.addPicture(path, mimetype);
        picture.addMetadata(addPhaidraMetadata(picture.getPID(),""));

        //Nel caso di pubblicazione postposta
        if (lockObjectsUntil != null) {
            if (lockObjectsUntil != "") {
                picture.grantUsername(username, lockObjectsUntil); 
               // picture.addDatastreamContent("RIGHTS", "text/xml", create_rights(lockObjectsUntil), "RIGHTS", "X");
            }
        }

        picture.save();

        return picture.getPID();
    }

    /**
     * Crea un' Oggetto Document di Fedora con relativi metadati e blocco
     * @param path Path del file 
     * @param mimetype Tipo di file
     * @return
     * @throws Exception 
     */
    protected String createDocument(String path, String mimetype) throws Exception {
        Document doc = phaidra.createDocument("Picture Java");
        doc.addPDF(path);
        doc.addMetadata(addPhaidraMetadata(doc.getPID(),""));

        //Nel caso di pubblicazione postposta
        if (lockObjectsUntil != null) {
            if (lockObjectsUntil != "") {
                doc.grantUsername(username, lockObjectsUntil); 
              //  doc.addDatastreamContent("RIGHTS", "text/xml", create_rights(lockObjectsUntil), "RIGHTS", "X");
            }
        }

        doc.save();

        return doc.getPID();
    }

    /**
     * Aggiunge i membri contenuti nell'array passato come parametro
     * nella collezione passata come parametro.
     * @param collect Collezione nella quale aggiungere i membri
     * @param members Membri da aggiungere (Lista di Stringhe PIDS)
     * @return
     * @throws Exception 
     */
    protected boolean addCollMembers(Collection collect, ArrayList<String> members) throws Exception {
        boolean result = true;
        String[] strArray = new String[members.size()];
        members.toArray(strArray);

        collect.addMembers(strArray);
        ArrayList<String> pids = collect.getMembers();

        for (int i = 0; i < strArray.length; i++) {
            if (!pids.contains(strArray[i])) {
                result = false;
            }
        }

        return result;
    }

    /**
     * Restituisce una stringa di metadati formata in base all'id dell'oggetto passato
     * come parametro
     * @param PID ID dell'oggetto sul quale aggiungere i metadati
     * @return
     * @throws Exception 
     */
    protected String addPhaidraMetadata(String PID,String title) throws Exception {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        String uploadDate = df.format(new Date());

        ByteArrayInputStream dataContent = getDataStreamContent(PID);
        HashMap<String, String> objectDefaultValues = getDefaultValues(PID, dataContent);

        org.w3c.dom.Document doc = MetaUtility.getInstance().create_uwmetadata(PID, -1, objectDefaultValues,title);

        String meta = Utility.getStringFromDocument(doc);

        String metadata = meta.replace("PID", PID);
        metadata = metadata.replace("UPLOADDATE", uploadDate);

        return metadata;
    }

    /**
     * Restitusce un nuovo oggetto Book
     * @return
     * @throws Exception 
     */
    protected Book createBook() throws Exception {
        Book book = null;
        try {
           book = phaidra.createBook("Java Book API Test");
        }
        catch(Exception ex){
             logger.error("Errore nella creazione del libro");
        }
        
        return book;
    }

    /**
     * Restituisce il testo completo OCR
     * @return 
     */
    protected String getFulltext() {
        return this.bookFulltext.getFulltext();
    }

    /**
     * Crea una pagina del libro con metadati/pubblicazione postposta e OCR
     * @param filename Nome del file da caricare
     * @param bookPID Pid del libro
     * @param structure Nome del capitolo nel quale aggiungere la pagina
     * @param pagenum Numero di pagine totale caricate
     * @param chapter Oggetto Capitolo nel quale caricare la pagina
     * @param book Oggetto libro nel quale caricare la pagina
     * @param phaidra Oggetto phaidra nel quale caricare le pagina
     * @param meta
     * @throws Exception
     */
    protected String createPage(String filename, String bookPID, String structure, int pagenum, Book.Chapter chapter, Book book,boolean startPage) throws Exception {
        // create page
        Page page = phaidra.createPage("Book Page " + pagenum, book, pagenum, Integer.toString(pagenum), structure, startPage);
        String PID = page.getPID();  
        
        if (PID != null) {
            page.addPicture(Globals.SELECTED_FOLDER_SEP + filename, Utility.getMimeType(Globals.SELECTED_FOLDER_SEP + filename));

            page.addMetadata(this.addPhaidraMetadata(PID,""));

            if (BookImporter.getInstance().ocrBoxIsChecked()) {
                ImPage dummy = new ImPage();
                String xmlPath = Globals.SELECTED_FOLDER_SEP + Utility.changeFileExt(filename);

                dummy = createOCRTEXT(xmlPath, PID, Integer.toString(pagenum));

                if (dummy != null) {
                    page.addDatastreamContent("OCRTEXT", "text/xml", dummy.getOcrXML(), "OCRTEXT of the Page", "X");
                    page.addDatastreamContent("FULLTEXT", "text/plain", dummy.getFulltext(), "Fulltext of the Page", "M");
                }
            }

            if (lockObjectsUntil != null) {
                if (lockObjectsUntil != "") {
                    page.grantUsername(username, lockObjectsUntil);
                    //page.addDatastreamContent("RIGHTS", "text/xml", create_rights(lockObjectsUntil), "RIGHTS", "X");
                }
            }

            book.addPage(chapter, page);

            page.save();
        }

        return PID;
    }

    /**
     * Crea XML contenente la data di pubblicazione dell'oggetto
     * e i dati di accesso per l'utente
     * @param value Numero di giorni da aggiungere ad oggi
     * @return Stringa che descrive l'xml realizzato
     * @throws Exception 
     */
   protected String create_rights(String value) throws Exception {
        String res = "";

        try {
            StreamResult result = new StreamResult(new StringWriter());
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);

            // create the xml document builder object and get the DOMImplementation object
            DocumentBuilder builder = factory.newDocumentBuilder();
            DOMImplementation domImpl = builder.getDOMImplementation();
            org.w3c.dom.Document xmlDoc = domImpl.createDocument("http://phaidra.univie.ac.at/XML/V1.0/rights", "uwr:rights", null);

            Transformer t = TransformerFactory.newInstance().newTransformer();
            t.setOutputProperty(OutputKeys.INDENT, "yes");
            t.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            t.setOutputProperty(OutputKeys.STANDALONE, "");
            t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

            // get the root element
            Element rootElement = xmlDoc.getDocumentElement();
            Element allow = xmlDoc.createElement("uwr:allow");
            rootElement.appendChild(allow);

            Element user = xmlDoc.createElement("uwr:username");
            user.setAttribute("expires", value);
            user.setTextContent(this.username);
            allow.appendChild(user);

            res = Utility.getStringFromDocument(xmlDoc);
        } catch (TransformerConfigurationException e) {
        } catch (Exception e) {
            throw new Exception(Utility.getBundleString("error7",bundle) + ": " + e.getMessage());
        }

        return res;
    }

    /**
     * Restituisce uno stream dell'oggetto restituito dalla connessione di Phaidra
     * @param pid Identificativo dell'oggetto
     * @return
     * @throws Exception 
     */
    private ByteArrayInputStream getDataStreamContent(String pid) throws Exception {
        try {
            byte[] dataContent = phaidra.getFedora().getAPIM().getObjectXML(pid);
            return new ByteArrayInputStream(dataContent);
        } catch (Exception ex) {
            throw new Exception(ex.getMessage());
        }
    }

    /**
     * Definisce parametri (size,identifier,upload_date,format) dell'oggetto
     * definito dall'identificativo passato come parametro
     * @param pid
     * @param dataStream
     * @return Una mappa dei parametri di default
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     * @throws XPathExpressionException 
     */
    private HashMap<String, String> getDefaultValues(String pid, ByteArrayInputStream dataStream) throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
        HashMap<String, String> values = new HashMap<String, String>();

        HashMap<String, String> getValues = new HashMap<String, String>();
        getValues.put("identifier", "/foxml:digitalObject/@PID");
        getValues.put("upload_date", "/foxml:digitalObject/foxml:objectProperties/foxml:property[@NAME=\"info:fedora/fedora-system:def/model#createdDate\"]/@VALUE");
        getValues.put("size", "/foxml:digitalObject/foxml:datastream[@ID=\"TECHINFO\"]/foxml:datastreamVersion[last()]/foxml:xmlContent/di:dsinfo/di:filesize");
        getValues.put("format", "/foxml:digitalObject/foxml:datastream[@ID=\"OCTETS\"]/foxml:datastreamVersion[last()]/@MIMETYPE");
        
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        dbFactory.setNamespaceAware(true);
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

        org.w3c.dom.Document doc = dBuilder.parse(dataStream);

        NamespaceContextImpl nsContext = new NamespaceContextImpl();
        nsContext.startPrefixMapping("foxml", "info:fedora/fedora-system:def/foxml#");
        nsContext.startPrefixMapping("di", "http://phaidra.univie.ac.at/XML/dsinfo/V1.0");

        //getValues Hash durchlaufen und XPath suchen -> Value in values Hash schreiben
        for (Map.Entry<String, String> field : getValues.entrySet()) {
            XPath xpath = XPathFactory.newInstance().newXPath();
            xpath.setNamespaceContext(nsContext);
            String path = (String) xpath.evaluate(field.getValue().toString(), doc, XPathConstants.STRING);
            values.put(field.getKey().toString(), path);
        }
        values.put("purpose", "70");
        values.put("peer_reviewed", "no");
        
        //Bei Büchern wird kein TECHINFO geschrieben - also auch keine Filesize :(
        try {
            Double size = Double.parseDouble(Utility.getValueFromKey(values, "size"));
        } catch (Exception ex) {
            for (Map.Entry<String, String> field : values.entrySet()) {
                if (field.getKey().toString().equals("size")) {
                    field.setValue("0");
                }
            }
        }

        return values;
    }

    /**
     * Crea dati OCR partendo da un Identificativo di un oggetto
     * @param path
     * @param pid
     * @param abspagenum
     * @return
     * @throws Exception 
     */
    private ImPage createOCRTEXT(String path, String pid, String abspagenum) throws Exception {
        ImPage thisPage = new ImPage();

        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            dbFactory.setNamespaceAware(true);
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

            org.w3c.dom.Document doc = dBuilder.parse(path);
            Node root = doc.getDocumentElement();

            NamespaceContextImpl nsContext = new NamespaceContextImpl();
            nsContext.startPrefixMapping("ocrtext", "http://phaidra.univie.ac.at/XML/book/ocrtext/V1.0");

            XPath xpath = XPathFactory.newInstance().newXPath();
            xpath.setNamespaceContext(nsContext);
            Object obj = xpath.evaluate("/ocrtext:ocrtext/ocrtext:page", doc, XPathConstants.NODE);
            Element page = (Element) obj;

            if (page != null) {
                if (page.getAttribute("pid") != null) {
                    page.setAttribute("pid", pid);
                }
                if (page.getAttribute("abspagenum") != null) {
                    page.setAttribute("abspagenum", abspagenum);
                }
            } else {
                throw new Exception(Utility.getBundleString("error12",bundle));
            }

            thisPage.setOcrXML(Utility.getOuterXml(root));

            //Fulltext auslesen
            Object obj2 = xpath.evaluate("/ocrtext:ocrtext/ocrtext:page/ocrtext:ocrword", doc, XPathConstants.NODESET);
            NodeList words = (NodeList) obj2;
            for (int s = 0; s < words.getLength(); s++) {
                if (words.item(s).getNodeType() == Node.ELEMENT_NODE) {
                    Element word = (Element) words.item(s);

                    thisPage.addToFulltext(word.getAttribute("word"));

                    this.bookFulltext.addText(thisPage.getFulltext());
                }
            }
        } catch (Exception ex) {
            throw new Exception("OCR Upload Exception: " + ex.getMessage());
        }

        return thisPage;
    }
}
