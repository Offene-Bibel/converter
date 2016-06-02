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
public abstract class TreeNode<SELF extends TreeNode<SELF>> implements Serializable, IVisitorHost<SELF>{
    private static final long serialVersionUID = 1L;
    protected LinkedList<SELF> children;
    protected SELF parent;

    public TreeNode()
    {
        children = new LinkedList<SELF>();
        parent = null;
    }

    @SuppressWarnings("unchecked")
    public boolean insertChild(int index, SELF node)
    {
        node.setParent((SELF) this);
        children.add(index, node);

        return true;
    }

    @SuppressWarnings("unchecked")
    public boolean appendChild(SELF node)
    {
        node.setParent((SELF) this);
        children.add(node);
        return true;
    }

    public SELF removeLastChild()
    {
        children.peekLast().parent = null;
        return children.removeLast();
    }

    public SELF peekChild()
    {
        return children.peekLast();
    }

    public int childCount()
    {
        return children.size();
    }

    public boolean removeChild(SELF node)
    {
        if(children.remove(node) == true) {
            node.parent = null;
        }
        return true;
    }

    public SELF getNextChild(SELF child)
    {
        int position = children.indexOf(child);
        if(position == -1 || position == children.size()-1) {
            return null;
        }
        else {
            return children.get(position + 1);
        }
    }

    public SELF getPreviousChild(SELF child)
    {
        int position = children.indexOf(child);
        if(position == -1 || position == 0) {
            return null;
        }
        else {
            return children.get(position - 1);
        }
    }

    @SuppressWarnings("unchecked")
    public SELF getNextSibling()
    {
        if(parent == null)
            return null;
        else
            return (SELF) parent.getNextChild((SELF) this);
    }

    @SuppressWarnings("unchecked")
    public SELF getPreviousSibling()
    {
        if(parent == null)
            return null;
        else
            return parent.getPreviousChild((SELF) this);
    }

    protected boolean setParent(SELF node)
    {
        if(parent != null) {
            parent.children.remove(this);
            parent = null;
        }
        parent = node;
        return true;
    }

    public SELF getParent()
    {
        return parent;
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

        for(SELF child : children) {
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
