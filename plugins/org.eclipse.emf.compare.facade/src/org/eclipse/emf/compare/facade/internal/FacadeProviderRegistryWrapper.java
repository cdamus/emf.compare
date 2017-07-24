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

import static com.google.common.base.Predicates.in;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.transform;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.emf.compare.facade.IFacadeProvider;
import org.eclipse.emf.compare.rcp.EMFCompareRCPPlugin;
import org.eclipse.emf.compare.rcp.internal.extension.IItemDescriptor;
import org.eclipse.emf.compare.rcp.internal.extension.IItemRegistry;
import org.eclipse.emf.compare.rcp.internal.extension.impl.ItemUtil;
import org.eclipse.emf.compare.rcp.internal.extension.impl.WrapperItemDescriptor;
import org.eclipse.emf.compare.rcp.internal.preferences.EMFComparePreferences;
import org.eclipse.emf.compare.scope.IComparisonScope;

/**
 * An API wrapper for the extension-based façade provider registry.
 * 
 * @author Christian W. Damus
 */
public class FacadeProviderRegistryWrapper implements IFacadeProvider.Factory.Registry {

	/** Instance of the registry that need to be wrapped. */
	private IItemRegistry<IFacadeProvider.Factory> registry;

	/**
	 * Initializes me.
	 * 
	 * @param registry
	 *            the extension-based registry that I wrap
	 */
	public FacadeProviderRegistryWrapper(IItemRegistry<IFacadeProvider.Factory> registry) {
		super();

		this.registry = registry;
	}

	/**
	 * Queries the descriptors of currently enabled (by user preferences) façade provider factories.
	 * 
	 * @return the factories
	 */
	private Iterable<IItemDescriptor<IFacadeProvider.Factory>> getEnabledFactories() {
		return Iterables.filter(registry.getItemDescriptors(), not(in(getDisabledFacadeProviders())));
	}

	/**
	 * {@inheritDoc}
	 */
	public IFacadeProvider.Factory getHighestRankingFacadeProviderFactory(IComparisonScope scope) {
		IItemDescriptor<IFacadeProvider.Factory> highest = null;
		IFacadeProvider.Factory result = null;

		for (IItemDescriptor<IFacadeProvider.Factory> next : getEnabledFactories()) {
			if ((highest == null) || (highest.getRank() > highest.getRank())) {
				IFacadeProvider.Factory factory = next.getItem();
				if (factory.isFacadeProviderFactoryFor(scope)) {
					highest = next;
					result = factory;
				}
			}
		}

		if (result == null) {
			result = IFacadeProvider.Factory.NULL_FACTORY;
		}

		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<IFacadeProvider.Factory> getFacadeProviderFactories(IComparisonScope scope) {
		Iterable<IFacadeProvider.Factory> result = filter(
				transform(getEnabledFactories(), IItemDescriptor::getItem),
				factory -> factory.isFacadeProviderFactoryFor(scope));
		return Lists.newArrayList(result);
	}

	/**
	 * {@inheritDoc}
	 */
	public IFacadeProvider.Factory add(IFacadeProvider.Factory factory) {
		Preconditions.checkNotNull(factory);

		IItemDescriptor<IFacadeProvider.Factory> previous = registry
				.add(new WrapperItemDescriptor<IFacadeProvider.Factory>("", "", //$NON-NLS-1$ //$NON-NLS-2$
						factory.getRanking(), factory.getClass().getName(), factory));

		if (previous != null) {
			return previous.getItem();
		} else {
			return null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public IFacadeProvider.Factory remove(String className) {
		Preconditions.checkNotNull(className);

		IItemDescriptor<IFacadeProvider.Factory> previous = registry.remove(className);
		if (previous != null) {
			return previous.getItem();
		} else {
			return null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void clear() {
		registry.clear();
	}

	/**
	 * Retrieves the extension-point-registered façade providers that are disabled by the user preferences.
	 * 
	 * @return the disabled façade providers
	 */
	private Collection<IItemDescriptor<IFacadeProvider.Factory>> getDisabledFacadeProviders() {
		Collection<IItemDescriptor<IFacadeProvider.Factory>> result = ItemUtil.getItemsDescriptor(registry,
				EMFCompareRCPPlugin.PLUGIN_ID, EMFComparePreferences.DISABLED_FACADE_PROVIDER);

		if (result == null) {
			result = Collections.emptyList();
		}

		return result;
	}
}
