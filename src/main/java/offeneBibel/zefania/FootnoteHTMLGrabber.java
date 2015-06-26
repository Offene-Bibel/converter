package offeneBibel.zefania;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import util.Misc;

public class FootnoteHTMLGrabber {

    private static final String BASE_URL = "http://www.offene-bibel.de/mediawiki/index.php?title=";

    private static final Pattern PROLOG_REGEX = Pattern.compile(
            "\n<div class=\"bemerkungen\">(.*?)\n</div>\n<p></p>\n<table class=\"references\">",
            Pattern.DOTALL);

    private static final Pattern FOOTNOTE_REGEX = Pattern.compile(
            "<td class=\"note\" id=\"note_[a-z]++\">(.*?)\n?</td>\n?</tr>\n?(<tr>\n?<td class=\"note_id\">|</table>\n?(<h2>|<div class=\"navi\">|</p>\n?<h2>))",
            Pattern.DOTALL);

    public static void main(String[] args) throws Exception {
        convert("offeneBibelStudienfassungZefania");
        convert("offeneBibelLesefassungZefania");
    }

    private static String[] WIKI_BOOKS = new String[] {
            null,
            "Genesis", "Exodus", "Levitikus", "Numeri", "Deuteronomium",
            "Josua", "Richter", "Rut", "1_Samuel", "2_Samuel",
            "1_Könige", "2_Könige", "1_Chronik", "2_Chronik", "Esra",
            "Nehemia", "Ester", "Ijob", "Psalm", "Sprichwörter",
            "Kohelet", "Hohelied", "Jesaja", "Jeremia", "Klagelieder",
            "Ezechiel", "Daniel", "Hosea", "Joel", "Amos",
            "Obadja", "Jona", "Micha", "Nahum", "Habakuk",
            "Zefanja", "Haggai", "Sacharja", "Maleachi", "Matthäus",
            "Markus", "Lukas", "Johannes", "Apostelgeschichte", "Römer",
            "1_Korinther", "2_Korinther", "Galater", "Epheser", "Philipper",
            "Kolosser", "1_Thessalonicher", "2_Thessalonicher", "1_Timotheus", "2_Timotheus",
            "Titus", "Philemon", "Hebräer", "Jakobus", "1_Petrus",
            "2_Petrus", "1_Johannes", "2_Johannes", "3_Johannes", "Judas",
            "Offenbarung", "Judit", "Weisheit", "Tobit", "Jesus_Sirach",
            "Baruch", "1_Makkabäer", "2_Makkabäer"
    };

    private static void convert(String zefFile) throws Exception {
        DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        XPath xpath = javax.xml.xpath.XPathFactory.newInstance().newXPath();
        Document doc = docBuilder.parse(new File(Misc.getResultsDir(), zefFile + ".xml"));

        Node identifier = ((Node) xpath.evaluate("/XMLBIBLE/INFORMATION/identifier", doc, XPathConstants.NODE)).getFirstChild();
        identifier.setNodeValue(identifier.getNodeValue() + "HtmlFn");
        for (Node bookNode = doc.getDocumentElement().getFirstChild().getNextSibling(); bookNode != null; bookNode = bookNode.getNextSibling()) {
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
            String wikiname = WIKI_BOOKS[Integer.parseInt(bookElement.getAttribute("bnumber"))];
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
                Map<String, String> htmlFragments = null;
                for (Node verseNode = chapterElement.getFirstChild(); verseNode != null; verseNode = verseNode.getNextSibling()) {
                    if (verseNode instanceof Text)
                        continue;
                    Element verseElement = (Element) verseNode;
                    if (verseElement.getNodeName().equals("PROLOG")) {
                        if (htmlFragments == null)
                            htmlFragments = grabFragments(wikiname, cnumber);
                        htmlize(verseElement, htmlFragments);
                        continue;
                    } else if (verseElement.getNodeName().equals("VERS")) {
                        for (Node node = verseElement.getFirstChild(); node != null; node = node.getNextSibling()) {
                            if (node instanceof Text)
                                continue;
                            Element elem = (Element) node;
                            if (elem.getNodeName().equals("NOTE")) {
                                String content = elem.getTextContent();
                                if (content.startsWith("[St"))
                                    continue;
                                if (htmlFragments == null)
                                    htmlFragments = grabFragments(wikiname, cnumber);
                                htmlize(elem, htmlFragments);
                            }
                        }
                    }
                }
            }
        }

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        transformer.transform(new DOMSource(doc), new StreamResult(new File(Misc.getResultsDir(), zefFile + "MitHtmlFußnoten.xml")));
    }

    private static void htmlize(Element elem, Map<String, String> htmlFragments) {
        String textVersion = elem.getTextContent();
        String htmlVersion = findHTMLVersion(textVersion, htmlFragments);
        if (htmlVersion != null && !htmlVersion.equals(textVersion)) {
            while (elem.getFirstChild() != null)
                elem.removeChild(elem.getFirstChild());
            elem.appendChild(elem.getOwnerDocument().createTextNode("<html>" + htmlVersion));
        }
    }

    private static Map<String, String> grabFragments(String wikiName, int chapter) throws IOException {
        String result;
        String fileCacheString = Misc.getPageCacheDir() + wikiName + "_" + chapter + ".html";
        if (new File(fileCacheString).exists()) {
            result = Misc.readFile(fileCacheString);
        }
        else {
            result = Misc.retrieveUrl(BASE_URL + URLEncoder.encode(wikiName + "_" + chapter, "UTF-8"));
            System.out.println("Grabbing " + wikiName + "_" + chapter);
            Misc.writeFile(result, fileCacheString);
        }
        Map<String,String> fragments = new HashMap<String, String>();
        result = result.replaceAll("[\r\n]++", "\n");
        for(Pattern regex : Arrays.asList(PROLOG_REGEX, FOOTNOTE_REGEX)) {
            Matcher m = regex.matcher(result);
            while (m.find()) {
                String fragment = m.group(1);
                fragment = fragment.replaceAll("<span class=\"backlinks\">.*", "");
                String key = fragment.replaceAll("<span class=\"tooltip\"><span class=\"tooltip_abbr\">(.*?)</span><span class=\"tooltip_tipwrapper\"><span class=\"tooltip_tip\"><span>.*?</span></span></span></span>", "$1");
                key = key.replaceAll("<[^<>]++>", "").replaceAll("&(#[0-9]+|[a-z]+);", "").replaceAll("[^A-Za-z0-9]++", "");
                if (key.length() == 0)
                    continue;
                fragments.put(key, fragment);
            }
        }
        return fragments;
    }

    private static String findHTMLVersion(String textVersion, Map<String, String> htmlFragments) {
        String key = textVersion.replaceAll("(?i)class=[\"a-z0-9]++", "").replaceAll("<[^<>]++>?", "");
        key = key.replaceAll("[^A-Za-z0-9]++", "");
        if (htmlFragments.containsKey(key))
            return htmlFragments.get(key);
        return null;
    }
}
