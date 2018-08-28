package util;

import android.util.Xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

public class ReadFile {
    FileInputStream fin;
    String fileName = "com.journaldev.searchview/data.txt";
    String results;
    File f;
    public String read_pi_list() {
        try{
           //f =  new File(fileName);
           fin = new FileInputStream(fileName);
           int length = fin.available();
           byte[] buffer = new byte[length];
           fin.read(buffer);



        } catch(Exception err) {
            err.printStackTrace();
        }
        return fin.toString();
    }
}
