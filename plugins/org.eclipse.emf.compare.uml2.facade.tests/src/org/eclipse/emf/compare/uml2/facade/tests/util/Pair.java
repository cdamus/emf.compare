/*******************************************************************************
 * Copyright (c) 2017 Christian W. Damus and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Christian W. Damus - initial API and implementation
 *******************************************************************************/
package org.eclipse.emf.compare.uml2.facade.tests.util;

import java.util.function.Supplier;

/**
 * A pair of things.
 *
 * @author Christian W. Damus
 */
public final class Pair<T, U> {
	private final T first;

	private final U second;

	/**
	 * Initializes me.
	 * 
	 * @param first
	 *            the first element
	 * @param second
	 *            the second element
	 */
	private Pair(T first, U second) {
		super();

		this.first = first;
		this.second = second;
	}

	/**
	 * Pair two things.
	 * 
	 * @param first
	 *            a thing
	 * @param second
	 *            another thing
	 * @return the pair of them
	 */
	public static <T, U> Pair<T, U> of(T first, U second) {
		return new Pair<>(first, second);
	}

	/**
	 * Supply pairs of things.
	 * 
	 * @param first
	 *            a supply of things
	 * @param second
	 *            a supply of other things
	 * @return a supply of pairs of the things
	 */
	public static <T, U> Supplier<Pair<T, U>> supplier(Supplier<? extends T> first,
			Supplier<? extends U> second) {

		return () -> new Pair<>(first.get(), second.get());
	}

	/**
	 * Obtains the first thing.
	 * 
	 * @return the first
	 */
	public T first() {
		return first;
	}

	/**
	 * Obtains the second thing.
	 * 
	 * @return the second
	 */
	public U second() {
		return second;
	}
}
