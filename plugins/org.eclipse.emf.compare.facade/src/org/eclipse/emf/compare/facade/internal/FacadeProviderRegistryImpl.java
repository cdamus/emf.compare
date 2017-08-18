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
package org.eclipse.emf.compare.facade.internal;

import static java.util.Comparator.comparing;

import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.emf.compare.facade.IFacadeProvider;
import org.eclipse.emf.compare.facade.IFacadeProvider.Factory;
import org.eclipse.emf.compare.scope.IComparisonScope;

/**
 * Default implementation of a façade provider registry.
 *
 * @author Christian W. Damus
 */
public class FacadeProviderRegistryImpl implements IFacadeProvider.Factory.Registry {

	/** The registered façade provider factories. */
	private final Map<String, IFacadeProvider.Factory> factories = Maps.newConcurrentMap();

	/**
	 * Initializes me.
	 */
	public FacadeProviderRegistryImpl() {
		super();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<IFacadeProvider.Factory> getFacadeProviderFactories(IComparisonScope scope) {
		return factories.values().stream() //
				.filter(f -> f.isFacadeProviderFactoryFor(scope))
				.sorted(comparing(IFacadeProvider.Factory::getRanking).reversed())
				.collect(Collectors.toList());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Factory add(IFacadeProvider.Factory facadeProviderFactory) {
		return factories.put(facadeProviderFactory.getClass().getName(), facadeProviderFactory);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Factory remove(String className) {
		return factories.remove(className);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void clear() {
		factories.clear();
	}

	/**
	 * Creates a façade provider registry populated with factories, if any, that are suitable defaults for
	 * stand-alone execution.
	 * 
	 * @return a default stand-alone façade-provider registry
	 */
	public static FacadeProviderRegistryImpl createStandaloneInstance() {
		return new FacadeProviderRegistryImpl();
	}
}
