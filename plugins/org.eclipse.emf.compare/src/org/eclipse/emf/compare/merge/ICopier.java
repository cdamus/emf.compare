/*******************************************************************************
 * Copyright (c) 2017 Christian W. Damus and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Christian W. Damus - initial API and implementation
 *******************************************************************************/
package org.eclipse.emf.compare.merge;

import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.emf.compare.utils.EMFCompareCopier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.XMIResource;
import org.eclipse.emf.ecore.xmi.XMLResource;

/**
 * Protocol for an algorithm that copies objects (to create merge results) and propagates XMI IDs from objects
 * merged from one side of a comparison to copies made from them for merge to the other side. This accounts
 * for scenarios such as <em>Façade Models</em> that may or may not have XMI IDs of their own but which map to
 * one or more objects in the underlying model that do likewise need to have their XMI IDs synchronized. The
 * providers of façade models then need to plug in copiers to the {@linkplain ICopier.Registry registry} to
 * handle that.
 *
 * @author Christian W. Damus
 * @see ICopier.Registry
 * @since 3.6
 */
public interface ICopier {
	/**
	 * ID of an option for {@linkplain IMergeOptionAware option-aware} mergers to specify a copier registry
	 * for them to use to look up copiers.
	 */
	String OPTION_COPIER_REGISTRY = "copier.registry"; //$NON-NLS-1$

	/**
	 * The default copier just uses the basic {@link EMFCompareCopier} and copies the XMI ID of the element as
	 * is, if it and its copy are contained within {@link XMIResource}s.
	 */
	ICopier DEFAULT = new ICopier() {

		/**
		 * {@inheritDoc}
		 */
		public EObject copy(EObject originalObject) {
			EcoreUtil.Copier copier = new EMFCompareCopier();
			return copier.copy(originalObject);
		}

		/**
		 * {@inheritDoc}
		 */
		public void copyXMIIDs(EObject originalObject, EObject copy) {
			Resource sourceResource = originalObject.eResource();
			Resource targetResource = copy.eResource();

			if ((sourceResource instanceof XMIResource) && (targetResource instanceof XMIResource)) {
				((XMIResource)targetResource).setID(copy,
						((XMIResource)sourceResource).getID(originalObject));
			}
		}

		/**
		 * {@inheritDoc}
		 */
		public String getIdentifier(EObject object) {
			String result = null;

			if (object != null) {
				if (object.eIsProxy()) {
					result = ((InternalEObject)object).eProxyURI().fragment();
				} else {
					Resource resource = object.eResource();
					if (resource instanceof XMLResource) {
						result = ((XMLResource)resource).getID(object);
					}

					if (result == null) {
						// Even if we got a resource, it may have been from a
						// a façade, in which case it wouldn't have an ID for it
						result = EcoreUtil.getID(object);
					}
				}
			}

			return result;

		}
	};

	/**
	 * Creates a copy of an object.
	 * 
	 * @param originalObject
	 *            the object to copy
	 * @return the copy
	 */
	EObject copy(EObject originalObject);

	/**
	 * Copy the XMI IDs for an original object that was copied, to the {@code copy}. This may be complicated
	 * by such factors as <em>Façade Model</em> which are non-persistent fronts for the underlying resource,
	 * where not only are XMI IDs of the façade objects not relevant (only the underlying objects are
	 * persisted) but there may be multiple persistent objects underlying a single façade object.
	 * 
	 * @param originalObject
	 *            an objects from one side of the comparison that was merged to the other side
	 * @param copy
	 *            the copy of the original object, which effects the merge
	 */
	void copyXMIIDs(EObject originalObject, EObject copy);

	/**
	 * Obtains the unique identifier of an {@code object}, whether it be an
	 * {@linkplain XMLResource#getID(EObject) XML ID} or an {@linkplain EcoreUtil#getID(EObject) intrinsic ID}
	 * or something else such as may be computed for façade models, of an {@code object}.
	 * 
	 * @param object
	 *            an object in one of the input models to the match phase of a comparison
	 * @return the {@code object}'s identifier, or {@code null} if it has none
	 */
	String getIdentifier(EObject object);

	//
	// Nested types
	//

	/**
	 * A descriptor of an EMF Compare copier which, if implemented for an Eclipse extension point, can defer
	 * creation of the copier instance and thus possibly also plug-in loading.
	 *
	 * @author Christian W. Damus
	 * @see ICopier.Registry
	 */
	interface Descriptor {

		/**
		 * Queries whether the copier that I describe would be applicable to an {@code object}.
		 * 
		 * @param object
		 *            an object that is to be copied in a merge operation
		 * @return whether my copier is applicable to it
		 */
		boolean isApplicableTo(EObject object);

		/**
		 * Queries the relative rank of the copier that I describe, to determine which of possibly multiple
		 * {@linkplain #isApplicableTo(EObject) applicable} copiers is the best match.
		 * 
		 * @return my relative rank
		 */
		int getRank();

		/**
		 * Obtains the copier that I describe, creating it if necessary.
		 * 
		 * @return the copier
		 */
		ICopier getCopier();

	}

	/**
	 * A registry of EMF Compare copiers.
	 *
	 * @author Christian W. Damus
	 * @see ICopier.Descriptor
	 */
	interface Registry {
		/**
		 * The copier registry instance.
		 */
		ICopier.Registry INSTANCE = new Impl();

		/**
		 * Obtains the best copier applicable to an object.
		 * 
		 * @param originalObject
		 *            the object that was copied in a merge operation
		 * @return the best copier. This will never be {@code null} but in the worst case just the
		 *         {@linkplain ICopier#DEFAULT default} copier
		 */
		ICopier getCopier(EObject originalObject);

		/**
		 * Registers a copier descriptor.
		 * 
		 * @param copierDescriptor
		 *            the copier descriptor to register
		 */
		void add(ICopier.Descriptor copierDescriptor);

		/**
		 * Unregisters a copier descriptor.
		 * 
		 * @param copierDescriptor
		 *            the copier descriptor to unregister
		 */
		void remove(ICopier.Descriptor copierDescriptor);

		//
		// Nested types
		//

		/**
		 * Simple implementation of the EMF Compare copier registry.
		 *
		 * @author Christian W. Damus
		 */
		final class Impl implements ICopier.Registry {
			/** A descriptor for the default copier. */
			private static final ICopier.Descriptor DEFAULT_DESCRIPTOR = new Descriptor() {

				public boolean isApplicableTo(EObject object) {
					return true;
				}

				public ICopier getCopier() {
					return ICopier.DEFAULT;
				}

				public int getRank() {
					return Integer.MIN_VALUE;
				}
			};

			/** Registered copier descriptors. */
			private final CopyOnWriteArrayList<ICopier.Descriptor> descriptors = new CopyOnWriteArrayList<>();

			/** Initializes me. */
			Impl() {
				super();
			}

			/**
			 * {@inheritDoc}
			 */
			@Override
			public ICopier getCopier(EObject originalObject) {
				ICopier.Descriptor result = DEFAULT_DESCRIPTOR;

				if (originalObject != null) {
					for (ICopier.Descriptor next : descriptors) {
						if (next.isApplicableTo(originalObject) && (next.getRank() > result.getRank())) {
							result = next;
						}
					}
				}

				return result.getCopier();
			}

			/**
			 * {@inheritDoc}
			 */
			@Override
			public void add(ICopier.Descriptor copierDescriptor) {
				descriptors.addIfAbsent(copierDescriptor);
			}

			/**
			 * {@inheritDoc}
			 */
			@Override
			public void remove(ICopier.Descriptor copierDescriptor) {
				descriptors.remove(copierDescriptor);
			}
		}
	}
}
