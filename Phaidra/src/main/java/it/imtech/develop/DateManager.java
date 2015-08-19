/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package it.imtech.develop;

import it.imtech.utility.Utility;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import net.miginfocom.swing.MigLayout;
import net.sourceforge.jdatepicker.JDatePicker;
import net.sourceforge.jdatepicker.impl.JDatePanelImpl;
import net.sourceforge.jdatepicker.impl.JDatePickerImpl;
import net.sourceforge.jdatepicker.impl.UtilDateModel;
import org.jdesktop.swingx.JXDatePicker;

/**
 *
 * @author mauro
 */
public class DateManager {
    
    JXDatePicker jxdatePicker = new JXDatePicker();
    
    public DateManager(){
        JFrame x = new JFrame("test");
        JPanel panel = new JPanel(new MigLayout());
        
        UtilDateModel model = new UtilDateModel();
        JDatePanelImpl datePanel = new JDatePanelImpl(model);
        final JDatePickerImpl jdatePicker = new JDatePickerImpl(datePanel);
        
        JButton test;
        test = new JButton("Get Date");
        test.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent event)
            {
                Format formatter = new SimpleDateFormat("yyyy-MM-dd");
                
                Date jdate = (Date) jdatePicker.getModel().getValue(); 
                String stDate = formatter.format(jdate);
                System.out.println(stDate);
            }
        });  
        
        JButton testjx;
        testjx = new JButton("Get Date");
        testjx.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent event)
            {
                Format formatter = new SimpleDateFormat("yyyy-MM-dd");
                
                Date jxdate = jxdatePicker.getDate();
                String stDate = formatter.format(jxdate);
                System.out.println(stDate);
            }
        });
                
        panel.add(jdatePicker);
        panel.add(test,"wrap");
        panel.add(jxdatePicker);
        panel.add(testjx,"wrap");
        
        x.add(panel);
        x.setSize(new Dimension(400,400));
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        x.setVisible(true);
    }
    
    public static void main(String[] args){
        SwingUtilities.invokeLater(new Runnable(){
            @Override
            public void run(){
                   new DateManager();
            }
        });
    };
}
