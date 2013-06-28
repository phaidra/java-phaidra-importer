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
import it.imtech.bookimporter.BookImporter;
import it.imtech.globals.Globals;
import it.imtech.upload.PhaidraUtils;
import it.imtech.xmltree.XMLTree;
import java.awt.Frame;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.FileNameMap;
import java.net.MalformedURLException;
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
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.*;
import org.apache.commons.lang.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
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

    public static String getOuterXml(Node node) throws TransformerConfigurationException, TransformerException {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty("omit-xml-declaration", "yes");

        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(node), new StreamResult(writer));
        return writer.toString();
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

    public static String getMimeType(String filename) {
        FileNameMap fileNameMap = URLConnection.getFileNameMap();
        return fileNameMap.getContentTypeFor(filename);
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
    public static void chooseBookOrCollection() {
        ResourceBundle bundle = ResourceBundle.getBundle(BookImporter.RESOURCES, BookImporter.currentLocale, Globals.loader);

        Object[] options = {Utility.getBundleString("collection", bundle), Utility.getBundleString("book", bundle), Utility.getBundleString("back", bundle)};
        int answer = JOptionPane.showOptionDialog(null,
                Utility.getBundleString("collorbook", bundle),
                Utility.getBundleString("titlecollobook", bundle),
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null, options, options[1]);

        switch (answer) {
            case JOptionPane.YES_OPTION:
                BookImporter.TYPEBOOK = Globals.COLLECTION;
                break;
            case JOptionPane.NO_OPTION:
                BookImporter.TYPEBOOK = Globals.BOOK;
                break;
            case JOptionPane.CANCEL_OPTION:
                BookImporter.TYPEBOOK = Globals.NOT_EXISTS;
                break;
            default:
                JOptionPane.showMessageDialog(new Frame(), "Program Exit");
                System.exit(0);
                break;
        }
    }

    public static int countXmlTreeLeaves() {
        int leaves = 0;

        try {
            leaves = XMLTree.getRoot().getLeafCount();
        } catch (Error ex) {
            leaves = 0;
        }
        return leaves;
    }
    
    public static Document getDocument(String filename,boolean classif) throws MalformedURLException, ParserConfigurationException, SAXException, IOException{
       DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
       DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
       
       File urlf;
       URL url;
       Document doc;
       String path;
      
       if(Globals.DEBUG) {      
           if(classif){
               int i = filename.lastIndexOf("/");
               String name = filename.substring(i, filename.length());
               path = filename.replace(PhaidraUtils.getInstance(null).getHTTPStaticBaseURL(),BookImporter.jrPath+Globals.DEBUG_FOLDER);
           }
            else
            path = BookImporter.jrPath+Globals.DEBUG_FOLDER+Utility.getSep()+filename;
          
          urlf = new File(path);
       }
       else {
            path = (classif)?filename:PhaidraUtils.getInstance(null).getHTTPStaticBaseURL()+filename;
            url = new URL(path);
        }
       
        if(Globals.DEBUG)
            doc = dBuilder.parse(urlf);
        else
            doc = dBuilder.parse(url.openStream());
    
        return doc;
    }

    public static TreeMap<String, String> getLanguages() {
        TreeMap<String, String> languages = new TreeMap<String, String>();

        if (BookImporter.currentLocale.getLanguage().equals(new Locale("de").getLanguage())) {
            languages.put("aa", "Afar");
            languages.put("ab", "Abchasisch");
            languages.put("ae", "Avestisch");
            languages.put("af", "Afrikaans");
            languages.put("ak", "Akan");
            languages.put("am", "Amharisch");
            languages.put("an", "Aragonesisch");
            languages.put("ar", "Arabisch");
            languages.put("as", "Assamesisch");
            languages.put("av", "Avaric");
            languages.put("ay", "Aymara");
            languages.put("az", "Aserbaidschanisch");
            languages.put("ba", "Baschkirisch");
            languages.put("be", "Belorussisch");
            languages.put("bg", "Bulgarisch");
            languages.put("bh", "Biharisch");
            languages.put("bi", "Bislamisch");
            languages.put("bm", "Bambara");
            languages.put("bn", "Bengalisch");
            languages.put("bo", "Tibetanisch");
            languages.put("br", "Bretonisch");
            languages.put("bs", "Bosnisch");
            languages.put("ca", "Katalanisch");
            languages.put("ce", "Tschetschenisch");
            languages.put("ch", "Chamorro");
            languages.put("co", "Korsisch");
            languages.put("cr", "Cree");
            languages.put("cs", "Tschechisch");
            languages.put("cu", "Kirchenslawisch");
            languages.put("cv", "Tschuwasch");
            languages.put("cy", "Walisisch");
            languages.put("da", "D�nisch");
            languages.put("de", "Deutsch");
            languages.put("dv", "Dhivehi");
            languages.put("dz", "Dzongkha, Bhutani");
            languages.put("ee", "Ewe");
            languages.put("el", "Griechisch");
            languages.put("en", "Englisch");
            languages.put("eo", "Esperanto");
            languages.put("es", "Spanisch");
            languages.put("et", "Estnisch");
            languages.put("eu", "Baskisch");
            languages.put("fa", "Persisch");
            languages.put("ff", "Fulah");
            languages.put("fi", "Finnisch");
            languages.put("fj", "Fiji");
            languages.put("fo", "Far�isch");
            languages.put("fr", "Franz�sisch");
            languages.put("fy", "Friesisch");
            languages.put("ga", "Irisch");
            languages.put("gd", "Schottisches G�lisch");
            languages.put("gl", "Galizisch");
            languages.put("gn", "Guarani");
            languages.put("gu", "Gujaratisch");
            languages.put("gv", "Manx");
            languages.put("ha", "Haussa");
            languages.put("he", "Hebr�isch");
            languages.put("hi", "Hindi");
            languages.put("hr", "Kroatisch");
            languages.put("ht", "Haitianisch");
            languages.put("hu", "Ungarisch");
            languages.put("hy", "Armenisch");
            languages.put("hz", "Herero");
            languages.put("ia", "Interlingua");
            languages.put("id", "Indonesisch");
            languages.put("ie", "Interlingue");
            languages.put("ig", "Igbo");
            languages.put("ii", "Sichuan Yi; Nuosu");
            languages.put("ik", "Inupiak");
            languages.put("in", "Indonesisch");
            languages.put("io", "Ido");
            languages.put("is", "Isl�ndisch");
            languages.put("it", "Italienisch");
            languages.put("iu", "Inuktitut");
            languages.put("iw", "Hebr�isch (veraltet, nun: he)");
            languages.put("ja", "Japanisch");
            languages.put("ji", "Jiddish (veraltet, nun: yi)");
            languages.put("jv", "Javanesisch");
            languages.put("jw", "Javanisch");
            languages.put("ka", "Georgisch");
            languages.put("kg", "Kongo");
            languages.put("ki", "Kikuyu");
            languages.put("kj", "Kuanyama");
            languages.put("kk", "Kasachisch");
            languages.put("kl", "Kalaallisut (Gr�nland)");
            languages.put("km", "Kambodschanisch");
            languages.put("kn", "Kannada");
            languages.put("ko", "Koreanisch");
            languages.put("kr", "Kanuri");
            languages.put("ks", "Kaschmirisch");
            languages.put("ku", "Kurdisch");
            languages.put("kv", "Komi");
            languages.put("kw", "Kornisch");
            languages.put("ky", "Kirgisisch");
            languages.put("la", "Lateinisch");
            languages.put("lb", "Luxemburgisch");
            languages.put("lg", "Ganda");
            languages.put("li", "Limburgisch");
            languages.put("ln", "Lingala");
            languages.put("lo", "Laotisch");
            languages.put("lt", "Litauisch");
            languages.put("lu", "Kiluba");
            languages.put("lv", "Lettisch");
            languages.put("mg", "Malagasisch");
            languages.put("mh", "Marshallesische Sprache");
            languages.put("mi", "Maorisch");
            languages.put("mk", "Mazedonisch");
            languages.put("ml", "Malajalam");
            languages.put("mn", "Mongolisch");
            languages.put("mo", "Moldavisch");
            languages.put("mr", "Marathi");
            languages.put("ms", "Malaysisch");
            languages.put("mt", "Maltesisch");
            languages.put("my", "Burmesisch");
            languages.put("na", "Nauruisch");
            languages.put("nb", "Norwegisch Bokmål");
            languages.put("nd", "Nord isiNdebele");
            languages.put("ne", "Nepalisch");
            languages.put("ng", "Ndonga");
            languages.put("nl", "Holl�ndisch");
            languages.put("nn", "Norwegisch Nynorsk");
            languages.put("no", "Norwegisch");
            languages.put("nr", "Süd Ndebele");
            languages.put("nv", "Navajo");
            languages.put("ny", "Chichewa; Chewa; Nyanja");
            languages.put("oc", "Okzitanisch");
            languages.put("oj", "Ojibwe");
            languages.put("om", "Oromo");
            languages.put("or", "Orija");
            languages.put("os", "Ossetisch");
            languages.put("pa", "Pundjabisch");
            languages.put("pi", "Pali");
            languages.put("pl", "Polnisch");
            languages.put("ps", "Paschtu");
            languages.put("pt", "Portugiesisch");
            languages.put("qu", "Quechua");
            languages.put("rm", "R�toromanisch");
            languages.put("rn", "Kirundisch");
            languages.put("ro", "Rum�nisch");
            languages.put("ru", "Russisch");
            languages.put("rw", "Kijarwanda");
            languages.put("sa", "Sanskrit");
            languages.put("sc", "Sardische Sprache");
            languages.put("sd", "Zinti");
            languages.put("se", "Nordsamisch");
            languages.put("sg", "Sango");
            languages.put("sh", "Serbokroatisch (veraltet)");
            languages.put("si", "Singhalesisch");
            languages.put("sk", "Slowakisch");
            languages.put("sl", "Slowenisch");
            languages.put("sm", "Samoanisch");
            languages.put("sn", "Schonisch");
            languages.put("so", "Somalisch");
            languages.put("sq", "Albanisch");
            languages.put("sr", "Serbisch");
            languages.put("ss", "Swasil�ndisch");
            languages.put("st", "Sesothisch");
            languages.put("su", "Sudanesisch");
            languages.put("sv", "Schwedisch");
            languages.put("sw", "Suaheli");
            languages.put("ta", "Tamilisch");
            languages.put("te", "Tegulu");
            languages.put("tg", "Tadschikisch");
            languages.put("th", "Thai");
            languages.put("ti", "Tigrinja");
            languages.put("tk", "Turkmenisch");
            languages.put("tl", "Tagalog");
            languages.put("tn", "Sezuan");
            languages.put("to", "Tongaisch");
            languages.put("tr", "T�rkisch");
            languages.put("ts", "Tsongaisch");
            languages.put("tt", "Tatarisch");
            languages.put("tw", "Twi");
            languages.put("ty", "Tahitianisch");
            languages.put("ug", "Uigur");
            languages.put("uk", "Ukrainisch");
            languages.put("ur", "Urdu");
            languages.put("uz", "Usbekisch");
            languages.put("vi", "Vietnamesisch");
            languages.put("vo", "Volap�k");
            languages.put("wa", "Wallon");
            languages.put("wo", "Wolof");
            languages.put("xh", "Xhosa");
            languages.put("yi", "Jiddish (fr�her: ji)");
            languages.put("yo", "Joruba");
            languages.put("za", "Zhuang");
            languages.put("zh", "Chinesisch");
            languages.put("zu", "Zulu");
        } else if (BookImporter.currentLocale.getLanguage().equals(new Locale("it").getLanguage())) {
            languages.put("aa", "Lingue Afro-Asiatiche");
            languages.put("ab", "Abkazia");
            languages.put("ae", "Avestica");
            languages.put("af", "Afrikaans");
            languages.put("ak", "Akan");
            languages.put("am", "Amarico");
            languages.put("an", "Aragonese");
            languages.put("ar", "Arabo");
            languages.put("as", "Assamese");
            languages.put("av", "Avaric");
            languages.put("ay", "Aymara");
            languages.put("az", "Azero");
            languages.put("ba", "Bashkir");
            languages.put("be", "Bielorusso");
            languages.put("bg", "Bulgaro");
            languages.put("bh", "Bihari");
            languages.put("bi", "Bislama");
            languages.put("bm", "Bambara");
            languages.put("bn", "Bengali");
            languages.put("bo", "Tibetano");
            languages.put("br", "Bretone");
            languages.put("bs", "Bosanski jezik");
            languages.put("ca", "Catalano");
            languages.put("ce", "Lingua cecena");
            languages.put("ch", "Chamorro");
            languages.put("co", "Corso");
            languages.put("cr", "Cree");
            languages.put("cs", "Ceco");
            languages.put("cu", "Slavo ecclesiastico");
            languages.put("cv", "Ciuvascio");
            languages.put("cy", "Gallese");
            languages.put("da", "Danese");
            languages.put("de", "Tedesco");
            languages.put("dv", "Maldiviana");
            languages.put("dz", "Dzongkha");
            languages.put("ee", "Ewe");
            languages.put("el", "Greco moderno");
            languages.put("en", "Inglese");
            languages.put("eo", "Esperanto");
            languages.put("es", "Spagnolo");
            languages.put("et", "Estone");
            languages.put("eu", "Basco");
            languages.put("fa", "Persiano");
            languages.put("ff", "Fulah");
            languages.put("fi", "Finlandese");
            languages.put("fj", "Fijian");
            languages.put("fo", "Faroese");
            languages.put("fr", "Francese");
            languages.put("fy", "Frisone");
            languages.put("ga", "Gaelico irlandese");
            languages.put("gd", "Gaelico");
            languages.put("gl", "Galiziano");
            languages.put("gn", "Guarani");
            languages.put("gu", "Gujarati");
            languages.put("gv", "Mannese");
            languages.put("ha", "Hausa");
            languages.put("he", "Ebraico");
            languages.put("hi", "Hindi");
            languages.put("hr", "Croato");
            languages.put("ht", "Haitiano");
            languages.put("hu", "Ungherese");
            languages.put("hy", "Armeno");
            languages.put("hz", "Herero");
            languages.put("ia", "Interlingua (International Auxiliary Language Association)");
            languages.put("id", "Indonesiano");
            languages.put("ie", "Interlingue");
            languages.put("ig", "Igbo");
            languages.put("ii", "Sichuan Yi; Nuosu");
            languages.put("ik", "Inupiaq");
            languages.put("in", "Indonesiano");
            languages.put("io", "Ido");
            languages.put("is", "Islandese");
            languages.put("it", "Italiano");
            languages.put("iu", "Inuktitut");
            languages.put("iw", "Ebraico");
            languages.put("ja", "Giapponese");
            languages.put("ji", "Yiddish");
            languages.put("jv", "Lingua Giavanese");
            languages.put("jw", "Giavanese");
            languages.put("ka", "Georgiano");
            languages.put("kg", "Congo");
            languages.put("ki", "Kikuyu");
            languages.put("kj", "Kuanyama");
            languages.put("kk", "Kazakh");
            languages.put("kl", "Kalaallisut");
            languages.put("km", "Khmer");
            languages.put("kn", "Kannada");
            languages.put("ko", "Coreano");
            languages.put("kr", "Kanuri");
            languages.put("ks", "Kashmiri");
            languages.put("ku", "Curdo");
            languages.put("kv", "Komi");
            languages.put("kw", "Cornish");
            languages.put("ky", "Kirghiso");
            languages.put("la", "Latino");
            languages.put("lb", "Lussemburghese");
            languages.put("lg", "Ganda");
            languages.put("li", "Limburgan");
            languages.put("ln", "Lingala");
            languages.put("lo", "Lao");
            languages.put("lt", "Lituano");
            languages.put("lu", "Luba-Katanga");
            languages.put("lv", "Lettone");
            languages.put("mg", "Malagasy");
            languages.put("mh", "Marshallese");
            languages.put("mi", "Maori");
            languages.put("mk", "Macedone");
            languages.put("ml", "Malayalam");
            languages.put("mn", "Mongolo");
            languages.put("mo", "Moldavo");
            languages.put("mr", "Marathi");
            languages.put("ms", "Malese");
            languages.put("mt", "Maltese");
            languages.put("my", "Burmese");
            languages.put("na", "Nauru");
            languages.put("nb", "Norvegese Bokmål");
            languages.put("nd", "Nord Ndebele");
            languages.put("ne", "Nepalese");
            languages.put("ng", "Ndonga");
            languages.put("nl", "Neerlandese");
            languages.put("nn", "Norvegese Nynorsk");
            languages.put("no", "Norvegese");
            languages.put("nr", "Sud Ndebele");
            languages.put("nv", "Navajo");
            languages.put("ny", "Chichewa; Chewa; Nyanja");
            languages.put("oc", "Occitano");
            languages.put("oj", "Ojibwa");
            languages.put("om", "Oromo");
            languages.put("or", "Oriya");
            languages.put("os", "Osseta");
            languages.put("pa", "Panjabi");
            languages.put("pi", "Pali");
            languages.put("pl", "Polacco");
            languages.put("ps", "Pushto");
            languages.put("pt", "Portoghese");
            languages.put("qu", "Quechua");
            languages.put("rm", "Reto-Romanzo (Rumantsch grischun)");
            languages.put("rn", "Rundi");
            languages.put("ro", "Rumeno");
            languages.put("ru", "Russo");
            languages.put("rw", "Kinyarwanda");
            languages.put("sa", "Sanscrito");
            languages.put("sc", "Sardo");
            languages.put("sd", "Sindhi");
            languages.put("se", "Sami del nord");
            languages.put("sg", "Sango");
            languages.put("sh", "Shan");
            languages.put("si", "Sinhalese");
            languages.put("sk", "Slovacco");
            languages.put("sl", "Sloveno");
            languages.put("sm", "Samoano");
            languages.put("sn", "Shona");
            languages.put("so", "Somalo");
            languages.put("sq", "Albanese");
            languages.put("sr", "Serbo");
            languages.put("ss", "Swati");
            languages.put("st", "Sotho, Meridionale");
            languages.put("su", "Sundanese");
            languages.put("sv", "Svedese");
            languages.put("sw", "Swahili");
            languages.put("ta", "Tamil");
            languages.put("te", "Telugu");
            languages.put("tg", "Tajik");
            languages.put("th", "Thai");
            languages.put("ti", "Tigre");
            languages.put("tk", "Turkmen");
            languages.put("tl", "Tagalog");
            languages.put("tn", "Tswana");
            languages.put("to", "Tonga (Nyasa)");
            languages.put("tr", "Turco");
            languages.put("ts", "Tsonga");
            languages.put("tt", "Tatar");
            languages.put("tw", "Twi");
            languages.put("ty", "Tahitiano");
            languages.put("ug", "Uighur");
            languages.put("uk", "Ucraino");
            languages.put("ur", "Urdu");
            languages.put("uz", "Uzbeco");
            languages.put("vi", "Vietnamita");
            languages.put("vo", "Volap�k");
            languages.put("wa", "Wallon");
            languages.put("wo", "Wolof");
            languages.put("xh", "Xhosa");
            languages.put("yi", "Yiddish");
            languages.put("yo", "Yoruba");
            languages.put("za", "Zhuang");
            languages.put("zh", "Cinese");
            languages.put("zu", "Zulu");
        } else {
            languages.put("aa", "Afar");
            languages.put("ab", "Abkhazian");
            languages.put("ae", "Avestan");
            languages.put("af", "Afrikaans");
            languages.put("ak", "Akan");
            languages.put("am", "Amharic");
            languages.put("an", "Aragonese");
            languages.put("ar", "Arabic");
            languages.put("as", "Assamese");
            languages.put("av", "Avaric");
            languages.put("ay", "Aymara");
            languages.put("az", "Azerbaijani");
            languages.put("ba", "Bashkir");
            languages.put("be", "Byelorussian");
            languages.put("bg", "Bulgarian");
            languages.put("bh", "Bihari");
            languages.put("bi", "Bislama");
            languages.put("bm", "Bambara");
            languages.put("bn", "Bengali");
            languages.put("bo", "Tibetan");
            languages.put("br", "Breton");
            languages.put("bs", "Bosnian");
            languages.put("ca", "Catalan");
            languages.put("ce", "Chechen");
            languages.put("ch", "Chamorro");
            languages.put("co", "Corsican");
            languages.put("cr", "Cree");
            languages.put("cs", "Czech");
            languages.put("cu", "Church Slavic; Old Slavonic;");
            languages.put("cv", "Chuvash");
            languages.put("cy", "Welch");
            languages.put("da", "Danish");
            languages.put("de", "German");
            languages.put("dv", "Divehi; Dhivehi; Maldivian");
            languages.put("dz", "Bhutani");
            languages.put("ee", "Ewe");
            languages.put("el", "Greek");
            languages.put("en", "English");
            languages.put("eo", "Esperanto");
            languages.put("es", "Spanish");
            languages.put("et", "Estonian");
            languages.put("eu", "Basque");
            languages.put("fa", "Persian");
            languages.put("ff", "Fulah");
            languages.put("fi", "Finnish");
            languages.put("fj", "Fiji");
            languages.put("fo", "Faeroese");
            languages.put("fr", "French");
            languages.put("fy", "Frisian");
            languages.put("ga", "Irish");
            languages.put("gd", "Scots Gaelic");
            languages.put("gl", "Galician");
            languages.put("gn", "Guarani");
            languages.put("gu", "Gujarati");
            languages.put("gv", "Manx");
            languages.put("ha", "Hausa");
            languages.put("he", "Hebrew");
            languages.put("hi", "Hindi");
            languages.put("hr", "Croatian");
            languages.put("ht", "Haitian");
            languages.put("hu", "Hungarian");
            languages.put("hy", "Armenian ");
            languages.put("hz", "Herero");
            languages.put("ia", "Interlingua");
            languages.put("id", "Indonesian");
            languages.put("ie", "Interlingue");
            languages.put("ig", "Igbo");
            languages.put("ii", "Sichuan Yi; Nuosu");
            languages.put("ik", "Inupiak");
            languages.put("in", "former Indonesian");
            languages.put("io", "Ido");
            languages.put("is", "Icelandic");
            languages.put("it", "Italian");
            languages.put("iu", "Inuktitut (Eskimo)");
            languages.put("iw", "former Hebrew");
            languages.put("ja", "Japanese");
            languages.put("ji", "former Yiddish");
            languages.put("jv", "Javanese");
            languages.put("jw", "Javanese");
            languages.put("ka", "Georgian");
            languages.put("kg", "Kongo");
            languages.put("ki", "Kikuyu; Gikuyu");
            languages.put("kj", "Kuanyama; Kwanyama");
            languages.put("kk", "Kazakh");
            languages.put("kl", "Greenlandic");
            languages.put("km", "Cambodian");
            languages.put("kn", "Kannada");
            languages.put("ko", "Korean");
            languages.put("kr", "Kanuri");
            languages.put("ks", "Kashmiri");
            languages.put("ku", "Kurdish");
            languages.put("kv", "Komi");
            languages.put("kw", "Cornish");
            languages.put("ky", "Kirghiz");
            languages.put("la", "Latin");
            languages.put("lb", "Luxembourgish; Letzeburgesch");
            languages.put("lg", "Ganda");
            languages.put("li", "Limburgan; Limburger; Limburgish");
            languages.put("ln", "Lingala");
            languages.put("lo", "Laothian");
            languages.put("lt", "Lithuanian");
            languages.put("lu", "Luba-Katanga");
            languages.put("lv", "Latvian, Lettish");
            languages.put("mg", "Malagasy");
            languages.put("mh", "Marshallese");
            languages.put("mi", "Maori");
            languages.put("mk", "Macedonian");
            languages.put("ml", "Malayalam");
            languages.put("mn", "Mongolian");
            languages.put("mo", "Moldavian");
            languages.put("mr", "Marathi");
            languages.put("ms", "Malay");
            languages.put("mt", "Maltese");
            languages.put("my", "Burmese");
            languages.put("na", "Nauru");
            languages.put("nb", "Bokm�l, Norwegian; Norwegian Bokm�l");
            languages.put("nd", "Ndebele, North; North Ndebele");
            languages.put("ne", "Nepali");
            languages.put("ng", "Ndonga");
            languages.put("nl", "Dutch");
            languages.put("nn", "Norwegian Nynorsk; Nynorsk, Norwegian");
            languages.put("no", "Norwegian");
            languages.put("nr", "Ndebele, South; South Ndebele");
            languages.put("nv", "Navajo; Navaho");
            languages.put("ny", "Chichewa; Chewa; Nyanja");
            languages.put("oc", "Occitan");
            languages.put("oj", "Ojibwa");
            languages.put("om", "(Afan) Oromo");
            languages.put("or", "Oriya");
            languages.put("os", "Ossetian; Ossetic");
            languages.put("pa", "Punjabi");
            languages.put("pi", "Pali");
            languages.put("pl", "Polish");
            languages.put("ps", "Pashto, Pushto");
            languages.put("pt", "Portuguese");
            languages.put("qu", "Quechua");
            languages.put("rm", "Rhaeto-Romance");
            languages.put("rn", "Kirundi");
            languages.put("ro", "Romanian");
            languages.put("ru", "Russian");
            languages.put("rw", "Kinyarwanda");
            languages.put("sa", "Sanskrit");
            languages.put("sc", "Sardinian");
            languages.put("sd", "Sindhi");
            languages.put("se", "Northern Sami");
            languages.put("sg", "Sangro");
            languages.put("sh", "Serbo-Croatian");
            languages.put("si", "Singhalese");
            languages.put("sk", "Slovak");
            languages.put("sl", "Slovenian");
            languages.put("sm", "Samoan");
            languages.put("sn", "Shona");
            languages.put("so", "Somali");
            languages.put("sq", "Albanian");
            languages.put("sr", "Serbian");
            languages.put("ss", "Siswati");
            languages.put("st", "Sesotho");
            languages.put("su", "Sudanese");
            languages.put("sv", "Swedish");
            languages.put("sw", "Swahili");
            languages.put("ta", "Tamil");
            languages.put("te", "Tegulu");
            languages.put("tg", "Tajik");
            languages.put("th", "Thai");
            languages.put("ti", "Tigrinya");
            languages.put("tk", "Turkmen");
            languages.put("tl", "Tagalog");
            languages.put("tn", "Setswana");
            languages.put("to", "Tonga");
            languages.put("tr", "Turkish");
            languages.put("ts", "Tsonga");
            languages.put("tt", "Tatar");
            languages.put("tw", "Twi");
            languages.put("ty", "Tahitian");
            languages.put("ug", "Uigur");
            languages.put("uk", "Ukrainian");
            languages.put("ur", "Urdu");
            languages.put("uz", "Uzbek");
            languages.put("vi", "Vietnamese");
            languages.put("vo", "Volapuk");
            languages.put("wa", "Walloon");
            languages.put("wo", "Wolof");
            languages.put("xh", "Xhosa");
            languages.put("yi", "Yiddish");
            languages.put("yo", "Yoruba");
            languages.put("za", "Zhuang");
            languages.put("zh", "Chinese");
            languages.put("zu", "Zulu");
        }
        return languages;
    }
}
