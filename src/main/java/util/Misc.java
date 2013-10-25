package util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.CodeSource;
import java.util.List;
import java.util.Vector;

public class Misc {

    public static List<List<String>> readCsv(String filename) throws IOException
    {
        List<List<String>> result = new Vector<List<String>>();
        try (BufferedReader stream = new BufferedReader(new FileReader(filename))) {
            String line;
            while((line = stream.readLine()) != null) {
                List<String> lineElements = new Vector<String>();
                for(String element : line.split(",")) {
                    lineElements.add(element.trim());
                }
                result.add(lineElements);
            }
        }
        return result;
    }

    public static String readBufferToString(BufferedReader reader) throws IOException
    {
        String result = "";
        char[] cbuf = new char[512];
        int readCount;
        while((readCount = reader.read(cbuf)) >= 0)
            result += String.valueOf(cbuf, 0, readCount);
        return result;
    }

    public static void writeFile(String text, String filename) throws IOException
    {
        BufferedWriter out = new BufferedWriter(new FileWriter(filename));
        out.write(text);
        out.close();
    }

    public static void createFolder(String path)
    {
        File folder = new File(path);
        if(folder.isDirectory())
            return;
        folder.mkdir();
    }

    public static String readFile(String file) throws IOException
    {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        return readBufferToString(reader);
    }

    public static void serializeBibleDataToFile(Serializable data, String filename) throws IOException
    {
        FileOutputStream fileStream = new FileOutputStream(filename);
        ObjectOutputStream objectStream = new ObjectOutputStream(fileStream);
        objectStream.writeObject(data);
        objectStream.flush();
        fileStream.close();
    }

    public static Object deserializeBibleDataToFile(String filename) throws IOException, ClassNotFoundException
    {
        try (ObjectInputStream objectStream = new ObjectInputStream(new FileInputStream(filename))) {
            Object data = objectStream.readObject();
            return data;
        }
    }

    public static File getDirOfProgram()
    {
        try {
            CodeSource codeSource = Misc.class.getProtectionDomain().getCodeSource();
            File jarFile = new File(codeSource.getLocation().toURI().getPath());
            return jarFile.getParentFile();


            /*
            String path = Misc.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            String decodedPath = URLDecoder.decode(path, "UTF-8");
            return new File(decodedPath);
             */
        } catch (URISyntaxException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static String getResourceDir()
    {
        return getDirOfProgram().getPath() + File.separator + ".." + File.separator + "resources" + File.separator;
    }

    public static String getPageCacheDir()
    {
        return getDirOfProgram().getPath() + File.separator + ".." + File.separator + "tmp" + File.separator + "pageCache" + File.separator;
    }

    public static String getResultsDir()
    {
        return getDirOfProgram().getPath() + File.separator + ".." + File.separator + "results" + File.separator;
    }

    public static String getWebResultsDir()
    {
        return getDirOfProgram().getPath() + File.separator + ".." + File.separator + "webResults" + File.separator;
    }

    public static String retrieveUrl(String url) throws IOException {
        URL urlHandler = new URL(url);
        try (BufferedReader in = new BufferedReader(new InputStreamReader(urlHandler.openStream(), "UTF-8"))) {
            return Misc.readBufferToString(in);
        }
    }
}
