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
package org.eclipse.emf.compare.tests.framework.junit;

import com.google.common.base.Function;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.common.util.WrappedException;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

/**
 * A rule that closes any {@link AutoCloseable} resource held by the test instance after execution of each
 * test.
 *
 * @author Christian W. Damus
 */
public class AutoCloseRule implements MethodRule {

	private static final LoadingCache<Class<?>, List<Field>> AUTO_CLOSEABLES = CacheBuilder.newBuilder()
			.build(new CacheLoader<Class<?>, List<Field>>() {
				/**
				 * {@inheritDoc}
				 */
				@Override
				public List<Field> load(Class<?> key) throws Exception {
					ImmutableList.Builder<Field> result = ImmutableList.builder();

					for (Field next : key.getDeclaredFields()) {
						if (isAutocloseable(next)) {
							next.setAccessible(true);
							result.add(next);
						}
					}

					Class<?> superclass = key.getSuperclass();
					if ((superclass != null) && (superclass != Object.class)) {
						result.addAll(AUTO_CLOSEABLES.get(superclass));
					}

					return result.build();
				}
			});

	/**
	 * Initializes me.
	 */
	public AutoCloseRule() {
		super();
	}

	/**
	 * {@inheritDoc}
	 */
	public Statement apply(final Statement base, FrameworkMethod method, final Object target) {
		return new Statement() {

			@Override
			public void evaluate() throws Throwable {
				List<Field> autocloseableFields = AUTO_CLOSEABLES.get(target.getClass());

				try {
					base.evaluate();
				} finally {
					try {
						for (AutoCloseable next : Iterables
								.concat(Iterables.transform(autocloseableFields, autocloseablesOf(target)))) {

							next.close();
						}
					} catch (WrappedException e) {
						// Unwrap
						throw e.exception();
					}
				}
			}
		};
	}

	static boolean isAutocloseable(Field field) {
		Type type = field.getGenericType();
		return isAutocloseable(type);
	}

	static boolean isSupertype(Class<?> supertype, Type subtype) {
		return (subtype instanceof Class<?>) && supertype.isAssignableFrom((Class<?>)subtype);
	}

	static boolean isAutocloseable(Type type) {
		boolean result;

		if (type instanceof Class<?>) {
			Class<?> classType = (Class<?>)type;
			if (classType.isArray()) {
				result = isAutocloseable(classType.getComponentType());
			} else {
				result = isSupertype(AutoCloseable.class, classType);
			}
		} else if (type instanceof ParameterizedType) {
			ParameterizedType parameterizedType = (ParameterizedType)type;
			Type rawType = parameterizedType.getRawType();

			if (isSupertype(Iterable.class, rawType)) {
				result = isAutocloseable(parameterizedType.getActualTypeArguments()[0]);
			} else if (isSupertype(Map.class, rawType)) {
				result = isAutocloseable(parameterizedType.getActualTypeArguments()[1]);
			} else {
				// TODO: There are other container types
				result = false;
			}
		} else {
			result = false;
		}

		return result;
	}

	static Iterable<AutoCloseable> getAutocloseables(Object owner, Field field) throws Exception {
		Iterable<AutoCloseable> result;

		Object value = field.get(owner);
		if (value == null) {
			result = Collections.emptyList();
		} else if (value instanceof AutoCloseable) {
			result = Collections.singletonList((AutoCloseable)value);
		} else if (value instanceof Object[]) {
			result = Iterables.filter(Arrays.asList((Object[])value), AutoCloseable.class);
		} else if (value instanceof Iterable<?>) {
			result = Iterables.filter((Iterable<?>)value, AutoCloseable.class);
		} else if (value instanceof Map<?, ?>) {
			result = Iterables.filter(((Map<?, ?>)value).values(), AutoCloseable.class);
		} else {
			result = Collections.emptyList();
		}

		return result;
	}

	static Function<Field, Iterable<AutoCloseable>> autocloseablesOf(final Object owner) {
		return new Function<Field, Iterable<AutoCloseable>>() {
			/**
			 * {@inheritDoc}
			 */
			public Iterable<AutoCloseable> apply(Field input) {
				try {
					return getAutocloseables(owner, input);
				} catch (Exception e) {
					throw new WrappedException(e);
				}
			}
		};
	}
}
