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
package org.eclipse.emf.compare.uml2.facade;

import java.util.function.BiFunction;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.compare.facade.FacadeAdapter;
import org.eclipse.emf.compare.facade.SyncDirectionKind;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.uml2.uml.Element;

/**
 * A specialized façade adapter for UML that includes support for stereotype applications representing DSML
 * concepts.
 *
 * @author Christian W. Damus
 */
public class UMLFacadeAdapter extends FacadeAdapter {

	/** The application of the primary stereotype, if any, representing the domain concept. */
	private final EObject stereotype;

	/**
	 * Initializes me with the façade and underlying UML model element that I associate with one another, and
	 * optionally a primary stereotype representing the façade's domain concept.
	 * 
	 * @param facade
	 *            the façade element
	 * @param umlElement
	 *            the underlying model element
	 * @param stereotype
	 *            application of the primary UML stereotype of the domain concept, or {@code null} if there is
	 *            no stereotype representing that concept
	 */
	public UMLFacadeAdapter(EObject facade, Element umlElement, EObject stereotype) {
		super(facade, umlElement);

		this.stereotype = stereotype;

		if (stereotype != null) {
			addAdapter(stereotype);
		}
	}

	/**
	 * Initializes with the façade and underlying UML model element that I associate with one another where
	 * the UML element does not have a primary stereotype for its domain concept.
	 * 
	 * @param facade
	 *            the façade element
	 * @param umlElement
	 *            the underlying model element
	 */
	public UMLFacadeAdapter(EObject facade, Element umlElement) {
		this(facade, umlElement, null);
	}

	/**
	 * Detaches me from all model elements that I adapt.
	 */
	@Override
	public void dispose() {
		super.dispose();

		if (stereotype != null) {
			removeAdapter(stereotype);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Element getUnderlyingElement() {
		return (Element)super.getUnderlyingElement();
	}

	/**
	 * Obtains the application of the primary stereotype, if any, representing the underlying UML model
	 * element's domain concept.
	 * 
	 * @return the stereotype application, or {@code null}
	 */
	public EObject getStereotype() {
		return stereotype;
	}

	/**
	 * Reacts to changes in the façade or underlying element to synchronize with the other.
	 * 
	 * @param notification
	 *            description of a change in either the façade or the underlying element
	 */
	@Override
	public void notifyChanged(Notification notification) {
		if (notification.isTouch()) {
			return;
		}

		if ((stereotype != null) && (notification.getNotifier() == stereotype)) {
			syncStereotypeToFacade(notification);
		} else {
			super.notifyChanged(notification);
		}
	}

	/**
	 * Synchronizes the underlying model's stereotype to the façade, triggered by the given
	 * {@code notification}.
	 * 
	 * @param notification
	 *            description of a change in the model
	 */
	protected void syncStereotypeToFacade(Notification notification) {
		synchronize(stereotype, getFacade(), false, notification);
	}

	/**
	 * Synchronizes the façade to its underlying model's stereotype, triggered by the given
	 * {@code notification}.
	 * 
	 * @param notification
	 *            description of a change in the façade
	 */
	protected void syncFacadeToStereotype(Notification notification) {
		synchronize(getFacade(), stereotype, true, notification);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void syncFacadeToModel(Notification notification) {
		super.syncFacadeToModel(notification);

		if (stereotype != null) {
			syncFacadeToStereotype(notification);
		}
	}

	/**
	 * I am an adapter for either the {@code UMLFacadeAdapter} type or whatever the superclass implementation
	 * determines.
	 * 
	 * @param type
	 *            the adapter type to test for
	 * @return whether I am an adapter of the given {@code type}
	 */
	@Override
	public boolean isAdapterForType(Object type) {
		return (type == UMLFacadeAdapter.class) || super.isAdapterForType(type);
	}

	@Override
	public void initialSync(SyncDirectionKind direction, EStructuralFeature feature) {
		super.initialSync(direction, feature);

		if (stereotype != null) {
			initialSync(getFacade(), stereotype, direction, feature);
		}
	}

	/**
	 * Ensures that a {@code facade} is connected to its underlying {@code model}.
	 * 
	 * @param facade
	 *            a facade object
	 * @param model
	 *            the underlying model element
	 * @param type
	 *            the type of adapter
	 * @param adapterCreator
	 *            a function that creates the adapter, if needed
	 * @return the adapter that connects the {@code facade} with its {@code model}
	 * @param <A>
	 *            the adapter type
	 * @param <F>
	 *            the façade type
	 * @param <M>
	 *            the model element type
	 */
	protected static <A extends UMLFacadeAdapter, F extends EObject, M extends Element> A connect(F facade,
			M model, Class<A> type, BiFunction<? super F, ? super M, ? extends A> adapterCreator) {

		A result = get(model, type);
		if ((result != null) && (result.getFacade() != facade)) {
			result.dispose();
			result = null;
		}

		if (result == null) {
			result = adapterCreator.apply(facade, model);
		}

		return result;
	}
}
