package offeneBibel.webViewerExporter;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.List;
import java.util.Vector;

import org.parboiled.Parboiled;
import org.parboiled.errors.ErrorUtils;
import org.parboiled.parserunners.BasicParseRunner;
import org.parboiled.parserunners.ParseRunner;
import org.parboiled.parserunners.RecoveringParseRunner;
import org.parboiled.parserunners.ReportingParseRunner;
import org.parboiled.parserunners.TracingParseRunner;
import org.parboiled.support.ParsingResult;

import com.beust.jcommander.JCommander;

import offeneBibel.osisExporter.ObWebViewerVisitor;
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
        Book book;
        int number;
        /** Text of this chapter as retrieved from the wiki. */
        String wikiText;
        /** The following two will be filled by {@link generateOsisChapterFragments}. */
        String studienfassungText;
        String lesefassungText;
        
        public Chapter(Book book, int number) {
            this.book = book;
            this.number = number;
        }
        
        /**
         * Downloads the wiki page.
         * @param wikiPage Page to download.
         */
        public void retrieveWikiPage(boolean forceDownload)
        {
            String wikiPage = book.wikiName + " " + number;
            try {
                String result = null;
                String fileCacheString = Misc.getPageCacheDir() + wikiPage;
                File fileCache = new File(fileCacheString);
                if(false == forceDownload && fileCache.exists()) {
                    if(fileCache.length() == 0)
                        return;
                    result = Misc.readFile(fileCacheString);
                }
                else {
                    try {
                        result = Misc.retrieveUrl(m_urlBase + URLEncoder.encode(wikiPage, "UTF-8"));
                        System.out.println(wikiPage);
                    } catch (IOException e) {
                        // chapter not yet created, skip
                        Misc.writeFile("", fileCacheString);
                        return;
                    }
                    Misc.createFolder(Misc.getPageCacheDir());
                    Misc.writeFile(result, fileCacheString);
                }
                wikiText = result;
            }
            catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        public boolean generateOsisTexts(OffeneBibelParser parser, BasicParseRunner<ObAstNode> parseRunner, ObVerseStatus requiredTranslationStatus) throws Throwable {
            if(wikiText != null) {
                ParsingResult<ObAstNode> result = parseRunner.run(wikiText);
                
                if(result.matched == false) {
        
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
                    ParsingResult<ObAstNode> validatorParsingResult = errorParseRunner.run(wikiText);
        
                    if(validatorParsingResult.hasErrors()) {
                        System.out.println(ErrorUtils.printParseErrors(validatorParsingResult));
                        /*
                        String parseTreePrintOut = ParseTreeUtils.printNodeTree(tracingResult);
                        ErrorUtils.printParseErrors(tracingParseRunner.getParseErrors());
                        */
                        System.out.println("=================================================");
                        System.out.println("Book: " + book.osisName);
                        System.out.println("Chapter: " + number);
                    }
                    else {
                        System.out.println("Validated sucessfully. This shouldn't be...");
                    }
                    return false;
                }
                else {
                    ObAstNode node = result.resultValue;
                    ObAstFixuper.fixupAstTree(node);
                    
                    ObWebViewerVisitor visitor = new ObWebViewerVisitor(number, book.osisName, requiredTranslationStatus);
                    try {
                        node.host(visitor);
                    } catch (Throwable e) {
                        throw e;
                    }
                    
                    studienfassungText = visitor.getStudienFassung();
                    lesefassungText = visitor.getLeseFassung();
                }
            }
            return true;
        }
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
            JCommander commander = new JCommander(m_commandLineArguments, args);
            if(m_commandLineArguments.m_help) {
                commander.usage();
                return;
            }
                
            List<Book> books = retrieveBooks();
    
            boolean success = generateOsisChapterFragments(books, ObVerseStatus.values()[m_commandLineArguments.m_exportLevel], !m_commandLineArguments.m_continueOnError);
            if(false == success) {
                return;
            }
            String studienFassung = generateCompleteOsisString(generateOsisBookFragment(books, false), false);
            String leseFassung = generateCompleteOsisString(generateOsisBookFragment(books, true), true);
            
            Misc.writeFile(studienFassung, m_studienFassungFilename);
            Misc.writeFile(leseFassung, m_leseFassungFilename);
            System.out.println("done");
        } catch (Throwable e) {
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
                Chapter chapter = new Chapter(book, i);
                chapter.retrieveWikiPage(false);
                book.chapters.add(chapter);
            }
            bookDataCollection.add(book);
        }
        return bookDataCollection;
    }

    /**
     * Takes a list of {@link Book}s and generates the OSIS Studien/Lesefassung for the wiki text contained therein.
     * @param books The books for which the OSIS texts should be generated and filled out.
     * @param stopOnError Stop on the first error found. If this is false and an error is found in a chapter, that chapter is skipped.
     * @return true if parsing was successful, false otherwise.
     * @throws Throwable If the {@link ObWebViewerVisitor} failed.
     */
    private boolean generateOsisChapterFragments(List<Book> books, ObVerseStatus requiredTranslationStatus, boolean stopOnError) throws Throwable
    {
        OffeneBibelParser parser = Parboiled.createParser(OffeneBibelParser.class);
        BasicParseRunner<ObAstNode> parseRunner = new BasicParseRunner<ObAstNode>(parser.Page());
        boolean reloadOnError = m_commandLineArguments.m_reloadOnError;
        String errorList = "";
        for(Book book : books) {
            for(Chapter chapter : book.chapters) {
                boolean success = chapter.generateOsisTexts(parser, parseRunner, requiredTranslationStatus);
                if(false == success && reloadOnError) {
                    reloadOnError = false;
                    chapter.retrieveWikiPage(true);
                    success = chapter.generateOsisTexts(parser, parseRunner, requiredTranslationStatus);
                }
                if(false == success) {
                    if(stopOnError) {
                        return false;
                    }
                    else {
                        errorList += book.wikiName + " " + chapter.number + "\n";
                    }
                }
            }
        }
        if(false == errorList.isEmpty()) {
            System.out.println("The following chapters contained errors and were skipped:\n"+errorList);
        }
        return true;
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
}
