/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package it.imtech.metadata;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Medelin Mauro
 */
public class Metadata {
        public Map<Object,Metadata> submetadatas = new HashMap<Object, Metadata>();
    
        public int MID;
        public int MID_parent;
        public String description;
        public String datatype;
        public String editable;
        public String foxmlname;
        public String foxmlnamespace;
        public String value;
        public String language;
        public String mandatory;
        public String hidden;
        public String sequence;

        public Metadata(int MID, int MID_parent, String description, String datatype, String editable, String foxmlname,String value, String foxmlnamespace,String mandatory,String hidden, String sequence)
        {
            this.MID = MID;
            this.MID_parent = MID_parent;
            this.description = description;
            this.datatype = datatype;
            this.editable = editable;
            this.foxmlname = foxmlname;
            this.value = value;
            this.foxmlnamespace = foxmlnamespace;
            this.language = null;
            this.mandatory = mandatory;
            this.hidden = hidden;
            this.sequence = sequence;
        }
}
