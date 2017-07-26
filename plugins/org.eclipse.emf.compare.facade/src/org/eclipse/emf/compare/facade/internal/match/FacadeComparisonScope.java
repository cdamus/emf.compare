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

import static com.google.common.base.Predicates.notNull;
import static com.google.common.collect.Iterators.filter;
import static com.google.common.collect.Iterators.transform;

import com.google.common.collect.AbstractIterator;

import java.util.Iterator;
import java.util.Set;

import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.compare.facade.IFacadeProvider;
import org.eclipse.emf.compare.scope.IComparisonScope;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;

/**
 * A decorating scope that extracts façade model elements from the underlying model elements in the base
 * scope. This assumes that those underlying model elements are already determined to be representable by a
 * façade, by the {@link FacadeMatchEngine}'s registry of {@link IFacadeProvider} factories having yielded at
 * least one suitable façade provider.
 *
 * @author Christian W. Damus
 */
class FacadeComparisonScope implements IComparisonScope {
	/** The façade provider factory that I use to get façade providers for each resource. */
	private final IFacadeProvider.Factory facadeProviderFactory;

	/** The façade provider that I use on resources that have façades. */
	private final IFacadeProvider facadeProvider;

	/** The comparison scope that provides the underlying model elements to be compared via the façade. */
	private final IComparisonScope delegate;

	/**
	 * Initializes me with my façade provider factory and underlying comparison scope.
	 * 
	 * @param facadeProviderFactory
	 *            the factory of providers of façade model elements
	 * @param delegate
	 *            the provider of underlying model elements to be compared via the façade
	 */
	FacadeComparisonScope(IFacadeProvider.Factory facadeProviderFactory, IComparisonScope delegate) {
		super();

		this.facadeProviderFactory = facadeProviderFactory;
		this.facadeProvider = facadeProviderFactory.getFacadeProvider();
		this.delegate = delegate;
	}

	public Notifier getLeft() {
		return facadeElse(delegate.getLeft());
	}

	public Notifier getRight() {
		return facadeElse(delegate.getRight());
	}

	public Notifier getOrigin() {
		return facadeElse(delegate.getOrigin());
	}

	/**
	 * Obtain the façade for a {@code notifier} or else {@code null}.
	 * 
	 * @param notifier
	 *            a notifier
	 * @return its corresponding façade or {@code null} if none
	 * @param <T>
	 *            the notifier type
	 */
	<T extends Notifier> T facade(T notifier) {
		return facadeElse(notifier, null);
	}

	/**
	 * Obtain the façade for a {@code notifier} or else just the original {@code notifier}, itself.
	 * 
	 * @param notifier
	 *            a notifier
	 * @return its corresponding façade or the {@code notifier} if none
	 * @param <T>
	 *            the notifier type
	 */
	<T extends Notifier> T facadeElse(T notifier) {
		return facadeElse(notifier, notifier);
	}

	/**
	 * Obtain the façade for a {@code notifier} or else a default result.
	 * 
	 * @param notifier
	 *            a notifier
	 * @param defaultResult
	 *            the return result in case the {@code notifier} has no corresponding façade
	 * @return its corresponding façade or the {@code defaultResult} if none
	 * @param <T>
	 *            the notifier type
	 */
	<T extends Notifier> T facadeElse(T notifier, T defaultResult) {
		T result = defaultResult;

		if (notifier instanceof EObject) {
			EObject facade = facadeProvider.createFacade((EObject)notifier);
			if (facade != null) {
				@SuppressWarnings("unchecked")
				T facadeAsT = (T)facade;
				result = facadeAsT;
			}
		}

		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	public Set<String> getResourceURIs() {
		return delegate.getResourceURIs();
	}

	/**
	 * {@inheritDoc}
	 */
	public Set<String> getNsURIs() {
		return delegate.getNsURIs();
	}

	/**
	 * {@inheritDoc}
	 */
	public Iterator<? extends Resource> getCoveredResources(ResourceSet resourceSet) {
		return delegate.getCoveredResources(resourceSet);
	}

	/**
	 * {@inheritDoc}
	 */
	public Iterator<? extends EObject> getCoveredEObjects(Resource resource) {
		if (!facadeProviderFactory.isFacadeProviderFactoryFor(resource)) {
			// No façades in here
			return delegate.getCoveredEObjects(resource);
		}

		Iterator<? extends EObject> roots = filter(transform(resource.getContents().iterator(), this::facade),
				notNull());

		return new AbstractIterator<EObject>() {
			private Iterator<? extends EObject> currentTree;

			/**
			 * {@inheritDoc}
			 */
			@Override
			protected EObject computeNext() {
				EObject result;

				if (currentTree == null) {
					if (roots.hasNext()) {
						result = roots.next();
						currentTree = getChildren(result);
					} else {
						result = endOfData();
					}
				} else {
					if (currentTree.hasNext()) {
						result = currentTree.next();
					} else {
						// This tail-recursive call will go into the previous top-level
						// if-case, so it won't recurse any further
						currentTree = null;
						result = computeNext();
					}
				}

				return result;
			}
		};
	}

	/**
	 * {@inheritDoc}
	 */
	public Iterator<? extends EObject> getChildren(EObject eObject) {
		// The input here is already a façade, if appropriate
		return delegate.getChildren(eObject);
	}
}
