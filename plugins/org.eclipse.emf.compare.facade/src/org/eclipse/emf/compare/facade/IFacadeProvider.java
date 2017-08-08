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

import com.google.common.base.Supplier;

import java.util.List;

import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.compare.scope.IComparisonScope;
import org.eclipse.emf.ecore.EObject;

/**
 * A provider of façades for model elements that should be compared <em>in lieu of</em> the input model
 * elements. It is expected that façades for semantically comparable model elements will, themselves, be
 * likewise comparable.
 *
 * @author Christian W. Damus
 */
@FunctionalInterface
public interface IFacadeProvider {

	/**
	 * A façade provider that provides no façade for any input.
	 */
	IFacadeProvider NULL_PROVIDER = ignored -> null;

	/**
	 * Creates the façade object for the given underlying object in the model.
	 * 
	 * @param underlyingObject
	 *            an object in the underlying (actual) representation of the model
	 * @return its façade, or {@code null} if the underlying object does not have a representation in the
	 *         façade (in which case it is expected to be managed by some other façade object)
	 */
	FacadeObject createFacade(EObject underlyingObject);

	/**
	 * Composes me with another façade provider to which I delegate when I cannot provider a façade for some
	 * underlying object.
	 * 
	 * @param elseProvider
	 *            the provider to delegate to when I provide no façade
	 * @return the composed façade provider
	 */
	default IFacadeProvider compose(IFacadeProvider elseProvider) {
		return underlyingObject -> {
			FacadeObject result = this.createFacade(underlyingObject);

			if (result == null) {
				result = elseProvider.createFacade(underlyingObject);
			}

			return result;
		};
	}

	//
	// Nested types
	//

	/**
	 * A (registered) factory that creates a façade provider appropriate to some model that is an input to a
	 * comparison.
	 *
	 * @author Christian W. Damus
	 */
	interface Factory {

		/**
		 * A factory that always returns the {@linkplain IFacadeProvider#NULL_PROVIDER null} façade provider.
		 * 
		 * @see IFacadeProvider#NULL_PROVIDER
		 */
		IFacadeProvider.Factory NULL_FACTORY = new AbstractImpl(Integer.MIN_VALUE,
				() -> IFacadeProvider.NULL_PROVIDER) {
			/**
			 * {@inheritDoc}
			 */
			public boolean isFacadeProviderFactoryFor(Notifier notifier) {
				return true;
			}

			/**
			 * {@inheritDoc}
			 */
			@Override
			public void setRanking(int ranking) {
				// Pass
			}
		};

		/**
		 * Obtains the façade provider.
		 * 
		 * @return the façade provider
		 */
		IFacadeProvider getFacadeProvider();

		/**
		 * Queries the ranking of this façade provider factory.
		 * 
		 * @return my ranking
		 */
		int getRanking();

		/**
		 * Sets the ranking of this façade provider factory.
		 * 
		 * @param ranking
		 *            my ranking
		 */
		void setRanking(int ranking);

		/**
		 * Queries whether the façade provider can create suitable façades for the given comparison
		 * {@code scope}.
		 * 
		 * @param scope
		 *            the scope of the comparison that is possibly to be re-directed to façades
		 * @return whether I can create façades for the model elements within the {@code scope}
		 */
		default boolean isFacadeProviderFactoryFor(IComparisonScope scope) {
			// CHECKSTYLE:OFF Sorry, but this just isn't complex to understand
			return ((scope.getLeft() != null) && isFacadeProviderFactoryFor(scope.getLeft()))
					|| ((scope.getRight() != null) && isFacadeProviderFactoryFor(scope.getRight()))
					|| ((scope.getOrigin() != null) && isFacadeProviderFactoryFor(scope.getOrigin()));
			// CHECKSTYLE:ON
		}

		/**
		 * Queries whether the façade provider can create suitable façades for the given {@code notifier} in a
		 * comparison.
		 * 
		 * @param notifier
		 *            one of the two or three sides in a comparison
		 * @return whether I can create façades for the model elements within the {@code notifier}
		 */
		boolean isFacadeProviderFactoryFor(Notifier notifier);

		/**
		 * Composes myself with another factory in ranking order (higher-ranked factory first).
		 * 
		 * @param otherFactory
		 *            another factory
		 * @return the composite factory
		 */
		default IFacadeProvider.Factory compose(IFacadeProvider.Factory otherFactory) {
			/**
			 * Implementation of a composed façade provider factory.
			 * 
			 * @author Christian W. Damus
			 */
			final class Composite extends AbstractImpl {
				private final IFacadeProvider.Factory higherDelegate;

				private final IFacadeProvider.Factory lesserDelegate;

				/**
				 * Initializes me with my delegates, in ranking order.
				 * 
				 * @param higherDelegate
				 *            the higher-ranked factory
				 * @param lesserDelegate
				 *            the lesser-ranked factory
				 */
				Composite(IFacadeProvider.Factory higherDelegate, IFacadeProvider.Factory lesserDelegate) {
					super(higherDelegate.getRanking(), () -> higherDelegate.getFacadeProvider()
							.compose(lesserDelegate.getFacadeProvider()));

					this.higherDelegate = higherDelegate;
					this.lesserDelegate = lesserDelegate;
				}

				/**
				 * {@inheritDoc}
				 */
				public boolean isFacadeProviderFactoryFor(IComparisonScope scope) {
					return higherDelegate.isFacadeProviderFactoryFor(scope)
							|| lesserDelegate.isFacadeProviderFactoryFor(scope);
				}

				/**
				 * {@inheritDoc}
				 */
				public boolean isFacadeProviderFactoryFor(Notifier notifier) {
					return higherDelegate.isFacadeProviderFactoryFor(notifier)
							|| lesserDelegate.isFacadeProviderFactoryFor(notifier);
				}
			}

			int myRanking = getRanking();
			int theirRanking = otherFactory.getRanking();

			if (myRanking >= theirRanking) {
				return new Composite(this, otherFactory);
			} else {
				return new Composite(otherFactory, this);
			}
		}

		//
		// Nested types
		//

		/**
		 * An abstract superclass for façade provider factories.
		 *
		 * @author Christian W. Damus
		 */
		abstract class AbstractImpl implements IFacadeProvider.Factory {
			/** An optional supplier of the façade provider to be created. */
			private final Supplier<? extends IFacadeProvider> facadeProviderSupplier;

			/** My ranking. */
			private int ranking;

			/** The created façade provider. */
			private IFacadeProvider provider;

			/**
			 * Initializes me with my ranking and a supplier that can create the façade provider when it is
			 * required.
			 * 
			 * @param ranking
			 *            my ranking
			 * @param facadeProviderSupplier
			 *            the supplier of the façade provider, or {@code null} if the subclass overrides the
			 *            {@link #createProvider()} method
			 */
			protected AbstractImpl(int ranking, Supplier<? extends IFacadeProvider> facadeProviderSupplier) {
				super();

				if (facadeProviderSupplier == null) {
					this.facadeProviderSupplier = this::mustOverrideCreateProvider;
				} else {
					this.facadeProviderSupplier = facadeProviderSupplier;
				}

				this.ranking = ranking;
			}

			/**
			 * Initializes me with my ranking. The subclass must override the {@link #createProvider()} method
			 * to create the façade provider.
			 * 
			 * @param ranking
			 *            my ranking
			 */
			protected AbstractImpl(int ranking) {
				this(ranking, null);
			}

			/**
			 * Initializes me a supplier that can create the façade provider when it is required.
			 * 
			 * @param facadeProviderSupplier
			 *            the supplier of the façade provider, or {@code null} if the subclass overrides the
			 *            {@link #createProvider()} method
			 */
			protected AbstractImpl(Supplier<? extends IFacadeProvider> facadeProviderSupplier) {
				this(0, facadeProviderSupplier);
			}

			/**
			 * Initializes me. The subclass must override the {@link #createProvider()} method to create the
			 * façade provider.
			 */
			protected AbstractImpl() {
				this(null);
			}

			/**
			 * {@inheritDoc}
			 */
			public int getRanking() {
				return ranking;
			}

			/**
			 * {@inheritDoc}
			 */
			public void setRanking(int ranking) {
				this.ranking = ranking;
			}

			/**
			 * {@inheritDoc}
			 */
			public IFacadeProvider getFacadeProvider() {
				if (provider == null) {
					provider = createProvider();
				}

				return provider;
			}

			/**
			 * Create a new provider.
			 * 
			 * @return a new façade provider
			 */
			protected IFacadeProvider createProvider() {
				return facadeProviderSupplier.get();
			}

			/**
			 * Throws an exception indicating that the class must override the {@link #createProvider()}
			 * method because it did not provide a façade-provider supplier.
			 * 
			 * @return never
			 * @throws IllegalArgumentException
			 *             always
			 */
			private IFacadeProvider mustOverrideCreateProvider() {
				throw new IllegalArgumentException(String
						.format("class %s must override the createProvider() method", getClass().getName())); //$NON-NLS-1$
			}
		}

		/**
		 * A registry of {@linkplain IFacadeProvider.Factory façade provider factories}.
		 * 
		 * @noimplement This interface is not intended to be implemented by clients.
		 * @noextend This interface is not intended to be extended by clients.
		 */
		interface Registry {

			/**
			 * Obtains a factory that creates a façade provider suitable for the given comparison
			 * {@code scope}, which is a ranked delegation chain over
			 * {@linkplain #getFacadeProviderFactories(IComparisonScope) all applicable providers}.
			 * 
			 * @param scope
			 *            a comparison scope
			 * @return the factory, never {@code null} but possibly the
			 *         {@linkplain IFacadeProvider.Factory#NULL_FACTORY null factory}
			 * @see #getFacadeProviderFactories(IComparisonScope)
			 */
			default IFacadeProvider.Factory getFacadeProviderFactory(IComparisonScope scope) {
				return getFacadeProviderFactories(scope).stream() //
						.reduce(IFacadeProvider.Factory::compose) //
						.orElse(IFacadeProvider.Factory.NULL_FACTORY);
			}

			/**
			 * Obtains the highest-ranked façade provider for the given {@code scope}.
			 * 
			 * @param scope
			 *            a comparison scope
			 * @return the highest-ranked façade provider, or {@code null} if none
			 */
			IFacadeProvider.Factory getHighestRankingFacadeProviderFactory(IComparisonScope scope);

			/**
			 * Obtains the façade providers applicable to the given {@code scope}, in rank order from highest
			 * to lowest rank.
			 * 
			 * @param scope
			 *            a comparison scope
			 * @return applicable façade factories, in rank order, or an empty list if none
			 */
			List<IFacadeProvider.Factory> getFacadeProviderFactories(IComparisonScope scope);

			/**
			 * Registers a façade provider.
			 * 
			 * @param facadeProviderFactory
			 *            a façade provider factory to add to the registry
			 * @return the previously registered façade provider of the same class as the new factory, or
			 *         {@code null} if there was previous registration of that class
			 */
			IFacadeProvider.Factory add(IFacadeProvider.Factory facadeProviderFactory);

			/**
			 * Removes the registration of a façade provider of the given class.
			 * 
			 * @param className
			 *            name of a (possibly) registered façade provider factory
			 * @return the façade provider factory that was removed, or {@code null} if no provider of the
			 *         given class was registered
			 */
			IFacadeProvider.Factory remove(String className);

			/**
			 * Removes the registration of a façade provider of the given class.
			 * 
			 * @param factoryClass
			 *            type of a (possibly) registered façade provider factory
			 * @return the façade provider factory that was removed, or {@code null} if no provider of the
			 *         given class was registered
			 */
			default IFacadeProvider.Factory remove(Class<? extends IFacadeProvider.Factory> factoryClass) {
				return remove(factoryClass.getName());
			}

			/**
			 * Removes the registration of a façade provider of the same class as the given {@code factory}.
			 * 
			 * @param factory
			 *            a (possibly) registered façade provider factory
			 * @return the façade provider factory that was removed, or {@code null} if no provider of the
			 *         {@code factory}'s type was registered
			 */
			default IFacadeProvider.Factory remove(IFacadeProvider.Factory factory) {
				return remove(factory.getClass());
			}

			/**
			 * Clear the registry.
			 */
			void clear();
		}
	}

}
