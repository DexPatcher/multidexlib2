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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;

public class ZipFileDexContainer extends AbstractMultiDexContainer<DexBackedDexFile> {

	public static boolean isZipFile(File zip) {
		if (!zip.isFile()) return false;
		try {
			ZipFile zipFile = new ZipFile(zip);
			zipFile.close();
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	public ZipFileDexContainer(File zip, DexFileNamer namer, Opcodes opcodes) throws IOException {
		Map<String, DexEntry<DexBackedDexFile>> entryMap = new TreeMap<>(new DexFileNameComparator(namer));
		try (ZipFile zipFile = new ZipFile(zip)) {
			Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();
			while (zipEntries.hasMoreElements()) {
				ZipEntry zipEntry = zipEntries.nextElement();
				String entryName = zipEntry.getName();
				if (namer.isValidName(entryName)) {
					DexBackedDexFile dexFile;
					try (InputStream inputStream = zipFile.getInputStream(zipEntry)) {
						dexFile = RawDexIO.readRawDexFile(inputStream, zipEntry.getSize(), opcodes);
					}
					DexEntry<DexBackedDexFile> entry = new BasicDexEntry<>(this, entryName, dexFile);
					if (entryMap.put(entryName, entry) != null) throw duplicateEntryName(entryName);
				}
			}
		}
		initialize(entryMap);
	}

}
