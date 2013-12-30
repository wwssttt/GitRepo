package org.knowceans.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * ArrayCache represents a cache for a large multidimensional array, with the
 * minor dimensions being type-dependent. The outmost array, say int[][] is
 * completely exposed to programming code, allowing the developer usage of the
 * ordinary array data structure. If code is about to access other parts of the
 * array, a fetch() message is sent to the array. After it is clear that the
 * element will not be touched for a while, a done() message is sent.
 * <p>
 * The idea is to lookup a row in the main array and check whether this is
 * loaded. All rows not currently fetched are set to null. The user needs to
 * either fetch the data to work on a given row or put it into the array
 * themselves and call done() on the row.
 * 
 * @author gregor
 * 
 * @param <T>
 *            type of the array elements
 */
// TODO: implement
public class ArrayCache<T> {

	public static void main(String[] args) {
		ArrayCache<int[]> ac;
		try {
			ac = new ArrayCache<int[]>("test.txt", 1000, 4, new int[1000][]);
			int[][] aa = ac.getArray();
			for (int i = 0; i < aa.length; i++) {

			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	/**
	 * worker array
	 */
	T[] array;

	/**
	 * file name of the backend of this cache
	 */
	private String backend;

	/**
	 * file of the backend
	 */
	RandomAccessFile database = null;

	private int rowsize;

	private int typesize;

	/**
	 * init the cache with the backend file and array
	 * 
	 * @param backend
	 * @param array
	 *            the T[length] array with all higher dimensions null or filled
	 * @param rowsize
	 *            length of rows
	 * @param typesize
	 *            size of one row element in bytes (array length x element size
	 *            for multidimensional arrays)
	 * @throws FileNotFoundException
	 */
	public ArrayCache(String backend, int rowsize, int typesize, T[] array)
			throws FileNotFoundException {
		this(backend, array, rowsize, typesize, "rw");
	}

	/**
	 * init the cache with the backend file and array
	 * 
	 * @param backend
	 * @param array
	 *            the T[length] array with all higher dimensions null or filled
	 * @param rowsize
	 *            length of rows
	 * @param typesize
	 *            size of one row element in bytes (array length x element size
	 *            for multidimensional arrays)
	 * @param access
	 *            string to control behaviour of file backend:
	 *            r(ead)|w(rite)|s(ynced)
	 * @throws FileNotFoundException
	 */
	public ArrayCache(String backend, T[] array, int rowsize, int typesize,
			String access) throws FileNotFoundException {
		this.array = array;
		this.rowsize = rowsize;
		this.typesize = typesize;
		this.backend = backend;
		this.database = new RandomAccessFile(new File(backend), access);

	}

	/**
	 * fetch a range of the array from the backend
	 * 
	 * @param rowstart
	 * @param length
	 * @return
	 * @throws IOException
	 */
	public boolean fetch(int rowstart, int length) throws IOException {
		database.seek(rowstart * rowsize * typesize);
		for (int i = 0; i < length; i++) {

		}
		return false;
	}

	/**
	 * fetch a single row
	 * 
	 * @param row
	 * @return
	 */
	public boolean fetch(int row) {
		return false;
	}

	/**
	 * 
	 * @param rowstart
	 * @param length
	 * @return
	 */
	public boolean done(int rowstart, int length) {
		return false;
	}

	/**
	 * done with the single row
	 * 
	 * @param row
	 * @return
	 */
	public boolean done(int row) {
		return false;
	}

	/**
	 * save the complete array to the backend
	 * 
	 * @return
	 */
	public boolean save() {
		return false;
	}

	/**
	 * get the array currently in this cache
	 * 
	 * @return
	 */
	private T[] getArray() {
		return array;
	}

}
