/*
 * Created on 06.04.2006
 */
/*
 * Copyright (c) 2006 Gregor Heinrich. All rights reserved. Redistribution and
 * use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met: 1. Redistributions of source
 * code must retain the above copyright notice, this list of conditions and the
 * following disclaimer. 2. Redistributions in binary form must reproduce the
 * above copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the distribution.
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESSED OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.knowceans.util;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * Which is a debug class that provides a stacktrace at runtime
 *
 * @author gregor
 */
public class Which {

    static class StaticTest {
        void testrun() {
            System.out.println(Which.stack());
            System.out.println(Which.thisclass(this));
        }
    }

    private static String mainClass;

    public static void main(String[] args) {
        run();
    }

    private static void run() {
        System.out.println(Which.thread());
        System.out.println(Which.stack());
        new StaticTest().testrun();
        new Thread() {
            public void run() {
                System.out.println(Which.stack());
                System.out.println(Which.thisclass(this));
            };
        }.start();
    }

    /**
     * returns a stack trace at the current point
     *
     * @return
     */
    public synchronized static List<StackTraceElement> fullstack() {
        String clazz = null;
        try {
            throw new Exception();
        } catch (Exception e) {
            List<StackTraceElement> t = Arrays.asList(e.getStackTrace());
            t = t.subList(1, t.size());
            return t;
        }
    }

    /**
     * returns a stack trace at the current point, without package names and
     * line numbers
     *
     * @return
     */
    public synchronized static List<String> stack() {
        List<StackTraceElement> t = fullstack();
        t = t.subList(1, t.size());
        Vector<String> v = new Vector<String>();
        for (StackTraceElement e : t) {

            String s = getShortClass(e);
            v.add(s + "." + e.getMethodName());
        }
        return v;
    }

    private synchronized static String getShortClass(StackTraceElement e) {
        String c = e.getClassName();
        String f = e.getFileName();
        f = f.substring(0, f.length() - 5);

        String d = shortClassName(c);
        d = resolveAnonymous(c, d);

        int i = d.indexOf('$');

        if (d.startsWith(f) && (i == f.length() || i == -1)) {
            return d;
        }
        d = "[" + f + "]" + d;
        return d;

    }

    /**
     * @param c
     * @return
     */
    private synchronized static String shortClassName(String c) {
        String d = c.substring(c.lastIndexOf('.') + 1);
        // PatternString pc = PatternString.create(c);
        // String d = null;
        // pc.find("((?:\\w+\\$)?\\w+)$");

        return d;
    }

    /**
     * resolve the anonymous supertype of the class name c
     *
     * @param c class name
     * @param d short class name
     * @return
     */
    private synchronized static String resolveAnonymous(String c, String d) {
        // if anonymous
        if (d.matches(".*\\$\\d+")) {
            String g = "Object";
            try {
                Class x = Class.forName(c);
                g = x.getGenericSuperclass().toString();
                System.out.println(g);
                g = g.substring(g.lastIndexOf('.') + 1);
            } catch (ClassNotFoundException e1) {
                e1.printStackTrace();
            }
            d = d + "(" + g + ")";
        }
        return d;
    }

    /**
     * resolve the anonymous supertype of the object
     *
     * @param c object
     * @param d short class name
     * @return
     */
    private synchronized static String resolveAnonymous(Object c, String d) {
        String g;
        // if anonymous
        if (d.matches(".*\\$\\d+")) {
            Class x = c.getClass();
            g = x.getGenericSuperclass().toString();
            System.out.println(g);
            g = shortClassName(g);
        } else {
            return d;
        }
        d = d + "(" + g + ")";
        return d;

    }

    /**
     * class that this thread was started with and the thread name
     *
     * @return
     */
    public synchronized static String thread() {
        String clazz = null;
        try {
            throw new Exception();
        } catch (Exception e) {
            StackTraceElement[] t = e.getStackTrace();
            clazz = t[t.length - 1].getClassName();
            clazz = shortClassName(clazz);
            clazz += "[" + Thread.currentThread().getName() + "]";
        }
        return clazz;
    }

    /**
     * return the class that the main method was started with.
     * <p>
     * FIXME: how can the calling class be determined when its main thread is.
     * As a provisorical method, Which.setMain() is called in the main function.
     * finished?
     *
     * @return
     */
    public synchronized static String main() {
        if (mainClass != null)
            return shortClassName(mainClass);
        Map<Thread, StackTraceElement[]> a = Thread.getAllStackTraces();
        for (Thread th : a.keySet()) {
            if (th.getName().equals("main")) {
                StackTraceElement[] t = a.get(th);
                String clazz = t[t.length - 1].getClassName();
                clazz = shortClassName(clazz);
                return clazz;
            }
        }
        return "impossible: no 'main' thread alive.";
    }

    /**
     * For multithreaded applications that finish the main thread, call this
     * method in the main() method. The first call to this method is kept.
     */
    public static void setMain() {
        if (mainClass == null) {
            StackTraceElement[] t = Thread.currentThread().getStackTrace();
            mainClass = t[t.length - 1].getClassName();
        }
    }

    /**
     * short description of runtime class (with supertype if anonymous)
     *
     * @param t
     * @return
     */
    public synchronized static Object thisclass(Object t) {
        String s = t.getClass().toString();
        s = shortClassName(s);
        s = resolveAnonymous(t, s);
        return s;
    }

    /**
     * get the memory currently used as a double (in megabytes) rounded to two
     * decimal digits.
     *
     * @return
     */
    public static double usedMemory() {
        return Math.round((Runtime.getRuntime().totalMemory() - Runtime
            .getRuntime().freeMemory()) / 10.24 / 1024.) / 100.;
    }
}
