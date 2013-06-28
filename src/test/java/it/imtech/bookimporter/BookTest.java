/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package it.imtech.bookimporter;
/*
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
 
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import at.ac.univie.phaidra.api.Phaidra;
import at.ac.univie.phaidra.api.objekt.Book;
import at.ac.univie.phaidra.api.objekt.Book.Chapter;
import at.ac.univie.phaidra.api.objekt.Page;

import junit.framework.TestCase;

public class BookTest extends TestCase{

	String username =   "testphaidra";
	String password =   "H83HC5Zq";
	String baseurl  =   "fedoradev.cab.unipd.it";
	String staticBaseURL = "phaidrastaticdev.cab.unipd.it";
	String stylesheetURL = "http://phaidrastaticdev.cab.unipd.it/stylesheets/externalview.xsl";
	String oaiIdentifier = "cab.unipd.it"; 
	
	public void setUp()	{
		BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.INFO);
		Logger.getLogger("at.ac.univie.phaidra").setLevel(Level.DEBUG);
	}
	
	public void tearDown() {
		BasicConfigurator.resetConfiguration();
	}
	
	public void testBook() throws Exception {
		
		Phaidra phaidra = new Phaidra(baseurl, staticBaseURL, stylesheetURL, oaiIdentifier, username, password);
		
		Book book = phaidra.createBook("Java Book API Test");
		book.addPDF("C://files//document.pdf");

		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		String uploadDate = df.format(new Date());

		String metadata = this.XMLtoSTRING("C://files//book//metadata.xml");					
		
		metadata = metadata.replace("PID", book.getPID());
		metadata = metadata.replace("UPLOADDATE", uploadDate);
		
		// add metadata
		book.addMetadata(metadata);		

		// add a chapter
		Chapter chapter = book.addChapter("Chapter 1");

		boolean startPage = true;
		Integer pagenum = 0;
		for (int i = 1 ; i <= 4 ; i++ ){
			String path = "C://files//book//pages//page" + i + ".jpg";
			pagenum++;
			
			Integer abspagenum = pagenum; 
			
			// create page
			Page page = phaidra.createPage("My Book Page " + abspagenum, book, abspagenum, pagenum.toString(), "Chapter 1", startPage);
			startPage = false;
			
			// add picture to page
                        page.addPicture(path, "image/jpeg");

                        metadata = metadata.replace("PID", page.getPID());
			df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
			uploadDate = df.format(new Date());
			metadata = metadata.replace("UPLOADDATE", uploadDate);
			
			page.addMetadata(metadata);
			// add page to book
			book.addPage(chapter, page);
			// save page
			page.save();
		}

		// save book
		book.save();
	}	
	
	private String XMLtoSTRING(String path) throws Exception{
		BufferedReader rd = new BufferedReader(new InputStreamReader(new FileInputStream(path), "UTF-8"));
		StringBuffer xml = new StringBuffer();
		String line = "";
		while((line = rd.readLine())!=null)	{
			xml.append(line);
		}
		return xml.toString();
	}
}
*/