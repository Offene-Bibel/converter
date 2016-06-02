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

            ObAstNoteLinkSetter linkSetter = new ObAstNoteLinkSetter(noteSearcher.m_noteList);
            tree.host(linkSetter);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private static class ObAstNoteLinkSetter implements IVisitor<AstNode>
    {
        public List<NoteNode> m_noteList = new Vector<NoteNode>();

        public ObAstNoteLinkSetter(List<NoteNode> noteList)
        {
            m_noteList = noteList;
        }

        @Override
        public void visit(AstNode node) throws Throwable
        {
            if(node.getNodeType() == AstNode.NodeType.noteLink) {
                NoteLinkNode link = (NoteLinkNode) node;
                for(NoteNode note : m_noteList) {
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
        public List<NoteNode> m_noteList = new Vector<NoteNode>();

        @Override
        public void visit(AstNode node) throws Throwable
        {
            if(node.getNodeType() == AstNode.NodeType.note) {
                m_noteList.add((NoteNode) node);
            }
        }

        @Override
        public void visitBefore(AstNode node) throws Throwable {}
        @Override
        public void visitAfter(AstNode node) throws Throwable {}
    }
}