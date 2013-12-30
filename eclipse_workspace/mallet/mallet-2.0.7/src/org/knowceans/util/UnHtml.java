/*
 * Created on 05.04.2006
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

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.net.URLConnection;

import javax.swing.text.BadLocationException;
import javax.swing.text.EditorKit;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

/**
 * UnHtml converts html to text, html comes either from a reader, a string or an
 * url.
 * 
 * @author gregor
 */
public class UnHtml {

    public static String getText(String html) {
        return getText(new StringReader(html));
    }

    public static String getText(URL url) {
        try {
            URLConnection conn = url.openConnection();
            return getText(new InputStreamReader(conn.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;

    }

    @SuppressWarnings("serial")
    public static String getText(Reader rd) {
        final StringBuffer buf = new StringBuffer(1024);
        try {
            HTMLDocument doc = new HTMLDocument() {
                public HTMLEditorKit.ParserCallback getReader(int pos) {
                    return new HTMLEditorKit.ParserCallback() {
                        public void handleText(char[] data, int pos) {
                            buf.append(data);
                            buf.append('\n');
                        }
                    };
                }
            };

            // Parse the HTML
            EditorKit kit = new HTMLEditorKit();
            kit.read(rd, doc, 0);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (BadLocationException e) {
            e.printStackTrace();
        }

        // Return the text
        return buf.toString();
    }
}
