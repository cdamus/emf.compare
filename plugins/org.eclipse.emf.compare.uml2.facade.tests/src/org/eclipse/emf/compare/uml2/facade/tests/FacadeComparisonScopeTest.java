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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.compare.facade.IFacadeProvider;
import org.eclipse.emf.compare.facade.internal.match.FacadeMatchEngine;
import org.eclipse.emf.compare.match.IComparisonFactory;
import org.eclipse.emf.compare.match.eobject.IEObjectMatcher;
import org.eclipse.emf.compare.scope.DefaultComparisonScope;
import org.eclipse.emf.compare.scope.IComparisonScope;
import org.eclipse.emf.compare.uml2.facade.tests.data.UMLInputData;
import org.eclipse.emf.compare.uml2.facade.tests.j2ee.internal.providers.J2EEFacadeProvider;
import org.eclipse.emf.compare.uml2.facade.tests.opaqexpr.OpaqueExpression;
import org.eclipse.emf.compare.uml2.facade.tests.opaqexpr.internal.providers.OpaqexprFacadeProvider;
import org.eclipse.emf.compare.uml2.tests.AbstractUMLInputData;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Unit tests for the {@code FacadeComparisonScope} class.
 *
 * @author Christian W. Damus
 */
@SuppressWarnings({"boxing", "restriction" })
public class FacadeComparisonScopeTest extends AbstractFacadeTest {

	private UMLInputData input = new UMLInputData();

	/**
	 * Initializes me.
	 */
	public FacadeComparisonScopeTest() {
		super();
	}

	@Test
	public void facadeResourceContents() {
		IComparisonScope scope = scope(input.getO1Left(), input.getO1Right());

		Iterator<? extends EObject> iter = scope.getCoveredEObjects((Resource)scope.getLeft());
		assertThat(iter, instanceOf(TreeIterator.class));
		TreeIterator<? extends EObject> left = (TreeIterator<? extends EObject>)iter;

		// Implicitly asserting hasNext()
		assertThat(left.next(), instanceOf(org.eclipse.uml2.uml.Package.class));
		assertThat(left.next(), instanceOf(OpaqueExpression.class));
		for (int i = 0; i < 4; i++) {
			assertThat(left.next(), instanceOf(Map.Entry.class));
		}

		assertThat(left.hasNext(), is(false));
	}

	@Test
	public void facadeResourceContents_prune() {
		IComparisonScope scope = scope(input.getO1Left(), input.getO1Right());

		Iterator<? extends EObject> iter = scope.getCoveredEObjects((Resource)scope.getLeft());
		assertThat(iter, instanceOf(TreeIterator.class));
		TreeIterator<? extends EObject> left = (TreeIterator<? extends EObject>)iter;

		// Implicitly asserting hasNext()
		assertThat(left.next(), instanceOf(org.eclipse.uml2.uml.Package.class));
		assertThat(left.next(), instanceOf(OpaqueExpression.class));
		left.prune();

		assertThat(left.hasNext(), is(false));
	}

	@Test
	public void facadeObjectContents() {
		IComparisonScope scope = scope(input.getO1Left(), input.getO1Right());

		Iterator<? extends EObject> iter = scope
				.getChildren(((Resource)scope.getLeft()).getContents().get(0));
		assertThat(iter, instanceOf(TreeIterator.class));
		TreeIterator<? extends EObject> left = (TreeIterator<? extends EObject>)iter;

		// Implicitly asserting hasNext()
		assertThat(left.next(), instanceOf(OpaqueExpression.class));
		for (int i = 0; i < 4; i++) {
			assertThat(left.next(), instanceOf(Map.Entry.class));
		}

		assertThat(left.hasNext(), is(false));
	}

	@Test
	public void facadeObjectContents_prune() {
		IComparisonScope scope = scope(input.getO1Left(), input.getO1Right());

		Iterator<? extends EObject> iter = scope
				.getChildren(((Resource)scope.getLeft()).getContents().get(0));
		assertThat(iter, instanceOf(TreeIterator.class));
		TreeIterator<? extends EObject> left = (TreeIterator<? extends EObject>)iter;

		// Implicitly asserting hasNext()
		assertThat(left.next(), instanceOf(OpaqueExpression.class));
		left.prune();

		assertThat(left.hasNext(), is(false));
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
	protected AbstractUMLInputData getInput() {
		return input;
	}

	IComparisonScope scope(Resource left, Resource right) {
		List<IFacadeProvider.Factory> providerFactories = Arrays.asList(new J2EEFacadeProvider.Factory(),
				new OpaqexprFacadeProvider.Factory());
		IFacadeProvider.Factory composed = providerFactories.stream().reduce(IFacadeProvider.Factory::compose)
				.get();

		IFacadeProvider.Factory.Registry facadeProviderRegistry = mock(
				IFacadeProvider.Factory.Registry.class);
		when(facadeProviderRegistry.getFacadeProviderFactories(any())).thenReturn(providerFactories);
		when(facadeProviderRegistry.getFacadeProviderFactory(any())).thenReturn(composed);

		class MyFacadeMatchEngine extends FacadeMatchEngine {
			/**
			 * Initializes me with mocks for the components that we don't need.
			 */
			MyFacadeMatchEngine() {
				super(mock(IEObjectMatcher.class), mock(IComparisonFactory.class), facadeProviderRegistry);
			}

			IComparisonScope scope() {
				IComparisonScope delegate = new DefaultComparisonScope(left, right, null);
				return wrap(delegate, facadeProviderRegistry.getFacadeProviderFactory(delegate));
			}
		}

		return new MyFacadeMatchEngine().scope();
	}
}
