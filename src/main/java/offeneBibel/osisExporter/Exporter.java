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
import java.util.List;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.parboiled.Parboiled;
import org.parboiled.errors.ErrorUtils;
import org.parboiled.parserunners.BasicParseRunner;
import org.parboiled.parserunners.ParseRunner;
import org.parboiled.parserunners.RecoveringParseRunner;
import org.parboiled.parserunners.ReportingParseRunner;
import org.parboiled.parserunners.TracingParseRunner;
import org.parboiled.support.ParsingResult;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.beust.jcommander.JCommander;

import offeneBibel.parser.ObAstFixuper;
import offeneBibel.parser.ObAstNode;
import offeneBibel.parser.ObVerseStatus;
import offeneBibel.parser.OffeneBibelParser;
import util.Misc;
	
public class Exporter
{
	/**
	 * URL prefix to use for retrieving the translation pages.
	 */
	//static final String m_urlBase = "http://www.offene-bibel.de/wiki/api.php?action=query&prop=revisions&rvprop=content&format=xml&titles=";
	static final String m_urlBase = "http://www.offene-bibel.de/wiki/index.php5?action=raw&title=";
	/**
	 * A list of all bible books as they are named on the wiki. 
	 * It was created by combining the wiki page: Vorlage:Kapitelzahl and the OSIS 2.1.1 manual Appendix C.1
	 */
	static final String m_bibleBooks = Misc.getResourceDir() + "bibleBooks.txt";
	//static final String m_bibleBooks = Misc.getResourceDir() + "testbibleBooks.txt";
	/**
	 * The template OSIS files. These files will be populated with the converted pages to form the final .osis document.
	 */
	static final String m_studienFassungTemplate = Misc.getResourceDir() + "offene-bibel-studienfassung_template.txt";
	static final String m_leseFassungTemplate = Misc.getResourceDir() + "offene-bibel-lesefassung_template.txt";
	/**
	 * Where the resulting .osis files should be saved.
	 */
	static final String m_studienFassungFilename = Misc.getResultsDir() + "offeneBibelStudienfassungModule.osis";
	static final String m_leseFassungFilename = Misc.getResultsDir() + "offeneBibelLesefassungModule.osis";
	
	CommandLineArguments m_commandLineArguments;
	
	class Chapter {
		int number;
		/** Text of this chapter as retrieved from the wiki. */
		String wikiText;
		/** The following two will be filled by {@link generateOsisChapterFragments}. */
		String studienfassungText;
		String lesefassungText;
	}
	
	class Book {
		/** Name of the book corresponding to the wiki page name. */
		String wikiName;
		/** Name of the book corresponding to the OSIS tag. */
		String osisName;
		/** Number of chapters in this book. */
		int chapterCount;
		Vector<Chapter> chapters = new Vector<Chapter>();
	}
	public static void main(String [] args)
	{
		Exporter exporter = new Exporter();
		exporter.run(args);
	}
	
	public void run(String [] args)
	{
		try {
			m_commandLineArguments = new CommandLineArguments();
			new JCommander(m_commandLineArguments, args);
				
			List<Book> books = retrieveBooks();
	
			generateOsisChapterFragments(books, ObVerseStatus.values()[m_commandLineArguments.m_exportLevel], true);
			String studienFassung = generateCompleteOsisString(generateOsisBookFragment(books, false), false);
			String leseFassung = generateCompleteOsisString(generateOsisBookFragment(books, true), true);
			
			Misc.writeFile(studienFassung, m_studienFassungFilename);
			Misc.writeFile(leseFassung, m_leseFassungFilename);
			System.out.println("done");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Puts together a list of {@link Book} objects with everything except of
	 * generated osis text already filled out.
	 * @return List of {@link Book}s.
	 * @throws IOException
	 */
	private List<Book> retrieveBooks() throws IOException
	{
		List<List<String>> bookDataList = Misc.readCsv(m_bibleBooks);
		List<Book>  bookDataCollection = new Vector<Book>();
		for(List<String> bookData : bookDataList) {
			Book book = new Book();
			//Genesis,Gen,50 == german name, sword name, chapter count
			book.wikiName = bookData.get(0);
			book.osisName = bookData.get(1);
			book.chapterCount = Integer.parseInt(bookData.get(2));
			
			for(int i = 1; i <= book.chapterCount; ++i) {
				String wikiPageName = book.wikiName + " " + i;
				String wikiText = retrieveWikiPage(wikiPageName);
				if(wikiText != null) {
					Chapter chapter = new Chapter();
					chapter.number = i;
					chapter.wikiText = wikiText;
					book.chapters.add(chapter);
				}
			}
			bookDataCollection.add(book);
		}
		return bookDataCollection;
	}
	
	/**
	 * Downloads a wiki page.
	 * @param wikiPage Page to download.
	 * @return Page contents in String format or null if an error occured.
	 */
	private static String retrieveWikiPage(String wikiPage)
	{
		try {
			String result = null;
			String fileCacheString = Misc.getPageCacheDir() + wikiPage;
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
		        Misc.createFolder(Misc.getPageCacheDir());
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

	/**
	 * Takes a list of {@link Book}s and generates the OSIS Studien/Lesefassung for the wiki text contained therein. 
	 * @param books The books for which the OSIS texts should be generated and filled out.
	 * @param stopOnError Stop on the first error found. If this is false and an error is found in a chapter, that chapter is skipped.
	 */
	private void generateOsisChapterFragments(List<Book> books, ObVerseStatus requiredTranslationStatus, boolean stopOnError)
	{
		OffeneBibelParser parser = Parboiled.createParser(OffeneBibelParser.class);
		BasicParseRunner<ObAstNode> parseRunner = new BasicParseRunner<ObAstNode>(parser.Page());
		
		for(Book book : books) {
			for(Chapter chapter : book.chapters) {
				ParsingResult<ObAstNode> result = parseRunner.run(chapter.wikiText);
	
				if(result.matched == false) {
					System.out.println("Book: " + book.osisName);
					System.out.println("Chapter: " + chapter.number);
					System.out.println("Error:");

					ParseRunner<ObAstNode> errorParseRunner = null;
					if(m_commandLineArguments.m_parseRunner.equalsIgnoreCase("tracing")) {
						errorParseRunner = new TracingParseRunner<ObAstNode>(parser.Page());
					}
					else if(m_commandLineArguments.m_parseRunner.equalsIgnoreCase("recovering")) {
						errorParseRunner = new RecoveringParseRunner<ObAstNode>(parser.Page());
					}
					else {
						errorParseRunner = new ReportingParseRunner<ObAstNode>(parser.Page());
					}
					ParsingResult<ObAstNode> validatorParsingResult = errorParseRunner.run(chapter.wikiText);

					if(validatorParsingResult.hasErrors()) {
						System.out.println(ErrorUtils.printParseErrors(validatorParsingResult));
						System.exit(1);
					}
					else {
						System.out.println("Validated sucessfully. This shouldn't be...");
						System.exit(0);
					}
					
					/*
					TracingParseRunner<ObAstNode> tracingParseRunner = new TracingParseRunner<ObAstNode>(parser.Page());
					ParsingResult<ObAstNode> tracingResult = tracingParseRunner.run(chapterData.getY());
					
					System.out.println("Tree:");
					String parseTreePrintOut = ParseTreeUtils.printNodeTree(tracingResult);
					System.out.println(parseTreePrintOut);
					
					System.out.println("Errors:");
					ErrorUtils.printParseErrors(tracingParseRunner.getParseErrors());
					
					System.out.println("Book: " + bookData.get("swordName"));
					System.out.println("Chapter: " + chapterData.getX());
					*/
					
					if(stopOnError) {
						return;
					}
				}
				else {
					ObAstNode node = result.resultValue;
					ObAstFixuper.fixupAstTree(node);
					
					ObAstVisitor visitor = new ObAstVisitor(chapter.number, book.osisName, requiredTranslationStatus);
					try {
						node.host(visitor);
					} catch (Throwable e) {
						e.printStackTrace();
						return;
					}
					
					chapter.studienfassungText = visitor.getStudienFassung();
					chapter.lesefassungText = visitor.getLeseFassung();
				}
			}
		}
	}

	private String generateOsisBookFragment(List<Book> books, boolean leseFassung)
	{
		String result = "";
		for(Book book : books) {
			boolean chapterExists = false;
			String bookString = "<div type=\"book\" osisID=\"" + book.osisName + "\" canonical=\"true\">\n<title type=\"main\">" + book.wikiName + "</title>\n";
			for(Chapter chapter : book.chapters) {
				String osisChapterText = leseFassung ? chapter.lesefassungText : chapter.studienfassungText;
				if(osisChapterText != null) { // prevent empty chapters
					chapterExists = true;
					bookString += "<chapter osisID=\"" + book.osisName + "." + chapter.number + "\">\n<title type=\"chapter\">Kapitel " + chapter.number + "</title>\n";
					bookString += osisChapterText;
					bookString += "</chapter>\n";
				}
			}
			bookString += "</div>\n";
			
			if(chapterExists == true) // prevent empty books
				result += bookString;
		}
		return result;
	}

	private String generateCompleteOsisString(String osisText, boolean leseFassung) throws IOException
	{
		String result = Misc.readFile(leseFassung ? m_leseFassungTemplate : m_studienFassungTemplate);
		
		result = result.replace("{{revision}}", "TODO");
		result = result.replace("{{year}}", "" + Calendar.getInstance().get(Calendar.YEAR));
		result = result.replace("{{content}}", osisText);
		return result;
	}

	@SuppressWarnings("unused")
	@Deprecated
	private String retrieveXmlWikiPage(String wikiPage)
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
	private String getContentFromXml(String xml)
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
