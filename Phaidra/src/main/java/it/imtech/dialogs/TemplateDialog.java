/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | TemplatesUtility
 * and open the template in the editor.
 */

package it.imtech.dialogs;

import it.imtech.bookimporter.BookImporter;
import it.imtech.develop.MetadataParser;
import it.imtech.globals.Globals;
import it.imtech.metadata.TemplatesUtility;
import it.imtech.utility.Utility;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.Border;
import net.miginfocom.swing.MigLayout;
import org.apache.log4j.Logger;

/**
 *
 * @author mauro
 */
public class TemplateDialog extends javax.swing.JDialog {
    private static final Logger logger = Logger.getLogger(MetadataParser.class);
    
    private boolean next;
    private JTextField input;
    private JPanel templatelist;
    private javax.swing.JScrollPane template_scroller;
    
    public boolean getChoice(){
        return next;
    }
    
    
    private void createTemplateListPanel(TreeMap<String, String> templates, final String panelname){
        ResourceBundle bundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);
        templatelist.removeAll();
        templatelist.revalidate();
        
        Border blackline = BorderFactory.createLineBorder(Color.black);
            
        JLabel templateheadername = new JLabel("  "+Utility.getBundleString("templateheadername", bundle)+"  ");
        JLabel templateheaderaction = new JLabel("  "+Utility.getBundleString("templateheaderaction", bundle)+"  ");
        Font font = templateheadername.getFont();
        Font boldFont = new Font(font.getFontName(), Font.BOLD, font.getSize());
            
        templateheadername.setFont(boldFont);
        templateheaderaction.setFont(boldFont);

        //templatelist.setBorder(blackline);
        JPanel header = new JPanel(new MigLayout("fillx,insets 5 5 5 5"));        

        header.add(templateheadername,"growx, width :300:");
        header.add(templateheaderaction, "wrap, width :50:");
        templatelist.add(header, "wrap, growx");
        templatelist.add(new JSeparator(), "wrap, growx");
            
        for(Map.Entry<String,String> entry : templates.entrySet()) {
            final String key = entry.getKey();
            final String value = entry.getValue();

            JLabel label = new JLabel();
            label.setName(key);
            label.setText(key.substring(0, Math.min(key.length(), 50)));

            JButton delete = new JButton(Utility.getBundleString("templatedelete", bundle));
            delete.addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent event)
                {
                    ResourceBundle innerbundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);

                    String mstext = Utility.getBundleString("templatedeletetext", innerbundle);
                    String mstitle = Utility.getBundleString("templatedeletetitle", innerbundle);
                    String buttonok = Utility.getBundleString("voc1", innerbundle);
                    String buttonko = Utility.getBundleString("voc2", innerbundle);
                    ConfirmDialog confirm = new ConfirmDialog(BookImporter.getInstance(), true, mstitle, mstext, buttonok, buttonko);

                    boolean response = false;

                    confirm.setVisible(true);
                    response = confirm.getChoice();
                    confirm.dispose();

                    if (response==true) {
                        BookImporter.getInstance().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                        if(TemplatesUtility.deleteTemplateXML(key, value)){
                            TreeMap<String, String> updatedtemplates = TemplatesUtility.getTemplatesList();
                            createTemplateListPanel(updatedtemplates, panelname);
                            //BookImporter.getInstance().drawTemplatePanel(panelname);
                            BookImporter.getInstance().redrawTemplatePanels();
                            JOptionPane.showMessageDialog(null, Utility.getBundleString("templatedeleteok", innerbundle));
                        }
                        else{
                            JOptionPane.showMessageDialog(null, Utility.getBundleString("templatedeleteko", innerbundle));
                        }
                        BookImporter.getInstance().setCursor(null);
                    }
                }
            });

            JPanel innerheader = new JPanel(new MigLayout("fillx,insets 5 5 5 5"));        

            innerheader.add(label,"growx, width :300:");
            innerheader.add(delete, "wrap, width :50:");
            templatelist.add(innerheader, "wrap, growx");
            templatelist.add(new JSeparator(), "wrap, growx");
            
            
        }
        templatelist.revalidate();
        templatelist.repaint();
    }
    
    /**
     * Creates new form Input
     * @param parent
     * @param modal
     * @param text
     * @param title
     * @param type
     */
    public TemplateDialog(java.awt.Frame parent, boolean modal,  String title, String text, String buttonok, String panelname) {
        super(parent, modal);
        setModal(true);
        ResourceBundle bundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);
        
        initComponents();
        this.setTitle(title);
        
        MigLayout main = new MigLayout();
        MigLayout nord = new MigLayout();
        MigLayout sud = new MigLayout("fillx,insets 5 5 5 5");
        
        getContentPane().setLayout(main);
        
        Icon ico = UIManager.getIcon("OptionPane.warningIcon");
       
        JLabel picture = new JLabel();
        JLabel sentence = new JLabel("<html>"+text+"</html>");
        picture.setIcon(ico);
        
        JPanel right = new JPanel(sud);
        right.add(sentence, "wrap");
        
        JPanel north = new JPanel(nord);
        
        TreeMap<String, String> templates = TemplatesUtility.getTemplatesList();
        
        if (!templates.isEmpty()){
            template_scroller = new javax.swing.JScrollPane();
            templatelist = new JPanel(new MigLayout("fillx,insets 5 5 5 5"));
            
            template_scroller.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            template_scroller.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

            template_scroller.setPreferredSize(new java.awt.Dimension(500, 350));
            template_scroller.setViewportView(templatelist);
            template_scroller.setBorder(null);
            template_scroller.setBounds(5, 5, 500, 350);
            
            createTemplateListPanel(templates, panelname);
            
            JPanel upper = new JPanel(new MigLayout("fillx,insets 5 5 5 5"));
            upper.add(picture);
            upper.add(right, "wrap");
            
            north.add(upper, "growx, wrap");
            north.add(new JSeparator(), "growx, wrap");
            north.add(template_scroller, "wrap, growx");
        }
        else{
            north.add(picture);
            north.add(right, "wrap");
        }
        
        this.add(north, "wrap, growx");
        //this.add(south, "wrap, growx");
        this.pack();
        
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (dim.width - getSize().width) / 2;
        int y = (dim.height - getSize().height) / 2;
        setLocation(x, y);
    }
    
    public String getInputText(){
        return input.getText();
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">                          
    private void initComponents() {

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 142, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 79, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>                        

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            logger.error(ex.getMessage());
        } catch (InstantiationException ex) {
            logger.error(ex.getMessage());
        } catch (IllegalAccessException ex) {
            logger.error(ex.getMessage());
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            logger.error(ex.getMessage());
        }
        //</editor-fold>

        /* Create and display the dialog */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                TemplateDialog dialog = new TemplateDialog(new javax.swing.JFrame(), true, "", "", "", "");
                dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                    @Override
                    public void windowClosing(java.awt.event.WindowEvent e) {
                        System.exit(0);
                    }
                });
                dialog.setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify                     
    // End of variables declaration                   
}
