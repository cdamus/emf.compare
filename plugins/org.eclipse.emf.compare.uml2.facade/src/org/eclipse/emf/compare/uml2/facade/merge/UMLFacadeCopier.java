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
package org.eclipse.emf.compare.uml2.facade.merge;

import static java.util.Objects.requireNonNull;
import static org.eclipse.emf.compare.facade.FacadeAdapter.getUnderlyingObject;
import static org.eclipse.emf.compare.utils.Optionals.ifAbsent;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.impl.AdapterImpl;
import org.eclipse.emf.compare.facade.FacadeAdapter;
import org.eclipse.emf.compare.facade.internal.merge.FacadeCopier;
import org.eclipse.emf.compare.utils.ReflectiveDispatch;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.Stereotype;
import org.eclipse.uml2.uml.util.UMLUtil;

/**
 * An EMF Compare copier for façades on UML elements.
 *
 * @author Christian W. Damus
 */
@SuppressWarnings("restriction")
public class UMLFacadeCopier extends FacadeCopier {

	/**
	 * The façade object whose XMI ID copying is currently being processed.
	 */
	private ThreadLocal<Pair> copyContext = new ThreadLocal<>();

	/**
	 * Initializes me.
	 */
	public UMLFacadeCopier() {
		super();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void copyXMIIDs(FacadeAdapter originalAdapter, FacadeAdapter copy) {
		// First, cover the principal underlying objects
		super.copyXMIIDs(originalAdapter, copy);

		EObject originalFacade = originalAdapter.getFacade();
		EObject copyFacade = copy.getFacade();
		EObject originalUnder = originalAdapter.getUnderlyingElement();
		EObject copyUnder = copy.getUnderlyingElement();

		if ((originalUnder instanceof Element) && (copyUnder instanceof Element)) {
			Element originalElement = (Element)originalUnder;
			Element copyElement = (Element)copyUnder;

			// Then, stereotype applications
			handleStereotypeApplications(originalElement, copyElement);

			final Pair oldContext = pushCopyContext(originalFacade, copyFacade);
			try {
				// And, finally, other related objects
				getRelatedElements(originalFacade, originalElement, copyFacade, copyElement)
						.forEach(Pair::delegateCopy);
			} finally {
				popCopyContext(originalFacade, oldContext);
			}
		}
	}

	/**
	 * For UML elements that have had their XMI IDs copied, ensures the same for their corresponding
	 * stereotype applications.
	 * 
	 * @param original
	 *            the original element merged to the other side
	 * @param copy
	 *            the copy resulting from the merge of the {@code original}
	 */
	protected void handleStereotypeApplications(Element original, Element copy) {
		for (EObject next : original.getStereotypeApplications()) {
			Stereotype stereotype = UMLUtil.getStereotype(next);
			if (stereotype != null) {
				// This is in another resource set, so the UML stereotype identity will be
				// different
				Stereotype corresponding = copy.getApplicableStereotype(stereotype.getQualifiedName());
				if (corresponding != null) {
					EObject other = copy.getStereotypeApplication(corresponding);
					if (other != null) {
						delegateCopyXMIIDs(next, other);
					}
				}
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void delegateCopyXMIIDs(EObject originalObject, EObject copy) {
		super.delegateCopyXMIIDs(originalObject, copy);

		if ((originalObject instanceof Element) && (copy instanceof Element)) {
			Element originalElement = (Element)originalObject;
			Element copyElement = (Element)copy;

			// Then, stereotype applications
			handleStereotypeApplications(originalElement, copyElement);
		}
	}

	/**
	 * Computes a stream of corresponding pairs of objects related to the merged model elements, which
	 * themselves would have been merged and so must have the same XMI IDs. The root definition of this method
	 * returns an empty stream; subclasses should just override it.
	 * 
	 * @param original
	 *            the original façade (source of the merge)
	 * @param originalUML
	 *            the original façade's underlying UML element
	 * @param copy
	 *            the copy façade (target of the merge)
	 * @param copyUML
	 *            the copy façade's underlying UML element
	 * @return pairs of related elements
	 */
	@SuppressWarnings("resource") // The entire point is to return the stream
	protected Stream<Pair> getRelatedElements(EObject original, Element originalUML, EObject copy,
			Element copyUML) {

		// Note that we don't have to worry about this calling ourselves in an endless loop
		// because
		// 1. the ReflectiveDispatch utility only invokes public methods, and
		// 2. if the subclass declares this same signature as a public override then we
		// won't be running this code anyways
		@SuppressWarnings("unchecked")
		Stream<Pair> result = (Stream<Pair>)ReflectiveDispatch.safeInvoke(this, "getRelatedElements", //$NON-NLS-1$
				original, originalUML, copy, copyUML);

		if (result == null) {
			// Usually because the subclass does not define an applicable method signature
			result = Stream.empty();
		}

		return result;
	}

	/**
	 * Creates a pair. If the {@code copy} is {@code null}, then the {@code original} is enqueued for a
	 * deferred XMI ID copy when the copy eventually is created indirectly by further merging of features of
	 * the façade corresponding to the {@code original}. If the {@code original} is {@code null}, then there
	 * is not nor ever will be any need to copy XMI IDs, so in this case nothing is posted for deferred
	 * processing.
	 * 
	 * @param original
	 *            the original
	 * @param copy
	 *            the copy
	 * @return the pair, or {@code null} if either of the proposed pair is {@code null}
	 */
	protected final Pair pair(EObject original, EObject copy) {
		if (original == null) {
			return null;
		}

		if (copy == null) {
			deferXMIIDCopy(original);
			return null;
		}

		return new Pair(original, copy);
	}

	/**
	 * Optionally creates a pair. If the {@code copy} is {@linkplain Optional#isPresent() absent}, then the
	 * {@code original} is enqueued for a deferred XMI ID copy when the copy eventually is created indirectly
	 * by further merging of features of the façade corresponding to the {@code original}. If the
	 * {@code original} is absent, then there is not nor ever will be any need to copy XMI IDs, so in this
	 * case nothing is posted for deferred processing.
	 * 
	 * @param original
	 *            an optional original
	 * @param copy
	 *            an optional copy
	 * @return the pair if they are present
	 */
	protected final Optional<Pair> pair(Optional<? extends EObject> original,
			Optional<? extends EObject> copy) {

		return original.flatMap(o -> ifAbsent(copy.map(c -> pair(o, c)), () -> deferXMIIDCopy(o)));
	}

	/**
	 * Coerce an optional an a stream of at most one element.
	 * 
	 * @param optional
	 *            an optional
	 * @return a stream of one or zero elements according to the {@code optional}
	 * @param <T>
	 *            the element type
	 */
	protected final <T> Stream<T> streamOf(Optional<T> optional) {
		return optional.map(Stream::of).orElseGet(Stream::empty);
	}

	/**
	 * Defers the XMI ID copying of an {@code object} until the next time the merge operation updates its
	 * corresponding copy. This accounts for the fact that a merge of an object is completed in stages, and at
	 * each stage new UML elements may be created in the underlying model that need to have their XMI IDs
	 * synchronized, but the merge knows nothing about these consequences: it is not directly responsible for
	 * creating (indirectly) what are effectively copies as it is for the case of the façade objects that it
	 * merges.
	 * 
	 * @param object
	 *            an object related to the underlying UML model element of the source façade of a merge that
	 *            does not yet have a counterpart in the merge result to which the XMI ID can be propagated
	 */
	private void deferXMIIDCopy(EObject object) {
		Pair context = getCopyContext();
		if (context != null) {
			XMIIDCopyAdapter adapter = XMIIDCopyAdapter.getInstance(context);
			adapter.addDeferredXMIIDCopy(object);
		}
	}

	private Pair getCopyContext() {
		return copyContext.get();
	}

	/**
	 * Push the context of the façade objects being merged into the thread-local storage for retrieval during
	 * scanning of related elements that should have XMI IDs synchronized.
	 * 
	 * @param object
	 *            the source façade object of the merge
	 * @param copy
	 *            the target façade object of the merge
	 * @return the previous context, to be stored on the program stack for later
	 *         {@linkplain #popCopyContext(EObject, Pair) restoration}
	 * @see #popCopyContext(EObject, Pair)
	 */
	private Pair pushCopyContext(EObject object, EObject copy) {
		Pair result = getCopyContext();
		copyContext.set(new Pair(object, copy));
		return result;
	}

	/**
	 * Restore the previous context of the façade objects being merged in the thread-local storage.
	 * 
	 * @param object
	 *            the source façade object of the merge that is completing processing
	 * @param previous
	 *            the context to restore (may be {@code null} if none)
	 * @throws IllegalArgumentException
	 *             if the specified {@code object} is not the original of the current copy context, which
	 *             means that this pop would corrupt the context
	 * @see #pushCopyContext(EObject, EObject)
	 */
	private void popCopyContext(EObject object, Pair previous) {
		Preconditions.checkArgument(getCopyContext().original == object, "Popping invalid copy context"); //$NON-NLS-1$
		if (previous == null) {
			copyContext.remove();
		} else {
			copyContext.set(previous);
		}
	}

	//
	// Nested types
	//

	/**
	 * A pair of elements related to the merged elements.
	 *
	 * @author Christian W. Damus
	 */
	protected final class Pair {
		/** An object from the source side of the merge. */
		private final EObject original;

		/** The corresponding object on the target side of the merge. */
		private final EObject copy;

		/**
		 * Initializes me.
		 * 
		 * @param original
		 *            object from the source side of the merge
		 * @param copy
		 *            the corresponding object on the target side of the merge
		 * @throws NullPointerException
		 *             if either argument is {@code null}
		 */
		public Pair(EObject original, EObject copy) {
			super();

			this.original = requireNonNull(original);
			this.copy = requireNonNull(copy);
		}

		/**
		 * Copies the XMI ID of my pair.
		 */
		void delegateCopy() {
			delegateCopyXMIIDs(getOriginal(), getCopy());
		}

		/**
		 * Obtains the original object of the pair.
		 * 
		 * @return the original
		 */
		public EObject getOriginal() {
			return original;
		}

		/**
		 * Obtains the copy object of the pair.
		 * 
		 * @return the copy
		 */
		public EObject getCopy() {
			return copy;
		}

		/**
		 * Obtains the XMI ID copier that owns me.
		 * 
		 * @return my owner
		 */
		UMLFacadeCopier getXMIIDCopier() {
			return UMLFacadeCopier.this;
		}
	}

	/**
	 * An adapter attached to the merge target to react to future merges by processing deferred XMI ID copies
	 * of related elements in case their corresponding targets are now available to perform that copy.
	 *
	 * @author Christian W. Damus
	 */
	private static final class XMIIDCopyAdapter extends AdapterImpl {
		/** The façade XMI ID copy context for which I process deferrals. */
		private final Pair copyContext;

		/**
		 * Objects related to the source side of the copy context that did not have correspondents on the
		 * target side at the time of first trying to propagate their XMI IDs.
		 */
		private Set<EObject> deferrals = Sets.newHashSet();

		/**
		 * Initialize me with my façade XMI ID copy context. I attach myself as an adapter to the target
		 * (copy) side of this context.
		 * 
		 * @param copyContext
		 *            my copy context
		 */
		private XMIIDCopyAdapter(Pair copyContext) {
			super();

			this.copyContext = copyContext;
			copyContext.copy.eAdapters().add(this);
		}

		/**
		 * Obtains the existing adapter, or creates a new one, for the given façade XMI ID copy context.
		 * 
		 * @param copyContext
		 *            the façade XMI ID copy context
		 * @return its XMI ID copy deferral adapter
		 */
		static XMIIDCopyAdapter getInstance(Pair copyContext) {
			XMIIDCopyAdapter result = (XMIIDCopyAdapter)EcoreUtil.getExistingAdapter(copyContext.copy,
					XMIIDCopyAdapter.class);
			if (result == null) {
				result = new XMIIDCopyAdapter(copyContext);
			}
			return result;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean isAdapterForType(Object type) {
			return type == XMIIDCopyAdapter.class;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void notifyChanged(Notification msg) {
			if (msg.isTouch()) {
				return;
			}

			// First, detach ourselves so that we do not react to further changes
			getTarget().eAdapters().remove(this);

			Element originalUML = (Element)getUnderlyingObject(copyContext.original);
			Element copyUML = (Element)getUnderlyingObject(copyContext.copy);

			// Process deferrals
			UMLFacadeCopier copier = copyContext.getXMIIDCopier();
			Stream<Pair> related = copier.getRelatedElements(copyContext.original, originalUML,
					copyContext.copy, copyUML);
			related.filter(pair -> deferrals.remove(pair.original)).forEach(Pair::delegateCopy);
		}

		/**
		 * Adds the source side related object for which the propagation of its XMI ID needs to be deferred in
		 * the context of my contextual façade object.
		 * 
		 * @param object
		 *            a source-side related object to defer
		 */
		void addDeferredXMIIDCopy(EObject object) {
			deferrals.add(object);
		}
	}
}
