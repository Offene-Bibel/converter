package offeneBibel.parser;

import offeneBibel.parser.ObAstNode.NodeType;

import org.parboiled.Action;
import org.parboiled.BaseParser;
import org.parboiled.Context;
import org.parboiled.Rule;
import org.parboiled.annotations.BuildParseTree;
import org.parboiled.support.StringVar;
import org.parboiled.support.ValueStack;
import org.parboiled.support.Var;

@BuildParseTree
public class OffeneBibelParser extends BaseParser<ObAstNode> {

	//page	:	ws* (chaptertag ws*)* lesefassung ws* studienfassung ws* '{{Kapitelseite Fuß}}' ws* EOF;
    public Rule Page() {
        return Sequence(
        	push(new ObChapterNode()),
            ZeroOrMore(Whitespace()),
            ZeroOrMore(Sequence(ChapterTag(), ZeroOrMore(Whitespace()))),
            "{{Lesefassung}}",
            ZeroOrMore(Whitespace()),
            Fassung(ObFassungNode.FassungType.lesefassung),
            ZeroOrMore(Whitespace()),
            "{{Studienfassung}}",
            ZeroOrMore(Whitespace()),
            Fassung(ObFassungNode.FassungType.studienfassung),
            ZeroOrMore(Whitespace()),
            "{{Kapitelseite Fuß}}",
            ZeroOrMore(Whitespace()),
            EOI
        );
    }
    
    /*
    chaptertag	:	"{{" chaptertagtype "}}"
	|	"{{" chaptertagtype '|Vers ' NUMBER '-' NUMBER "}}"
	;
     */
    Rule ChapterTag() {
    	Var<ObChapterTag.ChapterTagName> tagName = new Var<ObChapterTag.ChapterTagName>();
    	Var<Integer> startVerse = new Var<Integer>();
    	Var<Integer> stopVerse = new Var<Integer>();
    	return FirstOf(
    		Sequence("{{", ChapterTagType(tagName), "}}", ((ObChapterNode)peek()).addChapterTag(new ObChapterTag(tagName.get()))),
    		Sequence("{{",
    				ChapterTagType(tagName), "|Vers ",
    				Number(), startVerse.set(Integer.parseInt(match())), '-',
    				Number(), stopVerse.set(Integer.parseInt(match())), "}}",
    				((ObChapterNode)peek()).addChapterTag(new ObChapterTag(tagName.get(), startVerse.get(), stopVerse.get()))
    				)
    	);
    }
    
    Rule ChapterTagType(Var<ObChapterTag.ChapterTagName> tagName) {
    	return FirstOf(
			Sequence("Lesefassung in Arbeit", tagName.set(ObChapterTag.ChapterTagName.lesefassunginArbeit)),
			Sequence("Studienfassung in Arbeit", tagName.set(ObChapterTag.ChapterTagName.studienfassunginArbeit)),
			Sequence("Lesefassung zu prüfen", tagName.set(ObChapterTag.ChapterTagName.lesefassungZuPruefen)),
			Sequence("Studienfassung zu prüfen", tagName.set(ObChapterTag.ChapterTagName.studienfassungZuPruefen)),
			Sequence("Studienfassung liegt in Rohübersetzung vor", tagName.set(ObChapterTag.ChapterTagName.studienfassungLiegtInRohuebersetzungVor)),
			Sequence("Studienfassung erfüllt die meisten Kriterien", tagName.set(ObChapterTag.ChapterTagName.studienfassungErfuelltDieMeistenKriterien)),
			Sequence("Studienfassung und Lesefassung erfüllen die Kriterien", tagName.set(ObChapterTag.ChapterTagName.studienfassungUndLesefassungErfuellenDieKriterien)),
			Sequence("Überprüfung angefordert", tagName.set(ObChapterTag.ChapterTagName.ueberpruefungAngefordert)),
			Sequence("Vers unvollständig übersetzt", tagName.set(ObChapterTag.ChapterTagName.versUnvollstaendigUebersetzt))
    	);
    }
    
    //(poemstart ws*)? (vers ws*)* '{{Bemerkungen}}';
    Rule Fassung(ObFassungNode.FassungType fassung) {
    	return Sequence(
    			ZeroOrMore(Whitespace()),
    			push(new ObFassungNode(fassung)),
    			OneOrMore(FirstOf(
    				BibleText(),
    	    		Verse(),
    	    		Heading(),
    	    		Note()
    			)),
		        "{{Bemerkungen}}",
		        Optional(
		        	push(new ObAstNode(NodeType.fassungNotes)),
		        	ZeroOrMore(Whitespace()),
		        	NoteText(),
		        	peek(1).insertChild(0, pop()) // put the notes of the Fassung at the beginning
		        ),
		        peek(1).appendChild(pop())
		);
    }
    
    Rule BibleText() {
    	return OneOrMore(
			FirstOf(
				ScriptureText(),
				Quote(),
				Emphasis(),
				Italics(),
	    		Insertion(),
	    		Omission(),
	    		Alternative(),
	    		PoemStart(),
	    		PoemStop(),
	    		Break(),
	    		ParallelPassage()
			)
    	);
    }
    
    // "{{" ('S'|'L') '|' NUMBER  "}}" (ws* bibletext)?;
    Rule Verse() {
    	return Sequence(
    		ZeroOrMore(Whitespace()),
    		"{{",
    		FirstOf('S', 'L'),
    		new Action<ObAstNode>() {
    			public boolean run(Context<ObAstNode> context) {
                	ObFassungNode.FassungType fassung;
					try {
						fassung = getCurrentFassung(context.getValueStack());
					} catch (Exception e) {
						e.printStackTrace();
						return false;
					}
	                return ( fassung == ObFassungNode.FassungType.lesefassung && context.getMatch().equals("L") )
	                ||
	                ( fassung == ObFassungNode.FassungType.studienfassung && context.getMatch().equals("S") );
                }
            },
    		'|',
    		Number(), peek().appendChild(new ObVerseNode(Integer.parseInt(match()))),
    		"}}",
    		ZeroOrMore(Whitespace())
    	);
    }

    Rule Heading() {
    	return Sequence(
    			ACTION( ((ObFassungNode)(peek())).getFassung() == ObFassungNode.FassungType.lesefassung ),
    			"((",
        		push(new ObAstNode(ObAstNode.NodeType.heading)),
        		ScriptureText(),
        		"))",
        		peek(1).appendChild(pop())
    	);
    }

    Rule Quote() {
    	return Sequence(
    		'\u201e', // „
    		push(new ObAstNode(ObAstNode.NodeType.quote)),
    		OneOrMore(FirstOf(
    				BibleText(),
    				InnerQuote(),
    	    		Verse(),
    	    		Note()
        	)),
    		'\u201c', // “
    		peek(1).appendChild(pop())
    	);
    }

    Rule InnerQuote() {
    	return Sequence(
    		'\u00BB', // »
    		push(new ObAstNode(ObAstNode.NodeType.quote)),
    		OneOrMore(FirstOf(
	    		BibleText(),
	    		Verse(),
	    		Note()
            )),
    		'\u00AB', // «
    		peek(1).appendChild(pop())
    	);
    }

    Rule Emphasis() {
    	return Sequence(
    		"''",
    		push(new ObAstNode(ObAstNode.NodeType.emphasis)),
    		OneOrMore(FirstOf(
        			ScriptureText(),
    	    		Quote(),
    				Italics(),
    	    		Insertion(),
    	    		Alternative(),
    	    		Break(),
    	    		ParallelPassage(),
    	    		Note()
        		)),
    		"''",
    		peek(1).appendChild(pop())
    	);
    }
    
    Rule Italics() {
    	return FirstOf(
			Sequence(
	    		"<em>",
	    		push(new ObAstNode(ObAstNode.NodeType.italics)),
	    		OneOrMore(FirstOf(
	        			ScriptureText(),
	    	    		Quote(),
	    				Emphasis(),
	    	    		Insertion(),
	    	    		Alternative(),
	    	    		Break(),
	    	    		ParallelPassage(),
	    	    		Note()
	        		)),
	    		"</em>",
	    		peek(1).appendChild(pop())
	    	),
	    	Sequence(
	    		"<i>",
	    		push(new ObAstNode(ObAstNode.NodeType.italics)),
	    		OneOrMore(FirstOf(
	        			ScriptureText(),
	    	    		Quote(),
	    				Emphasis(),
	    	    		Insertion(),
	    	    		Alternative(),
	    	    		Break(),
	    	    		ParallelPassage(),
	    	    		Note()
	        		)),
	    		"</i>",
	    		peek(1).appendChild(pop())
	    	)
		    );
	    	
    }
    
    Rule NoteEmphasis() {
    	return Sequence(
    		"''",
    		push(new ObAstNode(ObAstNode.NodeType.emphasis)),
    		NoteTextText(),
    		"''",
    		peek(1).appendChild(pop())
    	);
    }
    
    Rule NoteItalics() {
    	return FirstOf(
    		Sequence(
	    		"<em>",
	    		push(new ObAstNode(ObAstNode.NodeType.italics)),
	    		NoteTextText(),
	    		"</em>",
	    		peek(1).appendChild(pop())
    		),
    		Sequence(
	    		"<i>",
	    		push(new ObAstNode(ObAstNode.NodeType.italics)),
	    		NoteTextText(),
	    		"</i>",
	    		peek(1).appendChild(pop())
        	)
    	);
    }
    
    Rule Insertion() {
    	return Sequence(
    		'[',
    		push(new ObAstNode(ObAstNode.NodeType.insertion)),
    		OneOrMore(FirstOf(
        			ScriptureText(),
    	    		Quote(),
    				Emphasis(),
    				Italics(),
    	    		Alternative(),
    	    		Break(),
    	    		ParallelPassage(),
    	    		Note()
        		)),
    		']',
    		peek(1).appendChild(pop())
    	);
    }
    
    Rule Omission() {
    	return Sequence(
    		'{',
    		push(new ObAstNode(ObAstNode.NodeType.omission)),
    		OneOrMore(FirstOf(
        			ScriptureText(),
    	    		Quote(),
    				Emphasis(),
    				Italics(),
    	    		Insertion(),
    	    		Alternative(),
    	    		Break(),
    	    		ParallelPassage(),
    	    		Note()
        		)),
    		'}',
    		peek(1).appendChild(pop())
    	);
    }
    
    Rule Alternative() {
    	return Sequence(
			'(',
			push(new ObAstNode(ObAstNode.NodeType.alternative)),
			OneOrMore(FirstOf(
				ScriptureText(),
				Quote(),
				Insertion(),
	    		Alternative(),
	    		Note()
			)),
			')',
    		peek(1).appendChild(pop())
    	);
    }
    
    Rule PoemStart() {
    	return Sequence(
    			"<poem>",
    			peek().appendChild(new ObAstNode(ObAstNode.NodeType.poemStart))
    			); // todo: create poem stack
    }
    
    Rule PoemStop() {
    	return Sequence(
    			"</poem>",
    			peek().appendChild(new ObAstNode(ObAstNode.NodeType.poemStop))
    			); // todo: create poem stack
    }
    
    Rule Break() {
    	return Sequence("<br/>",
    			peek().appendChild(new ObAstNode(ObAstNode.NodeType.textBreak))
    			);
    }
    
    // '{{par|' book '|' NUMBER '|' NUMBER ('|' NUMBER)? DOUBLEBRACECLOSE
    Rule ParallelPassage() { // todo: code to verify passage
    	StringVar bookName = new StringVar();
    	Var<Integer> chapter = new Var<Integer>();
    	Var<Integer> startVerse = new Var<Integer>();
    	Var<Integer> stopVerse = new Var<Integer>(-1);
    	return Sequence(
			FirstOf("{{Par|", "{{par|"),
			Book(), bookName.set(match()),
			'|',
			Number(), chapter.set(Integer.parseInt(match())),
			'|',
			Number(), startVerse.set(Integer.parseInt(match())),
			Optional(Sequence(
				'|',
				Number(), stopVerse.set(Integer.parseInt(match()))
			)),
			"}}", peek().appendChild(new ObParallelPassageNode(bookName.get(), chapter.get().intValue(), startVerse.get().intValue(), stopVerse.get().intValue()))
    	);
    }

    /*
      '<ref>' notetext '</ref>'
	  '<ref name="' tagtext '"' ws* noteend
	*/
    Rule Note() {
    	StringVar tagText = new StringVar();
    	return FirstOf(
    		Sequence(
    			"<ref>", push(new ObNoteNode()),
    			NoteText(),
    			"</ref>",
    			peek(1).appendChild(pop())
    		),
    		Sequence(
    			"<ref name=\"",
    			TagText(), tagText.set(match()),
    			"\"",
    			ZeroOrMore(Whitespace()),
    			"/>",
    			peek().appendChild(new ObNoteLinkNode(tagText.get()))
    		),
    		Sequence(
    			"<ref name=\"",
    			TagText(), tagText.set(match()),
    			"\"",
    			ZeroOrMore(Whitespace()),
    			">", push(new ObNoteNode(tagText.get())),
    			NoteText(),
    			"</ref>",
    			peek(1).appendChild(pop())
    		)
    	);
    }
    
    Rule TagText() {
    	return OneOrMore(TagChar());
    }
    
    Rule NoteText() {
    	return OneOrMore(FirstOf(
    			NoteTextText(),
    			NoteMarkup()
    	));
    }
    
    Rule NoteMarkup() {
    	return FirstOf(
    		BibleTextQuote(),
    		NoteInnerQuote(),
    		NoteEmphasis(),
			NoteItalics(),
    		Hebrew(),
    		WikiLink(),
    		NoteSuperScript(),
    		Break()
    	);
    }
    
    Rule NoteInnerQuote() {
    	return Sequence(
    		'\u00AB', // »
    		NoteText(),
    		'\u00BB' // «
    	);
    }

    Rule BibleTextQuote() {
    	return Sequence(
        	'\u201e', // „
        	NoteText(),
    		//BibleText(),
        	'\u201c' // “
    		
    	);
    }
    
    Rule Hebrew() {
    	return Sequence(
    		FirstOf("{{Hebr}}", "{{hebr}}"),
    		ZeroOrMore(Whitespace()),
    		push(new ObAstNode(NodeType.hebrew)),
    		HebrewText(),
    		FirstOf("{{Hebr ende}}", "{{hebr ende}}"),
    		peek(1).appendChild(pop())
    	);
    }
    
    Rule WikiLink() {
    	return FirstOf(
    		Sequence(
	    		"[[",
	    		OneOrMore(NoneOf("|]")),
	    		push(new ObWikiLinkNode(match())),
	    		Optional(
		    		'|',
		    		NoteTextText()
		    	),
	    		"]]",
	    		peek(1).appendChild(pop())
    		),
    		Sequence(
	    		"[",
	    		Sequence(
	        			"http://",
	        			OneOrMore(NoneOf(" ]"))
	        	),
	    		push(new ObWikiLinkNode(match())),
	    		Optional(
		    		' ',
		    		NoteTextText()
	    		),
	    		"]",
	    		peek(1).appendChild(pop())
    		)
    	);
    }
    
    Rule SuperScript() {
    	return Sequence(
    		"<sup>",
    		push(new ObSuperScriptTextNode()),
    		ScriptureText(),
    		"</sup>",
    		peek(1).appendChild(pop())
    	);
    }
    
    Rule NoteSuperScript() {
    	return Sequence(
    		"<sup>",
    		push(new ObSuperScriptTextNode()),
    		NoteTextText(),
    		"</sup>",
    		peek(1).appendChild(pop())
    	);
    }

    Rule Book() {
    	return FirstOf(
			OtBook(),
			NtBook(),
			ApBook()
    	);
    }
    
    Rule OtBook() {
    	return FirstOf(
    		"Genesis",
    		"Exodus",
    		"Levitikus",
    		"Numeri",
    		"Deuteronomium",
    		"Josua",
    		"Richter",
    		"Rut",
    		"1 Samuel", "1_Samuel",
    		"2 Samuel", "2_Samuel",
    		"1 Könige", "1_Könige",
    		"2 Könige", "2_Könige",
    		"1 Chronik", "1_Chronik",
    		"2 Chronik", "2_Chronik",
    		"Esra",
    		"Nehemia",
    		"Ester",
    		"Ijob",
    		"Psalm",
    		"Sprüche",
    		"Kohelet",
    		"Hohelied",
    		"Jesaja",
    		"Jeremia",
    		"Klagelieder",
    		"Ezechiel",
    		"Daniel",
    		"Hosea",
    		"Joel",
    		"Amos",
    		"Obadja",
    		"Jona",
    		"Micha",
    		"Nahum",
    		"Habakuk",
    		"Zefanja",
    		"Haggai",
    		"Sacharja",
    		"Maleachi"
    	);
    }
    
    Rule NtBook() {
    	return FirstOf(
    		"Matthäus", "Mt",
			"Markus", "Mk",
			"Lukas",
			"Johannes", "Joh",
			"Apostelgeschichte", "Apg",
			"Römer",
			"1 Korinther", "1_Korinther",
			"2 Korinther", "2_Korinther",
			"Galater",
			"Epheser", "Eph",
			"Philipper",
			"Kolosser", "Kol",
			"1 Thessalonicher", "1_Thessalonicher",
			"2 Thessalonicher", "2_Thessalonicher",
			"1 Timotheus", "1_Timotheus",
			"2 Timotheus", "2_Timotheus",
			"Titus",
			"Philemon",
			"Hebräer",
			"Jakobus",
			"1 Petrus", "1_Petrus",
			"2 Petrus", "2_Petrus",
			"1 Johannes", "1_Johannes",
			"2 Johannes", "2_Johannes",
			"3 Johannes", "3_Johannes",
			"Judas",
			"Offenbarung"
    	);
    }
    
    Rule ApBook() {
    	return FirstOf(
			"Baruch",
			"Judit",
			"1 Makkabäer",
			"2 Makkabäer",
			"Jesus Sirach",
			"Tobit",
			"Weisheit"
    	);
    }
    
    Rule HebrewText() {
    	return Sequence(
    			OneOrMore(FirstOf(
		    		CharRange('\u0590', '\u05ff'), // hebrew alphabet
		    		CharRange('\ufb1d', '\ufb4f'),  // hebrew presentation forms
		    		PunctuationChar()
    			)),
    			peek().appendChild(new ObTextNode(match()))
    		);
    }
    
    Rule NoteTextText() {
    	return Sequence(OneOrMore(NoteTextChar()), peek().appendChild(new ObTextNode(match())));
    }
    
    Rule ScriptureText() {
    	return Sequence(OneOrMore(TextChar()), peek().appendChild(new ObTextNode(match()))); // this might cause a problem because OneOrMore() fights with ScriptureText() for the chars
    }
    
    Rule TextChar() {
    	return FirstOf(
    		// C0 Controls and Basic Latin, http://unicode.org/charts/PDF/U0000.pdf
	    		CharRange('\u0041', '\u005a'), // A-Z
	    		CharRange('\u0061', '\u007a'), // a-z
    		// C1 Controls and Latin-1 Supplement, http://unicode.org/charts/PDF/U0080.pdf
	    		CharRange('\u00c0', '\u00d6'), // all letters from C1
	    		CharRange('\u00d8', '\u00f6'), // all letters from C1
	    		CharRange('\u00f8', '\u00ff'), // all letters from C1
	    		/*'\u00c4', // Ä
	    		'\u00d6', // Ö
	    		'\u00dc', // Ü
	    		'\u00df', // ß
	    		'\u00e4', // ä
	    		'\u00f6', // ö
	    		'\u00fc', // ü
	    		*/
    		PunctuationChar()
    	);
    }
    
    Rule PunctuationChar() {
    	return FirstOf(
        		// C0 Controls and Basic Latin, http://unicode.org/charts/PDF/U0000.pdf
    				'=', // TODO: DELETE THIS
    	    		'\u0021', // !
    	    		'\'', // \u0027
    	    		CharRange('\u002c', '\u002f'), // , - . /
    	    		CharRange('\u0030', '\u003b'), // 0-9 : ;
    	    		'\u003f', // ?
        		// General Punctuation, http://unicode.org/charts/PDF/U2000.pdf
    	    		'\u2013', // – en dash
    	    		'\u2014', // — em dash
    	    		'\u2026', // …
    	    		//CharRange('\u2010', '\u2015'), // all sorts of dashes
        		// Latin Extended Additional, http://www.unicode.org/charts/PDF/U1E00.pdf
        			CharRange('\u1e00', '\u1eff'), 
        		Whitespace()
        	);
    }
    
    Rule GreekTextChar() {
    	return OneOrMore(
    		// Greek and Coptic, http://unicode.org/charts/PDF/U0370.pdf
    		CharRange('\u0370', '\u03ff')
    	);
    }
    
    Rule NoteTextChar() {
    	return OneOrMore(
    		NoneOf("><[]'„“»«{}")
    		/*
    		FirstOf(
	    		TextChar(),
	    		GreekTextChar(),
	    		'(',
	    		')',
	    		'=',
	    		'+', // \u002b
	    		'~',
	    		'\u2192', // →
	    		'\u00a7', // §
	    		'\u00a9', // ©
	    		'\u00ae' // ®
    		)
    		*/
    	);
    }
    
    Rule TagChar() {
    	return FirstOf(
        	CharRange('a', 'z'),
        	CharRange('A', 'Z'),
        	'_'
        );
    }
    
    Rule Number() {
        return Sequence(
        	CharRange('1', '9'),
        	ZeroOrMore(CharRange('0', '9'))
        );
    }
    
    Rule Whitespace() {
    	return FirstOf(
    			' ',
    			'\n',
    			'\t',
    			'\r'
    	);
    }
    
	private static ObFassungNode.FassungType getCurrentFassung(ValueStack<ObAstNode> valueStack) throws Exception
	{
		for(ObAstNode node : valueStack) {
			if(node.getNodeType() == ObAstNode.NodeType.fassung) {
				return ((ObFassungNode)node).getFassung();
			}
		}
		throw new Exception("No Fassung found.");
	}
}