package org.knowceans.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

/**
 * DataThreadPool is a thread queue that keeps data that can be associated with
 * specific runnables. So only as many data are needed as there are threads, not
 * runnables (tasks) that implement the DataTask interface. <br/>
 * To use this, allocate an array of the data required in the workers,
 * instantiate the pool and add tasks which are executed from an internal queue.
 * Using the notifyCompletion() method allows the caller to wait for completion
 * of all tasks in the queue.
 * 
 * @author gregor
 */
public class DataThreadPool implements Serializable {
    private static final long serialVersionUID = 8039512463872181728L;
    public int nThreads;
    private int active = 0;
    private boolean stopping = false;
    private final WorkerThread[] threads;
    private LinkedList<DataTask> queue;
    private Object[] data;
    private Object completionMonitor;

    /**
     * create a thread pool of size nThreads which are assigned data (an array
     * of nThreads objects)
     * 
     * @param nThreads
     * @param data may be null
     */
    public DataThreadPool(int nThreads, Object[] data) {
        this.nThreads = nThreads;
        queue = new LinkedList<DataTask>();
        threads = new WorkerThread[nThreads];
        this.data = data;
        start();
    }

    /**
     * same as other constructor, with an initial
     * 
     * @param nThreads
     * @param data
     * @param queue
     */
    public DataThreadPool(int nThreads, Object[] data,
        Collection<DataTask> queue) {
        this(nThreads, data);
        add(new ArrayList<DataTask>());
    }

    /**
     * start executing the queue
     */
    public void start() {
        for (int i = 0; i < nThreads; i++) {
            threads[i] = new WorkerThread(i);
            threads[i].start();
        }
    }

    public void add(DataTask task) {
        synchronized (queue) {
            queue.addLast(task);
            queue.notify();
        }
    }

    public void add(Collection< ? extends DataTask> tasks) {
        // add the jobs atomically to prevent active count 
        // to become 0 (fast tasks, long queue) and
        // and thus the queue to exit prematurely
        synchronized (queue) {
            for (DataTask r : tasks) {
                queue.addLast(r);
                queue.notify();
            }
        }
    }

    public int getActive() {
        return active;
    }

    /**
     * called this before monitor.wait() to wait for completion of all tasks in
     * the pool's queue.
     */
    public void notifyCompletion(Object monitor) {
        completionMonitor = monitor;
    }

    /**
     * complete the current tasks and stop
     */
    public void finish() {
        this.stopping = true;
    }

    /**
     * whether this queue is stopping
     * 
     * @return
     */
    public boolean isStopping() {
        return stopping;
    }

    /**
     * WorkerThread is a thread that runs data runnables.
     * 
     * @author gregor
     */
    private class WorkerThread extends Thread implements Serializable {
        private static final long serialVersionUID = -672285282641278383L;
        int channel;

        public WorkerThread(int i) {
            channel = i;
        }

        public void run() {
            DataTask task;

            while (!isStopping()) {
                synchronized (queue) {
                    while (queue.isEmpty()) {
                        try {
                            queue.wait();
                        } catch (InterruptedException e) {
                            // ignore
                        }
                    }
                    // get next task
                    task = queue.removeFirst();
                    // assign data
                    task.assignData(data[channel]);
                    active++;
                }
                try {
                    task.run();
                } catch (RuntimeException e) {
                    e.printStackTrace();
                }
                synchronized (queue) {
                    active--;
                }
                if (completionMonitor != null && active == 0 && queue.isEmpty()) {
                    synchronized (completionMonitor) {
                        completionMonitor.notify();
                    }
                }
            }
        }
    }
}
