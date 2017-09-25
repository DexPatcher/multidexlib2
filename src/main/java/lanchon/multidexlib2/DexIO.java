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
import org.jf.dexlib2.writer.io.DexDataStore;
import org.jf.dexlib2.writer.io.FileDataStore;
import org.jf.dexlib2.writer.pool.DexPool;

public class DexIO {

	public static final int DEFAULT_MAX_DEX_POOL_SIZE = 0x10000;

	public interface Logger {
		void log(File file, String entryName, int typeCount);
	}

	private DexIO() {}

	// Single-Threaded Write

	static void writeRawDexSingleThread(DexDataStore dataStore, DexFile dexFile, int maxDexPoolSize,
			DexIO.Logger logger, File file) throws IOException {
		Set<? extends ClassDef> classes = dexFile.getClasses();
		Iterator<? extends ClassDef> classIterator = classes.iterator();
		DexPool dexPool = new DexPool(dexFile.getOpcodes());
		int classCount = 0;
		while (classIterator.hasNext()) {
			ClassDef classDef = classIterator.next();
			dexPool.internClass(classDef);
			if (getDexPoolOverflow(dexPool, maxDexPoolSize)) {
				handleDexPoolOverflow(classDef, classCount, classes.size());
				throw new AssertionError("unexpected type count");
			}
			classCount++;
		}
		if (logger != null) logger.log(file, SingletonDexContainer.UNDEFINED_ENTRY_NAME, classCount);
		dexPool.writeTo(dataStore);
	}

	static void writeMultiDexDirectorySingleThread(boolean multiDex, File directory, DexFileNameIterator nameIterator,
			DexFile dexFile, int minMainDexClassCount, boolean minimalMainDex, int maxDexPoolSize, DexIO.Logger logger)
			throws IOException {
		Set<? extends ClassDef> classes = dexFile.getClasses();
		if (!multiDex) {
			minMainDexClassCount = classes.size();
			minimalMainDex = false;
		}
		Object lock = new Object();
		synchronized (lock) {       // avoid multiple synchronizations in single-threaded mode
			writeMultiDexDirectoryCommon(directory, nameIterator, Iterators.peekingIterator(classes.iterator()),
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
					writeMultiDexDirectoryCommon(directory, nameIterator, batchedIterator, 0, false,
							dexFile.getOpcodes(), maxDexPoolSize, logger, lock);
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

	private static void writeMultiDexDirectoryCommon(File directory, DexFileNameIterator nameIterator,
			PeekingIterator<? extends ClassDef> classIterator, int minMainDexClassCount, boolean minimalMainDex,
			Opcodes opcodes, int maxDexPoolSize, DexIO.Logger logger, Object lock) throws IOException {
		do {
			DexPool dexPool = new DexPool(opcodes);
			int fileClassCount = 0;
			while (classIterator.hasNext()) {
				if (minimalMainDex && fileClassCount >= minMainDexClassCount) break;
				ClassDef classDef = classIterator.peek();
				dexPool.mark();
				dexPool.internClass(classDef);
				if (getDexPoolOverflow(dexPool, maxDexPoolSize)) {
					handleDexPoolOverflow(classDef, fileClassCount, minMainDexClassCount);
					dexPool.reset();
					break;
				}
				classIterator.next();
				fileClassCount++;
			}
			File file;
			synchronized (lock) {
				String name = nameIterator.next();
				file = new File(directory, name);
				if (logger != null) logger.log(directory, name, fileClassCount);
				if (classIterator instanceof BatchedIterator) ((BatchedIterator) classIterator).preloadBatch();
			}
			dexPool.writeTo(new FileDataStore(file));
			minMainDexClassCount = 0;
			minimalMainDex = false;
		} while (classIterator.hasNext());
	}

	private static boolean getDexPoolOverflow(DexPool dexPool, int maxDexPoolSize) {
		return
				dexPool.typeSection.getItemCount() > maxDexPoolSize ||
				//dexPool.protoSection.getItemCount() > maxDexPoolSize ||
				dexPool.fieldSection.getItemCount() > maxDexPoolSize ||
				dexPool.methodSection.getItemCount() > maxDexPoolSize ||
				//dexPool.classSection.getItemCount() > maxDexPoolSize ||
				false;
	}

	private static void handleDexPoolOverflow(ClassDef classDef, int classCount, int minClassCount) {
		if (classCount < minClassCount) throw new DexPoolOverflowException(
				"Dex pool overflowed while writing type " + (classCount + 1) + " of " + minClassCount);
		if (classCount == 0) throw new DexPoolOverflowException(
				"Type too big for dex pool: " + classDef.getType());
	}

}
