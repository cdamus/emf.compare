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
package org.eclipse.emf.compare.merge;

import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.XMIResource;

/**
 * Protocol for an algorithm that propagates XMI IDs from objects merged from one side of a comparison to
 * copies made from them for merge to the other side. This accounts for scenarios such as <em>Façade
 * Models</em> that may or may not have XMI IDs of their own but which map to one or more objects in the
 * underlying model that do likewise need to have their XMI IDs synchronized. The providers of façade models
 * then need to plug in XMI ID copiers to the {@linkplain IXMIIDCopier.Registry registry} to handle that.
 *
 * @author Christian W. Damus
 * @see IXMIIDCopier.Registry
 */
public interface IXMIIDCopier {
	/**
	 * ID of an option for {@linkplain IMergeOptionAware option-aware} mergers to specify an XMI ID copier
	 * registry for them to use to look up XMI ID copiers.
	 */
	String OPTION_XMIID_COPIER_REGISTRY = "xmiid.copier.registry"; //$NON-NLS-1$

	/**
	 * The default XMI ID copier just copies the XMI ID of the element as is, if it and its copy are contained
	 * within {@link XMIResource}s.
	 */
	IXMIIDCopier DEFAULT = new IXMIIDCopier() {

		public void copyXMIIDs(EObject originalObject, EObject copy) {
			Resource sourceResource = originalObject.eResource();
			Resource targetResource = copy.eResource();

			if ((sourceResource instanceof XMIResource) && (targetResource instanceof XMIResource)) {
				((XMIResource)targetResource).setID(copy,
						((XMIResource)sourceResource).getID(originalObject));
			}
		}
	};

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

	//
	// Nested types
	//

	/**
	 * A descriptor of an XMI ID copier which, if implemented for an Eclipse extension point, can defer
	 * creation of the copier instance and thus possibly also plug-in loading.
	 *
	 * @author Christian W. Damus
	 * @see IXMIIDCopier.Registry
	 */
	interface Descriptor {

		/**
		 * Queries whether the XMI ID copier that I describe would be applicable to an {@code object}.
		 * 
		 * @param object
		 *            an object that may have an XMI ID
		 * @return whether my copier is applicable to it
		 */
		boolean isApplicableTo(EObject object);

		/**
		 * Queries the relative rank of the XMI ID copier that I describe, to determine which of possibly
		 * multiple {@linkplain #isApplicableTo(EObject) applicable} copiers is the best match.
		 * 
		 * @return my relative rank
		 */
		int getRank();

		/**
		 * Obtains the XMI ID copier that I describe, creating it if necessary.
		 * 
		 * @return the copier
		 */
		IXMIIDCopier getXMIIDCopier();

	}

	/**
	 * A registry of XMI ID copiers.
	 *
	 * @author Christian W. Damus
	 * @see IXMIIDCopier.Descriptor
	 */
	interface Registry {
		/**
		 * The XMI ID copier registry instance.
		 */
		IXMIIDCopier.Registry INSTANCE = new Impl();

		/**
		 * Obtains the best XMI ID copier applicable to an object.
		 * 
		 * @param originalObject
		 *            the object that was copied in a merge operation
		 * @return the best XMI ID copier. This will never be {@code null} but in the worst case just the
		 *         {@linkplain IXMIIDCopier#DEFAULT default} copier
		 */
		IXMIIDCopier getXMIIDCopier(EObject originalObject);

		/**
		 * Registers an XMI ID copier descriptor.
		 * 
		 * @param copierDescriptor
		 *            the copier descriptor to register
		 */
		void add(IXMIIDCopier.Descriptor copierDescriptor);

		/**
		 * Unregisters an XMI ID copier descriptor.
		 * 
		 * @param copierDescriptor
		 *            the copier descriptor to unregister
		 */
		void remove(IXMIIDCopier.Descriptor copierDescriptor);

		//
		// Nested types
		//

		/**
		 * Simple implementation of the XMI ID copier registry.
		 *
		 * @author Christian W. Damus
		 */
		final class Impl implements IXMIIDCopier.Registry {
			/** A descriptor for the default copier. */
			private static final IXMIIDCopier.Descriptor DEFAULT_DESCRIPTOR = new Descriptor() {

				public boolean isApplicableTo(EObject object) {
					return true;
				}

				public IXMIIDCopier getXMIIDCopier() {
					return IXMIIDCopier.DEFAULT;
				}

				public int getRank() {
					return Integer.MIN_VALUE;
				}
			};

			/** Registered XMI ID copier descriptors. */
			private final CopyOnWriteArrayList<IXMIIDCopier.Descriptor> descriptors = new CopyOnWriteArrayList<>();

			/** Initializes me. */
			Impl() {
				super();
			}

			/**
			 * {@inheritDoc}
			 */
			@Override
			public IXMIIDCopier getXMIIDCopier(EObject originalObject) {
				IXMIIDCopier.Descriptor result = DEFAULT_DESCRIPTOR;

				for (IXMIIDCopier.Descriptor next : descriptors) {
					if (next.isApplicableTo(originalObject) && (next.getRank() > result.getRank())) {
						result = next;
					}
				}

				return result.getXMIIDCopier();
			}

			/**
			 * {@inheritDoc}
			 */
			@Override
			public void add(IXMIIDCopier.Descriptor copierDescriptor) {
				descriptors.addIfAbsent(copierDescriptor);
			}

			/**
			 * {@inheritDoc}
			 */
			@Override
			public void remove(IXMIIDCopier.Descriptor copierDescriptor) {
				descriptors.remove(copierDescriptor);
			}
		}
	}
}
