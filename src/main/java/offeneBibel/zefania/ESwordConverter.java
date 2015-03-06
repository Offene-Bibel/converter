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

    public static void main(String[] args) throws Exception {
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
    };

    private static String[] ZEF_SHORT_BOOKS = new String[] {
        null,
        "Gen", "Exo", "Lev", "Num", "Deu", "Jos", "Jdg", "Rth", "1Sa", "2Sa",
        "1Ki", "2Ki", "1Ch", "2Ch", "Ezr", "Neh", "Est", "Job", "Psa", "Pro",
        "Ecc", "Son", "Isa", "Jer", "Lam", "Eze", "Dan", "Hos", "Joe", "Amo",
        "Oba", "Jon", "Mic", "Nah", "Hab", "Zep", "Hag", "Zec", "Mal", "Mat",
        "Mar", "Luk", "Joh", "Act", "Rom", "1Co", "2Co", "Gal", "Eph", "Php",
        "Col", "1Th", "2Th", "1Ti", "2Ti", "Tit", "Phm", "Heb", "Jas", "1Pe",
        "2Pe", "1Jn", "2Jn", "3Jn", "Jud", "Rev"
    };

    // Some verses used by Offene Bibel are outside of the Canon used by
    // E-Sword. For now, just strip them...
    private static String[] INVALID_VERSES = new String[] {
            "Genesis 32:33",
            "Psalm 3:9", "Psalm 4:9", "Psalm 5:13", "Psalm 6:11",
            "Psalm 19:15", "Psalm 30:13", "Psalm 67:8", "Psalm 88:19",
            "Zechariah 2:14", "Zechariah 2:15", "Zechariah 2:16",
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
                    "<p>#define description=" + title + "</p>\n" +
                    "<p>#define abbreviation=" + identifier + "</p>\n" +
                    "<p>#define comments=" + description + "</p>\n" +
                    "<p>#define version=1</p>\n" +
                    "<p>#define strong=0</p>\n" +
                    "<p>#define right2left=0</p>\n" +
                    "<p>#define ot=1</p>\n" +
                    "<p>#define nt=1</p>\n" +
                    "<p>#define font=DEFAULT</p>\n" +
                    "<p>#define apocrypha=1</p>\n" +
                    "<p><span style=\"background-color:#C80000;\">\u00F7</span></p>\n");

            cmtx.write("<html><head>\n" +
                    "<meta http-equiv=\"Content-Type\" content=\"text/html;charset=utf-8\" />\n" +
                    "<style>\n" +
                    "p{margin-top:0pt;margin-bottom:0pt;}\n" +
                    "p.spc{margin-top:10pt;margin-bottom:0pt;}\n" +
                    "p.prologend{border-width:1px;border-top-style:none;border-right-style:none;border-bottom-style:solid;border-left-style:none;border-color:black}" +
                    "</style></head><body>\n" +
                    "<p>#define description=" + title + " (Kommentar)</p>\n" +
                    "<p>#define abbreviation=" + identifier + "</p>\n" +
                    "<p>#define comments=" + description + "</p>\n" +
                    "<p>#define version=1</p>\r\n");

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
                        }
                        if (!verseElement.getNodeName().equals("VERS"))
                            throw new IOException(verseElement.getNodeName());
                        if (verseElement.getFirstChild() == null)
                            continue;
                        int vnumber = Integer.parseInt(verseElement.getAttribute("vnumber"));
                        String vref = bname + " " + cnumber + ":" + vnumber;
                        if (invalidVerses.contains(vref))
                            continue;
                        bblx.write("<p>" + parseVerse(verseElement, vref, cmtx, prolog) + "</p>\n");
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
        boolean strikeOutOpen = false;
        StringBuilder verse = new StringBuilder(vref + " ");
        StringBuilder comments = new StringBuilder("<p><span style=\"background-color:#FF0000;\">\u00F7</span>" + vref + "</p>\n<p>");
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
                        comments.append("</p>\n<p><i>" + content + "</i></p>\n<p class=\"spc\">");
                        hasCommentary = true;
                    } else if (elem.getNodeName().equals("BR")) {
                        comments.append("<br />");
                    } else {
                        throw new IllegalStateException("invalid prolog tag: " + elem.getNodeName());
                    }
                }
            }
            comments.append("</p>\n<p class=\"prologend\">&nbsp;</p>\n<p class=\"spc\">");
            hasCommentary = true;
        }
        for (Node node = verseElement.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Text) {
                String txt = ((Text) node).getTextContent();
                txt = txt.replace("&", "&amp").replace("<", "&lt;").replace(">", "&gt;").replaceAll("[ \t\r\n]+", " ");
                if (strikeOutOpen) {
                    strikeOutOpen = false;
                    txt = "{" + txt;
                }
                while (txt.contains("{")) {
                    int pos1 = txt.indexOf('{'), pos2 = txt.indexOf('}');
                    if (pos2 == -1 && !strikeOutOpen) {
                        txt += "}";
                        strikeOutOpen = true;
                        pos2 = txt.indexOf('}');
                    }
                    if (pos2 < pos1)
                        throw new IOException(txt);
                    txt = txt.substring(0, pos1) +
                            "<span style=\"text-decoration:line-through;\">(" +
                            txt.substring(pos1 + 1, pos2) +
                            ")</span>" +
                            txt.substring(pos2 + 1);
                }
                verse.append(txt);
                comments.append("<b>" + txt + "</b>");
            } else {
                Element elem = (Element) node;
                if (elem.getNodeName().equals("NOTE")) {
                    String content = elem.getTextContent();
                    comments.append("</p>\n<p>" + content + "</p>\n<p class=\"spc\">");
                    hasCommentary = true;
                } else if (elem.getNodeName().equals("BR")) {
                    verse.append("<br />");
                    comments.append("<br />");
                } else if (elem.getNodeName().equals("XREF")) {
                    comments.append("</p>\n<p class=\"spc\">(Parallelstellen:");
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
                    comments.append(")</p>\n<p class=\"spc\">");
                    hasCommentary = true;
                } else {
                    throw new IllegalStateException("invalid verse level tag: " + elem.getNodeName());
                }
            }
        }
        if (strikeOutOpen)
            throw new IOException();
        if (hasCommentary) {
            comments.append("</p>\n");
            cmtx.write(comments.toString());
        }
        return verse.toString();
    }
}
