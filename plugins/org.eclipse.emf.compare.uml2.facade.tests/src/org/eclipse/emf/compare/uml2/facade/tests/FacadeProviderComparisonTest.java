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

import static org.eclipse.emf.compare.utils.EMFComparePredicates.attributeValueMatch;
import static org.hamcrest.CoreMatchers.anything;
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
import org.eclipse.emf.compare.uml2.facade.tests.data.UMLInputData;
import org.eclipse.emf.compare.uml2.facade.tests.j2ee.BeanKind;
import org.eclipse.emf.compare.uml2.facade.tests.j2ee.internal.providers.J2EEFacadeProvider;
import org.eclipse.emf.compare.uml2.tests.AbstractUMLInputData;
import org.eclipse.emf.compare.utils.EMFComparePredicates;
import org.eclipse.emf.compare.utils.UseIdentifiers;
import org.eclipse.emf.ecore.resource.Resource;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * This is the {@code FacadeProviderComparisonTest} type. Enjoy.
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

		Diff leftDiff = Iterables.find(conflict.getLeftDifferences(), EMFComparePredicates
				.changedReference("u1.Person.age", "type", "PrimitiveTypes.String", "PrimitiveTypes.Real"));
		assertThat(leftDiff, notNullValue());
		Diff rightDiff = Iterables.find(conflict.getRightDifferences(), EMFComparePredicates.changedReference(
				"u1.Person.age", "type", "PrimitiveTypes.String", "PrimitiveTypes.Integer"));
		assertThat(rightDiff, notNullValue());
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

}
