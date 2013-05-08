package offeneBibel.parser;

import offeneBibel.parser.ObAstNode.NodeType;

import org.parboiled.Action;
import org.parboiled.BaseParser;
import org.parboiled.Context;
import org.parboiled.Rule;
import org.parboiled.annotations.BuildParseTree;
import org.parboiled.annotations.SkipNode;
import org.parboiled.annotations.SuppressNode;
import org.parboiled.annotations.SuppressSubnodes;
import org.parboiled.support.MatcherPath;
import org.parboiled.support.StringVar;
import org.parboiled.support.ValueStack;
import org.parboiled.support.Var;

/**
 * This is the parser class that processes an Offene Bibel wiki page and turns it into
 * a {@link ObAstNode} tree. It is based on parboiled.
 * @see <a href="www.parboiled.org/">www.parboiled.org/</a>
 */
@BuildParseTree
public class OffeneBibelParser extends BaseParser<ObAstNode> {

    //page    :    ws* (chaptertag ws*)* lesefassung ws* studienfassung ws* '{{Kapitelseite Fuß}}' ws* EOF;
    public Rule Page() {
        return Sequence(
            push(new ObChapterNode()),
            ZeroOrMore(Whitespace()),
            Optional(Sequence(ChapterNotes(), ZeroOrMore(Whitespace()))),
            ZeroOrMore(Sequence(ChapterTag(), ZeroOrMore(Whitespace()))),
            Optional(Sequence("<small>", FreeWikiText(), "</small>"), ZeroOrMore(Whitespace())), // just ignore alternate readings for now
            "{{Lesefassung}}",
            ZeroOrMore(Whitespace()),
            FirstOf(
                "''(kommt später)''",
                Fassung(ObFassungNode.FassungType.lesefassung)
            ),
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
    
    Rule ChapterNotes() {
        return Sequence(
                "==Einführende Bemerkungen==",
                push(new ObAstNode(NodeType.chapterNotes)),
                ZeroOrMore(Whitespace()),
                FreeWikiText(),
                peek(1).appendChild(pop())
        );
    }
    
    Rule FreeWikiText() {
        return OneOrMore(FirstOf(
                NoteTextText(),
                BibleTextQuote(),
                NoteInnerQuote(),
                NoteEmphasis(),
                NoteItalics(),
                Hebrew(),
                WikiLink(),
                NoteSuperScript(),
                Break(),
                Note()
        ));
    }
    
    /*
    chaptertag    :    "{{" chaptertagtype "}}"
    |    "{{" chaptertagtype '|Vers ' NUMBER '-' NUMBER "}}"
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
                    Number(), safeParseIntSet(startVerse), FirstOf('-', " und "),
                    Number(), safeParseIntSet(stopVerse), "}}",
                    ((ObChapterNode)peek()).addChapterTag(new ObChapterTag(tagName.get(), startVerse.get(), stopVerse.get()))
                    ),
            Sequence("{{",
                    ChapterTagType(tagName), "|Vers ",
                    Number(), safeParseIntSet(startVerse),
                    "}}",
                    ((ObChapterNode)peek()).addChapterTag(new ObChapterTag(tagName.get(), startVerse.get(), startVerse.get()))
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
            Sequence("Lesefassung erfüllt die meisten Kriterien", tagName.set(ObChapterTag.ChapterTagName.lesefassungErfuelltDieMeistenKriterien)),
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
                Optional(Sequence(
                        push(new ObAstNode(NodeType.fassungNotes)),
                        ZeroOrMore(Whitespace()),
                        NoteText(),
                        peek(1).insertChild(0, pop()) // put the notes of the Fassung at the beginning
                    )
                ),
                peek(1).appendChild(pop())
        );
    }
    
    Rule BibleText() {
        return OneOrMore(
            FirstOf(
                ScriptureText(),
                LineQuote(),
                Quote(),
                Emphasis(),
                Italics(),
                Insertion(),
                Omission(),
                Alternative(),
                AlternateReading(),
                PoemStart(),
                PoemStop(),
                Break(),
                ParallelPassage()
            )
        );
    }
    
    // "{{" ('S'|'L') '|' NUMBER  "}}";
    Rule Verse() {
        Var<Integer> verseFromNumber = new Var<Integer>();
        Var<Integer> verseToNumber = new Var<Integer>();
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
            FirstOf(
                Sequence(
                    Number(), safeParseIntSet(verseFromNumber),
                    FirstOf("/", "-"),
                    Number(), safeParseIntSet(verseToNumber)
                ),
                Sequence(
                    Number(), safeParseIntSet(verseFromNumber), verseToNumber.set(verseFromNumber.get())
                )
            ),
            "}}",
            peek().appendChild(new ObVerseNode(verseFromNumber.get(), verseToNumber.get())),
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

    /**
     * Since quotes sometimes start in one chapter and end in a different one it is not always possible to
     * completely match a quote when parsing chapters separately. Thus we only optionally match the closing tag for now.
     * @return
     */
    Rule Quote() {
        return Sequence(
            /** {@link InnerQuotes} could contain {@link Quotes} via the {@link BibleText}, to prevent this we check here. */
            ACTION(false == isRuleParent("InnerQuote")),
            '\u201e', // „
            push(new ObAstNode(ObAstNode.NodeType.quote)),
            OneOrMore(FirstOf(
                    BibleText(),
                    InnerQuote(),
                    Verse(),
                    Note()
            )),
            // TODO: Once chapters are not parsed separately anymore quotes should forcefully match the closing tag again.
            Optional('\u201c'), // “
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
    
    /**
     * Quotes that are realized with the MediaWiki <i>Definition List</i> syntax.
     * In this syntax each line has to start with a ":". The quote extends until the first line without a ":" is found.
     * To prevent {@link Whitespace} eating up "\n" and thus preventing this rule to finish some special logic was
     * added there.
     */
    Rule LineQuote() {
        return FirstOf(
            /**
             * {@link LineQuotes} can not be matched in a completely hierarchical fashion, because the "\n:" can occur inside
             * other elements. Thus we need a way to eat up "\n:" inside elements nested in {@link LineQuote}s. This is done here.
             */
            Sequence(
                isRuleParent("LineQuote"),
                "\n:"
            ),
            /**
             * This is the actual {@link LineQuote} matching part.
             */
            Sequence(
                /**
                 * Without the <i>breakRecursion()</i> there would be a new {@link LineQuote} for every "\n:" found.
                 */
                breakRecursion(),
                "\n:",
                push(new ObAstNode(ObAstNode.NodeType.quote)),
                OneOrMore(FirstOf(
                        BibleText(),
                        Verse(),
                        Note()
                )),
                '\n',
                peek(1).appendChild(pop())
            )
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
                    AlternateReading(),
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
                        AlternateReading(),
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
                        AlternateReading(),
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
            breakRecursion(),
            "''",
            push(new ObAstNode(ObAstNode.NodeType.emphasis)),
            NoteTextWithBreaker(new StringVar("''")),
            "''",
            peek(1).appendChild(pop())
        );
    }
    
    Rule NoteItalics() {
        return FirstOf(
            Sequence(
                breakRecursion(),
                "<em>",
                push(new ObAstNode(ObAstNode.NodeType.italics)),
                NoteTextText(),
                "</em>",
                peek(1).appendChild(pop())
            ),
            Sequence(
                breakRecursion(),
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
                    AlternateReading(),
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
            /*
             * For a little better error reporting we catch the most often encountered false positives, before
             * consuming input.
             */
            TestNot(FirstOf("{{Studienfassung}}", "{{Lesefassung}}", "{{Bemerkungen}}", "{{Kapitelseite Fuß}}")),
            '{',
            push(new ObAstNode(ObAstNode.NodeType.omission)),
            OneOrMore(FirstOf(
                    ScriptureText(),
                    Quote(),
                    Emphasis(),
                    Italics(),
                    Insertion(),
                    Alternative(),
                    AlternateReading(),
                    Break(),
                    ParallelPassage(),
                    Note(),
                    Omission()
                )),
            '}',
            // Prevent "{{blabla}}", an omission that contains only one other omission.
            ACTION( peek().childCount()!=1 || ((ObAstNode)(peek().peekChild())).getNodeType()!=NodeType.omission),
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
                AlternateReading(),
                Omission(),
                Note()
            )),
            ')',
            //prevent "((blabla))", an alternative that contains only one other alternative
            ACTION( peek().childCount()!=1 || ((ObAstNode)(peek().peekChild())).getNodeType()!=NodeType.alternative),
            peek(1).appendChild(pop())
        );
    }
    
    /**
     * TODO: Flesh out alternate readings better.
     * Probably should be put in it's own node.
     * @return
     */
    Rule AlternateReading() {
        return Sequence(
            "(/",
            Optional(
                OneOrMore(
                    FirstOf(
                        '|',
                        Sequence(
                            TestNot('/'),
                            TextChar()
                        )
                    )
                ),
                peek().appendChild(new ObTextNode(match())),
                '/'
            ),
            OneOrMore(
                TestNot('/'),
                TextChar()
            ),
            '/',
            OneOrMore(
                TestNot('/'),
                TextChar()
            ),
            "/)"
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
        return Sequence(FirstOf("<br/>", "<br />", "<br>"),
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
            OneOrMore(NoneOf("|")), bookName.set(match()), ACTION(BookNameHelper.getInstance().isValid(bookName.get())),
            '|',
            Number(), safeParseIntSet(chapter),
            '|',
            Number(), safeParseIntSet(startVerse),
            Optional(Sequence(
                '|',
                Number(), safeParseIntSet(stopVerse)
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
                NoteTextWithBreaker(new StringVar("</ref>")),
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
                NoteTextWithBreaker(new StringVar("</ref>")),
                "</ref>",
                peek(1).appendChild(pop())
            )
        );
    }
    
    @SuppressNode
    Rule TagText() {
        return OneOrMore(TagChar());
    }
    
    Rule NoteInnerQuote() {
        return Sequence(
            '\u00AB', // »
            push(new ObAstNode(ObAstNode.NodeType.quote)),
            NoteTextWithBreaker(new StringVar("" + '\u00BB')),
            '\u00BB', // «
            peek(1).appendChild(pop())
        );
    }

    Rule BibleTextQuote() {
        return Sequence(
            '\u201e', // „
            push(new ObAstNode(ObAstNode.NodeType.quote)),
            NoteBibleQuoteTempFixup(),
            //NoteText(),
            //BibleText(),
            '\u201c', // “
            peek(1).appendChild(pop())
        );
    }

    /**
     * This rule is a hack. 
     * The bible text quote is not used consistently. Thus only allowing {@link BibleText} in a
     * {@link BibleTextQuote} will fail to match in a lot of different places. Thus we accept any
     * input inside such a quote for now.
     */
    Rule NoteBibleQuoteTempFixup() {
        return Sequence(OneOrMore(NoneOf("“")), peek().appendChild(new ObTextNode(match())));
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
                breakRecursion(),
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
                breakRecursion(),
                "[",
                Sequence(
                        "http://",
                        OneOrMore(NoneOf(" ]"))
                ),
                push(new ObWikiLinkNode(match())),
                Optional(
                    ' ',
                    NoteWikiLinkText()
                ),
                "]",
                peek(1).appendChild(pop())
            )
        );
    }
    
    Rule NoteWikiLinkText() {
        return OneOrMore(FirstOf(
                NoteTextText(),
                BibleTextQuote(),
                NoteInnerQuote(),
                NoteEmphasis(),
                NoteItalics(),
                Hebrew(),
                NoteSuperScript(),
                Break()
        ));
    }    
    
    Rule SuperScript() {
        return Sequence(
            breakRecursion(),
            "<sup>",
            push(new ObSuperScriptTextNode()),
            ScriptureText(),
            "</sup>",
            peek(1).appendChild(pop())
        );
    }
    
    Rule NoteSuperScript() {
        return Sequence(
            breakRecursion(),
            "<sup>",
            push(new ObSuperScriptTextNode()),
            NoteTextWithBreaker(new StringVar("</sup>")),
            //NoteTextText(),
            "</sup>",
            peek(1).appendChild(pop())
        );
    }
    
    Rule NoteText() {
        return OneOrMore(FirstOf(
                NoteTextText(),
                NoteMarkup()
        ));
    }

    /**
     * <h1>Why this rule is a hack</h1>
     * This rule is a hack. It has a very bad performance and as far as I can see prevents the validator to work. It is necessary, because
     * on some pages characters that usually trigger a NoteMarkup (><[]'„“»«{}) occur in a note text without actually starting any markup.
     * A simple example would be "3 < 5", here the < would cause the parser to try to search a matching markup rule.
     * I have not yet found a clean way to work around this limitation.
     * <h1>How it works</h1>
     * It is a variation of the {@link NoteText} rule. The difference is, that this version will swallow any
     * text it finds, including characters that usually trigger a different rule (><[]'„“»«{}). The only way to stop this rule to
     * swallow more input is when the given breaker string is found on the input. To differentiate between random input and a NoteMarkup
     * a trick is used. The {@link NoteTextTextWithBreaker} will for every character on the input try every possible NoteMarkup, discard
     * the result and only if no NoteMarkup matched swallow the next character.
     * @param breaker The element that, when seen on the input, causes this rule to stop.
     */
    Rule NoteTextWithBreaker(StringVar breaker) {
        return OneOrMore(
                FirstOf(
                    NoteTextTextWithBreaker(breaker),
                    NoteMarkup()
                )
            );
    }
    
    /**
     * This rule is part of the {@link NoteTextWithBreaker} hack. It looks at the input one character at a time. Each "safe" char is
     * accepted right away. If a trigger char (><[]'„“»«{}) is encountered, it will try to match a NoteMarkup. If the match fails the
     * char is obviously not the start of a markup rule and is swallowed. If the NoteMarkup matches though, the result of that rule is
     * discarded (by removing the resulting element from the stack) and this rule finishes (leaving the NoteMarkup text in the input).
     * In addition the breaker text is not allowed on the input. Finding the breaker also finishes the rule (leaving the breaker in the input).
     * @param breaker The string sequence that finishes this rule.
     */
    Rule NoteTextTextWithBreaker(StringVar breaker) {
        return Sequence(
                OneOrMore(
                    FirstOf(
                        NoneOf("><[]'„“»«{}"),
                        Sequence(
                            TestNot(breaker.get()),
                            TestNot(
                                Sequence(
                                    NoteMarkup(),
                                    new Action<ObAstNode>() {
                                        public boolean run(Context<ObAstNode> context) {
                                            context.getValueStack().peek().removeLastChild(); // the pop is needed to remove the result of the note markup in case it matched
                                            return true;
                                        }
                                    }
                                )
                            ),
                            AnyOf("><[]'„“»«{}") // match the rest
                        )
                    )
                ),
                peek().appendChild(new ObTextNode(match()))
            );
    }
    
    Rule NoteMarkup() {
        return FirstOf(
            Note(), // recursion is allowed
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

    @SuppressNode
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
    
    /**
     * The name of this rule is chosen a little poorly. It represents any normal text in notes.
     * It is named so strangely because the name {@link NoteText} is already in use for the complete content of a note.
     */
    @SuppressNode
    Rule NoteTextText() {
        return Sequence(OneOrMore(NoneOf("><[]'„“»«{}")), peek().appendChild(new ObTextNode(match())));
    }

    @SuppressNode
    Rule ScriptureText() {
        return Sequence(OneOrMore(TextChar()), peek().appendChild(new ObTextNode(match()))); // this might cause a problem because OneOrMore() fights with ScriptureText() for the chars
    }
    
    @SuppressNode
    Rule TextChar() {
        return FirstOf(
            LetterChar(),
            PunctuationChar()
        );
    }

    @SuppressNode
    Rule LetterChar() {
        return FirstOf(
                // C0 Controls and Basic Latin, http://unicode.org/charts/PDF/U0000.pdf
                    CharRange('\u0041', '\u005a'), // A-Z
                    CharRange('\u0061', '\u007a'), // a-z
                // C1 Controls and Latin-1 Supplement, http://unicode.org/charts/PDF/U0080.pdf
                    CharRange('\u00c0', '\u00d6'), // all letters from C1
                    CharRange('\u00d8', '\u00f6'), // all letters from C1
                    CharRange('\u00f8', '\u00ff') // all letters from C1
                    /*'\u00c4', // Ä
                    '\u00d6', // Ö
                    '\u00dc', // Ü
                    '\u00df', // ß
                    '\u00e4', // ä
                    '\u00f6', // ö
                    '\u00fc', // ü
                    */
            );
    }

    @SuppressNode
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
                // C1 Controls and Latin-1 Supplement, http://unicode.org/charts/PDF/U0080.pdf
                    //'\u00b4', // ´
                    '\u2019', // ’ preferred character to be used for apostrophe
                    //CharRange('\u2010', '\u2015'), // all sorts of dashes
                // Latin Extended Additional, http://www.unicode.org/charts/PDF/U1E00.pdf
                    CharRange('\u1e00', '\u1eff'), 
                Whitespace()
            );
    }

    @SuppressNode
    Rule TagChar() {
        return FirstOf(
            LetterChar(),
            '_',
            '-',
            ' ',
            CharRange('0', '9')
        );
    }

    @SuppressNode
    Rule GreekTextChar() {
        return OneOrMore(
            // Greek and Coptic, http://unicode.org/charts/PDF/U0370.pdf
            CharRange('\u0370', '\u03ff')
        );
    }

    @SuppressNode
    Rule Number() {
        return Sequence(
            CharRange('1', '9'),
            ZeroOrMore(CharRange('0', '9'))
        );
    }

    @SuppressNode
    Rule Whitespace() {
        return FirstOf(
                ' ',
                /**
                 * {@link LineQuotes} start with a "\n:" and extend to the first occurence of "\n" without a ":" following.
                 * Thus we must not eat the "\n" if we are in a {@link LineQuote}, or if a colon follows. Otherwise
                 * {@link LineQuotes} could neither start, nor end.  
                 */
                Sequence(
                    ACTION(false == isRuleParent("LineQuote")),
                    TestNot("\n:"),
                    '\n'
                ),
                '\t',
                '\r'
        );
    }
    
    /**
     * This method acts as a <i>parboiled action expression</i>.  It takes the <i>previous</i> match object
     * and tries to turn it into an integer and write it into the given variable <b>ref</b>.
     * If parsing the number as an integer fails <b>false</b> is returned.
     * Action expressions must not be private.
     * @see <a href="https://github.com/sirthias/parboiled/wiki/Parser-Action-Expressions">action expression documentation</a>
     * @param ref The variable to write the result to.
     * @return true if parsing succeeds, false otherwise
     */
    protected boolean safeParseIntSet(Var<Integer> ref)
    {
        try
        {
            ref.set(Integer.parseInt(match()));
            return true;
        }
        catch(NumberFormatException e)
        {
            return false;
        }
    }
    
    /**
     * Checks whether there is a recursion in the current rule stack. Only custom rules are taken into account.
     * This function is to be used as a <i>parboiled action expression</i>. Call this at the
     * beginning of a rule to prevent the rule from calling itself.
     * Action expressions must not be private.
     * @return false if there is a recursion, true otherwise
     * @see <a href="https://github.com/sirthias/parboiled/wiki/Parser-Action-Expressions">action expression documentation</a>
     */
    protected boolean breakRecursion()
    {
        MatcherPath path = getContext().getPath();
        while(path != null && false == path.element.matcher.hasCustomLabel()) {
            path = path.parent;
        }
        /**
         * The label of the rule we are currently in (ignoring builtin matchers).
         */
        String label = path.element.matcher.getLabel();
        path = path.parent;
        if(path != null) {
            for(int i = path.length() - 1; i >= 0; --i) {
                if(path.getElementAtLevel(i).matcher.getLabel().equals(label)) {
                    return false;
                }
            }
        }
        return true;
    }
    
    /**
     * Checks whether the rule with the given name is in the current rule stack. The current parent rule (ignoring
     * builtin rules) is ignored.
     * This function is to be used as a <i>parboiled action expression</i>.
     * Action expressions must not be private.
     * @return true if the rule with the given name is a currently in the rule stack, false otherwise.
     * @see <a href="https://github.com/sirthias/parboiled/wiki/Parser-Action-Expressions">action expression documentation</a>
     */
    protected boolean isRuleParent(String ruleLabel)
    {
        MatcherPath path = getContext().getPath();
        boolean skipParent = true;
        for(int i = path.length() - 1; i >= 0; --i) {
            if(skipParent && path.getElementAtLevel(i).matcher.hasCustomLabel()) {
                skipParent = false;
                continue;
            }
            if(path.getElementAtLevel(i).matcher.getLabel().equals(ruleLabel)) {
                return true;
            }
        }
        return false;
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