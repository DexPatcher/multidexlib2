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

import java.util.ArrayDeque;
import java.util.Iterator;

import com.google.common.collect.PeekingIterator;
import com.google.common.collect.UnmodifiableIterator;

public class BatchedIterator<E> extends UnmodifiableIterator<E> implements PeekingIterator<E> {

	private final Iterator<? extends E> iterator;
	private final Object iteratorLock;
	private final int batchSize;
	private final ArrayDeque<E> batch;

	public BatchedIterator(Iterator<? extends E> iterator, Object iteratorLock, int batchSize) {
		if (batchSize < 1) throw new IllegalArgumentException("batchSize");
		this.iterator = iterator;
		this.iteratorLock = iteratorLock;
		this.batchSize = batchSize;
		batch = new ArrayDeque<>(batchSize);
		preloadBatch();
	}

	@Override
	public boolean hasNext() {
		return !batch.isEmpty();
	}

	@Override
	public E peek() {
		return batch.element();                     // element throws NoSuchElementException if batch is empty
	}

	@Override
	public E next() {
		E item = batch.remove();                    // remove throws NoSuchElementException if batch is empty
		if (batch.isEmpty()) preloadBatch();
		return item;
	}

	public void preloadBatch() {
		synchronized (iteratorLock) {
			for (int n = batchSize - batch.size(); n > 0; n--) {
				if (!iterator.hasNext()) break;
				batch.add(iterator.next());         // add throws NullPointerException if element is null
			}
		}
	}

}
