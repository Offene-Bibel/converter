package offeneBibel.visitorPattern;

import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is a helper abstract class that can be used to write
 * visitors that differentiate between the different hosts
 * they visit.
 * One can directly start to write visitor functions for the
 * specific host classes, they will be selected and called
 * automatically.
 * One has to implement the visitBeforeDefault, visitDefault
 * and visitAfterDefault classes. They will be called if no
 * other function is found that matches the given host.
 * 
 * This class also provides another helpful feature, turning
 * off recursion for a subtree. This is useful if one wants
 * to do the recursion oneself.
 */
public abstract class DifferentiatingVisitor<H> implements IVisitor<H>
{
    private final static Logger log = Logger.getLogger(DifferentiatingVisitor.class.getName());

    private boolean noRecursion = false;
    private int noRecursionLevel = 0;

    protected abstract void visitBeforeDefault(H host) throws Throwable;
    protected abstract void visitDefault(H host) throws Throwable;
    protected abstract void visitAfterDefault(H host) throws Throwable;

    protected void stopRecursion()
    {
        if (this.noRecursion == false) // this check should not be neccessary, because no calls are left through to the visitor, but we check anyways
        {
            this.noRecursionLevel = 0;
            this.noRecursion = true;
        }
        else
        {
            RuntimeException ex = new RuntimeException("The method \"stopRecursion()\" was called where it logically can't be called, this is an error or bug.");
            log.log(Level.SEVERE, "", ex);
            throw ex;
        }
    }

    @Override
    public void visitBefore(H host) throws Throwable
    {
        if (noRecursion) // if no recursion mode is turned on we increase the noRecursionLevel, but don't do anything else
        {
            noRecursionLevel++;
            return;
        }

        Class<? extends Object> argumentType = host.getClass();

        try {
            //reflection, it searches for a method in the current class with name "visit" and one argument, of the type of argumentType, this method is then invoked
            this.getClass().getMethod( "visitBefore", argumentType).invoke( this, host);
        }
        catch (IllegalArgumentException e)	{e.printStackTrace();}
        catch (SecurityException e)			{e.printStackTrace();}
        catch (IllegalAccessException e)	{e.printStackTrace();}
        catch (InvocationTargetException e)	{throw e.getCause();}
        catch (NoSuchMethodException e)
        {
            // no function was found that works with this argument, we'll call the default one
            this.visitBeforeDefault(host);
        }
    }

    @Override
    public void visit(H host) throws Throwable
    {
        if (noRecursion) // if no recursion mode is turned on we don't do anything
            return;

        Class<? extends Object> argumentType = host.getClass();

        try {
            //reflection, it searches for a method in the current class with name "visit" and one argument, of the type of argumentType, this method is then invoked
            this.getClass().getMethod( "visit", argumentType).invoke( this, host);
        }
        catch (IllegalArgumentException e)	{e.printStackTrace();}
        catch (SecurityException e)			{e.printStackTrace();}
        catch (IllegalAccessException e)	{e.printStackTrace();}
        catch (InvocationTargetException e)	{throw e.getCause();}
        catch (NoSuchMethodException e)
        {
            // no function was found that works with this argument, we'll call the default one
            this.visitDefault(host);
        }
    }

    @Override
    public void visitAfter(H host) throws Throwable
    {
        if (noRecursion) // noRecursion is turned on
        {
            if (noRecursionLevel == 0) // check if we are back to the node that started the mode
            { // we are, exit recursion mode and continue
                noRecursion = false;
            }
            else
            { // we aren't, decrease recursion level and skip
                noRecursionLevel--;
                return;
            }
        }

        Class<? extends Object> argumentType = host.getClass();

        try {
            //reflection, it searches for a method in the current class with name "visit" and one argument, of the type of argumentType, this method is then invoked
            this.getClass().getMethod( "visitAfter", argumentType).invoke( this, host);
        }
        catch (IllegalArgumentException e)	{e.printStackTrace();}
        catch (SecurityException e)			{e.printStackTrace();}
        catch (IllegalAccessException e)	{e.printStackTrace();}
        catch (InvocationTargetException e)	{throw e.getCause();}
        catch (NoSuchMethodException e)
        {
            // no function was found that works with this argument, we'll call the default one
            this.visitAfterDefault(host);
        }
    }
}