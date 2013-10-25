package offeneBibel.parser;

import java.util.List;
import java.util.Vector;

import offeneBibel.visitorPattern.IVisitor;

public class ObAstFixuper
{
    public static void fixupAstTree(ObAstNode tree)
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

    private static class ObAstNoteLinkSetter implements IVisitor<ObAstNode>
    {
        public List<ObNoteNode> m_noteList = new Vector<ObNoteNode>();

        public ObAstNoteLinkSetter(List<ObNoteNode> noteList)
        {
            m_noteList = noteList;
        }

        @Override
        public void visit(ObAstNode node) throws Throwable
        {
            if(node.getNodeType() == ObAstNode.NodeType.noteLink) {
                ObNoteLinkNode link = (ObNoteLinkNode) node;
                for(ObNoteNode note : m_noteList) {
                    if(link.getTag().equals(note.getTag())) {
                        link.setLinkTarget(note);
                    }
                }
            }
        }

        @Override
        public void visitBefore(ObAstNode node) throws Throwable {}
        @Override
        public void visitAfter(ObAstNode node) throws Throwable {}
    }

    private static class ObAstNoteSearcher implements IVisitor<ObAstNode>
    {
        public List<ObNoteNode> m_noteList = new Vector<ObNoteNode>();

        @Override
        public void visit(ObAstNode node) throws Throwable
        {
            if(node.getNodeType() == ObAstNode.NodeType.note) {
                m_noteList.add((ObNoteNode) node);
            }
        }

        @Override
        public void visitBefore(ObAstNode node) throws Throwable {}
        @Override
        public void visitAfter(ObAstNode node) throws Throwable {}
    }
}