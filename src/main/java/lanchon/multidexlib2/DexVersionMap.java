/*
 * DexPatcher - Copyright 2015-2017 Rodrigo Balerdi
 * (GNU General Public License version 3 or later)
 *
 * DexPatcher is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 */

package lanchon.multidexlib2;

import org.jf.dexlib2.VersionMap;
import org.jf.dexlib2.dexbacked.raw.HeaderItem;

public class DexVersionMap {

	//private static final int DEVEL_API_LEVEL = 10000;   // Build.VERSION_CODES.CUR_DEVELOPMENT

	//private static final int MAX_DEX_VERSION = getDexVersionFromApiLevel(DEVEL_API_LEVEL);
	//private static final int MAX_API_LEVEL = getHighestApiLevelFromDexVersion(MAX_DEX_VERSION);

	/*
	public static boolean isSupportedApiLevel(int apiLevel) {
		return apiLevel <= MAX_API_LEVEL;
	}
	*/

	public static int getDexVersionFromApiLevel(int apiLevel) {
		return HeaderItem.getVersion(HeaderItem.getMagicForApi(apiLevel), 0);
	}

	/*
	public static boolean isSupportedDexVersion(int dexVersion) {
		try {
			VersionMap.mapDexVersionToApi(dexVersion);
			return true;
		} catch (RuntimeException e) {
			return false;
		}
	}
	*/

	public static int getHighestApiLevelFromDexVersion(int dexVersion) {
		//if (dexVersion > MAX_DEX_VERSION) return DEVEL_API_LEVEL;
		return VersionMap.mapDexVersionToApi(dexVersion);
	}

	private DexVersionMap() {}

}
