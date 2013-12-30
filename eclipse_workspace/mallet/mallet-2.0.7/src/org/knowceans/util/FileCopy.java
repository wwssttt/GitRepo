/*
 * Created on 18.07.2006
 */
package org.knowceans.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileCopy {

    public static void copy(String from, String to) throws IOException {

        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(from));
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(to));

        byte[] buffer = new byte[4096];
        int len;
        while((len = bis.read(buffer)) > 0) {
            bos.write(buffer, 0, len);
        }
        bos.close();
        bis.close();
    }
}
