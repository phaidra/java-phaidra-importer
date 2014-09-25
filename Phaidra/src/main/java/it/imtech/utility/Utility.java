/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package it.imtech.utility;

import it.imtech.upload.UnicodeUtil;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfImportedPage;
import com.itextpdf.text.pdf.PdfReader;
import it.imtech.globals.Globals;
import it.imtech.upload.SelectedServer;
import it.imtech.xmltree.XMLTree;
import java.awt.Frame;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.JOptionPane;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.*;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.ws.commons.util.NamespaceContextImpl;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 *
 * @author mauro
 */
public class Utility {
    //Gestore dei log
    public final static Logger logger = Logger.getLogger(Utility.class);
    
    /**
     * Setta la directory dell'eseguibile dell'applicazione
     * @return Path corrente
     */
    public static String getCurrentJarDirectory() {
        URL url = Globals.class.getProtectionDomain().getCodeSource().getLocation();
        String jrPath = null;
        
        try {
            final File jarPath = new File(url.toURI()).getParentFile();
            jrPath = jarPath.getAbsolutePath() + Utility.getSep();
        } catch (final URISyntaxException ex) {
            logger.error(ex.getMessage());
            JOptionPane.showMessageDialog(new Frame(), ex.getMessage());
        }
        
        return jrPath;
    }
 
    public static boolean internetConnectionAvailable(){
        boolean connected = false;
        try {
            try {
                URL url = new URL("http://www.google.com");
                System.out.println(url.getHost());
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.connect();
                
                if (con.getResponseCode() == 200){
                    connected = true;
                }
            } catch (Exception exception) {
                logger.info("Internet Connection not available!");
            }
        } catch (Exception e) {
            logger.info("Exception during check Internet Connection!");
        }
        
        return connected;
    }
    
    public static void cleanUndoDir() {
        
        try {
            boolean exists = (new File(Globals.UNDO_DIR)).exists();
            if (!exists) {
                boolean success = (new File(Globals.UNDO_DIR)).mkdir();
                if (!success) {
                    logger.error("E0001: Cannot create undo dir: " + Globals.UNDO_DIR);
                }
                else{
                    logger.debug("Undo dir created: " + Globals.UNDO_DIR);
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
            logger.debug("Undo dir cleaned");
        } catch (IOException ex) {
            logger.error("EX0001: Exception during undo dir clean");
            logger.error(ex.getMessage());
        }
    }
    
    public static String getSep() {
        String osname = System.getProperty("os.name").toLowerCase();
        String sep = "/";
        if (osname.startsWith("win")) {
            sep = "\\";
        }

        return sep;

    }

    public static String getBundleString(String name, ResourceBundle bundle) {
        try {
            return bundle.getString(name);
        } catch (Exception exp) {
            return name;
        }
    }

    public static byte[] convertImage(String path, float quality) throws IOException {
        BufferedImage image = ImageIO.read(new File(path));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageOutputStream ios = ImageIO.createImageOutputStream(baos);

        Iterator iter = ImageIO.getImageWritersByFormatName("JPEG");
        javax.imageio.ImageWriter writer = null;

        if (!iter.hasNext()) {
            throw new IOException("No Writers Available");
        }

        writer = (javax.imageio.ImageWriter) iter.next();

        writer.setOutput(ios);

        JPEGImageWriteParam iwp = new JPEGImageWriteParam(null);
        iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);

        iwp.setCompressionQuality(quality);

        writer.write(null, new IIOImage(image, null, null), iwp);

        baos.flush();
        byte[] imageInByte = baos.toByteArray();
        baos.close();

        return imageInByte;
    }

    public static String getValueFromKey(Map map, String key) {
        String result = "";
        for (Iterator i = map.keySet().iterator(); i.hasNext();) {
            String it = (String) i.next();
            if (map.get(it).equals(key)) {
                result = it;
            }
        }
        return result;
    }

    public static Date addDays(Date date, int days) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DATE, +days);
        return cal.getTime();
    }

    public static Element getXPathNode(String path, NamespaceContextImpl nsmgr, Document nav) {
        try {
            XPath xpath = XPathFactory.newInstance().newXPath();
            xpath.setNamespaceContext(nsmgr);
            XPathExpression expression = xpath.compile(path);
            Object result = expression.evaluate(nav, XPathConstants.NODE);
            Element node = (Element) result;
            return node;
        } catch (XPathException exp) {
            return null;
        }
    }

    public static String getOuterXml(Node node) {
        String results = "";
        try {    
              DOMSource domSource = new DOMSource(node); 
              StringWriter writer = new StringWriter(); 
              StreamResult result = new StreamResult(writer); 
              TransformerFactory tf = TransformerFactory.newInstance(); 
              Transformer transformer = tf.newTransformer(); 
              transformer.transform(domSource, result);
              
              byte[] out_1 = UnicodeUtil.convert(writer.toString().getBytes("UTF-16"), "UTF-8"); //Shanghai in Chinese
              results = new String(out_1);    
          
        } catch (Exception ex) {
            return null;
        }
        
        return results;
    }

    public static String changeFileExt(String filename) {
        String[] parts = filename.split(Utility.getSep() + ".");
        int i = parts.length - 1;
        parts[i] = "xml";
        return StringUtils.join(parts, ".");
    }

    public static String getStringFromDocument(Document doc) {
        String results = "";
        try {    
              DOMSource domSource = new DOMSource(doc); 
              StringWriter writer = new StringWriter(); 
              StreamResult result = new StreamResult(writer); 
              TransformerFactory tf = TransformerFactory.newInstance(); 
              Transformer transformer = tf.newTransformer(); 
              transformer.transform(domSource, result);
              
              byte[] out_1 = UnicodeUtil.convert(writer.toString().getBytes("UTF-16"), "UTF-8"); //Shanghai in Chinese
              results = new String(out_1);    
          
        } catch (Exception ex) {
            return null;
        }
        
        return results;
    }

    public static void concatPDFs(List<InputStream> streamOfPDFFiles, OutputStream outputStream, boolean paginate) {

        com.itextpdf.text.Document document = new com.itextpdf.text.Document();
        try {
            List<InputStream> pdfs = streamOfPDFFiles;
            List<com.itextpdf.text.pdf.PdfReader> readers = new ArrayList<com.itextpdf.text.pdf.PdfReader>();
            int totalPages = 0;
            Iterator<InputStream> iteratorPDFs = pdfs.iterator();

            // Create Readers for the pdfs.
            while (iteratorPDFs.hasNext()) {
                InputStream pdf = iteratorPDFs.next();
                com.itextpdf.text.pdf.PdfReader pdfReader = new PdfReader(pdf);
                readers.add(pdfReader);
                totalPages += pdfReader.getNumberOfPages();
            }
            // Create a writer for the outputstream
            com.itextpdf.text.pdf.PdfWriter writer = com.itextpdf.text.pdf.PdfWriter.getInstance(document, outputStream);

            document.open();
            com.itextpdf.text.pdf.BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA,
                    BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
            PdfContentByte cb = writer.getDirectContent(); // Holds the PDF
            // data

            PdfImportedPage page;
            int currentPageNumber = 0;
            int pageOfCurrentReaderPDF = 0;
            Iterator<PdfReader> iteratorPDFReader = readers.iterator();

            // Loop through the PDF files and add to the output.
            while (iteratorPDFReader.hasNext()) {
                PdfReader pdfReader = iteratorPDFReader.next();

                // Create a new page in the target for each source page.
                while (pageOfCurrentReaderPDF < pdfReader.getNumberOfPages()) {
                    document.newPage();
                    pageOfCurrentReaderPDF++;
                    currentPageNumber++;
                    page = writer.getImportedPage(pdfReader,
                            pageOfCurrentReaderPDF);
                    cb.addTemplate(page, 0, 0);

                    // Code for pagination.
                    if (paginate) {
                        cb.beginText();
                        cb.setFontAndSize(bf, 9);
                        cb.showTextAligned(PdfContentByte.ALIGN_CENTER, ""
                                + currentPageNumber + " of " + totalPages, 520,
                                5, 0);
                        cb.endText();
                    }
                }
                pageOfCurrentReaderPDF = 0;
            }
            outputStream.flush();
            document.close();
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (document.isOpen()) {
                document.close();
            }
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    public static boolean fileIsPicture(String mimetype) {
        return mimetype.substring(0, 5).equalsIgnoreCase("image");
    }

    public static boolean fileIsPdf(String file) {
        boolean result = true;

        try {
            PDDocument.load(file);
        } catch (IOException ex) {
            result = false;
        }

        return result;
    }
    
    public static boolean fileIsVideo(String mimetype) {
        return mimetype.substring(0, 5).equalsIgnoreCase("video");
    }

    public static String getMimeType(String filename) {
        
        String result = null;
        MediaType mimetype;
        
        try {
            File x = new File (filename);
            FileInputStream is = new FileInputStream(x);
        
            TikaConfig tika = new TikaConfig();
            
            mimetype = tika.getDetector().detect(TikaInputStream.get(is), new Metadata());
            result = mimetype.toString();
        }
        catch (Exception e) {
          logger.info("Exception during mimetype detection.");
        }
        return result;
    }

    public static File getUniqueFileName(String prefix, String suffix) {
        int imageCounter = 1;

        String uniqueName = null;
        File f = null;
        while (f == null || f.exists()) {
            uniqueName = prefix + imageCounter;
            f = new File(uniqueName + "." + suffix);
            imageCounter++;
        }

        return f;
    }

    /**
     *
     */
    /*
 
*/
    public static int countXmlTreeLeaves() {
        int leaves = 0;

        try {
            leaves = XMLTree.getRoot().getLeafCount();
        } catch (Error ex) {
            leaves = 0;
        }
        return leaves;
    }
    
    public static void makeBackupMetadata(String filename, String outputpath){
        BufferedInputStream inputStream = null;
        BufferedOutputStream outputStream = null;
        byte[] buffer = new byte[1024];
        int bytesRead;
        String path = "";
        Globals.BACKUP_METADATA = Globals.USER_DIR + filename;
        
        File f = new File(Globals.BACKUP_METADATA);
        File s = new File(outputpath);
        
        try {
            if (Globals.ONLINE){
                path = SelectedServer.getInstance(null).getHTTPStaticBaseURL() + filename;
                URL url = new URL(path);
                URLConnection connection = url.openConnection();

                inputStream = new BufferedInputStream(connection.getInputStream());
                outputStream = new BufferedOutputStream(new FileOutputStream(f));

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                inputStream.close();
                outputStream.close();
            }
            
            FileUtils.copyFile(f, s);
            FileUtils.copyFile(f, new File (Globals.DUPLICATION_FOLDER_SEP + Globals.BACKUP_INIT));
        } catch (MalformedURLException ex) {
            logger.error("Cannot donwload file: "+path+"  "+ex.getMessage());
        } catch (IOException ex) {
            logger.error("Cannot donwload file: "+path+"  "+ex.getMessage());
        }
    }
    
    public static Document getDocument(String filename,boolean classif) throws MalformedURLException, ParserConfigurationException, SAXException, IOException{
       DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
       DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
       
       File urlf = null;
       URL url = null;
       Document doc;
       String path;
      
       if (!Globals.ONLINE){
           path = Globals.USER_DIR + Utility.getSep() + filename;
           urlf = new File(path);
       }
       else{
            if(Globals.DEBUG) {      
                 if(classif){
                     int i = filename.lastIndexOf("/");
                     String name = filename.substring(i, filename.length());
                     path = filename.replace(SelectedServer.getInstance(null).getHTTPStaticBaseURL(),Globals.JRPATH + Globals.DEBUG_FOLDER);
                 }
                 else{
                     path = Globals.JRPATH+Globals.DEBUG_FOLDER+Utility.getSep()+filename;
                 }
                 urlf = new File(path);
             }
             else {
                 path = (classif)?filename:SelectedServer.getInstance(null).getHTTPStaticBaseURL()+filename;
                 url = new URL(path);
             }
       }
               
        if(Globals.DEBUG || !Globals.ONLINE)
            doc = dBuilder.parse(urlf);
        else
            doc = dBuilder.parse(url.openStream());
    
        return doc;
    }

    /**
     * Setta la lingua corrente come lingua di default nel file di
     * configurazione
     *
     * @throws ConfigurationException
     */
    public static void setDefaultLangCurrent() {
        try{
            XMLConfiguration internalConf = new XMLConfiguration(Globals.INTERNAL_CONFIG);
            internalConf.setAutoSave(true);
            internalConf.setProperty("locale.current[@value]", Globals.CURRENT_LOCALE.getLanguage());
        }
        catch(ConfigurationException ex){
           logger.debug("Exception: set default lang current");
           logger.debug(ex.getMessage());
        }
    }
    
    public static TreeMap<String, String> getLanguages() {
        TreeMap<String, String> languages = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);

        if (Globals.CURRENT_LOCALE.getLanguage().equals(new Locale("de").getLanguage())) {
            languages.put("Afar", "aa");
            languages.put("Abchasisch", "ab");
            languages.put("Avestisch", "ae");
            languages.put("Afrikaans", "af");
            languages.put("Akan", "ak");
            languages.put("Amharisch", "am");
            languages.put("Aragonesisch", "an");
            languages.put("Arabisch", "ar");
            languages.put("Assamesisch", "as");
            languages.put("Avaric", "av");
            languages.put("Aymara", "ay");
            languages.put("Aserbaidschanisch", "az");
            languages.put("Baschkirisch", "ba");
            languages.put("Belorussisch", "be");
            languages.put("Bulgarisch", "bg");
            languages.put("Biharisch", "bh");
            languages.put("Bislamisch", "bi");
            languages.put("Bambara", "bm");
            languages.put("Bengalisch", "bn");
            languages.put("Tibetanisch", "bo");
            languages.put("Bretonisch", "br");
            languages.put("Bosnisch", "bs");
            languages.put("Katalanisch", "ca");
            languages.put("Tschetschenisch", "ce");
            languages.put("Chamorro", "ch");
            languages.put("Korsisch", "co");
            languages.put("Cree", "cr");
            languages.put("Tschechisch", "cs");
            languages.put("Kirchenslawisch", "cu");
            languages.put("Tschuwasch", "cv");
            languages.put("Walisisch", "cy");
            languages.put("D\u00E4nisch", "da");
            languages.put("Deutsch", "de");
            languages.put("Dhivehi", "dv");
            languages.put("Dzongkha, Bhutani", "dz");
            languages.put("Ewe", "ee");
            languages.put("Griechisch", "el");
            languages.put("Englisch", "en");
            languages.put("Esperanto", "eo");
            languages.put("Spanisch", "es");
            languages.put("Estnisch", "et");
            languages.put("Baskisch", "eu");
            languages.put("Persisch", "fa");
            languages.put("Fulah", "ff");
            languages.put("Finnisch", "fi");
            languages.put("Fiji", "fj");
            languages.put("Far\u00F6isch", "fo");
            languages.put("Franz\u00F6sisch", "fr");
            languages.put("Friesisch", "fy");
            languages.put("Irisch", "ga");
            languages.put("Schottisches Galisch", "gd");
            languages.put("Galizisch", "gl");
            languages.put("Guarani", "gn");
            languages.put("Gujaratisch", "gu");
            languages.put("Manx", "gv");
            languages.put("Haussa", "ha");
            languages.put("Hebr\u00E4isch", "he");
            languages.put("Hindi", "hi");
            languages.put("Kroatisch", "hr");
            languages.put("Haitianisch", "ht");
            languages.put("Ungarisch", "hu");
            languages.put("Armenisch", "hy");
            languages.put("Herero", "hz");
            languages.put("Interlingua", "ia");
            languages.put("Indonesisch", "id");
            languages.put("Interlingue", "ie");
            languages.put("Igbo", "ig");
            languages.put("Sichuan Yi; Nuosu", "ii");
            languages.put("Inupiak", "ik");
            languages.put("Indonesisch", "in");
            languages.put("Ido", "io");
            languages.put("Isl\u00E4ndisch", "is");
            languages.put("Italienisch", "it");
            languages.put("Inuktitut", "iu");
            languages.put("Hebr\u00E4isch (veraltet, nun: he)", "iw");
            languages.put("Japanisch", "ja");
            languages.put("Jiddish (veraltet, nun: yi)", "ji");
            languages.put("Javanesisch", "jv");
            languages.put("Javanisch", "jw");
            languages.put("Georgisch", "ka");
            languages.put("Kongo", "kg");
            languages.put("Kikuyu", "ki");
            languages.put("Kuanyama", "kj");
            languages.put("Kasachisch", "kk");
            languages.put("Kalaallisut (Gr\u00E4nland)", "kl");
            languages.put("Kambodschanisch", "km");
            languages.put("Kannada", "kn");
            languages.put("Koreanisch", "ko");
            languages.put("Kanuri", "kr");
            languages.put("Kaschmirisch", "ks");
            languages.put("Kurdisch", "ku");
            languages.put("Komi", "kv");
            languages.put("Kornisch", "kw");
            languages.put("Kirgisisch", "ky");
            languages.put("Lateinisch", "la");
            languages.put("Luxemburgisch", "lb");
            languages.put("Ganda", "lg");
            languages.put("Limburgisch", "li");
            languages.put("Lingala", "ln");
            languages.put("Laotisch", "lo");
            languages.put("Litauisch", "lt");
            languages.put("Kiluba", "lu");
            languages.put("Lettisch", "lv");
            languages.put("Malagasisch", "mg");
            languages.put("Marshallesische Sprache", "mh");
            languages.put("Maorisch", "mi");
            languages.put("Mazedonisch", "mk");
            languages.put("Malajalam", "ml");
            languages.put("Mongolisch", "mn");
            languages.put("Moldavisch", "mo");
            languages.put("Marathi", "mr");
            languages.put("Malaysisch", "ms");
            languages.put("Maltesisch", "mt");
            languages.put("Burmesisch", "my");
            languages.put("Nauruisch", "na");
            languages.put("Norwegisch Bokmål", "nb");
            languages.put("Nord isiNdebele", "nd");
            languages.put("Nepalisch", "ne");
            languages.put("Ndonga", "ng");
            languages.put("Holl\u00E4ndisch", "nl");
            languages.put("Norwegisch Nynorsk", "nn");
            languages.put("Norwegisch", "no");
            languages.put("S\u00FCd Ndebele", "nr");
            languages.put("Navajo", "nv");
            languages.put("Chichewa; Chewa; Nyanja", "ny");
            languages.put("Okzitanisch", "oc");
            languages.put("Ojibwe", "oj");
            languages.put("Oromo", "om");
            languages.put("Orija", "or");
            languages.put("Ossetisch", "os");
            languages.put("Pundjabisch", "pa");
            languages.put("Pali", "pi");
            languages.put("Polnisch", "pl");
            languages.put("Paschtu", "ps");
            languages.put("Portugiesisch", "pt");
            languages.put("Quechua", "qu");
            languages.put("Retoromanisch", "rm");
            languages.put("Kirundisch", "rn");
            languages.put("Rum\u00E4nisch", "ro");
            languages.put("Russisch", "ru");
            languages.put("Kijarwanda", "rw");
            languages.put("Sanskrit", "sa");
            languages.put("Sardische Sprache", "sc");
            languages.put("Zinti", "sd");
            languages.put("Nordsamisch", "se");
            languages.put("Sango", "sg");
            languages.put("Serbokroatisch (veraltet)", "sh");
            languages.put("Singhalesisch", "si");
            languages.put("Slowakisch", "sk");
            languages.put("Slowenisch", "sl");
            languages.put("Samoanisch", "sm");
            languages.put("Schonisch", "sn");
            languages.put("Somalisch", "so");
            languages.put("Albanisch", "sq");
            languages.put("Serbisch", "sr");
            languages.put("Swasil\u00E4ndisch", "ss");
            languages.put("Sesothisch", "st");
            languages.put("Sudanesisch", "su");
            languages.put("Schwedisch", "sv");
            languages.put("Suaheli", "sw");
            languages.put("Tamilisch", "ta");
            languages.put("Tegulu", "te");
            languages.put("Tadschikisch", "tg");
            languages.put("Thai", "th");
            languages.put("Tigrinja", "ti");
            languages.put("Turkmenisch", "tk");
            languages.put("Tagalog", "tl");
            languages.put("Sezuan", "tn");
            languages.put("Tongaisch", "to");
            languages.put("T\u00FCrkisch", "tr");
            languages.put("Tsongaisch", "ts");
            languages.put("Tatarisch", "tt");
            languages.put("Twi", "tw");
            languages.put("Tahitianisch", "ty");
            languages.put("Uigur", "ug");
            languages.put("Ukrainisch", "uk");
            languages.put("Urdu", "ur");
            languages.put("Usbekisch", "uz");
            languages.put("Vietnamesisch", "vi");
            languages.put("Volap\u00E4k", "vo");
            languages.put("Wallon", "wa");
            languages.put("Wolof", "wo");
            languages.put("Xhosa", "xh");
            languages.put("Jiddish (fr?her: ji)", "yi");
            languages.put("Joruba", "yo");
            languages.put("Zhuang", "za");
            languages.put("Chinesisch", "zh");
            languages.put("Zulu", "zu");
        } else if (Globals.CURRENT_LOCALE.getLanguage().equals(new Locale("it").getLanguage())) {
            languages.put("Lingue Afro-Asiatiche", "aa");
            languages.put("Abkazia", "ab");
            languages.put("Avestica", "ae");
            languages.put("Afrikaans", "af");
            languages.put("Akan", "ak");
            languages.put("Amarico", "am");
            languages.put("Aragonese", "an");
            languages.put("Arabo", "ar");
            languages.put("Assamese", "as");
            languages.put("Avaric", "av");
            languages.put("Aymara", "ay");
            languages.put("Azero", "az");
            languages.put("Bashkir", "ba");
            languages.put("Bielorusso", "be");
            languages.put("Bulgaro", "bg");
            languages.put("Bihari", "bh");
            languages.put("Bislama", "bi");
            languages.put("Bambara", "bm");
            languages.put("Bengali", "bn");
            languages.put("Tibetano", "bo");
            languages.put("Bretone", "br");
            languages.put("Bosanski jezik", "bs");
            languages.put("Catalano", "ca");
            languages.put("Lingua cecena", "ce");
            languages.put("Chamorro", "ch");
            languages.put("Corso", "co");
            languages.put("Cree", "cr");
            languages.put("Ceco", "cs");
            languages.put("Slavo ecclesiastico", "cu");
            languages.put("Ciuvascio", "cv");
            languages.put("Gallese", "cy");
            languages.put("Danese", "da");
            languages.put("Tedesco", "de");
            languages.put("Maldiviana", "dv");
            languages.put("Dzongkha", "dz");
            languages.put("Ewe", "ee");
            languages.put("Greco moderno", "el");
            languages.put("Inglese", "en");
            languages.put("Esperanto", "eo");
            languages.put("Spagnolo", "es");
            languages.put("Estone", "et");
            languages.put("Basco", "eu");
            languages.put("Persiano", "fa");
            languages.put("Fulah", "ff");
            languages.put("Finlandese", "fi");
            languages.put("Fijian", "fj");
            languages.put("Faroese", "fo");
            languages.put("Francese", "fr");
            languages.put("Frisone", "fy");
            languages.put("Gaelico irlandese", "ga");
            languages.put("Gaelico", "gd");
            languages.put("Galiziano", "gl");
            languages.put("Guarani", "gn");
            languages.put("Gujarati", "gu");
            languages.put("Mannese", "gv");
            languages.put("Hausa", "ha");
            languages.put("Ebraico", "he");
            languages.put("Hindi", "hi");
            languages.put("Croato", "hr");
            languages.put("Haitiano", "ht");
            languages.put("Ungherese", "hu");
            languages.put("Armeno", "hy");
            languages.put("Herero", "hz");
            languages.put("Interlingua (International Auxiliary Language Association)", "ia");
            languages.put("Indonesiano", "id");
            languages.put("Interlingue", "ie");
            languages.put("Igbo", "ig");
            languages.put("Sichuan Yi; Nuosu", "ii");
            languages.put("Inupiaq", "ik");
            languages.put("Indonesiano", "in");
            languages.put("Ido", "io");
            languages.put("Islandese", "is");
            languages.put("Italiano", "it");
            languages.put("Inuktitut", "iu");
            languages.put("Ebraico", "iw");
            languages.put("Giapponese", "ja");
            languages.put("Yiddish", "ji");
            languages.put("Lingua Giavanese", "jv");
            languages.put("Giavanese", "jw");
            languages.put("Georgiano", "ka");
            languages.put("Congo", "kg");
            languages.put("Kikuyu", "ki");
            languages.put("Kuanyama", "kj");
            languages.put("Kazakh", "kk");
            languages.put("Kalaallisut", "kl");
            languages.put("Khmer", "km");
            languages.put("Kannada", "kn");
            languages.put("Coreano", "ko");
            languages.put("Kanuri", "kr");
            languages.put("Kashmiri", "ks");
            languages.put("Curdo", "ku");
            languages.put("Komi", "kv");
            languages.put("Cornish", "kw");
            languages.put("Kirghiso", "ky");
            languages.put("Latino", "la");
            languages.put("Lussemburghese", "lb");
            languages.put("Ganda", "lg");
            languages.put("Limburgan", "li");
            languages.put("Lingala", "ln");
            languages.put("Lao", "lo");
            languages.put("Lituano", "lt");
            languages.put("Luba-Katanga", "lu");
            languages.put("Lettone", "lv");
            languages.put("Malagasy", "mg");
            languages.put("Marshallese", "mh");
            languages.put("Maori", "mi");
            languages.put("Macedone", "mk");
            languages.put("Malayalam", "ml");
            languages.put("Mongolo", "mn");
            languages.put("Moldavo", "mo");
            languages.put("Marathi", "mr");
            languages.put("Malese", "ms");
            languages.put("Maltese", "mt");
            languages.put("Burmese", "my");
            languages.put("Nauru", "na");
            languages.put("Norvegese Bokmål", "nb");
            languages.put("Nord Ndebele", "nd");
            languages.put("Nepalese", "ne");
            languages.put("Ndonga", "ng");
            languages.put("Neerlandese", "nl");
            languages.put("Norvegese Nynorsk", "nn");
            languages.put("Norvegese", "no");
            languages.put("Sud Ndebele", "nr");
            languages.put("Navajo", "nv");
            languages.put("Chichewa; Chewa; Nyanja", "ny");
            languages.put("Occitano", "oc");
            languages.put("Ojibwa", "oj");
            languages.put("Oromo", "om");
            languages.put("Oriya", "or");
            languages.put("Osseta", "os");
            languages.put("Panjabi", "pa");
            languages.put("Pali", "pi");
            languages.put("Polacco", "pl");
            languages.put("Pushto", "ps");
            languages.put("Portoghese", "pt");
            languages.put("Quechua", "qu");
            languages.put("Reto-Romanzo (Rumantsch grischun)", "rm");
            languages.put("Rundi", "rn");
            languages.put("Rumeno", "ro");
            languages.put("Russo", "ru");
            languages.put("Kinyarwanda", "rw");
            languages.put("Sanscrito", "sa");
            languages.put("Sardo", "sc");
            languages.put("Sindhi", "sd");
            languages.put("Sami del nord", "se");
            languages.put("Sango", "sg");
            languages.put("Shan", "sh");
            languages.put("Sinhalese", "si");
            languages.put("Slovacco", "sk");
            languages.put("Sloveno", "sl");
            languages.put("Samoano", "sm");
            languages.put("Shona", "sn");
            languages.put("Somalo", "so");
            languages.put("Albanese", "sq");
            languages.put("Serbo", "sr");
            languages.put("Swati", "ss");
            languages.put("Sotho, Meridionale", "st");
            languages.put("Sundanese", "su");
            languages.put("Svedese", "sv");
            languages.put("Swahili", "sw");
            languages.put("Tamil", "ta");
            languages.put("Telugu", "te");
            languages.put("Tajik", "tg");
            languages.put("Thai", "th");
            languages.put("Tigre", "ti");
            languages.put("Turkmen", "tk");
            languages.put("Tagalog", "tl");
            languages.put("Tswana", "tn");
            languages.put("Tonga (Nyasa)", "to");
            languages.put("Turco", "tr");
            languages.put("Tsonga", "ts");
            languages.put("Tatar", "tt");
            languages.put("Twi", "tw");
            languages.put("Tahitiano", "ty");
            languages.put("Uighur", "ug");
            languages.put("Ucraino", "uk");
            languages.put("Urdu", "ur");
            languages.put("Uzbeco", "uz");
            languages.put("Vietnamita", "vi");
            languages.put("Volap?k", "vo");
            languages.put("Wallon", "wa");
            languages.put("Wolof", "wo");
            languages.put("Xhosa", "xh");
            languages.put("Yiddish", "yi");
            languages.put("Yoruba", "yo");
            languages.put("Zhuang", "za");
            languages.put("Cinese", "zh");
            languages.put("Zulu", "zu");
        } else {
            languages.put("Afar", "aa");
            languages.put("Abkhazian", "ab");
            languages.put("Avestan", "ae");
            languages.put("Afrikaans", "af");
            languages.put("Akan", "ak");
            languages.put("Amharic", "am");
            languages.put("Aragonese", "an");
            languages.put("Arabic", "ar");
            languages.put("Assamese", "as");
            languages.put("Avaric", "av");
            languages.put("Aymara", "ay");
            languages.put("Azerbaijani", "az");
            languages.put("Bashkir", "ba");
            languages.put("Byelorussian", "be");
            languages.put("Bulgarian", "bg");
            languages.put("Bihari", "bh");
            languages.put("Bislama", "bi");
            languages.put("Bambara", "bm");
            languages.put("Bengali", "bn");
            languages.put("Tibetan", "bo");
            languages.put("Breton", "br");
            languages.put("Bosnian", "bs");
            languages.put("Catalan", "ca");
            languages.put("Chechen", "ce");
            languages.put("Chamorro", "ch");
            languages.put("Corsican", "co");
            languages.put("Cree", "cr");
            languages.put("Czech", "cs");
            languages.put("Church Slavic; Old Slavonic;", "cu");
            languages.put("Chuvash", "cv");
            languages.put("Welch", "cy");
            languages.put("Danish", "da");
            languages.put("German", "de");
            languages.put("Divehi; Dhivehi; Maldivian", "dv");
            languages.put("Bhutani", "dz");
            languages.put("Ewe", "ee");
            languages.put("Greek", "el");
            languages.put("English", "en");
            languages.put("Esperanto", "eo");
            languages.put("Spanish", "es");
            languages.put("Estonian", "et");
            languages.put("Basque", "eu");
            languages.put("Persian", "fa");
            languages.put("Fulah", "ff");
            languages.put("Finnish", "fi");
            languages.put("Fiji", "fj");
            languages.put("Faeroese", "fo");
            languages.put("French", "fr");
            languages.put("Frisian", "fy");
            languages.put("Irish", "ga");
            languages.put("Scots Gaelic", "gd");
            languages.put("Galician", "gl");
            languages.put("Guarani", "gn");
            languages.put("Gujarati", "gu");
            languages.put("Manx", "gv");
            languages.put("Hausa", "ha");
            languages.put("Hebrew", "he");
            languages.put("Hindi", "hi");
            languages.put("Croatian", "hr");
            languages.put("Haitian", "ht");
            languages.put("Hungarian", "hu");
            languages.put("Armenian ", "hy");
            languages.put("Herero", "hz");
            languages.put("Interlingua", "ia");
            languages.put("Indonesian", "id");
            languages.put("Interlingue", "ie");
            languages.put("Igbo", "ig");
            languages.put("Sichuan Yi; Nuosu", "ii");
            languages.put("Inupiak", "ik");
            languages.put("former Indonesian", "in");
            languages.put("Ido", "io");
            languages.put("Icelandic", "is");
            languages.put("Italian", "it");
            languages.put("Inuktitut (Eskimo)", "iu");
            languages.put("former Hebrew", "iw");
            languages.put("Japanese", "ja");
            languages.put("former Yiddish", "ji");
            languages.put("Javanese", "jv");
            languages.put("Javanese", "jw");
            languages.put("Georgian", "ka");
            languages.put("Kongo", "kg");
            languages.put("Kikuyu; Gikuyu", "ki");
            languages.put("Kuanyama; Kwanyama", "kj");
            languages.put("Kazakh", "kk");
            languages.put("Greenlandic", "kl");
            languages.put("Cambodian", "km");
            languages.put("Kannada", "kn");
            languages.put("Korean", "ko");
            languages.put("Kanuri", "kr");
            languages.put("Kashmiri", "ks");
            languages.put("Kurdish", "ku");
            languages.put("Komi", "kv");
            languages.put("Cornish", "kw");
            languages.put("Kirghiz", "ky");
            languages.put("Latin", "la");
            languages.put("Luxembourgish; Letzeburgesch", "lb");
            languages.put("Ganda", "lg");
            languages.put("Limburgan; Limburger; Limburgish", "li");
            languages.put("Lingala", "ln");
            languages.put("Laothian", "lo");
            languages.put("Lithuanian", "lt");
            languages.put("Luba-Katanga", "lu");
            languages.put("Latvian, Lettish", "lv");
            languages.put("Malagasy", "mg");
            languages.put("Marshallese", "mh");
            languages.put("Maori", "mi");
            languages.put("Macedonian", "mk");
            languages.put("Malayalam", "ml");
            languages.put("Mongolian", "mn");
            languages.put("Moldavian", "mo");
            languages.put("Marathi", "mr");
            languages.put("Malay", "ms");
            languages.put("Maltese", "mt");
            languages.put("Burmese", "my");
            languages.put("Nauru", "na");
            languages.put("Bokm?l, Norwegian; Norwegian Bokm?l", "nb");
            languages.put("Ndebele, North; North Ndebele", "nd");
            languages.put("Nepali", "ne");
            languages.put("Ndonga", "ng");
            languages.put("Dutch", "nl");
            languages.put("Norwegian Nynorsk; Nynorsk, Norwegian", "nn");
            languages.put("Norwegian", "no");
            languages.put("Ndebele, South; South Ndebele", "nr");
            languages.put("Navajo; Navaho", "nv");
            languages.put("Chichewa; Chewa; Nyanja", "ny");
            languages.put("Occitan", "oc");
            languages.put("Ojibwa", "oj");
            languages.put("(Afan) Oromo", "om");
            languages.put("Oriya", "or");
            languages.put("Ossetian; Ossetic", "os");
            languages.put("Punjabi", "pa");
            languages.put("Pali", "pi");
            languages.put("Polish", "pl");
            languages.put("Pashto, Pushto", "ps");
            languages.put("Portuguese", "pt");
            languages.put("Quechua", "qu");
            languages.put("Rhaeto-Romance", "rm");
            languages.put("Kirundi", "rn");
            languages.put("Romanian", "ro");
            languages.put("Russian", "ru");
            languages.put("Kinyarwanda", "rw");
            languages.put("Sanskrit", "sa");
            languages.put("Sardinian", "sc");
            languages.put("Sindhi", "sd");
            languages.put("Northern Sami", "se");
            languages.put("Sangro", "sg");
            languages.put("Serbo-Croatian", "sh");
            languages.put("Singhalese", "si");
            languages.put("Slovak", "sk");
            languages.put("Slovenian", "sl");
            languages.put("Samoan", "sm");
            languages.put("Shona", "sn");
            languages.put("Somali", "so");
            languages.put("Albanian", "sq");
            languages.put("Serbian", "sr");
            languages.put("Siswati", "ss");
            languages.put("Sesotho", "st");
            languages.put("Sudanese", "su");
            languages.put("Swedish", "sv");
            languages.put("Swahili", "sw");
            languages.put("Tamil", "ta");
            languages.put("Tegulu", "te");
            languages.put("Tajik", "tg");
            languages.put("Thai", "th");
            languages.put("Tigrinya", "ti");
            languages.put("Turkmen", "tk");
            languages.put("Tagalog", "tl");
            languages.put("Setswana", "tn");
            languages.put("Tonga", "to");
            languages.put("Turkish", "tr");
            languages.put("Tsonga", "ts");
            languages.put("Tatar", "tt");
            languages.put("Twi", "tw");
            languages.put("Tahitian", "ty");
            languages.put("Uigur", "ug");
            languages.put("Ukrainian", "uk");
            languages.put("Urdu", "ur");
            languages.put("Uzbek", "uz");
            languages.put("Vietnamese", "vi");
            languages.put("Volapuk", "vo");
            languages.put("Walloon", "wa");
            languages.put("Wolof", "wo");
            languages.put("Xhosa", "xh");
            languages.put("Yiddish", "yi");
            languages.put("Yoruba", "yo");
            languages.put("Zhuang", "za");
            languages.put("Chinese", "zh");
            languages.put("Zulu", "zu");
        }
        return languages;
    }
}
