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
package org.eclipse.emf.compare.facade;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.impl.MinimalEObjectImpl.Container;

/**
 * This is the {@code FacadeObjectImpl} type. Enjoy.
 *
 * @author Christian W. Damus
 */
public class FacadeObjectImpl extends Container implements FacadeObject {

	/**
	 * {@inheritDoc}
	 */
	public EObject getUnderlyingElement() {
		EObject result = null;

		FacadeAdapter adapter = getFacadeAdapter();
		if (adapter != null) {
			result = adapter.getUnderlyingElement();
		}

		return result;
	}

}
