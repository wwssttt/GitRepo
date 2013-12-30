/*
 * Copyright (c) 2002-2006 Gregor Heinrich. All rights reserved. Redistribution and
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

/**
 * Class to start an external process and connect the three standard streams.
 * The process can be run using the constructor, then optionally connection its
 * output streams stdout and stdin with Java InputStreams using
 * getStreamFromStdout and getStreamFromStderr, then calling waitfor() to wait
 * until the process has finished. Any stdin of the external program can be
 * assigned via the last arg to the constructor; if stdin should be from a
 * String, use StringBufferInputStream. <br>
 * If getStreamFromStdout and getStreamFromStderr are used, they should be
 * buffered and run in a separate thread if possible. If they are not explicitly
 * requested by the caller, internal threads are used that collect the output in
 * a buffer that can be requested from the caller class via getCompleteStdout
 * and getCompleteStderr. Additionally, the threads display the process output
 * in Java's stdout and stderr. <br>
 * TODO: stdin does not work yet.
 * 
 * @author heinrich
 */
public class ExternalProcess {

    ProcessStdOutputThread thdStderr;

    ProcessStdOutputThread thdStdout;

    ProcessStdinThread thdStdin;

    Process p;

    private boolean useInternalStdoutThread = true;

    private boolean useInternalStderrThread = true;

    private InputStream stdin;

    private File workDir;

    /**
     * Initialise the external program. If a String is to be used as an input to
     * the process, StringReader can be used.
     * 
     * @param command
     *            required system-visible command
     * @param options
     *            optional, set null or zero-length if unwanted
     * @param environment
     *            optional, set null or zero-length if unwanted
     * @param stding
     *            stream to connect to stdin of the process, null if not wanted
     * @param workDir
     *            to run the process in
     */
    public ExternalProcess(String command, String[] options,
        String[] environment, InputStream stdin, File workDir) throws Exception {

        if (options == null)
            options = new String[] {};
        if (environment == null)
            environment = new String[] {};
        this.workDir = workDir;

        String cmd[] = new String[options.length + 1];
        cmd[0] = command;
        for (int i = 1; i < cmd.length; i++) {
            cmd[i] = options[i - 1];
        }

        this.p = execute(cmd, environment);
        this.stdin = stdin;

    }

    /**
     * start the process.
     * 
     * @param cmd
     * @param options
     */
    private Process execute(String[] cmd, String[] environment) {
        try {
            if (workDir == null)
                return Runtime.getRuntime().exec(cmd, environment);
            else
                return Runtime.getRuntime().exec(cmd, environment, workDir);

        } catch (java.io.IOException e) {
        }
        return null;
    }

    /**
     * should be called after the process has been started (using the
     * constructor)
     * 
     * @return
     * @throws InterruptedException
     */
    public int waitFor() throws InterruptedException {

        if (stdin != null) {

            thdStdin = new ProcessStdinThread(p.getOutputStream(), stdin);
            if (thdStdin != null)
                thdStdin.start();
        }

        if (useInternalStdoutThread) {
            thdStdout = new ProcessStdOutputThread(p.getInputStream(),
                System.out);
            thdStdout.start();
        }

        if (useInternalStderrThread) {
            thdStderr = new ProcessStdOutputThread(p.getErrorStream(),
                System.err);
            thdStderr.start();
        }

        int waitforCode = -1;
        try {
            waitforCode = p.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (thdStdout != null)
            thdStdout.join();
        if (thdStderr != null)
            thdStderr.join();
        if (thdStdin != null)
            thdStdin.join();
        return waitforCode;
    }

    /**
     * Stdout from the process is processed by the user. To run stable, the
     * stdout stream should be buffered and its processing run in its own
     * thread.
     * 
     * @return the stream from stdout of the process
     */
    public InputStream getStreamFromStdout() {
        useInternalStdoutThread = false;
        return p.getInputStream();
    }

    /**
     * Stderr from the process is processed by the user. see
     * getStreamFromStdout()
     * 
     * @return the stream from stderr of the process
     */
    public InputStream getStreamFromStderr() {
        useInternalStderrThread = false;
        return p.getErrorStream();
    }

    /**
     * get output from the stdout buffer thread.
     * 
     * @return
     */
    public String getCompleteStdout() {
        return thdStdout.getCompleteOutput();
    }

    /**
     * get output from the stderr buffer thread.
     */
    public String getCompleteStderr() {
        return thdStderr.getCompleteOutput();
    }

    /**
     * handle stdin of the external process in a separate thread. This class
     * does not close the input stream provided by the user.
     */
    class ProcessStdinThread extends Thread {

        BufferedWriter toStdin;

        InputStream fromUser = null;

        public ProcessStdinThread(OutputStream toProcessStdin,
            InputStream userInputStream) {
            this.fromUser = userInputStream;
            toStdin = new BufferedWriter(new OutputStreamWriter(toProcessStdin));
        }

        public void run() {
            try {
                int oneByte;
                while ((oneByte = fromUser.read()) != -1)
                    toStdin.write(oneByte);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * handle stdout and stderr of the external process in separate threads.
     * Collects the output from the process and outputs it to the output Stream
     * given in the constructor. This class does not close the output stream
     * provided by the user. TODO: Promote to public class and subclass.
     */
    class ProcessStdOutputThread extends Thread {

        private StringBuffer buffer = new StringBuffer();

        private BufferedReader fromStdout;

        private OutputStream toUser;

        public ProcessStdOutputThread(InputStream fromProcessStdout,
            OutputStream userOutputStream) {
            this.toUser = userOutputStream;
            fromStdout = new BufferedReader(new InputStreamReader(
                fromProcessStdout));
        }

        public void run() {
            try {
                int oneChar;
                while ((oneChar = fromStdout.read()) != -1) {
                    // toUser.write(oneChar);
                    buffer.append(oneChar);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public String getCompleteOutput() {
            return toUser.toString();
        }
    }

    /**
     * test method to run an external program
     * 
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) {

        ByteArrayInputStream sbi = new ByteArrayInputStream("teststring"
            .getBytes());

        ExternalProcess ec = null;
        File x = new File(System.getProperty("user.dir"));
        try {
            ec = new ExternalProcess("less", null, null, sbi, x);
        } catch (Exception e) {
            System.err.println("Program not known.");
            e.printStackTrace();
        }
        // final InputStream stderr = new
        // BufferedInputStream(ec.getStreamFromStderr());
        // final InputStream stdout = new
        // BufferedInputStream(ec.getStreamFromStdout());
        //            
        //
        // // create thread to capture stdout from process.
        // new Thread(new Runnable() {
        // public void run() {
        // BufferedReader br = new BufferedReader(new
        // InputStreamReader(stdout));
        // String line = null;
        // try {
        // while ((line = br.readLine()) != null) {
        // System.out.println(line);
        // }
        // } catch (IOException e) {
        // e.printStackTrace();
        // }
        //
        // }
        // }).start();
        //
        // // create thread to capture stderr from process.
        // new Thread(new Runnable() {
        // public void run() {
        // BufferedReader br = new BufferedReader(new
        // InputStreamReader(stderr));
        // String line = null;
        // try {
        // while ((line = br.readLine()) != null) {
        // System.err.println(line);
        // }
        // } catch (IOException e) {
        // e.printStackTrace();
        // }
        //
        // }
        // }).start();
        int returnCode = 0;
        try {
            returnCode = ec.waitFor();
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        System.out.println("Program exited with code: " + returnCode);
        System.out.flush();

    }
}
