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
package org.eclipse.emf.compare.uml2.facade.tests.util;

import static org.junit.Assert.fail;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;

import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;

import org.eclipse.emf.compare.facade.IFacadeProvider;
import org.eclipse.emf.compare.uml2.facade.tests.j2ee.internal.providers.J2EEFacadeProvider;
import org.eclipse.emf.compare.uml2.facade.tests.opaqexpr.internal.providers.OpaqexprFacadeProvider;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

/**
 * A rule that configures a test to use dynamic proxy façades or not.
 *
 * @author Christian W. Damus
 */
@SuppressWarnings("boxing")
public final class DynamicProxiesRule extends TestWatcher implements BooleanSupplier {

	private static final List<Class<? extends IFacadeProvider>> FACADE_PROVIDER_CLASSES = ImmutableList
			.of(J2EEFacadeProvider.class, OpaqexprFacadeProvider.class);

	private final boolean useDynamicProxies;

	private Set<Class<? extends IFacadeProvider>> wasUseDynamicProxies = Sets.newIdentityHashSet();

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
	public boolean getAsBoolean() {
		return useDynamicProxies;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void starting(Description description) {
		// Record which providers were using dynamic proxies
		wasUseDynamicProxies.clear();
		FACADE_PROVIDER_CLASSES.stream() //
				.filter(cl -> safeInvokeStatic(cl, "getUseDynamicProxies")) //$NON-NLS-1$
				.forEach(wasUseDynamicProxies::add);

		// Set them all
		FACADE_PROVIDER_CLASSES
				.forEach(cl -> safeInvokeStatic(cl, "setUseDynamicProxies", useDynamicProxies)); //$NON-NLS-1$
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void finished(Description description) {
		FACADE_PROVIDER_CLASSES.forEach(
				cl -> safeInvokeStatic(cl, "setUseDynamicProxies", wasUseDynamicProxies.contains(cl))); //$NON-NLS-1$
		wasUseDynamicProxies.clear();
	}

	@SuppressWarnings("unchecked")
	<T> T safeInvokeStatic(Class<?> owner, String methodName, Object... arg) {
		return (T)Stream.of(owner.getDeclaredMethods())
				.filter(m -> methodName.equals(m.getName()) && (m.getParameterCount() == arg.length)
						&& Modifier.isStatic(m.getModifiers()))
				.findFirst() //
				.map(method -> {
					try {
						return method.invoke(null, arg);
					} catch (Exception e) {
						e.printStackTrace();
						fail(String.format("Failed to invoke %s: %s", method, e.getMessage())); //$NON-NLS-1$
						return null;
					}
				}).orElse(null);
	}
}
