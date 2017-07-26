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

import static org.eclipse.emf.compare.tests.framework.CompareMatchers.matches;
import static org.eclipse.emf.compare.uml2.tests.AdditionalResourcesKind.REFERENCED_LOCAL;
import static org.eclipse.emf.compare.utils.EMFComparePredicates.addedToReference;
import static org.eclipse.emf.compare.utils.EMFComparePredicates.attributeValueMatch;
import static org.eclipse.emf.compare.utils.EMFComparePredicates.changedReference;
import static org.hamcrest.CoreMatchers.anything;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;

import java.util.List;

import org.eclipse.emf.common.EMFPlugin;
import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.Conflict;
import org.eclipse.emf.compare.ConflictKind;
import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.facade.IFacadeProvider;
import org.eclipse.emf.compare.facade.internal.EMFCompareFacadePlugin;
import org.eclipse.emf.compare.facade.internal.FacadeProviderRegistryImpl;
import org.eclipse.emf.compare.facade.internal.match.FacadeMatchEngine;
import org.eclipse.emf.compare.match.IMatchEngine;
import org.eclipse.emf.compare.scope.IComparisonScope;
import org.eclipse.emf.compare.uml2.facade.tests.data.UMLInputData;
import org.eclipse.emf.compare.uml2.facade.tests.j2ee.BeanKind;
import org.eclipse.emf.compare.uml2.facade.tests.j2ee.internal.providers.J2EEFacadeProvider;
import org.eclipse.emf.compare.uml2.tests.AbstractUMLInputData;
import org.eclipse.emf.compare.uml2.tests.AdditionalResources;
import org.eclipse.emf.compare.utils.UseIdentifiers;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.uml2.uml.resource.UMLResource;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test cases for comparison based on pluggable façade model providers.
 *
 * @author Christian W. Damus
 */
@SuppressWarnings({"nls", "boxing", "restriction" })
public class FacadeProviderComparisonTest extends AbstractFacadeTest {

	private UMLInputData input = new UMLInputData();

	/**
	 * Initializes me.
	 */
	public FacadeProviderComparisonTest() {
		super();
	}

	@Test
	public void changeBeanKind_a1() {
		Resource left = input.getA1Left();
		Resource right = input.getA1Right();
		Comparison comparison = compare(left, right);

		testAB1(TestKind.ADD, comparison);
	}

	private void testAB1(TestKind kind, Comparison comparison) {
		List<Diff> differences = comparison.getDifferences();

		// This is a very simple delta
		assertThat(differences.size(), is(1));

		Predicate<? super Diff> changeKindDescription;

		switch (kind) {
			case DELETE:
				changeKindDescription = attributeValueMatch("kind", BeanKind.ENTITY, false);
				break;
			case ADD:
				changeKindDescription = attributeValueMatch("kind", BeanKind.ENTITY, false);
				break;
			default:
				fail("Unsupported test kind: " + kind);
				return; // Unreachable
		}

		Diff changeKind = Iterators.find(differences.iterator(), changeKindDescription);
		assertThat(changeKind, notNullValue());
		assertThat(changeKind.getRefinedBy(), not(hasItem(anything())));
		assertThat(changeKind.getRefines(), not(hasItem(anything())));
		assertThat(changeKind.getRequiredBy(), not(hasItem(anything())));
		assertThat(changeKind.getRequires(), not(hasItem(anything())));
		assertThat(changeKind.getEquivalence(), nullValue());
	}

	@Test
	public void mergeBeanKindRL_a1() {
		Resource left = input.getA1Left();
		Resource right = input.getA1Right();

		testMergeRightToLeft(left, right, null);
	}

	@Test
	public void mergeBeanKindLR_a1() {
		Resource left = input.getA1Left();
		Resource right = input.getA1Right();

		testMergeLeftToRight(left, right, null);
	}

	/**
	 * A control test for the comparison of UML models that do not have a façade.
	 */
	@Test
	public void changeAttributeTypeControl_u1() {
		Resource base = input.getU1Base();
		Resource left = input.getU1Left();
		Resource right = input.getU1Right();
		Comparison comparison = compare(left, right, base);

		List<Conflict> conflicts = comparison.getConflicts();

		// This is a very simple comparison
		assertThat(conflicts.size(), is(1));
		Conflict conflict = conflicts.get(0);
		assertThat(conflict.getKind(), is(ConflictKind.REAL));

		assertThat(conflict.getLeftDifferences().size(), is(1));
		assertThat(conflict.getRightDifferences().size(), is(1));

		Diff leftDiff = Iterables.find(conflict.getLeftDifferences(),
				changedReference("u1.Person.age", "type", "PrimitiveTypes.String", "PrimitiveTypes.Real"));
		assertThat(leftDiff, notNullValue());
		Diff rightDiff = Iterables.find(conflict.getRightDifferences(),
				changedReference("u1.Person.age", "type", "PrimitiveTypes.String", "PrimitiveTypes.Integer"));
		assertThat(rightDiff, notNullValue());
	}

	@Test
	@AdditionalResources(REFERENCED_LOCAL)
	public void basicMixedModeComparison_m1() {
		Resource left = input.getM1Left();
		Resource right = input.getM1Right();
		Comparison comparison = compare(left, right);

		List<Diff> differences = comparison.getDifferences();

		assertThat(differences, hasItem(matches(Diff.class, "Add finder façade",
				addedToReference("j2ee-app", "finder", "j2ee-app.ThingByName"))));

		assertThat(differences, hasItem(matches(Diff.class, "Change UML collaboration role", changedReference(
				"m1.lookup_thing.finder", "type", "j2ee-app.ThingByID", "j2ee-app.ThingByName"))));
	}

	//
	// Test framework
	//

	@BeforeClass
	public static void setupClass() {
		fillRegistries();
	}

	@AfterClass
	public static void teardownClass() {
		resetRegistries();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void fillMatchEngineFactoryRegistry(IMatchEngine.Factory.Registry matchEngineFactoryRegistry) {
		super.fillMatchEngineFactoryRegistry(matchEngineFactoryRegistry);

		IFacadeProvider.Factory.Registry facadeProviderRegistry;
		if (EMFPlugin.IS_ECLIPSE_RUNNING) {
			// Use the extension-based registry
			facadeProviderRegistry = EMFCompareFacadePlugin.getDefault().getFacadeProviderRegistry();
		} else {
			facadeProviderRegistry = FacadeProviderRegistryImpl.createStandaloneInstance();
			facadeProviderRegistry.add(new J2EEFacadeProvider.Factory());
		}

		// Match by structure of the model, not identity of elements, because the merge of
		// the façade creates similar structures in the UML on one side as on the other,
		// but of course the elements that comprise it will have different XMI IDs
		IMatchEngine.Factory matchEngineFactory = new FacadeMatchEngine.Factory(UseIdentifiers.NEVER,
				facadeProviderRegistry);
		matchEngineFactory.setRanking(Integer.MAX_VALUE);

		matchEngineFactoryRegistry.add(matchEngineFactory);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected AbstractUMLInputData getInput() {
		return input;
	}

	/**
	 * Assert that, after comparison, the {@code scope} has only collected UML resource URIs.
	 * 
	 * @param scope
	 *            a comparison scope
	 */
	public void verifyComparisonScope(IComparisonScope scope) {
		scope.getResourceURIs().stream().forEach(
				uri -> assertThat("Not an UML URI", uri, endsWith("." + UMLResource.FILE_EXTENSION)));
	}

}
