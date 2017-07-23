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

import org.eclipse.emf.common.util.Monitor;
import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.facade.IFacadeProvider;
import org.eclipse.emf.compare.facade.internal.EMFCompareFacadePlugin;
import org.eclipse.emf.compare.match.DefaultComparisonFactory;
import org.eclipse.emf.compare.match.DefaultEqualityHelperFactory;
import org.eclipse.emf.compare.match.DefaultMatchEngine;
import org.eclipse.emf.compare.match.IComparisonFactory;
import org.eclipse.emf.compare.match.IMatchEngine;
import org.eclipse.emf.compare.match.eobject.IEObjectMatcher;
import org.eclipse.emf.compare.match.resource.IResourceMatcher;
import org.eclipse.emf.compare.match.resource.StrategyResourceMatcher;
import org.eclipse.emf.compare.rcp.EMFCompareRCPPlugin;
import org.eclipse.emf.compare.rcp.internal.match.DefaultRCPMatchEngineFactory;
import org.eclipse.emf.compare.scope.IComparisonScope;

/**
 * A match engine that matches façades for comparison instead of the underlying model elements, where façades
 * are available.
 *
 * @author Christian W. Damus
 */
public class FacadeMatchEngine extends DefaultMatchEngine {

	/** My registry of façade provider factories. */
	private final IFacadeProvider.Factory.Registry facadeProviderRegistry;

	/**
	 * Initializes me with my object matcher, comparison factory, and registry of façade providers.
	 * 
	 * @param matcher
	 *            my object matcher
	 * @param comparisonFactory
	 *            my comparison factory
	 * @param facadeProviderRegistry
	 *            registry of façade provider factories
	 */
	public FacadeMatchEngine(IEObjectMatcher matcher, IComparisonFactory comparisonFactory,
			IFacadeProvider.Factory.Registry facadeProviderRegistry) {
		super(matcher, comparisonFactory);

		this.facadeProviderRegistry = facadeProviderRegistry;
	}

	/**
	 * Initializes me with my object matcher, resource matcher, comparison factory, and registry of façade
	 * providers.
	 * 
	 * @param eObjectMatcher
	 *            my object matcher
	 * @param resourceMatcher
	 *            my resource matcher
	 * @param comparisonFactory
	 *            my comparison factory
	 * @param facadeProviderRegistry
	 *            registry of façade provider factories
	 */
	public FacadeMatchEngine(IEObjectMatcher eObjectMatcher, IResourceMatcher resourceMatcher,
			IComparisonFactory comparisonFactory, IFacadeProvider.Factory.Registry facadeProviderRegistry) {
		super(eObjectMatcher, resourceMatcher, comparisonFactory);

		this.facadeProviderRegistry = facadeProviderRegistry;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Comparison match(IComparisonScope scope, Monitor monitor) {
		IComparisonScope scopeToMatch = scope;
		IFacadeProvider facadeProvider = facadeProviderRegistry.getFacadeProviderFactory(scope)
				.getFacadeProvider();

		if (facadeProvider != IFacadeProvider.NULL_PROVIDER) {
			scopeToMatch = wrap(scope, facadeProvider);
		} // else there are no façades

		return super.match(scopeToMatch, monitor);
	}

	/**
	 * Wraps a comparison scope as a scope that supplies façades (where possible).
	 * 
	 * @param delegate
	 *            a comparison scope to wrap
	 * @param facadeProvider
	 *            the provider of façades for the {@code scope}
	 * @return the wrapping scope
	 */
	protected IComparisonScope wrap(IComparisonScope delegate, IFacadeProvider facadeProvider) {

		return new FacadeComparisonScope(facadeProvider, delegate);
	}

	//
	// Nested types
	//

	/**
	 * The factory for the {@link FacadeMatchEngine}.
	 *
	 * @author Christian W. Damus
	 */
	public static class Factory extends DefaultRCPMatchEngineFactory {
		/**
		 * Initializes me.
		 */
		public Factory() {
			super();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public IMatchEngine getMatchEngine() {
			IComparisonFactory comparisonFactory = new DefaultComparisonFactory(
					new DefaultEqualityHelperFactory());
			IEObjectMatcher eObjectMatcher = createDefaultEObjectMatcher(getUseIdentifierValue(),
					EMFCompareRCPPlugin.getDefault().getWeightProviderRegistry());
			IResourceMatcher resourceMatcher = new StrategyResourceMatcher();

			IMatchEngine result = new FacadeMatchEngine(eObjectMatcher, resourceMatcher, comparisonFactory,
					EMFCompareFacadePlugin.getDefault().getFacadeProviderRegistry());

			return result;
		}
	}
}
