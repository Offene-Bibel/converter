package offeneBibel.parser;

public class AstNode extends TreeNode<AstNode> { // implements IVisitorHost<ObAstNode> {
    private static final long serialVersionUID = 1L;

    public enum NodeType {
        fassung,
        fassungNotes,
        chapter,
        verse,
        text,
        fat,
        italics,
        insertion,
        omission,
        alternative,
        poemStart,
        poemStop,
        textBreak,
        parallelPassage,
        note,
        noteLink,
        hebrew,
        wikiLink,
        superScript,
        heading,
        quote,
        chapterNotes,
        alternateReading,
        secondaryContent,
        secondVoice,
        strikeThrough,
        underline,
    }
    private NodeType nodeType;

    public AstNode(NodeType type)
    {
        setNodeType(type);
    }

    public NodeType getNodeType() {
        return nodeType;
    }

    public boolean setNodeType(NodeType nodeType) {
        this.nodeType = nodeType;
        return true;
    }

    public boolean isDescendantOf(NodeType nodeType) {
        AstNode runner =  this;
        while(runner != null && runner.getNodeType() != nodeType)
            runner = runner.getParent();

        if (runner == null)
            return false;
        else
            return true;
    }
}
