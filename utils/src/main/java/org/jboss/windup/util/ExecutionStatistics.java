package org.jboss.windup.util;

import java.io.FileWriter;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

/**
 * <p>
 * {@link ExecutionStatistics} provides a simple system for storing the time taken to perform operations.
 * </p>
 * 
 * <p>
 * Example usage:
 * 
 * <pre>
 * ExecutionStatistics.get().begin(&quot;Process-01&quot;);
 * // ... your code to be timed goes here
 * ExecutionStatistics.get().end(&quot;Process-01&quot;);
 * </pre>
 * 
 * </p>
 * 
 * @author <a href="mailto:jesse.sightler@gmail.com">Jesse Sightler</a>
 *
 */
public class ExecutionStatistics
{
    private static Logger LOG = Logging.get(ExecutionStatistics.class);

    private static Map<Thread,ExecutionStatistics> stats = new HashMap<>();
    private Map<String, TimingData> executionInfo = new HashMap<>();


    private ExecutionStatistics()
    {

    }

    /**
     * Gets the instance associated with the current thread.
     */
    public static synchronized ExecutionStatistics get()
    {
        if (stats.get(Thread.currentThread()) == null)
        {
            stats.put(Thread.currentThread(),new ExecutionStatistics());
        }
        return stats.get(Thread.currentThread());
    }

    public static synchronized ExecutionStatistics get(Thread thread)
    {
        return stats.get(thread);
    }


    public void clear(Iterable<Thread> threads) {
        for (Thread thread : threads)
        {
            stats.remove(thread);
        }
    }

    public ExecutionStatistics mergeSiblingThreads() {
        int numberOfSiblingThreads = Thread.currentThread().getThreadGroup().activeCount();
        Thread[] threads = new Thread[numberOfSiblingThreads];
        Thread.currentThread().getThreadGroup().enumerate(threads);
        for(int i=0;i<threads.length;i++) {
            merge(stats.get(threads[i]));
        }
        return this;
    }


    public void merge(ExecutionStatistics statistics) {
        for (String key : statistics.executionInfo.keySet())
        {
            TimingData otherTimingData = statistics.executionInfo.get(key);
            TimingData currentTimingData = executionInfo.get(key);
            if(currentTimingData == null) {
                executionInfo.put(key,otherTimingData);
            } else {
                currentTimingData.merge(otherTimingData);
                executionInfo.put(key, currentTimingData);
            }
        }
    }

    /**
     * Clears the current threadlocal as well as any current state.
     */
    public void reset()
    {
        stats.remove(Thread.currentThread());
        executionInfo.clear();
    }

    /**
     * Serializes the timing data to a "~" delimited file at outputPath.
     */
    public void serializeTimingData(Path outputPath)
    {
        try (FileWriter fw = new FileWriter(outputPath.toFile()))
        {
            fw.write("Type~Number Of Executions~Total Milliseconds~Milliseconds per execution\n");
            for (Map.Entry<String, TimingData> timing : executionInfo.entrySet())
            {
                TimingData data = timing.getValue();
                long totalMillis = (data.totalNanos / 1000000);
                double millisPerExecution = (double) totalMillis / (double) data.numberOfExecutions;
                fw.write(timing.getKey() + "~" + data.numberOfExecutions + "~" + totalMillis + "~" + millisPerExecution);
                fw.write("\n");
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static <T> T performBenchmarked(String key, Task<T> operation)
    {
        ExecutionStatistics instance = ExecutionStatistics.get();
        instance.begin(key);
        try
        {
            return operation.execute();
        }
        finally
        {
            instance.end(key);
        }
    }

    /**
     * Start timing an operation with the given identifier.
     */
    public void begin(String key)
    {
        if (key == null)
        {
            return;
        }
        TimingData data = executionInfo.get(key);
        if (data == null)
        {
            data = new TimingData(key);
            executionInfo.put(key, data);
        }
        data.begin();
    }

    /**
     * Complete timing the operation with the given identifier. If you had not previously started a timing operation with this identifier, then this
     * will effectively be a noop.
     */
    public void end(String key)
    {
        if (key == null)
        {
            return;
        }
        TimingData data = executionInfo.get(key);
        if (data == null)
        {
            LOG.info("Called end with key: " + key + " without ever calling begin");
            return;
        }
        data.end();
    }

    private class TimingData
    {
        private String key;
        private long startTime;
        private long numberOfExecutions;
        private long totalNanos;

        public TimingData(String key)
        {
            this.key = key;
        }

        public void begin()
        {
            this.startTime = System.nanoTime();
        }

        public void end()
        {
            if (this.startTime == 0)
            {
                LOG.info("Called end with key: " + this.key + " without ever calling begin");
                return;
            }
            this.totalNanos += (System.nanoTime() - startTime);
            this.startTime = 0;
            this.numberOfExecutions++;
        }

        public void merge(TimingData other) {
            this.numberOfExecutions += other.numberOfExecutions;
            this.totalNanos = other.totalNanos;
        }
    }
}
