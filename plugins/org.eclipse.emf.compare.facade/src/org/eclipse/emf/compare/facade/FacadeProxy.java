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

import static org.eclipse.emf.compare.facade.FacadeAdapter.getUnderlyingObject;

import com.google.common.base.Predicates;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Sets;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.eclipse.emf.common.notify.impl.AdapterImpl;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;

/**
 * A factory of dynamic proxies implementing the {@link FacadeObject} protocol to adapt objects of EMF models
 * that do not natively implement this protocol.
 *
 * @author Christian W. Damus
 */
public final class FacadeProxy {
	/** Predicate matching classes that are not EObject. */
	private static final Predicate<Class<?>> NOT_EOBJECT_CLASS;

	/** Cache of interfaces implemented by EClasses. */
	private static final LoadingCache<Class<?>, Class<?>[]> INTERFACE_CACHE = CacheBuilder.newBuilder()
			.weakKeys().build(new CacheLoader<Class<?>, Class<?>[]>() {
				/**
				 * {@inheritDoc}
				 */
				@Override
				public Class<?>[] load(Class<?> key) throws Exception {
					return computeProxiedInterfaces(key);
				}
			});

	static {
		Predicate<Class<?>> isEObjectClass = Predicates.equalTo(EObject.class);
		NOT_EOBJECT_CLASS = isEObjectClass.negate();
	}

	/**
	 * Not instantiable by clients.
	 */
	private FacadeProxy() {
		super();
	}

	/**
	 * Creates or obtains the dynamic façade proxy for an {@code object} that does not natively implement the
	 * {@link FacadeObject} protocol. The resulting proxy implements all of the other interfaces of the
	 * original object in addition to {@code FacadeObject}.
	 * 
	 * @param object
	 *            an EMF object, or {@code null}
	 * @return its dynamic {@link FacadeObject} proxy, which may be the {@code object} if it already
	 *         implements the {@link FacadeObject} protocol or even {@code null} if the {@code object} is
	 *         {@code null}
	 */
	public static FacadeObject createProxy(EObject object) {
		FacadeObject result;

		if (object == null) {
			result = null;
		} else if (object instanceof FacadeObject) {
			result = (FacadeObject)object;
		} else {
			Impl impl = (Impl)EcoreUtil.getExistingAdapter(object, Impl.class);

			if (impl != null) {
				result = impl.getProxy();
			} else {
				impl = new Impl(object);
				object.eAdapters().add(impl); // There can be only one

				Class<?>[] interfaces = getProxiedInterfaces(object.getClass());

				result = (FacadeObject)Proxy.newProxyInstance(object.getClass().getClassLoader(), interfaces,
						impl);
				impl.setProxy(result);
			}
		}

		return result;
	}

	/**
	 * Gets a façade's actual own resource if it's in a resource, otherwise its underlying object's resource.
	 * 
	 * @param facade
	 *            a façade object
	 * @return the best resource available for it
	 */
	static Resource eResource(EObject facade) {
		Resource result = facade.eResource();

		if (result == null) {
			EObject underlying = getUnderlyingObject(facade);
			if (underlying != null) {
				result = underlying.eResource();
			}
		}

		return result;
	}

	/**
	 * Gets a façade's actual direct resource if it's contained in a resource or its own, otherwise its
	 * underlying object's direct resource.
	 * 
	 * @param facade
	 *            a façade object
	 * @return the best resource available for it
	 */
	static Resource.Internal eDirectResource(InternalEObject facade) {
		Resource.Internal result = facade.eDirectResource();

		// Be careful not to return a fake direct resource if the façade is actually
		// indirectly contained in a resource in the proper way
		if ((result == null) && (facade.eResource() == null)) {
			EObject underlying = getUnderlyingObject(facade);
			if (underlying instanceof InternalEObject) {
				result = ((InternalEObject)underlying).eDirectResource();
			}
		}

		return result;
	}

	/**
	 * Obtains all of the interfaces implemented by an Ecore class except for {@link EObject}.
	 * 
	 * @param eClass
	 *            an Ecore class
	 * @return its non-{@link EObject} interfaces
	 */
	private static Class<?>[] getProxiedInterfaces(Class<?> eClass) {
		// Our cache loader cannot fail
		return INTERFACE_CACHE.getUnchecked(eClass);
	}

	/**
	 * Obtains all of the interfaces implemented by a Java class except for {@link EObject}.
	 * 
	 * @param clazz
	 *            a Java class
	 * @return its non-{@link EObject} interfaces
	 */
	private static Class<?>[] computeProxiedInterfaces(Class<?> clazz) {
		Set<Class<?>> result = Sets.newHashSet();

		collectAllInterfacesExceptEObject(clazz, result);

		// This one is instead of EObject
		result.add(FacadeObject.class);

		return result.toArray(new Class<?>[result.size()]);
	}

	/**
	 * Recursively collects all of the interfaces implemented by a Java class except for {@link EObject}.
	 * 
	 * @param clazz
	 *            a Java class
	 * @param result
	 *            accumulator of its non-{@link EObject} interfaces
	 */
	private static void collectAllInterfacesExceptEObject(Class<?> clazz, Set<Class<?>> result) {
		if (clazz.isInterface()) {
			if (NOT_EOBJECT_CLASS.test(clazz)) {
				result.add(clazz);
			}
			return;
		}

		// It's not an interface but a class
		Stream.of(clazz.getInterfaces()).filter(NOT_EOBJECT_CLASS).forEach(result::add);

		// Look up the hierarchy for inherited interfaces
		Class<?> superclass = clazz.getSuperclass();
		if (superclass != null) {
			collectAllInterfacesExceptEObject(superclass, result);
		}
	}

	//
	// Nested types
	//

	/**
	 * Invocation handler for the dynamic façade proxy that is attached to the façade object as an adapter to
	 * ensure at most one proxy exists.
	 *
	 * @author Christian W. Damus
	 */
	private static final class Impl extends AdapterImpl implements InvocationHandler {
		/** The façade object that I adapt to the {@link FacadeObject} protocol. */
		private final EObject object;

		/** The dynamic implementation of the {@link FacadeObject} protocol. */
		private FacadeObject facadeProxy;

		/**
		 * Initializes me with the object that I adapt to the {@link FacadeObject} protocol.
		 * 
		 * @param object
		 *            the object that I proxy
		 */
		Impl(EObject object) {
			super();

			this.object = object;
		}

		/**
		 * Obtain the façade that is the proxy that I handle.
		 * 
		 * @return the façade proxy
		 */
		FacadeObject getProxy() {
			return facadeProxy;
		}

		/**
		 * Set the façade that is the proxy that I handle.
		 * 
		 * @param proxy
		 *            the façade proxy
		 */
		void setProxy(FacadeObject proxy) {
			this.facadeProxy = proxy;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean isAdapterForType(Object type) {
			return type == Impl.class;
		}

		/**
		 * {@inheritDoc}
		 */
		public Object invoke(Object proxy, Method method, Object[] args) throws Exception {
			Object result;

			if (method.getDeclaringClass() == Object.class) {
				switch (method.getName()) {
					case "equals": //$NON-NLS-1$
						result = Boolean.valueOf(proxy == args[0]);
						break;
					case "hashCode": //$NON-NLS-1$
						result = Integer.valueOf(System.identityHashCode(proxy));
						break;
					case "toString": //$NON-NLS-1$
						result = object.toString();
						break;
					default:
						// Other Object methods are not proxied
						throw new InternalError("Method unexpectedly proxied: " + method); //$NON-NLS-1$
				}
			} else if (method.getDeclaringClass() == FacadeObject.class) {
				switch (method.getName()) {
					case "getUnderlyingElement": //$NON-NLS-1$
						result = getUnderlyingObject(object);
						break;
					case "getFacadeAdapter": //$NON-NLS-1$
						result = FacadeAdapter.get(object, FacadeAdapter.class);
						break;
					default:
						throw new NoSuchMethodError(method.toString());
				}
			} else if (method.getDeclaringClass() == InternalEObject.class) {
				switch (method.getName()) {
					case "eDirectResource": //$NON-NLS-1$
						result = eDirectResource((InternalEObject)object);
						break;
					default:
						result = method.invoke(object, args);
				}
			} else if (method.getDeclaringClass() == EObject.class) {
				switch (method.getName()) {
					case "eResource": //$NON-NLS-1$
						result = eResource(object);
						break;
					default:
						result = method.invoke(object, args);
				}
			} else {
				// Delegate the rest
				result = method.invoke(object, args);
			}

			return result;
		}
	}
}
