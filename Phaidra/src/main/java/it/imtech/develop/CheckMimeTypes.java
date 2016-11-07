package it.imtech.develop;

import java.io.File;
import java.io.FileInputStream;
import java.net.FileNameMap;
import java.net.URLConnection;
import javax.swing.SwingUtilities;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;


/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author mauro
 */
public class CheckMimeTypes {
    FileNameMap fileNameMap = URLConnection.getFileNameMap();
    String pathp = "C:\\Users\\mauro\\Documents\\videotest\\autogen1.jpg";
    String pathv = "C:\\Users\\mauro\\Documents\\videotest\\test.avi";
    
    public CheckMimeTypes(){
        String picture = fileNameMap.getContentTypeFor(pathp);
        String video = fileNameMap.getContentTypeFor(pathv);

        System.out.println(picture);
        System.out.println(video);
        
        File x = new File(pathv);
        File y = new File(pathp);
        try {
            FileInputStream is = new FileInputStream(x);
        
            TikaConfig tika = new TikaConfig();
            MediaType mimetype = tika.getDetector().detect(TikaInputStream.get(is), new Metadata());
            System.out.println(mimetype.toString());
            
            is = new FileInputStream(y);
        
            tika = new TikaConfig();
            mimetype = tika.getDetector().detect(TikaInputStream.get(is), new Metadata());
            System.out.println(mimetype.toString());
          
        }
        catch (Exception e) {
          e.printStackTrace();
        }
    }
    
    public static void main(String[] args){
        SwingUtilities.invokeLater(new Runnable(){
            
            public void run(){
                   new CheckMimeTypes();
            }
        });
    };
}
