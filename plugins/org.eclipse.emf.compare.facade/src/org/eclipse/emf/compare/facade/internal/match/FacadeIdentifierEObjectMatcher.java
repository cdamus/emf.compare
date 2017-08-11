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
package org.eclipse.emf.compare.facade.internal.match;

import org.eclipse.emf.compare.facade.FacadeObject;
import org.eclipse.emf.compare.match.eobject.IEObjectMatcher;
import org.eclipse.emf.compare.match.eobject.IdentifierEObjectMatcher;
import org.eclipse.emf.ecore.EObject;

/**
 * A specialized identifier-based object matcher for façades, that gives them exterinsic identifiers.
 *
 * @author Christian W. Damus
 */
public class FacadeIdentifierEObjectMatcher extends IdentifierEObjectMatcher {

	/**
	 * Initializes me.
	 */
	public FacadeIdentifierEObjectMatcher() {
		super(new FacadeIDFunction());
	}

	/**
	 * Initializes me with a delegate for structural matching of objects that have no identifiers.
	 * 
	 * @param delegateWhenNoID
	 *            my structural matching delegate
	 */
	public FacadeIdentifierEObjectMatcher(IEObjectMatcher delegateWhenNoID) {
		super(delegateWhenNoID, new FacadeIDFunction());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected EObject getParentEObject(EObject eObject) {
		EObject result = super.getParentEObject(eObject);

		if ((result == null) && (eObject instanceof FacadeObject)) {
			// If it's a façade for an object that has a parent, then that
			// parent should be the façade's parent for the purposes of
			// organizing the match hierarchy
			EObject underlying = ((FacadeObject)eObject).getUnderlyingElement();
			if (underlying != null) {
				result = getParentEObject(underlying);
			}
		}

		return result;
	}

	//
	// Nested types
	//

	/**
	 * An ID function that can extract the ID of the object underlying a façade in case the façade has no ID
	 * of its own.
	 *
	 * @author Christian W. Damus
	 */
	protected static class FacadeIDFunction extends DefaultIDFunction {
		/**
		 * Initializes me.
		 */
		protected FacadeIDFunction() {
			super();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String apply(EObject eObject) {
			String result = super.apply(eObject);

			if ((result == null) && (eObject instanceof FacadeObject)) {
				// Get the ID of the underlying object
				result = apply(((FacadeObject)eObject).getUnderlyingElement());
			}

			return result;
		}
	}
}
