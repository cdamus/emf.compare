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

import com.google.common.base.Defaults;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;

import java.lang.annotation.Annotation;

import org.eclipse.emf.compare.utils.ReflectiveDispatch;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * A JUnit rule that accesses the value of some test annotation.
 *
 * @author Christian W. Damus
 */
public abstract class AnnotationRule<A extends Annotation, T> implements TestRule, Supplier<T> {

	private final Class<A> annotationType;

	private T value;

	/**
	 * Initializes me with my annotation type.
	 * 
	 * @param annotationType
	 *            the annotation type
	 */
	protected AnnotationRule(Class<A> annotationType) {
		super();

		this.annotationType = annotationType;
	}

	public static <A extends Annotation, T> AnnotationRule<A, T> create(Class<A> annotationType,
			final Class<? extends T> valueType) {

		return new ValueRule<>(annotationType, valueType, new Function<A, T>() {
			/**
			 * {@inheritDoc}
			 */
			public T apply(A input) {
				Object value = ReflectiveDispatch.safeInvoke(input, "value"); //$NON-NLS-1$
				if (!valueType.isInstance(value)) {
					return Defaults.defaultValue(valueType);
				} else {
					return valueType.cast(value);
				}
			}
		});
	}

	public static <A extends Annotation, T> AnnotationRule<A, T> create(Class<A> annotationType,
			Class<? extends T> valueType, Function<? super A, ? extends T> valueAccessor) {
		return new ValueRule<>(annotationType, valueType, valueAccessor);
	}

	public static <A extends Annotation, T> AnnotationRule<A, T> create(Class<A> annotationType,
			final T defaultValue) {

		Preconditions.checkNotNull(defaultValue,
				"defaultValue may not be null; use create(Class<A>, Class<? extends T>) method, instead"); //$NON-NLS-1$

		return new ValueRule<>(annotationType, defaultValue, new Function<A, T>() {
			/**
			 * {@inheritDoc}
			 */
			@SuppressWarnings("unchecked")
			public T apply(A input) {
				return (T)ReflectiveDispatch.safeInvoke(input, "value"); //$NON-NLS-1$
			}
		});
	}

	public static <A extends Annotation, T> AnnotationRule<A, T> create(Class<A> annotationType,
			T defaultValue, Function<? super A, ? extends T> valueAccessor) {

		Preconditions.checkNotNull(defaultValue,
				"defaultValue may not be null; use create(Class<A>, Class<? extends T>, Function<? super A, ? extends T>) method, instead"); //$NON-NLS-1$

		return new ValueRule<>(annotationType, defaultValue, valueAccessor);
	}

	public static <A extends Annotation> AnnotationRule<A, Boolean> create(Class<A> annotationType) {
		return new ExistenceRule<>(annotationType);
	}

	/**
	 * {@inheritDoc}
	 */
	public T get() {
		return value;
	}

	/**
	 * {@inheritDoc}
	 */
	public Statement apply(Statement base, Description description) {
		A annotation = description.getAnnotation(annotationType);
		if ((annotation == null) && description.isTest()) {
			// The description is of a test method and perhaps the class has an annotation?
			annotation = description.getTestClass().getAnnotation(annotationType);
		}

		value = extractValue(annotation, description);

		return base;
	}

	protected abstract T extractValue(A annotation, Description description);

	//
	// Nested types
	//

	private static final class ValueRule<A extends Annotation, T> extends AnnotationRule<A, T> {
		private final Function<? super A, ? extends T> valueAccessor;

		private final T defaultValue;

		/**
		 * Initializes me with my annotation type and value accessor. The default value is {@code null} or the
		 * appropriate zero for primitive wrappers.
		 * 
		 * @param annotationType
		 *            my annotation type
		 * @param valueType
		 *            my annotation value type
		 * @param valueAccessor
		 *            to extract the value of the annotation
		 */
		ValueRule(Class<A> annotationType, Class<? extends T> valueType,
				Function<? super A, ? extends T> valueAccessor) {

			this(annotationType, Defaults.defaultValue(valueType), valueAccessor);
		}

		/**
		 * Initializes me with my annotation type, default value, and value accessor.
		 * 
		 * @param annotationType
		 *            my annotation type
		 * @param defaultValue
		 *            the default value for when the annotation is not present
		 * @param valueAccessor
		 *            to extract the value of the annotation
		 */
		ValueRule(Class<A> annotationType, T defaultValue, Function<? super A, ? extends T> valueAccessor) {
			super(annotationType);

			this.valueAccessor = valueAccessor;
			this.defaultValue = defaultValue;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		protected T extractValue(A annotation, Description description) {
			if (annotation == null) {
				return defaultValue;
			} else {
				return valueAccessor.apply(annotation);
			}
		}

	}

	private static final class ExistenceRule<A extends Annotation> extends AnnotationRule<A, Boolean> {

		/**
		 * Initializes me with my annotation type.
		 * 
		 * @param annotationType
		 *            my annotation type
		 */
		ExistenceRule(Class<A> annotationType) {
			super(annotationType);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		protected Boolean extractValue(A annotation, Description description) {
			return Boolean.valueOf(annotation != null);
		}
	}
}
