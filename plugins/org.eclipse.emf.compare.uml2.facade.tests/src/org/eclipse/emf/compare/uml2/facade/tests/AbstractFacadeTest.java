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
package org.eclipse.emf.compare.uml2.facade.tests;

import static org.eclipse.emf.compare.utils.EMFComparePredicates.hasDirectOrIndirectConflict;
import static org.hamcrest.CoreMatchers.anything;
import static org.hamcrest.CoreMatchers.everyItem;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import com.google.common.base.Predicate;

import java.net.URL;
import java.util.List;

import org.eclipse.emf.common.EMFPlugin;
import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.ConflictKind;
import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.scope.DefaultComparisonScope;
import org.eclipse.emf.compare.scope.IComparisonScope;
import org.eclipse.emf.compare.uml2.facade.tests.j2ee.J2EEPackage;
import org.eclipse.emf.compare.uml2.facade.tests.j2ee.util.J2EEResource;
import org.eclipse.emf.compare.uml2.facade.tests.j2ee.util.J2EEResourceFactoryImpl;
import org.eclipse.emf.compare.uml2.facade.tests.j2eeprofile.J2EEProfilePackage;
import org.eclipse.emf.compare.uml2.tests.AbstractUMLTest;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.URIConverter;
import org.eclipse.uml2.uml.UMLPlugin;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * This is the {@code AbstractFacadeTest} type. Enjoy.
 *
 * @author Christian W. Damus
 */
@SuppressWarnings("nls")
public abstract class AbstractFacadeTest extends AbstractUMLTest {

	/**
	 * Initializes me.
	 */
	public AbstractFacadeTest() {
		super();
	}

	/**
	 * Each sublass of {@code AbstractFacadeTest} should call this method in a {@code @BeforeClass} annotated
	 * method. This allows each test to customize its context.
	 */
	public static void fillRegistries() {
		AbstractUMLTest.fillRegistries();

		if (!EMFPlugin.IS_ECLIPSE_RUNNING) {
			EPackage.Registry.INSTANCE.put(J2EEPackage.eNS_URI, J2EEPackage.eINSTANCE);
			EPackage.Registry.INSTANCE.put(J2EEProfilePackage.eNS_URI, J2EEProfilePackage.eINSTANCE);

			Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put(J2EEResource.FILE_EXTENSION,
					new J2EEResourceFactoryImpl());

			UMLPlugin.getEPackageNsURIToProfileLocationMap().put(J2EEProfilePackage.eNS_URI,
					URI.createURI("pathmap://UML2_FACADE_TESTS/j2ee.profile.uml#_0"));

			// Find the location of the profile
			URL profileURL = AbstractFacadeTest.class.getClassLoader().getResource("j2ee.profile.uml");
			URI profileURI = URI.createURI(profileURL.toExternalForm());
			URI baseURI = profileURI.trimSegments(1).appendSegment("");
			// Map the base resource location
			URIConverter.URI_MAP.put(URI.createURI("pathmap://UML2_FACADE_TESTS/"), baseURI);
		}
	}

	/**
	 * Each sublass of {@code AbstractFacadeTest} should call this method in an {@code @AfterClass} annotated
	 * method. This allows each test to safely delete its context.
	 */
	public static void resetRegistries() {
		if (!EMFPlugin.IS_ECLIPSE_RUNNING) {
			URIConverter.URI_MAP.remove(URI.createURI("pathmap://UML2_FACADE_TESTS/"));

			UMLPlugin.getEPackageNsURIToProfileLocationMap().remove(J2EEProfilePackage.eNS_URI);

			Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().remove(J2EEResource.FILE_EXTENSION);

			EPackage.Registry.INSTANCE.remove(J2EEProfilePackage.eNS_URI);
			EPackage.Registry.INSTANCE.remove(J2EEPackage.eNS_URI);
		}

		AbstractUMLTest.resetRegistries();
	}

	protected void assertCompareSame(Notifier left, Notifier right) {
		assertCompareSame(left, right, null, false);
	}

	protected void assertCompareSame(Notifier left, Notifier right, Notifier base) {
		assertCompareSame(left, right, base, false);
	}

	protected void assertCompareSame(Notifier left, Notifier right, boolean pseudoAllowed) {
		assertCompareSame(left, right, null, pseudoAllowed);
	}

	protected void assertCompareSame(Notifier left, Notifier right, Notifier base, boolean pseudoAllowed) {
		final IComparisonScope scope = new DefaultComparisonScope(left, right, base);
		final Comparison comparison = getCompare().compare(scope);

		EList<Diff> differences = comparison.getDifferences();

		Matcher<? super List<Diff>> assertion;
		if (pseudoAllowed) {
			assertion = everyItem(isPseudoConflict());
		} else {
			assertion = not(hasItem(anything()));
		}
		assertThat("No differences expected", differences, assertion);
	}

	protected static Matcher<Diff> isPseudoConflict() {
		Predicate<? super Diff> delegate = hasDirectOrIndirectConflict(ConflictKind.PSEUDO);

		return new TypeSafeMatcher<Diff>() {
			/**
			 * {@inheritDoc}
			 */
			public void describeTo(Description description) {
				description.appendText("is a pseudo-conflict");
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
}
