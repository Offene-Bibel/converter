package offeneBibel.parser;

import offeneBibel.parser.ObAstNode.NodeType;

import org.parboiled.Action;
import org.parboiled.BaseParser;
import org.parboiled.Context;
import org.parboiled.Rule;
import org.parboiled.annotations.BuildParseTree;
import org.parboiled.annotations.SuppressNode;
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
            Optional(Sequence("<small>", OneOrMore(FirstOf(FreeWikiMarkup(), Sequence(TestNot("</small>"), NoteChar(), createOrAppendTextNode(match())))), "</small>"), ZeroOrMore(Whitespace())), // just ignore alternate readings for now
            "{{Lesefassung}}",
            ZeroOrMore(Whitespace()),
            FirstOf(
                "''(kommt später)''",
                Fassung(ObFassungNode.FassungType.lesefassung, new StringVar("{{Studienfassung}}"))
            ),
            ZeroOrMore(Whitespace()),
            "{{Studienfassung}}",
            ZeroOrMore(Whitespace()),
            Fassung(ObFassungNode.FassungType.studienfassung, new StringVar("{{Kapitelseite Fuß}}")),
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
                OneOrMore(FirstOf(FreeWikiMarkup(), Sequence(TestNot(ChapterTag()), NoteChar(), createOrAppendTextNode(match())))),
                peek(1).appendChild(pop())
        );
    }
    
    Rule FreeWikiMarkup() {
        return OneOrMore(FirstOf(
                BibleTextQuote(),
                NoteQuote(),
                NoteEmphasis(),
                NoteItalics(),
                Hebrew(),
                WikiLink(),
                NoteSuperScript(),
                Break(),
                Note(),
                Comment()
        ));
    }
    
    /*
    chaptertag    :    "{{" chaptertagtype "}}"
    |    "{{" chaptertagtype '|Vers ' NUMBER '-' NUMBER "}}"
    ;
     */
    Rule ChapterTag() {
        Var<ObChapterTag> chapterTag = new Var<ObChapterTag>(new ObChapterTag());
        return FirstOf(
            Sequence("{{", ChapterTagType(chapterTag), "}}", ((ObChapterNode)peek()).addChapterTag(chapterTag.get())),
            Sequence("{{",
                    ChapterTagType(chapterTag), "|Vers ",
                    VerseRange(chapterTag), ZeroOrMore(" und ", VerseRange(chapterTag)),
                    "}}",
                    ((ObChapterNode)peek()).addChapterTag(chapterTag.get())
                    )
        );
    }
    
    Rule VerseRange(Var<ObChapterTag> chapterTag) {
        Var<Integer> startVerse = new Var<Integer>();
        Var<Integer> stopVerse = new Var<Integer>();
        return FirstOf(
            Sequence(
                Number(), safeParseIntSet(startVerse),
                '-',
                Number(), safeParseIntSet(stopVerse),
                chapterTag.get().addVerseRange(startVerse.get(), stopVerse.get())
            ),
            Sequence(
                Number(), safeParseIntSet(startVerse), chapterTag.get().addVerse(startVerse.get())
            )
        );
    }
    
    Rule ChapterTagType(Var<ObChapterTag> chapterTag) {
        return FirstOf(
            Sequence("Lesefassung in Arbeit", chapterTag.get().setTag(ObChapterTag.ChapterTagName.lesefassunginArbeit)),
            Sequence("Studienfassung in Arbeit", chapterTag.get().setTag(ObChapterTag.ChapterTagName.studienfassunginArbeit)),
            Sequence("Lesefassung zu prüfen", chapterTag.get().setTag(ObChapterTag.ChapterTagName.lesefassungZuPruefen)),
            Sequence("Studienfassung zu prüfen", chapterTag.get().setTag(ObChapterTag.ChapterTagName.studienfassungZuPruefen)),
            Sequence("Studienfassung liegt in Rohübersetzung vor", chapterTag.get().setTag(ObChapterTag.ChapterTagName.studienfassungLiegtInRohuebersetzungVor)),
            Sequence("Lesefassung erfüllt die meisten Kriterien", chapterTag.get().setTag(ObChapterTag.ChapterTagName.lesefassungErfuelltDieMeistenKriterien)),
            Sequence("Studienfassung erfüllt die meisten Kriterien", chapterTag.get().setTag(ObChapterTag.ChapterTagName.studienfassungErfuelltDieMeistenKriterien)),
            Sequence("Studienfassung und Lesefassung erfüllen die Kriterien", chapterTag.get().setTag(ObChapterTag.ChapterTagName.studienfassungUndLesefassungErfuellenDieKriterien)),
            Sequence("Überprüfung angefordert", chapterTag.get().setTag(ObChapterTag.ChapterTagName.ueberpruefungAngefordert)),
            Sequence("Vers unvollständig übersetzt", chapterTag.get().setTag(ObChapterTag.ChapterTagName.versUnvollstaendigUebersetzt))
        );
    }
    
    //(poemstart ws*)? (vers ws*)* '{{Bemerkungen}}';
    Rule Fassung(ObFassungNode.FassungType fassung, StringVar breaker) {
        return Sequence(
                ZeroOrMore(Whitespace()),
                push(new ObFassungNode(fassung)),
                OneOrMore(FirstOf(
                    BibleText(),
                    Verse(),
                    Heading(),
                    Note(),
                    Comment()
                )),
                "{{Bemerkungen}}",
                Optional(Sequence(
                        push(new ObAstNode(NodeType.fassungNotes)),
                        ZeroOrMore(Whitespace()),
                        NoteText(breaker),
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
                ParallelPassage(),
                Comment()
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
            ACTION(false == isRuleAncestor("InnerQuote")),
            '\u201e', // „
            push(new ObAstNode(ObAstNode.NodeType.quote)),
            OneOrMore(FirstOf(
                    BibleText(),
                    InnerQuote(),
                    Verse(),
                    Note(),
                    Comment()
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
                Note(),
                Comment()
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
                isRuleAncestor("LineQuote"),
                "\n:"
            ),
            /**
             * This is the actual {@link LineQuote} matching part.
             */
            Sequence(
                "\n:",
                push(new ObAstNode(ObAstNode.NodeType.quote)),
                OneOrMore(FirstOf(
                        BibleText(),
                        Verse(),
                        Note(),
                        Comment()
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
                    Note(),
                    Comment()
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
                        Note(),
                        Comment()
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
                        Note(),
                        Comment()
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
            NoteText(new StringVar("''")),
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
                OneOrMore(Sequence(TestNot("</em>"), NoteChar(), createOrAppendTextNode(match()))),
                "</em>",
                peek(1).appendChild(pop())
            ),
            Sequence(
                breakRecursion(),
                "<i>",
                push(new ObAstNode(ObAstNode.NodeType.italics)),
                OneOrMore(Sequence(TestNot("</i>"), NoteChar(), createOrAppendTextNode(match()))),
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
                    Note(),
                    Comment()
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
                    Omission(),
                    Comment()
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
                Note(),
                Comment()
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
    
    /**
     * {@link PoemStart} and {@link PoemStop} elements can freely interleave with other elements. Thus
     * it is impossible to represent a poem block as a syntax element with children.
     * TODO: Check for two PoemStarts without an intermediate PoemStop.
     */
    Rule PoemStart() {
        return Sequence(
                "<poem>",
                peek().appendChild(new ObAstNode(ObAstNode.NodeType.poemStart))
                );
    }
    
    /**
     * See {@link PoemStart}.
     */
    Rule PoemStop() {
        return Sequence(
                "</poem>",
                peek().appendChild(new ObAstNode(ObAstNode.NodeType.poemStop))
                );
    }
    
    Rule Break() {
        return Sequence(FirstOf("<br/>", "<br />", "<br>"),
                peek().appendChild(new ObAstNode(ObAstNode.NodeType.textBreak))
                );
    }
    
    /**
     * '{{par|' book '|' NUMBER '|' NUMBER ('|' NUMBER)? '}}'
     * 
     * TODO: code to verify passage
     */
    Rule ParallelPassage() {
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

    Rule Note() {
        StringVar tagText = new StringVar();
        return FirstOf(
            Sequence(
                "<ref>", push(new ObNoteNode()),
                NoteText(new StringVar("</ref>")),
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
                NoteText(new StringVar("</ref>")),
                "</ref>",
                peek(1).appendChild(pop())
            )
        );
    }

    Rule NoteText(StringVar breaker) {
        return OneOrMore(FirstOf(
                NoteMarkup(),
                Sequence(TestNot(breaker.get()), NoteChar(), createOrAppendTextNode(match()))
        ));
    }

    Rule NoteMarkup() {
        return FirstOf(
            Note(), // recursion is allowed
            BibleTextQuote(),
            NoteQuote(),
            NoteEmphasis(),
            NoteItalics(),
            Hebrew(),
            WikiLink(),
            NoteSuperScript(),
            Break(),
            Comment()
        );
    }

    @SuppressNode
    Rule TagText() {
        return OneOrMore(TagChar());
    }
    
    Rule NoteQuote() {
        return Sequence(
            '\u201e', // „
            push(new ObAstNode(ObAstNode.NodeType.quote)),
            NoteText(new StringVar("" + '\u201c')),
            '\u201c', // “
            peek(1).appendChild(pop())
        );
    }

    Rule BibleTextQuote() {
        return Sequence(
            '\u00BB', // »
            push(new ObAstNode(ObAstNode.NodeType.quote)),
            BibleText(),
            '\u00AB', // «
            peek(1).appendChild(pop())
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
                breakRecursion(),
                "[[",
                OneOrMore(NoneOf("|]")),
                push(new ObWikiLinkNode(match())),
                Optional(
                    '|',
                    OneOrMore(Sequence(TestNot("]]"), NoteChar(), createOrAppendTextNode(match())))
                ),
                "]]",
                peek(1).appendChild(pop())
            ),
            Sequence(
                breakRecursion(),
                "[http://",
                OneOrMore(NoneOf(" ]")),
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
                BibleTextQuote(),
                NoteQuote(),
                NoteEmphasis(),
                NoteItalics(),
                Hebrew(),
                NoteSuperScript(),
                Break(),
                Comment(),
                Sequence(TestNot("]"), NoteChar(), createOrAppendTextNode(match()))
        ));
    }
    
    Rule NoteSuperScript() {
        return Sequence(
            breakRecursion(),
            "<sup>",
            push(new ObSuperScriptTextNode()),
            NoteText(new StringVar("</sup>")),
            "</sup>",
            peek(1).appendChild(pop())
        );
    }
    
    Rule Comment() {
        return Sequence(
            "<!--",
            OneOrMore(
                TestNot("-->"),
                ANY
            ),
            "-->"
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
    Rule NoteChar() {
        return Sequence(
                   /*
                    * This rule generally consumes anything. In all calling cases it is guarded, not to consume
                    * input that is required by the surrounding rule and also guarded as to not consume elements
                    * that would form a valid other rule. But if all other guards fail, i.e. no other rule
                    * is interested in the found input then this rule will consume any input.
                    * This has the effect that many mistakes are hidden by this rule since broken syntactic constructs
                    * don't lead to a failure but are instead just consumed away. This behavior is wanted in many
                    * cases, since it allows successfully parsing text with unknown constructs. Hiding erroneous
                    * constructs is not wanted though. To at least mitigate some of this hiding-errors-effect
                    * we add loads of input that should never be matched here because it should be matched by
                    * other rules. This effectively prevents this rule from covering up (by eating up) broken
                    * other rules. Not everything can be prevented this way. Completely misspelled constructs that
                    * can not be recognized in any way will still slip through.
                    */
                   TestNot("<ref"), TestNot("</ref"),
                   TestNot("<i>"), TestNot("</i>"),
                   TestNot("<sup>"), TestNot("</sup>"),
                   TestNot("{{hebr"), TestNot("{{Hebr"),
                   TestNot("{{Lesefassung}}"), TestNot("{{Studienfassung}}"),
                   TestNot("{{Kapitelseite Fuß}}"),
                   TestNot("{{Bemerkungen}}"),
                   TestNot("{{L|"), TestNot("{{S|"),
                   TestNot('\u201e'), TestNot('\u201c'), // „“
                   TestNot('\u00BB'), TestNot('\u00AB'), // »«
                   TestNot("<em>"), TestNot("</em>"), TestNot("<i>"), TestNot("</i>"),
                   TestNot("<br/>"), TestNot("<br />"),TestNot("<br>"),
                   //TestNot("\n:"), // Linequotes in comments not yet implemented
                   TestNot("''"),
                   TestNot("[["),
                   ANY
               );
            /*FirstOf(
                TextChar(),
                '(', ')',
                '[', ']',
                '§',
                '+',
                '\n',
                CharRange('\u0370', '\u03ff'), // Greek and Coptic, http://unicode.org/charts/PDF/U0370.pdf
                '<', '>', '/', // swallow all sorts of unsupported XML like markup
                '{', '}', '|', '=', '"', // swallow wiki tables for now
                '*', '#' // swallow wiki lists for now
            );
            */
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
                    ACTION(false == isRuleAncestor("LineQuote")),
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
    protected boolean isRuleAncestor(String ruleLabel)
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

    /**
     * Add a text to the current node tree. If the latest element is a text node the given text is
     * appended to that node. If the latest element in the tree is not a text node a text node with
     * the given text is created and added to the tree.
     * This function is to be used as a <i>parboiled action expression</i>.
     * Action expressions must not be private.
     * @return true
     * @see <a href="https://github.com/sirthias/parboiled/wiki/Parser-Action-Expressions">action expression documentation</a>
     */
    protected boolean createOrAppendTextNode(String text)
    {
        if(peek().peekChild() instanceof ObTextNode) {
            ((ObTextNode)peek().peekChild()).appendText(text);
        }
        else {
            peek().appendChild(new ObTextNode(text));
        }
        return true;
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