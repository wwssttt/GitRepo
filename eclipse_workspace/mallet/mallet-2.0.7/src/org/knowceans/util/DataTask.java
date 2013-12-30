/*
 * Created on Jul 27, 2009
 */
package org.knowceans.util;

import java.io.Serializable;

/**
 * DataTask represents an algorithm that can run in a thread of a
 * DataThreadPool. The idea is to reuse data structures and save memory. These
 * structures are allocated by the DataThreadPool and assigned to the different
 * runnables.
 * 
 * @author gregor
 */
public interface DataTask extends Runnable, Serializable {

    /**
     * @param data assigns worker-specific data arrays to this thread
     */
    void assignData(Object data);

}
