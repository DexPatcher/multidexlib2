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

import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.VersionMap;

public class OpcodeUtils {

	public static Opcodes getOpcodesFromDexVersion(int dexVersion) {
		//return Opcodes.forApi(DexVersionMap.getHighestApiLevelFromDexVersion(dexVersion));
		//return Opcodes.forApi(VersionMap.mapDexVersionToApi(dexVersion));
		return Opcodes.forDexVersion(dexVersion);
	}

	public static int getDexVersionFromOpcodes(Opcodes opcodes) {
		if (opcodes.api == VersionMap.NO_VERSION) throw undefinedApiLevel();
		//return DexVersionMap.getDexVersionFromApiLevel(opcodes.api);
		//return HeaderItem.getVersion(HeaderItem.getMagicForApi(opcodes.api), 0);
		return VersionMap.mapApiToDexVersion(opcodes.api);
	}

	public static Opcodes getNewestOpcodes(Opcodes o1, Opcodes o2, boolean nullable) {
		if (nullable) {
			if (o1 == null) return o2;
			if (o2 == null) return o1;
		}
		if (o1.api == VersionMap.NO_VERSION || o2.api == VersionMap.NO_VERSION) throw undefinedApiLevel();
		return o1.api >= o2.api ? o1 : o2;
	}

	/*
	public static <T extends DexFile> Opcodes getNewestOpcodes(MultiDexContainer<T> container) throws IOException {
		Opcodes opcodes = null;
		for (String entryName : container.getDexEntryNames()) {
			opcodes = OpcodeUtils.getNewestOpcodes(opcodes, container.getEntry(entryName).getOpcodes(), true);
		}
		return opcodes;
	}
	*/

	private static IllegalArgumentException undefinedApiLevel() {
		return new IllegalArgumentException("Opcodes instance has undefined api level");
	}

	private OpcodeUtils() {}

}
