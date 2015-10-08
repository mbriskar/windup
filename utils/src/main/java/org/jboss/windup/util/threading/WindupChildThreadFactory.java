package org.jboss.windup.util.threading;

import java.util.concurrent.ThreadFactory;

/**
 * A threadgroup-aware thread factory. Thread-groups are useful to distinguish threads that were created from the given executor.
 */
public class WindupChildThreadFactory implements ThreadFactory
{

    private Thread mainWindupThread;

    public WindupChildThreadFactory(Thread mainWindupThread)
    {
        this.mainWindupThread = mainWindupThread;
    }

    @Override
    public Thread newThread(Runnable r)
    {
        Thread t = new WindupChildThread(mainWindupThread,r);
        t.setDaemon(true);
        return t;
    }
}
