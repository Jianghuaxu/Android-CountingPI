package util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.PatternSyntaxException;

import android.content.Context;

public class LocalStorageUtil {
    //Directory
    private static String sDir = "unsavedWOs";

    public static String getDir() {
        return sDir;
    }

    public static void setDir(String sDir) {
        LocalStorageUtil.sDir = sDir;
    }

    /**
     * Save a WO to internal storage.
     *
     */
    public static void saveWOToFile(Context mcoContext,String sWarehouseOrder, String sBody){
        saveDataToFile(mcoContext, sDir, sWarehouseOrder, sBody);
    }

    /**
     * Get unsaved WOs from internal storage.
     *
     */
    public static  ArrayList<String> getUnsavedWOs(Context mcoContext){
        return getFileNames(mcoContext, sDir, null, 1);
    }

    /**
     * Get unsaved data of a WO and delete it from internal storage.
     *
     */
    public static  String getUnsavedData(Context mcoContext, String sWarehouseOrder){
        String sBody = readDataFromFile(mcoContext, sDir, sWarehouseOrder);
        return sBody;
    }
    /**
     * Delete WO file from internal storage.
     *
     */
    public static boolean deleteWO(Context mcoContext, String sWarehouseOrder){
        File fileDir = new File(mcoContext.getFilesDir(),sDir);
        if(!fileDir.exists() || !fileDir.isDirectory()){
            return false;
        }
        if (sWarehouseOrder == null){
            String[] files = fileDir.list();

            for (int i = 0; i < files.length; i++) {
                File fileWO = new File(fileDir, files[i]);
                if(fileWO.exists()){
                    fileWO.delete();
                }
            }
            return true;
        }else{
            File fileWO = new File(fileDir, sWarehouseOrder);
            if(!fileWO.exists()){
                return false;
            }
            return fileWO.delete();
        }
    }
    /**
     * Write a file to internal storage.
     *
     */
    public static void saveDataToFile(Context mcoContext,String sDir,String sFileName, String sBody){
        File file = new File(mcoContext.getFilesDir(),sDir);
        if(!file.exists()){
            file.mkdir();
        }

        try{
            File gpxfile = new File(file, sFileName);
            FileWriter writer = new FileWriter(gpxfile);
            writer.append(sBody);
            writer.flush();
            writer.close();

        }catch (Exception e){
            e.printStackTrace();

        }
    }
    /**
     * Read a file from internal storage.
     *
     */
    public static String readDataFromFile(Context mcoContext,String sDir, String sFileName){
        File fileDir = new File(mcoContext.getFilesDir(),sDir);
        if(!fileDir.exists() || !fileDir.isDirectory()){
            return null;
        }
        File fileWO = new File(fileDir, sFileName);
        //reading text from file
        InputStreamReader InputRead = null;
        String s="";
        try {
            FileInputStream fileIn=new FileInputStream(fileWO);
            InputRead= new InputStreamReader(fileIn);

            char[] inputBuffer= new char[1024];

            int charRead;

            while ((charRead=InputRead.read(inputBuffer))>0) {
                String readstring=String.copyValueOf(inputBuffer,0,charRead);
                s +=readstring;
            }
            return s;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }finally {
            if (InputRead != null) {
                try {
                    InputRead.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Get a list of filenames in this folder.
     */
    public static ArrayList<String> getFileNames
    (Context mcoContext, final String folder, final String fileNameFilterPattern, final int sort)
            throws PatternSyntaxException
    {
        ArrayList<String> myData = new ArrayList<String>();
        File fileDir = new File(mcoContext.getFilesDir(),folder);
        //Boolean a = fileDir.exists();
        //Boolean b = fileDir.isDirectory();
        if(!fileDir.exists() || !fileDir.isDirectory()){
            return null;
        }

        String[] files = fileDir.list();

        if(files.length == 0){
            return null;
        }
        for (int i = 0; i < files.length; i++) {
            if(fileNameFilterPattern == null ||
                    files[i].matches(fileNameFilterPattern))
                myData.add(files[i]);
        }
        if (myData.size() == 0)
            return null;

        if (sort != 0)
        {
            Collections.sort(myData, String.CASE_INSENSITIVE_ORDER);
            if (sort < 0)
                Collections.reverse(myData);
        }

        return myData;
    }

}
