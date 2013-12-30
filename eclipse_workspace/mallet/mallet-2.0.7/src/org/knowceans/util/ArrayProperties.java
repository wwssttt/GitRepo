package org.knowceans.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * loads properties into a List of HashMaps according to the
 * separators ==+ in the file. Does not allow empty values
 * without =.
 * 
 * @author gregor
 * 
 */
public class ArrayProperties implements
        Iterable<Map<String, String>> {

public static void main(String[] args) throws IOException {
    ArrayProperties ap = new ArrayProperties(
            "lucene-restlet-targets.conf");
    System.out.println(ap.getProperties());
}

/**
 * properties list
 */
List<Map<String, String>> properties;

/**
 * load array properties file
 * 
 * @param file
 * @throws IOException
 */
public ArrayProperties(String file) throws IOException {
    loadFile(file);
}

/**
 * worker method to load file
 * 
 * @param file
 * @throws IOException
 */
private void loadFile(String file) throws IOException {
    BufferedReader br = new BufferedReader(new FileReader(
            file));
    StringBuffer buf = new StringBuffer();
    String line = null;
    properties = new ArrayList<Map<String, String>>();
    while ((line = br.readLine()) != null) {
        // ignore surrounding whitespace
        line = line.trim();
        // ignore comments
        if (line.startsWith("#"))
            continue;
        buf.append(line).append('\n');
    }
    br.close();
    // elements are separated by a line starting with ==
    String[] elements = buf.toString().split("\n==[^\n]*");
    for (String block : elements) {
        properties.add(parse(block));
    }
}

/**
 * parse properties block
 * 
 * @param block
 * @return
 */
private Map<String, String> parse(String block) {
    Map<String, String> propmap = new HashMap<String, String>();
    block = block.trim();
    String[] props = block.split("\n");
    Pattern p = Pattern.compile("([^=]+)\\s*=(.+)");
    for (String prop : props) {
        Matcher m = p.matcher(prop);
        if (m.matches()) {
            propmap.put(m.group(1).trim(), m.group(2).trim());
        }
    }
    return propmap;
}

/**
 * retrieve properties loaded
 * 
 * @return
 */
public List<Map<String, String>> getProperties() {
    return properties;
}

/**
 * length of the list
 * 
 * @return
 */
public int size() {
    return properties.size();
}

/**
 * get the property block at index
 * 
 * @param index
 * @return
 */
public Map<String, String> get(int index) {
    return properties.get(index);
}

/**
 * get the property at index and key
 * 
 * @param index
 * @param key
 * @return
 */
public String get(int index, String key) {
    return properties.get(index).get(key);
}

/**
 * get the property at index and key
 * 
 * @param index
 * @param key
 * @return
 */
public int getInt(int index, String key) {
    return Integer.parseInt(properties.get(index).get(key));
}

/**
 * get the property at index and key
 * 
 * @param index
 * @param key
 * @return
 */
public double getDouble(int index, String key) {
    return Double.parseDouble(properties.get(index).get(key));
}

/**
 * get the property at index and key
 * 
 * @param index
 * @param key
 * @return
 */
public long getLong(int index, String key) {
    return Long.parseLong(properties.get(index).get(key));
}

/**
 * get the property at index and key
 * 
 * @param index
 * @param key
 * @return
 */
public float getFloat(int index, String key) {
    return Float.parseFloat(properties.get(index).get(key));
}

/**
 * get the property at index and key
 * 
 * @param index
 * @param key
 * @return
 */
public boolean getBoolean(int index, String key) {
    return Boolean.parseBoolean(properties.get(index)
        .get(key));
}

/**
 * make available to foreach statement
 */
public Iterator<Map<String, String>> iterator() {
    return properties.iterator();
}

}
