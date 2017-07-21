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

import static org.eclipse.emf.compare.utils.EMFComparePredicates.addedToReference;
import static org.eclipse.emf.compare.utils.EMFComparePredicates.attributeValueMatch;
import static org.eclipse.emf.compare.utils.EMFComparePredicates.changedReference;
import static org.eclipse.emf.compare.utils.EMFComparePredicates.removedFromReference;
import static org.hamcrest.CoreMatchers.anything;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;

import java.io.IOException;
import java.util.List;

import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.match.IMatchEngine.Factory.Registry;
import org.eclipse.emf.compare.match.impl.MatchEngineFactoryImpl;
import org.eclipse.emf.compare.uml2.facade.tests.data.BasicFacadeInputData;
import org.eclipse.emf.compare.uml2.facade.tests.j2ee.BeanKind;
import org.eclipse.emf.compare.uml2.tests.AbstractUMLInputData;
import org.eclipse.emf.compare.utils.UseIdentifiers;
import org.eclipse.emf.ecore.resource.Resource;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * This is the {@code BasicFacadeTest} type. Enjoy.
 *
 * @author Christian W. Damus
 */
@SuppressWarnings("nls")
public class BasicFacadeComparisonTest extends AbstractFacadeTest {

	private BasicFacadeInputData input = new BasicFacadeInputData();

	/**
	 * Initializes me.
	 */
	public BasicFacadeComparisonTest() {
		super();
	}

	@Test
	public void testChangeBeanKind_a1() throws IOException {
		Resource left = input.getA1Left();
		Resource right = input.getA1Right();
		Comparison comparison = compare(left, right);

		testAB1(TestKind.ADD, comparison);
	}

	private void testAB1(TestKind kind, Comparison comparison) {
		List<Diff> differences = comparison.getDifferences();

		// This is a very simple delta
		assertEquals(1, differences.size());

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
		assertNotNull(changeKind);
		assertThat(changeKind.getRefinedBy(), not(hasItem(anything())));
		assertThat(changeKind.getRefines(), not(hasItem(anything())));
		assertThat(changeKind.getRequiredBy(), not(hasItem(anything())));
		assertThat(changeKind.getRequires(), not(hasItem(anything())));
		assertThat(changeKind.getEquivalence(), nullValue());
	}

	@Test
	public void testMergeBeanKindRL_a1() throws IOException {
		Resource left = input.getA1Left();
		Resource right = input.getA1Right();

		testMergeRightToLeft(left, right, null);

		// And the merge's effect on the UML representation
		left = input.getA1LeftUML();
		right = input.getA1RightUML();
		assertCompareSame(left, right);
	}

	@Test
	public void testMergeBeanKindLR_a1() throws IOException {
		Resource left = input.getA1Left();
		Resource right = input.getA1Right();

		testMergeLeftToRight(left, right, null);

		// And the merge's effect on the UML representation
		left = input.getA1LeftUML();
		right = input.getA1RightUML();
		assertCompareSame(left, right);
	}

	@Test
	public void testAddHomeInterface_a2() throws IOException {
		Resource left = input.getA2Left();
		Resource right = input.getA2Right();
		Comparison comparison = compare(left, right);

		testAB2(TestKind.ADD, comparison);
	}

	private void testAB2(TestKind kind, Comparison comparison) {
		List<Diff> differences = comparison.getDifferences();

		assertEquals(3, differences.size());

		Predicate<? super Diff> addHomeInterfaceDescription;
		Predicate<? super Diff> setHomeInterfaceBeanDescription;
		Predicate<? super Diff> setBeanHomeInterfaceDescription;

		switch (kind) {
			case DELETE:
				addHomeInterfaceDescription = removedFromReference("a2", "homeInterface", "a2.ThingHome");
				setHomeInterfaceBeanDescription = changedReference("a2.ThingHome", "bean", "a2.Thing", null);
				setBeanHomeInterfaceDescription = changedReference("a2.Thing", "homeInterface",
						"a2.ThingHome", null);
				break;
			case ADD:
				addHomeInterfaceDescription = addedToReference("a2", "homeInterface", "a2.ThingHome");
				setHomeInterfaceBeanDescription = changedReference("a2.ThingHome", "bean", null, "a2.Thing");
				setBeanHomeInterfaceDescription = changedReference("a2.Thing", "homeInterface", null,
						"a2.ThingHome");
				break;
			default:
				fail("Unsupported test kind: " + kind);
				return; // Unreachable
		}

		Diff addHomeInterface = Iterators.find(differences.iterator(), addHomeInterfaceDescription);
		assertNotNull(addHomeInterface);
		assertThat(addHomeInterface.getRefinedBy(), not(hasItem(anything())));
		assertThat(addHomeInterface.getRefines(), not(hasItem(anything())));
		if (kind == TestKind.ADD) {
			// The addition of the home-interface is required by other changes involving it
			assertThat(addHomeInterface.getRequiredBy(), hasItem(anything()));
			assertThat(addHomeInterface.getRequires(), not(hasItem(anything())));
		} else {
			// The deletion of the home-interface is not required by other changes involving it
			// but rather requires them
			assertThat(addHomeInterface.getRequiredBy(), not(hasItem(anything())));
			assertThat(addHomeInterface.getRequires(), hasItem(anything()));
		}
		assertThat(addHomeInterface.getEquivalence(), nullValue());

		Diff setHomeInterfaceBean = Iterators.find(differences.iterator(), setHomeInterfaceBeanDescription);
		assertNotNull(setHomeInterfaceBean);
		assertThat(setHomeInterfaceBean.getRefinedBy(), not(hasItem(anything())));
		assertThat(setHomeInterfaceBean.getRefines(), not(hasItem(anything())));
		if (kind == TestKind.ADD) {
			assertThat(setHomeInterfaceBean.getRequiredBy(), not(hasItem(anything())));
			assertThat(setHomeInterfaceBean.getRequires(), hasItem(addHomeInterface));
		} else {
			assertThat(setHomeInterfaceBean.getRequiredBy(), hasItem(addHomeInterface));
			assertThat(setHomeInterfaceBean.getRequires(), not(hasItem(anything())));
		}
		assertThat(setHomeInterfaceBean.getEquivalence(), notNullValue()); // It's an eOpposite

		Diff setBeanHomeInterface = Iterators.find(differences.iterator(), setBeanHomeInterfaceDescription);
		assertNotNull(setBeanHomeInterface);
		assertThat(setBeanHomeInterface.getRefinedBy(), not(hasItem(anything())));
		assertThat(setBeanHomeInterface.getRefines(), not(hasItem(anything())));
		if (kind == TestKind.ADD) {
			assertThat(setBeanHomeInterface.getRequiredBy(), not(hasItem(anything())));
			assertThat(setBeanHomeInterface.getRequires(), hasItem(addHomeInterface));
		} else {
			assertThat(setBeanHomeInterface.getRequiredBy(), hasItem(addHomeInterface));
			assertThat(setBeanHomeInterface.getRequires(), not(hasItem(anything())));
		}
		assertThat(setBeanHomeInterface.getEquivalence(), is(setHomeInterfaceBean.getEquivalence()));
	}

	@Test
	public void testDeleteHomeInterface_a2() throws IOException {
		Resource left = input.getA2Right();
		Resource right = input.getA2Left();
		Comparison comparison = compare(left, right);

		testAB2(TestKind.DELETE, comparison);
	}

	@Test
	public void testAddHomeInterfaceMergeRL_a2() throws IOException {
		Resource left = input.getA2Left();
		Resource right = input.getA2Right();

		testMergeRightToLeft(left, right, null);

		// And the merge's effect on the UML representation
		left = input.getA2LeftUML();
		right = input.getA2RightUML();
		assertCompareSame(left, right);
	}

	@Test
	public void testAddHomeInterfaceMergeLR_a2() throws IOException {
		Resource left = input.getA2Left();
		Resource right = input.getA2Right();

		testMergeLeftToRight(left, right, null);

		// And the merge's effect on the UML representation
		left = input.getA2LeftUML();
		right = input.getA2RightUML();
		assertCompareSame(left, right);
	}

	@Test
	public void testAddFinder_a3() throws IOException {
		Resource left = input.getA3Left();
		Resource right = input.getA3Right();
		Comparison comparison = compare(left, right);

		testAB3(TestKind.ADD, comparison);
	}

	private void testAB3(TestKind kind, Comparison comparison) {
		List<Diff> differences = comparison.getDifferences();

		assertEquals(3, differences.size());

		Predicate<? super Diff> addFinderDescription;
		Predicate<? super Diff> setFinderBeanDescription;
		Predicate<? super Diff> addBeanFinderDescription;

		switch (kind) {
			case DELETE:
				addFinderDescription = removedFromReference("a3", "finder", "a3.ThingByName");
				setFinderBeanDescription = changedReference("a3.ThingByName", "bean", "a3.Thing", null);
				addBeanFinderDescription = removedFromReference("a3.Thing", "finder", "a3.ThingByName");
				break;
			case ADD:
				addFinderDescription = addedToReference("a3", "finder", "a3.ThingByName");
				setFinderBeanDescription = changedReference("a3.ThingByName", "bean", null, "a3.Thing");
				addBeanFinderDescription = addedToReference("a3.Thing", "finder", "a3.ThingByName");
				break;
			default:
				fail("Unsupported test kind: " + kind);
				return; // Unreachable
		}

		Diff addFinder = Iterators.find(differences.iterator(), addFinderDescription);
		assertNotNull(addFinder);
		assertThat(addFinder.getRefinedBy(), not(hasItem(anything())));
		assertThat(addFinder.getRefines(), not(hasItem(anything())));
		if (kind == TestKind.ADD) {
			// The addition of the finder is required by other changes involving it
			assertThat(addFinder.getRequiredBy(), hasItem(anything()));
			assertThat(addFinder.getRequires(), not(hasItem(anything())));
		} else {
			// The deletion of the finder is not required by other changes involving it
			// but rather requires them
			assertThat(addFinder.getRequiredBy(), not(hasItem(anything())));
			assertThat(addFinder.getRequires(), hasItem(anything()));
		}
		assertThat(addFinder.getEquivalence(), nullValue());

		Diff setFinderBean = Iterators.find(differences.iterator(), setFinderBeanDescription);
		assertNotNull(setFinderBean);
		assertThat(setFinderBean.getRefinedBy(), not(hasItem(anything())));
		assertThat(setFinderBean.getRefines(), not(hasItem(anything())));
		if (kind == TestKind.ADD) {
			assertThat(setFinderBean.getRequiredBy(), not(hasItem(anything())));
			assertThat(setFinderBean.getRequires(), hasItem(addFinder));
		} else {
			assertThat(setFinderBean.getRequiredBy(), hasItem(addFinder));
			assertThat(setFinderBean.getRequires(), not(hasItem(anything())));
		}
		assertThat(setFinderBean.getEquivalence(), notNullValue()); // It's an eOpposite

		Diff addBeanFinder = Iterators.find(differences.iterator(), addBeanFinderDescription);
		assertNotNull(addBeanFinder);
		assertThat(addBeanFinder.getRefinedBy(), not(hasItem(anything())));
		assertThat(addBeanFinder.getRefines(), not(hasItem(anything())));
		if (kind == TestKind.ADD) {
			assertThat(addBeanFinder.getRequiredBy(), not(hasItem(anything())));
			assertThat(addBeanFinder.getRequires(), hasItem(addFinder));
		} else {
			assertThat(addBeanFinder.getRequiredBy(), hasItem(addFinder));
			assertThat(addBeanFinder.getRequires(), not(hasItem(anything())));
		}
		assertThat(addBeanFinder.getEquivalence(), is(setFinderBean.getEquivalence()));
	}

	@Test
	public void testDeleteFinder_a3() throws IOException {
		Resource left = input.getA3Right();
		Resource right = input.getA3Left();
		Comparison comparison = compare(left, right);

		testAB3(TestKind.DELETE, comparison);
	}

	@Test
	public void testAddFinderMergeRL_a3() throws IOException {
		Resource left = input.getA3Left();
		Resource right = input.getA3Right();

		testMergeRightToLeft(left, right, null);

		// And the merge's effect on the UML representation
		left = input.getA3LeftUML();
		right = input.getA3RightUML();
		assertCompareSame(left, right);
	}

	@Test
	public void testAddFinderMergeLR_a3() throws IOException {
		Resource left = input.getA3Left();
		Resource right = input.getA3Right();

		testMergeLeftToRight(left, right, null);

		// And the merge's effect on the UML representation
		left = input.getA3LeftUML();
		right = input.getA3RightUML();
		assertCompareSame(left, right);
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
	protected void fillMatchEngineFactoryRegistry(Registry matchEngineFactoryRegistry) {
		super.fillMatchEngineFactoryRegistry(matchEngineFactoryRegistry);

		// Match by structure of the model, not identity of elements, because the merge of
		// the façade creates similar structures in the UML on one side as on the other,
		// but of course the elements that comprise it will have different XMI IDs
		MatchEngineFactoryImpl byStructure = new MatchEngineFactoryImpl(UseIdentifiers.NEVER);
		byStructure.setRanking(Integer.MAX_VALUE);

		matchEngineFactoryRegistry.add(byStructure);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected AbstractUMLInputData getInput() {
		return input;
	}

}
