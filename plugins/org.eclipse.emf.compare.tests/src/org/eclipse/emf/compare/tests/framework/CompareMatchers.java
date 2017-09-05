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
package org.eclipse.emf.compare.tests.framework;

import static org.eclipse.emf.compare.utils.EMFComparePredicates.hasDirectOrIndirectConflict;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;

import java.util.regex.Pattern;

import org.eclipse.emf.compare.Conflict;
import org.eclipse.emf.compare.ConflictKind;
import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.uml2.common.util.UML2Util;
import org.eclipse.uml2.uml.NamedElement;
import org.hamcrest.BaseMatcher;
import org.hamcrest.CustomTypeSafeMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * Hamcrest matchers useful in compare test assertions.
 *
 * @author Christian W. Damus
 */
@SuppressWarnings("nls")
public final class CompareMatchers {

	/**
	 * Not instantiable by clients.
	 */
	private CompareMatchers() {
		super();
	}

	/**
	 * Match a diff that contributes, directly or indirectly, to a pseudo-conflict.
	 * 
	 * @return the pseudo-conflict diff matcher
	 */
	public static Matcher<Diff> hasPseudoConflict() {
		final Predicate<? super Diff> delegate = hasDirectOrIndirectConflict(ConflictKind.PSEUDO);

		return new TypeSafeMatcher<Diff>() {
			/**
			 * {@inheritDoc}
			 */
			public void describeTo(Description description) {
				description.appendText("has a pseudo-conflict");
			}

			/**
			 * {@inheritDoc}
			 */
			@Override
			protected boolean matchesSafely(Diff item) {
				return delegate.apply(item);
			}
		};
	}

	/**
	 * Match a conflict that is a pseudo-conflict.
	 * 
	 * @return the pseudo-conflict matcher
	 */
	public static Matcher<Conflict> isPseudoConflict() {
		return isConflict(ConflictKind.PSEUDO);
	}

	/**
	 * Match a conflict that is a real conflict.
	 * 
	 * @return the real conflict matcher
	 */
	public static Matcher<Conflict> isRealConflict() {
		return isConflict(ConflictKind.REAL);
	}

	/**
	 * Match a conflict that is the given kind.
	 * 
	 * @param ofKind
	 *            the conflict kind to match
	 * @return the pseudo-conflict matcher
	 */
	public static Matcher<Conflict> isConflict(final ConflictKind ofKind) {
		return new TypeSafeDiagnosingMatcher<Conflict>() {
			/**
			 * {@inheritDoc}
			 */
			public void describeTo(Description description) {
				description.appendText("is a ").appendValue(ofKind).appendText(" conflict");
			}

			/**
			 * {@inheritDoc}
			 */
			@Override
			protected boolean matchesSafely(Conflict item, Description mismatchDescription) {
				boolean result = item.getKind() == ofKind;

				if (!result) {
					mismatchDescription.appendText(item.getKind().name()).appendText(" conflict");
				}

				return result;
			}
		};
	}

	/**
	 * Wrap a Guava predicate as a Hamcrest matcher.
	 * 
	 * @param type
	 *            the target type of the predicate
	 * @param message
	 *            a message to supply in case of match failure
	 * @param predicate
	 *            the predicate to wrap
	 * @return the Hamcrest matcher
	 */
	public static <T> Matcher<T> matches(final Class<? extends T> type, final String message,
			final Predicate<? super T> predicate) {
		return new BaseMatcher<T>() {
			/**
			 * {@inheritDoc}
			 */
			public void describeTo(Description description) {
				description.appendText(message);
			}

			/**
			 * {@inheritDoc}
			 */
			public boolean matches(Object item) {
				return type.isInstance(item) && predicate.apply(type.cast(item));
			}
		};
	}

	/**
	 * Match elements having the given {@code name}.
	 * 
	 * @param name
	 *            the name of element to match
	 * @return the name matcher
	 */
	public static Matcher<NamedElement> named(final String name) {
		return new TypeSafeMatcher<NamedElement>() {
			/**
			 * {@inheritDoc}
			 */
			public void describeTo(Description description) {
				description.appendText("named ").appendText(name);
			}

			/**
			 * {@inheritDoc}
			 */
			@Override
			protected boolean matchesSafely(NamedElement item) {
				return UML2Util.safeEquals(item.getName(), name);
			}
		};
	}

	/**
	 * Matcher for unresolved proxies.
	 * 
	 * @return the proxy matcher
	 */
	public static Matcher<EObject> isProxy() {
		return new TypeSafeMatcher<EObject>() {
			/**
			 * {@inheritDoc}
			 */
			public void describeTo(Description description) {
				description.appendText("is proxy");
			}

			/**
			 * {@inheritDoc}
			 */
			@Override
			protected boolean matchesSafely(EObject item) {
				return item.eIsProxy();
			}
		};
	}

	/**
	 * Matcher for present {@link Optional}s.
	 * 
	 * @return a present optional matcher
	 */
	public static <T> Matcher<Optional<T>> isPresent() {
		return new CustomTypeSafeMatcher<Optional<T>>("optional is present") {
			/**
			 * {@inheritDoc}
			 */
			@Override
			protected boolean matchesSafely(Optional<T> item) {
				return item.isPresent();
			}
		};
	}

	/**
	 * Matcher for a regular expression {@code pattern}. This matches a string that has a region matching the
	 * given {@code pattern}. If a complete match is required, then use the {@code ^} and {@code $} anchors.
	 * 
	 * @param pattern
	 *            the regular expression to match
	 * @return matcher
	 */
	public static Matcher<String> regexMatches(String pattern) {
		final java.util.regex.Matcher matcher = Pattern.compile(pattern).matcher("");

		return new CustomTypeSafeMatcher<String>(String.format("matches regex /%s/", pattern)) {
			/**
			 * {@inheritDoc}
			 */
			@Override
			protected boolean matchesSafely(String item) {
				matcher.reset(item);
				return matcher.find();
			}
		};
	}
}
