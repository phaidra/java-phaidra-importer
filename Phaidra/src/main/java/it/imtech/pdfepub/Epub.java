/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package it.imtech.pdfepub;

import it.imtech.bookimporter.BookImporter;
import it.imtech.utility.Utility;
import it.imtech.metadata.MetaUtility;
import it.imtech.xmltree.XMLTree;
import it.imtech.globals.Globals;
import static it.imtech.pdfepub.PdfCreateMonitor.frame;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import nl.siegmann.epublib.domain.Author;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.epub.EpubWriter;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

/**
 *
 * @author mauro
 */
public class Epub extends javax.swing.JPanel implements java.beans.PropertyChangeListener {

    private Task task = null;
    private static JFrame frame = null;
    private static boolean disposed = false;
    private String pdfLocation = null;
    
    class Task extends SwingWorker<Void, Void> {
        /*
         * Main task. Executed in background thread.
         */

        @Override
        public Void doInBackground() {
            ResourceBundle bundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);
            
            int progress = 0;
            int total = Utility.countXmlTreeLeaves()+4;

            jProgressBar2.setMaximum(100);

            //Export Metadata
            BookImporter.getInstance().createComponentMap();
            String error = MetaUtility.getInstance().check_and_save_metadata(Globals.SELECTED_FOLDER_SEP + Globals.IMP_EXP_METADATA, false, true);

                    
            if (error.length() < 1) {
                try {
                    progress = addProgress(progress,total);
                    
                    XMLTree.exportBookstructure(Globals.SELECTED_FOLDER_SEP);
                    Book book = new Book();
                    addUploadInfoText(Utility.getBundleString("epubbookcreation",bundle));
                    
                    progress = addProgress(progress,total);
                    
                    String title = MetaUtility.getInstance().getObjectTitle();
                    book.getMetadata().addTitle(title);            
                    book.getMetadata().addAuthor(new Author("Phaidra","Importer"));
                    progress = addProgress(progress,total);
                    addUploadInfoText(Utility.getBundleString("epubbookmeta",bundle));
                    
                    progress = createEpub(book, progress, total);

                    EpubWriter epubWriter = new EpubWriter();
                    epubWriter.write(book, new FileOutputStream(pdfLocation));
                    progress = addProgress(progress,total);
                    addUploadInfoText(Utility.getBundleString("epubbookend",bundle));
                } catch (SAXException ex) {
                    addUploadInfoText(ex.getMessage());
                    Logger.getLogger(Epub.class.getName()).log(Level.SEVERE, null, ex);
                } catch (ParserConfigurationException ex) {
                    addUploadInfoText(ex.getMessage());
                    Logger.getLogger(Epub.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IOException ex) {
                    addUploadInfoText(ex.getMessage());
                    Logger.getLogger(Epub.class.getName()).log(Level.SEVERE, null, ex);
                } catch (TransformerConfigurationException ex) {
                    addUploadInfoText(ex.getMessage());
                    Logger.getLogger(Epub.class.getName()).log(Level.SEVERE, null, ex);
                } catch (TransformerException ex) {
                    addUploadInfoText(ex.getMessage());
                    Logger.getLogger(Epub.class.getName()).log(Level.SEVERE, null, ex);
                } catch (Exception ex) {
                    addUploadInfoText(ex.getMessage());
                    Logger.getLogger(Epub.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else {
                addUploadInfoText(error.toString());
            }
            return null;
        }

        private int createEpub(Book book, int progress, int total) throws SAXException, ParserConfigurationException, IOException, TransformerConfigurationException, TransformerException {
            ResourceBundle bundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);

            XMLTree.savedXmlDoc.getDocumentElement().normalize();

            NodeList nodeLst = XMLTree.savedXmlDoc.getElementsByTagName("book:structure");
            boolean startPage = true;

            int i = 0;
          
            //Scorri bookstructure per trovare coppie(Nome Capitolo, Nome File pagina)
            for (int s = 0; s < nodeLst.getLength() && !isCancelled(); s++) {
                if (nodeLst.item(s).getNodeType() == Node.ELEMENT_NODE) {
                    Element chapter = (Element) nodeLst.item(s);

                    String capitolo = chapter.getAttribute("name");

                    //Per ogni pagina del capitolo esegui l'upload
                    NodeList leaves = chapter.getChildNodes();
                    ArrayList<String> imglist = new ArrayList<String>();

                    for (int z = 0; z < leaves.getLength(); z++) {
                        if (leaves.item(z).getNodeType() == Node.ELEMENT_NODE) {
                            Element page = (Element) leaves.item(z);

                            imglist.add(page.getAttribute("href"));
                            addUploadInfoText(Utility.getBundleString("epubbookpage",bundle)+":"+page.getAttribute("pid"));
                        }
                    }
                    
                    java.io.InputStream c = createHtmlChapter(imglist, capitolo, i, startPage, book);
                    startPage = false;
                    i++;
                    book.addSection(capitolo, new Resource(c, capitolo + ".html"));
                    addUploadInfoText(Utility.getBundleString("epubbookchapter",bundle)+":"+capitolo);

                    Iterator it = imglist.iterator();
                    int z = 0;

                    while (it.hasNext()) {
                        String path = Globals.SELECTED_FOLDER_SEP + imglist.get(z);
                        InputStream fis = new FileInputStream(path);
                        
                        book.getResources().add(new Resource(fis, imglist.get(z)));
                        z++;
                        it.next();
                        progress = addProgress(progress,total);
                    }
                }
            }

            return progress;
        }

        private java.io.InputStream createHtmlChapter(ArrayList<String> imglist, String capitolo, int i,boolean startPage,Book book) throws ParserConfigurationException, TransformerConfigurationException, TransformerException, IOException {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            DOMImplementation domImpl = builder.getDOMImplementation();

            Document xmlDoc = domImpl.createDocument("http://www.w3.org/1999/xhtml", "html", null);

            Transformer t = TransformerFactory.newInstance().newTransformer();
            t.setOutputProperty(OutputKeys.INDENT, "yes");
            t.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            t.setOutputProperty(OutputKeys.STANDALONE, "");

            Element rootElement = xmlDoc.getDocumentElement();

            Element head = xmlDoc.createElement("head");
            rootElement.appendChild(head);

         
            Element title = xmlDoc.createElement("title");
            title.setTextContent("Prova");
            head.appendChild(title);
            
            
            Element meta = xmlDoc.createElement("meta");
            meta.setAttribute("http-equiv", "Content-Type");
            meta.setAttribute("content", "text/html; charset=utf-8");

            head.appendChild(meta);

            Iterator it = imglist.iterator();
            int z = 0;

            Element body = xmlDoc.createElement("body");
            rootElement.appendChild(body);
            
            //600x860
            while (it.hasNext()) {
                if(startPage){
                    startPage = false;
                    InputStream fis = new FileInputStream(Globals.SELECTED_FOLDER_SEP + imglist.get(z));
                    book.setCoverPage(new Resource(fis, imglist.get(z)));
                }
                     
                Element div = xmlDoc.createElement("div");
                div.setAttribute("width", "1024px");
                div.setAttribute("height","1024px");
                
                    Element img = xmlDoc.createElement("img");
                    img.setAttribute("alt", "");
                    img.setAttribute("src", imglist.get(z));
                    img.setAttribute("width",  "1024px");
                    img.setAttribute("height", "1024px");
                    
                    div.appendChild(img);
                
                body.appendChild(div);
                z++;
                it.next();
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Result outputTarget = new StreamResult(outputStream);

            t.transform(new DOMSource(xmlDoc), outputTarget);
            
            return new ByteArrayInputStream(outputStream.toByteArray());
        }

        /*
         * Executed in event dispatching thread
         */
        @Override
        public void done() {
            ResourceBundle bundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);
            jButton1.setEnabled(true);
            jButton1.setText(Utility.getBundleString("ok",bundle));
            jProgressBar2.setValue(100);
            setCursor(null);
        }
    }

    /**
     * Create the GUI and show it. As with all GUI code, this must run on the
     * event-dispatching thread.
     */
    public static void createAndShowGUI() {
        //Create and set up the window.
        ResourceBundle bundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);
        
        frame = new JFrame(Utility.getBundleString("createepub", bundle));
        frame.setDefaultCloseOperation(frame.DISPOSE_ON_CLOSE);

        //Create and set up the content pane.
        JComponent newContentPane = new Epub();

        if (!disposed) {
            newContentPane.setOpaque(true); //content panes must be opaque
            frame.setContentPane(newContentPane);

            //Display the window.
            frame.pack();
            
            //Posizionamento interfaccia
            Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
            int x = (dim.width - frame.getSize().width) / 2;
            int y = (dim.height - frame.getSize().height) / 2;
            frame.setLocation(x, y);
            frame.setVisible(true);
        } else {
            frame.dispose();
        }
    }
    
    /**
     * Invoked when task progress property changes.
     */
    public void propertyChange(PropertyChangeEvent evt) {
        if ("progress".equals(evt.getPropertyName())) {
            int progress = (Integer) evt.getNewValue();
            jProgressBar2.setValue(progress);
        }
    }

    protected int addProgress(int progress,int total){
        progress++;
        int percent = progress * 100 / total;
        jProgressBar2.setValue(percent);
        return progress;
    }
     
    private void addUploadInfoText(String add) {
        String text = jTextPane1.getText();
        text += "\n" + add;
        jTextPane1.setText(text);
        jTextPane1.revalidate();
    }

    /**
     * Creates new form Epub
     */
    private Epub() {
        ResourceBundle bundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);

        JFileChooser saveFile = new JFileChooser();//new save dialog  
       
        saveFile.setDialogTitle(Utility.getBundleString("selectfolder", bundle));
        saveFile.setApproveButtonText(Utility.getBundleString("saveepub", bundle));
        saveFile.setApproveButtonMnemonic('e');
        saveFile.setApproveButtonToolTipText(Utility.getBundleString("tooltipsaveepub", bundle));
        
        saveFile.setLocale(Globals.CURRENT_LOCALE);
        saveFile.updateUI();
        
        FileFilter filter = new FileNameExtensionFilter("epub", "epub");
        saveFile.addChoosableFileFilter(filter);
        
        int ret = saveFile.showSaveDialog(BookImporter.getInstance());
        boolean selected = false;

        if (ret == JFileChooser.APPROVE_OPTION) {
            File f = saveFile.getSelectedFile();

            String filePath = f.getAbsolutePath();
            if (!filePath.toLowerCase().endsWith(".epub")) {
                f = new File(filePath + ".epub");
            }

            pdfLocation = f.getAbsolutePath();

            if (new File(pdfLocation).isFile()) {
                Object[] options = {Utility.getBundleString("voc1",bundle), Utility.getBundleString("voc2",bundle)};
                int n = JOptionPane.showOptionDialog(this, Utility.getBundleString("fileexists",bundle),
                        Utility.getBundleString("fileexists",bundle),
                        JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

                if (n == JOptionPane.YES_OPTION) {
                    selected = true;
                }
            } else {
                selected = true;
            }
        }

        disposed = !selected;

        if (selected == true) {
            initComponents();
            updateLanguageLabel();

            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            task = new Epub.Task();
            task.addPropertyChangeListener(this);
            task.execute();

            jButton1.setEnabled(false);
            jButton1.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    //Close the application main form
                    task.cancel(true);
                    frame.dispose();
                }
            });
            
            frame.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                    task.cancel(true);
                    frame.dispose();
                }
            });
        }
    }
    
    private void updateLanguageLabel() {
        ResourceBundle bundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);
        jLabel3.setText(Utility.getBundleString("exportpdf1",bundle));
        jLabel2.setText(Utility.getBundleString("exportpdf2",bundle));
        jButton1.setText(Utility.getBundleString("exportbutt",bundle));
       
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
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextPane1 = new javax.swing.JTextPane();
        jPanel2 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jProgressBar2 = new javax.swing.JProgressBar();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(""));

        jScrollPane1.setViewportView(jTextPane1);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 160, Short.MAX_VALUE)
                .addContainerGap())
        );

        jLabel2.setText("Elaborazione");

        jLabel3.setText("Sto elaborando il file");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 126, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 28, Short.MAX_VALUE)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jProgressBar2, javax.swing.GroupLayout.DEFAULT_SIZE, 449, Short.MAX_VALUE)
                    .addComponent(jLabel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(19, 19, 19))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(18, 18, 18)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jProgressBar2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        jButton1.setText("jButton1");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(26, 26, 26)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jButton1)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap(16, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton1)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JProgressBar jProgressBar2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextPane jTextPane1;
    // End of variables declaration//GEN-END:variables
}
