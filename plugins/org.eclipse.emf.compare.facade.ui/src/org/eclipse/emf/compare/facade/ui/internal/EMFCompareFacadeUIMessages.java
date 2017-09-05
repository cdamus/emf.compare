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
package org.eclipse.emf.compare.facade.ui.internal;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Localized messages for the bundle.
 *
 * @author Christian W. Damus
 */
public final class EMFCompareFacadeUIMessages {
	/** The message bundle resource. */
	private static final String BUNDLE_NAME = "org.eclipse.emf.compare.facade.ui.internal.messages"; //$NON-NLS-1$

	/** The resource bundle loaded from the message bundle resource. */
	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);

	/** Not instantiable by clients. */
	private EMFCompareFacadeUIMessages() {
		super();
	}

	/**
	 * Obtains the local translation of the string for the given {@code key}.
	 * 
	 * @param key
	 *            the key to look up
	 * @return the localized message for the {@code key}
	 */
	public static String getString(String key) {
		try {
			return RESOURCE_BUNDLE.getString(key);
		} catch (MissingResourceException e) {
			return '!' + key + '!';
		}
	}
}
