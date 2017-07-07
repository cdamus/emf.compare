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
package org.eclipse.emf.compare.uml2.facade.tests.j2ee.internal.adapters;

import static org.eclipse.emf.compare.uml2.facade.tests.j2ee.internal.adapters.BeanAdapter.isBeanClass;
import static org.eclipse.emf.compare.uml2.facade.tests.j2ee.internal.adapters.FinderAdapter.isFinder;
import static org.eclipse.emf.compare.uml2.facade.tests.j2ee.internal.adapters.HomeInterfaceAdapter.isHomeInterface;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.util.List;
import java.util.Set;

import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.compare.facade.SyncDirectionKind;
import org.eclipse.emf.compare.uml2.facade.tests.j2ee.Bean;
import org.eclipse.emf.compare.uml2.facade.tests.j2ee.Finder;
import org.eclipse.emf.compare.uml2.facade.tests.j2ee.HomeInterface;
import org.eclipse.emf.compare.uml2.facade.tests.j2ee.NamedElement;
import org.eclipse.emf.compare.uml2.facade.tests.j2eeprofile.J2EEProfilePackage;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.Interface;
import org.eclipse.uml2.uml.PackageableElement;
import org.eclipse.uml2.uml.Type;
import org.eclipse.uml2.uml.UMLPackage;

/**
 * Façade adapter for packages in a J2EE model.
 *
 * @author Christian W. Damus
 */
public class PackageAdapter extends NamedElementAdapter {

	/**
	 * Initializes me. There is no package stereotype.
	 * 
	 * @param facade
	 *            the package façade
	 * @param umlElement
	 *            the UML package element
	 */
	public PackageAdapter(org.eclipse.emf.compare.uml2.facade.tests.j2ee.Package facade,
			org.eclipse.uml2.uml.Package umlElement) {

		super(facade, umlElement);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isAdapterForType(Object type) {
		return (type == org.eclipse.emf.compare.uml2.facade.tests.j2ee.Package.class)
				|| (type == PackageAdapter.class) //
				|| super.isAdapterForType(type);
	}

	@Override
	public org.eclipse.emf.compare.uml2.facade.tests.j2ee.Package getFacade() {
		return (org.eclipse.emf.compare.uml2.facade.tests.j2ee.Package)super.getFacade();
	}

	@Override
	public org.eclipse.uml2.uml.Package getUnderlyingElement() {
		return (org.eclipse.uml2.uml.Package)super.getUnderlyingElement();
	}

	/**
	 * Ensures that the façade and its UML element are connected by an adapter.
	 * 
	 * @param facade
	 *            a package façade
	 * @param package_
	 *            the UML package element
	 * @return the existing or new adapter
	 */
	static PackageAdapter connect(org.eclipse.emf.compare.uml2.facade.tests.j2ee.Package facade,
			org.eclipse.uml2.uml.Package package_) {

		return connect(facade, package_, PackageAdapter.class, PackageAdapter::new);
	}

	/**
	 * Obtains the adapter instance for some notifier.
	 * 
	 * @param notifier
	 *            a façade or UML model element
	 * @return the adapter, or {@code null}
	 */
	static PackageAdapter get(Notifier notifier) {
		return get(notifier, PackageAdapter.class);
	}

	/**
	 * Synchronize the owned beans from the façade to the UML model.
	 * 
	 * @param facade
	 *            the façade
	 * @param model
	 *            the UML element
	 */
	public void syncBeanToModel(org.eclipse.emf.compare.uml2.facade.tests.j2ee.Package facade,
			org.eclipse.uml2.uml.Package model) {

		// Add missing beans in the model
		Set<org.eclipse.uml2.uml.Class> legitBeanClasses = Sets.newHashSet();
		for (Bean bean : facade.getBeans()) {
			org.eclipse.uml2.uml.Class beanClass = getBeanClass(bean);
			if (beanClass != null) {
				legitBeanClasses.add(beanClass);
				BeanAdapter.connect(bean, beanClass).initialSync(SyncDirectionKind.TO_MODEL);
			}
		}

		// Destroy extraneous beans in the model
		List<Type> beanClassesToDestroy = Lists
				.newArrayList(Iterables.filter(model.getOwnedTypes(), BeanAdapter::isBeanClass));
		beanClassesToDestroy.removeAll(legitBeanClasses);
		beanClassesToDestroy.forEach(Element::destroy);
	}

	/**
	 * Synchronize the owned home-interfaces from the façade to the UML model.
	 * 
	 * @param facade
	 *            the façade
	 * @param model
	 *            the UML element
	 */
	public void syncHomeInterfaceToModel(org.eclipse.emf.compare.uml2.facade.tests.j2ee.Package facade,
			org.eclipse.uml2.uml.Package model) {

		// Add missing interfaces in the model
		Set<Interface> legitHomeInterfaces = Sets.newHashSet();
		for (HomeInterface home : facade.getHomeInterfaces()) {
			Interface homeInterface = getHomeInterface(home);
			if (homeInterface != null) {
				legitHomeInterfaces.add(homeInterface);
				HomeInterfaceAdapter.connect(home, homeInterface).initialSync(SyncDirectionKind.TO_MODEL);
			}
		}

		// Destroy extraneous interfaces in the model
		List<Type> homeInterfacesToDestroy = Lists
				.newArrayList(Iterables.filter(model.getOwnedTypes(), HomeInterfaceAdapter::isHomeInterface));
		homeInterfacesToDestroy.removeAll(legitHomeInterfaces);
		homeInterfacesToDestroy.forEach(Element::destroy);
	}

	/**
	 * Synchronize the owned finders from the façade to the UML model.
	 * 
	 * @param facade
	 *            the façade
	 * @param model
	 *            the UML element
	 */
	public void syncFinderToModel(org.eclipse.emf.compare.uml2.facade.tests.j2ee.Package facade,
			org.eclipse.uml2.uml.Package model) {

		// Add missing interfaces in the model
		Set<Interface> legitFinders = Sets.newHashSet();
		for (Finder finder : facade.getFinders()) {
			Interface finderInterface = getFinderInterface(finder);
			if (finderInterface != null) {
				legitFinders.add(finderInterface);
				FinderAdapter.connect(finder, finderInterface).initialSync(SyncDirectionKind.TO_MODEL);
			}
		}

		// Destroy extraneous interfaces in the model
		List<Type> findersToDestroy = Lists
				.newArrayList(Iterables.filter(model.getOwnedTypes(), FinderAdapter::isFinder));
		findersToDestroy.removeAll(legitFinders);
		findersToDestroy.forEach(Element::destroy);
	}

	/**
	 * Finds the UML bean class corresponding to a bean façade, creating it if necessary.
	 * 
	 * @param bean
	 *            a bean façade
	 * @return its UML representation
	 */
	org.eclipse.uml2.uml.Class getBeanClass(Bean bean) {
		org.eclipse.uml2.uml.Class result = null;

		// Look for existing adapter
		BeanAdapter adapter = BeanAdapter.get(bean);
		if (adapter != null) {
			result = adapter.getUnderlyingElement();
		} else if (bean.getPackage() != null) {
			// It's not deleted, so ensure the underlying model
			org.eclipse.uml2.uml.Package umlPackage = getUnderlyingElement();
			if (bean.getName() != null) {
				result = (org.eclipse.uml2.uml.Class)umlPackage.getOwnedType(bean.getName(), false,
						UMLPackage.Literals.CLASS, true);
			} else {
				result = applyStereotype(umlPackage.createOwnedClass(null, false),
						J2EEProfilePackage.Literals.BEAN);
			}

			BeanAdapter.connect(bean, result);
		}

		return result;
	}

	/**
	 * Finds the UML interface corresponding to a home-interface façade, creating it if necessary.
	 * 
	 * @param homeInterface
	 *            a home-interface façade
	 * @return its UML representation
	 */
	Interface getHomeInterface(HomeInterface homeInterface) {
		Interface result = null;

		// Look for existing adapter
		HomeInterfaceAdapter adapter = HomeInterfaceAdapter.get(homeInterface);
		if (adapter != null) {
			result = adapter.getUnderlyingElement();
		} else if (homeInterface.getPackage() != null) {
			// It's not deleted, so ensure the underlying model
			org.eclipse.uml2.uml.Package umlPackage = getUnderlyingElement();
			if (homeInterface.getName() != null) {
				result = (Interface)umlPackage.getOwnedType(homeInterface.getName(), false,
						UMLPackage.Literals.INTERFACE, true);
			} else {
				result = applyStereotype(umlPackage.createOwnedInterface(null),
						J2EEProfilePackage.Literals.HOME_INTERFACE);
			}

			HomeInterfaceAdapter.connect(homeInterface, result);
		}

		return result;
	}

	/**
	 * Finds the UML interface corresponding to a finder façade, creating it if necessary.
	 * 
	 * @param finder
	 *            a finder façade
	 * @return its UML representation
	 */
	Interface getFinderInterface(Finder finder) {
		Interface result = null;

		// Look for existing adapter
		FinderAdapter adapter = FinderAdapter.get(finder);
		if (adapter != null) {
			result = adapter.getUnderlyingElement();
		} else if (finder.getPackage() != null) {
			// It's not deleted, so ensure the underlying model
			org.eclipse.uml2.uml.Package umlPackage = getUnderlyingElement();
			if (finder.getName() != null) {
				result = (Interface)umlPackage.getOwnedType(finder.getName(), false,
						UMLPackage.Literals.INTERFACE, true);
			} else {
				result = applyStereotype(umlPackage.createOwnedInterface(null),
						J2EEProfilePackage.Literals.HOME_INTERFACE);
			}

			FinderAdapter.connect(finder, result);
		}

		return result;
	}

	/**
	 * Synchronize the owned beans from the UML model to the façade.
	 * 
	 * @param model
	 *            the UML element
	 * @param facade
	 *            the façade
	 */
	public void syncPackagedElementToFacade(org.eclipse.uml2.uml.Package model,
			org.eclipse.emf.compare.uml2.facade.tests.j2ee.Package facade) {

		// Add missing beans in the façade
		Set<NamedElement> legitElements = Sets.newHashSet();
		for (PackageableElement element : model.getPackagedElements()) {
			if (isBeanClass(element)) {
				org.eclipse.uml2.uml.Class beanClass = (org.eclipse.uml2.uml.Class)element;
				Bean bean = getBean(beanClass);
				if (bean != null) {
					legitElements.add(bean);
					BeanAdapter.connect(bean, beanClass).initialSync(SyncDirectionKind.TO_FACADE);
				}
			} else if (isHomeInterface(element)) {
				Interface homeInterface = (Interface)element;
				HomeInterface home = getHomeInterface(homeInterface);
				if (home != null) {
					legitElements.add(home);
					HomeInterfaceAdapter.connect(home, homeInterface)
							.initialSync(SyncDirectionKind.TO_FACADE);
				}
			} else if (isFinder(element)) {
				Interface finderInterface = (Interface)element;
				Finder finder = getFinder(finderInterface);
				if (finder != null) {
					legitElements.add(finder);
					FinderAdapter.connect(finder, finderInterface).initialSync(SyncDirectionKind.TO_FACADE);
				}
			}
		}

		// Destroy extraneous elements in the façade
		List<NamedElement> elementsToDestroy = Lists.newArrayList(facade.getMembers());
		elementsToDestroy.removeAll(legitElements);
		elementsToDestroy.forEach(EcoreUtil::remove);
	}

	/**
	 * Finds the bean façade corresponding to a UML class, creating it if necessary.
	 * 
	 * @param beanClass
	 *            a possible bean class
	 * @return its façade, or {@code null} if the class is not a valid bean
	 */
	Bean getBean(org.eclipse.uml2.uml.Class beanClass) {
		Bean result = null;

		// Look for existing adapter
		BeanAdapter adapter = BeanAdapter.get(beanClass);
		if (adapter != null) {
			result = adapter.getFacade();
		} else if (beanClass.getPackage() != null) {
			// It's not deleted, so ensure the underlying model
			org.eclipse.emf.compare.uml2.facade.tests.j2ee.Package package_ = getFacade();
			if (beanClass.getName() != null) {
				result = package_.getBean(beanClass.getName(), false, true);
			} else {
				result = package_.createBean(beanClass.getName());
			}

			BeanAdapter.connect(result, beanClass);
		}

		return result;
	}

	/**
	 * Finds the home-interface façade corresponding to a UML interface, creating it if necessary.
	 * 
	 * @param homeInterface
	 *            a possible UML home interface
	 * @return its façade, or {@code null} if the interface is not a valid home-interface
	 */
	HomeInterface getHomeInterface(Interface homeInterface) {
		HomeInterface result = null;

		// Look for existing adapter
		HomeInterfaceAdapter adapter = HomeInterfaceAdapter.get(homeInterface);
		if (adapter != null) {
			result = adapter.getFacade();
		} else if (homeInterface.getPackage() != null) {
			// It's not deleted, so ensure the underlying model
			org.eclipse.emf.compare.uml2.facade.tests.j2ee.Package package_ = getFacade();
			if (homeInterface.getName() != null) {
				result = package_.getHomeInterface(homeInterface.getName(), false, true);
			} else {
				result = package_.createHomeInterface(homeInterface.getName());
			}

			HomeInterfaceAdapter.connect(result, homeInterface);
		}

		return result;
	}

	/**
	 * Finds the finder façade corresponding to a UML interface, creating it if necessary.
	 * 
	 * @param finderInterface
	 *            a possible UML finder interface
	 * @return its façade, or {@code null} if the interface is not a valid finder-interface
	 */
	Finder getFinder(Interface finderInterface) {
		Finder result = null;

		// Look for existing adapter
		FinderAdapter adapter = FinderAdapter.get(finderInterface);
		if (adapter != null) {
			result = adapter.getFacade();
		} else if (finderInterface.getPackage() != null) {
			// It's not deleted, so ensure the underlying model
			org.eclipse.emf.compare.uml2.facade.tests.j2ee.Package package_ = getFacade();
			if (finderInterface.getName() != null) {
				result = package_.getFinder(finderInterface.getName(), false, true);
			} else {
				result = package_.createFinder(finderInterface.getName());
			}

			FinderAdapter.connect(result, finderInterface);
		}

		return result;
	}
}
