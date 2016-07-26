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

import offeneBibel.osisExporter.OsisGeneratorVisitor.NoteIndexCounter;
import util.Misc;

// generate MySword .SQLite files
public class MySwordConverter {

    public static void main(String[] args) throws Exception {
        convert("offeneBibelStudienfassungZefaniaMitHtmlFußnoten.xml");
        convert("offeneBibelLesefassungZefaniaMitHtmlFußnoten.xml");
    }

    private static final Pattern xrefPattern = Pattern.compile("([A-Za-z0-9]+) ([0-9]+), ([0-9]+)");

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
        String date = xpath.evaluate("/XMLBIBLE/INFORMATION/date/text()", zefDoc);

        File outFile = new File(Misc.getResultsDir(), identifier + ".bbl.mybible");
        outFile.delete();
        SqlJetDb db = SqlJetDb.open(outFile, true);
        db.getOptions().setAutovacuum(true);
        db.beginTransaction(SqlJetTransactionMode.WRITE);
        db.getOptions().setUserVersion(0);
        db.createTable("CREATE TABLE Details (Title NVARCHAR(255), Description TEXT, Abbreviation NVARCHAR(50), Comments TEXT, Version TEXT, VersionDate DATETIME, PublishDate DATETIME, Publisher TEXT, Author TEXT, Creator TEXT, Source TEXT, EditorialComments TEXT, Language NVARCHAR(3), RightToLeft BOOL, OT BOOL, NT BOOL, Strong BOOL, VerseRules TEXT)");
        db.createTable("CREATE TABLE Bible (Book INT, Chapter INT, Verse INT, Scripture TEXT)");
        db.createIndex("CREATE UNIQUE INDEX bible_key ON Bible (Book ASC, Chapter ASC, Verse ASC)");
        ISqlJetTable detailsTable = db.getTable("Details");
        ISqlJetTable bibleTable = db.getTable("bible");
        detailsTable.insert(title, description, identifier, "Lizenz: " + rights, date.replace("-", "."), date, date, "www.offene-bibel.de", null, null, null, null, "deu", "0", "1", "1", "0", "");
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
            int bnumber = Integer.parseInt(bookElement.getAttribute("bnumber"));
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
                footnoteTextCounter.reset();
                StringBuilder captions = new StringBuilder();
                for (Node verseNode = chapterElement.getFirstChild(); verseNode != null; verseNode = verseNode.getNextSibling()) {
                    if (verseNode instanceof Text)
                        continue;
                    Element verseElement = (Element) verseNode;
                    if (verseElement.getNodeName().equals("PROLOG")) {
                        continue;
                    } else if (verseElement.getNodeName().equals("CAPTION")) {
                        captions.append("<TS>" + escape(verseElement.getTextContent()) + "<Ts>");
                        continue;
                    }
                    if (!verseElement.getNodeName().equals("VERS"))
                        throw new IOException(verseElement.getNodeName());
                    if (verseElement.getFirstChild() == null)
                        continue;
                    int vnumber = Integer.parseInt(verseElement.getAttribute("vnumber"));
                    bibleTable.insert(bnumber, cnumber, vnumber, captions.toString() + parseVerse(verseElement));
                    captions.setLength(0);
                }
            }
        }
        db.commit();
        db.close();
    }

    private static String parseVerse(Element verseElement) throws SqlJetException {
        StringBuilder verse = new StringBuilder();
        for (Node node = verseElement.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Text) {
                verse.append(escape(node.getTextContent()));
            } else {
                Element elem = (Element) node;
                if (elem.getNodeName().equals("NOTE")) {
                    String txt = elem.getTextContent(), content;
                    if (txt.startsWith("<html>"))
                        content = txt.substring(6);
                    else
                        content = escape(txt);
                    final String footnoteSymbol;
                    if (content.startsWith("[St") && content.endsWith("]")) {
                        footnoteSymbol = "Status";
                    } else {
                        footnoteSymbol = footnoteTextCounter.getNextNoteString();
                    }
                    verse.append("<RF q=" + footnoteSymbol + ">" + content + "<Rf>");
                } else if (elem.getNodeName().equals("BR")) {
                    verse.append("<CM>");
                } else if (elem.getNodeName().equals("XREF")) {
                    for (String ref : elem.getAttribute("fscope").split("; ")) {
                        Matcher m = xrefPattern.matcher(ref);
                        if (!m.matches())
                            throw new IllegalStateException("Invalid XREF: " + ref);
                        String book = m.group(1);
                        int bnumber = Arrays.asList(ZEF_SHORT_BOOKS).indexOf(book);
                        if (bnumber == -1) {
                            throw new IllegalStateException("Invalid book: " + book);
                        }
                        verse.append("<RX" + bnumber + "." + m.group(2) + "." + m.group(3) + ">");
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
                content.append("<span style=\"font-weight: bold; color:gray;\">" + prefixBracket + "</span>");
            }
            if (!css.contains("zef-hoist-after")) {
                Text text = (Text) styleElement.getLastChild();
                String suffixBracket = "]";
                if (!text.getNodeValue().endsWith(suffixBracket))
                    suffixBracket = "] ";
                if (!text.getNodeValue().endsWith(suffixBracket))
                    throw new IllegalStateException("Missing bracket at end of addition.");
                text.setNodeValue(text.getNodeValue().substring(0, text.getNodeValue().length() - suffixBracket.length()));
                suffix += "<span style=\"font-weight: bold; color:gray;\">" + suffixBracket + "</span>";
            }
            css = "osis-style:added;";
        }
        content.append("<span style=\"" + css + "\">");
        for (Node node = styleElement.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Text) {
                content.append(escape(node.getTextContent()));
            } else {
                Element elem = (Element) node;
                if (elem.getNodeName().equals("BR")) {
                    content.append("<CM>");
                } else if (elem.getNodeName().equals("STYLE")) {
                    parseStyle(content, elem);
                } else {
                    throw new IllegalStateException("invalid STYLE level tag: " + elem.getNodeName());
                }
            }
        }
        content.append(suffix);
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static NoteIndexCounter footnoteTextCounter = new NoteIndexCounter();
}
