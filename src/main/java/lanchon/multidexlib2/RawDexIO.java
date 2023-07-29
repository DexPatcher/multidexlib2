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

import com.google.common.io.ByteStreamsHack;
import com.google.common.io.Files;
import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.DexFile;
import com.android.tools.smali.dexlib2.util.DexUtil;
import com.android.tools.smali.dexlib2.writer.io.DexDataStore;
import com.android.tools.smali.dexlib2.writer.io.FileDataStore;

public class RawDexIO {

	private RawDexIO() {}

	// Read

	public static DexBackedDexFile readRawDexFile(File file, Opcodes opcodes, DexIO.Logger logger) throws IOException {
		DexBackedDexFile dexFile = readRawDexFile(file, opcodes);
		if (logger != null) {
			logger.log(file, SingletonDexContainer.UNDEFINED_ENTRY_NAME, dexFile.getClasses().size());
		}
		return dexFile;
	}

	public static DexBackedDexFile readRawDexFile(File file, Opcodes opcodes) throws IOException {
		/*
		try (InputStream inputStream = new FileInputStream(file)) {
			return readRawDexFile(inputStream, file.length(), opcodes);
		}
		*/
		//noinspection UnstableApiUsage
		byte[] buf = Files.toByteArray(file);
		return readRawDexFile(buf, 0, opcodes);
	}

	public static DexBackedDexFile readRawDexFile(InputStream inputStream, long expectedSize, Opcodes opcodes)
			throws IOException {
		// TODO: Remove hack when issue is fixed: https://github.com/google/guava/issues/2616
		//byte[] buf = ByteStreams.toByteArray(inputStream);
		byte[] buf = ByteStreamsHack.toByteArray(inputStream, expectedSize);
		return readRawDexFile(buf, 0, opcodes);
	}

	public static DexBackedDexFile readRawDexFile(byte[] buf, int offset, Opcodes opcodes) {
		// This method now relies on the automatic dex version handling implemented in dexlib2 since version 2.2.4.
		DexUtil.verifyDexHeader(buf, offset);
		return new DexBackedDexFile(opcodes, buf, offset);
	}

	// Write

	public static void writeRawDexFile(File file, DexFile dexFile, int maxDexPoolSize, DexIO.Logger logger)
			throws IOException {
		DexIO.writeRawDexSingleThread(new FileDataStore(file), dexFile, maxDexPoolSize, logger, file);
	}

	public static void writeRawDexFile(File file, DexFile dexFile, int maxDexPoolSize) throws IOException {
		DexIO.writeRawDexSingleThread(new FileDataStore(file), dexFile, maxDexPoolSize, null, null);
	}

	public static void writeRawDexFile(DexDataStore dataStore, DexFile dexFile, int maxDexPoolSize) throws IOException {
		DexIO.writeRawDexSingleThread(dataStore, dexFile, maxDexPoolSize, null, null);
	}

}
