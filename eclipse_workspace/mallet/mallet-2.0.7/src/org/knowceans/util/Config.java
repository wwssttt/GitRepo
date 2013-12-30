/*
 * Copyright (c) 2010 Gregor Heinrich.  All rights reserved.
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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads configuration information from a properties file.
 * Instance-based conterpart of singleton Conf, but may be
 * used via Config.instance() after setting one instance
 * globally, which combines flexibility with clear
 * specification.
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
public class Config extends Properties {

/**
 * Comment for <code>serialVersionUID</code>
 */
private static final long serialVersionUID = 3256728368379344693L;
/**
 * Comment for <code>serialVersionUID</code>
 */
protected static Config theConfig;
protected static String propFile = null;
protected static String basePath = ".";
Pattern varPattern;

/**
 * get the instance of the singleton object
 * 
 * @return
 */
public static Config getConfig() {
    return theConfig;
}

/**
 * set the instance for the singleton object
 * 
 * @return
 */
public static void setConfig(Config config) {
    theConfig = config;
}

/**
 * Create properties file
 */
public Config(String file) {
    super();
    propFile = file;
    loadFile();
}

protected Config() {
}

/**
 * reload this configuration from file
 */
public void reload() {
    loadFile();
}

/**
 * load the properties file
 */
protected void loadFile() {
    try {
        load(new FileInputStream(propFile));
        varPattern = Pattern.compile("(\\@\\{(.+?)\\})+");
    } catch (FileNotFoundException e) {
        System.out.println("no properties file found: "
                + propFile);
        e.printStackTrace();
    } catch (IOException e) {
        e.printStackTrace();
    }
}

/**
 * Resolves all variables of the argument string using the
 * respective properties. The method works recursively, so
 * dependent variables are resolved.
 * 
 * @param p
 * @return
 */
protected synchronized String resolveVariables(String line) {
    StringBuffer sb = new StringBuffer();
    Matcher m = varPattern.matcher(line);
    while (m.find()) {
        String a = m.group(2);
        String x = get("@".concat(a));
        if (x != null)
            m = m.appendReplacement(sb, x);
    }
    sb = m.appendTail(sb);
    if (sb.toString() == "")
        return line;
    return sb.toString();

}

/**
 * get the named property from the singleton object
 * 
 * @return the value or null
 */
public String get(String key) {
    String p = getProperty(key);
    if (p != null) {
        p = resolveVariables(p).trim();
    }
    return p;
}

/**
 * Get the property and replace the braced values with the
 * array given.
 * 
 * @param key
 * @param braces
 * @return
 */
protected Object get(String key, String[] braces) {
    String a = get(key);
    MessageFormat mf = new MessageFormat(a);
    return mf.format(braces);
}

/**
 * get a numeric value.
 * 
 * @param key
 * @return
 */
public double getDouble(String key) {
    String a = get(key);
    return Double.parseDouble(a);
}

public float getFloat(String key) {
    return (float) getDouble(key);
}

/**
 * get a numeric value.
 * 
 * @param key
 * @return
 */
public long getLong(String key) {
    String a = get(key);
    return Long.parseLong(a);
}

/**
 * Get an integer value.
 * 
 * @param key
 * @return
 */
public int getInt(String key) {
    return (int) getLong(key);
}

/**
 * Get a double array from the file, where the vales are
 * separated by comma, semicolon or space.
 * 
 * @param key
 * @return
 */
public double[] getDoubleArray(String key) {
    String a = get(key);
    if (a == null || a.trim().equals("null"))
        return null;
    a = a.replaceAll(" +", " ").replaceAll(", ", ",");
    String[] ss = a.split("[;, ]");
    double[] ii = new double[ss.length];
    for (int i = 0; i < ii.length; i++) {
        ii[i] = Double.parseDouble(ss[i]);
    }
    return ii;
}

/**
 * Get an integer array from the file, where the vales are
 * separated by comma, semicolon or space.
 * 
 * @param key
 * @return
 */
public int[] getIntArray(String key) {
    String a = get(key);
    if (a == null || a.trim().equals("null"))
        return null;
    a = a.replaceAll(" +", " ").replaceAll(" *, *", ",");
    String[] ss = a.split("[;, ]");
    int[] ii = new int[ss.length];
    for (int i = 0; i < ii.length; i++) {
        ii[i] = Integer.parseInt(ss[i]);
    }
    return ii;
}

/**
 * get a boolean value: true and 1 are allowed for true,
 * anything else for false
 * 
 * @param key
 * @return
 */
public boolean getBoolean(String key) {
    String a = get(key);
    if (a.trim().equals("true") || a.trim().equals("1"))
        return true;
    return false;
}

/**
 * Get an instance of the class that corresponds to the
 * property name and has a default constructor. If no default
 * constructor exists, use getClass() instantiate in the
 * client code.
 * 
 * @param clazz
 * @return an instance of the class
 * @throws Exception
 */
public Object getInstance(String clazz) throws Exception {
    Class<?> cls = getClass(clazz);
    return cls.newInstance();
}

/**
 * Get class with the name specified by the property, using
 * the default class loader.
 * 
 * @param clazz
 * @return
 * @throws ClassNotFoundException
 */
public Class<?> getClass(String clazz)
        throws ClassNotFoundException {
    return Class.forName(get(clazz));
}

}
