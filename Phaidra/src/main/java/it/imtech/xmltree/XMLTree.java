package it.imtech.xmltree;


import it.imtech.bookimporter.BookImporter;
import it.imtech.globals.Globals;
import it.imtech.utility.Utility;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.ghost4j.document.DocumentException;
import org.ghost4j.document.PDFDocument;
import org.ghost4j.renderer.RendererException;
import org.ghost4j.renderer.SimpleRenderer;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

/**
 *
 * @author luigi
 */
public class XMLTree extends JTree {

    public final static Logger logger = Logger.getLogger(XMLTree.class.getName());
    private static XMLTreeModel xmlTreeModel;
    private static XMLNode root;
    private XMLTreeCellRenderer xmlTreeCellRenderer;
    private Document xmlDoc;
    private AbstractAction expandCollapseAction;
    private JPopupMenu popupMenu;
    private TreePath clickedPath;
    public static TreeMap<String, String> fileNameFromBookStructure;
    private static int abspagenum;
    private static int seq;
    private static int pagenum;
    //int[] selRowsShowPopup;
    List<XMLNode> listNodesForMenuAction;
    List<XMLNode> nodesToInsert;
    ResourceBundle bundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);
    //Oggetto contenente documento XML per non salvare il file
    public static Document savedXmlDoc = null;
    public static ArrayList<String> videotypes = new ArrayList<String>(Arrays.asList(new String[]{".mp4",".avi",".mpeg",".mov",".wmv"}));
    
    public static XMLNode getRoot() {
       return root;
    }

    public static XMLTreeModel getXmlTreeModel() {
        return xmlTreeModel;
    }

    /**
     *
     * @param selectedFolderOrFile
     * @param fromFile
     * @param refresh
     * @param isUndo
     */
    public XMLTree(String selectedFolderOrFile, boolean fromFile, boolean isUndo, boolean refresh) {
        try {

            String xmlFileBookstructure = selectedFolderOrFile;

            if (!isUndo) {
                xmlFileBookstructure = getFileBookstructure(selectedFolderOrFile, fromFile, refresh);
            }
            
            root = getRoot(xmlFileBookstructure, fromFile, refresh);
            if (root == null) {
                return;
            }
           
            //logger.debug(root.toString());
        } catch (ParserConfigurationException ex) {
            logger.error("XMLTree ParserConfigurationException exception:"
                    + ex.getMessage());
        } catch (SAXException ex) {
            logger.error("XMLTree SAXException exception:"
                    + ex.getMessage());
        } catch (IOException ex) {
            logger.error("XMLTree IOException exception:"
                    + ex.getMessage());
        } catch (NullPointerException ex) {
            logger.error("XMLTree NullPointerException exception:"
                    + ex.getMessage());
        } catch (Exception ex) {
            logger.error("XMLTree Exception exception:" + ex.getMessage());
        }

        xmlTreeModel = new XMLTreeModel(root);
        xmlTreeCellRenderer = new XMLTreeCellRenderer();

        // set the treemodel
        setModel(xmlTreeModel);

        setShowsRootHandles(true);
        // add Tree Will Expand Listener
        addTreeWillExpandListener(xmlTreeModel);
        // enable drag n drop
        setDragEnabled(true);
        setDropMode(DropMode.ON_OR_INSERT);

        xmlTreeModel.addTreeModelListener(new XMLTreeModelListener());

        setTransferHandler(new TreeTransferHandler());
        getSelectionModel().setSelectionMode(TreeSelectionModel.CONTIGUOUS_TREE_SELECTION);
        putClientProperty("JTree.lineStyle", "Angled");
        // put Client Property for icons
        putClientProperty("icons", getIcons());
        // set the tree cell renderer
        setCellRenderer(xmlTreeCellRenderer);
        // attach the popup
        popupMenu = getJPopupForExplorerTree();
        add(popupMenu);
        
        
        updateLanguage();
        
        // add the mouse listener for showing the popup
        addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                //Node is always selected
                clickedPath = getPathForLocation(e.getX(), e.getY());

                if (clickedPath != null) {
                    XMLNode node = (XMLNode) clickedPath.getLastPathComponent();
                    JLabel labelImage = BookImporter.getInstance().getLabelPreviewImage();
                    Dimension label_dim = labelImage.getSize();
                        
                    if (node.isLeaf()) {
                    	String file = node.getHref();
                        Element el = (Element) node.getUserObject();
                        String dir = (StringUtils.isEmpty(el.getAttribute("folder")) ? "" : el.getAttribute("folder") + Utility.getSep());
                        file = Globals.SELECTED_FOLDER_SEP + dir + file;
                     	setImagePreview(labelImage, file);
                    }
                    else{
                    	labelImage.setText("");
                        labelImage.setMinimumSize(label_dim);
                    }
                }
				
                if (e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e)) {
                    //Select node only if there is a single selection
                    TreePath[] paths = getSelectionPaths();
                    
                    if (paths == null || paths.length == 1){
                        setSelectionPath(clickedPath);  
                    }
                    showPopup(e);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopup(e);
                }
            }

            private void showPopup(MouseEvent e) {
                clickedPath = getPathForLocation(e.getX(), e.getY());

                //reinizializzo la lista dei nodi xml per averla disponibile
                //per le action del menu contestuale
                listNodesForMenuAction = new ArrayList<XMLNode>();
                int[] selRowsForMenuAction = BookImporter.xmlTree.getSelectionRows();
                TreePath[] selPaths = BookImporter.xmlTree.getSelectionPaths();
                if (selRowsForMenuAction != null) {
                    for (int i = 0; i < selRowsForMenuAction.length; i++) {
                        TreePath path = BookImporter.xmlTree.getPathForRow(selRowsForMenuAction[i]);
                        XMLNode xmlNode = (XMLNode) path.getLastPathComponent();
                        listNodesForMenuAction.add(xmlNode);

                    }
                }


                if (clickedPath != null) {
                    XMLNode node = (XMLNode) clickedPath.getLastPathComponent();
                    // org.w3c.dom.Element el = node.getElement();
                    if (!node.isLeaf()) {
                        if (isExpanded(clickedPath)) {
                            expandCollapseAction.putValue(Action.NAME, Utility.getBundleString("mc_collapse", bundle));
                        } else {
                            expandCollapseAction.putValue(Action.NAME, Utility.getBundleString("mc_expand", bundle));
                        }
                    } else {
                        expandCollapseAction.putValue(Action.NAME, Utility.getBundleString("mc_open", bundle));
                    }
                    popupMenu.show((JComponent) e.getSource(), e.getX(), e.getY());
                    setSelectionPaths(selPaths);

                }
            }
        });

    }

    /**
     * gestione menu contestuale
     *
     * @return
     */
    private JPopupMenu getJPopupForExplorerTree() {

        AbstractAction renameAction;
        AbstractAction copyAction;
        AbstractAction cutAction;
        AbstractAction pasteAction;
        AbstractAction deleteAction;
        AbstractAction addAction;
        AbstractAction undoAction;
        AbstractAction setFirstpageAction;
        AbstractAction addBlankPageAction;
        AbstractAction addNewPageAction;
        
        AbstractAction exportSpecificMetadata;
        AbstractAction viewSpecificMetadata;

        JPopupMenu popup = new JPopupMenu();

        expandCollapseAction = new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
                if (clickedPath == null) {
                    return;
                }
                if (isExpanded(clickedPath)) {
                    collapsePath(clickedPath);
                } else {
                    expandPath(clickedPath);
                }
            }
        };


        addBlankPageAction = new AbstractAction(Utility.getBundleString("mc_blankpage", bundle), IconFactory.getIcon(
                "blankpage", IconFactory.IconSize.SIZE_16X16)) {

            public void actionPerformed(ActionEvent e) {
                if (Globals.FOLDER_WRITABLE) {
                    XMLNode xmlNode = (XMLNode) getSelectionPath().getLastPathComponent();
                    exportBookstructureXUndo();
                    String suffix = ".jpg";

                    List<String> lf = getBlankPageFromSelectedFolder(Globals.SELECTED_FOLDER);
                    Iterator<String> ilf = lf.listIterator();
                    int max = 0;
                    while (ilf.hasNext()) {
                        String sn = StringUtils.substringBetween(ilf.next(), "blankpage_", suffix);
                        int n = new Integer(sn).intValue();
                        if (n > max) {
                            max = n;
                        }
                    }
                    max++;

                    String newblankpage = "blankpage_" + max + ".jpg";

                    XMLUtil.copyfile(Globals.BLANKPAGE, Globals.SELECTED_FOLDER_SEP + newblankpage);

                    XMLNode newXmlnode = XMLUtil.createXMLNode(newblankpage, newblankpage);

                    nodesToInsert = new ArrayList<XMLNode>();
                    nodesToInsert.add(newXmlnode);
                    insertListXmlNodesInParentNode(xmlNode);
                    
                } else {
                    JOptionPane.showMessageDialog(new Frame(), Utility.getBundleString("opnotpermitted", bundle));
                }
            }
        };

        exportSpecificMetadata = new AbstractAction(Utility.getBundleString("mc_exmetadata", bundle), IconFactory.getIcon(
                "blankpage", IconFactory.IconSize.SIZE_16X16)) {

            public void actionPerformed(ActionEvent e) {
                String singlemetadata = "";
                    
                if (Globals.FOLDER_WRITABLE) {
                    XMLNode xmlNode = (XMLNode) getSelectionPath().getLastPathComponent();
                    String metadata = ((Element) xmlNode.getUserObject()).getAttribute("metadata");
                    
                    if (metadata != null && !metadata.isEmpty()){
                        singlemetadata = Globals.SELECTED_FOLDER_SEP + metadata;
                    }
                    else{
                        File singlefile = Utility.getUniqueFileName(Globals.SELECTED_FOLDER_SEP + "uwmetadata", "xml");
                        singlemetadata = singlefile.getAbsolutePath();
                    }
                    
                    BookImporter.getInstance().exportMetadataSilent(singlemetadata );
                    File metadatafile = new File(singlemetadata);
                    
                    ((Element) xmlNode.getUserObject()).setAttribute("metadata", metadatafile.getName());
                } else {
                    JOptionPane.showMessageDialog(new Frame(), Utility.getBundleString("opnotpermitted", bundle));
                }
            }
        };
        
        viewSpecificMetadata = new AbstractAction(Utility.getBundleString("mc_vimetadata", bundle), IconFactory.getIcon(
                "blankpage", IconFactory.IconSize.SIZE_16X16)) {

            public void actionPerformed(ActionEvent e) {
                if (Globals.FOLDER_WRITABLE) {
                    XMLNode xmlNode = (XMLNode) getSelectionPath().getLastPathComponent();
                    String metadata = Globals.SELECTED_FOLDER_SEP + ((Element) xmlNode.getUserObject()).getAttribute("metadata");
                    
                    if (new File(metadata).isFile()){ 
                        BookImporter.getInstance().importMetadataSilent(metadata);
                    }
                    else{
                        
                    }
                } else {
                    JOptionPane.showMessageDialog(new Frame(), Utility.getBundleString("opnotpermitted", bundle));
                }
            }
        };

        
        addNewPageAction = new AbstractAction(Utility.getBundleString("mc_newpage", bundle), IconFactory.getIcon(
                "newpage", IconFactory.IconSize.SIZE_16X16)) {

            public void actionPerformed(ActionEvent e) {
                if (Globals.FOLDER_WRITABLE) {
                    XMLNode xmlNode = (XMLNode) getSelectionPath().getLastPathComponent();
                    String[] filesName = XMLUtil.chooseFileImage();
                    if (filesName != null) {
                        exportBookstructureXUndo();
                        nodesToInsert = new ArrayList<XMLNode>();
                        for (String fileName : filesName) {

                            XMLNode newXmlnode = XMLUtil.createXMLNode(fileName, fileName);

                            //imposta nodi da inserire
                            nodesToInsert.add(newXmlnode);
                        }

                        insertListXmlNodesInParentNode(xmlNode);
                    }
                } else {
                    JOptionPane.showMessageDialog(new Frame(), Utility.getBundleString("opnotpermitted", bundle));
                }
            }
        };

        renameAction  = new AbstractAction(Utility.getBundleString("mc_rename", bundle), IconFactory.getIcon(
                    "rename", IconFactory.IconSize.SIZE_16X16)) {

                public void actionPerformed(ActionEvent e) {
                    XMLNode xmlNode = (XMLNode) getSelectionPath().getLastPathComponent();

                    String nodeName = xmlNode.getName();
                    if (!("root".equals(nodeName) || 
                            Utility.getBundleString("sbook", bundle).equals(nodeName) || 
                            Utility.getBundleString("scollection", bundle).equals(nodeName))) {
                        exportBookstructureXUndo();
                        setInvokesStopCellEditing(true);
                        setEditable(true);
                        startEditingAtPath(clickedPath);
                    }
                }
        };
        
        copyAction  = new AbstractAction(Utility.getBundleString("mc_copy", bundle), IconFactory.getIcon("copy",
                IconFactory.IconSize.SIZE_16X16)) {

            public void actionPerformed(ActionEvent e) {
                setNodesToPast();
            }
        };
        
        cutAction  = new AbstractAction(Utility.getBundleString("mc_cut", bundle), IconFactory.getIcon("cut",
                IconFactory.IconSize.SIZE_16X16)) {

            public void actionPerformed(ActionEvent e) {
                setNodesToPast();
                deleteXmlNodes();
            }
        };
            
        pasteAction  = new AbstractAction(Utility.getBundleString("mc_paste", bundle), IconFactory.getIcon("paste",
                    IconFactory.IconSize.SIZE_16X16)) {

                public void actionPerformed(ActionEvent e) {
                    if (nodesToInsert == null) {
                        return;
                    }
                    
                    exportBookstructureXUndo();
         
                    XMLNode xmlNode = (XMLNode) getSelectionPath().getLastPathComponent();
                    insertListXmlNodesInParentNode(xmlNode);
                }
            };
            deleteAction  = new AbstractAction(Utility.getBundleString("mc_delete", bundle), IconFactory.getIcon(
                    "delete", IconFactory.IconSize.SIZE_16X16)) {

                public void actionPerformed(ActionEvent e) {
                    XMLNode xmlNode = (XMLNode) getSelectionPath().getLastPathComponent();

                    String nodeName = xmlNode.getName();
                    if (!("root".equals(nodeName) || Utility.getBundleString("sbook", bundle).equals(nodeName) 
                            || Utility.getBundleString("scollection", bundle).equals(nodeName))) {
                        exportBookstructureXUndo();
                        deleteXmlNodes();
                        listNodesForMenuAction = null;
                    }
                }
            };
            addAction  = new AbstractAction(Utility.getBundleString("mc_add", bundle), IconFactory.getIcon("addFolder",
                    IconFactory.IconSize.SIZE_16X16)) {

                public void actionPerformed(ActionEvent e) {
                    exportBookstructureXUndo();
                    DefaultMutableTreeNode root = (DefaultMutableTreeNode) XMLTree.getXmlTreeModel().getRoot();
                    DefaultMutableTreeNode nodeToAdd = null;
                    Enumeration enum1 = root.breadthFirstEnumeration();
                    while (enum1.hasMoreElements()) {
                        // get the node
                        nodeToAdd = (DefaultMutableTreeNode) enum1.nextElement();
                        if (Utility.getBundleString("sbook", bundle).equals(nodeToAdd.toString()) || 
                            Utility.getBundleString("scollection", bundle).equals(nodeToAdd.toString())) {
                            break;
                        }
                    }

                    try {
                        final DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                        final Document doc = builder.newDocument();
                        final Element capitolo = doc.createElement("book:structure");

                        capitolo.setAttribute("name", "new");
                        capitolo.setAttribute("seq", "1");
                        doc.appendChild(capitolo);

                        XMLNode newxmlnode = new XMLNode(capitolo);
                        nodeToAdd.add(newxmlnode);

                        XMLTree.getXmlTreeModel().reload(root);
                        
                       // XMLTree.root=(XMLNode) root;
                        XMLTree.expandAll(BookImporter.xmlTree);

                    } catch (Exception ex) {
                        // TODO: handle exception
                        logger.error(ex.getMessage());
                    }

                }
            };
            undoAction  = new AbstractAction(Utility.getBundleString("mc_undo", bundle), IconFactory.getIcon("undo",
                    IconFactory.IconSize.SIZE_16X16)) {

                public void actionPerformed(ActionEvent e) {
                    logger.debug(e.getActionCommand());
                    String lastFileName = UndoGetLastFile();

                    if (lastFileName != null) {
                        BookImporter.getInstance().initializeUndoXmlTree(lastFileName);
                        File file = new File(lastFileName);
                        file.delete();
                    }
                    else {
                        ResourceBundle bundles = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);
                        JOptionPane.showMessageDialog(new Frame(), Utility.getBundleString("nomoreundo", bundles));
                    }
                }
            };
            setFirstpageAction  = new AbstractAction(Utility.getBundleString("mc_firstpage", bundle), IconFactory.getIcon(
                    "firstpage", IconFactory.IconSize.SIZE_16X16)) {

                public void actionPerformed(ActionEvent e) {
                    exportBookstructureXUndo();

                    DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) root;
                    Enumeration en = rootNode.breadthFirstEnumeration();
                    //Reimposto tutte i nodi leaf con firstpage a false
                    while (en.hasMoreElements()) {
                        XMLNode xmlnode = (XMLNode) en.nextElement();
                        if (xmlnode.isLeaf()) {
                            ((Element) xmlnode.getUserObject()).setAttribute("firstpage", "false");
                        }
                    }

                    XMLNode xmlNodeFirstPage = (XMLNode) getSelectionPath().getLastPathComponent();
                    ((Element) xmlNodeFirstPage.getUserObject()).setAttribute("firstpage", "true");

                    //export e inizializzazione x refresh albero
                    exportBookstructure(Globals.SELECTED_FOLDER_SEP);
                    BookImporter.getInstance().initializeXmlTree(false,true);
                }
            };

            popup.add (expandCollapseAction);

            popup.addSeparator ();

            popup.add (addBlankPageAction);

            popup.add (addNewPageAction);

            popup.add (setFirstpageAction);

            popup.add (renameAction);

            popup.add (copyAction);

            popup.add (cutAction);

            popup.add (pasteAction);

            popup.add (deleteAction);

            popup.add (addAction);

            //popup.add (exportSpecificMetadata);
            
            //popup.add (viewSpecificMetadata);
            
            popup.add (undoAction);
            return popup ;
        }

    

    private void deleteXmlNodes() {

        Iterator<XMLNode> iter = listNodesForMenuAction.listIterator();
        while (iter.hasNext()) {
            XMLNode xmlNode = iter.next();
            if ((XMLNode) (xmlNode.getParent()) != null) {
                xmlTreeModel.removeNodeFromParent(xmlNode);
            }

        }
    }

    /**
     * Imposta l'insieme dei nodi da inserire in ordine invertito perche
     * l'inserimento della paste action sia corretto
     */
    private void setNodesToPast() {
            nodesToInsert = new ArrayList<XMLNode>();
            Object[] array = listNodesForMenuAction.toArray();
            int len = array.length;
            for (int i = 0; i < len; i++) {
                XMLNode oldXmlNode = (XMLNode) array[len - i - 1];
                XMLNode newXmlnode = XMLUtil.createXMLNode(oldXmlNode.getName(), oldXmlNode.getHref());
                nodesToInsert.add(newXmlnode);
            }
    }

    public static void insertAtTheEnd(XMLNode xmlNode, ArrayList<String> files) {
        HashMap<Integer, MutableTreeNode> hmParentNode = XMLUtil.getIndexNode(xmlNode);
        ArrayList<XMLNode> newItems = new ArrayList<XMLNode>();
        
        if (!hmParentNode.isEmpty()) {
            Iterator<Integer> i = hmParentNode.keySet().iterator();
            Integer index = (Integer) i.next();

            for (int is = files.size()-1; is >=0 ; is--) {
                XMLNode newXmlnode = XMLUtil.createXMLNode(files.get(is), files.get(is));
                newItems.add(newXmlnode);
            }
            Iterator<XMLNode> iterNodesToInsert = newItems.listIterator();
            while (iterNodesToInsert.hasNext()) {
                MutableTreeNode mn = (MutableTreeNode) iterNodesToInsert.next();
                xmlTreeModel.insertNodeInto(mn, hmParentNode.get(index), index);
            }
        }
    }

    /**
     * Inserisce la lista dei nodi sul padre del nodo scelto (la HashMap
     * contiene il parent node e l'index dell'xmlNode di riferimento
     * (xmlNodeRif)
     *
     * @param xmlNodeRif
     */
    private void insertListXmlNodesInParentNode(XMLNode xmlNodeRif) {
        HashMap<Integer, MutableTreeNode> hmParentNode = XMLUtil.getIndexNode(xmlNodeRif);
        
        if (!hmParentNode.isEmpty()) {
            Iterator<Integer> i = hmParentNode.keySet().iterator();
            Integer index = (Integer) i.next();
            Iterator<XMLNode> iterNodesToInsert = nodesToInsert.listIterator();
            while (iterNodesToInsert.hasNext()) {
                MutableTreeNode mn = (MutableTreeNode) iterNodesToInsert.next();
                xmlTreeModel.insertNodeInto(mn, hmParentNode.get(index), index);
            }
        }

        nodesToInsert = null;
    }

    /**
     * Restituisce tutte le immagini di un libro in ordine dalla prima
     * all'ultima
     *
     * @return ArrayList<String> contenten tutti i path alle immagini di un
     * libro
     * @throws ParserConfigurationException
     */
    public static ArrayList<XMLPage> getImagesFromStructure() {
        ArrayList<XMLPage> result = new ArrayList<XMLPage>();       
        
        try {
            savedXmlDoc.getDocumentElement().normalize();

            NodeList nodeLst = null;
            if (Globals.TYPE_BOOK == Globals.BOOK) {
                nodeLst = savedXmlDoc.getElementsByTagName("book:structure");
            } else {
                NodeList list = savedXmlDoc.getElementsByTagName("coll:pages");
                if (list.getLength() == 1) {
                    Node nodo = list.item(0);
                    nodeLst = nodo.getChildNodes();
                }
            }
            XMLPage resultPage = null;
            
            for (int s = 0; s < nodeLst.getLength(); s++) {
                resultPage = null;
                if (nodeLst.item(s).getNodeType() == Node.ELEMENT_NODE) {
                    Element chapter = (Element) nodeLst.item(s);

                    if (!chapter.hasChildNodes()) {
                        if (chapter.hasAttribute("folder")) {
                            resultPage = new XMLPage(chapter.getAttribute("folder") + Utility.getSep() + chapter.getAttribute("pid"), "folder");
                        } else {
                            resultPage = new XMLPage(chapter.getAttribute("pid"), chapter.getAttribute("href"));
                        }
                        result.add(resultPage);
                    } else {
                        NodeList leaves = chapter.getChildNodes();
                        for (int z = 0; z < leaves.getLength(); z++) {
                            if (leaves.item(z).getNodeType() == Node.ELEMENT_NODE) {
                                Element page = (Element) leaves.item(z);

                                if (page.hasAttribute("folder")) {
                                    resultPage = new XMLPage(page.getAttribute("folder") + Utility.getSep() + page.getAttribute("pid"), "folder");
                                } else {
                                    resultPage = new XMLPage(page.getAttribute("pid"), page.getAttribute("href"));
                                }
                                result.add(resultPage);
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            logger.error(ex);
        } 

        return result;
    }

    public void setImagePreview(JLabel labelImage, String imageFile)  {
        
        if ( imageFile.toLowerCase().endsWith(".pdf")){
            File file=new File(imageFile);
            
            try {
                if (System.getProperty("os.name").toLowerCase().startsWith("win")) {
                    System.load(Globals.JRPATH+"lib"+Utility.getSep()+"gsdll32.dll");
                }
                
                PDFDocument document = new PDFDocument();
                document.load(new File(imageFile));
                SimpleRenderer renderer = new SimpleRenderer();
                
                renderer.setResolution(100);
                int pages = document.getPageCount();
        
                if (pages > 0){
                    Image img = renderer.render(document, 0, 1).get(0);
                    
                    Icon scaledIcon = (Icon) new ImageIcon(IconFactory.getScaledImage(img, IconFactory.IconSize.SIZE_SCALE));
                    labelImage.setIcon(scaledIcon);
                    labelImage.setBorder(null);
                    labelImage.setMinimumSize(new Dimension(scaledIcon.getIconWidth(), scaledIcon.getIconHeight()));
                }
            } catch (IOException ex) {
                java.util.logging.Logger.getLogger(XMLTree.class.getName()).log(Level.SEVERE, null, ex);
            } catch (RendererException ex) {
                java.util.logging.Logger.getLogger(XMLTree.class.getName()).log(Level.SEVERE, null, ex);
            } catch (DocumentException ex) {
                java.util.logging.Logger.getLogger(XMLTree.class.getName()).log(Level.SEVERE, null, ex);
            } 
        }
        else {
            Icon scaledIcon = IconFactory.getPreviewImage(imageFile, IconFactory.IconSize.SIZE_SCALE);
            labelImage.setIcon(scaledIcon);
            labelImage.setBorder(null);
            labelImage.setMinimumSize(new Dimension(scaledIcon.getIconWidth(), scaledIcon.getIconHeight()));
        }
    }

    private static String UndoGetLastFile() {
        ArrayList<String> fn = UndoGetArrayListFileName();
        if (fn == null || fn.isEmpty()) {
            return null;
        }

        return fn.get(fn.size() - 1);

    }

    private static void UndoCheckMaxFile() {
        ArrayList<String> fn = UndoGetArrayListFileName();

        if (fn != null && fn.size() > Globals.UNDO_MAX_FILE) {
            String firstFile = fn.get(0);
            File file = new File(firstFile);
            file.delete();
        }
    }

    private static ArrayList<String> UndoGetArrayListFileName() {
        ArrayList<String> alist = null;
        try {
            File dir = new File(Globals.UNDO_DIR);
            File[] listOfFiles = dir.listFiles();
            if (listOfFiles.length == 0) {
                return null;
            }

            TreeMap<String, String> filesName = new TreeMap<String, String>();

            for (int i = 0; i < listOfFiles.length; i++) {
                filesName.put(listOfFiles[i].getCanonicalPath(),
                        listOfFiles[i].getCanonicalPath());
            }

            alist = new ArrayList<String>(filesName.values());

        } catch (Exception e) {
            logger.error(e.getMessage());
            alist = null;
        }
        return alist;

    }

    private Hashtable<String, Icon> getIcons() {

        Hashtable<String, Icon> icons = new Hashtable<String, Icon>();
        icons.put("unknown",
                IconFactory.getIcon("unknown", IconFactory.IconSize.SIZE_16X16));
        icons.put("pdf",
                IconFactory.getIcon("Adobe", IconFactory.IconSize.SIZE_16X16));
        icons.put("dir",
                IconFactory.getIcon("dir", IconFactory.IconSize.SIZE_16X16));
        icons.put("/",
                IconFactory.getIcon("usb", IconFactory.IconSize.SIZE_16X16));
        icons.put("book",
                IconFactory.getIcon("book", IconFactory.IconSize.SIZE_16X16));
        icons.put("tiff",
                IconFactory.getIcon("tiff", IconFactory.IconSize.SIZE_16X16));
        icons.put("tif",
                IconFactory.getIcon("tiff", IconFactory.IconSize.SIZE_16X16));
        icons.put("jpg",
                IconFactory.getIcon("jpg", IconFactory.IconSize.SIZE_16X16));
        icons.put("jpeg",
                IconFactory.getIcon("jpg", IconFactory.IconSize.SIZE_16X16));
        icons.put("page",
                IconFactory.getIcon("page", IconFactory.IconSize.SIZE_16X16));
        icons.put("png",
                IconFactory.getIcon("png", IconFactory.IconSize.SIZE_16X16));
        icons.put("collection", IconFactory.getIcon("collection",
                IconFactory.IconSize.SIZE_16X16));
        icons.put("firstpage", IconFactory.getIcon("firstpage",
                IconFactory.IconSize.SIZE_16X16));
        icons.put("newpage", IconFactory.getIcon("newpage",
                IconFactory.IconSize.SIZE_16X16));
        icons.put("blankpage", IconFactory.getIcon("blankpage",
                IconFactory.IconSize.SIZE_16X16));
        
        for (int i = 0; i < XMLTree.videotypes.size(); i++) {
            String extension = XMLTree.videotypes.get(i);
            extension = extension.substring(1);
            icons.put(extension, IconFactory.getIcon("movie", IconFactory.IconSize.SIZE_16X16));
        }
        
        return icons;
    }

    /**
     *
     * @param selectedFolder
     * @return
     */
    public static char getTypeFileBookstructure(String selectedFolder)
            throws ParserConfigurationException, SAXException, IOException {

        File file = new File(selectedFolder + Globals.IMP_EXP_BOOK);
        if (!file.exists()) {
            return Globals.NOT_EXISTS;
        }

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = dbFactory.newDocumentBuilder();

        Document doc = builder.parse(new File(selectedFolder
                + Globals.IMP_EXP_BOOK));

        NodeList nodeLst = doc.getElementsByTagName("book:pages");

        if (nodeLst.getLength() > 0) {
            return Globals.BOOK;
        }

        nodeLst = doc.getElementsByTagName("coll:pages");
        if (nodeLst.getLength() > 0) {
            return Globals.COLLECTION;
        } else {
            logger.warn("bookstructure.xml found but is malformed. Now the file will be deleted.");
            file.delete();
            return Globals.NOT_EXISTS;
        }

    }

    /**
     *
     * @param selectedFolder
     * @param refresh
     * @return
     */
    private String getFileBookstructure(String selectedFolder, boolean fromFile,boolean refresh) {

        String xmlfile = selectedFolder + Globals.IMP_EXP_BOOK;

        if ((!(new File(selectedFolder + Globals.IMP_EXP_BOOK)).exists() && savedXmlDoc==null) || (!fromFile && !refresh)) {
            switch (Globals.TYPE_BOOK) {
                case Globals.BOOK:
                    xmlfile = createBookstructure(selectedFolder,
                            Globals.IMP_EXP_BOOK);
                    break;
                case Globals.COLLECTION:
                    xmlfile = createBookstructureCollection(selectedFolder,
                            Globals.IMP_EXP_BOOK);
                    break;
            }
        }

        return xmlfile;
    }

    /**
     *
     */
    private static List<String> getFileFromSelectedFolder(String selectedFolder) {

        File dir = new File(selectedFolder);
        //HashMap<String, String> filesName = new HashMap<String, String>();
        List<String> filesName = new ArrayList<String>();
        //TreeSet filesName = new TreeSet();
        try {
            boolean isCollectionLoading = (Globals.TYPE_BOOK == Globals.COLLECTION ? true : false);
            File[] listOfFiles = dir.listFiles();

            for (int i = 0; i < listOfFiles.length; i++) {
                String name = listOfFiles[i].getName();
                String extension = "."+FilenameUtils.getExtension(name);
                                
                boolean loadPdf = (name.toLowerCase().endsWith(".pdf") ||
                                   videotypes.contains(extension.toLowerCase())) && isCollectionLoading;
                
                if (listOfFiles[i].isFile() && loadPdf
                        || name.toLowerCase().endsWith(".jpg")
                        || name.toLowerCase().endsWith(".jpeg")
                        || name.toLowerCase().endsWith(".tiff")
                        || name.toLowerCase().endsWith(".tif")
                        || name.toLowerCase().endsWith(".png")) {

                    filesName.add(listOfFiles[i].getName());
                }
               
            }
            Collections.sort(filesName);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return filesName;
    }

    /**
     *
     */
    private static List<String> getBlankPageFromSelectedFolder(
            String selectedFolder) {

        File dir = new File(selectedFolder);
        //HashMap<String, String> filesName = new HashMap<String, String>();
        List<String> filesName = new ArrayList<String>();
        try {

            File[] listOfFiles = dir.listFiles();
            for (int i = 0; i < listOfFiles.length; i++) {
                if (listOfFiles[i].isFile()
                        && listOfFiles[i].getName().toLowerCase().startsWith("blankpage")) {
                    filesName.add(listOfFiles[i].getName());
                }

            }

        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return filesName;
    }

    /**
     *
     */
    private static TreeMap<String, String> getDirFromSelectedFolder(
            String selectedFolder) {

        File dir = new File(selectedFolder);
        TreeMap<String, String> dirName = new TreeMap<String, String>();
        try {

            File[] listOfFiles = dir.listFiles();
            for (int i = 0; i < listOfFiles.length; i++) {
                //String name = listOfFiles[i].getName();
                if (listOfFiles[i].isDirectory()) {
                    dirName.put(listOfFiles[i].getName(),
                            listOfFiles[i].getName());
                }

            }

        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return dirName;
    }

    public static void expandAll(XMLTree tree) {
        int row = 0;
        while (row < tree.getRowCount()) {
            tree.expandRow(row);
            row++;
        }
    }

    private XMLNode getRoot(String xmlFile,boolean fromFile,boolean refresh)
            throws ParserConfigurationException, SAXException, IOException {

        if (xmlFile == null) {
            return null;
        }

        Vector<Element> elements;

        if(fromFile && !refresh){
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = dbFactory.newDocumentBuilder();

            xmlDoc = builder.parse(new File(xmlFile));
            xmlDoc.normalize();
            XMLUtil.xmlWriter(xmlDoc);
            
            savedXmlDoc = xmlDoc;
        }
        elements = XMLTreeModel.getChildElements(savedXmlDoc);
        
        if (elements.size() > 0) {
            return new XMLNode(elements.get(0));
        }

        return null;
    }

    /**
     *
     */
    public static void exportBookstructureXUndo() {
        try {
            abspagenum = 0;
            seq = 0;
            pagenum = 0;

            final DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            final Document doc = builder.newDocument();
            final Element rootDoc = doc.createElement("book:book");
            rootDoc.setAttribute("xmlns:book",
                    "http://phaidra.univie.ac.at/XML/book/V1.0");
            final DefaultMutableTreeNode dmtn = (DefaultMutableTreeNode) xmlTreeModel.getRoot();

            addNode(doc, rootDoc, dmtn);
            doc.appendChild(rootDoc);

            long time = System.currentTimeMillis();
            XMLUtil.xmlWriter(doc, Globals.UNDO_DIR + time + ".xml");
            UndoCheckMaxFile();

        } catch (ParserConfigurationException e) {
            logger.error(e.getMessage());
        }
    }

    public static void exportBookStructureToFile(String path){ 
        if(Globals.FOLDER_WRITABLE){
            exportBookstructure(path);
            XMLUtil.xmlWriter(savedXmlDoc, path + Globals.IMP_EXP_BOOK);
        }
        else{
            ResourceBundle bundles = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);
            JOptionPane.showMessageDialog(new Frame(), Utility.getBundleString("opnotpermitted", bundles));
        }
        
    } 
    /**
     *
     *
     */
    public static void exportBookstructure(String path) {
        try {
            abspagenum = 0;
            seq = 0;
            pagenum = 0;

            final DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            final Document doc = builder.newDocument();
            final Element rootDoc = doc.createElement("book:book");
            rootDoc.setAttribute("xmlns:book",
                    "http://phaidra.univie.ac.at/XML/book/V1.0");
            final DefaultMutableTreeNode dmtn = (DefaultMutableTreeNode) root;

            addNode(doc, rootDoc, dmtn);
            doc.appendChild(rootDoc);

            savedXmlDoc = doc;
        } catch (ParserConfigurationException e) {
            logger.error(e.getMessage());
        }
    }

    /**
     *
     * @param node
     * @param document
     * @return
     */
    protected static Element createElement(DefaultMutableTreeNode node,
            Document document) {

        Node nodeObj = (Node) node.getUserObject();
        String tagName = nodeObj.getNodeName();

        final Element element = document.createElement(tagName);
        NamedNodeMap nmp = nodeObj.getAttributes();

        for (int i = 0; i < nmp.getLength(); i++) {
            Attr attr = (Attr) nmp.item(i);
            element.setAttribute(attr.getNodeName(), attr.getNodeValue());
            // ridefinisco gli attributi dei nodi per un riferimento corretto
            // della numerazione
            if ("book:structure".equals(tagName)
                    && "seq".equals(attr.getNodeName())) {
                element.setAttribute(attr.getNodeName(), "" + (++seq));
                pagenum = 0;
            } else if ("book:page".equals(tagName)
                    && "abspagenum".equals(attr.getNodeName())) {
                element.setAttribute(attr.getNodeName(), "" + (++abspagenum));
            } else if ("book:page".equals(tagName)
                    && "pagenum".equals(attr.getNodeName())) {
                element.setAttribute(attr.getNodeName(), "" + (++pagenum));
            }

        }

        return element;
    }

    /**
     *
     * @param document
     * @param parentNode
     * @param treeNode
     */
    protected static void addNode(Document document, Element parentNode, DefaultMutableTreeNode treeNode) {
        @SuppressWarnings("unchecked")
        Enumeration<DefaultMutableTreeNode> children = (Enumeration<DefaultMutableTreeNode>) treeNode.children();
        while (children.hasMoreElements()) {
            final DefaultMutableTreeNode node = (DefaultMutableTreeNode) children.nextElement();
            final Element element = createElement(node, document);
            parentNode.appendChild(element);
            addNode(document, element, node);
        }
    }

    /**
     * get FileName from nodes of the tree
     */
    public static TreeMap<String, String> getFileNameFromBookstructure() {

        fileNameFromBookStructure = new TreeMap<String, String>();
        walk(xmlTreeModel, root);

        return fileNameFromBookStructure;

    }

    /**
     *
     * @param model
     * @param o
     */
    private static void walk(XMLTreeModel model, Object o) {

        for (int i = 0; i < model.getChildCount(o); i++) {
            Object child = model.getChild(o, i);

            if (model.isLeaf(child)) {
                fileNameFromBookStructure.put(child.toString(),
                        child.toString());
            } else {
                walk(model, child);
            }

        }
    }

    /**
     *
     * @param selectedFolder
     * @param xmlfile
     * @return
     */
    public static String createBookstructureCollection(String selectedFolder,  String xmlfile) {
        String pathXmlfile = selectedFolder + xmlfile;
        String pid = "";
        
        try {
            final DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            final Document doc = builder.newDocument();
            final Element root = doc.createElement("book:book");
            root.setAttribute("xmlns:book", "http://phaidra.univie.ac.at/XML/book/V1.0");
            doc.appendChild(root);
            Element cg = doc.createElement("coll:pages");
            cg.setAttribute("id", "collection");
            Element els = null;
            int seqDir = 0;
            int iabs = 0;
            int irel = 0;
            List<String> sf = getFileFromSelectedFolder(Globals.SELECTED_FOLDER);
            
            Iterator<String> iterFileSf = sf.iterator();

            while (iterFileSf.hasNext()) {
                irel++;
                iabs++;
                Element elp = doc.createElement("book:page");
                elp.setAttribute("abspagenum", "" + iabs);
                elp.setAttribute("pagenum", "" + irel);
                elp.setAttribute("folder", "");
                
                pid = iterFileSf.next();
                elp.setAttribute("pid", pid);
                elp.setAttribute("href", pid);
                cg.appendChild(elp);
            }

            irel = 0;
            TreeMap<String, String> dirSelectedFolder = getDirFromSelectedFolder(selectedFolder);
            Collection<String> cf = dirSelectedFolder.values();

            Iterator<String> iterDir = cf.iterator();
            while (iterDir.hasNext()) {
                seqDir++;
                String dir = iterDir.next();
                els = doc.createElement("book:structure");
                
                els.setAttribute("name", dir);
                els.setAttribute("seq", seqDir + "");
                List<String> lf = getFileFromSelectedFolder(Globals.SELECTED_FOLDER_SEP + dir);

                Iterator<String> iterFile = lf.iterator();
                while (iterFile.hasNext()) {
                    irel++;
                    iabs++;
                    Element elp = doc.createElement("book:page");
                    elp.setAttribute("abspagenum", "" + iabs);
                    elp.setAttribute("pagenum", "" + irel);
                    elp.setAttribute("folder", dir);
                    
                    pid = iterFile.next();
                    elp.setAttribute("pid", pid);
                    elp.setAttribute("href", pid);
                    els.appendChild(elp);
                }
                irel = 0;
                cg.appendChild(els);
            }

            root.appendChild(cg);
            savedXmlDoc = doc;
            //XMLUtil.xmlWriter(doc, pathXmlfile);

        } catch (Exception e) {
            logger.error(e.getMessage());
            return null;
        }
        return pathXmlfile;

    }
    
    private void updateSingleNodeLanguage(String word, ResourceBundle bundle, XMLNode node){
        node.setName(Utility.getBundleString(word, bundle));
        Element object = (Element) node.getUserObject();
        object.setAttribute("name", Utility.getBundleString(word, bundle));
        node.setUserObject(object);
        xmlTreeModel.nodeStructureChanged(node);
    }
    
    public void updateLanguage(){
        bundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);
       
        final DefaultMutableTreeNode parent = (DefaultMutableTreeNode) xmlTreeModel.getRoot();
        final DefaultMutableTreeNode firstChild = (DefaultMutableTreeNode) parent.getFirstChild();
        
        XMLNode rootnode = (XMLNode) firstChild;
        if (Globals.TYPE_BOOK == Globals.BOOK && rootnode.getId().equals("book")){
            updateSingleNodeLanguage("sbook", bundle, rootnode);
        }
        if (Globals.TYPE_BOOK == Globals.COLLECTION && rootnode.getId().equals("collection")){
            updateSingleNodeLanguage("scollection", bundle, rootnode);
        }
        
        if (firstChild != null){
            Enumeration<DefaultMutableTreeNode> children = (Enumeration<DefaultMutableTreeNode>) firstChild.children();
            while (children.hasMoreElements()) {
                XMLNode node = (XMLNode) children.nextElement();
                if (Globals.TYPE_BOOK == Globals.BOOK && node.getId().equals("copertina")){
                    updateSingleNodeLanguage("scopertina", bundle, node);
                }
                if (Globals.TYPE_BOOK == Globals.BOOK && node.getId().equals("capitolo_uno")){
                    updateSingleNodeLanguage("scapitolo", bundle, node);
                }
            }
        }
  
        remove(popupMenu);
        popupMenu = getJPopupForExplorerTree();
        add(popupMenu);
    }
    
    public static String createBookstructure(String selectedFolder, String xmlfile) {
        String pathXmlfile = selectedFolder + xmlfile;
        String pid = "";
        
        try {
            ResourceBundle bundle = ResourceBundle.getBundle(Globals.RESOURCES, Globals.CURRENT_LOCALE, Globals.loader);
            List<String> lf = getFileFromSelectedFolder(Globals.SELECTED_FOLDER);

            final DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            final Document doc = builder.newDocument();
            final Element root = doc.createElement("book:book");
            root.setAttribute("xmlns:book", "http://phaidra.univie.ac.at/XML/book/V1.0");
            
            doc.appendChild(root);

            Element pg = doc.createElement("book:pages");
            pg.setAttribute("id", "book");
            
            Element els = doc.createElement("book:structure");
            els.setAttribute("name", Utility.getBundleString("scopertina", bundle));
            els.setAttribute("id", "copertina");
            els.setAttribute("seq", "0");

            if (!lf.isEmpty()) {
                Element elp = doc.createElement("book:page");
                elp.setAttribute("abspagenum", "1");
                elp.setAttribute("pagenum", "1");
                elp.setAttribute("firstpage", "false");

                Iterator<String> iter = lf.iterator();
                
                pid = iter.next();
                elp.setAttribute("pid", pid);
                elp.setAttribute("href", pid);
                els.appendChild(elp);
                pg.appendChild(els);

                els = doc.createElement("book:structure");
                els.setAttribute("name", Utility.getBundleString("scapitolo", bundle));
                els.setAttribute("id", "capitolo_uno");
                els.setAttribute("seq", "1");
                int iabs = 2;
                int irel = 1;
                while (iter.hasNext()) {
                    elp = doc.createElement("book:page");
                    elp.setAttribute("abspagenum", "" + iabs);
                    elp.setAttribute("pagenum", "" + irel);
                    
                    pid = iter.next();
                    elp.setAttribute("pid", pid);
                    elp.setAttribute("href", pid);
                    elp.setAttribute("firstpage", "false");
                    els.appendChild(elp);
                    irel++;
                    iabs++;
                }
            }

            pg.appendChild(els);
            root.appendChild(pg);

            savedXmlDoc = doc;
        } catch (Exception e) {
            logger.error(e.getMessage());
            return null;
        }
        return pathXmlfile;

    }
}
