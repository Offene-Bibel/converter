/* Copyright (C) 2012-2015 Patrick Zimmermann, Michael Schierl,
 * Stephan Kreutzer
 *
 * This file is part of converter.
 *
 * converter is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3
 * as published by the Free Software Foundation.
 *
 * converter is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License 3 for more details.
 *
 * You should have received a copy of the GNU General Public License 3
 * along with converter.  If not, see <http://www.gnu.org/licenses/>.
 */

package offeneBibel.osisExporter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import offeneBibel.parser.BookNameHelper;
import offeneBibel.parser.ObAstFixuper;
import offeneBibel.parser.ObAstNode;
import offeneBibel.parser.ObVerseStatus;
import offeneBibel.parser.OffeneBibelParser;

import org.parboiled.Parboiled;
import org.parboiled.common.Base64;
import org.parboiled.errors.ErrorUtils;
import org.parboiled.parserunners.BasicParseRunner;
import org.parboiled.parserunners.ParseRunner;
import org.parboiled.parserunners.RecoveringParseRunner;
import org.parboiled.parserunners.ReportingParseRunner;
import org.parboiled.parserunners.TracingParseRunner;
import org.parboiled.support.ParsingResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import util.Misc;

import com.beust.jcommander.JCommander;

public class Exporter
{
    /**
     * URL prefix to use for retrieving the translation pages.
     */
    //static final String m_urlBase = "http://www.offene-bibel.de/wiki/api.php?action=query&prop=revisions&rvprop=content&format=xml&titles=";
    static final String m_urlBase = "http://www.offene-bibel.de/wiki/index.php5?action=raw&title=";

    /**
     * URL prefix to use for retrieving history of a page.
     */
    static final String m_historyURLBase = "http://www.offene-bibel.de/wiki/api.php5?action=query&prop=revisions&rvprop=ids|timestamp&rvlimit=100&format=xml&titles=";

    /**
     * Oldest date for a history page to be considered to be retrieved. Increase it when parsing succeeded to find errors faster.
     */
    static final String m_minHistoryDate = "2015-01-01";

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

    static final String m_studienFassungConfigFilename = "offbist.conf";
    static final String m_leseFassungConfigFilename = "offbile.conf";

    CommandLineArguments m_commandLineArguments;

    class Chapter {
        Book book;
        int number;
        /** Text of this chapter as retrieved from the wiki. */
        String wikiText = null;
        /** Result of the parsing of the text. */
        ObAstNode node = null;

        /** The following is used by the {@link generateOsisChapterFragments} method. */
        String studienfassungText = null;
        String lesefassungText = null;

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
            String wikiPage = book.urlName + "_" + number;
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

        public boolean generateAst(OffeneBibelParser parser, BasicParseRunner<ObAstNode> parseRunner) throws Throwable {
            if(wikiText != null) {
                File cacheFile = null;
                if (m_commandLineArguments.m_cacheAST) {
                    MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
                    sha1.update(wikiText.getBytes("UTF-8"));
                    cacheFile = new File(Misc.getPageCacheDir() + ".." + File.separator + "asts" + File.separator + Base64.custom().encodeToString(sha1.digest(), false) + ".ast");
                    if (cacheFile.exists()) {
                        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(cacheFile))) {
                            node = (ObAstNode) ois.readObject();
                        }
                        return true;
                    }
                }

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
                    node = result.resultValue;
                    ObAstFixuper.fixupAstTree(node);
                    node.host(new EmptyVerseFixupVisitor());
                    if (cacheFile != null) {
                        cacheFile.getParentFile().mkdirs();
                        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(cacheFile))) {
                            oos.writeObject(node);
                        }
                    }
                }
            }
            return true;
        }
    }

    class Book {
        /** Name of the book corresponding to the wiki page name. */
        String wikiName;
        /** Name of the book corresponding to the wiki URL. */
        String urlName;
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

            System.out.print("Retrieving wiki pages...");
            List<Book> books = retrieveBooks();
            System.out.println(" Done.");

            System.out.println("Parsing wiki pages...");
            boolean success = generateAsts(books, ObVerseStatus.values()[m_commandLineArguments.m_exportLevel], !m_commandLineArguments.m_continueOnError);
            if(false == success) {
                return;
            }
            System.out.println("Done parsing wiki pages.");

            if(false == m_commandLineArguments.m_skipGenerateOsis) {
                System.out.print("Generating OSIS documents...");
                generateOsisChapterFragments(books, ObVerseStatus.values()[m_commandLineArguments.m_exportLevel]);
                String studienFassung = generateCompleteOsisString(generateOsisBookFragment(books, false), false);
                String leseFassung = generateCompleteOsisString(generateOsisBookFragment(books, true), true);
                Misc.writeFile(studienFassung, m_studienFassungFilename);
                Misc.writeFile(leseFassung, m_leseFassungFilename);
                System.out.println(" Done.");
            }

            if(m_commandLineArguments.m_generateWeb) {
                System.out.print("Generating website backing files...");
                generateWebViewerFragments(books, ObVerseStatus.values()[m_commandLineArguments.m_exportLevel]);
                System.out.println(" Done.");
            }
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
        List<String> bookFilter = new ArrayList<String>();
        if (m_commandLineArguments.m_books.length() > 0) {
            for(String bookName : m_commandLineArguments.m_books.split(",")) {
                bookFilter.add(BookNameHelper.getInstance().getUnifiedBookNameForString(bookName));
            }
        }
        List<List<String>> bookDataList = Misc.readCsv(m_bibleBooks);
        List<Book>  bookDataCollection = new Vector<Book>();
        for(List<String> bookData : bookDataList) {
            Book book = new Book();
            //Genesis,Gen,50 == german name, sword name, chapter count
            book.wikiName = bookData.get(0);
            book.urlName = book.wikiName.replaceAll(" ", "_");
            book.osisName = bookData.get(1);
            book.chapterCount = Integer.parseInt(bookData.get(2));

            if (bookFilter.size() > 0 && !bookFilter.contains(book.osisName)) {
                continue;
            }

            for(int i = 1; i <= book.chapterCount; ++i) {
                Chapter chapter = new Chapter(book, i);
                chapter.retrieveWikiPage(false);
                book.chapters.add(chapter);
            }
            bookDataCollection.add(book);
        }
        return bookDataCollection;
    }

    private boolean generateAsts(List<Book> books, ObVerseStatus requiredTranslationStatus, boolean stopOnError) throws Throwable
    {
        OffeneBibelParser parser = Parboiled.createParser(OffeneBibelParser.class);
        BasicParseRunner<ObAstNode> parseRunner = new BasicParseRunner<ObAstNode>(parser.Page());
        boolean reloadOnError = m_commandLineArguments.m_reloadOnError;
        StringBuilder errorList = new StringBuilder();
        for(Book book : books) {
            for(Chapter chapter : book.chapters) {
                boolean success = chapter.generateAst(parser, parseRunner);
                if(false == success && reloadOnError) {
                    reloadOnError = false;
                    chapter.retrieveWikiPage(true);
                    success = chapter.generateAst(parser, parseRunner);
                }
                if (false == success && m_commandLineArguments.m_tryPreviousVersions) {
                    String wikiPage = chapter.book.urlName + "_" + chapter.number;
                    List<Integer> ids = new ArrayList<Integer>();
                    String revisions = Misc.retrieveUrl(m_historyURLBase + URLEncoder.encode(wikiPage, "UTF-8"));
                    Document revisionsXML = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(revisions)));
                    NodeList revisionNodes = (NodeList) XPathFactory.newInstance().newXPath().evaluate("/api/query/pages/page/revisions/rev", revisionsXML, XPathConstants.NODESET);
                    for(int i=0; i < revisionNodes.getLength(); i++) {
                        Element revision = (Element) revisionNodes.item(i);
                        if (revision.getAttribute("timestamp").compareTo(m_minHistoryDate) < 0)
                            break;
                        ids.add(Integer.parseInt(revision.getAttribute("revid")));
                    }
                    ids.add(-1); // make sure there is an (invalid, therefore empty) revision at the end of the list
                    String fileCacheString = Misc.getPageCacheDir() + wikiPage;
                    for(int id : ids) {
                        try {
                            String result = Misc.retrieveUrl(m_urlBase + URLEncoder.encode(wikiPage, "UTF-8")+"&oldid="+id);
                            Misc.writeFile(result, fileCacheString);
                            System.out.println(wikiPage+"@"+id);
                        } catch (IOException e) {
                            Misc.writeFile("", fileCacheString);
                            System.out.println(wikiPage+"@"+id+" failed");
                        }
                        chapter.retrieveWikiPage(false);
                        success = chapter.generateAst(parser, parseRunner);
                        if (success)
                            break;
                    }
                }
                if(false == success) {
                    if(stopOnError) {
                        return false;
                    }
                    else {
                        errorList.append(book.wikiName + " " + chapter.number + "\n");
                    }
                }
            }
        }
        if(errorList.length() != 0) {
            System.out.println("The following chapters contained errors and were skipped:\n"+errorList.toString());
        }
        return true;
    }

    public void generateWebViewerFragments(List<Book> books, ObVerseStatus requiredTranslationStatus) throws Throwable
    {
        Date date = new Date();
        DateFormat format = DateFormat.getDateInstance();
        StringBuilder statusFileString = new StringBuilder("# Generated on " + format.format(date) + ".\n");
        statusFileString.append("# Export level: " + m_commandLineArguments.m_exportLevel + "\n");
        for (Book book : books) {
            for (Chapter chapter : book.chapters) {
                if (chapter.node != null) {
                    ObWebViewerVisitor visitor = new ObWebViewerVisitor(requiredTranslationStatus);
                    chapter.node.host(visitor);
                    statusFileString.append(writeWebScriptureToFile(visitor.getStudienFassung(), book, chapter, visitor.getStudienFassungQuality(), "sf"));
                    statusFileString.append(writeWebScriptureToFile(visitor.getLeseFassung(), book, chapter, visitor.getLeseFassungQuality(), "lf"));
                }
            }
        }
        FileWriter statusFileWriter = new FileWriter(Misc.getWebResultsDir() + "generated.index");
        statusFileWriter.write(statusFileString.toString());
        statusFileWriter.close();
    }

    private String writeWebScriptureToFile(String scriptureText, Book book, Chapter chapter, int quality, String type) throws IOException {
        String statusFileLine = "";
        if(scriptureText != null) {
            String filename = book.urlName + "_" + chapter.number + "_" + type;
            FileWriter writer = new FileWriter(Misc.getWebResultsDir() + filename);
            writer.write(scriptureText);
            writer.close();
            statusFileLine = book.urlName + " " + chapter.number + " " + type + " " + quality + " " + filename + "\n";
        }
        return statusFileLine;
    }

    /**
     * Takes a chapter object and generates a Studienfassung OSIS XML fragment and a Lesefassung OSIS XML fragment for it.
     * @param chapter The chapter to generate the OSIS fragments for.
     * @param requiredTranslationStatus The minimum status of the verses have to meet for inclusion.
     * @return a String array. [0] = Studienfassung or null if not yet existent, [1] = Lesefassung or null if not yet existent.
     * @throws Throwable
     */
    public String[] generateOsisTexts(Chapter chapter, ObVerseStatus requiredTranslationStatus) throws Throwable {
        String[] texts = new String[] {null, null};
        if(chapter.node != null) {
            ObOsisGeneratorVisitor visitor = new ObOsisGeneratorVisitor(chapter.number, chapter.book.osisName, requiredTranslationStatus, m_commandLineArguments.m_inlineVerseStatus, m_commandLineArguments.m_unmilestonedLineGroup);
            chapter.node.host(visitor);
            texts[0] = visitor.getStudienFassung();
            texts[1] = visitor.getLeseFassung();
        }
        return texts;
    }

    /**
     * Takes a list of {@link Book}s and generates the OSIS Studien/Lesefassung for the wiki text contained therein.
     * @param books The books for which the OSIS texts should be generated and filled out.
     * @param stopOnError Stop on the first error found. If this is false and an error is found in a chapter, that chapter is skipped.
     * @return true if parsing was successful, false otherwise.
     * @throws Throwable If the {@link ObOsisGeneratorVisitor} failed.
     */
    private void generateOsisChapterFragments(List<Book> books, ObVerseStatus requiredTranslationStatus) throws Throwable
    {
        for(Book book : books) {
            for(Chapter chapter : book.chapters) {
                String[] texts = generateOsisTexts(chapter, requiredTranslationStatus);
                chapter.studienfassungText = texts[0];
                chapter.lesefassungText = texts[1];
            }
        }
    }

    private String generateOsisBookFragment(List<Book> books, boolean leseFassung)
    {
        StringBuilder result = new StringBuilder();
        for(Book book : books) {
            boolean chapterExists = false;
            StringBuilder bookString = new StringBuilder("<div type=\"book\" osisID=\"" + book.osisName + "\" canonical=\"true\">\n<title type=\"main\">" + book.wikiName + "</title>\n");
            for(Chapter chapter : book.chapters) {
                String osisChapterText = leseFassung ? chapter.lesefassungText : chapter.studienfassungText;
                if(osisChapterText != null) { // prevent empty chapters
                    chapterExists = true;
                    bookString.append("<chapter osisID=\"" + book.osisName + "." + chapter.number + "\">\n<title type=\"chapter\">Kapitel " + chapter.number + "</title>\n");
                    bookString.append(osisChapterText);
                    bookString.append("</chapter>\n");
                }
            }
            bookString.append("</div>\n");

            if(chapterExists == true) // prevent empty books
                result.append(bookString);
        }
        return result.toString();
    }

    private String generateCompleteOsisString(String osisText, boolean leseFassung) throws IOException
    {
        String confFile = null;

        if (leseFassung == true) {
            confFile = m_leseFassungConfigFilename;
        }
        else {
            confFile = m_studienFassungConfigFilename;
        }

        Properties config = new Properties();
        try (FileInputStream fis = new FileInputStream(new File(Misc.getResourceDir(), confFile))) {
            config.load(fis);
        }

        // Properties parses as Latin-1, but the file is UTF-8. Recode
        // everything.
        for (Object prop : config.keySet()) {
            config.put(prop, new String(config.get(prop).toString().getBytes("ISO-8859-1"), "UTF-8"));
        }

        String result = Misc.readFile(leseFassung ? m_leseFassungTemplate : m_studienFassungTemplate);

        String dateString = new SimpleDateFormat("yyyy.MM.dd'T'HH.mm.ss").format(new Date());
        result = result.replace("{{date}}", "" + dateString);
        result = result.replace("{{rights}}", config.getProperty("DistributionLicense"));
        result = result.replace("{{content}}", osisText);
        return result;
    }
}
