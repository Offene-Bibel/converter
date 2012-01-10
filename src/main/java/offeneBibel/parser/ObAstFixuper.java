package offeneBibel.parser;

import java.util.List;
import java.util.Vector;

import offeneBibel.parser.ObAstNode;
import offeneBibel.parser.ObTreeNode;
import offeneBibel.visitorPattern.IVisitor;

public class ObAstFixuper
{
	public static void fixupAstTree(ObTreeNode tree)
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
	
	private static class ObAstNoteLinkSetter implements IVisitor<ObTreeNode>
	{
		public List<ObNoteNode> m_noteList = new Vector<ObNoteNode>();
		
		public ObAstNoteLinkSetter(List<ObNoteNode> noteList)
		{
			m_noteList = noteList;
		}
		
		public void visit(ObTreeNode node) throws Throwable
		{
			ObAstNode astNode = (ObAstNode)node;
			
			if(astNode.getNodeType() == ObAstNode.NodeType.noteLink) {
				ObNoteLinkNode link = (ObNoteLinkNode) node;
				for(ObNoteNode note : m_noteList) {
					if(link.getTag().equals(note.getTag())) {
						link.setLinkTarget(note);
					}
				}
			}
		}
	
		public void visitBefore(ObTreeNode node) throws Throwable {}
		public void visitAfter(ObTreeNode node) throws Throwable {}
	}
	
	private static class ObAstNoteSearcher implements IVisitor<ObTreeNode>
	{
		public List<ObNoteNode> m_noteList = new Vector<ObNoteNode>();
		
		public void visit(ObTreeNode node) throws Throwable
		{
			ObAstNode astNode = (ObAstNode)node;
			if(astNode.getNodeType() == ObAstNode.NodeType.note) {
				m_noteList.add((ObNoteNode) node);
			}
		}
	
		public void visitBefore(ObTreeNode node) throws Throwable {}
		public void visitAfter(ObTreeNode node) throws Throwable {}
	}
}