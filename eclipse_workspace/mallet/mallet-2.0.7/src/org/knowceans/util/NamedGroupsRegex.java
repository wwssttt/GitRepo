/*
 * Copyright (c) 2005-2007 Gregor Heinrich. All rights reserved. Redistribution and
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

/*
 * Created on 03.04.2007
 */
package org.knowceans.util;

import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * NamedGroupRegex allows to name groups in regular expressions, similar to
 * python, but using simpler syntax:
 *
 * <pre>
 * ({name}groupstring)
 * </pre>
 *
 * If an additional qualifier to the group is needed, such as multiline case
 * insensitive <text>(?si:groupstring)</text>, this is inserted <i>after</i>
 * the name brace, i.e., <text>({name}?si:groupstring)</text>.
 * <p>
 * The substitution syntax is:
 *
 * <pre>
 * substitionst${name}ring
 * </pre>
 *
 * The following syntax can be used for back references:
 *
 * <pre>
 * ({name}groupstring) ${name}
 * </pre>
 *
 * (Because of the string in braces, the $ can be disambiguated from the
 * end-of-line symbol.)
 * <p>
 * Usage: Before a pattern with named groups is used, a NamedGroupsDecoder
 * object is created for the pattern using <text>new NamedGroupsDecoder(String
 * pattern)</text>. This preprocesses the regex into a java-compliant string
 * that can be used with the Pattern class, which is accessed using <text>String
 * getJavaPattern()</text>, and an internal mapping of group names to group
 * numbers. Groups then can be accessed via the method <text>int
 * getGroup(String)</text>, or directly in the Matcher, <text>
 * m.group(ng.getNamedGroup(String))</text>. When substitutions are needed, the
 * second constructor can be used: <text>NamedGroupsDecoder(String pattern,
 * String substitution)</text>, which also creates a substitution string with
 * java-compliant backrefs. This is accessed using <text>String getJavaSub()</text>
 * and can be directly used in the methods <text>Matcher.replaceFirst/All(String
 * replacement)</text>.
 *
 * @author gregor
 */
public class NamedGroupsRegex {

    /**
     * named group string
     */
    // neg lookbehind for escapes, identify non-capturing groups (group 1)
    // and the content of any named groups (group 2)
    private static Pattern ng = Pattern
        .compile("(?<!\\\\)\\((\\?)?(?:\\{(\\w+)\\})?");

    /**
     * named group substitution string
     */
    // named group substitution
    private static Pattern ngs = Pattern.compile("(?<!\\\\)\\(\\{\\w+\\}");

    /**
     * named backref
     */
    // neg lookbehind for escapes, positive lookbehind and lookahead for braces
    private static Pattern nb = Pattern.compile("(?<!\\\\)\\$\\{(\\w+)\\}");

    /**
     * name-to-number group matching
     */
    private Hashtable<String, Integer> name2group;

    /**
     * java-compliant substitution pattern
     */
    private String javaSub;

    /**
     * java pattern
     */
    private Pattern javaPattern;

    public static void main(String[] args) {
        String s = "teststring with some groups.";
        // define some named groups
        String p = "({tt}tes.).+?({uu}so..)";
        String q = "tt=${tt}, uu=${uu}";

        NamedGroupsRegex named = new NamedGroupsRegex(p, q);
        // named replacement
        System.out.println(s);
        s = s.replaceAll(named.getJavaPatternString(), named.getJavaSub());
        System.out.println(s);

        s = "how wow pow sow now row vow tow cow mow cow mow";
        System.out.println("s = " + s);
        // p = "({repeat}\\w.)..${repeat}";
        System.out.println();
        System.out.println();

        p = "({test}\\w+)..${test}";

        named = new NamedGroupsRegex(p);
        Matcher m = named.getMatcher(s);
        System.out.println(named.getJavaPattern());
        while (m.find()) {
            System.out.println(m.group(named.getGroup("test")));
        }

    }

    /**
     * Creates a decoder for a regex string
     *
     * @param pattern
     */
    public NamedGroupsRegex(String pattern) {
        String p = findNamedGroups(pattern);
        this.javaPattern = Pattern.compile(p);
    }

    /**
     * Creates a decoder for regex and replacement string
     *
     * @param pattern
     * @param replacement
     */
    public NamedGroupsRegex(String pattern, String replacement) {
        this(pattern);
        javaSub = replaceNamedBackrefs(replacement, true);
    }

    /**
     * Gets the complile pattern for the regex.
     *
     * @return
     */
    public Pattern getJavaPattern() {
        return javaPattern;
    }


    /**
     * Creates a matcher for the regex.
     *
     * @param seq
     * @return
     */
    public Matcher getMatcher(CharSequence seq) {
        return javaPattern.matcher(seq);
    }

    /**
     * fills the table of named groups and creates the java-compliant regex
     * string.
     *
     * @param pattern
     */
    private String findNamedGroups(String pattern) {

        name2group = new Hashtable<String, Integer>();

        Matcher m = ng.matcher(pattern);
        int groupno = 0;
        while (m.find()) {
            // group 1 is the ? for non-capturing groups
            if (m.group(1) == null) {
                // capturing group detected
                groupno++;
                // is it a named group?
                if (m.group(2) != null) {
                    name2group.put(m.group(2), groupno);
                }
            }
        }
        // make anonymous group from named group
        m = ngs.matcher(pattern);
        String patstring = m.replaceAll("\\(");
        patstring = replaceNamedBackrefs(patstring, false);
        return patstring;
    }

    /**
     * creates the java-compliant replacement string
     *
     * @param string
     * @param insubstitution true if in the substitution string, false if in the
     *        regex
     */
    private String replaceNamedBackrefs(String string, boolean insubstitution) {

        Matcher m = nb.matcher(string);
        StringBuffer b = new StringBuffer();
        while (m.find()) {
            String name = m.group(1);
            StringBuffer c = new StringBuffer();
            if (insubstitution) {
                c.append("\\$");
            } else {
                c.append("\\\\");
            }
            c.append(name2group.get(name));
            m.appendReplacement(b, c.toString());
        }
        m.appendTail(b);
        return b.toString();
    }

    /**
     * get java-compliant regex string
     *
     * @return
     */
    public final String getJavaPatternString() {
        return javaPattern.toString();
    }

    /**
     * get java-compliant substitution / replacement string
     *
     * @return
     */
    public final String getJavaSub() {
        return javaSub;
    }

    public final int getGroup(String name) {
        return name2group.get(name);
    }

}
