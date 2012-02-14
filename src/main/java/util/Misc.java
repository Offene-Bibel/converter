package util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Vector;

public class Misc {

	public static List<List<String>> readCsv(String filename) throws IOException
	{
		List<List<String>> result = new Vector<List<String>>();
		BufferedReader stream = new BufferedReader(new FileReader(filename));
		String line;
		while((line = stream.readLine()) != null) {
			List<String> lineElements = new Vector<String>();
			for(String element : line.split(",")) {
				lineElements.add(element.trim());
			}
			result.add(lineElements);
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
	       FileInputStream fileStream = new FileInputStream(filename);
	       ObjectInputStream objectStream = new ObjectInputStream(fileStream);
	       Object data = objectStream.readObject();
	       return data;
	}

}
