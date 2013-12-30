package org.knowceans.util;

import java.io.PrintStream;
import java.lang.reflect.Array;
import java.util.List;

/**
 * convenience class to print information. Can also be used to construct
 * strings, then the internal PrintWriter writes to a StringBuilder that can be
 * output as a String object or to stdout (string() method).
 * 
 * @author gregor
 * 
 */
public class Print {

	private static PrintStream sout = System.out;
	private static StringBuilder sb = null;

	/**
	 * sets the output stream for the printer
	 * 
	 * @param s null to disable
	 */
	public static void setOutput(PrintStream s) {
		sout = s;
	}

	/**
	 * reset output to write to a string buffer
	 * 
	 * @param sb
	 */
	public static void setString(StringBuilder sb) {
		sout = new PrintStream(new StringOutputStream(sb));
	}

	/**
	 * set output to stdout
	 */
	public static void setToStdout() {
		sout = System.out;
	}

	/**
	 * set output to stderr
	 */
	public static void setToStderr() {
		sout = System.err;
	}

	/**
	 * setup the Print object for a new string output
	 */
	public static void newString() {
		sb = new StringBuilder();
		setString(sb);
	}

	/**
	 * get current output stream
	 * 
	 * @return
	 */
	public static PrintStream getOutput() {
		return sout;
	}

	/**
	 * prints all objects via tostring methods
	 * 
	 * @param a
	 * @param b
	 */
	public static void strings(Object a, Object... b) {
		if (sout == null)
			return;
		StringBuffer sb = new StringBuffer(a.toString());
		for (Object s : b) {
			sb.append(' ');
			sb.append(s);
		}
		System.out.println(sb);
	}

	/**
	 * prints all objects via tostring methods, separated by sep
	 * 
	 * @param a
	 * @param b
	 */
	public static void stringSep(String sep, Object... b) {
		if (sout == null)
			return;
		StringBuffer sb = new StringBuffer();
		for (Object s : b) {
			sb.append(sep);
			sb.append(s);
		}
		System.out.println(sb);
	}

	/**
	 * checks whether there are arrays in the objects
	 * 
	 * @param a
	 * @param b
	 */
	public static void arrays(Object a, Object... b) {
		if (sout == null)
			return;
		StringBuffer sb = new StringBuffer();
		printarray(sb, a);
		for (Object s : b) {
			sb.append(' ');
			printarray(sb, s);
		}
		sout.println(sb);
	}

	/**
	 * checks whether there are arrays in the objects. Adds the separator to
	 * each of the
	 * 
	 * @param
	 * @param b
	 */
	public static void arraysRowSep(String rowSep, Object... b) {
		// FIXME: rowsep and colsep must be handled differently
		arraysRowColSep(rowSep, "\n", b);
	}

	/**
	 * checks whether there are arrays in the objects. Adds the separator to
	 * each of the
	 * 
	 * @param a
	 * @param b
	 */
	public static void arraysRowColSep(String rowSep, String colSep,
			Object... b) {
		if (sout == null)
			return;
		StringBuffer sb = new StringBuffer();
		for (Object s : b) {
			// FIXME: rowsep and colsep must be handled differently
			// for array elements that are themselves arrays
			printarrayRowColSep(sb, colSep, colSep, s);
			sb.append(rowSep);
		}
		sout.println(sb);
	}

	/**
	 * simply printfs to sout
	 * 
	 * @param string
	 * @param m
	 * @param doc
	 */
	public static void f(String format, Object... b) {
		if (sout == null)
			return;
		sout.print(String.format(format, b));
	}

	/**
	 * call f() with newline
	 * 
	 * @param format
	 * @param b
	 */
	public static void fln(String format, Object... b) {
		f(format + "\n", b);
	}

	/**
	 * formats each array element to format.
	 * 
	 * @param a
	 * @param b
	 */
	public static void arraysf(String format, Object... b) {
		if (sout == null)
			return;
		StringBuffer sb = new StringBuffer();
		for (Object s : b) {
			sb.append(' ');
			printarray(sb, s, format);
		}
		sout.println(sb);
	}

	/**
	 * prints an array to sb
	 * 
	 * @param sb
	 * @param s
	 * @param format
	 */
	private static void printarray(StringBuffer sb, Object s, String format) {
		if (ArrayUtils.isArray(s)) {
			Object z = getElement0(s);
			if (ArrayUtils.isArray(z)) {
				for (int i = 0; i < Array.getLength(s); i++) {
					printarray(sb, Array.get(s, i), format);
					// sb.append("; ");
					sb.append(";\n ");
				}
			} else {
				sb.append(Vectors.printf(s, format, ", "));
			}
		} else {
			sb.append(s);
		}
	}

	private static void printarray(StringBuffer sb, Object s) {
		printarrayRowColSep(sb, " ", ";\n ", s);
	}

	private static void printarrayRowColSep(StringBuffer sb, String rowSep,
			String colSep, Object s) {
		if (ArrayUtils.isArray(s)) {
			// nested arrays?
			Object z = getElement0(s);
			if (z == null) {
				sb.append("[]");
			} else if (ArrayUtils.isArray(z)) {
				for (int i = 0; i < Array.getLength(s); i++) {
					printarray(sb, Array.get(s, i));
					// sb.append("; ");
					sb.append(colSep);
				}
			} else {
				// not using Vectors.print as we control the separator
				// sb.append(Vectors.print(s));
				// onedimensional array
				Object[] ss = ArrayUtils.convert(s);
				for (int i = 0; i < ss.length; i++) {
					if (i > 0) {
						sb.append(rowSep);
					}
					sb.append(ss[i]);
				}
			}
		} else {
			sb.append(s);
		}
	}

	/**
	 * gets the first element or null if empty array
	 * 
	 * @param s
	 * @return
	 */
	public static Object getElement0(Object s) {
		try {
			Object z = Array.get(s, 0);
			return z;
		} catch (ArrayIndexOutOfBoundsException e) {
			return null;
		}
	}

	/**
	 * prints the stack element of the current code location
	 */
	public static void whereami() {
		if (sout == null)
			return;
		List<StackTraceElement> here = Which.fullstack();
		strings(here.get(here.size() - 1));
	}

	/**
	 * returns a string with file and line position of the current location
	 * 
	 * @return
	 */
	public static String fileline() {
		List<StackTraceElement> here = Which.fullstack();
		StackTraceElement hereami = here.get(here.size() - 1);
		return hereami.getFileName() + ":" + hereami.getLineNumber();
	}

	/**
	 * returns a string with class and method position of the current position
	 * 
	 * @return
	 */
	public static String classmethod() {
		List<StackTraceElement> here = Which.fullstack();
		StackTraceElement hereami = here.get(here.size() - 1);
		return hereami.getClassName() + "." + hereami.getMethodName();
	}

	/**
	 * get the last string that has been written to (if any)
	 * 
	 * @return
	 */
	public static String getString() {
		return sb.toString();
	}

	/**
	 * get the last string that has been written to (if any)
	 * 
	 * @return
	 */
	public static void string() {
		System.out.println(getString());
	}

	public static void main(String[] args) {
		Print.newString();
		double[] a = new double[] { 0.1, 0.2, 0.7 };
		Print.arrays("test string", Samplers.randMult(a, 100));
		Print.setToStdout();
		Print.string();
		Print.newString();
	}

}
