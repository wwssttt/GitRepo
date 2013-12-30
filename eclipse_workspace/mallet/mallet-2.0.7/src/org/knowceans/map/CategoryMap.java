package org.knowceans.map;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * map that allows to cumulate entries of type T, keeping track of frequencies
 * and id values.
 * <p>
 * TODO: remove() not handling ids internally, and any new element is created
 * using the size of the category map as an id.
 * 
 * @author gregor
 * 
 * @param <T>
 */
public class CategoryMap<T> extends HashMap<T, Integer> {

	private static final long serialVersionUID = -4586403552798487356L;

	private HashMap<T, Integer> categories = new HashMap<T, Integer>();

	/**
	 * add one point to the element, returning an integer id of the category of
	 * the element.
	 * 
	 * @param element
	 * @return category of element
	 */
	public int add(T element) {
		Integer count = get(element);
		Integer category = null;
		if (count == null) {
			put(element, 1);
			category = categories.size();
			categories.put(element, category);
			return category;
		} else {
			count++;
			put(element, count);
			category = categories.get(element);
		}
		return category;
	}

	/**
	 * subtract one point to the element. If the result is zero, the element is
	 * not removed
	 * 
	 * @param element
	 * @return category of the element
	 */
	public int subtract(T element) {
		Integer count = get(element);
		Integer category = null;
		if (count == null) {
			put(element, -1);
			category = categories.size();
			categories.put(element, category);
			return -1;
		} else {
			count--;
			put(element, count);
			category = categories.get(element);
		}
		return category;
	}

	@Override
	public Integer put(T key, Integer value) {
		Integer v = super.put(key, value);
		if (v == null) {
			categories.put(key, categories.size());
		}
		return v;
	}

	@Override
	public void putAll(Map<? extends T, ? extends Integer> m) {
		for (Entry<? extends T, ? extends Integer> e : m.entrySet()) {
			put(e.getKey(), e.getValue());
		}
	}

	/**
	 * Note: does not remove element to preserve id integrity. Use clear() and
	 * rebuild the map.
	 */
	@Override
	public Integer remove(Object key) {
		return get(key);
	}

	@Override
	public void clear() {
		super.clear();
		categories.clear();
	}

	/**
	 * gets the numerical category value for this element (whereas the get()
	 * method returns the bin count.)
	 * 
	 * @param key
	 * @return
	 */
	public Integer getCategory(T key) {
		return categories.get(key);
	}
}
