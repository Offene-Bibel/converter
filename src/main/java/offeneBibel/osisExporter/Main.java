package offeneBibel.osisExporter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.parboiled.Parboiled;
import org.parboiled.errors.ErrorUtils;
import org.parboiled.parserunners.BasicParseRunner;
import org.parboiled.parserunners.TracingParseRunner;
import org.parboiled.support.ParseTreeUtils;
import org.parboiled.support.ParsingResult;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import offeneBibel.parser.ObAstFixuper;
import offeneBibel.parser.ObAstNode;
import offeneBibel.parser.OffeneBibelParser;
import util.Misc;
import util.Pair;
	
public class Main
{
	//static final String m_urlBase = "http://www.offene-bibel.de/wiki/api.php?action=query&prop=revisions&rvprop=content&format=xml&titles=";
	static final String m_urlBase = "http://www.offene-bibel.de/wiki/index.php5?action=raw&title=";
	//the following list was created by combining the wiki page: Vorlage:Kapitelzahl and the OSIS 2.1.1 manual Appendix C.1
	static final String m_bibleBooks = "resources/bibleBooks.txt";
	//static final String m_bibleBooks = "resources/testbibleBooks.txt";
	static final String m_pageCache = "resources/pageCache/";
	static final String m_studienFassungTemplate = "resources/offene-bibel-studienfassung_template.txt";
	static final String m_leseFassungTemplate = "resources/offene-bibel-lesefassung_template.txt";
	static final String m_studienFassungFilename = "resources/offeneBibelStudienfassungModule.osis";
	static final String m_leseFassungFilename = "resources/offeneBibelLesefassungModule.osis";
	static final String m_bibleTextObjectFilename = "resources/bibleTextObjectFile.bin";
	
	public static void main(String [] args)
	{
		try {
		List<Map<String, Object>> bibleTexts = null;
//		File bibleTextObjectFile = new File(m_bibleTextObjectFilename);
//		if(bibleTextObjectFile.exists() == false) {
			bibleTexts = retrieveBooks();
/*			Misc.serializeBibleDataToFile((Serializable) bibleTexts, m_bibleTextObjectFilename);
		}
		else {
			bibleTexts = (List<Map<String, Object>>) Misc.deserializeBibleDataToFile(m_bibleTextObjectFilename);
		}
*/
		createOsisTextsForChapters(bibleTexts, true);
		String studienFassung = constructOsisText(putOsisTextTogether(bibleTexts, false), false);
		String leseFassung = constructOsisText(putOsisTextTogether(bibleTexts, true), true);
		
		Misc.writeFile(studienFassung, m_studienFassungFilename);
		Misc.writeFile(leseFassung, m_leseFassungFilename);
		System.out.println("done");
		
		} catch (IOException e) {
			e.printStackTrace();
		}
/*		catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
*/
	}

	private static String retrieveWikiPage(String wikiPage)
	{
		try {
			String result = null;
			String fileCacheString = m_pageCache + wikiPage;
			File fileCache = new File(fileCacheString);
			if(fileCache.exists()) {
				if(fileCache.length() == 0)
					return null;
				result = Misc.readFile(fileCacheString);
			}
			else {
				URL url = new URL(m_urlBase + URLEncoder.encode(wikiPage, "UTF-8"));
				System.out.println(url.toString());
				try {
					BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8"));
					result = Misc.readBufferToString(in);
			        in.close();
				} catch (FileNotFoundException e) {
			        Misc.writeFile("", fileCacheString);
					// chapter not yet created, skip
		        	return null;
				}
		        Misc.createFolder(m_pageCache);
		        Misc.writeFile(result, fileCacheString);
			}
	        return result;
        }
		catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private static List<Map<String, Object>> retrieveBooks() throws IOException
	{
		List<List<String>> bookList = Misc.readCsv(m_bibleBooks);
		List<Map<String, Object>>  bookDataCollection = new Vector<Map<String, Object>>();
		for(List<String> book : bookList) {
			Map<String, Object> bookData = new HashMap<String, Object>();
			bookData.put("germanName", book.get(0));
			bookData.put("swordName", book.get(1));
			bookData.put("chapterCount", Integer.parseInt(book.get(2)));
			
			List<Pair<Integer, String>> chapters = new Vector<Pair<Integer, String>>();
			//Genesis,Gen,50 == german name, sword name, chapter count
			for(int i = 1; i <= Integer.parseInt(book.get(2)); ++i) {
				String wikiPageName = book.get(0) + " " + i;
				String wikiText = retrieveWikiPage(wikiPageName);
				if(wikiText != null) {
					chapters.add(new Pair<Integer, String>(i, wikiText));
				}
			}
			bookData.put("chapterTexts", chapters);
			bookDataCollection.add(bookData);
		}
		
		return bookDataCollection;
	}

	/**
	 * 4<-studienfassung, 5<-lesefassung
	 * @param wikiTexts, 0 = german name, 1 = sword name, 2 = chapter count, 3 = wiki text
	 * @param leseFassung
	 */
	private static void createOsisTextsForChapters(List<Map<String, Object>> bibleTexts, boolean stopOnError)
	{
		OffeneBibelParser parser = Parboiled.createParser(OffeneBibelParser.class);
		BasicParseRunner<ObAstNode> parseRunner = new BasicParseRunner<ObAstNode>(parser.Page());
		
		for(Map<String, Object> bookData : bibleTexts) {
			List<Pair<Integer, String>> osisLesefassungChapterTexts = new Vector<Pair<Integer, String>>();
			List<Pair<Integer, String>> osisStudienfassungChapterTexts = new Vector<Pair<Integer, String>>();
			for(Pair<Integer, String> chapterData : (List<Pair<Integer, String>>)(bookData.get("chapterTexts"))) {
				ParsingResult<ObAstNode> result = parseRunner.run(chapterData.getY());
	
				if(result.matched == false) {
					TracingParseRunner<ObAstNode> tracingParseRunner = new TracingParseRunner<ObAstNode>(parser.Page());
					ParsingResult<ObAstNode> tracingResult = tracingParseRunner.run(chapterData.getY());
					
					System.out.println("Tree:");
					String parseTreePrintOut = ParseTreeUtils.printNodeTree(tracingResult);
					System.out.println(parseTreePrintOut);
					
					System.out.println("Errors:");
					ErrorUtils.printParseErrors(tracingParseRunner.getParseErrors());
					
					System.out.println("Book: " + bookData.get("swordName"));
					System.out.println("Chapter: " + chapterData.getX());
					
					if(stopOnError) {
						return;
					}
				}
				else {
					ObAstNode node = result.resultValue;
					ObAstFixuper.fixupAstTree(node);
					
					ObAstVisitor visitor = new ObAstVisitor(chapterData.getX(), (String)bookData.get("swordName"));
					try {
						node.host(visitor);
					} catch (Throwable e) {
						e.printStackTrace();
						return;
					}
					
	
					osisStudienfassungChapterTexts.add(new Pair<Integer, String>(chapterData.getX(), visitor.getStudienFassung()));				
					osisLesefassungChapterTexts.add(new Pair<Integer, String>(chapterData.getX(), visitor.getLeseFassung()));
				}
			}
			bookData.put("osisStudienfassungChapterTexts", osisStudienfassungChapterTexts);
			bookData.put("osisLesefassungChapterTexts", osisLesefassungChapterTexts);
		}
	}

	private static String putOsisTextTogether(List<Map<String, Object>> bibleTexts, boolean leseFassung)
	{
		String result = "";
		String studienVsLeseTag = leseFassung ? "osisLesefassungChapterTexts" : "osisStudienfassungChapterTexts";
		
		for(Map<String, Object> bookData : bibleTexts) {
			result += "<div type=\"book\" osisID=\"" + bookData.get("swordName") + "\" canonical=\"true\">\n<title type=\"main\">" + bookData.get("germanName") + "</title>\n";
			for(Pair<Integer, String> chapterData : (Vector<Pair<Integer, String>>)(bookData.get(studienVsLeseTag)) ) {
				result += "<chapter osisID=\"" + bookData.get("swordName") + "." + chapterData.getX() + "\">\n<title type=\"chapter\">Kapitel " + chapterData.getX() + "</title>\n";
				result += chapterData.getY();
				result += "</chapter>\n";
			}
			result += "</div>\n";
		}
		return result;
	}

	private static String constructOsisText(String osisText, boolean leseFassung) throws IOException
	{
		String result = Misc.readFile(leseFassung ? m_leseFassungTemplate : m_studienFassungTemplate);
		
		result = result.replace("{{revision}}", "TODO");
		result = result.replace("{{year}}", "" + Calendar.getInstance().get(Calendar.YEAR));
		result = result.replace("{{content}}", osisText);
		return result;
	}

	@SuppressWarnings("unused")
	@Deprecated
	private static String retrieveXmlWikiPage(String wikiPage)
	{
		try {
			URL url = new URL(m_urlBase + URLEncoder.encode(wikiPage, "UTF-8"));
			System.out.println(url.toString());
			BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8"));
			String xml = Misc.readBufferToString(in);
	        in.close();
	        return getContentFromXml(xml);
        } catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Deprecated
	private static String getContentFromXml(String xml)
	{
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			InputSource is = new InputSource(new StringReader(xml));
			Document doc = db.parse(is);
			doc.getDocumentElement().normalize();
			NodeList nodeList = doc.getElementsByTagName("rev");
			if(nodeList.getLength() == 0) {
				return null;
			}
			else {
				return nodeList.item(0).getFirstChild().getNodeValue();
			}
		} catch (ParserConfigurationException e1) {
			e1.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}
