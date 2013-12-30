/*
 * Copyright (c) 2002 Gregor Heinrich.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESSED
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */
package org.knowceans.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Adds write functionality to the Config class.
 * 
 * @author heinrich
 */
public class MutableConfig extends Config {

Map<String, String> modifications;
Map<String, String> additions;
/**
 * Comment for <code>serialVersionUID</code>
 */
StringBuffer content;

/**
 * Comment for <code>serialVersionUID</code>
 */
private static final long serialVersionUID = 3256728368379344693L;

/**
 * Load or create the properties file
 * 
 * @param file
 */
public MutableConfig(String file) {
    super();
    try {
        propFile = file;
        content = new StringBuffer();
        modifications = new HashMap<String, String>();
        additions = new LinkedHashMap<String, String>();
        File f = new File(file);
        if (f.exists()) {
            loadFile();
        } else {
            f.createNewFile();
        }
    } catch (IOException e) {
        e.printStackTrace();
    }
}

/**
 * reload this configuration from file, discarding any changes
 */
public void reload() {
    content.setLength(0);
    modifications.clear();
    additions.clear();
    loadFile();
}

/**
 * this method tries to load the config file. If it is not
 * found, an empty file is assumed that can be appended
 * properties using the set methods.
 */
@Override
protected void loadFile() {
    super.loadFile();
    try {
        // again load the small file (as this is done only
        // once, we can accept this overhead instead of
        // fiddling with the large, well-tested
        // supermethod)
        BufferedReader br = new BufferedReader(
                new FileReader(propFile));
        String line = null;
        while ((line = br.readLine()) != null) {
            content.append(line).append('\n');
        }
        br.close();
    } catch (FileNotFoundException e) {
        e.printStackTrace();
    } catch (IOException e) {
        e.printStackTrace();
    }
}

/**
 * add the configuration object to this one
 * 
 * @param conf
 */
public void set(Config conf) {
    for (Map.Entry<Object, Object> e : conf.entrySet()) {
        Object val = e.getKey();
        if (val == null || !val.equals(e.getValue())) {
            set((String) e.getKey(), (String) e.getValue());
        }
    }
}

/**
 * save the current properties file. This resolves all
 * variables with @.
 * 
 * @throws IOException
 */
public void save() throws IOException {
    // we replace the key = value pairs in the original
    // file with their changed values, preserving all other
    // formatting
    Pattern p = Pattern
        .compile("(\\s*(?:#.*?\n)*\\s*)?([^=\n]+ *)=( *)([^\n]+)\n");
    StringBuffer sb = new StringBuffer();
    Matcher m = p.matcher(content);
    int end = 0;
    while (m.find()) {
        String before = m.group(1);
        if (before != null) {
            sb.append(before);
        }
        String key = m.group(2);
        sb.append(key).append('=').append(m.group(3));
        String value = m.group(4);
        String mod = modifications.get(key.trim());
        if (mod != null) {
            sb.append(mod);
        } else {
            sb.append(value);
        }
        sb.append('\n');
        end = m.end(4);
    }
    if (content.length() > 0) {
        sb.append(content.substring(end + 1));
    }
    // now put in the additions
    for (Map.Entry<String, String> e : additions.entrySet()) {
        sb.append(e.getKey()).append(" = ").append(
            e.getValue()).append('\n');
    }
    File f = new File(propFile);
    SimpleDateFormat a = new SimpleDateFormat(
            "-yyMMdd-HHmmss");
    f.renameTo(new File(propFile + a.format(new Date())));
    BufferedWriter bw = new BufferedWriter(new FileWriter(
            propFile));
    bw.append(sb);
    bw.close();
}

/**
 * set a given property
 * 
 * @param key
 * @param value
 */
public void set(String key, String value) {
    String x = get(key);
    if (x != null) {
        if (!x.equals(value)) {
            modifications.put(key, value);
            put(key, value);
        }
    } else {
        additions.put(key, value);
        put(key, value);
    }
}

public void set(String key, int value) {
    set(key, Integer.toString(value));
}

public void set(String key, long value) {
    set(key, Long.toString(value));
}

public void set(String key, double value) {
    set(key, Double.toString(value));
}

public void set(String key, float value) {
    set(key, Float.toString(value));
}

public void set(String key, boolean value) {
    set(key, Boolean.toString(value));
}

public void set(String key, Object value) {
    set(key, value.toString());
}

public static void main(String[] args) {
    MutableConfig conf = new MutableConfig("test.conf");
    System.out.println(conf.get("prop.number.one"));
    System.out.println(conf);
    conf.set("prop.number.two", "changedprop"
            + conf.get("prop.number.one"));
    conf.set("prop.number.four", "addedprop");
    System.out.println(conf);
    try {
        conf.save();
    } catch (IOException e) {
        e.printStackTrace();
    }

    MutableConfig conf2 = new MutableConfig("newtest.conf");
    conf2.set(conf);
    conf2.set("first.entry", "guess what");
    try {
        conf2.save();
    } catch (IOException e) {
        e.printStackTrace();
    }
}
}
