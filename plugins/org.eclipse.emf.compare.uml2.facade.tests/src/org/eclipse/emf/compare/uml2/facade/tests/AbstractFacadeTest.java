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
package org.eclipse.emf.compare.uml2.facade.tests;

import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Lists.newArrayList;
import static org.eclipse.emf.compare.tests.framework.CompareMatchers.hasPseudoConflict;
import static org.eclipse.emf.compare.utils.EMFComparePredicates.hasNoDirectOrIndirectConflict;
import static org.hamcrest.CoreMatchers.anything;
import static org.hamcrest.CoreMatchers.everyItem;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.common.EMFPlugin;
import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.common.util.BasicMonitor;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.Conflict;
import org.eclipse.emf.compare.ConflictKind;
import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.DifferenceSource;
import org.eclipse.emf.compare.facade.FacadeAdapter;
import org.eclipse.emf.compare.merge.BatchMerger;
import org.eclipse.emf.compare.merge.IBatchMerger;
import org.eclipse.emf.compare.merge.ICopier;
import org.eclipse.emf.compare.scope.DefaultComparisonScope;
import org.eclipse.emf.compare.scope.IComparisonScope;
import org.eclipse.emf.compare.uml2.facade.tests.framework.MergeUtils;
import org.eclipse.emf.compare.uml2.facade.tests.j2ee.J2EEPackage;
import org.eclipse.emf.compare.uml2.facade.tests.j2ee.internal.providers.J2EECopier;
import org.eclipse.emf.compare.uml2.facade.tests.j2ee.util.J2EEResource;
import org.eclipse.emf.compare.uml2.facade.tests.j2ee.util.J2EEResourceFactoryImpl;
import org.eclipse.emf.compare.uml2.facade.tests.j2eeprofile.J2EEProfilePackage;
import org.eclipse.emf.compare.uml2.facade.tests.opaqexpr.OpaqexprPackage;
import org.eclipse.emf.compare.uml2.facade.tests.opaqexpr.internal.providers.OpaqexprCopier;
import org.eclipse.emf.compare.uml2.facade.tests.opaqexpr.util.OpaqexprResource;
import org.eclipse.emf.compare.uml2.facade.tests.opaqexpr.util.OpaqexprResourceFactoryImpl;
import org.eclipse.emf.compare.uml2.tests.AbstractUMLTest;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.URIConverter;
import org.eclipse.uml2.uml.UMLPlugin;
import org.hamcrest.Matcher;

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
			EPackage.Registry.INSTANCE.put(OpaqexprPackage.eNS_URI, OpaqexprPackage.eINSTANCE);

			Map<String, Object> factories = Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap();
			factories.put(J2EEResource.FILE_EXTENSION, new J2EEResourceFactoryImpl());
			factories.put(OpaqexprResource.FILE_EXTENSION, new OpaqexprResourceFactoryImpl());

			UMLPlugin.getEPackageNsURIToProfileLocationMap().put(J2EEProfilePackage.eNS_URI,
					URI.createURI("pathmap://UML2_FACADE_TESTS/j2ee.profile.uml#_0"));

			// Map the base resource location
			URI baseURI = getModelsBaseURI();
			URIConverter.URI_MAP.put(URI.createURI("pathmap://UML2_FACADE_TESTS/"), baseURI);

			ICopier.Registry.INSTANCE.add(new CopierDescriptor(J2EEPackage.eINSTANCE, new J2EECopier()));
			ICopier.Registry.INSTANCE
					.add(new CopierDescriptor(OpaqexprPackage.eINSTANCE, new OpaqexprCopier()));
		}
	}

	/**
	 * Determine the URI of the {@code model/} folder in the plug-in bundle, whether it is in source form in
	 * the workspace (accounting for classes being then in a {@code bin/} folder) or in binary form in an
	 * installed JAR.
	 * 
	 * @return the URI of the {@code model/} folder containing profiles and metamodels
	 */
	private static URI getModelsBaseURI() {
		URI result;

		URL classURL = AbstractFacadeTest.class.getResource("AbstractFacadeTest.class");
		URI classURI = URI.createURI(classURL.toExternalForm());

		int segments = classURI.segmentCount();
		int trim;
		for (trim = 1; trim < segments; trim++) {
			if (classURI.segment(segments - trim).equals("org")) {
				// Are we in a source project in the workspace?
				if ((trim < segments) && classURI.segment(segments - trim - 1).equals("bin")) {
					// Yup.
					trim++;
				}
				break;
			}
		}

		result = classURI.trimSegments(trim).appendSegment("model") //
				.appendSegment(""); // Ensure a trailing separator
		return result;
	}

	/**
	 * Each sublass of {@code AbstractFacadeTest} should call this method in an {@code @AfterClass} annotated
	 * method. This allows each test to safely delete its context.
	 */
	public static void resetRegistries() {
		if (!EMFPlugin.IS_ECLIPSE_RUNNING) {
			ICopier.Registry.INSTANCE.remove(new CopierDescriptor(OpaqexprPackage.eINSTANCE, null));
			ICopier.Registry.INSTANCE.remove(new CopierDescriptor(J2EEPackage.eINSTANCE, null));

			URIConverter.URI_MAP.remove(URI.createURI("pathmap://UML2_FACADE_TESTS/"));

			UMLPlugin.getEPackageNsURIToProfileLocationMap().remove(J2EEProfilePackage.eNS_URI);

			Map<String, Object> factories = Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap();
			factories.remove(J2EEResource.FILE_EXTENSION);
			factories.remove(OpaqexprResource.FILE_EXTENSION);

			EPackage.Registry.INSTANCE.remove(OpaqexprPackage.eNS_URI);
			EPackage.Registry.INSTANCE.remove(J2EEProfilePackage.eNS_URI);
			EPackage.Registry.INSTANCE.remove(J2EEPackage.eNS_URI);
		}

		AbstractUMLTest.resetRegistries();
	}

	/**
	 * Obtains the (real) model element underlying the given {@code facade} object.
	 * 
	 * @param facade
	 *            a façade object
	 * @return its (principal) underlying model element
	 */
	public static EObject getUnderlyingObject(EObject facade) {
		return FacadeAdapter.getUnderlyingObject(facade);
	}

	protected Comparison preMerge(Notifier left, Notifier right, Notifier base) {
		IComparisonScope scope = new DefaultComparisonScope(left, right, base);
		Comparison comparison = getCompare().compare(scope);
		List<Diff> allDiffs = comparison.getDifferences();
		List<Diff> mergeableDiffs = newArrayList(
				filter(allDiffs, hasNoDirectOrIndirectConflict(ConflictKind.REAL, ConflictKind.PSEUDO)));

		if (!mergeableDiffs.isEmpty()) {
			IBatchMerger merger = new BatchMerger(getMergerRegistry());
			merger.copyAllLeftToRight(mergeableDiffs, new BasicMonitor());

			return getCompare().compare(scope);
		}

		return comparison; // Don't need to re-compare if we didn't merge
	}

	protected void acceptAllNonConflicting(Comparison comparison) {
		MergeUtils.acceptAllNonConflicting(comparison, getMergerRegistry());
	}

	protected void accept(DifferenceSource side, Conflict... conflict) {
		accept(side, Arrays.asList(conflict));
	}

	protected void accept(DifferenceSource side, Iterable<? extends Conflict> conflicts) {
		MergeUtils.accept(side, getMergerRegistry(), conflicts);
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
			assertion = everyItem(hasPseudoConflict());
		} else {
			assertion = not(hasItem(anything()));
		}
		assertThat("No differences expected", differences, assertion);
	}

	//
	// Nested types
	//

	/**
	 * Descriptor for a statically provided copier in the registry.
	 *
	 * @author Christian W. Damus
	 */
	private static final class CopierDescriptor implements ICopier.Descriptor {
		private final EPackage targetPackage;

		private final ICopier copier;

		/**
		 * Initializes me with the {@code copier} to register for a given package.
		 * 
		 * @param targetPackage
		 *            the target package. Must not be {@code null}
		 * @param copier
		 *            the copier to register. May be {@code null} if the descriptor exists only to remove a
		 *            registration
		 */
		CopierDescriptor(EPackage targetPackage, ICopier copier) {
			super();

			this.targetPackage = targetPackage;
			this.copier = copier;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean isApplicableTo(EObject object) {
			return object.eClass().getEPackage() == targetPackage;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public int getRank() {
			return 50;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public ICopier getCopier() {
			return copier;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public int hashCode() {
			return targetPackage.hashCode();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean equals(Object obj) {
			return (obj instanceof CopierDescriptor)
					&& (((CopierDescriptor)obj).targetPackage == this.targetPackage);
		}
	}
}
