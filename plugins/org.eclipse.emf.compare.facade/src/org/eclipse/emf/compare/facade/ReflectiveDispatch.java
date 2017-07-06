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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.log4j.Logger;
import org.eclipse.emf.compare.facade.FacadeAdapter.Synchronizer;

/**
 * An adapter that links a façade object with its underlying model element, coördination synchronziation of
 * changes between the two.
 *
 * @author Christian W. Damus
 */
public final class ReflectiveDispatch {
	/** The logger. */
	private static final Logger LOGGER = Logger.getLogger(ReflectiveDispatch.class);

	/** Cache of resolved methods. */
	private static LoadingCache<CacheKey, Optional<Method>> methodCache = CacheBuilder.newBuilder()
			.concurrencyLevel(4).build(CacheLoader.from(ReflectiveDispatch::resolveMethod));

	/**
	 * Not instantiable by clients.
	 */
	private ReflectiveDispatch() {
		super();
	}

	/**
	 * Safely invokes a method by reflection.
	 * 
	 * @param target
	 *            the object on which to invoke the method
	 * @param name
	 *            the name of the method to invoke on it
	 * @param arg
	 *            the arguments to pass to it
	 * @return the result of the method invocation, or {@code null} if it failed
	 */
	public static Object safeInvoke(Object target, String name, Object... arg) {
		Method method = lookupMethod(target, name, arg);

		if (method == null) {
			return null;
		} else {
			return safeInvoke(target, method, arg);
		}
	}

	/**
	 * Resolves the most specific overload of the {@code name}d method that is applicable to the given
	 * arguments.
	 * 
	 * @param owner
	 *            the owner of the method to invoke
	 * @param name
	 *            the method name
	 * @param arg
	 *            the arguments to be dispatched to the method
	 * @return the resolved method, or {@code null} if there is no method that can accept the arguments
	 */
	public static Method lookupMethod(Object owner, String name, Object... arg) {
		Class<?>[] argTypes = new Class<?>[arg.length];
		for (int i = 0; i < arg.length; i++) {
			if (arg[i] == null) {
				argTypes[i] = Void.class;
			} else {
				argTypes[i] = arg[i].getClass();
			}
		}

		return lookupMethod(owner.getClass(), name, argTypes);
	}

	/**
	 * Resolves the most specific overload of the {@code name}d method that is applicable to the given
	 * argument types.
	 * 
	 * @param owner
	 *            the owner type of the method to resolve
	 * @param name
	 *            the method name
	 * @param argType
	 *            the types of arguments to be dispatched to the method
	 * @return the resolved method, or {@code null} if there is no method that can accept the arguments
	 */
	public static Method lookupMethod(Class<?> owner, String name, Class<?>... argType) {
		return methodCache.getUnchecked(new CacheKey(owner, name, argType)).orElse(null);
	}

	/**
	 * Resolves a method as specified by its caching {@code key}.
	 * 
	 * @param key
	 *            a caching key
	 * @return the method to cache. Must not be {@code null}, but instead in that case should be the
	 *         {@link Synchronizer#PASS PASS} instance
	 */
	private static Optional<Method> resolveMethod(CacheKey key) {
		return resolveMethod(key.owner, key.methodName, key.argTypes);
	}

	/**
	 * Gets a stream over the public methods of the {@code owner} class matching a name filter and argument
	 * types.
	 * 
	 * @param owner
	 *            the owner type of the methods to retrieve
	 * @param nameFilter
	 *            predicate on method names
	 * @param argType
	 *            types of proposed arguments to be passed to the method parameters
	 * @return the applicable methods
	 */
	public static Stream<Method> getMethods(Class<?> owner, Predicate<? super String> nameFilter,
			Class<?>... argType) {

		return Stream.of(owner.getMethods()).filter(m -> nameFilter.test(m.getName()))
				.filter(m -> m.getParameterCount() == argType.length)
				.filter(m -> signatureCompatible(m.getParameterTypes(), argType));
	}

	/**
	 * Finds the most specific method of my class that has the specified {@code name} and can accept
	 * parameters of the given types.
	 * 
	 * @param owner
	 *            the owner type of the method to resolve
	 * @param name
	 *            the method name to look up
	 * @param argType
	 *            types of anticipated arguments to be passed to the resulting method
	 * @return the matching method
	 */
	static Optional<Method> resolveMethod(Class<?> owner, String name, Class<?>... argType) {
		return getMethods(owner, name::equals, argType)
				.sorted(Comparator.comparing(Method::getParameterTypes, signatureSpecificity())) //
				.findFirst();
	}

	/**
	 * Queries whether the signature of a method, as indicated by the given parameter types, is compatible
	 * with proposed arguments.
	 * 
	 * @param parameterTypes
	 *            method parameter types, in order
	 * @param argumentTypes
	 *            anticipated types of arguments
	 * @return whether the arguments types are all position-wise compatible with the parameter types
	 */
	private static boolean signatureCompatible(Class<?>[] parameterTypes, Class<?>[] argumentTypes) {
		boolean result = parameterTypes.length == argumentTypes.length;

		for (int i = 0; result && (i < parameterTypes.length); i++) {
			if (!parameterTypes[i].isAssignableFrom(argumentTypes[i])) {
				result = false;
			}
		}

		return result;
	}

	/**
	 * A comparator of method signatures, sorting a more specific signature (position-wise by parameter type)
	 * ahead of a more general signature, in the case of method overloads.
	 * 
	 * @return a method overload signature-specificity comparator
	 */
	private static Comparator<Class<?>[]> signatureSpecificity() {
		return (a, b) -> {
			if (signatureCompatible(a, b)) {
				if (signatureCompatible(b, a)) {
					return 0;
				} else {
					return +1;
				}
			} else {
				return -1;
			}
		};
	}

	/**
	 * Safely invokes a method by reflection.
	 * 
	 * @param owner
	 *            the owner of the method to invoke
	 * @param method
	 *            the method to invoke on it
	 * @param arg
	 *            the arguments to pass to it
	 * @return the result of the method invocation, or {@code null} if it failed
	 */
	public static Object safeInvoke(Object owner, Method method, Object... arg) {
		try {
			return method.invoke(owner, arg);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			LOGGER.error("Failed to invoke facade synchronization method", e); //$NON-NLS-1$
			return null;
		}
	}

	//
	// Nested types
	//

	/**
	 * Caching key that uniquely identifies a reflective method dispatch.
	 *
	 * @author Christian W. Damus
	 */
	private static final class CacheKey {
		/** The method owner. */
		final Class<?> owner;

		/** The name of the method. */
		final String methodName;

		/** The argument types to dispatch. */
		final Class<?>[] argTypes;

		/**
		 * Initializes me with my caching parameters.
		 * 
		 * @param owner
		 *            the owner type
		 * @param methodName
		 *            the name of the method to dispatch
		 * @param argTypes
		 *            the argument types to dispatch
		 */
		CacheKey(Class<?> owner, String methodName, Class<?>[] argTypes) {
			super();

			this.owner = owner;
			this.methodName = methodName;
			this.argTypes = argTypes;
		}

		/**
		 * {@inheritDoc}
		 */
		// CHECKSTYLE:OFF (generated)
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((owner == null) ? 0 : owner.hashCode());
			result = prime * result + ((methodName == null) ? 0 : methodName.hashCode());
			result = prime * result + ((argTypes == null) ? 0 : argTypes.hashCode());
			return result;
		}
		// CHECKSTYLE:ON

		/**
		 * {@inheritDoc}
		 */
		// CHECKSTYLE:OFF (generated)
		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			CacheKey other = (CacheKey)obj;
			if (owner == null) {
				if (other.owner != null) {
					return false;
				}
			} else if (!owner.equals(other.owner)) {
				return false;
			}
			if (methodName == null) {
				if (other.methodName != null) {
					return false;
				}
			} else if (!methodName.equals(other.methodName)) {
				return false;
			}
			if (argTypes == null) {
				if (other.argTypes != null) {
					return false;
				}
			} else if (!Arrays.equals(argTypes, other.argTypes)) {
				return false;
			}
			return true;
		}
		// CHECKSTYLE:ON

	}
}
