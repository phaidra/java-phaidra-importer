package imtech.updater;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


/**
 *
 * @author Thomas Otero (H3rootR3T1C)
 */
public class Updater extends JFrame{

    private Thread worker;
    private final String root = "update"+getSep();
    private String namezip = "imphaidrazip.zip";
    private String jarpath = getCurrentJarDirectory();
    
    private JTextArea outText;
    private JButton cancle;
    private JButton launch;
    private JScrollPane sp;
    private JPanel pan1;
    private JPanel pan2;
    private String USER_DIR = System.getProperty("user.home")+getSep()+".imphaidraimportertwo";
     
    public Updater() {   
        this.setAlwaysOnTop(true);
        initComponents();
        
        //Posizionamento interfaccia
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (dim.width - getSize().width) / 2;
        int y = (dim.height - getSize().height) / 2;
        setLocation(x, y);
        setVisible(true);
            
        outText.setText("Contacting Download Server...");
        outText.setText(outText.getText()+"\n"+USER_DIR);
        download();
    }
     
    private void initComponents() {

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        
        pan1 = new JPanel();
        pan1.setLayout(new BorderLayout());

        pan2 = new JPanel();
        pan2.setLayout(new FlowLayout());

        outText = new JTextArea();
        sp = new JScrollPane();
        sp.setViewportView(outText);
        
        launch = new JButton("Launch App");
        launch.setEnabled(false);
        launch.addActionListener(new ActionListener(){

            public void actionPerformed(ActionEvent e) {
                launch();
            }
        });
        pan2.add(launch);

        cancle = new JButton("Cancel Update");
        cancle.addActionListener(new ActionListener(){

            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
        pan2.add(cancle);
        pan1.add(sp,BorderLayout.CENTER);
        pan1.add(pan2,BorderLayout.SOUTH);

        add(pan1);
        pack();
        this.setSize(600, 500);
    }

    private void updateVersion() throws ConfigurationException, ParserConfigurationException, MalformedURLException, SAXException, IOException{
        XMLConfiguration config = new XMLConfiguration(USER_DIR + getSep() + "config" + getSep() + "config.xml");
        config.setAutoSave(true);

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        
        URL url2 = new URL(config.getString("configurl[@path]"));
        XMLConfiguration config2 = new XMLConfiguration(url2);
        
        URL url = new URL(config2.getString("urlupdater.descrurl"));
        Document doc = dBuilder.parse(url.openStream());
         
        NodeList nl = doc.getElementsByTagName("version");
        
        Element el = (Element) nl.item(0);
        
        String version = el.getTextContent();
        
        config.setProperty("version[@current]", version);
    }    
    
    private void download()
    {
        worker = new Thread(
        new Runnable(){
            public void run()
            {
                try {
                    outText.setText(outText.getText()+"\nDownloading:"+getDownloadLinkFromHost());
                    downloadFile(getDownloadLinkFromHost());
                    outText.setText(outText.getText()+"\nunzipping");
                    unzip();
                    outText.setText(outText.getText()+"\ncopy files");
                    copyFiles(new File(root),jarpath);
                    cleanup();
                    updateVersion();
                    
                    launch.setEnabled(true);
                    outText.setText(outText.getText()+"\nUpdate Finished!");
                } 
                catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(null, "An error occured while preforming update!");
                }
            }
        });
        worker.start();
    }
    
    private void launch()
    {
        String path = getCurrentJarDirectory()+"PhaidraImporter.jar";
        String[] run = {"java", "-jar", path};
        try {
            Runtime.getRuntime().exec(run);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        System.exit(0);
    }
    
    private void cleanup()
    {
        outText.setText(outText.getText()+"\nPerforming clean up...");
        File f = new File(USER_DIR+getSep()+namezip);
        f.delete();
        remove(new File(root));
        new File(root).delete();
    }
    
    private void remove(File f)
    {
        File[]files = f.listFiles();
        for(File ff:files)
        {
            if(ff.isDirectory())
            {
                remove(ff);
                ff.delete();
            }
            else
            {
                ff.delete();
            }
        }
    }
    
    private void copyFiles(File f,String dir) throws IOException
    {
        String tmp_dir = dir;
        File[]files = f.listFiles();
        for(File ff:files)
        {
            if(ff.isDirectory()){
                if(ff.getName().equals("config")){
                    dir = USER_DIR;
                }   
                else
                    dir = getSep() + tmp_dir;
                try   {  
                    new File(dir+getSep()+ff.getName()).mkdir();
                }
                
                catch(Exception ex){ 
                    outText.setText(outText.getText()+"\nCreating folder "+ff.getName()+" permission denied!");
                }
                
                //Config xml non si aggiorna
                copyFiles(ff, dir+getSep()+ff.getName());
            }
            else
            {
                dir = tmp_dir;
                try  {  
                    if(!(dir.equals(USER_DIR+getSep()+"config") && ff.getName().equals("config.xml"))){  
                        outText.setText(outText.getText()+"\nCopied: "+ff.getName()+" to "+dir);
                        copy(ff.getAbsolutePath(), dir + getSep()+ff.getName()); 
                    }
                }
                catch(Exception ex){ 
                    outText.setText(outText.getText()+"\nCreating file "+dir+getSep()+ff.getName()+" permission denied!");
                }
            }

        }
    }
    public void copy(String srFile, String dtFile) throws FileNotFoundException, IOException{

          File f1 = new File(srFile);
          File f2 = new File(dtFile);

          InputStream in = new FileInputStream(f1);

          OutputStream out = new FileOutputStream(f2);

          byte[] buf = new byte[1024];
          int len;
          while ((len = in.read(buf)) > 0){
            out.write(buf, 0, len);
          }
          in.close();
          out.close();
      }
    private void unzip() throws IOException
    {
         int BUFFER = 2048;
         BufferedOutputStream dest = null;
         BufferedInputStream is = null;
         ZipEntry entry;
         ZipFile zipfile = new ZipFile(USER_DIR+getSep()+namezip);
         Enumeration e = zipfile.entries();
         (new File(root)).mkdir();
         while(e.hasMoreElements()) {
            entry = (ZipEntry) e.nextElement();
            outText.setText(outText.getText()+"\nExtracting: " +entry);
            if(entry.isDirectory())
                (new File(root+entry.getName())).mkdir();
            else{
                (new File(root+entry.getName())).createNewFile();
                is = new BufferedInputStream
                  (zipfile.getInputStream(entry));
                int count;
                byte data[] = new byte[BUFFER];
                FileOutputStream fos = new
                  FileOutputStream(root+entry.getName());
                dest = new
                  BufferedOutputStream(fos, BUFFER);
                while ((count = is.read(data, 0, BUFFER))
                  != -1) {
                   dest.write(data, 0, count);
                }
                dest.flush();
                dest.close();
                is.close();
            }
         }

    }
    private void downloadFile(String link) throws MalformedURLException, IOException
    {
        URL url = new URL(link);
        URLConnection conn = url.openConnection();
        InputStream is = conn.getInputStream();
        long max = conn.getContentLength();
        outText.setText(outText.getText()+"\n"+"Downloding file...\nUpdate Size(compressed): "+max+" Bytes");
        BufferedOutputStream fOut = new BufferedOutputStream(new FileOutputStream(new File(USER_DIR+getSep()+namezip)));
   
        byte[] buffer = new byte[32 * 1024];
        int bytesRead = 0;
        int in = 0;
        while ((bytesRead = is.read(buffer)) != -1) {
            in += bytesRead;
            fOut.write(buffer, 0, bytesRead);
        }
        fOut.flush();
        fOut.close();
        is.close();
        
        outText.setText("\n"+"File downloaded in: "+USER_DIR+getSep()+namezip);
        outText.setText(outText.getText()+"\nDownload Complete!");

    }
    
    public static String getSep() {
        String osname = System.getProperty("os.name").toLowerCase();
        String sep = "/";
        if (osname.startsWith("win")) {
            sep = "\\";
        }

        return sep;

    }
    
    private String getDownloadLinkFromHost() throws MalformedURLException, IOException, ParserConfigurationException, SAXException, ConfigurationException{   
        XMLConfiguration config = new XMLConfiguration(USER_DIR+getSep()+"config" + getSep() + "config.xml");

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        
        URL url2 = new URL(config.getString("configurl[@path]"));
        XMLConfiguration config2 = new XMLConfiguration(url2);
        
        //URL url = new URL(config2.getString("urlupdater.descrurl"));
        URL url = new URL("http://www.im-tech.it/releases/phaidra/updatertwo.xml");
        Document doc = dBuilder.parse(url.openStream());
         
        NodeList nl = doc.getElementsByTagName("release");
        
        Element el = (Element) nl.item(0);
        
        return el.getTextContent();
    }
    
    /**
     * Setta la directory dell'eseguibile dell'applicazione
     * @return Path corrente
     */
    public static String getCurrentJarDirectory() {
        URL url = Updater.class.getProtectionDomain().getCodeSource().getLocation();
        String jrPath = null;
        
        try {
            final File jarPath = new File(url.toURI()).getParentFile();
            jrPath = jarPath.getAbsolutePath() + getSep();
        } catch (final URISyntaxException ex) {
            JOptionPane.showMessageDialog(new Frame(), ex.getMessage());
        }
        
        return jrPath;
    }
    
    

    
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Updater().setVisible(true);
            }
        });
    }


}

