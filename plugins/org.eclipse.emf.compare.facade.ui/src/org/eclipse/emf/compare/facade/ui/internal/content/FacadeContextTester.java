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
package org.eclipse.emf.compare.facade.ui.internal.content;

import java.util.Map;

import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.adapterfactory.context.AbstractContextTester;
import org.eclipse.emf.compare.facade.IFacadeProvider;
import org.eclipse.emf.compare.facade.util.FacadeUtil;

/**
 * Tester for the <em>Façade Provider</em> context.
 *
 * @author Christian W. Damus
 */
public class FacadeContextTester extends AbstractContextTester {

	/**
	 * Initializes me.
	 */
	public FacadeContextTester() {
		super();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean apply(Map<Object, Object> context) {
		boolean result = false;

		Comparison comparison = getComparison(context);
		if (comparison != null) {
			IFacadeProvider facadeProvider = FacadeUtil.getFacadeProvider(comparison);
			result = (facadeProvider != null) && (facadeProvider != IFacadeProvider.NULL_PROVIDER);
		}

		return result;
	}

}
