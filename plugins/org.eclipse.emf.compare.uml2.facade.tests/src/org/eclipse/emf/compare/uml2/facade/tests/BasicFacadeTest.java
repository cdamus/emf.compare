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

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assume.assumeThat;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.emf.compare.uml2.facade.tests.data.BasicFacadeInputData;
import org.eclipse.emf.compare.uml2.facade.tests.j2ee.Bean;
import org.eclipse.emf.compare.uml2.facade.tests.j2ee.BeanKind;
import org.eclipse.emf.compare.uml2.facade.tests.j2ee.HomeInterface;
import org.eclipse.emf.compare.uml2.facade.tests.j2ee.Package;
import org.eclipse.emf.compare.uml2.tests.AbstractUMLInputData;
import org.eclipse.emf.compare.uml2.tests.AbstractUMLTest;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.Interface;
import org.eclipse.uml2.uml.Usage;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;

/**
 * This is the {@code BasicFacadeTest} type. Enjoy.
 *
 * @author Christian W. Damus
 */
@SuppressWarnings({"nls", "boxing" })
public class BasicFacadeTest extends AbstractUMLTest {

	private BasicFacadeInputData input = new BasicFacadeInputData();

	/**
	 * Initializes me.
	 */
	public BasicFacadeTest() {
		super();
	}

	@Test
	public void simpleBean() {
		Package package_ = requirePackage(input.getA1Left(), "a1");
		Bean thing = requireBean(package_, "Thing");

		assertThat(thing.getKind(), is(BeanKind.ENTITY));
	}

	@Test
	public void changeBeanKindFromFacade() {
		Package package_ = requirePackage(input.getA1Left(), "a1");
		Bean thing = requireBean(package_, "Thing");

		thing.setKind(BeanKind.MESSAGEDRIVEN);
		assertThat(thing.getUnderlyingElement(), hasKind(BeanKind.MESSAGEDRIVEN));
	}

	@Test
	public void changeBeanKindFromUML() {
		Package package_ = requirePackage(input.getA1Left(), "a1");
		Bean thing = requireBean(package_, "Thing");

		setKind(thing.getUnderlyingElement(), BeanKind.MESSAGEDRIVEN);
		assertThat(thing.getUnderlyingElement(), hasKind(BeanKind.MESSAGEDRIVEN));
	}

	@Test
	public void addBeanInFacade() {
		Package package_ = requirePackage(input.getA1Left(), "a1");
		Bean newBean = package_.createBean("Doodad");

		org.eclipse.uml2.uml.Class class_ = (org.eclipse.uml2.uml.Class)newBean.getUnderlyingElement();
		assertThat("No UML class", class_, notNullValue());
		assertThat("Wrong class name", class_.getName(), is("Doodad"));
		assertThat("Class not stereotyped as «Bean»", class_, hasStereotype("Bean"));
	}

	@Test
	public void addBeanInUML() {
		Package package_ = requirePackage(input.getA1Left(), "a1");

		org.eclipse.uml2.uml.Package uml = (org.eclipse.uml2.uml.Package)package_.getUnderlyingElement();
		org.eclipse.uml2.uml.Class class_ = uml.createOwnedClass("Doodad", false);
		applyStereotype(class_, "Bean");

		Bean newBean = requireBean(package_, "Doodad");
		assertThat(newBean.getUnderlyingElement(), is(class_));
	}

	@Test
	public void simpleHomeInterface() {
		Package package_ = requirePackage(input.getA2Left(), "a2");
		HomeInterface thingHome = requireHomeInterface(package_, "ThingHome");

		Bean thing = thingHome.getBean();
		assertThat("Home interface has no bean", thing, notNullValue());
		assertThat("Wrong bean", thing.getName(), is("Thing"));
	}

	@Test
	public void changeHomeInterfaceBeanFromFacade() {
		Package package_ = requirePackage(input.getA2Left(), "a2");
		HomeInterface thingHome = requireHomeInterface(package_, "ThingHome");

		Bean whatsit = requireBean(package_, "Whatsit");
		thingHome.setBean(whatsit);

		org.eclipse.uml2.uml.Class whatsitClass = (org.eclipse.uml2.uml.Class)whatsit.getUnderlyingElement();
		Interface thingHomeInterface = (Interface)thingHome.getUnderlyingElement();

		List<Usage> usages = thingHomeInterface.getClientDependencies().stream() //
				.filter(Usage.class::isInstance).map(Usage.class::cast).collect(Collectors.toList());
		assertThat("Extra usage created or usage deleted", usages.size(), is(1));
		assertThat("Wrong usage relationship", usages.get(0).getSuppliers(), is(singletonList(whatsitClass)));
	}

	@Test
	public void changeHomeInterfaceBeanFromUML1() {
		Package package_ = requirePackage(input.getA2Left(), "a2");
		HomeInterface thingHome = requireHomeInterface(package_, "ThingHome");

		Bean whatsit = requireBean(package_, "Whatsit");
		org.eclipse.uml2.uml.Class whatsitClass = (org.eclipse.uml2.uml.Class)whatsit.getUnderlyingElement();
		Interface thingHomeInterface = (Interface)thingHome.getUnderlyingElement();

		List<Usage> usages = thingHomeInterface.getClientDependencies().stream() //
				.filter(Usage.class::isInstance).map(Usage.class::cast).collect(Collectors.toList());
		assertThat("Should have an unique usage", usages.size(), is(1));

		// The optimal way to do it
		assumeThat("Invalid usage", usages.get(0).getSuppliers().size(), is(1));
		usages.get(0).getSuppliers().set(0, whatsitClass);

		assertThat("Bean not updated", thingHome.getBean(), is(whatsit));
	}

	@Test
	public void changeHomeInterfaceBeanFromUML2() {
		Package package_ = requirePackage(input.getA2Left(), "a2");
		HomeInterface thingHome = requireHomeInterface(package_, "ThingHome");

		Bean whatsit = requireBean(package_, "Whatsit");
		org.eclipse.uml2.uml.Class whatsitClass = (org.eclipse.uml2.uml.Class)whatsit.getUnderlyingElement();
		Interface thingHomeInterface = (Interface)thingHome.getUnderlyingElement();

		List<Usage> usages = thingHomeInterface.getClientDependencies().stream() //
				.filter(Usage.class::isInstance).map(Usage.class::cast).collect(Collectors.toList());
		assertThat("Should have an unique usage", usages.size(), is(1));

		// A different way to do it
		usages.get(0).getSuppliers().clear();
		usages.get(0).getSuppliers().add(whatsitClass);

		assertThat("Bean not updated", thingHome.getBean(), is(whatsit));
	}

	//
	// Test framework
	//

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected AbstractUMLInputData getInput() {
		return input;
	}

	Package requirePackage(Resource resource, String name) {
		Package result = input.getPackage(resource);

		assertThat(String.format("No package '%s'", name), result, notNullValue());
		assertThat("Wrong package name", result.getName(), is(name));

		return result;
	}

	Bean requireBean(Package package_, String name) {
		Bean result = package_.getBean(name);

		assertThat(String.format("No bean '%s'", name), result, notNullValue());

		return result;
	}

	HomeInterface requireHomeInterface(Package package_, String name) {
		HomeInterface result = package_.getHomeInterface(name);

		assertThat(String.format("No home-interface '%s'", name), result, notNullValue());

		return result;
	}

	@SuppressWarnings({"unchecked", "rawtypes" })
	void setKind(EObject owner, BeanKind kind) {
		EObject target = resolveBeanKindTarget(owner);
		assertThat("Cannot set bean kind of " + owner, target, notNullValue());
		Optional<EAttribute> kindAttr = resolveKindAttribute(target);
		kindAttr.ifPresent(a -> target.eSet(a,
				Enum.valueOf((Class<Enum>)a.getEAttributeType().getInstanceClass(), kind.name())));
	}

	private Optional<EAttribute> resolveKindAttribute(EObject owner) {
		return owner.eClass().getEAllAttributes().stream() //
				.filter(a -> a.getEAttributeType() instanceof EEnum)
				.filter(a -> "BeanKind".equals(a.getEAttributeType().getName())).findAny();
	}

	private EObject resolveBeanKindTarget(EObject object) {
		EObject result = null;

		Optional<EAttribute> kindAttr = resolveKindAttribute(object);
		if (kindAttr.isPresent()) {
			result = object;
		} else if (object instanceof Element) {
			result = ((Element)object).getStereotypeApplications().stream()
					.filter(st -> resolveKindAttribute(st).isPresent()).findAny().orElse(null);
		}

		return result;
	}

	Matcher<EObject> hasKind(BeanKind kind) {
		return new TypeSafeDiagnosingMatcher<EObject>() {
			/**
			 * {@inheritDoc}
			 */
			public void describeTo(Description description) {
				description.appendText("has Bean kind ").appendValue(kind);
			}

			/**
			 * {@inheritDoc}
			 */
			@SuppressWarnings("boxing")
			@Override
			protected boolean matchesSafely(EObject item, Description mismatchDescription) {
				EObject target = resolveBeanKindTarget(item);
				if (target == null) {
					mismatchDescription.appendText("has no 'kind' attribute");
					return false;
				}

				Optional<EAttribute> kindAttr = resolveKindAttribute(target);

				return kindAttr.map(a -> {
					Enum<?> actual = (Enum<?>)target.eGet(a);

					boolean result = actual.name().equals(kind.name());

					if (!result) {
						mismatchDescription.appendText("kind was " + actual);
					}

					return result;
				}).orElseGet(() -> {
					mismatchDescription.appendText("has no 'kind' attribute");
					return false;
				});
			}
		};
	}

	Matcher<Element> hasStereotype(String name) {
		return new TypeSafeMatcher<Element>() {
			/**
			 * {@inheritDoc}
			 */
			public void describeTo(Description description) {
				description.appendText(String.format("stereotyped as «%s»", name));
			}

			/**
			 * {@inheritDoc}
			 */
			@Override
			protected boolean matchesSafely(Element item) {
				return item.getAppliedStereotypes().stream().anyMatch(s -> name.equals(s.getName()));
			}
		};
	}

	@SuppressWarnings("boxing")
	void applyStereotype(Element element, String name) {
		Optional<EObject> application = element.getApplicableStereotypes().stream() //
				.filter(s -> name.equals(s.getName())).findAny().map(s -> element.applyStereotype(s));

		assertThat("Stereotype not applied: " + name, application.isPresent(), is(true));
	}
}
