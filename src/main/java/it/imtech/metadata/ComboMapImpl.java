/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package it.imtech.metadata;

import java.awt.Component;
import java.util.*;
import javax.swing.*;

/**
 *
 * @author mauro
 */

public class ComboMapImpl extends AbstractListModel implements ComboBoxModel, Map<String, String> {

    private boolean vocabulary = false;
    
    private TreeMap<String,String> values = new TreeMap<String,String>(String.CASE_INSENSITIVE_ORDER);

    private Map.Entry<String, String> selectedItem = null;

    public Object getSelectedItem() {
        return selectedItem;
    }

    public void setSelectedItem(Object anItem) {
        this.selectedItem = (java.util.Map.Entry<String, String>) anItem;
        fireContentsChanged(this, -1, -1);
    }

    public Object getElementAt(int index) {
        List<Map.Entry<String, String>> list = new ArrayList<Map.Entry<String, String>>(values.entrySet());
        return list.get(index);
    }

    public int getSize() {
        return values.size();
    }

    public void clear() {
        values.clear();
    }

    public boolean containsKey(Object key) {
        return values.containsKey(key);
    }

    public boolean containsValue(Object value) {
        return values.containsValue(value);
    }

    public Set<java.util.Map.Entry<String, String>> entrySet() {
        return values.entrySet();
    }

    public String get(Object key) {
        return values.get(key);
    }

    public Set<String> keySet() {
        return values.keySet();
    }

    public String put(String key, String value) {
        return values.put(key, value);
    }

    public String remove(Object key) {
        return values.remove(key);
    }

    public int size() {
        return values.size();
    }

    public Collection<String> values() {
        return values.values();
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public void putAll(Map<? extends String, ? extends String> m) {
        values.putAll(m);
    }
    
    public static String entryToString(Map.Entry<String, String> entry) {
        String str = entry.getKey();
        return str;
    }
    
    public void setVocabularyCombo(boolean valuec){
        vocabulary=valuec;
    }
    
    public void specialRenderCombo(JComboBox voc){
        voc.setRenderer(new DefaultListCellRenderer(){
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
                if(value instanceof Map.Entry){
                    Map.Entry<String,String> entry = (java.util.Map.Entry<String, String>) value;
                    if(vocabulary)    
                        return super.getListCellRendererComponent(list, entry.getKey(), index, isSelected, cellHasFocus);
                    else
                        return super.getListCellRendererComponent(list, entry.getValue(), index, isSelected, cellHasFocus);
                }
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
           }
       });
    }
}
