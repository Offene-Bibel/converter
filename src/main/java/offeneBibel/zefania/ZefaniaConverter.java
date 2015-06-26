package offeneBibel.zefania;

import util.Misc;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;

import org.w3c.dom.*;

public class ZefaniaConverter {

    public static void main(String[] args) throws Exception {
        convert("offeneBibelLesefassungModule.osis", "offbile.conf", "offeneBibelLesefassungZefania.xml", "OffBiLe");
        convert("offeneBibelStudienfassungModule.osis", "offbist.conf", "offeneBibelStudienfassungZefania.xml", "OffBiSt");
    }

    private static Map<String, String[]> zefBooks = new HashMap();

    static {
        zefBooks.put("Gen", new String[] { "1", "Genesis", "Gen" });
        zefBooks.put("Exod", new String[] { "2", "Exodus", "Exo" });
        zefBooks.put("Lev", new String[] { "3", "Leviticus", "Lev" });
        zefBooks.put("Num", new String[] { "4", "Numbers", "Num" });
        zefBooks.put("Deut", new String[] { "5", "Deuteronomy", "Deu" });
        zefBooks.put("Josh", new String[] { "6", "Joshua", "Jos" });
        zefBooks.put("Judg", new String[] { "7", "Judges", "Jdg" });
        zefBooks.put("Ruth", new String[] { "8", "Ruth", "Rth" });
        zefBooks.put("1Sam", new String[] { "9", "1 Samuel", "1Sa" });
        zefBooks.put("2Sam", new String[] { "10", "2 Samuel", "2Sa" });
        zefBooks.put("1Kgs", new String[] { "11", "1 Kings", "1Ki" });
        zefBooks.put("2Kgs", new String[] { "12", "2 Kings", "2Ki" });
        zefBooks.put("1Chr", new String[] { "13", "1 Chronicles", "1Ch" });
        zefBooks.put("2Chr", new String[] { "14", "2 Chronicles", "2Ch" });
        zefBooks.put("Ezra", new String[] { "15", "Ezra", "Ezr" });
        zefBooks.put("Neh", new String[] { "16", "Nehemiah", "Neh" });
        zefBooks.put("Esth", new String[] { "17", "Esther", "Est" });
        zefBooks.put("Job", new String[] { "18", "Job", "Job" });
        zefBooks.put("Ps", new String[] { "19", "Psalm", "Psa" });
        zefBooks.put("Prov", new String[] { "20", "Proverbs", "Pro" });
        zefBooks.put("Eccl", new String[] { "21", "Ecclesiastes", "Ecc" });
        zefBooks.put("Song", new String[] { "22", "Song of Solomon", "Son" });
        zefBooks.put("Isa", new String[] { "23", "Isaiah", "Isa" });
        zefBooks.put("Jer", new String[] { "24", "Jeremiah", "Jer" });
        zefBooks.put("Lam", new String[] { "25", "Lamentations", "Lam" });
        zefBooks.put("Ezek", new String[] { "26", "Ezekiel", "Eze" });
        zefBooks.put("Dan", new String[] { "27", "Daniel", "Dan" });
        zefBooks.put("Hos", new String[] { "28", "Hosea", "Hos" });
        zefBooks.put("Joel", new String[] { "29", "Joel", "Joe" });
        zefBooks.put("Amos", new String[] { "30", "Amos", "Amo" });
        zefBooks.put("Obad", new String[] { "31", "Obadiah", "Oba" });
        zefBooks.put("Jonah", new String[] { "32", "Jonah", "Jon" });
        zefBooks.put("Mic", new String[] { "33", "Micah", "Mic" });
        zefBooks.put("Nah", new String[] { "34", "Nahum", "Nah" });
        zefBooks.put("Hab", new String[] { "35", "Habakkuk", "Hab" });
        zefBooks.put("Zeph", new String[] { "36", "Zephaniah", "Zep" });
        zefBooks.put("Hag", new String[] { "37", "Haggai", "Hag" });
        zefBooks.put("Zech", new String[] { "38", "Zechariah", "Zec" });
        zefBooks.put("Mal", new String[] { "39", "Malachi", "Mal" });
        zefBooks.put("Matt", new String[] { "40", "Matthew", "Mat" });
        zefBooks.put("Mark", new String[] { "41", "Mark", "Mar" });
        zefBooks.put("Luke", new String[] { "42", "Luke", "Luk" });
        zefBooks.put("John", new String[] { "43", "John", "Joh" });
        zefBooks.put("Acts", new String[] { "44", "Acts", "Act" });
        zefBooks.put("Rom", new String[] { "45", "Romans", "Rom" });
        zefBooks.put("1Cor", new String[] { "46", "1 Corinthians", "1Co" });
        zefBooks.put("2Cor", new String[] { "47", "2 Corinthians", "2Co" });
        zefBooks.put("Gal", new String[] { "48", "Galatians", "Gal" });
        zefBooks.put("Eph", new String[] { "49", "Ephesians", "Eph" });
        zefBooks.put("Phil", new String[] { "50", "Philippians", "Php" });
        zefBooks.put("Col", new String[] { "51", "Colossians", "Col" });
        zefBooks.put("1Thess", new String[] { "52", "1 Thessalonians", "1Th" });
        zefBooks.put("2Thess", new String[] { "53", "2 Thessalonians", "2Th" });
        zefBooks.put("1Tim", new String[] { "54", "1 Timothy", "1Ti" });
        zefBooks.put("2Tim", new String[] { "55", "2 Timothy", "2Ti" });
        zefBooks.put("Titus", new String[] { "56", "Titus", "Tit" });
        zefBooks.put("Phlm", new String[] { "57", "Philemon", "Phm" });
        zefBooks.put("Heb", new String[] { "58", "Hebrews", "Heb" });
        zefBooks.put("Jas", new String[] { "59", "James", "Jas" });
        zefBooks.put("1Pet", new String[] { "60", "1 Peter", "1Pe" });
        zefBooks.put("2Pet", new String[] { "61", "2 Peter", "2Pe" });
        zefBooks.put("1John", new String[] { "62", "1 John", "1Jn" });
        zefBooks.put("2John", new String[] { "63", "2 John", "2Jn" });
        zefBooks.put("3John", new String[] { "64", "3 John", "3Jn" });
        zefBooks.put("Jude", new String[] { "65", "Jude", "Jud" });
        zefBooks.put("Rev", new String[] { "66", "Revelation", "Rev" });
        zefBooks.put("Jdt", new String[] { "67", "Judit", "Jdt" });
        zefBooks.put("Wis", new String[] { "68", "Wisdom", "Wis" });
        zefBooks.put("Tob", new String[] { "69", "Tobit", "Tob" });
        zefBooks.put("Sir", new String[] { "70", "Sirach", "Sir" });
        zefBooks.put("Bar", new String[] { "71", "Baruch", "Bar" });
        zefBooks.put("1Macc", new String[] { "72", "1 Maccabees", "1Ma" });
        zefBooks.put("2Macc", new String[] { "73", "2 Maccabees", "2Ma" });
        // 74 = xDan
        // 75 = xEst
        // 76 = Man
        // 77 = 3Macc
        // 78 = 4Macc
        // 79 = EpJer
        zefBooks.put("1Esd", new String[] { "80", "1 Esdras", "1Es" });
        zefBooks.put("2Esd", new String[] { "81", "2 Esdras", "2Es" });
    }

    private static void convert(String osisFile, String confFile, String outFile, String identifier) throws Exception {
        Properties config = new Properties();
        try (FileInputStream fis = new FileInputStream(new File(Misc.getResourceDir(), confFile))) {
            config.load(fis);
        }
        // Properties parses as Latin-1, but the file is UTF-8. Recode
        // everything.
        for (Object prop : config.keySet()) {
            config.put(prop, new String(config.get(prop).toString().getBytes("ISO-8859-1"), "UTF-8"));
        }
        DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        XPath xpath = javax.xml.xpath.XPathFactory.newInstance().newXPath();
        Document osisDoc = docBuilder.parse(new File(Misc.getResultsDir(), osisFile));
        String date = xpath.evaluate("/osis/osisText/header/revisionDesc/date/text()", osisDoc).substring(0, 10).replace('.', '-');
        Document doc = docBuilder.newDocument();
        doc.setXmlStandalone(true);
        doc.appendChild(doc.createElement("XMLBIBLE"));
        Element root = doc.getDocumentElement();
        root.setAttribute("version", date.replace('-','.').replace(".0","."));
        root.setAttributeNS("http://www.w3.org/2001/XMLSchema-instance", "noNamespaceSchemaLocation", "zef2005.xsd");
        root.setAttribute("biblename", config.getProperty("Description"));
        root.setAttribute("type", "x-bible");
        root.setAttribute("status", "v");
        root.setAttribute("revision", date.replace("-",""));
        Element info = doc.createElement("INFORMATION");
        root.appendChild(info);
        appendTextChild(info, "title", config.getProperty("Description"));
        appendTextChild(info, "creator", "offene-bibel-converter");
        info.appendChild(doc.createElement("subject"));
        appendTextChild(info, "description", config.getProperty("About") + " [" + date + "]");
        info.appendChild(doc.createElement("publisher"));
        info.appendChild(doc.createElement("contributors"));
        appendTextChild(info, "date", date);
        info.appendChild(doc.createElement("type"));
        appendTextChild(info, "format", "Zefania XML Bible Markup Language");
        appendTextChild(info, "identifier", identifier);
        appendTextChild(info, "source", config.getProperty("TextSource"));
        appendTextChild(info, "language", "ger");
        appendTextChild(info, "coverage", "provide the bible to the nations of the world");
        appendTextChild(info, "rights", config.getProperty("DistributionLicense"));

        NodeList osisBooks = (NodeList) xpath.evaluate("/osis/osisText/div[@type='book']", osisDoc, XPathConstants.NODESET);
        for (int bookIndex = 0; bookIndex < osisBooks.getLength(); bookIndex++) {
            Element osisBook = (Element) osisBooks.item(bookIndex);
            String bookOsisID = osisBook.getAttribute("osisID");
            String[] bookZefName = zefBooks.get(bookOsisID);
            Element bibleBook = doc.createElement("BIBLEBOOK");
            root.appendChild(bibleBook);
            bibleBook.setAttribute("bnumber", bookZefName[0]);
            bibleBook.setAttribute("bname", bookZefName[1]);
            bibleBook.setAttribute("bsname", bookZefName[2]);
            parseBook(bookOsisID, osisBook, bibleBook);
        }
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        transformer.transform(new DOMSource(doc), new StreamResult(new File(Misc.getResultsDir(), outFile)));
    }

    private static void appendTextChild(Element parent, String name, String text) {
        Element elem = parent.getOwnerDocument().createElement(name);
        parent.appendChild(elem);
        elem.appendChild(parent.getOwnerDocument().createTextNode(text));
    }

    private static void parseBook(String bookName, Element osisBook, Element bibleBook) {
        for (Node node = osisBook.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Text) {
                if (((Text) node).getTextContent().trim().length() > 0)
                    throw new IllegalStateException("Non-whitespace at book level");
            } else {
                Element elem = (Element) node;
                if (elem.getNodeName().equals("title")) {
                    if (elem.getAttribute("type").equals("main")) {
                        bibleBook.setAttribute("bname", getTextChildren(elem));
                    } else {
                        throw new IllegalStateException("invalid book level title type: " + elem.getAttribute("type"));
                    }
                } else if (elem.getNodeName().equals("chapter")) {
                    String chapterName = elem.getAttribute("osisID");
                    if (!chapterName.startsWith(bookName + "."))
                        throw new IllegalStateException("Invalid chapter " + chapterName + " of book " + bookName);
                    Element chapter = bibleBook.getOwnerDocument().createElement("CHAPTER");
                    bibleBook.appendChild(chapter);
                    chapter.setAttribute("cnumber", chapterName.substring(bookName.length() + 1));
                    parseChapter(chapterName, elem, chapter);
                } else {
                    throw new IllegalStateException("invalid book level tag: " + elem.getNodeName());
                }
            }
        }
    }

    private static String getTextChildren(Element elem) {
        StringBuilder result = new StringBuilder();
        flattenChildren(elem);
        for (Node node = elem.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element) {
                throw new IllegalStateException("Unsupported tag inside " + elem.getNodeName() + ": " + node.getNodeName());
            }
            result.append(((Text) node).getTextContent());
        }
        return result.toString();
    }

    private static void parseChapter(String chapterName, Element osisChapter, Element chapter) {
        Element verse = null;
        Element prolog = null;
        flattenChildren(osisChapter);
        int nextVerse = 1;
        for (Node node = osisChapter.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Text) {
                if (verse != null) {
                    verse.appendChild(verse.getOwnerDocument().importNode(node, true));
                } else if (nextVerse == 1) {
                    if (prolog == null && ((Text) node).getTextContent().trim().length() == 0)
                        continue;
                    if (prolog == null) {
                        prolog = chapter.getOwnerDocument().createElement("PROLOG");
                        chapter.appendChild(prolog);
                    }
                    prolog.appendChild(prolog.getOwnerDocument().importNode(node,  true));
                } else if (((Text) node).getTextContent().trim().length() > 0) {
                    throw new IllegalStateException("Non-whitespace at chapter level: "+node);
                }
            } else {
                Element elem = (Element) node;
                if (elem.getNodeName().equals("title")) {
                    if (elem.getAttribute("type").equals("chapter")) {
                        // these are useless, as they only say "Kapitel ##" anyway.
                    } else {
                        Element caption = chapter.getOwnerDocument().createElement("CAPTION");
                        caption.setAttribute("vref", String.valueOf(nextVerse));
                        // footnotes in captions are not supported in Zefania XML
                        if (elem.getLastChild().getNodeName().equals("note"))
                            elem.removeChild(elem.getLastChild());
                        caption.appendChild(chapter.getOwnerDocument().createTextNode(getTextChildren(elem)));
                        chapter.appendChild(caption);
                    }
                } else if (elem.getNodeName().equals("br")) {
                    if(verse != null) {
                        Element br = verse.getOwnerDocument().createElement("BR");
                        br.setAttribute("art", "x-nl");
                        verse.appendChild(br);
                    }
                } else if (elem.getNodeName().equals("divineName")) {
                    Element parent = nextVerse == 1 ? prolog : verse;
                    if (parent == null)
                        throw new IllegalStateException("divineName at invalid location");
                    addStyle(parent, "divineName", "osis-style: divine-name; font-variant: small-caps; color: red;", getTextChildren(elem));
                } else if (elem.getNodeName().equals("seg")) {
                    if (verse == null)
                        throw new IllegalStateException("seg at invalid location");
                    parseStructuredText(verse, elem);
                } else if (elem.getNodeName().equals("transChange")) {
                    if (verse == null)
                        throw new IllegalStateException("transChange at invalid location");
                    parseStructuredText(verse, elem);
                } else if (elem.getNodeName().equals("note")) {
                    if (elem.getAttribute("type").equals("crossReference")) {
                        if (verse != null) {
                            Element note = verse.getOwnerDocument().createElement("XREF");
                            verse.appendChild(note);
                            StringBuilder fscope = new StringBuilder();
                            for (Node ref = elem.getFirstChild(); ref != null; ref = ref.getNextSibling()) {
                                if (ref instanceof Text)
                                    continue;
                                String[] fields = ((Element) ref).getAttribute("osisRef").split("\\.");
                                if (fscope.length() > 0)
                                    fscope.append("; ");
                                String book = fields[0];
                                if (zefBooks.containsKey(book))
                                    book = zefBooks.get(book)[2];
                                fscope.append(book).append(" ").append(fields[1]).append(", ").append(fields[2]);
                            }
                            note.setAttribute("fscope", fscope.toString());
                        } else {
                            throw new IllegalStateException("note tag of type crossReference at invalid location");
                        }
                    } else if (verse != null) {
                        Element note = verse.getOwnerDocument().createElement("NOTE");
                        verse.appendChild(note);
                        note.setAttribute("type", "x-studynote");
                        flattenChildren(elem);
                        note.appendChild(note.getOwnerDocument().createTextNode(getTextChildren(elem)));
                        normalizeWhitespace(note);
                    } else if (nextVerse == 1) {
                        if (prolog == null) {
                            prolog = chapter.getOwnerDocument().createElement("PROLOG");
                            chapter.appendChild(prolog);
                        }
                        // NOTE tag not allowed; abuse STYLE for it :)
                        flattenChildren(elem);
                        addStyle(prolog, "italic", null, "["+getTextChildren(elem)+"]");
                    } else {
                        throw new IllegalStateException("note tag at invalid location");
                    }
                } else if (elem.getNodeName().equals("verse")) {
                    String osisID = elem.getAttribute("osisID");
                    if (osisID.isEmpty())
                        osisID = null;
                    String sID = elem.getAttribute("sID");
                    if (sID.isEmpty())
                        sID = null;
                    String eID = elem.getAttribute("eID");
                    if (eID.isEmpty())
                        eID = null;
                    if (osisID != null && sID != null && eID == null && osisID.equals(sID)) {
                        if (!sID.startsWith(chapterName + "."))
                            throw new IllegalStateException("Invalid verse " + sID + " in chapter " + chapterName);
                        if (verse != null)
                            throw new IllegalStateException("Opening verse" + sID + " while verse " + verse.getAttribute("vnumber") + " is open");
                        verse = chapter.getOwnerDocument().createElement("VERS");
                        chapter.appendChild(verse);
                        verse.setAttribute("vnumber", sID.substring(chapterName.length() + 1));
                        nextVerse = Integer.parseInt(verse.getAttribute("vnumber")) + 1;
                    } else if (osisID == null && sID == null && eID != null) {
                        if (verse == null) {
                            throw new IllegalStateException("Closing verse that is not open");
                        } else if (!eID.equals(chapterName + "." + verse.getAttribute("vnumber"))) {
                            throw new IllegalStateException("Closing verse " + eID + " but open is " + verse.getAttribute("vnumber"));
                        }
                        normalizeWhitespace(verse);
                        verse = null;
                    } else {
                        throw new IllegalStateException("Invalid combination of verse IDs:" + osisID + "/" + sID + "/" + eID);
                    }
                } else {
                    throw new IllegalStateException("invalid book level tag: " + elem.getNodeName());
                }
            }
        }
        if (prolog != null) {
            normalizeWhitespace(prolog);
            prolog.setAttribute("vref", "1");
        }
    }

    private static void addStyle(Element parent, String fs, String css, String text) {
        Element style = parent.getOwnerDocument().createElement("STYLE");
        if (fs != null)
            style.setAttribute("fs", fs);
        if (css != null)
            style.setAttribute("css", css);
        style.appendChild(style.getOwnerDocument().createTextNode(text));
        parent.appendChild(style);
        normalizeWhitespace(style);
    }

    private static void parseStructuredText(Element parent, Element textElem) {
        String fs, css;
        if (textElem.getNodeName().equals("seg") && textElem.getAttribute("type").equals("x-alternative")) {
            fs = null;
            css = "osis-style: alternative; color: gray;";
        } else if (textElem.getNodeName().equals("transChange") && textElem.getAttribute("type").equals("added")) {
            fs = "italic";
            css = "osis-style: added; font-style:italic;";
        } else if (textElem.getNodeName().equals("transChange") && textElem.getAttribute("type").equals("deleted")) {
            fs = "line-through";
            css = "osis-style: deleted; text-decoration: line-through; color: gray;";
        } else {
            throw new IllegalStateException("Invalid " + textElem.getNodeName() + " type: " + textElem.getAttribute("type"));
        }
        addStyle(parent, fs, css, "");
        Element style = (Element) parent.getLastChild();
        flattenChildren(textElem);
        for (Node node = textElem.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Text) {
                style.appendChild(style.getOwnerDocument().importNode(node, true));
            } else {
                Element elem = (Element) node;
                if (elem.getNodeName().equals("br")) {
                    Element br = style.getOwnerDocument().createElement("BR");
                    br.setAttribute("art", "x-nl");
                    style.appendChild(br);

                } else if (elem.getNodeName().equals("divineName")) {
                    addStyle(style, "divineName", "osis-style: divine-name; font-variant: small-caps; color: red;", getTextChildren(elem));
                } else if (elem.getNodeName().equals("seg")) {
                    parseStructuredText(style, elem);
                } else if (elem.getNodeName().equals("transChange")) {
                    parseStructuredText(style, elem);
                } else if (elem.getNodeName().equals("note")) {
                    if (elem.getAttribute("type").equals("crossReference")) {
                        Element note = style.getOwnerDocument().createElement("XREF");
                        style.appendChild(note);
                        StringBuilder fscope = new StringBuilder();
                        for (Node ref = elem.getFirstChild(); ref != null; ref = ref.getNextSibling()) {
                            if (ref instanceof Text)
                                continue;
                            String[] fields = ((Element) ref).getAttribute("osisRef").split("\\.");
                            if (fscope.length() > 0)
                                fscope.append("; ");
                            String book = fields[0];
                            if (zefBooks.containsKey(book))
                                book = zefBooks.get(book)[2];
                            fscope.append(book).append(" ").append(fields[1]).append(", ").append(fields[2]);
                        }
                        note.setAttribute("fscope", fscope.toString());
                    } else {
                        Element note = style.getOwnerDocument().createElement("NOTE");
                        style.appendChild(note);
                        note.setAttribute("type", "x-studynote");
                        flattenChildren(elem);
                        note.appendChild(note.getOwnerDocument().createTextNode(getTextChildren(elem)));
                        normalizeWhitespace(note);
                    }
                } else {
                    throw new IllegalStateException("invalid structured element level tag: " + elem.getNodeName());
                }
            }
        }
        normalizeWhitespace(style);

        // push whitespace into style nodes (to work around bug in MyBible and potentially others)
        for (Node node = style.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Text && node.getTextContent().length() > 0 && node.getTextContent().trim().length() == 0) {
                if (node.getPreviousSibling() != null && node.getPreviousSibling() instanceof Element && node.getPreviousSibling().getNodeName().equals("STYLE")) {
                    Element styleNode = (Element)node.getPreviousSibling();
                    styleNode.setAttribute("css", style.getAttribute("css")+" zef-whitespace-after: true;");
                    styleNode.appendChild(node);
                    node = styleNode;
                } else if (node.getNextSibling() != null && node.getNextSibling() instanceof Element && node.getNextSibling().getNodeName().equals("STYLE")) {
                    Element styleNode = (Element) node.getNextSibling();
                    styleNode.setAttribute("css", style.getAttribute("css")+" zef-whitespace-before: true;");
                    styleNode.insertBefore(node, styleNode.getFirstChild());
                } else {
                    throw new IllegalStateException("Unable to push whitespace into style");
                }
            }
        }

        // hoist nested notes and xrefs
        for (Node node = style.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Text)
                continue;
            Element elem = (Element) node;
            if (!elem.getNodeName().equals("NOTE") && !elem.getNodeName().equals("XREF"))
                continue;
            style.setAttribute("css", style.getAttribute("css") + " zef-hoist-after: true;");
            Node nextNode = node.getNextSibling();
            style.removeChild(elem);
            parent.appendChild(elem);
            addStyle(parent, fs, css + " zef-hoist-before: true;", "");
            Element newStyle = (Element) parent.getLastChild();
            while (nextNode != null) {
                node = nextNode.getNextSibling();
                style.removeChild(nextNode);
                newStyle.appendChild(nextNode);
                nextNode = node;
            }
            style = newStyle;
            node = style.getFirstChild();
        }
    }

    private static void normalizeWhitespace(Element parent) {
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (!(node instanceof Text))
                continue;
            String content = ((Text)node).getTextContent();
            String newContent = content.replaceAll("[ \t\r\n]+", " ");
            if (newContent.startsWith(" ") && node.getPreviousSibling() == null) {
                newContent = newContent.substring(1);
            }
            if (newContent.endsWith(" ") && node.getNextSibling() == null) {
                newContent = newContent.substring(0, newContent.length()-1);
            }
            if (!content.equals(newContent)) {
                node.setTextContent(newContent);
            }
        }
    }

    private static void flattenChildren(Element parent) {
        // flatten quotes / foreign / line groups / lines; add <br> tags around lines
        boolean brTagsInserted = false;
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node.getNodeName().equals("l")) {
                brTagsInserted = true;
                parent.insertBefore(parent.getOwnerDocument().createElement("br"), node);
                if (node.getNextSibling() == null)
                    parent.appendChild(parent.getOwnerDocument().createElement("br"));
                else
                    parent.insertBefore(parent.getOwnerDocument().createElement("br"), node.getNextSibling());
            }
            if (Arrays.asList("q", "foreign", "lg", "l", "hi", "a").contains(node.getNodeName())) {
                while (node.getFirstChild() != null) {
                    Node child = node.getFirstChild();
                    node.removeChild(child);
                    parent.insertBefore(child, node);
                }
                parent.removeChild(node);
                node = parent.getFirstChild();
            }
        }
        if (brTagsInserted) {
            for (Node node1 = parent.getFirstChild(); node1 != null; node1=node1.getNextSibling()) {
                Node node2 = node1.getNextSibling();
                while (node2 != null && node1.getNodeName().equals("br") && node2.getNodeName().equals("br")) {
                    parent.removeChild(node2);
                    node2 = node1.getNextSibling();
                }
            }
        }
    }
}
