/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package it.imtech.bookimporter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Vector;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import at.ac.univie.phaidra.api.Objekt;
import at.ac.univie.phaidra.api.Phaidra;
import at.ac.univie.phaidra.api.objekt.Picture;


/**
 *
 * @author mauro
 */
public class RightsTest {
/*	String username =   "testphaidra";
	String password =   "H83HC5Zq";
	String baseurl  =   "fedoradev.cab.unipd.it";
	String staticBaseURL = "phaidrastaticdev.cab.unipd.it";
	String stylesheetURL = "http://phaidrastaticdev.cab.unipd.it/stylesheets/externalview.xsl";
	String oaiIdentifier = "cab.unipd.it"; 
	
	public void setUp()
	{
		BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.INFO);
		Logger.getLogger("at.ac.univie.phaidra").setLevel(Level.DEBUG);
	}
	
	public void tearDown()
	{
		BasicConfigurator.resetConfiguration();
	}
	
	// Neues Objekt vorbereiten, noch nicht gespeichert mit save()
	public Picture createObject(Phaidra phaidra) throws Exception
	{
		Picture picture = phaidra.createPicture("blalabel Java");
		picture.addPicture("files/picture.jpg", "image/jpeg");
		
		BufferedReader rd = new BufferedReader(new InputStreamReader(new FileInputStream("files/uwmetadata.xml"), "UTF-8"));
		StringBuffer xml = new StringBuffer();
		String line = "";
		while((line = rd.readLine())!=null)
		{
			xml.append(line);
		}
		picture.addMetadata(xml.toString());
		
		return picture;
	}
	
	public void testRights() throws Exception
	{
		Phaidra phaidra = new Phaidra(baseurl, staticBaseURL, stylesheetURL, oaiIdentifier,username, password);
		Picture picture = createObject(phaidra);
		
		picture.grantUsername("tw", null);
		picture.grantUsername("hohensh8", "2010-02-05T16:45:35");
		picture.grantSubEinheit("A140", null);
		
		// Sind die Rechte intern richtig gesetzt?
		HashMap<String, Vector<HashMap<String, String>>> rights = picture.getRights();
		HashMap<String, String> m = new HashMap<String, String>();
		
                m.clear();
		m.put("who", "tw");
		
                m.clear();
		m.put("who", "hohensh8");
		m.put("expires", "2010-02-05T16:45:35");
		
                m.clear();
		m.put("who", "A140");
	
		picture.save();
		
		// Ist der gespeicherte Datastream ok?
		Objekt o = phaidra.loadObject(picture.getPID());
		rights = o.getRights();
	
                m.clear();
		m.put("who", "tw");
	
                m.clear();
		m.put("who", "hohensh8");
		m.put("expires", "2010-02-05T16:45:35");
	
                m.clear();
		m.put("who", "A140");
		
		// Etwas revoken
		o.revokeUsername("hohensh8");
		rights = o.getRights();
	
                m.clear();
		m.put("who", "tw");
	
                m.clear();
		m.put("who", "hohensh8");
		m.put("expires", "2010-02-05T16:45:35");
	
                m.clear();
		m.put("who", "A140");
		
		// Speichern und wieder schauen...
		o.save();
		o = null;
		
		Objekt o2 = phaidra.loadObject(picture.getPID());
		rights = o2.getRights();
	
                m.clear();
		m.put("who", "tw");
	
                m.clear();
		m.put("who", "hohensh8");
		m.put("expires", "2010-02-05T16:45:35");
	
                m.clear();
		m.put("who", "A140");
	}
*/}

