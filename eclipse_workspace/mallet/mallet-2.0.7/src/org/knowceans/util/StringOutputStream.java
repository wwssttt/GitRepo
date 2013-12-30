package org.knowceans.util;

import java.io.OutputStream;

/**
 * stream output into a string builder. This is non-threadsafe
 * 
 * @author gregor
 * 
 */
public class StringOutputStream extends OutputStream {

	/**
	 * where to output alternative
	 */
	StringBuilder sb;

	/**
	 * init with new string builder
	 */
	public StringOutputStream() {
		sb = new StringBuilder(0x200);
	}

	/**
	 * init with provided string builder
	 */
	public StringOutputStream(StringBuilder sb) {
		if (sb != null)
			this.sb = sb;
		else
			this.sb = new StringBuilder(0x200);
	}

	/**
	 * write byte to the string buffer
	 */
	public void write(int b) {
		sb.append((char) b);
	}

	/**
	 * compile to string
	 */
	public String toString() {
		return sb.toString();
	}

	/**
	 * get the underlying string buffer
	 * 
	 * @return
	 */
	public StringBuilder getBuffer() {
		return sb;
	}
}
