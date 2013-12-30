/*
 * Copyright (c) 2005-2006 Gregor Heinrich. All rights reserved. Redistribution and
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
import java.util.Stack;
import java.util.Vector;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PatternString is a wrapper around pattern matching and substitution
 * functionality inspired by the Perl <code>a =~ exp</code> functions.
 * <p>
 * This implementation puts less emphasis on performance (by putting high
 * priority to reusing Matchers), but is intended for easy use, as most time
 * usually is used for development / porting rather than for operation... The
 * idea is that String-like data can be used for matching operations much more
 * convenient as PatternString objects than by using the final classes String,
 * StringBuffer, Matcher in the Java API.
 * <p>
 * Further, the methods are organised a bit different from the standard Java
 * API. Basically, there are three operations, find(), match() and substitute().
 * Unlike their API counterpart find(), which returns a boolean, find() returns
 * this (a substring copy with an empty matcher), in order to allow
 * concatenations. In a subsequent loop, the function found() returns the status
 * of the last find() and operation, and findNext() can be called to advance the
 * parser.
 * <p>
 * The API functions replaceFirst() and replaceAll() usually return new
 * instances of the String (as it is immutable). The corresponding substitute(),
 * however, returns <code>this</code> and resets the internal matcher if it
 * was global. For the non-global version, the current state of the matcher,
 * i.e. its current region is used. Therefore, it is possible to run through a
 * string using find() or findNext() (findNext() can only be called after
 * find()) and substitute() [TODO: this hot-needle code must be thoroughly
 * tested!].
 * <p>
 * TODO: make pattern string with a constant (and pre-compilable pattern). TODO:
 * fix problems with cascading substitution.
 *
 * @author gregor heinrich arbylon.net
 */
public class PatternString implements CharSequence, MatchResult {

    public static void main(String[] args) {

        String a = "ein langer string mit mit yyy";
        PatternString p = PatternString.create(a);
        p.debug = true;
        p.nperl("/lang.../");
        PatternString q = p.findNext();
        if (p.found()) {
            System.out.println(p.start());
            System.out.println(q);
        }
        p.nperl("s/la//g");
        System.out.println(p);
        p.perl("s/([gin])/X$1/g");
        System.out.println(p);
        System.out.println(p);
        p.perl("/yyy/");
        if (p.found()) {
            System.out.println(p.group());
        }
        p.nperl("s/(\\w+) \\1/$1/");
        if (p.found()) {
            System.out.println(p);
        }

        PatternString s = new PatternString("abc dlcid c bC ab c");

        Vector<PatternString> b = s.findAll("b ?c", "i");
        System.out.println(b);

        s = new PatternString("abc dlcid c bC ab c");

        Vector<PatternString> v = s.findAll("(b ?c)", "i$1$1", "i");
        System.out.println(v);

    }

    public boolean debug = false;

    /**
     * Create a PatternString from the input, with an empty matcher.
     *
     * @param text
     */
    public PatternString(String text) {
        this.text = new StringBuffer(text);
    }

    /**
     * Create a PatternString from the input, with an empty matcher.
     *
     * @param text
     */
    public PatternString(StringBuffer text) {
        this.text = text;
    }

    /**
     * Create a empty PatternString. The object text can be changed using
     * setB().
     *
     * @param s
     */
    public PatternString() {
        text = new StringBuffer();
    }

    /**
     * copies the pattern string content with an new matcher set to the region
     * of the current one but the matchresult my set, ie., all references to
     * groups information of the last match are kept.
     *
     * @return
     */
    public PatternString copy() {
        PatternString pp = new PatternString(text);
        pp.mr = mr;
        reset();
        m.region(m.regionStart(), m.regionEnd());
        return pp;
    }

    /**
     * convenience method to get a pattern string.
     *
     * @param s
     * @return
     */
    public static PatternString create(String s) {
        return new PatternString(s);
    }

    private StringBuffer text;

    private Matcher m;

    private MatchResult mr;

    private int flags;

    private boolean found;

    /**
     * Like perl, but resets the parser before.
     *
     * @param patternCommand
     * @return
     */
    public boolean nperl(String patternCommand) {
        reset();
        return perl(patternCommand);
    }

    /**
     * Perform the command in a perl specification on this and return this,
     * e.g., <code>this =~ s/exp/subs/flags</code> will call substitute(exp,
     * subs, flags).
     * <p>
     * FIXME: with cascaded substitution, the string of the matcher is always
     * reset to the first value (the field text diverges from the internal state
     * of the matcher).
     *
     * @param patternCommand -- everything that appears right of a
     *        <code>=~</code> in Perl, i.e., the expression includes commands
     *        and delimiters. Examples: <code>/abc/</code> for finding,
     *        <code>s/x(\d+)/u$1/gi</code> for substituting all x34 or X34
     *        etc. with u34 etc.
     * @return whether the pattern could be matched (and with substitution,
     *         whether the new string is different from the old one)
     */
    public boolean perl(String patternCommand) {

        PatternString s = new PatternString(patternCommand);

        // parse patter and escap the / char
        s.match("([sm])?" + "/" + "((?:[\\\\][/]|[^/])+)" + "/" + "(?:"
            + "((?:[\\\\][/]|[^/])*)" + "/" + ")?" + "([\\w]*)", "");
        // TODO: allow flexible / char, e.g., #. The test below does not
        // work, and [\2] is not allowed as a rule
        // s.find("([sm])?" + "([\\W])" + "((?:\\\\\\2|[^/])+)" + "\\2" + "(?:"
        // + "((?:\\\\\\2|[^/])*)" + "\\2" + ")?" + "([\\w]+)", "");
        if (!s.found()) {
            throw new IllegalArgumentException("wrong Perl pattern string");
        }
        int flags = translatePerlFlags(s.group(4));

        // System.out.println(s.debugString());

        if (s.group(1) == null) {
            find(s.group(2), flags);
            return s.found();
        } else if (s.group(1).equals("m")) {
            return match(s.group(2), flags);
        } else if (s.group(1).equals("s")) {
            if (s.group(3) == null)
                throw new IllegalArgumentException(
                    "wrong Perl substitution pattern string");
            StringBuffer temp = text;
            if (s.group(4) != null && s.group(4).contains("g")) {
                substitute(s.group(2), s.group(3), flags, true).toString()
                    .equals(temp);
            } else {
                substitute(s.group(2), s.group(3), flags, false).toString()
                    .equals(temp);
            }
            return (temp != text);
        }
        return false;
    }

    /**
     * emulates a perl find expression like:
     * <code> this =~ /expression/perlFlags</code>
     *
     * @param expression
     * @param perlFlags (see putPerlFlags)
     * @return
     */
    public PatternString find(String expression, String perlFlags) {

        if (perlFlags != null) {
            int flags = translatePerlFlags(perlFlags);
            return find(expression, flags);
        }
        return find(expression, 0);
    }

    /**
     * emulates a perl find expression like:
     * <code> this =~ /expression/perlFlags</code>
     *
     * @param expression
     * @param perlFlags {@see Pattern.compile}
     * @return
     */
    public PatternString find(String expression, int flags) {
        // don't compile pattern etc if expression is the same.
        if (matcherUptodate(expression, flags)) {
            return findNext();
        }
        Pattern p = Pattern.compile(expression, flags);
        configureMatcher(p);
        return findNext();
    }

    /**
     * Emulates a perl find expression like: <code> this =~ /expression/ </code>
     *
     * @param expression
     * @return
     */
    public PatternString find(String expression) {
        return find(expression, 0);
    }

    /**
     * Emulates a repeated perl find like:
     * <code> foreach this =~ /expression/ \@a += \@_</code>.
     * <p>
     * Returns the array of strings found. After this, found will be false
     * because the search is exhaustive and the matcher of this pattern string
     * is positioned at the end of the last match. Can be used to use find in a
     * Java foreach construct. Use reset() to start at the beginning.
     *
     * @param expression
     * @return
     */
    public Vector<PatternString> findAll(String expression) {
        return findAll(expression, "");
    }

    /**
     * Emulates a repeated perl find like:
     * <code> foreach this =~ /expression/ @@a += @@_</code>. Returns the array of strings found. After this, found will
     *     be false because the search is exhaustive and the matcher of this
     *     pattern string is positioned at the end of the last match. Can be
     *     used to use find in a Java foreach construct. Use reset() to start at
     *     the beginning.
     * @param expression
     * @return
     */
    public Vector<PatternString> findAll(String expression, String perlFlags) {
        Vector<PatternString> r = new Vector<PatternString>();
        find(expression, perlFlags);
        while (found()) {
            r.add(new PatternString(group()));
            findNext();
        }
        return r;
    }

    /**
     * Finds all occurrences of the expression and substitutes them with the
     * replacement. Does not change the internal string but resets the internal
     * matcher and those of the generated pattern strings.
     *
     * @param expression
     * @return
     */
    public Vector<PatternString> findAll(String expression, String replacement,
        String perlFlags) {
        Vector<PatternString> r = new Vector<PatternString>();

        find(expression, perlFlags);
        while (found()) {
            PatternString ps = new PatternString(group());
            // TODO: this is duplicate matching work since we know the
            // expression matches
            ps.substitute(expression, replacement, perlFlags);
            ps.reset();
            r.add(ps);
            findNext();
        }
        reset();
        return r;
    }

    /**
     * Finds the next occurrence of the pattern and returns it (i.e., return
     * group(0)). The success of this find() operation can be checked with
     * found(), and the actual groups can be checked with group(...).
     *
     * @return
     */
    public PatternString findNext() {
        found = false;
        if (m.find()) {
            this.found = true;
            mr = m.toMatchResult();
            if (debug)
                System.out.println("found '" + m.group() + "' =~ /"
                    + m.pattern() + "/");
            return new PatternString(m.group());
        }
        if (debug)
            System.out.println("pattern not found /" + m.pattern() + "/");
        mr = m.toMatchResult();
        return null;
    }

    /**
     * Returns true whether the last find or matching operation has been
     * successful, i.e., the pattern has been found.
     *
     * @return
     */
    public boolean found() {
        return found;
    }

    /**
     * Emulates a perl matching expression like:
     * <code> this =~ m/expression/perlFlags</code>
     *
     * @param expression
     * @param perlFlags (see putPerlFlags)
     * @return the matched string
     */
    public boolean match(String expression, String perlFlags) {
        int flags = translatePerlFlags(perlFlags);
        return match(expression, flags);
    }

    /**
     * emulates a perl match expression like:
     * <code> this =~ /expression/perlFlags</code>
     *
     * @param expression
     * @param perlFlags {@see Pattern.compile}
     * @return
     */
    public boolean match(String expression, int flags) {
        found = false;
        Pattern p = Pattern.compile(expression, flags);
        configureMatcher(p);
        found = m.matches();
        mr = m.toMatchResult();
        if (debug)
            if (found) {
                System.out.println("matched '" + m.group() + "' =~ m/"
                    + m.pattern() + "/");
            } else {
                System.out
                    .println("pattern not matched m/" + m.pattern() + "/");

            }
        return matched();
    }

    /**
     * Same as found().
     *
     * @return
     */
    public boolean matched() {
        return found;
    }

    /**
     * emulates a perl substitution expression like:
     * <code> this =~ s/expression/replacement/perlFlags</code>
     *
     * @param expression
     * @param replacement
     * @param perlFlags (see putPerlFlags)
     * @return this
     */
    public PatternString substitute(String expression, String replacement,
        String perlFlags) {

        int flags = translatePerlFlags(perlFlags);

        boolean replaceRemaining = false;
        if (perlFlags.contains("g"))
            replaceRemaining = true;

        return substitute(expression, replacement, flags, replaceRemaining);
    }

    /**
     * emulates a perl substitution expression like:
     * <code> this =~ s/expression/replacement/perlFlags</code>
     *
     * @param expression
     * @param replacement
     * @param perlFlags (see Pattern)
     * @param replaceRemaining whether to substitute all remaining occurrences
     *        or only the next one (prior reset() if you want to replace all.)
     * @return this
     */
    public PatternString substitute(String expression, String replacement,
        int flags, boolean replaceRemaining) {
        Pattern p = Pattern.compile(expression, flags);
        configureMatcher(p);

        // TODO: inline replacement possible with string buffer?
        if (replaceRemaining) {
            text = new PatternString(replaceRemaining(replacement)).text;
        } else {
            text = new PatternString(replaceNext(replacement)).text;
        }
        return this; // new PatternString(text);
    }

    /**
     * Replaces the next occurrence of the pattern.
     *
     * @param replacement
     * @return
     */
    private String replaceNext(String replacement) {
        found = false;
        StringBuffer sb = new StringBuffer();
        if (m.find()) {
            m.appendReplacement(sb, replacement);
            found = true;
            if (debug)
                System.out.println("replace from pos " + m.regionStart()
                    + " with /" + replacement + "/, found '" + m.group()
                    + "' =~ /" + m.pattern() + "/");
        } else {
            if (debug)
                System.out.println("replacement pattern from pos "
                    + m.regionStart() + " with /" + replacement
                    + "/, not found /" + m.pattern() + "/");
        }
        // new matcher position should be after last replacement:
        int regstart = sb.length();
        m.appendTail(sb);
        synchroniseMatcher(sb.toString(), regstart, sb.length());

        return sb.toString();
    }

    /**
     * Replaces all occurrences of the pattern from the current state of the
     * matcher. If replaceAll should be used, call reset() prior to
     * replaceRemaining()
     *
     * @param replacement
     * @return true if at least one replacement has been done.
     */
    private String replaceRemaining(String replacement) {
        found = false;
        boolean result = m.find();
        found = result;
        if (result) {
            StringBuffer sb = new StringBuffer();
            int i = 0;
            do {
                if (debug)
                    i++;
                m.appendReplacement(sb, replacement);
                result = m.find();
            } while (result);
            if (debug)
                System.out.println("replace from pos " + m.regionStart()
                    + " with /" + replacement + "/, " + i + "x found /"
                    + m.pattern() + "/");
            int regstart = sb.length();
            m.appendTail(sb);
            synchroniseMatcher(sb.toString(), regstart, sb.length());
            return sb.toString();
        } else {
            if (debug)
                System.out.println("replacement pattern all from pos "
                    + m.regionStart() + " with /" + replacement
                    + "/, not found /" + m.pattern() + "/");
        }
        return text.toString();
    }

    /**
     * save the current matcher state and reset it to new input pattern
     *
     * @param text
     * @param regstart
     * @param regend
     */
    private void synchroniseMatcher(String text, int regstart, int regend) {
        if (debug)
            System.out.println("move matcher region from [" + m.regionStart()
                + ", " + m.regionEnd() + "] to [" + regstart + ", " + regend
                + "]");
        mr = m.toMatchResult();
        m.reset(text);
        m.region(regstart, regend);
    }

    /**
     * emulates a perl substitution expression like:
     * <code> this =~ s/expression/replacement/</code>
     *
     * @param expression
     * @param replacement
     * @return this
     */
    public PatternString substitute(String expression, String replacement) {
        return substitute(expression, replacement, 0, false);
    }

    /**
     * performs global replace of the string expression with the replacement.
     * After the operation, the internal string buffer is filled with the
     * substitute (and the original string lost).
     *
     * @param expression
     * @param replacement
     * @return this
     */
    public PatternString substituteAll(String expression, String replacement) {
        return substitute(expression, replacement, 0, true);
    }

    /**
     * resets the matcher in order to allow new parsing.
     */
    public void reset() {
        if (debug)
            System.out.println("reset matcher with text '" + text + "'");
        if (m != null) {
            m.reset(text);
            mr = m.toMatchResult();
        }
    }

    /**
     * Returns whether the matcher is up to date or must be set with new
     * parameters. This allows to call find(exp, flags) etc. with parameters in
     * a while loop and avoid to have to call find() after the first match
     * separately.
     * <p>
     * Note: The expression is checked by reference, i.e., a new instance of the
     * variable expression yields a restarting loop! TODO: check if this makes
     * sense in practice, e.g., with on the fly string concatenations. If not,
     * change == to equals.
     *
     * @param expression
     * @param flags
     * @return
     */
    public boolean matcherUptodate(String expression, int flags) {
        if (m == null)
            return false;
        // cheaper: reference identity
        if (expression != m.pattern().pattern())
            return false;
        if (flags != this.flags)
            return false;
        return true;
    }

    /**
     * sets the matcher with the new pattern
     *
     * @param p
     */
    public void configureMatcher(Pattern p) {
        if (m == null) {
            if (debug)
                System.out.println("create matcher with pattern /" + p + "/");
            m = p.matcher(text.toString());
        } else {
            if (debug)
                System.out.println("set pattern /" + p + "/");
            m.usePattern(p);
        }
    }

    public Matcher getMatcher() {
        return m;
    }

    public Pattern getPattern() {
        return m.pattern();
    }

    public int length() {
        return text.length();
    }

    public char charAt(int index) {
        return 0;
    }

    public CharSequence subSequence(int start, int end) {
        return text.substring(start, end);
    }

    /**
     * add optional flags
     * <ul>
     * <li>g - global, otherwise only first occurrence. In order to really
     * replace all and not only the remaining occurrences, call reset().
     * <li>i - case-insensitive
     * <li>m - multiline (i.e., match ^$ at newlines)
     * <li>s - singleline (i.e., . matches \n)
     * </ul>
     *
     * @param perlFlags
     * @return the corresponding the Pattern flags value.
     */
    public int translatePerlFlags(String perlFlags) {
        int flags = 0;
        if (perlFlags == null) {
            return flags;
        }
        if (perlFlags.contains("m")) {
            flags |= Pattern.MULTILINE;
        }
        if (perlFlags.contains("i")) {
            flags |= Pattern.CASE_INSENSITIVE;
        }
        if (perlFlags.contains("s")) {
            flags |= Pattern.DOTALL;
        }
        return flags;
    }

    /**
     * return the group with the number after the last match
     *
     * @param number
     * @return
     */
    public String group(int number) {
        return mr.group(number);
    }

    /**
     * return the group with the number after the last match
     *
     * @param number
     * @return
     */
    public PatternString groupP(int number) {
        return new PatternString(mr.group(number));
    }

    /**
     * return the start of the internal matcher's region
     *
     * @return
     */
    public int regionStart() {
        return m.regionStart();
    }

    /**
     * return the end of the internal matcher's region
     *
     * @return
     */
    public int regionEnd() {
        return m.regionEnd();
    }

    /**
     * sets the region for this pattern string
     *
     * @param start
     * @param end
     */
    public void region(int start, int end) {
        if (debug)
            System.out.println("set region to [" + start + ", " + end + "]");
        m.region(start, end);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.regex.MatchResult#start()
     */
    public int start() {
        return mr.start();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.regex.MatchResult#end()
     */
    public int end() {
        return mr.end();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.regex.MatchResult#start(int)
     */
    public int start(int group) {
        return mr.start(group);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.regex.MatchResult#end(int)
     */
    public int end(int group) {
        return mr.end(group);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.regex.MatchResult#group()
     */
    public String group() {
        return mr.group();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.regex.MatchResult#groupCount()
     */
    public int groupCount() {
        return mr.groupCount();
    }

    /**
     * return the variable with the Perl name perlVar, e.g., <code>$_</code>
     * for last match.
     * <ul>
     * <li>$_ holds the full match.
     * <li>$1, $2, $3, etc. hold the backreferences.
     * <li>$+ holds the last (highest-numbered) backreference.
     * <li>$& (dollar ampersand) holds the entire regex match.
     * <li>$' (dollar followed by an apostrophe or single quote) holds the part
     * of the string after (to the right of) the regex match.
     * <li>$` (dollar backtick) holds the part of the string before (to the
     * left of) the regex match.
     * </ul>
     *
     * @param perlVar
     * @return
     */
    public PatternString variablePattern(String perlVar) {

        return new PatternString(variable(perlVar));
    }

    /**
     * return the variable with the Perl name perlVar, e.g., <code>$_</code>
     * for last match.
     * <ul>
     * <li>$_ holds the full match.
     * <li>$1, $2, $3, etc. hold the backreferences.
     * <li>$+ holds the last (highest-numbered) backreference.
     * <li>$& (dollar ampersand) holds the entire regex match.
     * <li>$' (dollar followed by an apostrophe or single quote) holds the part
     * of the string after (to the right of) the regex match.
     * <li>$` (dollar backtick) holds the part of the string before (to the
     * left of) the regex match.
     * </ul>
     *
     * @param perlVar
     * @return
     */
    public String variable(String perlVar) {
        if (perlVar.equals("$_")) {
            return m.group();
        } else if (perlVar.startsWith("$") && perlVar.length() == 2
            && Character.isDigit(perlVar.charAt(1))) {
            int i = perlVar.charAt(1) - 48;
            return m.group(i);
        } else if (perlVar.equals("$+")) {
            return m.group(m.groupCount());
        } else if (perlVar.equals("$&")) {
            return m.group();
        } else if (perlVar.equals("$'")) {
            return text.substring(0, m.start() - 1);
        } else if (perlVar.equals("$`")) {
            return text.substring(m.end());
        }
        return null;
    }

    /**
     * shows which groups have matched which strings.
     *
     * @return
     */
    public String debugString() {

        StringBuffer b = debugPatternString(m.pattern().pattern());

        StringBuffer cc = space(text.length());
        for (int i = 0; i < cc.length() / 10; i++) {
            cc.setCharAt(i * 10, (char) (i % 10 + 48));
            if (10 * i + 5 < cc.length())
                cc.setCharAt(i * 10 + 5, '.');
        }
        b.append("10 =  ").append(cc);
        b.append("\nin = '").append(text).append("'");

        // create group information
        if (group() != null) {

            for (int i = 0; i <= groupCount(); i++) {
                b.append("\n").append("$").append(i).append(" = ");
                if (group(i) != null) {

                    b.append("'").append(group(i)).append("' [").append(
                        start(i)).append(",").append(end(i)).append("]");
                } else {
                    b.append("[null]");
                }
            }
            return b.toString();
        }
        return b.append("[no match]").toString();
    }

    /**
     * parses the pattern and outputs the capturing and non-capturing group
     * positions.
     *
     * @param pattern
     * @return
     */
    public StringBuffer debugPatternString(String pattern) {
        StringBuffer b = new StringBuffer();

        StringBuffer[] bb = groupsStrings(pattern);
        b.append("() =  ").append(bb[1]);
        b.append("\nre = '").append(pattern).append("'\n");
        b.append("(? =  ").append(bb[0]).append("\n");
        return b;
    }

    /**
     * assembles a string that shows the group boundaries of the current
     * pattern.
     *
     * @return
     */
    private StringBuffer[] groupsStrings(String pattern) {

        List<int[]> a = getGroupBounds(pattern);

        int len = pattern.length();
        // two buffers for real and non-capturing groups
        StringBuffer x = space(len);
        StringBuffer[] aa = new StringBuffer[2];
        aa[0] = x;
        aa[1] = new StringBuffer(x);
        int[] ii = {1, 1};
        for (int[] par : a) {
            if (par[0] >= 0)
                aa[par[2]].setCharAt(par[0], (char) (ii[par[2]] + 48));
            if (par[1] >= 0)
                aa[par[2]].setCharAt(par[1], (char) (ii[par[2]] + 48));
            ii[par[2]]++;
        }
        return aa;
    }

    /**
     * create a StringBuffer with len space characters.
     *
     * @param len
     * @return
     */
    private StringBuffer space(int len) {

        StringBuffer b = new StringBuffer();

        char[] a = new char[len];
        Arrays.fill(a, ' ');

        b.append(a);
        return b;
    }

    /**
     * Create a list of group bounds from the pattern string, which basically
     * looks at parentheses. Elements are [startpos, endpos, type] where type =
     * 1 and 0 for capturing and non-capturing groups. The list is ordered by
     * the group start positions.
     *
     * @param pattern
     * @return
     */
    private List<int[]> getGroupBounds(String pattern) {

        // the stack contains the groups in
        //
        Stack<int[]> pp = new Stack<int[]>();
        Vector<int[]> qq = new Vector<int[]>();

        PatternString s = new PatternString(pattern);

        // neg lookbehind and pos lookahead for escapes, special group
        // to capture non-capturing groups
        s.find("(?<!\\\\)\\((\\?)?|(?<!\\\\)\\)");
        while (s.found()) {

            if (s.group().startsWith("(")) {
                // opening parenthesis
                int[] a = {-1, -1, -1};
                a[0] = s.start();
                if (s.group(1) != null) {
                    a[2] = 0;
                } else {
                    a[2] = 1;
                }
                pp.push(a);
                qq.add(a);
            } else {
                // closing parenthesis.

                if (pp.isEmpty()) {
                    System.out.println("Too many closing parentheses.");
                    qq.add(new int[] {-1, s.start(), 0});
                } else {
                    int[] a = pp.pop();
                    a[1] = s.start();
                }
            }

            s.findNext();
        }

        if (!pp.isEmpty()) {
            System.out.println("Too many opening parentheses.");
        }

        return qq;
    }

    @Override
    public String toString() {
        return text.toString();
    }

    public final StringBuffer getText() {
        return text;
    }

    public final void setText(StringBuffer b) {
        this.text = b;
        m.reset(b);
    }

    public final int getFlags() {
        return flags;
    }

    public final void setFlags(int flags) {
        this.flags = flags;
    }

    public final Matcher getM() {
        return m;
    }

    public final void setM(Matcher m) {
        this.m = m;
    }
}
