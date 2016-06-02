package offeneBibel.osisExporter;

import java.util.ArrayList;
import java.util.List;

import offeneBibel.parser.AstNode;
import offeneBibel.parser.FassungNode;
import offeneBibel.parser.FassungNode.FassungType;
import offeneBibel.parser.VerseNode;
import offeneBibel.visitorPattern.DifferentiatingVisitor;
import offeneBibel.visitorPattern.IVisitor;

public class VerseStatisticVisitor extends DifferentiatingVisitor<AstNode> implements IVisitor<AstNode>
{

    private final String chapName;
    private int currentFassung = -1;
    // 0 - 7 : ordinals based on ObVerseStatus, 8 = empty (only whitespace!), 9
    // = missing
    private final int[][] statusCounters = new int[2][10];
    private final List<List<String>> remainingVerses;

    public VerseStatisticVisitor(String chapName, List<String> allVerses)
    {
        this.chapName = chapName;
        this.remainingVerses = new ArrayList<List<String>>();
        remainingVerses.add(new ArrayList<String>(allVerses));
        remainingVerses.add(new ArrayList<String>(allVerses));
    }

    @Override
    public void visitBeforeDefault(AstNode node) throws Throwable
    {
        if (node.getNodeType() == AstNode.NodeType.fassung) {
            FassungNode fassung = (FassungNode) node;
            currentFassung = fassung.getFassung().ordinal();
        }
    }

    @Override
    public void visitDefault(AstNode node) throws Throwable
    {
        if (node.getNodeType() == AstNode.NodeType.verse) {
            VerseNode verse = (VerseNode) node;
            int ordinal = 7-verse.getStatus().ordinal();
            if (verse.getNextSibling() instanceof VerseNode) {
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
    public void visitAfterDefault(AstNode node) throws Throwable
    {
        if (node.getNodeType() == AstNode.NodeType.fassung) {
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
