using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace eodeditor
{
    class ComboVocabulary
    {
        string ID = null;
        string description = null;
        public ComboVocabulary(string ID, string description)
        {
            this.ID = ID;
            this.description = description;
        }
        public override string ToString()
        {
            return this.description;
        }
        public string getValue()
        {
            return this.ID;
        }
    }
}
