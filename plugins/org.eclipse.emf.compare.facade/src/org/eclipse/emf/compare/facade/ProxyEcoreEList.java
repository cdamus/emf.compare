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

import com.google.common.collect.Iterators;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.eclipse.emf.common.notify.NotificationChain;
import org.eclipse.emf.common.util.DelegatingEList;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.util.EcoreEList;
import org.eclipse.emf.ecore.util.InternalEList;

/**
 * A delegating reference list that wraps elements as dynamic façades proxies.
 *
 * @param <E>
 *            the list's element type
 * @author Christian W. Damus
 */
final class ProxyEcoreEList<E extends EObject> //
		extends DelegatingEList<E> //
		implements InternalEList.Unsettable<E>, EStructuralFeature.Setting {

	private static final long serialVersionUID = 1L;

	/** The backing list. */
	private final EcoreEList<E> delegate;

	/**
	 * Initializes me with my backing list.
	 * 
	 * @param delegate
	 *            my backing list
	 */
	ProxyEcoreEList(EcoreEList<E> delegate) {
		super();

		this.delegate = delegate;
	}

	/**
	 * Wrap an {@code object} in the list as a dynamic façade proxy.
	 * 
	 * @param object
	 *            an object
	 * @return its dynamic façade proxy wrapper
	 */
	@SuppressWarnings("unchecked")
	E wrap(E object) {
		return (E)FacadeProxy.createProxy(object);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected List<E> delegateList() {
		return delegate;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected int delegateIndexOf(Object object) {
		if (object instanceof EObject) {
			return super.delegateIndexOf(FacadeProxy.unwrap((EObject)object));
		}
		return super.delegateIndexOf(object);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected E resolve(int index, E object) {
		return wrap(object);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected E validate(int index, E object) {
		return FacadeProxy.unwrap(super.validate(index, object));
	}

	//
	// Internal EMF list APIs
	//

	/**
	 * {@inheritDoc}
	 */
	@Override
	public EObject getEObject() {
		return delegate.getEObject();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public EStructuralFeature getEStructuralFeature() {
		return delegate.getEStructuralFeature();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object get(boolean resolve) {
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@SuppressWarnings("unchecked")
	public void set(Object newValue) {
		clear();
		addAll((List<? extends E>)newValue);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isSet() {
		return delegate.isSet();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void unset() {
		delegate.unset();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object[] toArray() {
		return Iterators.toArray(iterator(), Object.class);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object[] basicToArray() {
		return super.toArray();
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <T> T[] toArray(T[] array) {
		T[] result = array;

		if (result.length < size()) {
			result = (T[])Array.newInstance(array.getClass().getComponentType(), size());
		}

		int i = 0;
		for (E next : this) {
			result[i++] = (T)next;
		}
		if (i < array.length) {
			result[i] = null; // Terminate the array
		}

		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <T> T[] basicToArray(T[] array) {
		return super.toArray(array);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int basicIndexOf(Object object) {
		return super.indexOf(object);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int basicLastIndexOf(Object object) {
		return super.lastIndexOf(object);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean basicContains(Object object) {
		return super.contains(object);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean basicContainsAll(Collection<?> collection) {
		return super.containsAll(collection);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Iterator<E> basicIterator() {
		return super.basicIterator();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ListIterator<E> basicListIterator() {
		return super.basicListIterator();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ListIterator<E> basicListIterator(int index) {
		return super.basicListIterator(index);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public E basicGet(int index) {
		return super.basicGet(index);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected E primitiveGet(int index) {
		return wrap(super.primitiveGet(index));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<E> basicList() {
		// They need to be wrapped, but not by me
		return new ProxyEList<E>((EList<E>)super.basicList());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public NotificationChain basicAdd(E object, NotificationChain notifications) {
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public NotificationChain basicRemove(Object object, NotificationChain notifications) {
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean addAllUnique(Collection<? extends E> collection) {
		return addAllUnique(size(), collection);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean addAllUnique(int index, Collection<? extends E> collection) {
		++modCount;

		if (collection.isEmpty()) {
			return false;
		} else {
			Collection<? extends E> unwrapped = FacadeProxy.unwrap(collection);
			delegateList().addAll(unwrapped);

			int i = index;
			for (E object : unwrapped) {
				didAdd(i, object);
				didChange();
				i++;
			}

			return true;
		}
	}

}
