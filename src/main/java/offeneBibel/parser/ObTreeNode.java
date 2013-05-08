package offeneBibel.parser;

import java.util.LinkedList;

import offeneBibel.visitorPattern.IVisitor;
import offeneBibel.visitorPattern.IVisitorHost;

/**
 * A tree structure implementation.
 * It works with the types of the deriving class.
 * 
 * The generics specification guarantees, that SELF will
 * always be of the objects own type. So "this" can safely be
 * assumed to be of type SELF.
 * @author patrick
 *
 * @param <SELF>
 */
public abstract class ObTreeNode<SELF extends ObTreeNode<SELF>> implements IVisitorHost<SELF>{
	protected LinkedList<SELF> m_children;
	protected SELF m_parent;
	
	public ObTreeNode()
	{
		m_children = new LinkedList<SELF>();
		m_parent = null;
	}
	
	/* (non-Javadoc)
     * @see offeneBibel.parser.IObTreeNode#insertChild(int, offeneBibel.parser.ObTreeNode)
     */
    @SuppressWarnings("unchecked")
    public boolean insertChild(int index, SELF node)
	{
		node.setParent((SELF) this);
		m_children.add(index, node);
		
		return true;
	}
	
	/* (non-Javadoc)
     * @see offeneBibel.parser.IObTreeNode#appendChild(offeneBibel.parser.ObTreeNode)
     */
    @SuppressWarnings("unchecked")
    public boolean appendChild(SELF node)
	{
		node.setParent((SELF) this);
		m_children.add(node);
		return true;
	}
	
	/* (non-Javadoc)
     * @see offeneBibel.parser.IObTreeNode#removeLastChild()
     */
    public SELF removeLastChild()
	{
		m_children.peekLast().m_parent = null;
		return m_children.removeLast();
	}
	
	/* (non-Javadoc)
     * @see offeneBibel.parser.IObTreeNode#peekChild()
     */
    public SELF peekChild()
	{
		return m_children.peek();
	}
	
	/* (non-Javadoc)
     * @see offeneBibel.parser.IObTreeNode#childCount()
     */
    public int childCount()
	{
		return m_children.size();
	}
	
	/* (non-Javadoc)
     * @see offeneBibel.parser.IObTreeNode#removeChild(offeneBibel.parser.ObTreeNode)
     */
    public boolean removeChild(SELF node)
	{
		if(m_children.remove(node) == true) {
			node.m_parent = null;
		}
		return true;
	}
	
	/* (non-Javadoc)
     * @see offeneBibel.parser.IObTreeNode#getNextChild(offeneBibel.parser.IObTreeNode)
     */
    public SELF getNextChild(SELF child)
	{
		int position = m_children.indexOf(child);
		if(position == -1 || position == m_children.size()-1) {
			return null;
		}
		else {
			return m_children.get(position + 1);
		}
	}
	
	/* (non-Javadoc)
     * @see offeneBibel.parser.IObTreeNode#getPreviousChild(offeneBibel.parser.IObTreeNode)
     */
    public SELF getPreviousChild(SELF child)
	{
		int position = m_children.indexOf(child);
		if(position == -1 || position == 0) {
			return null;
		}
		else {
			return m_children.get(position - 1);
		}
	}
	
	/* (non-Javadoc)
     * @see offeneBibel.parser.IObTreeNode#getNextSibling()
     */
    @SuppressWarnings("unchecked")
    public SELF getNextSibling()
	{
		if(m_parent == null)
			return null;
		else
			return (SELF) m_parent.getNextChild((SELF) this);
	}
	
	/* (non-Javadoc)
     * @see offeneBibel.parser.IObTreeNode#getPreviousSibling()
     */
    @SuppressWarnings("unchecked")
    public SELF getPreviousSibling()
	{
		if(m_parent == null)
			return null;
		else
			return m_parent.getPreviousChild((SELF) this);
	}
	
	protected boolean setParent(SELF node)
	{
		if(m_parent != null) {
			m_parent.m_children.remove(this);
			m_parent = null;
		}
		m_parent = node;
		return true;
	}
	
	/* (non-Javadoc)
     * @see offeneBibel.parser.IObTreeNode#getParent()
     */
    public SELF getParent()
	{
		return m_parent;
	}
	
	/* (non-Javadoc)
     * @see offeneBibel.parser.IObTreeNode#host(offeneBibel.visitorPattern.IVisitor)
     */
	@Override
    public void host(IVisitor<SELF> visitor) throws Throwable {
		host(visitor, true);
	}

	/* (non-Javadoc)
     * @see offeneBibel.parser.IObTreeNode#host(offeneBibel.visitorPattern.IVisitor, boolean)
     */
	@SuppressWarnings("unchecked")
    @Override
    public void host(IVisitor<SELF> visitor, boolean inclusive) throws Throwable {
		if(inclusive) {
			visitor.visitBefore((SELF) this);
			visitor.visit((SELF) this);
		}
		
		for(SELF child : m_children) {
			child.host(visitor);
		}
		
		if(inclusive) {
			visitor.visitAfter((SELF) this);
		}
	}
	
	/* (non-Javadoc)
     * @see offeneBibel.parser.IObTreeNode#isDescendantOf(java.lang.Class)
     */
    public boolean isDescendantOf(Class<? extends SELF> classType) {
		@SuppressWarnings("unchecked")
        SELF runner = (SELF) this;
		while(runner != null && ! classType.isInstance(runner)) {
			runner = runner.getParent();
		}
		
		if (runner == null)
			return false;
		else
			return true;
	}
}
