package offeneBibel.parser;

import offeneBibel.parser.ObAstNode.NodeType;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private int divineNameStyle = 0;

    public void setDivineNameStyle(int divineNameStyle) {
        this.divineNameStyle = divineNameStyle;
    }

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
    
    public Rule ChapterNotes() {
        return Sequence(
                "==Einführende Bemerkungen==",
                push(new ObAstNode(NodeType.chapterNotes)),
                ZeroOrMore(Whitespace()),
                OneOrMore(FirstOf(FreeWikiMarkup(), Sequence(TestNot(ChapterTag()), NoteChar(), createOrAppendTextNode(match())))),
                peek(1).appendChild(pop())
        );
    }
    
    public Rule FreeWikiMarkup() {
        return OneOrMore(FirstOf(
                BibleTextQuote(),
                NoteQuote(),
                NoteFat(),
                NoteItalics(),
                Hebrew(),
                WikiLink(),
                NoteSuperScript(),
                NoteStrikeThrough(),
                NoteUnderline(),
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
    public Rule ChapterTag() {
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
    
    public Rule VerseRange(Var<ObChapterTag> chapterTag) {
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
    
    public Rule ChapterTagType(Var<ObChapterTag> chapterTag) {
        return FirstOf(
            Sequence("Lesefassung in Arbeit", chapterTag.get().setTag(ObChapterTag.ChapterTagName.lesefassunginArbeit)),
            Sequence("Lesefassung folgt später", chapterTag.get().setTag(ObChapterTag.ChapterTagName.studienfassunginArbeit), todo()),
            Sequence("Ungeprüfte Lesefassung", chapterTag.get().setTag(ObChapterTag.ChapterTagName.studienfassunginArbeit), todo()),
            Sequence("Lesefassung kann erstellt werden", chapterTag.get().setTag(ObChapterTag.ChapterTagName.studienfassunginArbeit), todo()),
            Sequence("Zuverlässige Studienfassung", chapterTag.get().setTag(ObChapterTag.ChapterTagName.studienfassungErfuelltDieMeistenKriterien), todo()),
            Sequence("zuverlässige Studienfassung", chapterTag.get().setTag(ObChapterTag.ChapterTagName.studienfassungErfuelltDieMeistenKriterien), todo()),
            Sequence("Sehr gute Studienfassung", chapterTag.get().setTag(ObChapterTag.ChapterTagName.studienfassungErfuelltDieMeistenKriterien), todo()),
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
    
    public boolean todo() {
        return true;
    }

    //(poemstart ws*)? (vers ws*)* '{{Bemerkungen}}';
    public Rule Fassung(ObFassungNode.FassungType fassung, StringVar breaker) {
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
    
    public Rule BibleText() {
        return FirstOf(
            Sequence(
                ACTION(getCurrentFassung(getContext().getValueStack()) == ObFassungNode.FassungType.lesefassung),
                LfBibleText()
            ),
            Sequence(
                ACTION(getCurrentFassung(getContext().getValueStack()) == ObFassungNode.FassungType.studienfassung),
                SfBibleText()
            )
        );
    }

    public Rule LfBibleText() {
        return OneOrMore(
            FirstOf(
                UnsupportedMarkup(),
                ScriptureText(),
                TextParenthesis(),
                LineQuote(),
                Quote(),
                Fat(),
                Italics(),
                AlternateReading(),
                PoemStart(),
                PoemStop(),
                SecondVoice(),
                Break(),
                ParallelPassage(),
                Comment(),
                SecondaryContent()
            )
        );
    }
    
    public Rule SfBibleText() {
        return OneOrMore(
            FirstOf(
                UnsupportedMarkup(),
                ScriptureText(),
                LineQuote(),
                Quote(),
                Fat(),
                Italics(),
                Insertion(),
                Omission(),
                Alternative(),
                AlternateReading(),
                PoemStart(),
                PoemStop(),
                SecondVoice(),
                Break(),
                ParallelPassage(),
                Comment(),
                SecondaryContent()
            )
        );
    }
    
    // "{{" ('S'|'L') '|' NUMBER  "}}";
    public Rule Verse() {
        Var<Integer> verseFromNumber = new Var<Integer>();
        Var<Integer> verseToNumber = new Var<Integer>();
        return Sequence(
            ZeroOrMore(Whitespace()),
            "{{",
            FirstOf(
                Sequence(
                    ACTION(getCurrentFassung(getContext().getValueStack()) == ObFassungNode.FassungType.lesefassung),
                    'L'
                ),
                Sequence(
                    ACTION(getCurrentFassung(getContext().getValueStack()) == ObFassungNode.FassungType.studienfassung),
                    'S'
                )
            ),
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

    public Rule Heading() {
        return Sequence(
                ACTION( getCurrentFassung(getContext().getValueStack()) == ObFassungNode.FassungType.lesefassung ),
                "((",
                push(new ObAstNode(ObAstNode.NodeType.heading)),
                ScriptureText(),
                Optional(Note()),
                "))",
                peek(1).appendChild(pop())
        );
    }

    /**
     * Since quotes sometimes start in one chapter and end in a different one it is not always possible to
     * completely match a quote when parsing chapters separately. Thus we only optionally match the closing tag for now.
     * @return
     */
    public Rule Quote() {
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

    public Rule InnerQuote() {
        return Sequence(
            ACTION(isRuleAncestor("Quote")),
            '\u201A', // ‚
            push(new ObAstNode(ObAstNode.NodeType.quote)),
            OneOrMore(FirstOf(
                BibleText(),
                InnerQuote(),
                Verse(),
                Note(),
                Comment()
            )),
            '\u2018', // ‘
            peek(1).appendChild(pop())
        );
    }
    
    /**
     * Quotes that are realized with the MediaWiki <i>Definition List</i> syntax.
     * In this syntax each line has to start with a ":". The quote extends until the first line without a ":" is found.
     * To prevent {@link Whitespace} eating up "\n" and thus preventing this rule to finish some special logic was
     * added there.
     */
    public Rule LineQuote() {
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
                // Keep the boxing right. LineQuote -> Quote -> InnerQuote -> InnerQuote -> ...
                // Line quotes are not allowed in poems.
                ACTION(false == isRuleAncestor("Quote")),
                ACTION(false == isRuleAncestor("InnerQuote")),
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

    public Rule Fat() {
        return FirstOf(
            Sequence(
                "'''",
                push(new ObAstNode(ObAstNode.NodeType.fat)),
                OneOrMore(FirstOf(
                        ScriptureText(),
                        Quote(),
                        //Italics(),
                        Insertion(),
                        Alternative(),
                        AlternateReading(),
                        Break(),
                        ParallelPassage(),
                        Note(),
                        Comment(),
                        SecondaryContent()
                    )),
                "'''",
                peek(1).appendChild(pop())
            ),
            Sequence(
                "<b>",
                push(new ObAstNode(ObAstNode.NodeType.fat)),
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
                        Comment(),
                        SecondaryContent()
                    )),
                "</b>",
                peek(1).appendChild(pop())
            )
        );
    }
    
    public Rule Italics() {
        return FirstOf(
            Sequence(
                "''",
                push(new ObAstNode(ObAstNode.NodeType.italics)),
                OneOrMore(FirstOf(
                        ScriptureText(),
                        Quote(),
                        Fat(),
                        Insertion(),
                        Alternative(),
                        AlternateReading(),
                        Break(),
                        ParallelPassage(),
                        Note(),
                        Comment(),
                        SecondaryContent()
                    )),
                "''",
                peek(1).appendChild(pop())
            ),
            Sequence(
                "<em>",
                push(new ObAstNode(ObAstNode.NodeType.italics)),
                OneOrMore(FirstOf(
                        ScriptureText(),
                        Quote(),
                        Fat(),
                        Insertion(),
                        Alternative(),
                        AlternateReading(),
                        Break(),
                        ParallelPassage(),
                        Note(),
                        Comment(),
                        SecondaryContent()
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
                        Fat(),
                        Insertion(),
                        Alternative(),
                        AlternateReading(),
                        Break(),
                        ParallelPassage(),
                        Note(),
                        Comment(),
                        SecondaryContent()
                    )),
                "</i>",
                peek(1).appendChild(pop())
            )
        );
    }
    
    // Parses/skips stuff that exists in the wiki but is not supported by the parser
    public Rule UnsupportedMarkup() {
        return FirstOf(
            // verse subsplitting with roman and greek letters
            Sequence(
                "<span style=\"color:gray\"><sup><i>",
                ZeroOrMore(AnyOf("0123456789")),
                OneOrMore(AnyOf("abcdefghijklmnopqrstuvwxyz")),
                ZeroOrMore(AnyOf("αβγδεζηθικλμνξοπρςστυφχψω")),
                "</i></sup></span>"
            ),
            // ((blabla)), only in Studienfassung (breaks headlines in Lesefassung)
            Sequence(
                    ACTION(getCurrentFassung(getContext().getValueStack()) == ObFassungNode.FassungType.studienfassung),
                    "((",createOrAppendTextNode(match()),
                    ScriptureText(),
                    "))",createOrAppendTextNode(match())
                ),
            // non-breaking spaces
            Sequence("&#160;", createOrAppendTextNode(" ")),
            // nowiki tags
            Sequence(
                "<nowiki>",
                ZeroOrMore(FirstOf(CharRange(' ', ';'),CharRange('?','~'))),
                createOrAppendTextNode(match()),
                "</nowiki>"
            ),
            // [Jesus] in Lesefassung (Matthäus 17)
            Sequence("[Jesus]", createOrAppendTextNode("[Jesus]")),
            // Asterisk (Markus 15)
            Sequence("*", createOrAppendTextNode("*")),
            // empty footnotes (Genesis 10, 1Chronik 1, Johannes 15, Jakobus 1)
            "<ref></ref>"
        );
    }

    public Rule NoteFat() {
        return FirstOf(
            Sequence(
                breakRecursion(),
                "'''",
                push(new ObAstNode(ObAstNode.NodeType.fat)),
                OneOrMore(Sequence(TestNot("</b>"), NoteChar(), createOrAppendTextNode(match()))),
                "'''",
                peek(1).appendChild(pop())
            ),
            Sequence(
                breakRecursion(),
                "<b>",
                push(new ObAstNode(ObAstNode.NodeType.fat)),
                OneOrMore(Sequence(TestNot("</b>"), NoteChar(), createOrAppendTextNode(match()))),
                "</b>",
                peek(1).appendChild(pop())
            )
        );
    }
    
    public Rule NoteItalics() {
        return FirstOf(
            Sequence(
                breakRecursion(),
                "''",
                push(new ObAstNode(ObAstNode.NodeType.italics)),
                NoteText(new StringVar("''")),
                "''",
                peek(1).appendChild(pop())
            ),
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
    
    public Rule Insertion() {
        return Sequence(
            '[',
            push(new ObAstNode(ObAstNode.NodeType.insertion)),
            OneOrMore(FirstOf(
                    ScriptureText(),
                    Quote(),
                    Fat(),
                    Italics(),
                    Alternative(),
                    AlternateReading(),
                    Break(),
                    ParallelPassage(),
                    Note(),
                    Comment(),
                    SecondaryContent()
                )),
            ']',
            peek(1).appendChild(pop())
        );
    }
    
    public Rule Omission() {
        return Sequence(
            /*
             * For a little better error reporting we catch the most often encountered false positives, before
             * consuming input.
             */
            TestNot(FirstOf("{{Studienfassung}}", "{{Lesefassung}}", "{{Bemerkungen}}", "{{Kapitelseite Fuß}}", "{{Sekundär}}", "{{Sekundär ende}}", "{{Hebr}}", "{{Hebr ende}}")),
            '{',
            push(new ObAstNode(ObAstNode.NodeType.omission)),
            OneOrMore(FirstOf(
                    ScriptureText(),
                    Quote(),
                    Fat(),
                    Italics(),
                    Insertion(),
                    Alternative(),
                    AlternateReading(),
                    Break(),
                    ParallelPassage(),
                    Note(),
                    Omission(),
                    Comment(),
                    SecondaryContent()
                )),
            '}',
            // Prevent "{{blabla}}", an omission that contains only one other omission.
            ACTION( peek().childCount()!=1 || ((ObAstNode)(peek().peekChild())).getNodeType()!=NodeType.omission),
            peek(1).appendChild(pop())
        );
    }

    public Rule TextParenthesis() {
        return Sequence(
            '(', TestNot('/'), TestNot('('), peek().appendChild(new ObTextNode(match())),
            OneOrMore(FirstOf(
                    BibleText(),
                    Verse(),
                    Heading(),
                    Note(),
                    Comment()
            )),
            ')', peek().appendChild(new ObTextNode(match()))
        );
    }
    
    public Rule Alternative() {
        return Sequence(
            '(',
            push(new ObAstNode(ObAstNode.NodeType.alternative)),
            OneOrMore(FirstOf(
                ScriptureText(),
                Quote(),
                Sequence(ACTION(isRuleAncestor("Quote")), InnerQuote()),
                Insertion(),
                Alternative(),
                AlternateReading(),
                Omission(),
                Note(),
                Comment(),
                SecondaryContent()
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
    public Rule AlternateReading() {
        StringVar part1 = new StringVar(), part2=new StringVar(), part3 = new StringVar();
        return Sequence(
            "(/",
            OneOrMore(
                FirstOf(
                    '|',
                    Sequence(
                        TestNot('/'),
                        TextChar()
                    )
                )
            ),
            part1.set(match()),
            '/',
            OneOrMore(
                TestNot('/'),
                TextChar()
            ),
            part2.set(match()),
            Optional(
            '/',
            TestNot(')'),
            OneOrMore(
                TestNot('/'),
                TextChar()
            ),
            part3.set(match())),
            peek().appendChild(new ObTextNode(makeDivineName(part1.get(),part2.get(),part3.get()))),
            "/)"
        );
    }
    
    public String makeDivineName(String part1, String part2, String part3) {
        if (divineNameStyle == 0)
            return part1;

        // code based on https://github.com/Offene-Bibel/website/blob/a7e8a917a786a998b4856cca5df418493b140df1/website/mediawiki/extensions/OffeneBibel/OffeneBibel_body.php#L129-L175
        String p1 = part3, p2 = part2;
        if (part3 == null || part3.isEmpty()) { p1 = part2; p2 = part1; }
        Matcher m1 = Pattern.compile("\\b(ICH|DU|ER|MIR|DIR|IHM|MICH|DICH|IHN|[MDS]EIN(E[MNRS]?)?)\\b").matcher(p1);
        Matcher m2 = Pattern.compile("\\b(([Uu]ns|[Ee]u)er\\s(GOTT|HERR)|([Uu]nse|[Ee]u)re[mn]\\s(GOTT|HERRN)|([Uu]nse|[Ee]u)res\\s(GOTTES|HERRN))\\b").matcher(p2);
        if (!m1.find() || !m2.find()) { return part1; }
        String pfx1 = p1.substring(0, m1.start()), ptn1 = p1.substring(m1.start(), m1.end()), sfx1 = p1.substring(m1.end());
        String pfx2 = p2.substring(0, m2.start()), ptn2 = p2.substring(m2.start(), m2.end()), sfx2 = p2.substring(m2.end());
        ptn2 = ptn2.replace("HERRN", "Herrn").replace("HERR", "Herr").replace("GOTTES", "Herrn").replace("rem GOTT", "rem Herrn").replace("ren GOTT", "ren Herrn").replace("GOTT", "Herr");

        // code based on https://github.com/Offene-Bibel/website/blob/44ec1a587c62e7e991a330cfbecfc3aa9d0cc81a/website/static/js/replacement.js#L36-L181
        boolean isGenitiv = ptn2.matches("(?:[Uu]nseres|[Ee]ures) Herrn");
        String der = ptn2.replaceFirst("^(?:unser|euer) ", "der ").replaceFirst("^(?:Unser|Euer) ", "Der ").replaceFirst("^(?:unsere|eure)([nms]) ", "de$1 ").replaceFirst("^(?:Unsere|Eure)([nms]) ", "De$1 ").replace(" Herrn", " @n").replace(" Herr", " @");
        if (ptn1.equals("DU")) { der = "@@"; }
        String[] variations = { part1,
                pfx2 + (isGenitiv ? "von יהוה" : "יהוה") + sfx2,
                pfx1 + (isGenitiv ? "JHWHs" : "JHWH") + sfx1,
                pfx1 + (isGenitiv ? "Jahwes" : "Jahwe") + sfx1,
                pfx1 + (isGenitiv ? "Jahos" : "Jaho") + sfx1,
                pfx1 + (isGenitiv ? "Gottes" : "Gott") + sfx1,
                pfx2 + der.replace("@@", "Herr").replace("@", "Herr") + sfx2,
                pfx2 + der.replace("@@", "Ewiger").replace("@", "Ewige") + sfx2,
                pfx1 + ptn1 + sfx1,
                pfx1 + (isGenitiv ? "Ich-Bin-Das" : "Ich-Bin-Da") + sfx1,
                pfx2 + (isGenitiv ? ptn2.replace(" Herrn", " Gottes") : ptn2.replace(" Herrn", " Gott").replace(" Herr", " Gott")) + sfx2,
                pfx2 + ptn2 + sfx2,
                pfx2 + (isGenitiv ? "von Adonai" : "Adonai") + sfx2,
                pfx2 + (isGenitiv ? "von Ha-Schem" : "Ha-Schem") + sfx2,
        };
        return variations[divineNameStyle];
     }

    public Rule SecondaryContent() {
        return Sequence(
                 FirstOf("{{Sekundär}}", "{{sekundär}}"),
                 push(new ObAstNode(ObAstNode.NodeType.secondaryContent)),
                 OneOrMore(FirstOf(
                     ScriptureText(),
                     Verse(),
                     Quote(),
                     InnerQuote(),
                     Fat(),
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
                 FirstOf("{{Sekundär ende}}", "{{sekundär ende}}"),
                 peek(1).appendChild(pop())
                 );
    }
    
    /**
     * {@link PoemStart} and {@link PoemStop} elements can freely interleave with other elements. Thus
     * it is impossible to represent a poem block as a syntax element with children.
     * TODO: Check for two PoemStarts without an intermediate PoemStop.
     */
    public Rule PoemStart() {
        return Sequence(
                "<poem>",
                Optional("\n"),
                peek().appendChild(new ObAstNode(ObAstNode.NodeType.poemStart)),
                // We make sure there is a new line following the poem start
                // to make sure the first line in a poem is also wrapped in <l> tags.
                createOrAppendTextNode("\n")
                );
    }
    
    /**
     * See {@link PoemStart}.
     */
    public Rule PoemStop() {
        return Sequence(
                "</poem>",
                peek().appendChild(new ObAstNode(ObAstNode.NodeType.poemStop))
                );
    }
    
    /**
     * SecondVoice is similar to line quotes in how it works. In addition
     * it can interleave with other elements and is thus represented using
     * separate start and stop tags. This is similar to how poem tags work.
     * A Secondary voice starts on the first occurence of "\n_" and stops
     * on the first ocurrence of "\n" without a following "_".
     * Secondary voices are only allowed inside poem tags. It's not possible
     * to determine whether we are in a poem though. Thus this is not checked.
     */
    public Rule SecondVoice() {
        return toRule("\n_");
            /**
             * {@link SecondVoice} elements can freely interleave with other constructs (e.g. Quotes) and can thus not be represented
             * as a single element with children. This is similar to poem tags.
             * {@link SecondVoice} can not be matched in a completely hierarchical fashion, because the "\n" can occur inside
             * other elements. Thus we need a way to eat up "\n:" inside elements nested in {@link LineQuote}s. This is done here.
             */
            /*Sequence(
                isRuleAncestor("SecondVoice"),
                "\n_"
            ),*/
            /**
             * This is the actual {@link SecondVoice} matching part.
             */
            /*Sequence(
                "\n_",
                push(new ObAstNode(ObAstNode.NodeType.secondVoice)),
                OneOrMore(FirstOf(
                        BibleText(),
                        Verse(),
                        Note(),
                        Comment()
                )),
                '\n',
                peek(1).appendChild(pop())
            )
        );*/
    }
    
    public Rule Break() {
        return Sequence(FirstOf("<br/>", "<br />", "<br>"),
                peek().appendChild(new ObAstNode(ObAstNode.NodeType.textBreak))
                );
    }
    
    /**
     * '{{par|' book '|' NUMBER '|' NUMBER ('|' NUMBER)? '}}'
     * 
     * TODO: code to verify passage
     */
    public Rule ParallelPassage() {
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

    public Rule Note() {
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

    public Rule NoteText(StringVar breaker) {
        return OneOrMore(FirstOf(
                NoteMarkup(),
                Sequence(TestNot(breaker.get()), NoteChar(), createOrAppendTextNode(match()))
        ));
    }

    public Rule NoteMarkup() {
        return FirstOf(
            Note(), // recursion is allowed
            BibleTextQuote(),
            UnsupportedMarkup(),
            NoteQuote(),
            NoteFat(),
            NoteItalics(),
            Hebrew(),
            WikiLink(),
            NoteSuperScript(),
            NoteStrikeThrough(),
            NoteUnderline(),
            Break(),
            Comment()
        );
    }

    @SuppressNode
    public Rule TagText() {
        return OneOrMore(TagChar());
    }
    
    public Rule NoteQuote() {
        return Sequence(
            '\u201e', // „
            push(new ObAstNode(ObAstNode.NodeType.quote)),
            NoteText(new StringVar("" + '\u201c')),
            '\u201c', // “
            peek(1).appendChild(pop())
        );
    }

    public Rule BibleTextQuote() {
        return Sequence(
            '\u00BB', // »
            push(new ObAstNode(ObAstNode.NodeType.quote)),
            BibleText(),
            '\u00AB', // «
            peek(1).appendChild(pop())
        );
    }
    
    public Rule Hebrew() {
        return Sequence(
            FirstOf("{{Hebr}}", "{{hebr}}"),
            FirstOf(Sequence(
                    "[",
                    Test(FirstOf("http://", "https://")), 
                    OneOrMore(NoneOf(" ]")),
                    push(new ObWikiLinkNode(match(), false)),
                    ' ',
                    push(new ObAstNode(NodeType.hebrew)),
                    FirstOf(
                        Sequence('\u202b',HebrewText(),'\u202c'), // optional directional markers
                        HebrewText()
                    ),
                    "]",
                    peek(1).appendChild(pop())
            ), Sequence(
            push(new ObAstNode(NodeType.hebrew)),
            FirstOf(
                Sequence('\u202b',HebrewText(),'\u202c'), // optional directional markers
                HebrewText()
            ))),
            FirstOf("{{Hebr ende}}", "{{hebr ende}}"),
            peek(1).appendChild(pop())
        );
    }
    
    public Rule WikiLink() {
        return FirstOf(
            Sequence(
                breakRecursion(),
                "[[",
                OneOrMore(NoneOf("|]")),
                push(new ObWikiLinkNode(match(), true)),
                Optional(
                    '|',
                    NoteText(new StringVar("]]"))
                ),
                "]]",
                peek(1).appendChild(pop())
            ),
            Sequence(
                breakRecursion(),
                "[",
                Test(FirstOf("http://","https://")),
                OneOrMore(NoneOf(" ]")),
                push(new ObWikiLinkNode(match(), false)),
                Optional(
                    ' ',
                    NoteWikiLinkText()
                ),
                "]",
                peek(1).appendChild(pop())
            )
        );
    }
    
    public Rule NoteWikiLinkText() {
        return OneOrMore(FirstOf(
                BibleTextQuote(),
                NoteQuote(),
                NoteFat(),
                NoteItalics(),
                Hebrew(),
                NoteSuperScript(),
                Break(),
                Comment(),
                Sequence(TestNot("]"), NoteChar(), createOrAppendTextNode(match()))
        ));
    }
    
    public Rule NoteSuperScript() {
        return Sequence(
            breakRecursion(),
            "<sup>",
            push(new ObSuperScriptTextNode()),
            NoteText(new StringVar("</sup>")),
            "</sup>",
            peek(1).appendChild(pop())
        );
    }
    
    public Rule NoteStrikeThrough() {
        return Sequence(
            breakRecursion(),
            "<s>",
            push(new ObAstNode(NodeType.strikeThrough)),
            NoteText(new StringVar("</s>")),
            "</s>",
            peek(1).appendChild(pop())
        );
    }
    
    public Rule NoteUnderline() {
        return Sequence(
            breakRecursion(),
            "<u>",
            push(new ObAstNode(NodeType.underline)),
            NoteText(new StringVar("</u>")),
            "</u>",
            peek(1).appendChild(pop())
        );
    }
    
    public Rule Comment() {
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
    public Rule HebrewText() {
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
    public Rule ScriptureText() {
        return Sequence(OneOrMore(TextChar()), peek().appendChild(new ObTextNode(match()))); // this might cause a problem because OneOrMore() fights with ScriptureText() for the chars
    }

    @SuppressNode
    public Rule TextChar() {
        return FirstOf(
            LetterChar(),
            PunctuationChar()
        );
    }

    @SuppressNode
    public Rule LetterChar() {
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
    public Rule PunctuationChar() {
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
    public Rule NoteChar() {
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
                   TestNot("'''"),
                   TestNot("[["),
                   TestNot("<nowiki>"),
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
    public Rule TagChar() {
        return FirstOf(
            LetterChar(),
            '_',
            '-',
            '.',
            ' ',
            CharRange('0', '9')
        );
    }

    @SuppressNode
    public Rule Number() {
        return Sequence(
            CharRange('1', '9'),
            ZeroOrMore(CharRange('0', '9'))
        );
    }

    @SuppressNode
    public Rule Whitespace() {
        return FirstOf(
                ' ',
                '\t',
                '\r',
                /**
                 * {@link LineQuote}s start with a "\n:" and extend to the first occurence of "\n" without a ":" following.
                 * Thus we must not eat the "\n" if we are in a {@link LineQuote}, or if a colon follows. Otherwise
                 * {@link LineQuote}s could neither start, nor end. Same for {@link SecondVoice}.
                 */
                Sequence(
                    ACTION(false == isRuleAncestor("LineQuote")),
                    TestNot("\n:"),
                    ACTION(false == isRuleAncestor("SecondVoice")),
                    TestNot("\n_"),
                    '\n'
                )
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
    
    protected static ObFassungNode.FassungType getCurrentFassung(ValueStack<ObAstNode> valueStack)
    {
        for(ObAstNode node : valueStack) {
            if(node.getNodeType() == ObAstNode.NodeType.fassung) {
                return ((ObFassungNode)node).getFassung();
            }
        }
        return null;
    }
}