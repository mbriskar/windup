package org.jboss.windup.util.threading;

import java.util.concurrent.ThreadFactory;

/**
 * A threadgroup-aware thread factory. Thread-groups are useful to distinguish threads that were created from the given executor.
 */
public class ThreadGroupThreadFactory implements ThreadFactory
{

    private ThreadGroup threadGroup;

    public ThreadGroupThreadFactory(ThreadGroup threadGroup)
    {
        this.threadGroup = threadGroup;
    }

    @Override
    public Thread newThread(Runnable r)
    {
        Thread t = new Thread(threadGroup,r);
        t.setDaemon(true);
        return t;
    }
}
