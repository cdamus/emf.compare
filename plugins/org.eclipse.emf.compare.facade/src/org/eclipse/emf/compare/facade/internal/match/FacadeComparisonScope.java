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

import com.google.common.base.Suppliers;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.MapMaker;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Supplier;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.emf.common.EMFPlugin;
import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.compare.facade.FacadeObject;
import org.eclipse.emf.compare.facade.IFacadeProvider;
import org.eclipse.emf.compare.scope.AbstractComparisonScope;
import org.eclipse.emf.compare.scope.IComparisonScope;
import org.eclipse.emf.compare.utils.TreeIterators;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;

/**
 * A decorating scope that extracts façade model elements from the underlying model elements in the base
 * scope. This assumes that those underlying model elements are already determined to be representable by a
 * façade, by the {@link FacadeMatchEngine}'s registry of {@link IFacadeProvider} factories having yielded at
 * least one suitable façade provider.
 *
 * @author Christian W. Damus
 */
public final class FacadeComparisonScope implements IComparisonScope, IAdaptable {

	/**
	 * Mapping of comparison scopes to façade wrappers. Depends on the values using weak references to the
	 * keys to avoid memory leaks. <b>Note</b> that weak values cannot be used, otherwise we would get
	 * premature collection of the façade scope wrappers from the map as they are quickly forgotten by the
	 * comparison algorithm.
	 */
	private static final Map<IComparisonScope, IComparisonScope> INSTANCES = new MapMaker().weakKeys()
			.makeMap();

	/** The façade provider factory that I use to get façade providers for each resource. */
	private final IFacadeProvider.Factory facadeProviderFactory;

	/** The façade provider that I use on resources that have façades. */
	private final IFacadeProvider facadeProvider;

	/**
	 * The comparison scope that provides the underlying model elements to be compared via the façade. Use a
	 * weak reference to avoid leaking the wrapping scope in the static map of instances.
	 */
	private final Supplier<IComparisonScope> delegate;

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

		Supplier<IComparisonScope> backup = Suppliers.memoize(EmptyScope::new);
		this.delegate = Suppliers.compose(d -> {
			if (d == null) {
				return backup.get();
			} else {
				return d;
			}
		}, new WeakReference<>(delegate)::get);

		INSTANCES.put(delegate, this);
	}

	/**
	 * Obtains the most appropriate view of a scope for inclusion of façades in the given {@code scope}.
	 * 
	 * @param scope
	 *            a comparison scope
	 * @return a façade-providing view of the {@code scope}, or else the {@code scope} if façades are not
	 *         applicable
	 */
	public static IComparisonScope getFacadeScope(IComparisonScope scope) {
		return INSTANCES.getOrDefault(scope, scope);
	}

	@Override
	public Notifier getLeft() {
		return facadeElse(delegate.get().getLeft());
	}

	@Override
	public Notifier getRight() {
		return facadeElse(delegate.get().getRight());
	}

	@Override
	public Notifier getOrigin() {
		return facadeElse(delegate.get().getOrigin());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <T> T getAdapter(Class<T> adapter) {
		T result;

		if (adapter.isInstance(this)) {
			result = adapter.cast(this);
		} else if (adapter == IFacadeProvider.class) {
			result = adapter.cast(facadeProvider);
		} else if (adapter == IFacadeProvider.Factory.class) {
			result = adapter.cast(facadeProviderFactory);
		} else if (EMFPlugin.IS_ECLIPSE_RUNNING) {
			result = Platform.getAdapterManager().getAdapter(this, adapter);
		} else {
			result = null;
		}

		return result;
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
	 *            the notifier type, either {@link ResourceSet}, {@link Resource}, or {@link EObject} but not
	 *            more specific than these because unerlying and façade types are necessarily different
	 */
	<T extends Notifier> T facadeElse(T notifier, T defaultResult) {
		T result = defaultResult;

		if (notifier instanceof EObject) {
			EObject facade = facadeProvider.createFacade((EObject)notifier);
			if ((facade != FacadeObject.NULL) && (facade != null)) {
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
	@Override
	public Set<String> getResourceURIs() {
		return delegate.get().getResourceURIs();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<String> getNsURIs() {
		return delegate.get().getNsURIs();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Iterator<? extends Resource> getCoveredResources(ResourceSet resourceSet) {
		return delegate.get().getCoveredResources(resourceSet);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Iterator<? extends EObject> getCoveredEObjects(Resource resource) {
		if (!facadeProviderFactory.isFacadeProviderFactoryFor(resource)) {
			// No façades in here
			return delegate.get().getCoveredEObjects(resource);
		}

		// Ignore root elements that are not façades
		Iterator<? extends EObject> roots = filter(transform(resource.getContents().iterator(), this::facade),
				notNull());

		// Iterate the tree per the delegate's getChildren(EObject)
		return new ResourceIterator(roots);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Iterator<? extends EObject> getChildren(EObject eObject) {
		Iterator<? extends EObject> result;
		Iterator<? extends EObject> raw = delegate.get().getChildren(eObject);

		if (raw instanceof TreeIterator<?>) {
			result = new FacadeTreeIterator(raw);
		} else {
			// Any elements that aren't façades are taken as is
			result = Iterators.transform(raw, this::facadeElse);
		}

		return result;
	}

	//
	// Nested types
	//

	/**
	 * An iterator over {@link EObject} content that substitutes façades where they are available for
	 * underlying elements and, when an element is replaced by a façade, iterates the façade's subtree instead
	 * of the original element's.
	 *
	 * @author Christian W. Damus
	 */
	protected class FacadeTreeIterator implements TreeIterator<EObject> {
		/** The top sub-tree iterator in the prune stack. */
		Iterator<? extends EObject> current;

		/** A stack of iterators to resume after completion of a prune substitution. */
		List<TreeIterator<? extends EObject>> pruneStack = Lists.newArrayListWithExpectedSize(3);

		/** The current state of the iteration. */
		private State state = State.INITIAL;

		/** The prepared next object to return from the iteration. */
		private EObject preparedNext;

		/**
		 * Initializes me with my delegate.
		 * 
		 * @param delegate
		 *            the underlying (raw) content iterator
		 */
		public FacadeTreeIterator(Iterator<? extends EObject> delegate) {
			super();

			this.current = delegate;
		}

		/**
		 * Pops the tree-iterator to resume after finishing a façade sub-tree.
		 * 
		 * @return the tree iterator to resume, or {@code null} if iteration is finished
		 */
		protected final TreeIterator<? extends EObject> pop() {
			TreeIterator<? extends EObject> result = null;

			if (!pruneStack.isEmpty()) {
				result = pruneStack.remove(pruneStack.size() - 1);
				current = result;
			}

			return result;
		}

		/**
		 * If an {@code iterator} is a tree iterator, prunes it and pushes it onto the stack for resumption
		 * later after we have finished with the façade sub-tree.
		 * 
		 * @param iterator
		 *            an iterator to prune and push, if it is a tree iterator
		 * @return {@code true} if the {@code iterator} could be pruned and pushed; {@code false} otherwise,
		 *         in which case it should continue to be the current
		 */
		protected final boolean push(Iterator<? extends EObject> iterator) {
			boolean result = false;

			// We can only push tree iterators for pruning
			if (iterator instanceof TreeIterator<?>) {
				TreeIterator<? extends EObject> subtree = (TreeIterator<? extends EObject>)iterator;
				subtree.prune();
				result = pruneStack.add(subtree);
			}

			return result;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean hasNext() {
			boolean result;

			if (state.isDone()) {
				result = false;
			} else if (state.isPrepared()) {
				result = true;
			} else {
				prepareNext();
				result = hasNext();
			}

			return result;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public final EObject next() {
			EObject result;

			if (!hasNext()) {
				throw new NoSuchElementException();
			}

			result = preparedNext;
			state = state.returned();
			preparedNext = null;

			return result;
		}

		/** Prepares the next value to return. */
		private void prepareNext() {
			preparedNext = computeNext();
		}

		/**
		 * Signals that there is nothing left to iterate.
		 * 
		 * @return a dummy, useful as the return result from {@link #computeNext()}
		 */
		final EObject endOfData() {
			state = State.DONE;
			return null;
		}

		/**
		 * {@inheritDoc}
		 */
		EObject computeNext() {
			EObject result;

			if (current == null) {
				// Pop one off the stack, if available
				if (pop() != null) {
					result = computeNext();
				} else {
					result = endOfData();
				}
			} else {
				if (current.hasNext()) {
					result = current.next();

					EObject facade = facadeElse(result);
					if (facade == result) {
						// Just a normal iteration step
						state = state.prepared();
					} else {
						// Got a facade. Substitute it for the result and the sub-tree, too
						result = facade;

						if (push(current)) {
							current = getChildren(facade);
							state = state.substituted();
						} else {
							// Can't prune? Have to hope that the children of the original map
							// one-for-one to façades, then
						}
					}
				} else {
					current = null;
					result = computeNext();
				}
			}

			return result;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void prune() {
			switch (state) {
				case RETURNED:
					if (current instanceof TreeIterator<?>) {
						((TreeIterator<?>)current).prune();
					}
					break;
				case RETURNED_SUBSTITUTE:
					// The current iterator is that object's contents, so skip it entirely
					current = null;
					break;
				default:
					// Can only prune after a result was returned
					break;

			}
		}
	}

	/**
	 * An iterator over {@link Resource} content that is seeded with an iterator over the resource's contents
	 * and so handles that case specially.
	 *
	 * @author Christian W. Damus
	 */
	private class ResourceIterator extends AbstractIterator<EObject> implements TreeIterator<EObject> {
		/** The roots of the resource. */
		private final Iterator<? extends EObject> roots;

		/** The current root object's sub-tree. */
		private Iterator<? extends EObject> currentTree;

		/** The iterator to which to delegate a prune() call. */
		private TreeIterator<? extends EObject> pruneIterator;

		/**
		 * Initializes me with the resource roots that I iterate over with their contents.
		 * 
		 * @param roots
		 *            the resource roots
		 */
		ResourceIterator(Iterator<? extends EObject> roots) {
			super();

			this.roots = roots;
		}

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

					// Calling prune() now should remove the entire currentTree, not
					// just some portion of it
					pruneIterator = null;
				} else {
					result = endOfData();
				}
			} else {
				if (currentTree instanceof TreeIterator<?>) {
					// Now that we have stepped into this iterator, it can be pruned
					pruneIterator = (TreeIterator<? extends EObject>)currentTree;
				}

				if (currentTree.hasNext()) {
					result = currentTree.next();
				} else {
					currentTree = null;
					result = computeNext();
				}
			}

			return result;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void prune() {
			if (pruneIterator != null) {
				pruneIterator.prune();
			} else {
				// Just prune off the entire current sub-tree
				currentTree = null;
			}
		}
	}

	/**
	 * Enumeration of the possible states of the {@link FacadeTreeIterator}.
	 *
	 * @author Christian W. Damus
	 */
	enum State {
		/** We haven't yet computed anything. */
		INITIAL,
		/** We have prepared the next element to return. */
		PREPARED,
		/** We have returned an element and are ready to prepare another. */
		RETURNED,
		/** We have prepared the next element to return and it was a façade substitution. */
		PREPARED_SUBSTITUTE,
		/** We have returned a façade substitution and are ready to prepare the next element. */
		RETURNED_SUBSTITUTE,
		/** Iteration has finished; we have no more. */
		DONE;

		/**
		 * Queries whether I am the <em>done</em> state.
		 * 
		 * @return whether I am the <em>done</em> state
		 */
		boolean isDone() {
			return this == DONE;
		}

		/**
		 * Queries whether I am a kind of <em>prepared</em> state.
		 * 
		 * @return whether I am a kind of <em>prepared</em> state
		 */
		boolean isPrepared() {
			return (this == PREPARED) || (this == PREPARED_SUBSTITUTE);
		}

		/**
		 * Transitions from a <em>prepared</em> state to the corresponding <em>returned</em> state.
		 * 
		 * @return the <em>returned</em> state
		 * @throws IllegalStateException
		 *             if we cannot transition to a <em>returned</em> state from this
		 */
		State returned() {
			switch (this) {
				case PREPARED:
					return RETURNED;
				case PREPARED_SUBSTITUTE:
					return RETURNED_SUBSTITUTE;
				default:
					throw new IllegalStateException("return from " + this); //$NON-NLS-1$
			}
		}

		/**
		 * Transitions to the <em>prepared</em> state.
		 * 
		 * @return the <em>prepared</em> state
		 * @throws IllegalStateException
		 *             if we cannot transition to the <em>prepared</em> state from this
		 */
		State prepared() {
			switch (this) {
				case INITIAL:
				case RETURNED:
				case RETURNED_SUBSTITUTE:
					return PREPARED;
				default:
					throw new IllegalStateException("prepare from " + this); //$NON-NLS-1$
			}
		}

		/**
		 * Transitions to the <em>prepared substitution</em> state.
		 * 
		 * @return the <em>prepared substitution</em> state
		 * @throws IllegalStateException
		 *             if we cannot transition to the <em>prepared substitution</em> state from this
		 */
		State substituted() {
			switch (this) {
				case INITIAL:
				case RETURNED:
				case RETURNED_SUBSTITUTE:
					return PREPARED_SUBSTITUTE;
				default:
					throw new IllegalStateException("substitute from " + this); //$NON-NLS-1$
			}
		}
	}

	/**
	 * An empty comparison scope.
	 *
	 * @author Christian W. Damus
	 */
	private static final class EmptyScope extends AbstractComparisonScope {
		/**
		 * Initializes me.
		 */
		EmptyScope() {
			super(new ResourceSetImpl(), new ResourceSetImpl(), new ResourceSetImpl());
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Iterator<? extends EObject> getChildren(EObject eObject) {
			return TreeIterators.emptyIterator();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Iterator<? extends EObject> getCoveredEObjects(Resource resource) {
			return TreeIterators.emptyIterator();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Iterator<? extends Resource> getCoveredResources(ResourceSet resourceSet) {
			return TreeIterators.emptyIterator();
		}

	}
}
