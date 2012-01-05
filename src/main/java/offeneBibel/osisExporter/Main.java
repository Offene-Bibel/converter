package offeneBibel.osisExporter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
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
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import offeneBibel.parser.ObAstNode;
import offeneBibel.parser.OffeneBibelParser;
	
public class Main
{
	//static final String m_urlBase = "http://www.offene-bibel.de/wiki/api.php?action=query&prop=revisions&rvprop=content&format=xml&titles=";
	static final String m_urlBase = "http://www.offene-bibel.de/wiki/index.php5?action=raw&title=";
	//static final String m_bibleBooks = "resources/bibleBooks.txt";
	static final String m_bibleBooks = "resources/testbibleBooks.txt";
	static final String m_studienFassungTemplate = "resources/offene-bibel-studienfassung_template.txt";
	static final String m_leseFassungTemplate = "resources/offene-bibel-lesefassung_template.txt";
	static final String m_studienFassungFilename = "offeneBibelStudienfassungModule.osis";
	static final String m_leseFassungFilename = "offeneBibelLesefassungModule.osis";
	
	public static void main(String [] args)
	{
		try {
		List<Map<String, Object>> bibleTexts = retrieveBooks();
		createOsisTextsForChapters(bibleTexts);
		String studienFassung = constructOsisText(putOsisTextTogether(bibleTexts, false), false);
		String leseFassung = constructOsisText(putOsisTextTogether(bibleTexts, true), true);
		
		writeFile(studienFassung, m_studienFassungFilename);
		writeFile(leseFassung, m_leseFassungFilename);
		
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/*
	def retrieveAndParseWikiPage(page):
	    url = "http://www.offene-bibel.de/wiki/api.php?action=query&prop=revisions&rvprop=content&format=xml&titles=" + urllib.parse.quote(page)
	    print(url)
	    xml = urllib.request.urlopen(url).read().decode('utf-8')
	    dom = parseString(xml)
	    return dom
	*/
	private static String retrieveWikiPage(String wikiPage)
	{
		try {
			URL url = new URL(m_urlBase + URLEncoder.encode(wikiPage, "UTF-8"));
			System.out.println(url.toString());
			BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8"));
			String result = readBufferToString(in);
	        in.close();
	        return result;
        } catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private static String retrieveXmlWikiPage(String wikiPage)
	{
		try {
			URL url = new URL(m_urlBase + URLEncoder.encode(wikiPage, "UTF-8"));
			System.out.println(url.toString());
			BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8"));
			String xml = readBufferToString(in);
	        in.close();
	        return getContentFromXml(xml);
        } catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	/*
	def getContentFromDom(dom):
	    revElement = dom.getElementsByTagName("rev").item(0)
	    if revElement is None:
	        return ""
	    else:
	        return revElement.firstChild.data
	*/
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

	private static List<Map<String, Object>> retrieveBooks() throws IOException
	{
		List<List<String>> bookList = readCsv(m_bibleBooks);
		List<Map<String, Object>>  bookDataCollection = new Vector<Map<String, Object>>();
		for(List<String> book : bookList) {
			Map<String, Object> bookData = new HashMap<String, Object>();
			bookData.put("germanName", book.get(0));
			bookData.put("swordName", book.get(1));
			bookData.put("chapterCount", Integer.parseInt(book.get(2)));
			
			Map<Integer, String> chapters = new HashMap<Integer, String>();
			//Genesis,Gen,50 == german name, sword name, chapter count
			for(int i = 1; i <= Integer.parseInt(book.get(2)); ++i) {
				String wikiPageName = book.get(0) + " " + i;
				String wikiText = retrieveWikiPage(wikiPageName);
				if(wikiText != null) {
					chapters.put(i, wikiText);
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
	 	private static void createOsisTextsForChapters(List<Map<String, Object>> bibleTexts)
	{
		OffeneBibelParser parser = Parboiled.createParser(OffeneBibelParser.class);
		BasicParseRunner<ObAstNode> parseRunner = new BasicParseRunner<ObAstNode>(parser.Page());
		
		for(Map<String, Object> bookData : bibleTexts) {
			Map<Integer, String> osisLesefassungChapterTexts = new HashMap<Integer, String>();
			Map<Integer, String> osisStudienfassungChapterTexts = new HashMap<Integer, String>();
			for(Map.Entry<Integer, String> chapterData : ((Map<Integer, String>)(bookData.get("chapterTexts"))).entrySet() ) {
				ParsingResult<ObAstNode> result = parseRunner.run(chapterData.getValue());
	
				if(result.hasErrors()) {				
					TracingParseRunner<ObAstNode> tracingParseRunner = new TracingParseRunner<ObAstNode>(parser.Page());
					ParsingResult<ObAstNode> tracingResult = parseRunner.run(chapterData.getValue());
					
					System.out.println("Tree:");
					String parseTreePrintOut = ParseTreeUtils.printNodeTree(tracingResult);
					System.out.println(parseTreePrintOut);
					
					System.out.println("Errors:");
					ErrorUtils.printParseErrors(parseRunner.getParseErrors());
					
					System.out.println("Log:");
					System.out.println(tracingParseRunner.getLog());
				}
				else {
					ObAstVisitor visitor = new ObAstVisitor((Integer)bookData.get("chapterCount"), (String)bookData.get("swordName"));
					ObAstNode node = result.resultValue;
					try {
						node.host(visitor);
					} catch (Throwable e) {
						e.printStackTrace();
						return;
					}
					
	
					osisStudienfassungChapterTexts.put(chapterData.getKey(), visitor.getStudienFassung());				
					osisLesefassungChapterTexts.put(chapterData.getKey(), visitor.getLeseFassung());
				}
			}
			bookData.put("osisStudienfassungChapterTexts", osisStudienfassungChapterTexts);
			bookData.put("osisLesefassungChapterTexts", osisLesefassungChapterTexts);
		}
	}

	/*
	def writeOsisFile(templateFileName, outputFileName, content):
	    
	    templateFile = open(templateFileName)
	    template = templateFile.read()
	    templateFile.close()
	    
	    #TODO: get revision information from somewhere, possible?
	    revisionInfo = "TODO"
	    
	    #template = template.replace("{{revision}}", revisionInfo, 1)
	    template = template.replace("{{year}}", str(date.today().year), 1)
	    template = template.replace("{{content}}", content, 1)
	    
	    outputFile = open(outputFileName, 'w', -1, 'utf_8', 'strict', '\n', True)
	    outputFile.write(template)
	    outputFile.close()
	*/
	private static String constructOsisText(String osisText, boolean leseFassung) throws IOException
	{
		String result = readFile(leseFassung ? m_leseFassungTemplate : m_studienFassungTemplate);
		
		result = result.replace("{{revision}}", "TODO");
		result = result.replace("{{year}}", "" + Calendar.getInstance().get(Calendar.YEAR));
		result = result.replace("{{content}}", osisText);
		return result;
	}
	
	private static String putOsisTextTogether(List<Map<String, Object>> bibleTexts, boolean leseFassung)
	{
		String result = "";
		String studienVsLeseTag = leseFassung ? "osisLesefassungChapterTexts" : "osisStudienfassungChapterTexts";
		
		for(Map<String, Object> bookData : bibleTexts) {
			result += "<div type=\"book\" osisID=\"" + bookData.get("swordName") + "\" canonical=\"true\">\n<title type=\"main\">" + bookData.get("germanName") + "</title>\n";
			for(Map.Entry<Integer, String> chapterData : ((Map<Integer, String>)(bookData.get(studienVsLeseTag))).entrySet() ) {
				result += "<chapter osisID=\"" + bookData.get("swordName") + "." + chapterData.getKey() + "\">\n<title type=\"chapter\">Kapitel " + chapterData.getKey() + "</title>\n";
				result += chapterData.getKey();
				result += "</chapter>\n";
			}
			result += "</div>\n";
		}
		return result;
	}
	
	/*
	def retrieveBooks():
	    #bookRegex = re.compile('^\|(.+)=(\d+)$')
	    #for bookLine in bookLines:
	    #    result = bookRegex.match(bookLine)
	    #    bookNamesWithChapterCount[result.group(1)] = int(result.group(2))

	    # the following list was created by combining the wiki page: Vorlage:Kapitelzahl and the OSIS 2.1.1 manual Appendix C.1

	    
	    # this variable will be filled with the finished parsing results below
	    result = ["", ""]

	    #helper to add chapter markers
	    def addChapterMarks(verseText, book, chapter):
	        result = ""
	        if verseText:
	            chapterTag = book[1] + '.' + str(chapter)
	            result += '<chapter osisID="' + chapterTag + '">\n<title type="chapter">Kapitel ' + str(chapter)  + '</title>\n'
	            result += verseText
	            result += '</chapter>\n'
	        return result

	    for biblePart in [otBooks, ntBooks, apBooks]:
	#    for biblePart in [testBooks]:
	        #TODO: part header goes here
	        for book in biblePart:
	            print("Parsing book " + book[0] + ".")
	            #TODO: retrieve book description from book page
	            
	            bookBeginning = '<div type="book" osisID="' + book[1] + '" canonical="true">\n<title type="main">' + book[0] + '</title>\n'
	            result[0] += bookBeginning
	            result[1] += bookBeginning

	            for chapter in range(1, book[2] + 1):
	                bookDom = retrieveAndParseWikiPage(book[0] + " " + str(chapter)) # %20 = space in urls
	                bookText = getContentFromDom(bookDom)
	                verseTexts = parseBook(bookText, book[1], chapter)
	                
	                if len(verseTexts) == 2:
	                    result[0] += addChapterMarks(verseTexts[0][0], book, chapter) # bibletext of the lesefassung
	                    result[1] += addChapterMarks(verseTexts[1][0], book, chapter) # bibletext of the studienfassung

	            bookEnd = '</div>\n'
	            result[0] += bookEnd
	            result[1] += bookEnd

	            print("Done.")
	    return result
	*/
	    
	/*
		# program starts here
		#TODO: insert date in fromat: yyyy.mm.ddThh.mm.ss
	
		content = retrieveBooks()
		writeOsisFile("offene-bibel-lesefassung_template.txt", "offeneBibelLesefassungModule.osis", content[0])
		writeOsisFile("offene-bibel-studienfassung_template.txt", "offeneBibelStudienfassungModule.osis", content[1])
	*/
	
	
	
	private static void writeFile(String text, String filename) throws IOException
	{
		BufferedWriter out = new BufferedWriter(new FileWriter(filename));
		out.write(text);
		out.close();
	}

	private static String readFile(String file) throws IOException
	{
		BufferedReader reader = new BufferedReader(new FileReader(file));
		return readBufferToString(reader);
	}
	
	private static String readBufferToString(BufferedReader reader) throws IOException
	{
		String result = "";
		char[] cbuf = new char[512];
		int readCount;
		while((readCount = reader.read(cbuf)) >= 0)
			result += String.valueOf(cbuf, 0, readCount);
		return result;
	}
	
	private static List<List<String>> readCsv(String filename) throws IOException
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
}
