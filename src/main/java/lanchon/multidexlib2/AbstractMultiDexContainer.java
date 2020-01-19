/*
 * multidexlib2 - Copyright 2015-2020 Rodrigo Balerdi
 * (GNU General Public License version 3 or later)
 *
 * multidexlib2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 */

package lanchon.multidexlib2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jf.dexlib2.iface.DexFile;
import org.jf.dexlib2.iface.MultiDexContainer;

public abstract class AbstractMultiDexContainer<T extends DexFile> implements MultiDexContainer<T> {

	private Map<String, DexEntry<T>> entryMap;
	private List<String> entryNames;

	protected AbstractMultiDexContainer() {}

	protected void initialize(Map<String, DexEntry<T>> entryMap) {
		if (entryMap == null) throw new NullPointerException("entryMap");
		if (this.entryMap != null) throw new IllegalStateException("Already initialized");
		this.entryMap = entryMap;
		// See: https://github.com/JesusFreke/smali/issues/458
		entryNames = Collections.unmodifiableList(new ArrayList<>(entryMap.keySet()));
	}

	@Override
	public List<String> getDexEntryNames() {
		return entryNames;
	}

	@Override
	public DexEntry<T> getEntry(String entryName) {
		return entryMap.get(entryName);
	}

	protected DuplicateEntryNameException duplicateEntryName(String entryName) {
		return new DuplicateEntryNameException(entryName);
	}

}
