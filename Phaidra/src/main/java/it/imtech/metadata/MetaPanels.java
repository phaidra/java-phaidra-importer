/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package it.imtech.metadata;

import javax.swing.JLayeredPane;
import javax.swing.JPanel;

/**
 *
 * @author mauro
 */
public class MetaPanels {
    JPanel panel;
    JLayeredPane pane;
    
    public MetaPanels(JPanel p, JLayeredPane l){
        this.panel = p;
        this.pane = l;
    }
    
    public JPanel getPanel(){
        return panel;
    }

    public JLayeredPane getPane(){
        return pane;
    }
}
