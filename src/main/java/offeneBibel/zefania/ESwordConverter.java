package offeneBibel.zefania;

import java.io.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;

import util.Misc;

public class ESwordConverter {

    private static String marker;
    public static void main(String[] args) throws Exception {
        marker = args.length > 0 ? args[0] : "";
        convert("offeneBibelStudienfassungZefania.xml");
        convert("offeneBibelLesefassungZefania.xml");
    }

    private static final Pattern xrefPattern = Pattern.compile("([A-Za-z0-9]+) ([0-9]+), ([0-9]+)");

    private static String[] E_SWORD_BOOKS = new String[] {
            null, // 0
            "Genesis", // 1
            "Exodus",
            "Leviticus",
            "Numbers",
            "Deuteronomy",
            "Joshua",
            "Judges",
            "Ruth",
            "1 Samuel",
            "2 Samuel",
            "1 Kings",
            "2 Kings",
            "1 Chronicles",
            "2 Chronicles",
            "Ezra",
            "Nehemiah",
            "Esther",
            "Job",
            "Psalm",
            "Proverbs", // 20
            "Ecclesiastes",
            "Song of Solomon",
            "Isaiah",
            "Jeremiah",
            "Lamentations",
            "Ezekiel",
            "Daniel",
            "Hosea",
            "Joel",
            "Amos",
            "Obadiah",
            "Jonah",
            "Micah",
            "Nahum",
            "Habakkuk",
            "Zephaniah",
            "Haggai",
            "Zechariah",
            "Malachi",
            "Matthew", // 40
            "Mark",
            "Luke",
            "John",
            "Acts",
            "Romans",
            "1 Corinthians",
            "2 Corinthians",
            "Galatians",
            "Ephesians",
            "Philippians",
            "Colossians",
            "1 Thessalonians",
            "2 Thessalonians",
            "1 Timothy",
            "2 Timothy",
            "Titus",
            "Philemon",
            "Hebrews",
            "James",
            "1 Peter", // 60
            "2 Peter",
            "1 John",
            "2 John",
            "3 John",
            "Jude",
            "Revelation", // 66
            "Judith",
            "Wisdom",
            "Tobit",
            "Sirach", // 70
            "Baruch",
            "1Maccabees",
            "2Maccabees",
            null, // xDan
            null, // xEst

            "Prayer of Manasseh",
            "3Maccabees",
            "4Maccabees",
            null, // LetJer
            "1 Esdras", // 80
            "2 Esdras",
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

    // Some verses used by Offene Bibel are outside of the Canon used by
    // E-Sword. For now, just strip them...
    private static String[] INVALID_VERSES = new String[] {
            "Genesis 32:33",
            "Exodus 7:26", "Exodus 7:27", "Exodus 7:28", "Exodus 7:29",
            "Exodus 21:37",
            "Leviticus 5:20", "Leviticus 5:21", "Leviticus 5:22", "Leviticus 5:23",
            "Leviticus 5:24", "Leviticus 5:25", "Leviticus 5:26",
            "Numbers 17:14", "Numbers 17:15", "Numbers 17:16", "Numbers 17:17",
            "Numbers 17:18", "Numbers 17:19", "Numbers 17:20", "Numbers 17:21",
            "Numbers 17:22", "Numbers 17:23", "Numbers 17:24", "Numbers 17:25",
            "Numbers 17:26", "Numbers 17:27", "Numbers 17:28",
            "Numbers 25:19",
            "Numbers 30:17",
            "Deuteronomy 13:19", "Deuteronomy 23:26", "Deuteronomy 28:69",
            "1 Samuel 21:16", "1 Samuel 24:23",
            "2 Samuel 19:44",
            "1 Kings 5:19", "1 Kings 5:20", "1 Kings 5:21", "1 Kings 5:22",
            "1 Kings 5:23", "1 Kings 5:24", "1 Kings 5:25", "1 Kings 5:26",
            "1 Kings 5:27", "1 Kings 5:28", "1 Kings 5:29", "1 Kings 5:30",
            "1 Kings 5:31", "1 Kings 5:32",
            "1 Kings 22:54",
            "2 Kings 12:22",
            "1 Chronicles 5:27", "1 Chronicles 5:28", "1 Chronicles 5:29", "1 Chronicles 5:30",
            "1 Chronicles 5:31", "1 Chronicles 5:32", "1 Chronicles 5:33", "1 Chronicles 5:34",
            "1 Chronicles 5:35", "1 Chronicles 5:36", "1 Chronicles 5:37", "1 Chronicles 5:38",
            "1 Chronicles 5:39", "1 Chronicles 5:40", "1 Chronicles 5:41",
            "1 Chronicles 12:41",
            "2 Chronicles 1:18", "2 Chronicles 13:23",
            "Nehemiah 3:33", "Nehemiah 3:34", "Nehemiah 3:35", "Nehemiah 3:36",
            "Nehemiah 3:37", "Nehemiah 3:38",
            "Nehemiah 10:40",
            "Job 40:25", "Job 40:26", "Job 40:27", "Job 40:28",
            "Job 40:29", "Job 40:30", "Job 40:31", "Job 40:32",
            "Psalm 3:9", "Psalm 4:9", "Psalm 5:13", "Psalm 6:11",
            "Psalm 7:18", "Psalm 8:10", "Psalm 9:21", "Psalm 12:9",
            "Psalm 18:51", "Psalm 19:15", "Psalm 20:10", "Psalm 21:14",
            "Psalm 22:32", "Psalm 30:13", "Psalm 31:25", "Psalm 34:23",
            "Psalm 36:13", "Psalm 38:23", "Psalm 39:14", "Psalm 40:18",
            "Psalm 41:14", "Psalm 42:12", "Psalm 44:27", "Psalm 45:18",
            "Psalm 46:12", "Psalm 47:10", "Psalm 48:15", "Psalm 49:21",
            "Psalm 51:20", "Psalm 51:21", "Psalm 52:10", "Psalm 52:11",
            "Psalm 53:7", "Psalm 54:8", "Psalm 54:9", "Psalm 55:24",
            "Psalm 56:14", "Psalm 57:12", "Psalm 58:12", "Psalm 59:18",
            "Psalm 60:13", "Psalm 60:14",
            "Psalm 61:9", "Psalm 62:13", "Psalm 63:12", "Psalm 64:11",
            "Psalm 65:14", "Psalm 67:8", "Psalm 68:36", "Psalm 69:37",
            "Psalm 70:6", "Psalm 75:11", "Psalm 76:13", "Psalm 77:21",
            "Psalm 80:20", "Psalm 81:17", "Psalm 83:19", "Psalm 84:13",
            "Psalm 85:14", "Psalm 88:19", "Psalm 89:53", "Psalm 92:16",
            "Psalm 102:29", "Psalm 108:14", "Psalm 140:14", "Psalm 142:8",
            "Ecclesiastes 4:17",
            "Song of Solomon 7:14",
            "Isaiah 8:23",
            "Jeremiah 8:23",
            "Ezekiel 21:33", "Ezekiel 21:34", "Ezekiel 21:35", "Ezekiel 21:36",
            "Ezekiel 21:37",
            "Daniel 3:31", "Daniel 3:32", "Daniel 3:33",
            "Daniel 6:29",
            "Hosea 2:24", "Hosea 2:25",
            "Hosea 12:15", "Hosea 14:10",
            "Joel 4:1", "Joel 4:2", "Joel 4:3", "Joel 4:4",
            "Joel 4:5", "Joel 4:6", "Joel 4:7", "Joel 4:8",
            "Joel 4:9", "Joel 4:10", "Joel 4:11", "Joel 4:12",
            "Joel 4:13", "Joel 4:14", "Joel 4:15", "Joel 4:16",
            "Joel 4:17", "Joel 4:18", "Joel 4:19", "Joel 4:20",
            "Joel 4:21",
            "Jonah 2:11",
            "Micah 4:14",
            "Nahum 2:14",
            "Zechariah 2:14", "Zechariah 2:15", "Zechariah 2:16", "Zechariah 2:17",
            "Malachi 3:19", "Malachi 3:20", "Malachi 3:21", "Malachi 3:22",
            "Malachi 3:23", "Malachi 3:24",
            "3 John 1:15",
            "Revelation 12:18",
            "Baruch 1:22", "Baruch 3:38",
            "Judith 15:14",
            "Sirach 33:32", "Sirach 33:33",
            "Sirach 35:21", "Sirach 35:22", "Sirach 35:23", "Sirach 35:24",
            "Sirach 36:27",
            "Sirach 41:24", "Sirach 41:25", "Sirach 41:26", "Sirach 41:27",
            "Tobit 5:22", "Tobit 5:23",
            "Tobit 6:18", "Tobit 6:19",
            "Tobit 10:13", "Tobit 10:14",
    };

    private static void convert(String zefFile) throws Exception {
        HashSet<String> invalidVerses = new HashSet<>(Arrays.asList(INVALID_VERSES));
        DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        XPath xpath = javax.xml.xpath.XPathFactory.newInstance().newXPath();
        Document zefDoc = docBuilder.parse(new File(Misc.getResultsDir(), zefFile));
        String title = xpath.evaluate("/XMLBIBLE/INFORMATION/title/text()", zefDoc);
        String description = xpath.evaluate("/XMLBIBLE/INFORMATION/description/text()", zefDoc);
        String identifier = xpath.evaluate("/XMLBIBLE/INFORMATION/identifier/text()", zefDoc);

        try (BufferedWriter bblx = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(Misc.getResultsDir(), identifier + ".bblx.HTM"))));
                BufferedWriter cmtx = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(Misc.getResultsDir(), identifier + ".cmtx.HTM"))))) {

            bblx.write("<html><head>\n" +
                    "<meta http-equiv=\"Content-Type\" content=\"text/html;charset=utf-8\" />\n" +
                    "<style>p{margin-top:0pt;margin-bottom:0pt;}</style>\n" +
                    "</head><body>\n" +
                    "<p>#define description=" + title + marker + "</p>\n" +
                    "<p>#define abbreviation=" + identifier + marker + "</p>\n" +
                    "<p>#define comments=" + description + marker + "</p>\n" +
                    "<p>#define version=1" + marker + "</p>\n" +
                    "<p>#define strong=0" + marker + "</p>\n" +
                    "<p>#define right2left=0" + marker + "</p>\n" +
                    "<p>#define ot=1" + marker + "</p>\n" +
                    "<p>#define nt=1" + marker + "</p>\n" +
                    "<p>#define font=DEFAULT" + marker + "</p>\n" +
                    "<p>#define apocrypha=1" + marker + "</p>\n" +
                    "<p><span style=\"background-color:#C80000;\">\u00F7</span>" + marker + "</p>\n");

            cmtx.write("<html><head>\n" +
                    "<meta http-equiv=\"Content-Type\" content=\"text/html;charset=utf-8\" />\n" +
                    "<style>\n" +
                    "p{margin-top:0pt;margin-bottom:0pt;}\n" +
                    "p.spc{margin-top:10pt;margin-bottom:0pt;}\n" +
                    "p.prologend{border-width:1px;border-top-style:none;border-right-style:none;border-bottom-style:solid;border-left-style:none;border-color:black}" +
                    "</style></head><body>\n" +
                    "<p>#define description=" + title + " (Kommentar)" + marker + "</p>\n" +
                    "<p>#define abbreviation=" + identifier + marker + "</p>\n" +
                    "<p>#define comments=" + description + marker + "</p>\n" +
                    "<p>#define version=1" + marker + "</p>\r\n");

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
                String bname = E_SWORD_BOOKS[Integer.parseInt(bookElement.getAttribute("bnumber"))];
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
                    Element prolog = null;
                    for (Node verseNode = chapterElement.getFirstChild(); verseNode != null; verseNode = verseNode.getNextSibling()) {
                        if (verseNode instanceof Text)
                            continue;
                        Element verseElement = (Element) verseNode;
                        if (verseElement.getNodeName().equals("PROLOG")) {
                            prolog = verseElement;
                            continue;
                        } else if (verseElement.getNodeName().equals("CAPTION")) {
                            // captions not supported by E-Sword
                            continue;
                        }
                        if (!verseElement.getNodeName().equals("VERS"))
                            throw new IOException(verseElement.getNodeName());
                        if (verseElement.getFirstChild() == null)
                            continue;
                        int vnumber = Integer.parseInt(verseElement.getAttribute("vnumber"));
                        String vref = bname + " " + cnumber + ":" + vnumber;
                        if (invalidVerses.contains(vref))
                            continue;
                        bblx.write("<p>" + parseVerse(verseElement, vref, cmtx, prolog) + marker + "</p>\n");
                        prolog = null;
                    }
                }
            }

            bblx.write("</body></html>");
            cmtx.write("</body></html>");
        }
    }

    private static String parseVerse(Element verseElement, String vref, BufferedWriter cmtx, Element prologElement) throws IOException {
        boolean hasCommentary = false;
        StringBuilder verse = new StringBuilder(vref + " ");
        StringBuilder comments = new StringBuilder("<p><span style=\"background-color:#FF0000;\">\u00F7</span>" + vref + marker + "</p>\n<p>");
        if (prologElement != null) {
            for (Node node = prologElement.getFirstChild(); node != null; node = node.getNextSibling()) {
                if (node instanceof Text) {
                    String txt = ((Text) node).getTextContent();
                    txt = txt.replace("&", "&amp").replace("<", "&lt;").replace(">", "&gt;").replace("{", "(").replace("}",")").replaceAll("[ \t\r\n]+", " ");
                    comments.append(txt);
                } else {
                    Element elem = (Element) node;
                    if (elem.getNodeName().equals("STYLE")) {
                        String content = elem.getTextContent();
                        comments.append(marker + "</p>\n<p><i>" + content + "</i>" + marker + "</p>\n<p class=\"spc\">");
                        hasCommentary = true;
                    } else if (elem.getNodeName().equals("BR")) {
                        comments.append("<br />");
                    } else {
                        throw new IllegalStateException("invalid prolog tag: " + elem.getNodeName());
                    }
                }
            }
            comments.append(marker + "</p>\n<p class=\"prologend\">&nbsp;" + marker + "</p>\n<p class=\"spc\">");
            hasCommentary = true;
        }
        for (Node node = verseElement.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Text) {
                String txt = ((Text) node).getTextContent();
                txt = txt.replace("&", "&amp").replace("<", "&lt;").replace(">", "&gt;").replaceAll("[ \t\r\n]+", " ");
                if (txt.contains("{"))
                    throw new IllegalStateException();
                verse.append(txt);
                comments.append("<b>" + txt + "</b>");
            } else {
                Element elem = (Element) node;
                if (elem.getNodeName().equals("NOTE")) {
                    String content = elem.getTextContent();
                    comments.append(marker + "</p>\n<p>" + content + marker + "</p>\n<p class=\"spc\">");
                    hasCommentary = true;
                } else if (elem.getNodeName().equals("BR")) {
                    verse.append("<br />");
                    comments.append("<br />");
                } else if (elem.getNodeName().equals("XREF")) {
                    comments.append(marker + "</p>\n<p class=\"spc\">(Parallelstellen:");
                    for(String ref : elem.getAttribute("fscope").split("; ")) {
                        Matcher m = xrefPattern.matcher(ref);
                        if (!m.matches())
                            throw new IllegalStateException("Invalid XREF: "+ref);
                        String book = m.group(1);
                        int bookIdx = Arrays.asList(ZEF_SHORT_BOOKS).indexOf(book);
                        if (bookIdx != -1) book = E_SWORD_BOOKS[bookIdx];
                        comments.append(" <span style=\"color:#008000;font-weight:bold;text-decoration:underline;\">");
                        comments.append(book+"_"+m.group(2)+":"+m.group(3)+"</span>");
                    }
                    comments.append(")" + marker + "</p>\n<p class=\"spc\">");
                    hasCommentary = true;
                } else if (elem.getNodeName().equals("STYLE")) {
                    StringBuilder styleContent = new StringBuilder();
                    appendStyle(styleContent, elem);
                    verse.append(styleContent.toString());
                    comments.append("<b>"+styleContent.toString()+"</b>");
                } else {
                    throw new IllegalStateException("invalid verse level tag: " + elem.getNodeName());
                }
            }
        }
        if (hasCommentary) {
            comments.append(marker + "</p>\n");
            cmtx.write(comments.toString());
        }
        if (verse.toString().trim().equals(vref))
            verse.append("-");
        return verse.toString();
    }

    private static void appendStyle(StringBuilder styleContent, Element styleElement) {
        styleContent.append("<span style=\"" + styleElement.getAttribute("css") + "\">");
        for (Node node = styleElement.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Text) {
                String txt = ((Text) node).getTextContent();
                txt = txt.replace("&", "&amp").replace("<", "&lt;").replace(">", "&gt;").replaceAll("[ \t\r\n]+", " ").replace('{', '(').replace('}', ')');
                styleContent.append(txt);
            } else {
                Element elem = (Element) node;
                if (elem.getNodeName().equals("BR")) {
                    styleContent.append("<br />");
                } else if (elem.getNodeName().equals("STYLE")) {
                    appendStyle(styleContent, elem);
                } else {
                    throw new IllegalStateException("invalid style level tag: " + elem.getNodeName());
                }
            }
        }
        styleContent.append("</span>");
    }
}
