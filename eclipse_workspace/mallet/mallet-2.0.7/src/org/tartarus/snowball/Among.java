package org.tartarus.snowball;

import java.lang.reflect.Method;

/**
 * Used under BSD-license [1] from Snowball package [2]. Copyright (c) 2001, Dr
 * Martin Porter, and (for the Java developments) Copyright (c) 2002, Richard
 * Boulton.
 * <p>
 * References:
 * <p>
 * [1] http://www.opensource.org/licenses/bsd-license.html
 * <p>
 * [2] http://snowball.tartarus.org
 * 
 */
public class Among {
	public Among(String s, int substring_i, int result, String methodname,
			SnowballProgram methodobject) {
		this.s_size = s.length();
		this.s = s.toCharArray();
		this.substring_i = substring_i;
		this.result = result;
		this.methodobject = methodobject;
		if (methodname.length() == 0) {
			this.method = null;
		} else {
			try {
				this.method = methodobject.getClass().getDeclaredMethod(
						methodname, new Class[0]);
			} catch (NoSuchMethodException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public final int s_size; /* search string */
	public final char[] s; /* search string */
	public final int substring_i; /* index to longest matching substring */
	public final int result; /* result of the lookup */
	public final Method method; /* method to use if substring matches */
	public final SnowballProgram methodobject; /* object to invoke method on */
};
