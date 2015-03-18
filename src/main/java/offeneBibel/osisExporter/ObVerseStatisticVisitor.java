package offeneBibel.osisExporter;

import java.util.ArrayList;
import java.util.List;

import offeneBibel.parser.ObAstNode;
import offeneBibel.parser.ObChapterNode;
import offeneBibel.parser.ObChapterTag;
import offeneBibel.parser.ObChapterTag.ChapterTagName;
import offeneBibel.parser.ObFassungNode;
import offeneBibel.parser.ObFassungNode.FassungType;
import offeneBibel.parser.ObNoteNode;
import offeneBibel.parser.ObParallelPassageNode;
import offeneBibel.parser.ObTextNode;
import offeneBibel.parser.ObVerseNode;
import offeneBibel.parser.ObVerseStatus;
import offeneBibel.visitorPattern.DifferentiatingVisitor;
import offeneBibel.visitorPattern.IVisitor;

public class ObVerseStatisticVisitor extends DifferentiatingVisitor<ObAstNode> implements IVisitor<ObAstNode>
{

    private final String chapName;
    private int currentFassung = -1;
    // 0 - 7 : ordinals based on ObVerseStatus, 8 = empty (only whitespace!), 9
    // = missing
    private final int[][] statusCounters = new int[2][10];
    private final List<List<String>> remainingVerses;

    public ObVerseStatisticVisitor(String chapName, List<String> allVerses)
    {
        this.chapName = chapName;
        this.remainingVerses = new ArrayList<List<String>>();
        remainingVerses.add(new ArrayList<String>(allVerses));
        remainingVerses.add(new ArrayList<String>(allVerses));
    }

    @Override
    public void visitBeforeDefault(ObAstNode node) throws Throwable
    {
        if (node.getNodeType() == ObAstNode.NodeType.fassung) {
            ObFassungNode fassung = (ObFassungNode) node;
            currentFassung = fassung.getFassung().ordinal();
        }
    }

    @Override
    public void visitDefault(ObAstNode node) throws Throwable
    {
        if (node.getNodeType() == ObAstNode.NodeType.verse) {
            ObVerseNode verse = (ObVerseNode) node;
            int ordinal = 7-verse.getStatus().ordinal();
            if (verse.getNextSibling() instanceof ObVerseNode) {
                ordinal = 8;
            }
            List<String> verses = remainingVerses.get(currentFassung);
            for (int number = verse.getFromNumber(); number <= verse.getToNumber(); number++) {
                if (!verses.remove("" + number)) {
                    System.out.println("Invalid verse " + chapName + ":" + number);
                }
                statusCounters[currentFassung][ordinal]++;
            }
        }
    }

    @Override
    public void visitAfterDefault(ObAstNode node) throws Throwable
    {
        if (node.getNodeType() == ObAstNode.NodeType.fassung) {
            currentFassung = -2;
        }
    }

    public int[] getStatusCounters(FassungType fassung) {
        if (remainingVerses.get(fassung.ordinal()).size() > 0) {
            statusCounters[fassung.ordinal()][9] += remainingVerses.get(fassung.ordinal()).size();
            remainingVerses.get(fassung.ordinal()).clear();
        }
        return statusCounters[fassung.ordinal()];
    }
}
