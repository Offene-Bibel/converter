package offeneBibel.parser;

import java.util.LinkedList;

import offeneBibel.visitorPattern.IVisitor;
import offeneBibel.visitorPattern.IVisitorHost;


public class ObTreeNode implements IVisitorHost<ObTreeNode>{
	private LinkedList<ObTreeNode> m_children;
	private ObTreeNode m_parent;
	
	public ObTreeNode()
	{
		m_children = new LinkedList<ObTreeNode>();
		m_parent = null;
	}
	
	public boolean insertChild(int index, ObTreeNode node)
	{
		node.setParent(this);
		m_children.add(index, node);
		
		return true;
	}
	
	public boolean appendChild(ObTreeNode node)
	{
		node.setParent(this);
		m_children.add(node);
		return true;
	}
	
	public ObTreeNode removeLastChild()
	{
		m_children.peekLast().m_parent = null;
		return m_children.removeLast();
	}
	
	public boolean pushChild(ObTreeNode node)
	{
		node.setParent(this);
		m_children.push(node);
		return true;
	}
	
	public ObTreeNode popChild()
	{
		m_children.peek().m_parent = null;
		return m_children.pop();
	}
	
	public ObTreeNode peekChild()
	{
		return m_children.peek();
	}
	
	public int childCount()
	{
		return m_children.size();
	}
	
	public boolean removeChild(ObTreeNode node)
	{
		if(m_children.remove(node) == true) {
			node.m_parent = null;
		}
		return true;
	}
	
	private boolean setParent(ObTreeNode node)
	{
		if(m_parent != null) {
			m_parent.m_children.remove(this);
			m_parent = null;
		}
		m_parent = node;
		return true;
	}
	
	public ObTreeNode getParent()
	{
		return m_parent;
	}
	
	public void host(IVisitor<ObTreeNode> visitor) throws Throwable {
		host(visitor, true);
	}

	public void host(IVisitor<ObTreeNode> visitor, boolean inclusive) throws Throwable {
		if(inclusive) {
			visitor.visitBefore(this);
			visitor.visit(this);
		}
		
		for(ObTreeNode child : m_children) {
			child.host(visitor);
		}
		
		if(inclusive) {
			visitor.visitAfter(this);
		}
	}
}
