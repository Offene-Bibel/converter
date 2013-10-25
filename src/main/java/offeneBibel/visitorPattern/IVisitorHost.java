package offeneBibel.visitorPattern;

/**
 * This interface describes a generic visitor host.
 * A visitor host is always implemented by some sort of recursive structure.
 * It should call the visitor on all elements of this recursive
 * structure.
 * @param H the type of the visitor host
 */
public interface IVisitorHost<H>
{
    /**
     * This function should take a visitor and call it's visitBefore
     * function before recursion.
     * And should call the visitors visitAfter function
     * after recursion.
     * And call the visitors visit function between the visitBefore
     * and visitAfter function.
     * This method is equivalent to host(visitor, true);
     * @param visitor The visitor that visits this tree structure
     */
    void host(IVisitor<H> visitor) throws Throwable;

    /**
     * This function should take a visitor and call it's visitBefore
     * function before recursion.
     * And should call the visitors visitAfter function
     * after recursion.
     * And call the visitors visit function between the visitBefore
     * and visitAfter function.
     * If inclusive is set to false, only the children should be visited.
     * @param visitor The visitor that visits this tree structure
     * @param inclusive Whether the node itself should be visited.
     */
    void host(IVisitor<H> visitor, boolean inclusive) throws Throwable;
}