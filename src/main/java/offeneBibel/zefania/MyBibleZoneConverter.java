package offeneBibel.zefania;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;

import org.tmatesoft.sqljet.core.SqlJetException;
import org.tmatesoft.sqljet.core.SqlJetTransactionMode;
import org.tmatesoft.sqljet.core.table.ISqlJetTable;
import org.tmatesoft.sqljet.core.table.SqlJetDb;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import offeneBibel.osisExporter.ObOsisGeneratorVisitor.NoteIndexCounter;
import util.Misc;

// generate MyBible.Zone .SQLite files
public class MyBibleZoneConverter {

    public static void main(String[] args) throws Exception {
        convert("offeneBibelStudienfassungZefaniaMitHtmlFußnoten.xml");
        convert("offeneBibelLesefassungZefaniaMitHtmlFußnoten.xml");
    }

    private static final Pattern xrefPattern = Pattern.compile("([A-Za-z0-9]+) ([0-9]+), ([0-9]+)");

    private static int[] BOOK_NUMBERS = new int[] {
            0, //
            10, 20, 30, 40, 50, 60, 70, 80, 90, 100, // 10
            110, 120, 130, 140, 150, 160, 190, 220, 230, 240, // 20
            250, 260, 290, 300, 310, 330, 340, 350, 360, 370, // 30
            380, 390, 400, 410, 420, 430, 440, 450, 460, 470, // 40
            480, 490, 500, 510, 520, 530, 540, 550, 560, 570, // 50
            580, 590, 600, 610, 620, 630, 640, 650, 660, 670, // 60
            680, 690, 700, 710, 720, 730, 180, 270, 170, 280, // 70
            320, 462, 464, 900, 910, 790, 466, 0, 315, 468, // 80
            165, // 81
    };

    private static String[] BOOK_COLORS = new String[] {
            null, // 0
            "#ccccff", "#ccccff", "#ccccff", "#ccccff", "#ccccff", "#ffcc99", "#ffcc99", "#ffcc99", "#ffcc99", "#ffcc99", // 10
            "#ffcc99", "#ffcc99", "#ffcc99", "#ffcc99", "#ffcc99", "#ffcc99", "#ffcc99", "#66ff99", "#66ff99", "#66ff99", // 20
            "#66ff99", "#66ff99", "#ff9fb4", "#ff9fb4", "#ff9fb4", "#ff9fb4", "#ff9fb4", "#ffff99", "#ffff99", "#ffff99", // 30
            "#ffff99", "#ffff99", "#ffff99", "#ffff99", "#ffff99", "#ffff99", "#ffff99", "#ffff99", "#ffff99", "#ff6600", // 40
            "#ff6600", "#ff6600", "#ff6600", "#00ffff", "#ffff00", "#ffff00", "#ffff00", "#ffff00", "#ffff00", "#ffff00", // 50
            "#ffff00", "#ffff00", "#ffff00", "#ffff00", "#ffff00", "#ffff00", "#ffff00", "#ffff00", "#00ff00", "#00ff00", // 60
            "#00ff00", "#00ff00", "#00ff00", "#00ff00", "#00ff00", "#ff7c80", "#ffcc99", "#66ff99", "#ffcc99", "#66ff99", // 70
            "#ff9fb4", "#d3d3d3", "#d3d3d3", "#66ff99", "#66ff99", "#66ff99", "#d3d3d3", null, "#ff9fb4", "#d3d3d3", // 80
            "#ffcc99", // 81
    };

    private static String[] ZEF_SHORT_BOOKS = new String[] {
            null,
            "Gen", "Exo", "Lev", "Num", "Deu", "Jos", "Jdg", "Rth", "1Sa", "2Sa",
            "1Ki", "2Ki", "1Ch", "2Ch", "Ezr", "Neh", "Est", "Job", "Psa", "Pro",
            "Ecc", "Son", "Isa", "Jer", "Lam", "Eze", "Dan", "Hos", "Joe", "Amo",
            "Oba", "Jon", "Mic", "Nah", "Hab", "Zep", "Hag", "Zec", "Mal", "Mat",
            "Mar", "Luk", "Joh", "Act", "Rom", "1Co", "2Co", "Gal", "Eph", "Php",
            "Col", "1Th", "2Th", "1Ti", "2Ti", "Tit", "Phm", "Heb", "Jas", "1Pe",
            "2Pe", "1Jn", "2Jn", "3Jn", "Jud", "Rev", "Jdt", "Wis", "Tob", "Sir",
            "Bar", "1Ma", "2Ma", "xDa", "xEs", "Man", "3Ma", "4Ma", "EpJ", "1Es",
            "2Es",
    };

    private static void convert(String zefFile) throws Exception {
        DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        XPath xpath = javax.xml.xpath.XPathFactory.newInstance().newXPath();
        Document zefDoc = docBuilder.parse(new File(Misc.getResultsDir(), zefFile));
        String title = xpath.evaluate("/XMLBIBLE/INFORMATION/title/text()", zefDoc);
        String description = xpath.evaluate("/XMLBIBLE/INFORMATION/description/text()", zefDoc);
        String identifier = xpath.evaluate("/XMLBIBLE/INFORMATION/identifier/text()", zefDoc).replace("HtmlFn", "");
        String rights = xpath.evaluate("/XMLBIBLE/INFORMATION/rights/text()", zefDoc);

        File outFile = new File(Misc.getResultsDir(), identifier + ".SQLite3");
        outFile.delete();
        SqlJetDb db = SqlJetDb.open(outFile, true);
        db.getOptions().setAutovacuum(true);
        db.beginTransaction(SqlJetTransactionMode.WRITE);
        db.getOptions().setUserVersion(0);
        db.createTable("CREATE TABLE info (name TEXT, value TEXT)");
        db.createTable("CREATE TABLE books (book_number NUMERIC, book_color TEXT, short_name TEXT, long_name TEXT)");
        db.createTable("CREATE TABLE introductions (book_number NUMERIC, introduction TEXT)");
        db.createIndex("CREATE UNIQUE INDEX introductions_index on introductions(book_number)");
        db.createTable("CREATE TABLE verses (book_number INTEGER, chapter INTEGER, verse INTEGER, text TEXT)");
        db.createIndex("CREATE UNIQUE INDEX verses_index on verses (book_number, chapter, verse)");
        db.createTable("CREATE TABLE stories (book_number NUMERIC, chapter NUMERIC, verse NUMERIC, order_if_several NUMERIC, title TEXT)");
        db.createIndex("CREATE UNIQUE INDEX stories_index on stories(book_number, chapter, verse, order_if_several)");

        File commentaryFile = new File(Misc.getResultsDir(), identifier + ".commentaries.SQLite3");
        commentaryFile.delete();
        SqlJetDb cdb = SqlJetDb.open(commentaryFile, true);
        cdb.getOptions().setAutovacuum(true);
        cdb.beginTransaction(SqlJetTransactionMode.WRITE);
        cdb.getOptions().setUserVersion(0);
        cdb.createTable("CREATE TABLE info (name TEXT, value TEXT)");
        cdb.createTable("CREATE TABLE commentaries (book_number NUMERIC, chapter_number_from NUMERIC, verse_number_from NUMERIC, chapter_number_to NUMERIC, verse_number_to NUMERIC, marker TEXT, text TEXT )");
        cdb.createIndex("CREATE INDEX commentaries_index on commentaries(book_number, chapter_number_from, verse_number_from)");

        ISqlJetTable infoTable = db.getTable("info");
        ISqlJetTable booksTable = db.getTable("books");
        ISqlJetTable introductionsTable = db.getTable("introductions");
        ISqlJetTable versesTable = db.getTable("verses");
        ISqlJetTable storiesTable = db.getTable("stories");
        ISqlJetTable cInfoTable = cdb.getTable("info");
        ISqlJetTable footnotesTable = cdb.getTable("commentaries");

        infoTable.insert("language", "de");
        infoTable.insert("description", title);
        infoTable.insert("detailed_info", description + "<br />Lizenz: " + rights);
        infoTable.insert("russian_numbering", "false");
        infoTable.insert("chapter_string", "Kapitel");
        infoTable.insert("introduction_string", "Einleitung");
        infoTable.insert("strong_numbers", "false");
        infoTable.insert("right_to_left", "false");
        infoTable.insert("digits0-9", "0123456789");
        infoTable.insert("swaps_non_localized_words_in_mixed_language_line", "false");
        infoTable.insert("localized_book_abbreviations", "false");
        infoTable.insert("font_scale", "1.0");
        infoTable.insert("contains_accents", "true");
        cInfoTable.insert("language", "de");
        cInfoTable.insert("description", title);
        cInfoTable.insert("russian_numbering", "false");
        cInfoTable.insert("is_footnotes", "true");

        for (Node bookNode = zefDoc.getDocumentElement().getFirstChild().getNextSibling(); bookNode != null; bookNode = bookNode.getNextSibling()) {
            if (bookNode instanceof Text) {
                if (bookNode.getTextContent().trim().length() > 0)
                    throw new IOException();
                continue;
            }
            Element bookElement = (Element) bookNode;
            if (bookElement.getNodeName().equals("INFORMATION"))
                continue;
            if (!bookElement.getNodeName().equals("BIBLEBOOK"))
                throw new IOException(bookElement.getNodeName());
            int bnum = BOOK_NUMBERS[Integer.parseInt(bookElement.getAttribute("bnumber"))];
            if (bnum == 0)
                continue;
            String bcol = BOOK_COLORS[Integer.parseInt(bookElement.getAttribute("bnumber"))];
            booksTable.insert(bnum, bcol, bookElement.getAttribute("bsname"), bookElement.getAttribute("bname"));
            StringBuilder prologs = new StringBuilder();
            for (Node chapterNode = bookNode.getFirstChild(); chapterNode != null; chapterNode = chapterNode.getNextSibling()) {
                if (chapterNode instanceof Text) {
                    if (chapterNode.getTextContent().trim().length() > 0)
                        throw new IOException();
                    continue;
                }
                Element chapterElement = (Element) chapterNode;
                if (!chapterElement.getNodeName().equals("CHAPTER"))
                    throw new IOException(chapterElement.getNodeName());
                int cnumber = Integer.parseInt(chapterElement.getAttribute("cnumber"));
                xrefFootnoteCounter = 0;
                footnoteTextCounter.reset();

                for (Node verseNode = chapterElement.getFirstChild(); verseNode != null; verseNode = verseNode.getNextSibling()) {
                    if (verseNode instanceof Text)
                        continue;
                    Element verseElement = (Element) verseNode;
                    if (verseElement.getNodeName().equals("PROLOG")) {
                        prologs.append("<h1>" + (cnumber == 1 ? bookElement.getAttribute("bname") : "" + cnumber) + "</h1>");
                        for (Node node = verseElement.getFirstChild(); node != null; node = node.getNextSibling()) {
                            if (node instanceof Text) {
                                String txt = ((Text) node).getTextContent();
                                if (txt.startsWith("<html>"))
                                    txt = txt.substring(6).replaceAll("[ \t\r\n]+", " ");
                                else
                                    txt = txt.replace("&", "&amp").replace("<", "&lt;").replace(">", "&gt;").replaceAll("[ \t\r\n]+", " ");
                                prologs.append(txt);
                            } else {
                                Element elem = (Element) node;
                                if (elem.getNodeName().equals("STYLE")) {
                                    String content = elem.getTextContent();
                                    prologs.append(content);
                                } else if (elem.getNodeName().equals("BR")) {
                                    prologs.append("<br />");
                                } else {
                                    throw new IllegalStateException("invalid prolog tag: " + elem.getNodeName());
                                }
                            }
                        }
                        continue;
                    } else if (verseElement.getNodeName().equals("CAPTION")) {
                        int vref = Integer.parseInt(verseElement.getAttribute("vref"));
                        storiesTable.insert(bnum, cnumber, vref, 0, verseElement.getTextContent());
                        continue;
                    }
                    if (!verseElement.getNodeName().equals("VERS"))
                        throw new IOException(verseElement.getNodeName());
                    if (verseElement.getFirstChild() == null)
                        continue;
                    int vnumber = Integer.parseInt(verseElement.getAttribute("vnumber"));
                    versesTable.insert(bnum, cnumber, vnumber, parseVerse(verseElement, footnotesTable, new int[] { bnum, cnumber, vnumber }));
                }
            }
            if (prologs.length() > 0) {
                introductionsTable.insert(bnum, prologs.toString());
            }
        }
        db.commit();
        db.close();
        cdb.commit();
        cdb.close();
    }

    private static String parseVerse(Element verseElement, ISqlJetTable footnotesTable, int[] bcv) throws SqlJetException {
        StringBuilder verse = new StringBuilder();
        for (Node node = verseElement.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Text) {
                verse.append(node.getTextContent());
            } else {
                Element elem = (Element) node;
                if (elem.getNodeName().equals("NOTE")) {
                    String txt = elem.getTextContent(), content;
                    if (txt.startsWith("<html>"))
                        content = txt.substring(6);
                    else
                        content = txt.replace("&", "&amp").replace("<", "&lt;").replace(">", "&gt;");
                    verse.append(buildFootnote(content, footnotesTable, bcv));
                } else if (elem.getNodeName().equals("BR")) {
                    verse.append("<br/>");
                } else if (elem.getNodeName().equals("XREF")) {
                    for (String ref : elem.getAttribute("fscope").split("; ")) {
                        Matcher m = xrefPattern.matcher(ref);
                        if (!m.matches())
                            throw new IllegalStateException("Invalid XREF: " + ref);
                        String book = m.group(1);
                        int bookIdx = Arrays.asList(ZEF_SHORT_BOOKS).indexOf(book);
                        if (bookIdx == -1) {
                            throw new IllegalStateException("Invalid book: " + book);
                        }
                        int bnum = BOOK_NUMBERS[bookIdx];
                        String xref = "<a href=\"B:" + bnum + " " + m.group(2) + ":" + m.group(3) + "\">" + ref + "</a>";
                        final String footnoteSymbol = "[℘" + (++xrefFootnoteCounter) + "]";
                        footnotesTable.insert(bcv[0], bcv[1], bcv[2], bcv[1], bcv[2], footnoteSymbol, xref);
                        verse.append("<f>" + footnoteSymbol + "</f>");
                    }
                } else if (elem.getNodeName().equals("STYLE")) {
                    parseStyle(verse, elem);
                } else {
                    throw new IllegalStateException("invalid verse level tag: " + elem.getNodeName());
                }
            }
        }
        return verse.toString();
    }

    private static void parseStyle(StringBuilder content, Element styleElement) {
        String css = styleElement.getAttribute("css");
        String suffix = "</span>";
        if (css.contains("osis-style: added;")) {
            if (!css.contains("zef-hoist-before")) {
                Text text = (Text) styleElement.getFirstChild();
                String prefixBracket = "[";
                if (!text.getNodeValue().startsWith(prefixBracket))
                    prefixBracket = " [";
                if (!text.getNodeValue().startsWith(prefixBracket))
                    throw new IllegalStateException("Missing bracket at start of addition.");
                text.setNodeValue(text.getNodeValue().substring(prefixBracket.length()));
                content.append("<span style=\"font-weight: bold; color:gray;\"><i>" + prefixBracket + "</i></span>");
            }
            if (!css.contains("zef-hoist-after")) {
                Text text = (Text) styleElement.getLastChild();
                String suffixBracket = "]";
                if (!text.getNodeValue().endsWith(suffixBracket))
                    suffixBracket = "] ";
                if (!text.getNodeValue().endsWith(suffixBracket))
                    throw new IllegalStateException("Missing bracket at end of addition.");
                text.setNodeValue(text.getNodeValue().substring(0, text.getNodeValue().length() - suffixBracket.length()));
                suffix += "<span style=\"font-weight: bold; color:gray;\"><i>" + suffixBracket + "</i></span>";
            }
            css = "osis-style:added;";
        }
        content.append("<span style=\"" + css + "\">");
        if (css.contains("color: gray;")) {
            content.append("<i>");
            suffix = "</i>" + suffix;
        }
        for (Node node = styleElement.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Text) {
                content.append(node.getTextContent());
            } else {
                Element elem = (Element) node;
                if (elem.getNodeName().equals("BR")) {
                    content.append("<br/>");
                } else if (elem.getNodeName().equals("STYLE")) {
                    parseStyle(content, elem);
                } else {
                    throw new IllegalStateException("invalid STYLE level tag: " + elem.getNodeName());
                }
            }
        }
        content.append(suffix);
    }

    private static int xrefFootnoteCounter = 0;
    private static NoteIndexCounter footnoteTextCounter = new NoteIndexCounter();

    private static String buildFootnote(String content, ISqlJetTable footnotesTable, int[] bcv) throws SqlJetException {
        final String footnoteSymbol;
        if (content.startsWith("[St") && content.endsWith("]")) {
            footnoteSymbol = "[Status]";
        } else {
            footnoteSymbol = "[" + footnoteTextCounter.getNextNoteString() + "]";
        }
        footnotesTable.insert(bcv[0], bcv[1], bcv[2], bcv[1], bcv[2], footnoteSymbol, content);
        return "<f>" + footnoteSymbol + "</f>";
    }
}
