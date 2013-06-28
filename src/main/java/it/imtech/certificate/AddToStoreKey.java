/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package it.imtech.certificate;

import it.imtech.bookimporter.BookImporter;
import it.imtech.utility.Utility;
import it.imtech.globals.Globals;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.io.*;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.*;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingWorker;

/**
 *
 * @author mauro
 */
public class AddToStoreKey extends javax.swing.JPanel implements java.beans.PropertyChangeListener {

    String[] args = null;
    private static final char[] HEXDIGITS = "0123456789abcdef".toCharArray();
    private static JFrame frame = null;
    private AddKeyTask task = null;

    class AddKeyTask extends SwingWorker<Void, Void> {

        @Override
        public Void doInBackground() {
            try {
                ResourceBundle bundle = ResourceBundle.getBundle(BookImporter.RESOURCES, BookImporter.currentLocale, Globals.loader);

                String host;
                int port;
                char[] passphrase;
                if ((args.length == 1) || (args.length == 2)) {
                    String[] c = args[0].split(":");
                    host = c[0];
                    port = (c.length == 1) ? 443 : Integer.parseInt(c[1]);
                    String p = (args.length == 1) ? "changeit" : args[1];
                    passphrase = p.toCharArray();
                } else {
                    addUploadInfoText(Utility.getBundleString("skusage",bundle));
                    return null;
                }

                
               File file = new File("jssecacerts");
               if (file.isFile() == false) {
                    char SEP = File.separatorChar;
                    File dir = new File(System.getProperty("java.home") + SEP
                        + "lib" + SEP + "security");
                    file = new File(dir, "jssecacerts");
                    if (file.isFile() == false) {
                        file = new File(dir, "cacerts");
                    }
                }


                addUploadInfoText(Utility.getBundleString("skloading",bundle) + file + "...");
                InputStream in = new FileInputStream(file);
                KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
                ks.load(in, passphrase);
                in.close();

                SSLContext context = SSLContext.getInstance("TLS");
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init(ks);
                X509TrustManager defaultTrustManager = (X509TrustManager) tmf.getTrustManagers()[0];
                SavingTrustManager tm = new SavingTrustManager(defaultTrustManager);
                context.init(null, new TrustManager[]{tm}, null);
                SSLSocketFactory factory = context.getSocketFactory();

                addUploadInfoText(Utility.getBundleString("skopen",bundle) + host + ":" + port + "...");
                SSLSocket socket = (SSLSocket) factory.createSocket(host, port);
                socket.setSoTimeout(10000);
                try {
                    addUploadInfoText(Utility.getBundleString("skssl",bundle));
                    socket.startHandshake();
                    socket.close();
                    addUploadInfoText("");
                    addUploadInfoText(Utility.getBundleString("skerror",bundle));
                } catch (SSLException e) {
                    addUploadInfoText("");
                    e.printStackTrace(System.out);
                }

                X509Certificate[] chain = tm.chain;
                if (chain == null) {
                    addUploadInfoText(Utility.getBundleString("skchain",bundle));
                    return null;
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

                addUploadInfoText("");
                addUploadInfoText(Utility.getBundleString("sksent",bundle) + chain.length + " " + Utility.getBundleString("skcerts",bundle));
                addUploadInfoText("");
                MessageDigest sha1 = MessageDigest.getInstance("SHA1");
                MessageDigest md5 = MessageDigest.getInstance("MD5");

                String[] possibilities = new String[chain.length];
                int p = 1;
                for (int i = 0; i < chain.length; i++) {
                    X509Certificate cert = chain[i];
                    String alias = host + "-" + (i + 1);
                    ks.setCertificateEntry(alias, cert);
                    
                    //Aggiungi tutta la catena certificati disponibili
                    /*   addUploadInfoText(" " + (i + 1) + " " + Utility.getBundleString("sksubject",bundle) + " " + cert.getSubjectDN());
                    addUploadInfoText(Utility.getBundleString("skissuer",bundle) + " " + cert.getIssuerDN());
                    sha1.update(cert.getEncoded());
                    addUploadInfoText(Utility.getBundleString("sksha",bundle) +" " + toHexString(sha1.digest()));
                    md5.update(cert.getEncoded());
                    addUploadInfoText(Utility.getBundleString("skmd5",bundle) + " " + toHexString(md5.digest()));
                    addUploadInfoText("");

                    possibilities[i] = Integer.toString(p);
                    p++;*/
                }

         /*       String s = null;
                if (p > 2) {
                    //addUploadInfoText("Enter certificate to add to trusted keystore or 'q' to quit: [1]");
                    Object[] options = {Utility.getBundleString("voc1",bundle), Utility.getBundleString("voc2",bundle)};
                    s = (String) JOptionPane.showInputDialog(frame,
                            Utility.getBundleString("PhaidraLocationLabel",bundle), Utility.getBundleString("PhaidraLocationLabel",bundle),
                            JOptionPane.PLAIN_MESSAGE, null, possibilities, null);
                } else {
                    s = "1";
                }

                //Aggiungi i parametri a PhaidraUtils
                if ((s != null)) {
                    String line = s;
                    int k;
                    try {
                        k = (line.length() == 0) ? 0 : Integer.parseInt(line) - 1;
                    } catch (NumberFormatException e) {
                        addUploadInfoText(Utility.getBundleString("sknotchanged",bundle));
                        return null;
                    }

                    X509Certificate cert = chain[k];
                    String alias = host + "-" + (k + 1);
                    ks.setCertificateEntry(alias, cert);
*/
                    String outputFile = Globals.USER_DIR+"certs"+Utility.getSep()+"jssecacerts.jks";

                    boolean javaPath = true;
                    OutputStream out = null;

                    try{
                        addUploadInfoText(Utility.getBundleString("skaddcert",bundle) + outputFile);
                        
                        out = new FileOutputStream(outputFile);
                        ks.store(out, passphrase);
                        out.close();
                    } 
                    catch (Exception e) {
                        addUploadInfoText(Utility.getBundleString("skpermdenied",bundle));
                        return null;
                    }
                    
                    addUploadInfoText("");
                    char SEP = File.separatorChar;

                    addUploadInfoText(Utility.getBundleString("skadded",bundle) + " " + outputFile);
                    addUploadInfoText(Utility.getBundleString("skalias",bundle) + " 'jssecacerts'");
                    addUploadInfoText(Utility.getBundleString("skrestart",bundle));
  //              } else {
  //                  return null;
   //             }
            } catch (FileNotFoundException e) {
                addUploadInfoText("Exception: " + e.getMessage());
            } catch (KeyStoreException e) {
                addUploadInfoText("Exception: " + e.getMessage());
            } catch (IOException e) {
                addUploadInfoText("Exception: " + e.getMessage());
            } catch (NoSuchAlgorithmException e) {
                addUploadInfoText("Exception: " + e.getMessage());
            } catch (CertificateException e) {
                addUploadInfoText("Exception: " + e.getMessage());
            } catch (KeyManagementException e) {
                addUploadInfoText("Exception: " + e.getMessage());
            }

            return null;
        }

        private String toHexString(byte[] bytes) {
            StringBuilder sb = new StringBuilder(bytes.length * 3);
            for (int b : bytes) {
                b &= 0xff;
                sb.append(HEXDIGITS[b >> 4]);
                sb.append(HEXDIGITS[b & 15]);
                sb.append(' ');
            }
            return sb.toString();
        }

        private class SavingTrustManager implements X509TrustManager {

            private final X509TrustManager tm;
            private X509Certificate[] chain;

            SavingTrustManager(X509TrustManager tm) {
                this.tm = tm;
            }

            public X509Certificate[] getAcceptedIssuers() {
                throw new UnsupportedOperationException();
            }

            public void checkClientTrusted(X509Certificate[] chain, String authType)
                    throws CertificateException {
                throw new UnsupportedOperationException();
            }

            public void checkServerTrusted(X509Certificate[] chain, String authType)
                    throws CertificateException {
                this.chain = chain;
                tm.checkServerTrusted(chain, authType);
            }
        }

        /*
         * Executed in event dispatching thread
         */
        @Override
        public void done() {
            ResourceBundle bundle = ResourceBundle.getBundle(BookImporter.RESOURCES, BookImporter.currentLocale, Globals.loader);
            setCursor(null);
            try {
                if(System.getProperty("os.name").toLowerCase().startsWith("win"))
                    Runtime.getRuntime().exec("java -jar "+'"'+BookImporter.jrPath+"PhaidraImporter.jar"+'"');
                else
                    Runtime.getRuntime().exec("java -jar "+BookImporter.jrPath+"PhaidraImporter.jar");
            }    
            catch (IOException ex) {
                Logger.getLogger(AddToStoreKey.class.getName()).log(Level.SEVERE, null, ex);
            }
            System.exit(0);
        }
    }

    /**
     * Create the GUI and show it. As with all GUI code, this must run on the
     * event-dispatching thread.
     */
    public static void createAndShowGUI(String[] argos) {
        //Create and set up the window.
        String title = "Update Trust Store";

        frame = new JFrame(title);
        frame.setDefaultCloseOperation(frame.DISPOSE_ON_CLOSE);

        //Create and set up the content pane.
        JComponent newContentPane = new AddToStoreKey(argos);

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
        
        //E' stato richiesto di non renderlo visibile
        //frame.setVisible(true);
    }
    
    /**
     * Invoked when task progress property changes.
     */
    public void propertyChange(PropertyChangeEvent evt) {
        if ("progress".equals(evt.getPropertyName())) {
            int progress = (Integer) evt.getNewValue();
            //   jProgressBar1.setValue(progress);
        }
    }
    
    protected void addUploadInfoText(String add) {
        String text = jTextPane1.getText();
        text += "\n" + add;
        jTextPane1.setText(text);
        jTextPane1.revalidate();
    }
    
    /**
     * Creates new form AddToStoreKey
     */
    private AddToStoreKey(String[] argos) {
        this.args = argos;

        initComponents();

        jButton1.setEnabled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        task = new AddKeyTask();

        task.addPropertyChangeListener(this);
        task.execute();

        initComponents();

        
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (dim.width - getSize().width) / 2;
        int y = (dim.height - getSize().height) / 2;
        setLocation(x, y);
    }

    public static void main(String[] args) {
        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                //createAndShowGUI();
            }
        });
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        jTextPane1 = new javax.swing.JTextPane();
        jButton1 = new javax.swing.JButton();

        jScrollPane1.setViewportView(jTextPane1);

        jButton1.setText("Restart Application");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 568, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 151, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 323, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 13, Short.MAX_VALUE)
                .addComponent(jButton1)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        try {
            if(System.getProperty("os.name").toLowerCase().startsWith("win"))
                Runtime.getRuntime().exec("java -jar "+'"'+BookImporter.jrPath+"BookImporter.jar"+'"');
            else
                Runtime.getRuntime().exec("java -jar "+BookImporter.jrPath+"BookImporter.jar");
        } 
        catch (IOException ex) {
            Logger.getLogger(AddToStoreKey.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.exit(0);
    }//GEN-LAST:event_jButton1ActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextPane jTextPane1;
    // End of variables declaration//GEN-END:variables
}
