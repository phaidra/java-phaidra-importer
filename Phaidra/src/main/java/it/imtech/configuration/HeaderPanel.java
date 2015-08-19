/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package it.imtech.configuration;

import static gnu.cajo.invoke.Remote.config;
import it.imtech.globals.Globals;
import it.imtech.utility.Utility;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ResourceBundle;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import net.miginfocom.swing.MigLayout;
import org.apache.log4j.Logger;

/**
 *
 * @author mauro
 */
public class HeaderPanel extends javax.swing.JPanel {
    public  Logger logger = Logger.getLogger(ChooseServer.class);
        
    JLabel wizard_head_1 = new JLabel();
    JLabel wizard_head_2 = new JLabel();
    
    JSeparator separator = new JSeparator();
    
    /**
     * Creates new form headerPanel
     */
    public HeaderPanel() {
        initComponents();

        MigLayout layout = new MigLayout("fillx", "[right]rel[grow,fill]");
        this.setLayout(layout);
        
        BufferedImage head;
        try {
            head = ImageIO.read(getClass().getResource("images/phaidra.png"));
            JLabel picLabel = new JLabel(new ImageIcon(head));
            this.add(picLabel, "span 1 2");
        } catch (IOException ex) {
            logger.error(ex.getMessage());
        }
        
        ResourceBundle bundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);
        Font boldFont=new Font(wizard_head_1.getFont().getName(),Font.BOLD,wizard_head_1.getFont().getSize());  
        wizard_head_1.setText(Utility.getBundleString("header_1", bundle));
        wizard_head_1.setFont(boldFont);  
        wizard_head_1.setHorizontalAlignment(SwingConstants.RIGHT);
        
        wizard_head_2.setText(Utility.getBundleString("header_2", bundle) + " " + Globals.CURRENT_VERSION);
        wizard_head_2.setFont(boldFont);  
        wizard_head_2.setHorizontalAlignment(SwingConstants.RIGHT);
       
        this.add(wizard_head_1, "wrap");
        this.add(wizard_head_2, "align right, wrap");
        
        this.add(separator, "growx, span 2, wrap");
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
