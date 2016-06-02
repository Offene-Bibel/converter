package offeneBibel.parser;

import java.util.List;
import java.util.Vector;

import offeneBibel.visitorPattern.IVisitor;

public class AstFixuper
{
    public static void fixupAstTree(AstNode tree)
    {
        try {
            ObAstNoteSearcher noteSearcher = new ObAstNoteSearcher();
            tree.host(noteSearcher);

            ObAstNoteLinkSetter linkSetter = new ObAstNoteLinkSetter(noteSearcher.noteList);
            tree.host(linkSetter);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private static class ObAstNoteLinkSetter implements IVisitor<AstNode>
    {
        public List<NoteNode> noteList = new Vector<NoteNode>();

        public ObAstNoteLinkSetter(List<NoteNode> noteList)
        {
            this.noteList = noteList;
        }

        @Override
        public void visit(AstNode node) throws Throwable
        {
            if(node.getNodeType() == AstNode.NodeType.noteLink) {
                NoteLinkNode link = (NoteLinkNode) node;
                for(NoteNode note : noteList) {
                    if(link.getTag().equals(note.getTag())) {
                        link.setLinkTarget(note);
                    }
                }
            }
        }

        @Override
        public void visitBefore(AstNode node) throws Throwable {}
        @Override
        public void visitAfter(AstNode node) throws Throwable {}
    }

    private static class ObAstNoteSearcher implements IVisitor<AstNode>
    {
        public List<NoteNode> noteList = new Vector<NoteNode>();

        @Override
        public void visit(AstNode node) throws Throwable
        {
            if(node.getNodeType() == AstNode.NodeType.note) {
                noteList.add((NoteNode) node);
            }
        }

        @Override
        public void visitBefore(AstNode node) throws Throwable {}
        @Override
        public void visitAfter(AstNode node) throws Throwable {}
    }
}