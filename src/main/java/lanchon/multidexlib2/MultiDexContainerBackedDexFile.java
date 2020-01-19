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

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.DexFile;
import org.jf.dexlib2.iface.MultiDexContainer;

public class MultiDexContainerBackedDexFile<T extends DexFile> implements DexFile {

	private final Set<? extends ClassDef> classes;
	private final Opcodes opcodes;

	public MultiDexContainerBackedDexFile(MultiDexContainer<T> container) throws IOException {
		List<String> entryNames = container.getDexEntryNames();
		if (entryNames.size() == 1) {
			String entryName = entryNames.get(0);
			T entryDex = container.getEntry(entryName).getDexFile();
			classes = Collections.unmodifiableSet(entryDex.getClasses());
			opcodes = entryDex.getOpcodes();
		} else {
			LinkedHashSet<ClassDef> accumulatedClasses = new LinkedHashSet<>();
			Opcodes resolvedOpcodes = null;
			for (String entryName : entryNames) {
				T entryDex = container.getEntry(entryName).getDexFile();
				for (ClassDef entryClass : entryDex.getClasses()) {
					if (!accumulatedClasses.add(entryClass)) throw new DuplicateTypeException(entryClass.getType());
				}
				resolvedOpcodes = OpcodeUtils.getNewestOpcodes(resolvedOpcodes, entryDex.getOpcodes(), true);
			}
			classes = Collections.unmodifiableSet(accumulatedClasses);
			opcodes = resolvedOpcodes;
		}
	}

	@Override
	public Set<? extends ClassDef> getClasses() {
		return classes;
	}

	@Override
	public Opcodes getOpcodes() {
		return opcodes;
	}

}
