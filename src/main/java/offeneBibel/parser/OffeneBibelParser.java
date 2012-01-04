package offeneBibel.parser;

import org.parboiled.BaseParser;
import org.parboiled.Rule;
import org.parboiled.annotations.BuildParseTree;
import org.parboiled.support.StringVar;
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
			push(new ObFassungNode(fassung)),
            Optional(Sequence(PoemStart(), ZeroOrMore(Whitespace()))),
            ZeroOrMore(Sequence(Vers(fassung), ZeroOrMore(Whitespace()))),
            "{{Bemerkungen}}",
            peek(1).pushChild(pop())
		);
    }
    
    // "{{" ('S'|'L') '|' NUMBER  "}}" (ws* bibletext)?;
    Rule Vers(ObFassungNode.FassungType fassung) {
    	return Sequence(
    		"{{",
    		FirstOf('S', 'L'), (fassung == ObFassungNode.FassungType.lesefassung && matchedChar() == 'L') || (fassung == ObFassungNode.FassungType.studienfassung && matchedChar() == 'S') ? true : false,
    		'|',
    		Number(), push(new ObVerseNode(Integer.parseInt(match()))),
    		"}}", 
    		Optional(Sequence(ZeroOrMore(Whitespace()), BibleText())),
    		peek(1).pushChild(pop())
    	);
    }
    
    Rule BibleText() {
    	return OneOrMore(
			FirstOf(
				Sequence(ScriptureText(), peek().pushChild(new ObAstNode(ObAstNode.NodeType.text, match()))), // this might cause a problem because OneOrMore() fights with ScriptureText() for the chars
				BibleTextMarkup()
			)
    	);
    }
    
    Rule BibleTextMarkup() {
    	return FirstOf(
			Emphasis(),
    		Insertion(),
    		Omission(),
    		Alternative(),
    		PoemStart(),
    		PoemStop(),
    		Break(),
    		ParallelPassage(),
    		Note()
    	);
    }
    
    Rule Emphasis() {
    	return Sequence(
    		"''",
    		ScriptureText(), peek().pushChild(new ObAstNode(ObAstNode.NodeType.emphasis, match())),
    		"''"
    	);
    }
    
    Rule Insertion() {
    	return Sequence(
    		'[',
    		ScriptureText(), peek().pushChild(new ObAstNode(ObAstNode.NodeType.insertion, match())),
    		']'
    	);
    }
    
    Rule Omission() {
    	return Sequence(
    		'{',
    		ScriptureText(), peek().pushChild(new ObAstNode(ObAstNode.NodeType.omission, match())),
    		'}'
    	);
    }
    
    Rule Alternative() {
    	return Sequence(
			'(',
			push(new ObAstNode(ObAstNode.NodeType.alternative)),
			OneOrMore(FirstOf(
				ScriptureText(),
				Insertion()
			)),
			')',
    		peek(1).pushChild(pop())
    	);
    }
    
    Rule PoemStart() {
    	return Sequence(
    			String("<poem>"),
    			peek().pushChild(new ObAstNode(ObAstNode.NodeType.poemStart))
    			); // todo: create poem stack
    }
    
    Rule PoemStop() {
    	return Sequence(
    			String("</poem>"),
    			peek().pushChild(new ObAstNode(ObAstNode.NodeType.poemStop))
    			); // todo: create poem stack
    }
    
    Rule Break() {
    	return Sequence(String("<br/>"),
    			peek().pushChild(new ObAstNode(ObAstNode.NodeType.textBreak))
    			);
    }
    
    // '{{par|' book '|' NUMBER '|' NUMBER ('|' NUMBER)? DOUBLEBRACECLOSE
    Rule ParallelPassage() { // todo: code to verify passage
    	StringVar bookName = new StringVar();
    	Var<Integer> chapter = new Var<Integer>();
    	Var<Integer> startVerse = new Var<Integer>();
    	Var<Integer> stopVerse = new Var<Integer>(-1);
    	return Sequence(
			"{{Par|",
			Book(), bookName.set(match()),
			'|',
			Number(), chapter.set(Integer.parseInt(match())),
			'|',
			Number(), startVerse.set(Integer.parseInt(match())),
			Optional(Sequence(
				'|',
				Number(), stopVerse.set(Integer.parseInt(match()))
			)),
			"}}", peek().pushChild(new ObParallelPassageNode(bookName.get(), chapter.get().intValue(), startVerse.get().intValue(), stopVerse.get().intValue()))
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
    			"<ref>",
    			NoteText(),
    			"</ref>",
    			peek().pushChild(new ObNoteNode())
    		),
    		Sequence(
    			"<ref name=\"",
    			TagText(), tagText.set(match()),
    			"\"",
    			ZeroOrMore(Whitespace()),
    			"/>",
    			peek().pushChild(new ObNoteNode(tagText.get()))
    		),
    		Sequence(
    			"<ref name=\"",
    			TagText(), tagText.set(match()),
    			"\"",
    			ZeroOrMore(Whitespace()),
    			">", push(new ObNoteNode(tagText.get())),
    			NoteText(),
    			"</ref>",
    			peek(1).pushChild(pop())
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
    		Hebrew(),
    		WikiLink(),
    		SuperScript(),
    		Break()
    	);
    }
    
    Rule Hebrew() {
    	return Sequence(
    		"{{Hebr}}",
    		ZeroOrMore(Whitespace()),
    		HebrewText(), peek().pushChild(new ObHebrewTextNode(match())),
    		"{{Hebr ende}}"
    		
    	);
    }
    
    Rule WikiLink() {
    	StringVar url = new StringVar();
    	return FirstOf(
    		Sequence(
	    		"[[",
	    		UrlText(), url.set(match()),
	    		'|',
	    		ScriptureText(), peek().pushChild(new ObWikiLinkNode(match(), url.get())),
	    		"]]"
    		),
    		Sequence(
	    		"[",
	    		UrlText(), url.set(match()),
	    		' ',
	    		ScriptureText(), peek().pushChild(new ObWikiLinkNode(match(), url.get())),
	    		"]"
    		)
    	);
    }
    
    Rule SuperScript() {
    	return Sequence(
    		"<sup>",
    		ScriptureText(), peek().pushChild(new ObSuperScriptTextNode(match())),
    		"</sup>"
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
    		"1 Samuel",
    		"2 Samuel",
    		"1 Könige",
    		"2 Könige",
    		"1 Chronik",
    		"2 Chronik",
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
    		"Matthäus",
			"Markus",
			"Lukas",
			"Johannes",
			"Apostelgeschichte",
			"Römer",
			"1 Korinther",
			"2 Korinther",
			"Galater",
			"Epheser",
			"Philipper",
			"Kolosser",
			"1 Thessalonicher",
			"2 Thessalonicher",
			"1 Timotheus",
			"2 Timotheus",
			"Titus",
			"Philemon",
			"Hebräer",
			"Jakobus",
			"1 Petrus",
			"2 Petrus",
			"1 Johannes",
			"2 Johannes",
			"3 Johannes",
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
    	return OneOrMore(FirstOf(
    		CharRange('\u0590', '\u05ff'), // hebrew alphabet
    		CharRange('\ufb1d', '\ufb4f')  // hebrew presentation forms
    	));
    }
    
    Rule UrlText() {
    	return OneOrMore(FirstOf(
    		CharRange('a', 'z'),
    		CharRange('A', 'Z'),
    		CharRange('0', '9'),
    		'-',
    		'.',
    		':',
    		'/',
    		'?',
    		'$'
    	));
    }
    
    Rule NoteTextText() {
    	return OneOrMore(FirstOf(
    		ScriptureChar(),
    		'(',
    		')',
    		'[',
    		']',
    		'\u00AB', // »
    		'\u00BB' // «
    	));
    }
    
    Rule ScriptureText() {
    	return OneOrMore(ScriptureChar());
    }
    
    Rule ScriptureChar() {
    	return FirstOf(
    		'\u0021', // !
    		CharRange('\u002c', '\u002f'), // , - . /
    		//CharRange('\u2010', '\u2015'), // all sorts of dashes
    		'\u2013', // – <- this is a EN DASH
    		CharRange('\u0030', '\u003b'), // 0-9 : ;
    		'\u003f', // ?
    		CharRange('\u0041', '\u005a'), // A-Z
    		CharRange('\u0061', '\u007a'), // a-z
    		'\u00c4', // Ä
    		'\u00d6', // Ö
    		'\u00dc', // Ü
    		'\u00df', // ß
    		'\u00e4', // ä
    		'\u00f6', // ö
    		'\u00fc', // ü
    		'\u201e', // „
    		'\u201c', // “
    		Whitespace()
    	);
    }
    
    Rule TagChar() {
    	return FirstOf(
        	CharRange('a', 'z'),
        	CharRange('A', 'Z')
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
}