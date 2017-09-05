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
package org.eclipse.emf.compare.uml2.tests;

/**
 * An enumeration of the kinds of additional resources to include in a comparison of some starting resources.
 *
 * @author Christian W. Damus
 */
public enum AdditionalResourcesKind {
	/** Include no referenced resources. */
	NONE,
	/**
	 * Include resources referenced (transitively) by the starting resources that are local (stored in the
	 * same location as the starting resource).
	 */
	REFERENCED_LOCAL,
	/**
	 * Includes all resources referenced (transitively) by the starting resources, regardless of whether they
	 * are stored locally.
	 */
	REFERENCED_ALL;

	public boolean isLocal() {
		// The NONE kind is effectively local
		return (this == NONE) || (this == AdditionalResourcesKind.REFERENCED_LOCAL);
	}
}
