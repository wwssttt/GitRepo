package org.tartarus.snowball;


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
public abstract class SnowballStemmer extends SnowballProgram {
	public abstract boolean stem();
};
