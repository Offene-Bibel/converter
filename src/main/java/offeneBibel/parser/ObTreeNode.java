package offeneBibel.parser;

import java.io.Serializable;
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
 *
 * @param <SELF>
 */
public abstract class ObTreeNode<SELF extends ObTreeNode<SELF>> implements Serializable, IVisitorHost<SELF>{
    protected LinkedList<SELF> m_children;
    protected SELF m_parent;

    public ObTreeNode()
    {
        m_children = new LinkedList<SELF>();
        m_parent = null;
    }

    @SuppressWarnings("unchecked")
    public boolean insertChild(int index, SELF node)
    {
        node.setParent((SELF) this);
        m_children.add(index, node);

        return true;
    }

    @SuppressWarnings("unchecked")
    public boolean appendChild(SELF node)
    {
        node.setParent((SELF) this);
        m_children.add(node);
        return true;
    }

    public SELF removeLastChild()
    {
        m_children.peekLast().m_parent = null;
        return m_children.removeLast();
    }

    public SELF peekChild()
    {
        return m_children.peekLast();
    }

    public int childCount()
    {
        return m_children.size();
    }

    public boolean removeChild(SELF node)
    {
        if(m_children.remove(node) == true) {
            node.m_parent = null;
        }
        return true;
    }

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

    @SuppressWarnings("unchecked")
    public SELF getNextSibling()
    {
        if(m_parent == null)
            return null;
        else
            return (SELF) m_parent.getNextChild((SELF) this);
    }

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

    public SELF getParent()
    {
        return m_parent;
    }

    @Override
    public void host(IVisitor<SELF> visitor) throws Throwable {
        host(visitor, true);
    }

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
