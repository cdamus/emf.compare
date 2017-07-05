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
import org.eclipse.emf.ecore.util.EcoreUtil;

/**
 * The core protocol for elements of a <em>Façade Model</em>.
 *
 * @author Christian W. Damus
 */
public interface FacadeObject extends EObject {

	/**
	 * Obtains the model element underlying this façade, with which it is synchronized.
	 * 
	 * @return the underlying model element (never {@code null})
	 */
	EObject getUnderlyingElement();

	/**
	 * Obtains my associated façade adapter, if any.
	 * 
	 * @return my façade adapter or {@code null}
	 */
	default FacadeAdapter getFacadeAdapter() {
		return (FacadeAdapter)EcoreUtil.getExistingAdapter(this, FacadeObject.class);
	}
}
