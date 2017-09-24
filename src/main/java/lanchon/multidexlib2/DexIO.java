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

import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.DexFile;
import org.jf.dexlib2.writer.io.FileDataStore;
import org.jf.dexlib2.writer.pool.DexPool;

public class DexIO {

	public static final int DEFAULT_MAX_DEX_POOL_SIZE = 0x10000;

	public interface Logger {
		void log(File file, String entryName, int typeCount);
	}

	private DexIO() {}

	// Single-Threaded Write

	static void writeRawDexSingleThread(File file, DexFile dexFile, int maxDexPoolSize, DexIO.Logger logger)
			throws IOException {
		writeCommonSingleThread(false, file, null, SingletonDexContainer.UNDEFINED_ENTRY_NAME, file, dexFile, 0, false,
				maxDexPoolSize, logger);
	}

	static void writeMultiDexDirectorySingleThread(boolean multiDex, File directory, DexFileNameIterator nameIterator,
			DexFile dexFile, int minMainDexClassCount, boolean minimalMainDex, int maxDexPoolSize, DexIO.Logger logger)
			throws IOException {
		writeCommonSingleThread(multiDex, directory, nameIterator, null, null, dexFile, minMainDexClassCount,
				minimalMainDex, maxDexPoolSize, logger);
	}

	private static void writeCommonSingleThread(boolean multiDex, File base, DexFileNameIterator nameIterator,
			String currentName, File currentFile, DexFile dexFile, int minMainDexClassCount, boolean minimalMainDex,
			int maxDexPoolSize, DexIO.Logger logger) throws IOException {
		Set<? extends ClassDef> classes = dexFile.getClasses();
		if (!multiDex) {
			minMainDexClassCount = classes.size();
			minimalMainDex = false;
		}
		Object lock = new Object();
		synchronized (lock) {       // avoid multiple synchronizations in single-threaded mode
			writeCommon(base, nameIterator, currentName, currentFile, Iterators.peekingIterator(classes.iterator()),
					minMainDexClassCount, minimalMainDex, dexFile.getOpcodes(), maxDexPoolSize, logger, lock);
		}
	}

	// Multi-Threaded Write

	private static final int PER_THREAD_BATCH_SIZE = 100;

	static void writeMultiDexDirectoryMultiThread(int threadCount, final File directory,
			final DexFileNameIterator nameIterator, final DexFile dexFile, final int maxDexPoolSize,
			final DexIO.Logger logger) throws IOException {
		Iterator<? extends ClassDef> classIterator = dexFile.getClasses().iterator();
		final Object lock = new Object();
		List<Callable<Void>> callables = new ArrayList<>(threadCount);
		for (int i = 0; i < threadCount; i++) {
			final BatchedIterator<ClassDef> batchedIterator =
					new BatchedIterator<>(classIterator, lock, PER_THREAD_BATCH_SIZE);
			if (i != 0 && !batchedIterator.hasNext()) break;
			callables.add(new Callable<Void>() {
				@Override
				public Void call() throws IOException {
					writeCommon(directory, nameIterator, null, null, batchedIterator, 0, false, dexFile.getOpcodes(),
							maxDexPoolSize, logger, lock);
					return null;
				}
			});
		}
		ExecutorService service = Executors.newFixedThreadPool(threadCount);
		try {
			List<Future<Void>> futures = service.invokeAll(callables);
			for (Future<Void> future : futures) {
				try {
					future.get();
				} catch (ExecutionException e) {
					Throwable c = e.getCause();
					if (c instanceof IOException) throw (IOException) c;
					if (c instanceof RuntimeException) throw (RuntimeException) c;
					if (c instanceof Error) throw (Error) c;
					throw new UndeclaredThrowableException(c);
				}
			}
		} catch (InterruptedException e) {
			InterruptedIOException ioe = new InterruptedIOException();
			ioe.initCause(e);
			throw ioe;
		} finally {
			service.shutdown();
		}
	}

	// Common Code

	private static void writeCommon(File base, DexFileNameIterator nameIterator, String currentName, File currentFile,
			PeekingIterator<? extends ClassDef> classIterator, int minMainDexClassCount, boolean minimalMainDex,
			Opcodes opcodes, int maxDexPoolSize, DexIO.Logger logger, Object lock) throws IOException {
		do {
			DexPool dexPool = new DexPool(opcodes);
			int fileClassCount = 0;
			while (classIterator.hasNext()) {
				if (minimalMainDex && fileClassCount >= minMainDexClassCount) break;
				ClassDef classDef = classIterator.peek();
				if (!internClass(dexPool, classDef, maxDexPoolSize)) {
					checkDexPoolOverflow(classDef, fileClassCount, minMainDexClassCount);
					break;
				}
				classIterator.next();
				fileClassCount++;
			}
			synchronized (lock) {
				if (currentFile == null) {
					currentName = nameIterator.next();
					currentFile = new File(base, currentName);
				}
				if (logger != null) logger.log(base, currentName, fileClassCount);
				if (classIterator instanceof BatchedIterator) ((BatchedIterator) classIterator).preloadBatch();
			}
			dexPool.writeTo(new FileDataStore(currentFile));
			currentFile = null;
			minMainDexClassCount = 0;
			minimalMainDex = false;
		} while (classIterator.hasNext());
	}

	private static boolean internClass(DexPool dexPool, ClassDef classDef, int maxDexPoolSize) {
		dexPool.mark();
		dexPool.internClass(classDef);
		if (
				dexPool.typeSection.getItemCount() > maxDexPoolSize ||
				//dexPool.protoSection.getItemCount() > maxDexPoolSize ||
				dexPool.fieldSection.getItemCount() > maxDexPoolSize ||
				dexPool.methodSection.getItemCount() > maxDexPoolSize ||
				//dexPool.classSection.getItemCount() > maxDexPoolSize ||
				false
		) {
			dexPool.reset();    // roll back interning on pool overflow
			return false;
		}
		return true;
	}

	private static void checkDexPoolOverflow(ClassDef classDef, int classCount, int minClassCount) {
		if (classCount < minClassCount) throw new DexPoolOverflowException(
				"Dex pool overflowed while writing type " + (classCount + 1) + " of " + minClassCount);
		if (classCount == 0) throw new DexPoolOverflowException(
				"Type too big for dex pool: " + classDef.getType());
	}

}
