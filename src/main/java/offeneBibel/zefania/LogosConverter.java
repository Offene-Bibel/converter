package offeneBibel.zefania;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;

import offeneBibel.osisExporter.OsisGeneratorVisitor.NoteIndexCounter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import util.Misc;

// open resulting HTML files in LibreOffice 4.4 (as Writer, not as Writer/Web) and save as MS Office 2007 DOCX.
public class LogosConverter {

	public static void main(String[] args) throws Exception {
		convert("offeneBibelStudienfassungZefania.xml");
		convert("offeneBibelLesefassungZefania.xml");
		convert("offeneBibelStudienfassungZefaniaMitHtmlFußnoten.xml");
		convert("offeneBibelLesefassungZefaniaMitHtmlFußnoten.xml");
	}

	private static final Pattern xrefPattern = Pattern.compile("([A-Za-z0-9]+) ([0-9]+), ([0-9]+)");

	private static String[] LOGOS_BOOKS = new String[] {
			null, // 0
			"Ge", // 1
			"Ex",
			"Le",
			"Nu",
			"Dt",
			"Jos",
			"Jdg",
			"Ru",
			"1Sa",
			"2Sa",
			"1Ki",
			"2Ki",
			"1Ch",
			"2Ch",
			"Ezr",
			"Ne",
			"Es",
			"Job",
			"Ps",
			"Pr", // 20
			"Ec",
			"So",
			"Is",
			"Je",
			"La",
			"Eze",
			"Da",
			"Ho",
			"Joe",
			"Am",
			"Ob",
			"Jon",
			"Mic",
			"Na",
			"Hab",
			"Zep",
			"Hag",
			"Zec",
			"Mal",
			"Mt", // 40
			"Mk",
			"Lk",
			"Jn",
			"Ac",
			"Ro",
			"1Co",
			"2Co",
			"Ga",
			"Eph",
			"Php",
			"Col",
			"1Th",
			"2Th",
			"1Ti",
			"2Ti",
			"Tt",
			"Phm",
			"Heb",
			"Jas",
			"1Pe", // 60
			"2Pe",
			"1Jn",
			"2Jn",
			"3Jn",
			"Jud",
			"Re", // 66
			"Jdt",
			"Wis",
			"Tob",
			"Sir", // 70
			"Bar",
			"1Mac",
			"2Mac",
			null, // xDan
			null, // xEst
			"PrMan",
			"3Mac",
			"4Mac",
			"LetJer",
			"1Esd", // 80
			"2Esd",
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
		footnoteCounter = 0;
		DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		XPath xpath = javax.xml.xpath.XPathFactory.newInstance().newXPath();
		Document zefDoc = docBuilder.parse(new File(Misc.getResultsDir(), zefFile));
		String title = xpath.evaluate("/XMLBIBLE/INFORMATION/title/text()", zefDoc);
		String description = xpath.evaluate("/XMLBIBLE/INFORMATION/description/text()", zefDoc);
		String identifier = xpath.evaluate("/XMLBIBLE/INFORMATION/identifier/text()", zefDoc);
		String rights = xpath.evaluate("/XMLBIBLE/INFORMATION/rights/text()", zefDoc);

		try (BufferedWriter bblx = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(Misc.getResultsDir(), identifier + ".logos.html"))))) {
			bblx.write("<html><head>\n" +
					"<meta http-equiv=\"Content-Type\" content=\"text/html;charset=utf-8\" />\n" +
					"<style>\n" +
					"body, h1, h2, h3, h4 { font-family: \"Times New Roman\";}\n" +
					"a.sdfootnotesym, a.sdendnotesym { font-style: italic;}\n" +
					"</style>\n"+
					"</head><body lang=\"de-DE\">\n" +
					"<h1>" + title + "</h1>\n" +
					description + "<br />Lizenz: " + rights + "\n");

			StringBuilder footnotes = new StringBuilder();

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
				String babbr = LOGOS_BOOKS[Integer.parseInt(bookElement.getAttribute("bnumber"))];
				bblx.write("<h2>[[@" + getVerseMap(babbr) + ":" + babbr + "]]" + bookElement.getAttribute("bname") + "</h2>\n");
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
					String chapterRef = "@" + getVerseMap(babbr) + ":" + babbr + " " + cnumber;
					bblx.write("<h3>[[" + chapterRef + "]]Kapitel " + cnumber + "</h3>\n");
					footnoteTextCounter.reset();
					Element prolog = null, caption = null;
					for (Node verseNode = chapterElement.getFirstChild(); verseNode != null; verseNode = verseNode.getNextSibling()) {
						if (verseNode instanceof Text)
							continue;
						Element verseElement = (Element) verseNode;
						if (verseElement.getNodeName().equals("PROLOG")) {
							prolog = verseElement;
							continue;
						} else if (verseElement.getNodeName().equals("CAPTION")) {
							caption = verseElement;
							continue;
						}
						if (!verseElement.getNodeName().equals("VERS"))
							throw new IOException(verseElement.getNodeName());
						if (verseElement.getFirstChild() == null)
							continue;

						if (prolog != null) {
							for (Node node = prolog.getFirstChild(); node != null; node = node.getNextSibling()) {
								if (node instanceof Text) {
									String txt = ((Text) node).getTextContent();
									if (txt.startsWith("<html>"))
										txt = txt.substring(6).replaceAll("[ \t\r\n]+", " ");
									else
										txt = txt.replace("&", "&amp").replace("<", "&lt;").replace(">", "&gt;").replaceAll("[ \t\r\n]+", " ");
									bblx.write(tagForeign(txt));
								} else {
									Element elem = (Element) node;
									if (elem.getNodeName().equals("STYLE")) {
										String content = elem.getTextContent();
										bblx.write(buildFootnote(content, footnotes));
									} else if (elem.getNodeName().equals("BR")) {
										bblx.write("\n<br />");
									} else {
										throw new IllegalStateException("invalid prolog tag: " + elem.getNodeName());
									}
								}
							}
							bblx.write("\n<br/>\n");
						}

						if (caption != null) {
							bblx.write("<h4>"+caption.getTextContent()+"</h4>\n");
						}

						int vnumber = Integer.parseInt(verseElement.getAttribute("vnumber"));
						String vref = chapterRef + ":" + vnumber;
						bblx.write("<br />[[" + vref + "]]<b>" + vnumber + "</b> {{field-on:bible}}" + parseVerse(verseElement, footnotes) + "{{field-off:bible}}\n");
						prolog = null;
						caption = null;
					}
				}
			}

			bblx.write(footnotes.toString());
			bblx.write("</body></html>");
		}
	}

	private static String parseVerse(Element verseElement, StringBuilder footnotes) throws IOException {
		StringBuilder verse = new StringBuilder();
		for (Node node = verseElement.getFirstChild(); node != null; node = node.getNextSibling()) {
			if (node instanceof Text) {
				String txt = ((Text) node).getTextContent();
				txt = txt.replace("&", "&amp").replace("<", "&lt;").replace(">", "&gt;").replaceAll("[ \t\r\n]+", " ");
				verse.append(txt);
			} else {
				Element elem = (Element) node;
				if (elem.getNodeName().equals("NOTE")) {
					String txt = elem.getTextContent(), content;
					if (txt.startsWith("<html>"))
						content = txt.substring(6);
					else
						content = txt.replace("&", "&amp").replace("<", "&lt;").replace(">", "&gt;");
					verse.append(buildFootnote(content, footnotes));
				} else if (elem.getNodeName().equals("BR")) {
					verse.append("<br />");
				} else if (elem.getNodeName().equals("XREF")) {
					for (String ref : elem.getAttribute("fscope").split("; ")) {
						Matcher m = xrefPattern.matcher(ref);
						if (!m.matches())
							throw new IllegalStateException("Invalid XREF: " + ref);
						String book = m.group(1);
						int bookIdx = Arrays.asList(ZEF_SHORT_BOOKS).indexOf(book);
						if (bookIdx != -1)
							book = LOGOS_BOOKS[bookIdx];
						verse.append("<sup>[[ ℘  &gt;&gt; " + getVerseMap(book) + ":" + book + " " + m.group(2) + ":" + m.group(3) + "]]</sup>");
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
		content.append("<span style=\""+css+"\">");
		for (Node node = styleElement.getFirstChild(); node != null; node = node.getNextSibling()) {
			if (node instanceof Text) {
				String txt = ((Text) node).getTextContent();
				txt = txt.replace("&", "&amp").replace("<", "&lt;").replace(">", "&gt;").replaceAll("[ \t\r\n]+", " ");
				content.append(txt);
			} else {
				Element elem = (Element) node;
				if (elem.getNodeName().equals("BR")) {
					content.append("<br />");
				} else if (elem.getNodeName().equals("STYLE")) {
					parseStyle(content, elem);
				} else {
					throw new IllegalStateException("invalid STYLE level tag: " + elem.getNodeName());
				}
			}
		}
		content.append(suffix);
	}

	private static String getVerseMap(String book) {
		return "BibleTOBIBLE";
		// // it should also work to use different verse maps for OT and NT;
		// somehow does not work...
		// int idx = Arrays.asList(LOGOS_BOOKS).indexOf(book);
		// if (idx < 40 || idx > 66) { // OT
		// return "BibleLXXMTPAR";
		// } else {
		// return "BibleNA27";
		// }
	}

	private static int footnoteCounter = 0;
	private static NoteIndexCounter footnoteTextCounter = new NoteIndexCounter();

	private static String buildFootnote(String content, StringBuilder footnotes) {
		footnoteCounter++;
		final String footnoteSymbol;
		if (content.startsWith("[St") && content.endsWith("]")) {
			footnoteSymbol = "Status";
		} else {
			footnoteSymbol = footnoteTextCounter.getNextNoteString();
		}
		footnotes.append("<DIV ID=\"sdfootnote" + footnoteCounter + "\">" + tagForeign(content) + "</DIV>\n");
		return "<A CLASS=\"sdfootnoteanc\" HREF=\"#sdfootnote" + footnoteCounter + "sym\" sdfixed><sup>" + footnoteSymbol + "</sup></A>";
	}

	private static final Pattern NO_GREEK_OR_HEBREW = Pattern.compile("[\\P{IsGreek}&&\\P{IsHebrew}]*");
	private static final Pattern FIND_HEBREW = Pattern.compile("[\\p{IsHebrew}]+([\\p{IsCommon}]+[\\p{IsHebrew}]+)*");
	private static final Pattern FIND_GREEK = Pattern.compile("[\\p{IsGreek}]+([\\p{IsCommon}]+[\\p{IsGreek}]+)*");

	private static String tagForeign(String str) {
		// fast path for when every character is neither greek nor hebrew
		if (NO_GREEK_OR_HEBREW.matcher(str).matches())
			return str;
		// do the tagging
		return tagOne(tagOne(str, FIND_HEBREW, "he-IL"), FIND_GREEK, "el-GR");
	}

	private static String tagOne(String str, Pattern pattern, String languageCode) {
		Matcher m = pattern.matcher(str);
		StringBuffer result = new StringBuffer(str.length());
		while (m.find()) {
			m.appendReplacement(result, "<span lang=\"" + languageCode + "\">$0</span>");
		}
		m.appendTail(result);
		return result.toString();
	}
}
