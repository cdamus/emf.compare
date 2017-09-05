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

import static com.google.common.collect.Lists.newArrayList;
import static org.eclipse.emf.compare.tests.framework.CompareMatchers.isPseudoConflict;
import static org.eclipse.emf.compare.tests.framework.CompareMatchers.matches;
import static org.eclipse.emf.compare.utils.EMFComparePredicates.addedToReference;
import static org.eclipse.emf.compare.utils.EMFComparePredicates.attributeValueMatch;
import static org.eclipse.emf.compare.utils.EMFComparePredicates.changedReference;
import static org.eclipse.emf.compare.utils.EMFComparePredicates.removedFromReference;
import static org.hamcrest.CoreMatchers.anything;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.everyItem;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.Conflict;
import org.eclipse.emf.compare.ConflictKind;
import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.match.IMatchEngine.Factory.Registry;
import org.eclipse.emf.compare.match.impl.MatchEngineFactoryImpl;
import org.eclipse.emf.compare.scope.IComparisonScope;
import org.eclipse.emf.compare.uml2.facade.tests.data.BasicFacadeInputData;
import org.eclipse.emf.compare.uml2.facade.tests.j2ee.BeanKind;
import org.eclipse.emf.compare.uml2.facade.tests.j2ee.util.J2EEResource;
import org.eclipse.emf.compare.uml2.tests.AbstractUMLInputData;
import org.eclipse.emf.compare.utils.EMFComparePredicates;
import org.eclipse.emf.compare.utils.UseIdentifiers;
import org.eclipse.emf.ecore.resource.Resource;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Basic façade-based comparison scenarios with the J2EE façade.
 *
 * @author Christian W. Damus
 */
@SuppressWarnings({"nls", "boxing" })
public class BasicFacadeComparisonTest extends AbstractFacadeTest {

	private BasicFacadeInputData input = new BasicFacadeInputData();

	/**
	 * Initializes me.
	 */
	public BasicFacadeComparisonTest() {
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

		// And the merge's effect on the UML representation
		left = input.getA1LeftUML();
		right = input.getA1RightUML();
		assertCompareSame(left, right);
	}

	@Test
	public void mergeBeanKindLR_a1() {
		Resource left = input.getA1Left();
		Resource right = input.getA1Right();

		testMergeLeftToRight(left, right, null);

		// And the merge's effect on the UML representation
		left = input.getA1LeftUML();
		right = input.getA1RightUML();
		assertCompareSame(left, right);
	}

	@Test
	public void addHomeInterface_a2() {
		Resource left = input.getA2Left();
		Resource right = input.getA2Right();
		Comparison comparison = compare(left, right);

		testAB2(TestKind.ADD, comparison);
	}

	private void testAB2(TestKind kind, Comparison comparison) {
		List<Diff> differences = comparison.getDifferences();

		assertThat(differences.size(), is(3));

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
		assertThat(addHomeInterface, notNullValue());
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
		assertThat(setHomeInterfaceBean, notNullValue());
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
		assertThat(setBeanHomeInterface, notNullValue());
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
	public void deleteHomeInterface_a2() {
		Resource left = input.getA2Right();
		Resource right = input.getA2Left();
		Comparison comparison = compare(left, right);

		testAB2(TestKind.DELETE, comparison);
	}

	@Test
	public void mergeHomeInterfaceRL_a2() {
		Resource left = input.getA2Left();
		Resource right = input.getA2Right();

		testMergeRightToLeft(left, right, null);

		// And the merge's effect on the UML representation
		left = input.getA2LeftUML();
		right = input.getA2RightUML();
		assertCompareSame(left, right);
	}

	@Test
	public void mergeHomeInterfaceLR_a2() {
		Resource left = input.getA2Left();
		Resource right = input.getA2Right();

		testMergeLeftToRight(left, right, null);

		// And the merge's effect on the UML representation
		left = input.getA2LeftUML();
		right = input.getA2RightUML();
		assertCompareSame(left, right);
	}

	@Test
	public void addFinder_a3() {
		Resource left = input.getA3Left();
		Resource right = input.getA3Right();
		Comparison comparison = compare(left, right);

		testAB3(TestKind.ADD, comparison);
	}

	private void testAB3(TestKind kind, Comparison comparison) {
		List<Diff> differences = comparison.getDifferences();

		assertThat(differences.size(), is(3));

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
		assertThat(addFinder, notNullValue());
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
		assertThat(setFinderBean, notNullValue());
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
		assertThat(addBeanFinder, notNullValue());
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
	public void deleteFinder_a3() {
		Resource left = input.getA3Right();
		Resource right = input.getA3Left();
		Comparison comparison = compare(left, right);

		testAB3(TestKind.DELETE, comparison);
	}

	@Test
	public void mergeFinderRL_a3() {
		Resource left = input.getA3Left();
		Resource right = input.getA3Right();

		testMergeRightToLeft(left, right, null);

		// And the merge's effect on the UML representation
		left = input.getA3LeftUML();
		right = input.getA3RightUML();
		assertCompareSame(left, right);
	}

	@Test
	public void mergeFinderLR_a3() {
		Resource left = input.getA3Left();
		Resource right = input.getA3Right();

		testMergeLeftToRight(left, right, null);

		// And the merge's effect on the UML representation
		left = input.getA3LeftUML();
		right = input.getA3RightUML();
		assertCompareSame(left, right);
	}

	@Test
	public void addHomeInterface3WayNoConflict_b1() {
		Resource base = input.getB1Base();
		Resource left = input.getB1Left();
		Resource right = input.getB1Right();
		Comparison comparison = preMerge(left, right, base);

		List<Conflict> conflicts = comparison.getConflicts();

		assertThat(conflicts.size(), is(2));
		assertThat(conflicts, everyItem(isPseudoConflict()));

		List<Diff> leftDiffs = conflicts.stream() //
				.map(Conflict::getLeftDifferences).flatMap(List::stream) //
				.collect(Collectors.toList());
		List<Diff> rightDiffs = conflicts.stream() //
				.map(Conflict::getRightDifferences).flatMap(List::stream) //
				.collect(Collectors.toList());
		assertThat(leftDiffs.size(), is(3));
		assertThat(rightDiffs.size(), is(3));

		assertThat(leftDiffs, hasItem(matches(Diff.class, "Home interface added to package",
				addedToReference("b1", "homeInterface", "b1.ThingHome"))));
		assertThat(leftDiffs, hasItem(matches(Diff.class, "Home interface set in Thing bean",
				EMFComparePredicates.changedReference("b1.Thing", "homeInterface", null, "b1.ThingHome"))));
		assertThat(leftDiffs, hasItem(matches(Diff.class, "Bean set in ThingHome interface",
				EMFComparePredicates.changedReference("b1.ThingHome", "bean", null, "b1.Thing"))));

		// And the UML representation is effectively the same, too
		assertCompareSame(left, right, base, true);
	}

	@Test
	public void addHomeInterface3WayConflict_b2() {
		Resource base = input.getB2Base();
		Resource left = input.getB2Left();
		Resource right = input.getB2Right();
		Comparison comparison = preMerge(left, right, base);

		List<Conflict> conflicts = newArrayList(comparison.getConflicts());
		conflicts.removeIf(c -> c.getKind() == ConflictKind.PSEUDO);

		assertThat(conflicts.size(), is(1));

		List<Diff> leftDiffs = conflicts.get(0).getLeftDifferences();
		List<Diff> rightDiffs = conflicts.get(0).getRightDifferences();
		assertThat(leftDiffs.size(), is(2));
		assertThat(rightDiffs.size(), is(2));

		assertThat(leftDiffs, hasItem(matches(Diff.class, "Left Thing home interface",
				changedReference("b2.Thing", "homeInterface", null, "b2.Home1"))));
		assertThat(leftDiffs, hasItem(matches(Diff.class, "Left home interface bean",
				changedReference("b2.Home1", "bean", null, "b2.Thing"))));
		assertThat(rightDiffs, hasItem(matches(Diff.class, "Right Whatsit home interface",
				changedReference("b2.Whatsit", "homeInterface", null, "b2.Home1"))));
		assertThat(rightDiffs, hasItem(matches(Diff.class, "Right home interface bean",
				changedReference("b2.Home1", "bean", null, "b2.Whatsit"))));
	}

	@Test
	public void mergeHomeInterface3WayConflictLR_b2() {
		Resource base = input.getB2Base();
		Resource left = input.getB2Left();
		Resource right = input.getB2Right();

		testMergeLeftToRight(left, right, base, true);

		// And the merge's effect on the UML representation
		base = input.getB2BaseUML();
		left = input.getB2LeftUML();
		right = input.getB2RightUML();
		assertCompareSame(left, right, base, true);
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

	/**
	 * Assert that, after comparison, the {@code scope} has only collected façade resource URIs.
	 * 
	 * @param scope
	 *            a comparison scope
	 */
	public void verifyComparisonScope(IComparisonScope scope) {
		scope.getResourceURIs().stream().forEach(
				uri -> assertThat("Not a façade URI", uri, endsWith("." + J2EEResource.FILE_EXTENSION)));
	}
}
