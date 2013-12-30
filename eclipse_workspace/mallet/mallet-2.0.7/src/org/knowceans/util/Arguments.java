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
/*
 * Created on May 26, 2005
 */
package org.knowceans.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Arguments is a convenience class that reads command line arguments for main
 * and validates them against a given format. The general grammar of a
 * commandline is:
 * <p>
 * 
 * <pre>
 * 
 *  commandline ::= command &quot; &quot; options arguments
 *  
 * </pre>
 * 
 * <p>
 * Here the options and arguments follow the grammar:
 * <p>
 * 
 * <pre>
 * 
 *  options   ::= ( option &quot; &quot; )+
 *  option    ::= &quot;-&quot; name (&quot; &quot; value)?
 *  name      ::= &lt;LITERAL&gt;  
 *  arguments ::= ( argument )+ 
 *  argument  ::= value
 *  value     ::= &lt;LITERAL&gt; | &quot;&quot;&quot;( &lt;LITERAL&gt; | &quot; &quot; )+ &quot;&quot;&quot;
 *  
 * </pre>
 * 
 * with some value. Values can be restricted to specific formats, which is done
 * using a format string. Known types are int, float, long, double, boolean,
 * String, File and URL. String and File values can contain spaces if put within
 * quotes.
 * <p>
 * TODO: resolve problem with command line monitor getCallString()
 * 
 * @author heinrich
 */
public class Arguments {

    /**
     * TeeStream splits output to two PrintStreams.
     * 
     * @author heinrich
     */
    class TeeStream extends PrintStream {

        PrintStream stream2 = null;

        /**
         * @param out
         */
        public TeeStream(OutputStream out1, OutputStream out2) {
            super(out1, true);
            stream2 = new PrintStream(out2, true);
        }

        public void close() {
            super.close();
            stream2.close();
        }

        public void flush() {
            super.flush();
            stream2.flush();
        }

        public void write(int b) {
            byte[] buf = new byte[1];
            buf[0] = (byte) b;
            write(buf, 0, 1);
        }

        public void write(byte[] b, int off, int len) {
            super.write(b, off, len);
            stream2.write(b, off, len);
        }

    }

    private boolean debug = false;

    private boolean tee = true;

    String helpFormat = "\\s*(\\{([^\\}]+)\\})?";

    String types = "ilfdbspu";

    String optionFormat = "([\\w\\d_-]+(\\|[\\w\\d_-]+)?)(=([" + types
        + "0]))?" + helpFormat;

    String argFormat = "([" + types + "])" + helpFormat;

    PrintStream stdout = null;

    PrintStream stderr = null;

    /**
     * contains the option types
     */
    TreeMap<String, Character> optionTypes = new TreeMap<String, Character>();

    /**
     * contains help information
     */
    TreeMap<String, String> help = new TreeMap<String, String>();

    /**
     * help text.
     */
    String helptext = null;

    /**
     * contains the argument types of required arguments
     */
    StringBuffer argTypes = new StringBuffer();

    /**
     * stores synonyms.
     */
    HashMap<String, String> synonyms = new HashMap<String, String>();

    /**
     * contains the arguments
     */
    Vector<Object> arguments = new Vector<Object>();

    /**
     * contains the options
     */
    HashMap<String, Object> options = new HashMap<String, Object>();

    String variable = null;

    int minArgs = 0;

    int maxArgs = 0;

    /**
     * allow redirection of input/output streams
     */
    private boolean redirect = false;

    private boolean allowVariable = false;

    public static void main(String[] args) {
        String[] commandline = "-xserverstarttime .4 -s test -v -words 34 -f \"test object\" -g 1.2 -t http://www.ud path path2 true"
            .split(" ");

        String format = "xserverstarttime=f {xserv test constant} " + //
            "words=i {number of words}" + //
            "f=s {file string} " + //
            "g=f {global weight} " + //
            "s=p {output file} " + //
            "v=0 {verbose} " + "r=0 {reloadable\n(for the server)} t=u";
        String types = "p {infile} p {outfile} | b {use patterns} b {debug}";
        Arguments arg = new Arguments(format, types);
        arg.parse(commandline);
        System.out.println(arg.getOption("xserverstarttime", .5));
        System.out.println(arg.getOption("g"));
        System.out.println(arg.getOption("t"));
        System.out.println(arg.getOption("v"));
        System.out.println(arg.getOptions());
        System.out.println(arg.getArguments());
        System.out.println(arg.maxArgs);
        System.out.println(arg);
    }

    /**
     * Initialise the arguments parser with a format string and an argument type
     * specification. For the options and arguments, the formats are defined by
     * this constructor.
     * <p>
     * The format string for the options is composed of the following grammar:
     * 
     * <pre>
     * 
     *  foptions   ::= ( option )*
     *  foption    ::= name ( &quot;|&quot; name )? &quot;=&quot; fotype &quot; &quot;? ( &quot;{&quot; fhelp &quot;}&quot; )?
     *  fname      ::= &lt;LITERAL&gt;
     *  fotype     ::= ( i | l | f | d | b | u | p | s | 0 )
     *  fhelp      ::= &lt;LITERAL&gt;
     *  
     * </pre>
     * 
     * The literals of fotype correspond to the types int, float, long, double,
     * boolean, java.net.URL, java.io.File (p), java.lang.String, and void (0)
     * for unparametrised options
     * <p>
     * The format string for the arguments is composed of the following grammar:
     * 
     * <pre>
     * 
     *  farguments    ::= frequiredargs &quot;|&quot; foptionalargs
     *  frequiredargs ::= ( fatype &quot; &quot;? ( &quot;{&quot; fhelp &quot;}&quot; )? )+
     *  foptionalargs ::= ( fatype &quot; &quot;? ( &quot;{&quot; fhelp &quot;}&quot; )? )+)+
     *  fatype        ::= ( i | l | f | d | b | u | p | s )
     *  fhelp      ::= &lt;LITERAL&gt;
     *  
     * </pre>
     * 
     * The help strings can include line breaks "\n".
     * 
     * @param optformat format string for options
     * @param argtypes format string for arguments
     */
    public Arguments(String optformat, String argtypes) {
        Matcher m = Pattern.compile(optionFormat).matcher(optformat);
        while (m.find()) {
            String[] keys = m.group(1).split("\\|");
            if (keys.length > 1) {
                synonyms.put(keys[0], keys[1]);
                synonyms.put(keys[1], keys[0]);
            }

            for (String key : keys) {
                if (optionTypes.containsKey(key))
                    throw new IllegalArgumentException("option " + key
                        + " is duplicate in format specification.");
            }

            String type = m.group(4) != null ? m.group(4) : "0";
            optionTypes.put(keys[0], type.charAt(0));
            String desc = m.group(6);
            if (desc != null) {
                help.put(keys[0], desc);
            }
        }
        m = Pattern.compile(argFormat).matcher(argtypes);
        int narg = 1;
        while (m.find()) {
            argTypes.append(m.group(1));
            String desc = m.group(3);
            if (desc != null) {
                help.put(Integer.toString(narg), desc);
                narg++;
            }
        }
        String typechars = argtypes.replaceAll(" ", "").replaceAll(
            "\\{[^\\}]+\\}", "");
        minArgs = typechars.indexOf('|');
        if (minArgs == -1) {
            minArgs = argTypes.length();
        }
        maxArgs = argTypes.length();
    }

    /**
     * same as 2-argument constructor, but sets a help text.
     * 
     * @param optformat
     * @param argtypes
     * @param helptext
     */
    public Arguments(String optformat, String argtypes, String helptext) {
        this(optformat, argtypes);
        help(helptext);
    }

    /**
     * get the map of options.
     * 
     * @return
     */
    public HashMap<String, Object> getOptions() {
        return options;
    }

    /**
     * returns the named option value. If it has no parameter, Boolean.TRUE is
     * returned if given, otherwise Boolean.FALSE.
     * 
     * @param string key for the option parameter.
     * @return value of option parameter (that can be casted to the specific
     *         type) or null if not given at command line or not in format.
     * @throws IllegalArgumentException
     */
    public Object getOption(String string) throws IllegalArgumentException {
        String voption = getVOption(string);
        if (voption == null) {
            throw new IllegalArgumentException("Option " + string + " unknown.");
        }
        Object obj = options.get(voption);
        Character type = optionTypes.get(voption);
        if (obj == null && type == null) {
            throw new IllegalArgumentException("Option " + string + " unknown.");
        }
        if (type == '0') {
            if (obj == null)
                obj = Boolean.FALSE;
        }
        return obj;
    }

    // jan 3532872
    /**
     * Same as getOption, but allows default value (whose type is NOT checked).
     * 
     * @param string
     * @param defaultValue
     * @return
     * @throws IllegalArgumentException
     */
    public Object getOption(String string, Object defaultValue)
        throws IllegalArgumentException {
        Object obj = getOption(string);
        if (obj == null) {
            obj = defaultValue;
        }
        if (debug) {
            System.out.println(string.replaceFirst("^\\-", "") + " = " + obj);
        }
        return obj;
    }

    /**
     * get the vector of all arguments.
     * 
     * @return
     */
    public Vector<Object> getArguments() {
        return arguments;
    }

    /**
     * Returns the argument with index i and null if i is compliant with the
     * format but not specified at commandline. Arguments start at the index 1,
     * index 0 returns a the full name of the main class.
     * 
     * @param i
     * @return
     * @throws IllegalArgumentException
     */
    public Object getArgument(int i) throws IllegalArgumentException {
        if (i == 0) {
            return Which.main();
        }
        if (i > argTypes.length()) {
            throw new IllegalArgumentException("Format supports only "
                + maxArgs + " arguments, not " + (i + 1) + ".");
        }
        if (i > arguments.size()) {
            return null;
        }
        Object obj = arguments.get(i - 1);
        if (debug) {
            System.out.println((i) + " = " + obj);
        }
        return obj;
    }

    /**
     * The full call string of the program, including the classpath and working
     * directory. TODO: parameterless options are printed with boolean value...
     * 
     * @param format true to format lines.
     * @return
     */
    public String getCallString(boolean format) {
        String linesep = format ? "\n\t" : "";
        StringBuffer path = new StringBuffer(System.getProperty("user.dir"));
        String classpath = System.getProperty("java.class.path");
        path.append(File.separatorChar + "java -cp " + linesep);
        path.append(classpath.replaceAll(File.pathSeparator, File.pathSeparator
            + linesep)
            + linesep);
        path.append(linesep).append(" ");
        path.append(Which.main());
        path.append(linesep).append(" ");
        for (String key : options.keySet()) {
            // String param = "";
            // VerifyError...
            // if (optionTypes.get(key) != '0') {
            // param += options.get(key).toString();
            // }
            path.append(linesep + "-" + key + " " + options.get(key) + " ");
        }
        path.append(linesep);
        for (Object arg : arguments) {
            path.append(linesep).append(arg).append(" ");
        }
        return path.toString();
    }

    /**
     * Same as getArgument, but returns a default value if optional argument is
     * not set. The type of the default is NOT checked.
     * 
     * @param i
     * @param defaultValue
     * @return
     */
    public Object getArgument(int i, Object defaultValue) {
        Object obj = getArgument(i);
        if (obj == null) {
            return defaultValue;
        }
        return obj;
    }

    /**
     * Parses the command line arguments string whose values can be found with
     * the getOption and getArgument methods afterwards. Further, if option -?
     * is not specified in the format string, the help string (accessible via
     * toString()) is output to stdout and System.exit(0) called. The same works
     * for -stdout and a file where the output is sent to, -stderr, -stdouterr,
     * and -stdin work accordingly. Stream redirection must be explicitly
     * enabled by calling redirect(true) and, at the end of the program, to call
     * the close() method. By default, streams are duplicated to stdout / stderr
     * to allow monitoring, disable stream duplication by calling tee(false).
     * <p>
     * Another important possibility is the option -$, which allows to set a
     * variable that can be used afterwards in the option and argument values by
     * using $@. Enable this using the variable(true);
     * 
     * @param args the argument string, typically directly that of a main
     *        method.
     * @throws IllegalArgumentException if the commandline arguments do not
     *         match the given format.
     */
    public void parse(String[] args) throws IllegalArgumentException {
        int nargs = 0;
        // boolean needsparam = false;
        String option = "";
        for (int i = 0; i < args.length; i++) {
            // allow more than one space between arguments
            if (args[i].trim().equals(""))
                continue;
            if (args[i].startsWith("-") && nargs == 0) {
                option = args[i].substring(1, args[i].length());
            } else {
                if (nargs > argTypes.length() - 1) {
                    String argstring = "";
                    for (String arg : args) {
                        argstring += arg + " ";
                    }
                    throw new IllegalArgumentException(
                        "Options do not comply with format. "
                            + "Check option parameters:\n  " + argstring + "\n\n" + this.toString());
                }
                char type = argTypes.charAt(nargs);
                if (allowVariable && variable != null) {
                    args[i] = replaceVariable(args[i]);
                }
                Object argument = getObject(args[i], type);
                if (argument == null) {
                    throw new IllegalArgumentException("Option " + option
                        + " has not required type " + type + ".");
                }
                arguments.add(argument);
                nargs++;
            }
            if (nargs > 0)
                continue;
            String voption = getVOption(option);
            if (voption == null) {
                if (option.equals("?")) {
                    System.out.println(this);
                    System.exit(0);
                } else if (option.equals("stdout") && redirect) {
                    i++;
                    String outfile = args[i];
                    try {
                        BufferedOutputStream bos = new BufferedOutputStream(
                            new FileOutputStream(replaceVariable(outfile)));
                        stdout = tee ? new TeeStream(bos, System.out)
                            : new PrintStream(bos, true);
                        System.setOut(stdout);
                    } catch (Exception e) {
                        throw new IllegalArgumentException(
                            "Unable to redirect stdout to " + outfile);
                    }
                    continue;
                } else if (option.equals("stderr") && redirect) {
                    i++;
                    String outfile = args[i];
                    try {
                        BufferedOutputStream bos = new BufferedOutputStream(
                            new FileOutputStream(replaceVariable(outfile)));
                        stderr = tee ? new TeeStream(bos, System.out)
                            : new PrintStream(bos);
                        System.setErr(stderr);
                    } catch (Exception e) {
                        throw new IllegalArgumentException(
                            "Unable to redirect stderr to " + outfile);
                    }
                    continue;
                } else if (option.equals("stdouterr") && redirect) {
                    i++;
                    String outfile = args[i];
                    try {
                        BufferedOutputStream bos = new BufferedOutputStream(
                            new FileOutputStream(replaceVariable(outfile)));
                        stdout = tee ? new TeeStream(bos, System.out)
                            : new PrintStream(bos);
                        System.setOut(stdout);
                        System.setErr(stdout);
                    } catch (Exception e) {
                        throw new IllegalArgumentException(
                            "Unable to redirect stderr to " + outfile);
                    }
                    continue;
                } else if (option.equals("stdin") && redirect) {
                    System.out
                        .println("Stdin redirection not implemented yet.");
                    i++;
                    continue;
                } else if (option.equals("$") && allowVariable) {
                    i++;
                    variable = args[i];
                    continue;
                } else {
                    throw new IllegalArgumentException("Option " + option
                        + " unknown.");
                }
            }
            Character type = optionTypes.get(voption);

            // read the parameter (if still reading options)
            if (type != '0') {
                i++;

                String value = "";
                // consume quoted parameters
                if (args[i].startsWith("\"")) {
                    args[i] = args[i].substring(1);
                    while (!args[i].endsWith("\"")) {
                        value += args[i] + " ";
                        i++;
                    }
                    args[i] = args[i].substring(0, args[i].length() - 1);
                }
                value += args[i];
                value = replaceVariable(value);
                Object param = getObject(value, type);
                if (param == null) {
                    throw new IllegalArgumentException("Option " + option
                        + " has not required type " + type + ".");
                }
                options.put(voption, param);
            } else {
                options.put(voption, Boolean.TRUE);
            }
        }
        if (nargs < minArgs) {
            throw new IllegalArgumentException(
                "Number of required arguments is " + minArgs + ", but only "
                    + nargs + " given.");
        }
    }

    /**
     * @param value
     * @return
     */
    private String replaceVariable(String value) {
        if (allowVariable && value != null) {
            int rep = value.indexOf("$@");
            if (rep != -1) {
                value = value.substring(0, rep) + variable
                    + value.substring(rep + 2);
            }

        }
        return value;
    }

    /**
     * get the main variant of the option.
     * 
     * @param option
     * @return
     */
    private String getVOption(String option) {
        Character c = optionTypes.get(option);

        if (c != null)
            return option;

        String voption = synonyms.get(option);
        if (voption == null)
            return null;

        c = optionTypes.get(voption);
        if (c != null)
            return voption;

        return null;
    }

    /**
     * @param string
     * @param b
     */
    private Object getObject(String string, char type)
        throws IllegalArgumentException {
        Object obj = null;
        try {
            if (type == 'b') {
                obj = Boolean.valueOf(string);
            } else if (type == 'f') {
                obj = Float.valueOf(string);
            } else if (type == 'd') {
                obj = Double.valueOf(string);
            } else if (type == 'i') {
                obj = Integer.valueOf(string);
            } else if (type == 'l') {
                obj = Long.valueOf(string);
            } else if (type == 's') {
                obj = string;
            } else if (type == 'u') {
                obj = new URL(string);
            } else if (type == 'p') {
                obj = new File(string).getCanonicalFile();
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Wrong parameter format ("
                + type + "): " + string);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Wrong URL format: " + string);
        } catch (IOException e) {
            throw new IllegalArgumentException("Wrong file name format: "
                + string);
        }
        return obj;
    }

    /**
     * describe the current arguments set
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        if (helptext != null) {
            sb.append(helptext).append("\n\n");
        }
        sb.append(Which.main());
        // if (optionTypes.size() > 0)
        sb.append(" <options>");
        if (minArgs > 0)
            sb.append(" <required arguments>");
        if (maxArgs > minArgs)
            sb.append(" <optional arguments>");
        sb.append("\n");
        int maxOption = 8;

        sb.append("\nOptions:\n");
        for (String a : optionTypes.keySet()) {
            String syn = synonyms.get(a);
            int len = (syn == null) ? 0 : syn.length() + 3;
            maxOption = Math.max(maxOption, a.length() + len);
        }
        maxOption += 8;
        for (String key : optionTypes.keySet()) {
            sb.append("  ").append("-").append(key);
            String syn = synonyms.get(key);
            if (syn != null)
                sb.append(" | -").append(syn);
            spacePad(sb, maxOption);
            sb.append(type(optionTypes.get(key)));
            addDescription(sb, key, maxOption + 10);
            sb.append("\n");

        }
        if (allowVariable) {
            sb.append("  ").append("-$");
            spacePad(sb, maxOption);
            sb.append("string    # a variable that farther right replaces $@");
            sb.append("\n");
        }
        if (!optionTypes.containsKey("?")) {
            sb.append("  ").append("-?");
            spacePad(sb, maxOption);
            sb.append("(void)    # this synopsis");
            sb.append("\n");
        }

        if (redirect) {
            sb.append("  ").append("-stdout");
            spacePad(sb, maxOption);
            sb.append("filename  # redirect stdout");
            sb.append("\n");
            sb.append("  ").append("-stderr");
            spacePad(sb, maxOption);
            sb.append("filename  # redirect stderr");
            sb.append("\n");
            sb.append("  ").append("-stdouterr");
            spacePad(sb, maxOption);
            sb.append("filename  # redirect stdout + stderr");
            sb.append("\n");
        }

        if (minArgs > 0)
            sb.append("\nRequired arguments:\n");
        else
            sb.append("\nNo required arguments.\n");
        for (int i = 0; i < maxArgs; i++) {
            if (i == minArgs) {
                if (maxArgs - minArgs > 0)
                    sb.append("\nOptional arguments:\n");
                else
                    sb.append("\nNo optional arguments.\n");
            }
            sb.append("  ").append(i + 1);
            spacePad(sb, maxOption);
            sb.append(type(argTypes.charAt(i)));
            addDescription(sb, Integer.toString(i + 1), maxOption + 10);
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * @param sb
     * @param key
     */
    private void addDescription(StringBuffer sb, String key, int position) {
        String desc = help.get(key);
        if (desc == null)
            return;
        String[] lines = desc.split("\n");
        for (int i = 0; i < lines.length; i++) {
            spacePad(sb, position);
            sb.append("# ").append(lines[i]);
            if (i < lines.length - 1)
                sb.append("\n");
        }
    }

    /**
     * @return
     */
    public void spacePad(StringBuffer b, int length) {
        int linelength = b.length() - b.lastIndexOf("\n");
        for (int i = 0; i < length - linelength; i++) {
            b.append(' ');
        }
    }

    public String type(char c) {
        if (c == 'i')
            return "int";
        if (c == 'l')
            return "long";
        if (c == 'f')
            return "float";
        if (c == 'd')
            return "double";
        if (c == 'p')
            // return "java.io.File";
            return "filename";
        if (c == 'u')
            // return "java.net.URL";
            return "url";
        if (c == '0')
            return "(void)";
        if (c == 'b')
            return "boolean";
        if (c == 's')
            // return "java.lang.String";
            return "string";
        return "(unknown)";
    }

    /**
     * @param b
     */
    public void debug(boolean b) {

        debug = b;
    }

    /**
     * @param string
     */
    public void help(String string) {

        helptext = string;
    }

    /**
     * This method must be called if output redirection is used.
     */
    public void close() {
        if (stdout != null && stdout != System.out) {
            stdout.close();
        }
        if (stderr != null && stderr != System.err) {
            stderr.close();
        }
    }

    /**
     * Enable / disable redirection of pipe streams (default = disabled)
     * 
     * @param b
     */
    public void redirect(boolean b) {

        redirect = b;
    }

    /**
     * Enable / disable variable replacement (default = disabled).
     * 
     * @param b
     */
    public void variable(boolean b) {

        allowVariable = b;
    }

    /**
     * Enable / disable output duplication (default = enabled).
     * 
     * @param b
     */
    public void tee(boolean b) {

        tee = b;
    }

}
