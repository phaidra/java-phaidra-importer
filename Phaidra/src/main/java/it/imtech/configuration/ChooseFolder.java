/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package it.imtech.configuration;

import it.imtech.dialogs.AlertDialog;
import it.imtech.dialogs.BookCollectionDialog;
import it.imtech.globals.Globals;
import it.imtech.utility.Utility;
import it.imtech.xmltree.XMLTree;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ResourceBundle;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.xml.parsers.ParserConfigurationException;
import net.miginfocom.swing.MigLayout;
import org.apache.log4j.Logger;
import org.xml.sax.SAXException;


/**
 * Represent the card used to select the working folder
 * @author I.M. Technologies
 */
public class ChooseFolder extends javax.swing.JPanel {
    
    private final Logger logger = Logger.getLogger(ChooseFolder.class);
    
    private final JLabel label_folder_1 = new JLabel();
    private final JLabel label_folder_2 = new JLabel();
    private final JLabel label_folder_3 = new JLabel();
    
    private final JTextField folder_path = new JTextField();
    private final JButton choose_folder = new JButton();
    private final JPanel main_panel;
    
    private ResourceBundle bundle;
    
    
    /**
     * Creates new form chooseFolder
     */
    public ChooseFolder() {
        initComponents();
        
        bundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);
        
        MigLayout choose_layout = new MigLayout("fillx, insets 10 20 10 50");  
        
        updateLanguage();
    
        main_panel = new JPanel(choose_layout);
        main_panel.add(label_folder_1, "wrap 20, span 2");
        main_panel.add(label_folder_2, "wrap 30, span 2");
        main_panel.add(label_folder_3, "wrap 20, span 2");
        
        folder_path.setMinimumSize(new Dimension(450, 20));
        folder_path.setName("folder_path");
        choose_folder.setMinimumSize(new Dimension(120,20));

        main_panel.add(folder_path);
        main_panel.add(choose_folder, "wrap 5");
        
        this.setLayout(new BorderLayout());
        this.add(BorderLayout.CENTER, main_panel);
        
        this.setPreferredSize(this.getPreferredSize());
        this.validate();
        this.repaint();
        
        choose_folder.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                fileChooser.setBackground(Color.WHITE);
                fileChooser.setOpaque(true);
                fileChooser.setAcceptAllFileFilterUsed(false);
                fileChooser.setDialogTitle(Utility.getBundleString("selectfolder", bundle));
                fileChooser.setApproveButtonText(Utility.getBundleString("select", bundle));
                fileChooser.setApproveButtonMnemonic('e');
                fileChooser.setApproveButtonToolTipText(Utility.getBundleString("tooltipselect", bundle));

                fileChooser.setLocale(Globals.CURRENT_LOCALE);
                fileChooser.updateUI();

                if (fileChooser.showOpenDialog(ChooseFolder.this) == JFileChooser.APPROVE_OPTION) {
                    folder_path.setText(fileChooser.getSelectedFile().toString());
                    logger.info("Selected Working Folder = "+fileChooser.getSelectedFile().toString());
                } else {
                    folder_path.setText("");
                }
            }
        });
    }
    
    /**
     * Verifies if the folder contains non compatible files
     * @return boolean (Check result)
     */
    public boolean checkFolderSelectionValidity() {
        String message = "";
        boolean error = true;
        
        try{
            String path = folder_path.getText();
            if (new File(path).isDirectory()){
                Globals.SELECTED_FOLDER = path;
                File pathfile = new File(path);
                Globals.SELECTED_FOLDER_SEP = Globals.SELECTED_FOLDER + Utility.getSep();

                checkWritableFolder();

                Globals.TYPE_BOOK = XMLTree.getTypeFileBookstructure(Globals.SELECTED_FOLDER_SEP);
                
                if (Globals.TYPE_BOOK == Globals.NOT_EXISTS) {
                    chooseBookOrCollection();
                    
                    if (Globals.TYPE_BOOK == Globals.NOT_EXISTS) {
                        message = Utility.getBundleString("select_book_type_error", bundle);
                    }
                    else{
                        error = false;
                    }
                }
                else{
                    error = false;
                }

                String file = Utility.checkDirectory(pathfile, Utility.getAvailableExtensions());
                if(!file.isEmpty()) {
                    ResourceBundle bundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);
                    AlertDialog alert = new AlertDialog(StartWizard.getInstance().mainFrame, true, 
                            Utility.getBundleString("dialog_2_title", bundle), 
                            Utility.getBundleString("dialog_2", bundle)+" "+file, "Ok");
                }
            }
            else{
                message = Utility.getBundleString("dirpath_error", bundle);
            }
        }
        catch (ParserConfigurationException ex) {
            message = Utility.getBundleString("exc_select_folder_1", bundle);
            logger.error(ChooseFolder.class.getName()+ " : "+ex);
        } catch (SAXException ex) {
            message = Utility.getBundleString("exc_select_folder_2", bundle);
            logger.error(ChooseFolder.class.getName()+ " : "+ex);
        } catch (IOException ex) {
            message = Utility.getBundleString("exc_select_folder_3", bundle);
            logger.error(ChooseFolder.class.getName()+ " : "+ex);
        }
        
        if (error == true){
            Object[] options = {Utility.getBundleString("ok", bundle)};
            int answer = JOptionPane.showOptionDialog(null, message,
                Utility.getBundleString("select_folder_error_title", bundle),
                JOptionPane.OK_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null, options, options[0]);  
        }
        
        return error;
    }
    
    /**
     * Creates a dialog to select the structure type of the object to 
     * create / upload.It is called only if there isn't a phaidrastructure.xml 
     * file in worrking folder.
     */
    private  void chooseBookOrCollection() {
        ResourceBundle bundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);
        int result = 0;
        
        String text = Utility.getBundleString("collorbook", bundle);
        String title = Utility.getBundleString("titlecollobook", bundle);
        String buttoncoll = Utility.getBundleString("collection", bundle);
        String buttonbook = Utility.getBundleString("book", bundle);
        String buttonko = Utility.getBundleString("back", bundle);
        BookCollectionDialog confirm = new BookCollectionDialog(StartWizard.getInstance().mainFrame, true, title, text, buttoncoll, buttonbook, buttonko);

        confirm.setVisible(true);
        result = confirm.getChoice();
        confirm.dispose();
 
        switch (result) {
            case 0:
                logger.info("Object Type selected: Collection");
                Globals.TYPE_BOOK = Globals.COLLECTION;
                break;
            case 1:
                logger.info("Object Type selected: Book");
                Globals.TYPE_BOOK = Globals.BOOK;
                break;
            case 2:
                logger.info("Object Type selected: Nothing");
                Globals.TYPE_BOOK = Globals.NOT_EXISTS;
                break;
            default:
                logger.info("Object Type selected: Nothing");
                Globals.TYPE_BOOK = Globals.NOT_EXISTS;
                break;
        }
    }
       
    /**
     * Checks if the selected folder is writable and 
     * initialise the globals variable FOLDER_WRITABLE
     */
    private void checkWritableFolder(){
        try{
            File testFile = Utility.getUniqueFileName(Globals.SELECTED_FOLDER_SEP+"testfile", "txt");
            Writer output = null;
            output = new BufferedWriter(new FileWriter(testFile));
            output.close();
            testFile.delete();
        }
        catch (Exception ex) {
            Globals.FOLDER_WRITABLE = false;
        }
    }
    
    /**
     * Updates all the labels contained in the form
     */
    public void updateLanguage(){
        bundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);
        
        label_folder_1.setText("<html>"+Utility.getBundleString("folder_1", bundle)+"</html>");
        label_folder_2.setText("<html>"+Utility.getBundleString("folder_2", bundle)+"</html>");
        label_folder_3.setText("<html>"+Utility.getBundleString("folder_3", bundle)+"</html>");
        choose_folder.setText(Utility.getBundleString("bselect", bundle));
        
        JButton nextButton = (JButton) StartWizard.footerPanel.getComponentByName("next_button");
        JButton prevButton = (JButton) StartWizard.footerPanel.getComponentByName("prev_button");
        
        nextButton.setText(Utility.getBundleString("next", bundle));
        prevButton.setText(Utility.getBundleString("back", bundle));
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jFileChooser1 = new javax.swing.JFileChooser();

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 228, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 94, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JFileChooser jFileChooser1;
    // End of variables declaration//GEN-END:variables
}
