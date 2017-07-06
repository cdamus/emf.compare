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

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Utility operations on {@link Optional}s that should have been provided by that API.
 *
 * @author Christian W. Damus
 * @since 3.5
 */
public final class Optionals {

	/**
	 * Not instantiable by clients.
	 */
	private Optionals() {
		super();
	}

	/**
	 * Apply an action on the value of an {@code optional} if it is present, else run an alternative action if
	 * it isn't.
	 * 
	 * @param optional
	 *            an optional
	 * @param ifPresent
	 *            consumer of its value if there is one
	 * @param elseNot
	 *            action to run in case the {@code optional} is absent
	 * @param <T>
	 *            the optional type
	 */
	public static <T> void ifPresentElse(Optional<T> optional, Consumer<? super T> ifPresent,
			Runnable elseNot) {

		optional.ifPresent(ifPresent);
		if (!optional.isPresent()) {
			elseNot.run();
		}
	}

}
