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
package org.eclipse.emf.compare.uml2.tests;

import com.google.common.base.Objects;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.common.notify.impl.AdapterImpl;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EcoreUtil;

/**
 * Opaque graph of transitive reachability from resources to other resources.
 *
 * @author Christian W. Damus
 */
class ResourceGraph extends AdapterImpl {

	/** Which resources does a resource transitively reach to? */
	private final Multimap<URI, URI> reachTo = HashMultimap.create();

	/** Which resources is a resource transitively reachable from? */
	private final Multimap<URI, URI> reachFrom = HashMultimap.create();

	private final boolean localOnly;

	/**
	 * Initializes me.
	 * 
	 * @param localOnly
	 *            whether only referenced resources stored locally to the referencing should be tracked. In
	 *            practise, this is a resource that shares a common parent URI of at least one segment's
	 *            length
	 */
	public ResourceGraph(boolean localOnly) {
		super();

		this.localOnly = localOnly;
	}

	/**
	 * Queries whether the resource graph is connected (possibly indirectly) {@code from} one resource
	 * {@code to} another.
	 * 
	 * @param from
	 *            the source resource URI
	 * @param to
	 *            the destination resource URI
	 * @return whether the destination is reachable by cross-references from the source
	 */
	public boolean reaches(URI from, URI to) {
		return reachTo.containsEntry(from, to);
	}

	/**
	 * Queries whether the resource graph is connected (possibly indirectly) {@code from} one resource
	 * {@code to} another.
	 * 
	 * @param from
	 *            the source resource
	 * @param to
	 *            the destination resource
	 * @return whether the destination is reachable by cross-references from the source
	 */
	public boolean reaches(Resource from, Resource to) {
		return reaches(from.getURI(), to.getURI());
	}

	/**
	 * Queries whether the resource graph is connected (possibly indirectly) {@code from} any of a set of
	 * resources {@code to} another.
	 * 
	 * @param from
	 *            the source resources
	 * @param to
	 *            the destination resource
	 * @return whether the destination is reachable by cross-references from the source
	 */
	public boolean anyReaches(Set<Resource> from, Resource to) {
		boolean result = false;

		for (Resource next : from) {
			result = reaches(next, to);
			if (result) {
				break;
			}
		}

		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void notifyChanged(Notification msg) {
		if (msg.isTouch()) {
			return;
		}

		if (msg.getNotifier() instanceof ResourceSet) {
			switch (msg.getFeatureID(ResourceSet.class)) {
				case ResourceSet.RESOURCE_SET__RESOURCES:
					switch (msg.getEventType()) {
						case Notification.ADD:
							handleResourceAdded((Resource)msg.getNewValue());
							break;
						case Notification.ADD_MANY:
							for (Object next : (Collection<?>)msg.getNewValue()) {
								handleResourceAdded((Resource)next);
							}
							break;
						default:
							// Pass
							break;
					}
					break;
				default:
					// Pass
					break;
			}
		} else if (msg.getNotifier() instanceof Resource) {
			switch (msg.getFeatureID(Resource.class)) {
				case Resource.RESOURCE__IS_LOADED:
					if (!msg.getOldBooleanValue() && msg.getNewBooleanValue()) {
						handleResourceLoaded((Resource)msg.getNotifier());
					}
					break;
				default:
					// Pass
					break;
			}
		}
	}

	public void addAdapter(Notifier notifier) {
		EList<Adapter> adapters = notifier.eAdapters();
		if (!adapters.contains(this)) {
			adapters.add(this);

			if (notifier instanceof ResourceSet) {
				// Discover this resource set (defensive copy)
				for (Resource next : ImmutableList.copyOf(((ResourceSet)notifier).getResources())) {
					addAdapter(next);
				}
			} else if (notifier instanceof Resource) {
				// Discover this resource
				Resource resource = (Resource)notifier;
				if (resource.isLoaded()) {
					handleResourceLoaded(resource);
				}
			}
		}
	}

	protected void handleResourceAdded(Resource resource) {
		addAdapter(resource);
	}

	protected void handleResourceLoaded(Resource resource) {
		URI referencer = resource.getURI();

		// This much is trivially true
		reachTo.put(referencer, referencer);
		mapReach(referencer, referencer);

		for (Iterator<EObject> iter = EcoreUtil.getAllProperContents(resource, false); iter.hasNext();) {
			EObject next = iter.next();
			for (EObject xref : next.eCrossReferences()) {
				URI uri = EcoreUtil.getURI(xref);
				if (uri != null) {
					uri = uri.trimFragment();
					if (!localOnly || isLocal(referencer, uri)) {
						if (reachTo.put(referencer, uri)) {
							// New mapping
							mapReach(referencer, uri);
						}
					}
				}
			}
		}
	}

	protected boolean isLocal(URI referencer, URI referenced) {
		boolean result = false;

		if (referencer == referenced) {
			result = true;
		} else if (Objects.equal(referencer.scheme(), referenced.scheme()) && referencer.isHierarchical()
				&& referenced.isHierarchical()) {

			if (referenced.isRelative()) {
				// Has to be local if it's a relative reference
				result = true;
			} else if (referencer.hasAuthority()) {
				// Same authority is good enough (e.g., http:// server, bundleresource:// bundle,
				// pathmap:// variable)
				result = Objects.equal(referencer.authority(), referenced.authority());
			} else if ((referencer.segmentCount() > 0) && (referenced.segmentCount() > 0)) {
				// Require same root of the file system (e.g., both platform:/resource)
				result = Objects.equal(referencer.segment(0), referenced.segment(0));
			}
		}

		return result;
	}

	private void mapReach(URI referencer, URI referenced) {
		reachFrom.put(referenced, referencer);

		for (URI next : reachFrom.get(referencer)) {
			if (reachTo.put(next, referenced)) {
				// Recursive again
				mapReach(next, referenced);
			}
		}
	}
}
