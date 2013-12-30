/*
 * Created on Feb 7, 2010
 */
package org.knowceans.util;

/**
 * AccentRemover removes accents from unicode strings.
 * 
 * @author gregor
 */
public class AccentRemover {

	// from http://www.rgagnon.com/javadetails/java-0456.html
	private static final String PLAIN_ASCII = "AaEeIiOoUu" // grave
			+ "AaEeIiOoUuYy" // acute
			+ "AaEeIiOoUuYy" // circumflex
			+ "AaOoNn" // tilde
			+ "AaEeIiOoUuYy" // umlaut
			+ "Aa" // ring
			+ "Cc" // cedilla
			+ "OoUu" // double acute
	;

	private static final String UNICODE = //
	"\u00C0\u00E0\u00C8\u00E8\u00CC\u00EC\u00D2\u00F2\u00D9\u00F9" // grave
			+ "\u00C1\u00E1\u00C9\u00E9\u00CD\u00ED\u00D3\u00F3\u00DA\u00FA\u00DD\u00FD" // acute
			+ "\u00C2\u00E2\u00CA\u00EA\u00CE\u00EE\u00D4\u00F4\u00DB\u00FB\u0176\u0177" // circumflex
			+ "\u00C3\u00E3\u00D5\u00F5\u00D1\u00F1" // tilde
			+ "\u00C4\u00E4\u00CB\u00EB\u00CF\u00EF\u00D6\u00F6\u00DC\u00FC\u0178\u00FF" // umlaut
			+ "\u00C5\u00E5" // ring
			+ "\u00C7\u00E7" // cedilla
			+ "\u0150\u0151\u0170\u0171" // double acute
	;

	/**
	 * replaces unicode accents by their unaccented counterparts
	 * 
	 * @param termBuffer
	 * @param termLength
	 */
	public static void replaceAccents(char[] termBuffer, int termLength) {
		for (int i = 0; i < termLength; i++) {
			char c = termBuffer[i];
			int pos = UNICODE.indexOf(c);
			if (pos > -1) {
				termBuffer[i] = PLAIN_ASCII.charAt(pos);
			}
		}
	}

	/**
	 * apply accent removal to a string
	 * 
	 * @param word
	 * @return
	 */
	public static String replaceAccents(String word) {
		char[] termBuffer = word.toCharArray();
		replaceAccents(termBuffer, word.length());

		word = String.copyValueOf(termBuffer);
		return word;
	}
}
