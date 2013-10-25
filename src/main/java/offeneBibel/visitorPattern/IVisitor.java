package offeneBibel.visitorPattern;

/**
 * This interface describes a generic visitor.
 * Recursion will be done automatically by the structure the visitorHost.
 * The "visitBefore" function will be called before a node recurses down
 * into the hierarchy and "visitAfter" is called after recursion.
 * By using reflection one can differentiate between different types of
 * nodes and thus write specific visitor functions for different node
 * types.
 * @param H the type of the visitor host
 * @param R the type of the result of a recursion
 */
public interface IVisitor<H>
{
    /**
     * This function is called by all host nodes.
     * Recursing nodes call it after the recursion step.
     * @param hostNode The host node that is currently visited
     * @throws Throwable
     */
    void visitBefore(H hostNode) throws Throwable;

    /**
     * This function is called on every node. No further guarantees are made.
     * @param hostNode The host node that is currently visited
     * @throws Throwable
     */
    void visit(H hostNode) throws Throwable;

    /**
     * This function is called by all host nodes.
     * Recursing nodes call it after the recursion step.
     * @param hostNode The host node that is currently visited
     * @return
     * @throws Throwable
     */
    void visitAfter(H hostNode) throws Throwable;
}