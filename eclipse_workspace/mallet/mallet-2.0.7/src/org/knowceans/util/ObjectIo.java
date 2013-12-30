/*
 * Created on 24.05.2006
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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * ObjectIo provides a simplified interface for object io to zip files. Open
 * streams using open*Stream(filename), then write to / read from the returned
 * Object*Stream and close the stream using close*Stream() or
 * Object*Stream.close(). The close*Stream() methods are provided for symmetry
 * with the open*Stream() methods, in order to improve code readability.
 * 
 * @author gregor
 */
public class ObjectIo {

    /*
     * this class can also be implemented as abstract non-static, calling an
     * abstract function from what is now open*Stream and closing it afterwards.
     */

    /**
     * Opens an object output stream with optional zip compression. The returned
     * DataOutputStream can be written to and must be closed using
     * closeStream(ObjectOutputStream dos) or dos.close().
     * 
     * @param filename
     * @return
     * @throws IOException
     * @throws FileNotFoundException
     */
    public static ObjectOutputStream openOutputStream(String filename)
        throws FileNotFoundException, IOException {
        ObjectOutputStream dos = null;

        if (filename.endsWith(".zip")) {
            ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(
                filename));
            String name = new File(filename).getName();
            zip.putNextEntry(new ZipEntry(name.substring(0, name.length() - 3)
                + "bin"));
            dos = new ObjectOutputStream(new BufferedOutputStream(zip));
        } else {
            dos = new ObjectOutputStream(new BufferedOutputStream(
                new FileOutputStream(filename)));
        }
        return dos;
    }

    /**
     * Close the data output, which results in flushing the write buffer and
     * closing the file.
     * 
     * @param dos
     * @throws IOException
     */
    public static void closeOutputStream(ObjectOutputStream dos)
        throws IOException {
        dos.close();
    }

    /**
     * Opens a data input stream with optional zip compression. The returned
     * ObjectInputStream can be read from and must be closed using
     * closeStream(ObjectOutputStream dos) or dos.close().
     * 
     * @param filename
     * @return
     * @throws IOException
     */
    public static ObjectInputStream openInputStream(String filename)
        throws IOException {
        ObjectInputStream dis = null;

        if (filename.endsWith(".zip")) {

            ZipFile f = new ZipFile(filename);
            String name = new File(filename).getName();
            dis = new ObjectInputStream(new BufferedInputStream(f
                .getInputStream(f.getEntry(name.substring(0, name.length() - 3)
                    + "bin"))));
        } else {
            dis = new ObjectInputStream(new BufferedInputStream(
                new FileInputStream(filename)));
        }
        return dis;
    }

    /**
     * Close the input stream
     * 
     * @param dis
     * @throws IOException
     */
    public static void closeInputStream(ObjectInputStream dis)
        throws IOException {
        dis.close();
    }

}
