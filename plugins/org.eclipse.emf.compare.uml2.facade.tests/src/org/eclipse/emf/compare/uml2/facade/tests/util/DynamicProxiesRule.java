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

import java.util.function.BooleanSupplier;

import org.eclipse.emf.compare.facade.internal.EMFCompareFacadePlugin;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

/**
 * A rule that configures a test to use dynamic proxy façades or not.
 *
 * @author Christian W. Damus
 */
@SuppressWarnings("restriction")
public final class DynamicProxiesRule extends TestWatcher implements BooleanSupplier {

	private final boolean useDynamicProxies;

	private boolean wasUseDynamicProxies;

	/**
	 * Initializes me.
	 * 
	 * @param useDynamicProxies
	 *            whether to use dynamic proxies
	 */
	public DynamicProxiesRule(boolean useDynamicProxies) {
		super();

		this.useDynamicProxies = useDynamicProxies;
	}

	/**
	 * Queries whether we are using dynamic proxies in the current test.
	 * 
	 * @return whether we are using dynamic proxies
	 */
	@Override
	public boolean getAsBoolean() {
		return useDynamicProxies;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void starting(Description description) {
		wasUseDynamicProxies = EMFCompareFacadePlugin.isUseDynamicProxies();
		EMFCompareFacadePlugin.setUseDynamicProxies(useDynamicProxies);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void finished(Description description) {
		EMFCompareFacadePlugin.setUseDynamicProxies(wasUseDynamicProxies);
	}
}
