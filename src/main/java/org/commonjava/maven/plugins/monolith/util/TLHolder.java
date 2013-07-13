package org.commonjava.maven.plugins.monolith.util;

public class TLHolder<T>
{

    private final ThreadLocal<T> tl = new ThreadLocal<T>();

    private final Initializer<T> initializer;

    public interface Initializer<C>
    {
        C create();
    }

    public TLHolder( final Initializer<T> initializer )
    {
        this.initializer = initializer;
    }

    public void set( final T value )
    {
        tl.set( value );
    }

    public T rawGet()
    {
        return tl.get();
    }

    public T get()
    {
        T value = tl.get();
        if ( value == null )
        {
            value = initializer.create();
            tl.set( value );
        }

        return value;
    }

    public T clear()
    {
        final T value = tl.get();
        System.out.println( "Clearing thread-local value: " + value );
        tl.set( null );

        return value;
    }

}
