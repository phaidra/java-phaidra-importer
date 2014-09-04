/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package it.imtech.pdfepub;

import com.itextpdf.text.BadElementException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.pdf.PdfWriter;
import it.imtech.bookimporter.BookImporter;
import it.imtech.globals.Globals;
import it.imtech.utility.Utility;
import it.imtech.xmltree.XMLNode;
import it.imtech.xmltree.XMLPage;
import it.imtech.xmltree.XMLTree;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.RenderedImage;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingWorker;
import org.ghost4j.document.PDFDocument;
import org.ghost4j.renderer.SimpleRenderer;


/**
 *
 * @author mauro
 */
public class PdfCreateMonitor extends javax.swing.JPanel implements java.beans.PropertyChangeListener {
    //Gestore dei log
    public final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(PdfCreateMonitor.class);
    
    private String pdfLocation = null;
    private Task task;
    private static boolean disposed = false;
    private boolean export = false;
    protected static JFrame frame = null;
    
    protected class Task extends SwingWorker<Void, Void> {
        /*
         * Main task. Executed in background thread.
         */

        @Override
        public Void doInBackground() {
            if (export) {
                exportPdf();
            } else {
                importPdf();
                //BookImporter.getInstance().initializeXmlTree(true);
            }

            return null;
        }

        private void importPdf() {
            FileOutputStream fos = null;
            
            if (System.getProperty("os.name").toLowerCase().startsWith("win")) {
                System.load(Globals.JRPATH+"lib"+Utility.getSep()+"gsdll32.dll");
            }
            
            try {
                int progress = 0;
                int percent = 0;
                jProgressBar1.setMaximum(100);

                String prefix = Globals.SELECTED_FOLDER_SEP + "autogen";
                String suffix = "jpg";
                PDFDocument document = new PDFDocument();
                document.load(new File(pdfLocation));

                ArrayList<String> newPages = new ArrayList<String>();

                // create renderer
                SimpleRenderer renderer = new SimpleRenderer();
                // set resolution (in DPI)
                renderer.setResolution(200);
                int pages = document.getPageCount();
                java.util.List<java.awt.Image> images;
                for (int i = 0; i < pages && !isCancelled(); i++) {
                    try {
                        images = renderer.render(document, i, i + 1);
                        File file = Utility.getUniqueFileName(prefix, suffix);
                        ImageIO.write((RenderedImage) images.get(0), "jpg", file);

                        newPages.add(file.getName());
                        jLabel4.setText(file.getName());
                        progress++;
                        percent = progress * 100 / pages;
                        jProgressBar1.setValue(percent);
                    } catch (IOException e) {
                        jLabel4.setText("Error:" + e.getMessage());
                        logger.error(e.getMessage());
                    }
                }
                XMLNode xmlNode = (XMLNode) BookImporter.xmlTree.getSelectionPath().getLastPathComponent();
                it.imtech.xmltree.XMLTree.insertAtTheEnd(xmlNode,newPages);
            } catch (IOException ex) {
                jLabel4.setText("Error:" + jLabel4.getText() + " File not found!");
                logger.error(ex.getMessage());
            } catch (Exception ex) {
                logger.error(ex.getMessage());
                jLabel4.setText("Error:" + jLabel4.getText() + " Lang Exception!");
            }
        }

        private void exportPdf() {
            ResourceBundle bundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);

            float quality = (float) 0.5;
            int progress = 0;
            int percent = 0;
            int total = Utility.countXmlTreeLeaves();
            String mimetype = null;
         
            jProgressBar1.setMaximum(100);

            com.itextpdf.text.Document document = new com.itextpdf.text.Document(PageSize.A4);

            try {
                PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(pdfLocation));

                document.open();

                ArrayList<XMLPage> images = XMLTree.getImagesFromStructure();
                com.itextpdf.text.Image img;
                XMLPage test = new XMLPage("test","test");
                
                for (int i = 0; i < images.size() && !isCancelled(); i++) {
                    jLabel4.setText(images.get(i).toString());

                    mimetype = Utility.getMimeType(Globals.SELECTED_FOLDER_SEP + images.get(i).getHref());
                    if (Utility.fileIsPicture(mimetype)) {
                        if(mimetype.equals("image/tiff"))
                            img = Image.getInstance(Globals.SELECTED_FOLDER_SEP + images.get(i).getHref());
                        else
                            img = Image.getInstance(Utility.convertImage(Globals.SELECTED_FOLDER_SEP + images.get(i).getHref(), quality));
                        
                        img.setAlignment(Element.ALIGN_CENTER);
                        img.scaleToFit(PageSize.A4.getWidth(), PageSize.A4.getHeight());

                        document.add(img);

                        progress++;
                        percent = progress * 100 / total;
                        jProgressBar1.setValue(percent);
                    }
                }
            } catch (FileNotFoundException ex) {
                jLabel4.setText("Error:" + jLabel4.getText() + " File not found!");
                Logger.getLogger(PdfCreateMonitor.class.getName()).log(Level.SEVERE, "File not found " + jLabel4.getText(), ex);
            } catch (BadElementException ex) {
                jLabel4.setText("Error:" + jLabel4.getText() + " Bad Element!");
                Logger.getLogger(PdfCreateMonitor.class.getName()).log(Level.SEVERE, "Bad Element " + jLabel4.getText(), ex);
            } catch (MalformedURLException ex) {
                jLabel4.setText("Error:" + jLabel4.getText() + " Malformed URL!");
                Logger.getLogger(PdfCreateMonitor.class.getName()).log(Level.SEVERE, "malformed url " + jLabel4.getText(), ex);
            } catch (IOException ex) {
                jLabel4.setText("Error:" + jLabel4.getText() + " IO Exception!");
                Logger.getLogger(PdfCreateMonitor.class.getName()).log(Level.SEVERE, "IO Exception " + jLabel4.getText(), ex);
            }  catch (com.itextpdf.text.DocumentException ex) {
                jLabel4.setText("Error: " + jLabel4.getText() + " Document Exception!");
                Logger.getLogger(PdfCreateMonitor.class.getName()).log(Level.SEVERE, "Document Exception " + jLabel4.getText(), ex);
            } catch (Exception ex) {
                jLabel4.setText("Error: " + jLabel4.getText() + " Document Exception!");
                Logger.getLogger(PdfCreateMonitor.class.getName()).log(Level.SEVERE, "Document Exception " + jLabel4.getText(), ex);
            }

            document.close();
        }

        /*
         * Executed in event dispatching thread
         */
        @Override
        public void done() {
            ResourceBundle bundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);
            jProgressBar1.setValue(100);
            jButton1.setEnabled(true);
            jButton1.setText(Utility.getBundleString("ok",bundle));
            setCursor(null);
            
        }
    }

    public static void main(String[] args) {
        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {}
        });
    }
    
    /**
     * Create the GUI and show it. As with all GUI code, this must run on the
     * event-dispatching thread.
     */
    public static void createAndShowGUI(boolean exp, String location) {
        //Create and set up the window.
        String title = (exp) ? "Export PDF" : "Import PDF";

        frame = new JFrame(title);
        frame.setDefaultCloseOperation(frame.DISPOSE_ON_CLOSE);
        
        //Create and set up the content pane.
        JComponent newContentPane = new PdfCreateMonitor(exp, location);
        
        if (!disposed) {
            newContentPane.setOpaque(true); //content panes must be opaque
            frame.setContentPane(newContentPane);

            frame.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    frame.dispose();
                }
            });
            frame.pack();
            
            //Display the window.
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
            jProgressBar1.setValue(progress);
        }
    }

    /**
     * Creates new form PdfCreateMonitor
     */
    private PdfCreateMonitor(boolean exp, String location) {
        ResourceBundle bundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);
        this.pdfLocation = location;

        this.export = exp;

        initComponents();
        updateLanguageLabel();

        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        task = new Task();
        task.addPropertyChangeListener(this);
        task.execute();

        jButton1.setEnabled(true);
        jButton1.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
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

    private void updateLanguageLabel() {
        ResourceBundle bundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);
        jLabel1.setText(Utility.getBundleString("exportpdf1",bundle));
        jLabel2.setText(Utility.getBundleString("exportpdf2",bundle));
        jLabel3.setText(Utility.getBundleString("exportpdf3",bundle));
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

        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jProgressBar1 = new javax.swing.JProgressBar();
        jLabel3 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        jLabel4 = new javax.swing.JLabel();

        jLabel1.setText("Sto elaborando i file");

        jLabel2.setText("Elaborazione");

        jLabel3.setText("Creazione del PDF iniziata. NB: potrebbe impiegare svariati minuti e assorbe molta potenza del processore del PC!");

        jButton1.setText("Close");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(23, 23, 23)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jProgressBar1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 618, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 686, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(28, 28, 28)
                                .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 102, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(22, 22, 22)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jProgressBar1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(jButton1))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JProgressBar jProgressBar1;
    // End of variables declaration//GEN-END:variables
}
