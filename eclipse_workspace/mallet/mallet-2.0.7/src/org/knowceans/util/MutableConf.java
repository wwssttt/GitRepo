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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads configuration information from a properties file.
 * Implemented as singleton.
 * <p>
 * The file is looked up according to the following priorities
 * list:
 * <ul>
 * <li>[Explicitly named file using load()]
 * <li>[Content of system property conf.properties.file if
 * set] (e.g., runtime jvm option
 * -Dknowceans.properties.file=d
 * :\eclipse\workspace\indexer.properties)
 * <li>./[Main class name without package declaration and
 * .class suffix].properties
 * <li>./knowceans.conf, ./knowceans.properties
 * <li>c:/knowceans.conf, c:/knowceans.properties
 * </ul>
 * <p>
 * This version allows the definition of variables that can be
 * expanded at readtime:
 * 
 * <pre>
 * @{x1} in values will be expanded according to the respective property
 * @x1=val. The user MUST avoid circular references.
 * </pre>
 * 
 * @author heinrich
 */
public class MutableConf extends Conf {

Map<String, String> modifications;
Map<String, String> additions;

/**
 * Comment for <code>serialVersionUID</code>
 */
private static final long serialVersionUID = 3256728368379344693L;
/**
 * Comment for <code>serialVersionUID</code>
 */
StringBuffer content;

/**
 * get the instance of the singleton object
 * 
 * @return
 */
public static MutableConf get() {

    if (instance == null)
        instance = new MutableConf();

    return (MutableConf) instance;
}

public static void reload() {
    instance = new MutableConf();
}

/**
 * Instantiate the configuration, e.g., in the main class.
 * 
 * @param file
 * @return false if as the result of this call the
 *         configuration could NOT be loaded (can be used to
 *         find a conf file), false otherwise (if loading was
 *         successful or if class instance already exists)
 */
public static boolean load(String file) {
    if (instance == null) {
        if (new File(file).exists()) {
            instance = new MutableConf(file);
            return true;
        } else {
            return false;
        }
    }
    return true;
}

public MutableConf(String file) {
    super();
    propFile = file;
    loadFile();
}

public MutableConf() {
    super();
}

@Override
protected void loadFile() {
    try {
        load(new FileInputStream(propFile));
        varPattern = Pattern.compile("(\\@\\{([^\\}]+)\\})+");
        // again load the small file (as this is done only
        // once, we can accept this overhead instead of
        // fiddling with the large, well-tested supermethod)
        BufferedReader br = new BufferedReader(
                new FileReader(propFile));
        content = new StringBuffer();
        String line = null;
        while ((line = br.readLine()) != null) {
            content.append(line).append('\n');
        }
        br.close();
        modifications = new HashMap<String, String>();
        additions = new HashMap<String, String>();
    } catch (FileNotFoundException e) {
        System.out.println("no properties file found: "
                + propFile);
        e.printStackTrace();
    } catch (IOException e) {
        e.printStackTrace();
    }
}

/**
 * save the current properties file. This resolves all
 * variables with @.
 * 
 * @throws IOException
 */
public static void save() throws IOException {
    // we replace the key = value pairs in the original
    // file with their changed values, preserving all other
    // formatting
    MutableConf minst = ((MutableConf) instance);
    Pattern p = Pattern
        .compile("(.*?\n)?([^=\n]+ *)=( *)([^\n]+)\n");
    StringBuffer sb = new StringBuffer();
    Matcher m = p.matcher(minst.content);
    int end = 0;
    while (m.find()) {
        String before = m.group(1);
        if (before != null) {
            sb.append(before);
        }
        String key = m.group(2);
        sb.append(key).append('=').append(m.group(3));
        String value = m.group(4);
        String mod = minst.modifications.get(key.trim());
        if (mod != null) {
            sb.append(mod);
        } else {
            sb.append(value);
        }
        sb.append('\n');
        end = m.end(4);
    }
    sb.append(minst.content.substring(end + 1));
    // now put in the additions
    for (Map.Entry<String, String> e : minst.additions
        .entrySet()) {
        sb.append(e.getKey()).append(" = ").append(
            e.getValue()).append('\n');
    }
    System.out.println(sb);
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
 * get the named property from the singleton object
 * 
 * @return the value or null
 */
public static String get(String key) {
    String p = get().getProperty(key);
    if (p != null) {
        p = instance.resolveVariables(p).trim();
    }
    return p;
}

/**
 * set a given property
 * 
 * @param key
 * @param value
 */
public static void set(String key, String value) {
    String x = get(key);
    if (x != null) {
        ((MutableConf) instance).modifications
            .put(key, value);
    } else {
        ((MutableConf) instance).additions.put(key, value);
    }
    instance.put(key, value);
}

public static void set(String key, int value) {
    set(key, Integer.toString(value));
}

public static void set(String key, long value) {
    set(key, Long.toString(value));
}

public static void set(String key, double value) {
    set(key, Double.toString(value));
}

public static void set(String key, float value) {
    set(key, Float.toString(value));
}

public static void set(String key, boolean value) {
    set(key, Boolean.toString(value));
}

public static void set(String key, Object value) {
    set(key, value.toString());
}

public static void main(String[] args) {
    MutableConf.setPropFile("test.conf");
    System.out.println(MutableConf.get("prop.number.one"));
    System.out.println(instance);
    MutableConf.set("prop.number.two", "changedprop"
            + get("prop.number.one"));
    MutableConf.set("prop.number.four", "addedprop");
    System.out.println(instance);
    try {
        MutableConf.save();
    } catch (IOException e) {
        e.printStackTrace();
    }
}
}
