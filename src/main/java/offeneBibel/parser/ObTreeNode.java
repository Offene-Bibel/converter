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
	
	public ObTreeNode getNextChild(ObTreeNode child)
	{
		int position = m_children.indexOf(child);
		if(position == -1 || position == m_children.size()-1) {
			return null;
		}
		else {
			return m_children.get(position + 1);
		}
	}
	
	public ObTreeNode getPreviousChild(ObTreeNode child)
	{
		int position = m_children.indexOf(child);
		if(position == -1 || position == 0) {
			return null;
		}
		else {
			return m_children.get(position - 1);
		}
	}
	
	public ObTreeNode getNextSibling()
	{
		if(m_parent == null)
			return null;
		else
			return m_parent.getNextChild(this);
	}
	
	public ObTreeNode getPreviousSibling()
	{
		if(m_parent == null)
			return null;
		else
			return m_parent.getPreviousChild(this);
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
	
	public boolean isDescendantOf(Class<? extends ObTreeNode> classType) {
		ObTreeNode runner = this;
		while(runner != null && ! classType.isInstance(runner)) {
			runner = runner.getParent();
		}
		
		if (runner == null)
			return false;
		else
			return true;
	}
}
