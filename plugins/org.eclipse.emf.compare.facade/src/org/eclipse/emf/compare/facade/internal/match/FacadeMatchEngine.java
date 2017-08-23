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
import org.eclipse.emf.compare.facade.internal.FacadeProviderRegistryImpl;
import org.eclipse.emf.compare.match.DefaultComparisonFactory;
import org.eclipse.emf.compare.match.DefaultEqualityHelperFactory;
import org.eclipse.emf.compare.match.DefaultMatchEngine;
import org.eclipse.emf.compare.match.IComparisonFactory;
import org.eclipse.emf.compare.match.IMatchEngine;
import org.eclipse.emf.compare.match.eobject.CachingDistance;
import org.eclipse.emf.compare.match.eobject.EditionDistance;
import org.eclipse.emf.compare.match.eobject.IEObjectMatcher;
import org.eclipse.emf.compare.match.eobject.ProximityEObjectMatcher;
import org.eclipse.emf.compare.match.eobject.WeightProvider;
import org.eclipse.emf.compare.match.eobject.WeightProviderDescriptorRegistryImpl;
import org.eclipse.emf.compare.match.resource.IResourceMatcher;
import org.eclipse.emf.compare.match.resource.StrategyResourceMatcher;
import org.eclipse.emf.compare.rcp.EMFCompareRCPPlugin;
import org.eclipse.emf.compare.rcp.internal.match.DefaultRCPMatchEngineFactory;
import org.eclipse.emf.compare.scope.IComparisonScope;
import org.eclipse.emf.compare.utils.UseIdentifiers;

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
	 * Creates and configures an {@link IEObjectMatcher} with the strategy given by {@code useIDs}.
	 * 
	 * @param useIDs
	 *            strategy for whether and when to rely on IDs for matching
	 * @param weightProviderRegistry
	 *            registry of {@link WeightProvider}s to use in case of structural matching
	 * @return a new IEObjectMatcher.
	 */
	public static IEObjectMatcher createDefaultEObjectMatcher(UseIdentifiers useIDs,
			WeightProvider.Descriptor.Registry weightProviderRegistry) {

		IEObjectMatcher result;
		EditionDistance editionDistance = new EditionDistance(weightProviderRegistry);
		CachingDistance cachedDistance = new CachingDistance(editionDistance);

		switch (useIDs) {
			case NEVER:
				result = new ProximityEObjectMatcher(cachedDistance);
				break;
			case ONLY:
				result = new FacadeIdentifierEObjectMatcher();
				break;
			case WHEN_AVAILABLE:
				// fall through to default
			default:
				// Use an ID matcher, delegating to proximity when no ID is available
				IEObjectMatcher contentMatcher = new ProximityEObjectMatcher(cachedDistance);
				result = new FacadeIdentifierEObjectMatcher(contentMatcher);
				break;

		}

		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Comparison match(IComparisonScope scope, Monitor monitor) {
		IComparisonScope scopeToMatch = scope;
		IFacadeProvider.Factory facadeProviderFactory = facadeProviderRegistry
				.getFacadeProviderFactory(scope);

		if (facadeProviderFactory != IFacadeProvider.Factory.NULL_FACTORY) {
			scopeToMatch = wrap(scope, facadeProviderFactory);
		} // else there are no façades

		return super.match(scopeToMatch, monitor);
	}

	/**
	 * Wraps a comparison scope as a scope that supplies façades (where possible).
	 * 
	 * @param delegate
	 *            a comparison scope to wrap
	 * @param facadeProviderFactory
	 *            creates providers of façades for the {@code scope}
	 * @return the wrapping scope
	 */
	protected IComparisonScope wrap(IComparisonScope delegate,
			IFacadeProvider.Factory facadeProviderFactory) {

		return new FacadeComparisonScope(facadeProviderFactory, delegate);
	}

	//
	// Nested types
	//

	/**
	 * The factory for the stand-alone (not using Eclipse extensions) {@link FacadeMatchEngine}.
	 *
	 * @author Christian W. Damus
	 */
	public static class Factory implements IMatchEngine.Factory {

		/** My relative ranking. */
		private int ranking;

		/** The option for identifier-based matching. */
		private final UseIdentifiers useIDs;

		/** Registry of weight-providers for structure-based (non-identifier-based) matching. */
		private final WeightProvider.Descriptor.Registry weightProviderRegistry;

		/** Registry of weight-providers for façade-based matching. */
		private final IFacadeProvider.Factory.Registry facadeProviderRegistry;

		/**
		 * Initializes me with the option for matching on identifiers when they are available. For
		 * structure-based matching I use the default stand-alone weight-provider registry and the default
		 * façade providers.
		 */
		public Factory() {
			this(UseIdentifiers.WHEN_AVAILABLE);
		}

		/**
		 * Initializes me with the specified option for matching on identifiers. For structure-based matching
		 * I use the default stand-alone weight-provider registry and the default façade providers.
		 * 
		 * @param useIDs
		 *            the option for matching on identifiers
		 */
		public Factory(UseIdentifiers useIDs) {
			this(useIDs, WeightProviderDescriptorRegistryImpl.createStandaloneInstance());
		}

		/**
		 * Initializes me with the specified option for matching on identifiers and a particular
		 * weight-provider registry. I use the default façade providers.
		 * 
		 * @param useIDs
		 *            the option for matching on identifiers
		 * @param weightProviderRegistry
		 *            weight-provider registry for structural (non-identifier-based) matching
		 */
		public Factory(UseIdentifiers useIDs, WeightProvider.Descriptor.Registry weightProviderRegistry) {
			this(useIDs, weightProviderRegistry, FacadeProviderRegistryImpl.createStandaloneInstance());
		}

		/**
		 * Initializes me with the specified option for matching on identifiers and a particular
		 * façade-provider registry. I use the default stand-alone wight-provider registry.
		 * 
		 * @param useIDs
		 *            the option for matching on identifiers
		 * @param facadeProviderRegistry
		 *            the façade-provider registry for façade-based matching (and comparisons)
		 */
		public Factory(UseIdentifiers useIDs, IFacadeProvider.Factory.Registry facadeProviderRegistry) {
			this(useIDs, WeightProviderDescriptorRegistryImpl.createStandaloneInstance(),
					facadeProviderRegistry);
		}

		/**
		 * Initializes me with the specified option for matching on identifiers and a particular
		 * weight-provider registry and façade-provider registry.
		 * 
		 * @param useIDs
		 *            the option for matching on identifiers
		 * @param weightProviderRegistry
		 *            weight-provider registry for structural (non-identifier-based) matching
		 * @param facadeProviderRegistry
		 *            the façade-provider registry for façade-based matching (and comparisons)
		 */
		public Factory(UseIdentifiers useIDs, WeightProvider.Descriptor.Registry weightProviderRegistry,
				IFacadeProvider.Factory.Registry facadeProviderRegistry) {

			this.useIDs = useIDs;
			this.weightProviderRegistry = weightProviderRegistry;
			this.facadeProviderRegistry = facadeProviderRegistry;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public int getRanking() {
			return ranking;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void setRanking(int ranking) {
			this.ranking = ranking;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean isMatchEngineFactoryFor(IComparisonScope scope) {
			return true;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public IMatchEngine getMatchEngine() {
			IComparisonFactory comparisonFactory = new DefaultComparisonFactory(
					new DefaultEqualityHelperFactory());
			IEObjectMatcher eObjectMatcher = createDefaultEObjectMatcher(useIDs, weightProviderRegistry);
			IResourceMatcher resourceMatcher = new StrategyResourceMatcher();

			IMatchEngine result = new FacadeMatchEngine(eObjectMatcher, resourceMatcher, comparisonFactory,
					facadeProviderRegistry);

			return result;
		}
	}

	/**
	 * The factory for the RCP-based (using Eclipse extensions) {@link FacadeMatchEngine}.
	 *
	 * @author Christian W. Damus
	 */
	public static class RCPFactory extends DefaultRCPMatchEngineFactory {
		/**
		 * Initializes me.
		 */
		public RCPFactory() {
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
