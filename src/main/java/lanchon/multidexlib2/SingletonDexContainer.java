/*
 * DexPatcher - Copyright 2015-2019 Rodrigo Balerdi
 * (GNU General Public License version 3 or later)
 *
 * DexPatcher is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 */

package lanchon.multidexlib2;

import java.util.Collections;
import java.util.Map;

import org.jf.dexlib2.iface.DexFile;

public class SingletonDexContainer<T extends DexFile> extends AbstractMultiDexContainer<T> {

	// I insist that some dex container entries do not have names
	// even though dexlib2 does not allow null entry names.
	// If this constant is ever changed to hold a non-null value,
	// the new value will be a unique instance of String that will
	// act as a sentinel. Clients should always test for this value
	// using the '==' operator rather than the 'equals(...)' method.
	public static final String UNDEFINED_ENTRY_NAME = null;

	public SingletonDexContainer(T dexFile) {
		this(UNDEFINED_ENTRY_NAME, dexFile);
	}

	public SingletonDexContainer(String entryName, T dexFile) {
		DexEntry<T> entry = new BasicDexEntry<>(this, entryName, dexFile);
		Map<String, DexEntry<T>> entryMap = Collections.singletonMap(entryName, entry);
		initialize(entryMap);
	}

}
