/*
 * Copyright (c) 2017 Christian W. Damus and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Christian W. Damus - initial API and implementation
 *
 */
package org.eclipse.emf.compare.utils;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Iterators;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.eclipse.emf.common.util.TreeIterator;

/**
 * Static utility methods for working with {@link TreeIterator}s as for {@link Iterator}s in the Guava
 * {@link Iterators} API.
 *
 * @author Christian W. Damus
 */
public final class TreeIterators {

	/** The shared empty iterator instance. */
	private static final TreeIterator<Object> EMPTY_ITERATOR = new Empty<>();

	/**
	 * Not instantiable by clients.
	 */
	private TreeIterators() {
		super();
	}

	/**
	 * Obtains an empty tree iterator.
	 * 
	 * @return an empty tree iterator if either argument is {@code null}
	 * @param <E>
	 *            the iterator element type
	 */
	@SuppressWarnings("unchecked")
	public static <E> TreeIterator<E> emptyIterator() {
		return (TreeIterator<E>)EMPTY_ITERATOR;
	}

	/**
	 * Obtains an immutable filtering view of a tree {@code iterator}.
	 * 
	 * @param iterator
	 *            the iterator to filter
	 * @param filter
	 *            the filter
	 * @return the filtering tree iterator
	 * @throws NullPointerException
	 *             if either argument is {@code null}
	 * @param <E>
	 *            the iterator element type
	 */
	public static <E> TreeIterator<E> filter(TreeIterator<E> iterator, Predicate<? super E> filter) {
		Preconditions.checkNotNull(iterator, "iterator"); //$NON-NLS-1$
		Preconditions.checkNotNull(filter, "filter"); //$NON-NLS-1$

		return new Filtering<>(iterator, filter);
	}

	//
	// Nested types
	//

	/**
	 * An empty tree iterator.
	 * 
	 * @param <E>
	 *            the iterator element type
	 */
	private static final class Empty<E> implements TreeIterator<E> {
		/**
		 * Initializes me.
		 */
		Empty() {
			super();
		}

		/**
		 * {@inheritDoc}
		 */
		public boolean hasNext() {
			return false;
		}

		/**
		 * {@inheritDoc}
		 */
		public E next() {
			throw new NoSuchElementException();
		}

		/**
		 * {@inheritDoc}
		 */
		public void remove() {
			throw new UnsupportedOperationException();
		}

		/**
		 * {@inheritDoc}
		 */
		public void prune() {
			// Prune is never expected to throw, so this is just a no-op
		}
	}

	/**
	 * An immutable filtering view of a tree {@code iterator}.
	 * 
	 * @param <E>
	 *            the iterator element type
	 */
	private static final class Filtering<E> extends AbstractIterator<E> implements TreeIterator<E> {
		/** The backing iterator. */
		private final TreeIterator<E> delegate;

		/** The filter condition. */
		private final Predicate<? super E> filter;

		/**
		 * Initializes me.
		 * 
		 * @param iterator
		 *            the iterator to filter
		 * @param filter
		 *            the filter
		 */
		Filtering(TreeIterator<E> iterator, Predicate<? super E> filter) {
			super();

			this.delegate = iterator;
			this.filter = filter;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		protected E computeNext() {
			E result;

			out: if (delegate.hasNext()) {
				do {
					// Be optimistic about this
					result = delegate.next();
					if (filter.apply(result)) {
						break out;
					}
				} while (delegate.hasNext());

				result = endOfData();
			} else {
				result = endOfData();
			}

			return result;
		}

		/**
		 * {@inheritDoc}
		 */
		public void prune() {
			delegate.prune();
		}
	}
}
