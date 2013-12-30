package org.knowceans.corpus;

import java.util.Map.Entry;
import java.util.TreeMap;

import org.knowceans.map.BijectiveHashMap;

/**
 * Corpus resolver that is being created, so methods to add information are
 * implemented here as well as those that handle connection to a
 * CreateLabelNumCorpus.
 * 
 * @author gregor
 * 
 */
public class CreateCorpusResolver extends CorpusResolver {

	@SuppressWarnings("unchecked")
	BijectiveHashMap<String, Integer>[] keyMaps = new BijectiveHashMap[keyExtensions.length];

	/**
	 * create empty resolver instance
	 * 
	 * @param filebase
	 */
	public CreateCorpusResolver(String filebase) {
		this.filebase = filebase;
	}

	/**
	 * allocate keys in resolver
	 */
	public void allocKeys() {
		// nothing now, static init of outer array
	}

	/**
	 * allocate the array for the particular key type
	 * 
	 * @param keyType
	 * @param size
	 */
	public void allocKeyType(int keyType, int size) {
		data[keyType] = new String[size];

	}

	/**
	 * initialises resolver maps to build an index of the given array of label
	 * types (NOT key types, because most likely the array of label tyes is
	 * already there).
	 * 
	 * @param labelTypes L-types
	 */
	public void initMapsForLabelTypes(int[] labelTypes) {

		for (int labelType : labelTypes) {
			int keyType = labelId2keyExt[labelType];
			if (keyType >= 0) {
				initMapForKeyType(keyType);
			}
		}
	}

	/**
	 * initialises resolver maps to build an index of the given array of key
	 * types (NOT key types, because most likely the array of label tyes is
	 * already there).
	 * 
	 * @param keyTypes K-types
	 */
	public void initMapsForKeyTypes(int[] keyTypes) {

		for (int keyType : keyTypes) {
			initMapForKeyType(keyType);
		}
	}

	/**
	 * initialises the resolver map for the given key type
	 * 
	 * @param keyType
	 */
	public void initMapForKeyType(int keyType) {
		keyMaps[keyType] = new BijectiveHashMap<String, Integer>();
	}

	/**
	 * resolve the values of the label according to the
	 * 
	 * @param doc
	 * @param labelType
	 */
	public int[] addAndResolve(int keyType, String[] labels) {

		int[] ids = new int[labels.length];
		for (int i = 0; i < labels.length; i++) {
			ids[i] = addAndResolve(keyType, labels[i]);
		}
		return ids;
	}

	/**
	 * add a label to the index, potentially reusing an existing one
	 * 
	 * @param keyType
	 * @param labelString
	 * @return
	 */
	public int addAndResolve(int keyType, String labelString) {

		if (keyMaps[keyType] == null) {
			// TODO: may instantiate new map with exisiting data[keyType]
			System.out.println("warning: key map null for type "
					+ keyNames[keyType]);
			return -1;
		}
		Integer label = keyMaps[keyType].get(labelString);
		if (label == null) {
			label = keyMaps[keyType].size();
			keyMaps[keyType].put(labelString, label);
		}
		return label;
	}

	/**
	 * transform the existing label maps to the arrays.
	 * 
	 * @param deteteMaps whether to finish the addition process.
	 */
	public void compile(boolean deleteMaps) {
		for (int type = 0; type < keyExtensions.length; type++) {
			if (keyMaps[type] != null) {
				data[type] = getArray(keyMaps[type]);
			}
			keyMaps[type] = null;
		}
	}

	/**
	 * converts the map into an array of keys sorted by the values
	 * 
	 * @param map
	 * @return
	 */
	private String[] getArray(BijectiveHashMap<String, Integer> map) {
		String[] newList = new String[map.size()];
		TreeMap<Integer, String> inv = new TreeMap<Integer, String>(
				map.getInverse());
		for (Entry<Integer, String> e : inv.entrySet()) {
			newList[e.getKey()] = e.getValue();
		}
		return newList;
	}

	/**
	 * sets the key in the resolver data
	 * 
	 * @param type
	 * @param id
	 * @param value
	 */
	public void setValue(int type, int id, String value) {
		data[type][id] = value;
	}

	/**
	 * sets the complete array of keys for a particular type
	 * 
	 * @param type
	 */
	public void setKeys(int type, String[] values) {
		data[type] = values;
	}
}
