/**
 * Copyright (c) 2011, 2012 Obeo.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Obeo - initial API and implementation
 */
package org.eclipse.emf.compare.uml2diff.provider;

import java.util.Collection;
import java.util.List;

import org.eclipse.emf.common.notify.AdapterFactory;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.util.ResourceLocator;
import org.eclipse.emf.compare.diff.metamodel.DiffPackage;
import org.eclipse.emf.compare.diff.metamodel.UpdateReference;
import org.eclipse.emf.compare.diff.provider.UpdateReferenceItemProvider;
import org.eclipse.emf.compare.uml2diff.UML2DiffFactory;
import org.eclipse.emf.compare.uml2diff.UML2DiffPackage;
import org.eclipse.emf.compare.uml2diff.UMLStereotypeUpdateReference;
import org.eclipse.emf.compare.util.AdapterUtils;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.edit.provider.ComposeableAdapterFactory;
import org.eclipse.emf.edit.provider.IEditingDomainItemProvider;
import org.eclipse.emf.edit.provider.IItemLabelProvider;
import org.eclipse.emf.edit.provider.IItemPropertyDescriptor;
import org.eclipse.emf.edit.provider.IItemPropertySource;
import org.eclipse.emf.edit.provider.IStructuredItemContentProvider;
import org.eclipse.emf.edit.provider.ITreeItemContentProvider;
import org.eclipse.emf.edit.provider.ItemPropertyDescriptor;
import org.eclipse.emf.edit.provider.ViewerNotification;

/**
 * This is the item provider adapter for a
 * {@link org.eclipse.emf.compare.uml2diff.UMLStereotypeUpdateReference} object. <!-- begin-user-doc --> <!--
 * end-user-doc -->
 * 
 * @generated
 */
public class UMLStereotypeUpdateReferenceItemProvider extends UpdateReferenceItemProvider implements IEditingDomainItemProvider, IStructuredItemContentProvider, ITreeItemContentProvider, IItemLabelProvider, IItemPropertySource {
	/**
	 * This constructs an instance from a factory and a notifier. <!-- begin-user-doc --> <!-- end-user-doc
	 * -->
	 * 
	 * @generated
	 */
	public UMLStereotypeUpdateReferenceItemProvider(AdapterFactory adapterFactory) {
		super(adapterFactory);
	}

	/**
	 * This returns the property descriptors for the adapted class. <!-- begin-user-doc --> <!-- end-user-doc
	 * -->
	 * 
	 * @generated
	 */
	@Override
	public List<IItemPropertyDescriptor> getPropertyDescriptors(Object object) {
		if (itemPropertyDescriptors == null) {
			super.getPropertyDescriptors(object);

			addHideElementsPropertyDescriptor(object);
			addIsCollapsedPropertyDescriptor(object);
			addStereotypePropertyDescriptor(object);
		}
		return itemPropertyDescriptors;
	}

	/**
	 * This adds a property descriptor for the Hide Elements feature. <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @generated
	 */
	protected void addHideElementsPropertyDescriptor(Object object) {
		itemPropertyDescriptors
				.add(createItemPropertyDescriptor(
						((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
						getResourceLocator(),
						getString("_UI_AbstractDiffExtension_hideElements_feature"), //$NON-NLS-1$
						getString(
								"_UI_PropertyDescriptor_description", "_UI_AbstractDiffExtension_hideElements_feature", "_UI_AbstractDiffExtension_type"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						DiffPackage.Literals.ABSTRACT_DIFF_EXTENSION__HIDE_ELEMENTS, true, false, true, null,
						null, null));
	}

	/**
	 * This adds a property descriptor for the Is Collapsed feature. <!-- begin-user-doc --> <!-- end-user-doc
	 * -->
	 * 
	 * @generated
	 */
	protected void addIsCollapsedPropertyDescriptor(Object object) {
		itemPropertyDescriptors
				.add(createItemPropertyDescriptor(
						((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
						getResourceLocator(),
						getString("_UI_AbstractDiffExtension_isCollapsed_feature"), //$NON-NLS-1$
						getString(
								"_UI_PropertyDescriptor_description", "_UI_AbstractDiffExtension_isCollapsed_feature", "_UI_AbstractDiffExtension_type"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						DiffPackage.Literals.ABSTRACT_DIFF_EXTENSION__IS_COLLAPSED, true, false, false,
						ItemPropertyDescriptor.BOOLEAN_VALUE_IMAGE, null, null));
	}

	/**
	 * This adds a property descriptor for the Stereotype feature. <!-- begin-user-doc --> <!-- end-user-doc
	 * -->
	 * 
	 * @generated
	 */
	protected void addStereotypePropertyDescriptor(Object object) {
		itemPropertyDescriptors
				.add(createItemPropertyDescriptor(
						((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
						getResourceLocator(),
						getString("_UI_UMLStereotypePropertyChange_stereotype_feature"), //$NON-NLS-1$
						getString(
								"_UI_PropertyDescriptor_description", "_UI_UMLStereotypePropertyChange_stereotype_feature", "_UI_UMLStereotypePropertyChange_type"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						UML2DiffPackage.Literals.UML_STEREOTYPE_PROPERTY_CHANGE__STEREOTYPE, true, false,
						true, null, null, null));
	}

	/**
	 * This returns UMLStereotypeUpdateReference.gif. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated NOT
	 */
	@Override
	public Object getImage(Object object) {
		return super.getImage(object);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected EObject getSemanticElement(UpdateReference updateReference) {
		EObject semanticElement = null;

		final EObject leftElement = updateReference.getLeftElement();
		final EObject rightElement = updateReference.getRightElement();
		final EReference eRef = updateReference.getReference();

		if (canAccessReference(eRef, leftElement)) {
			semanticElement = (EObject)leftElement.eGet(eRef);
		} else if (canAccessReference(eRef, rightElement)) {
			semanticElement = (EObject)rightElement.eGet(eRef);
		} else {
			semanticElement = updateReference.getRightTarget();
		}
		return semanticElement;
	}

	private boolean canAccessReference(EReference reference, EObject leftElement) {
		return leftElement.eClass().getEAllStructuralFeatures().contains(reference);
	}

	/**
	 * This returns the label text for the adapted class. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated NOT
	 */
	@Override
	public String getText(Object object) {
		final UpdateReference operation = (UpdateReference)object;

		final String elementLabel = AdapterUtils.getItemProviderText(operation.getLeftElement());
		final String referenceLabel = AdapterUtils.getItemProviderText(operation.getReference());
		final String leftValueLabel = AdapterUtils.getItemProviderText(operation.getLeftTarget());
		final String rightValueLabel = AdapterUtils.getItemProviderText(operation.getRightTarget());

		final String diffLabel;
		if (operation.isRemote()) {
			diffLabel = getString("_UI_RemoteStereotypeUpdateReference_type", new Object[] {referenceLabel, //$NON-NLS-1$
					elementLabel, leftValueLabel, rightValueLabel,});
		} else {
			if (operation.isConflicting()) {
				diffLabel = getString(
						"_UI_UpdateStereotypeReference_conflicting", new Object[] {referenceLabel, //$NON-NLS-1$
								elementLabel, rightValueLabel, leftValueLabel,});
			} else {
				diffLabel = getString(
						"_UI_UpdateStereotypeReference_type", new Object[] {referenceLabel, elementLabel, //$NON-NLS-1$
								rightValueLabel, leftValueLabel,});
			}
		}

		return diffLabel;
	}

	/**
	 * This handles model notifications by calling {@link #updateChildren} to update any cached children and
	 * by creating a viewer notification, which it passes to {@link #fireNotifyChanged}. <!-- begin-user-doc
	 * --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	@Override
	public void notifyChanged(Notification notification) {
		updateChildren(notification);

		switch (notification.getFeatureID(UMLStereotypeUpdateReference.class)) {
			case UML2DiffPackage.UML_STEREOTYPE_UPDATE_REFERENCE__IS_COLLAPSED:
				fireNotifyChanged(new ViewerNotification(notification, notification.getNotifier(), false,
						true));
				return;
		}
		super.notifyChanged(notification);
	}

	/**
	 * This adds {@link org.eclipse.emf.edit.command.CommandParameter}s describing the children that can be
	 * created under this object. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	@Override
	protected void collectNewChildDescriptors(Collection<Object> newChildDescriptors, Object object) {
		super.collectNewChildDescriptors(newChildDescriptors, object);

		newChildDescriptors.add(createChildParameter(DiffPackage.Literals.DIFF_ELEMENT__SUB_DIFF_ELEMENTS,
				UML2DiffFactory.eINSTANCE.createUMLAssociationChangeLeftTarget()));

		newChildDescriptors.add(createChildParameter(DiffPackage.Literals.DIFF_ELEMENT__SUB_DIFF_ELEMENTS,
				UML2DiffFactory.eINSTANCE.createUMLAssociationChangeRightTarget()));

		newChildDescriptors.add(createChildParameter(DiffPackage.Literals.DIFF_ELEMENT__SUB_DIFF_ELEMENTS,
				UML2DiffFactory.eINSTANCE.createUMLAssociationBranchChangeLeftTarget()));

		newChildDescriptors.add(createChildParameter(DiffPackage.Literals.DIFF_ELEMENT__SUB_DIFF_ELEMENTS,
				UML2DiffFactory.eINSTANCE.createUMLAssociationBranchChangeRightTarget()));

		newChildDescriptors.add(createChildParameter(DiffPackage.Literals.DIFF_ELEMENT__SUB_DIFF_ELEMENTS,
				UML2DiffFactory.eINSTANCE.createUMLDependencyBranchChangeLeftTarget()));

		newChildDescriptors.add(createChildParameter(DiffPackage.Literals.DIFF_ELEMENT__SUB_DIFF_ELEMENTS,
				UML2DiffFactory.eINSTANCE.createUMLDependencyBranchChangeRightTarget()));

		newChildDescriptors.add(createChildParameter(DiffPackage.Literals.DIFF_ELEMENT__SUB_DIFF_ELEMENTS,
				UML2DiffFactory.eINSTANCE.createUMLGeneralizationSetChangeLeftTarget()));

		newChildDescriptors.add(createChildParameter(DiffPackage.Literals.DIFF_ELEMENT__SUB_DIFF_ELEMENTS,
				UML2DiffFactory.eINSTANCE.createUMLGeneralizationSetChangeRightTarget()));

		newChildDescriptors.add(createChildParameter(DiffPackage.Literals.DIFF_ELEMENT__SUB_DIFF_ELEMENTS,
				UML2DiffFactory.eINSTANCE.createUMLDependencyChangeLeftTarget()));

		newChildDescriptors.add(createChildParameter(DiffPackage.Literals.DIFF_ELEMENT__SUB_DIFF_ELEMENTS,
				UML2DiffFactory.eINSTANCE.createUMLDependencyChangeRightTarget()));

		newChildDescriptors.add(createChildParameter(DiffPackage.Literals.DIFF_ELEMENT__SUB_DIFF_ELEMENTS,
				UML2DiffFactory.eINSTANCE.createUMLExtendChangeLeftTarget()));

		newChildDescriptors.add(createChildParameter(DiffPackage.Literals.DIFF_ELEMENT__SUB_DIFF_ELEMENTS,
				UML2DiffFactory.eINSTANCE.createUMLExtendChangeRightTarget()));

		newChildDescriptors.add(createChildParameter(DiffPackage.Literals.DIFF_ELEMENT__SUB_DIFF_ELEMENTS,
				UML2DiffFactory.eINSTANCE.createUMLExecutionSpecificationChangeLeftTarget()));

		newChildDescriptors.add(createChildParameter(DiffPackage.Literals.DIFF_ELEMENT__SUB_DIFF_ELEMENTS,
				UML2DiffFactory.eINSTANCE.createUMLExecutionSpecificationChangeRightTarget()));

		newChildDescriptors.add(createChildParameter(DiffPackage.Literals.DIFF_ELEMENT__SUB_DIFF_ELEMENTS,
				UML2DiffFactory.eINSTANCE.createUMLDestructionEventChangeLeftTarget()));

		newChildDescriptors.add(createChildParameter(DiffPackage.Literals.DIFF_ELEMENT__SUB_DIFF_ELEMENTS,
				UML2DiffFactory.eINSTANCE.createUMLDestructionEventChangeRightTarget()));

		newChildDescriptors.add(createChildParameter(DiffPackage.Literals.DIFF_ELEMENT__SUB_DIFF_ELEMENTS,
				UML2DiffFactory.eINSTANCE.createUMLIntervalConstraintChangeLeftTarget()));

		newChildDescriptors.add(createChildParameter(DiffPackage.Literals.DIFF_ELEMENT__SUB_DIFF_ELEMENTS,
				UML2DiffFactory.eINSTANCE.createUMLIntervalConstraintChangeRightTarget()));

		newChildDescriptors.add(createChildParameter(DiffPackage.Literals.DIFF_ELEMENT__SUB_DIFF_ELEMENTS,
				UML2DiffFactory.eINSTANCE.createUMLMessageChangeLeftTarget()));

		newChildDescriptors.add(createChildParameter(DiffPackage.Literals.DIFF_ELEMENT__SUB_DIFF_ELEMENTS,
				UML2DiffFactory.eINSTANCE.createUMLMessageChangeRightTarget()));

		newChildDescriptors.add(createChildParameter(DiffPackage.Literals.DIFF_ELEMENT__SUB_DIFF_ELEMENTS,
				UML2DiffFactory.eINSTANCE.createUMLStereotypeAttributeChangeLeftTarget()));

		newChildDescriptors.add(createChildParameter(DiffPackage.Literals.DIFF_ELEMENT__SUB_DIFF_ELEMENTS,
				UML2DiffFactory.eINSTANCE.createUMLStereotypeAttributeChangeRightTarget()));

		newChildDescriptors.add(createChildParameter(DiffPackage.Literals.DIFF_ELEMENT__SUB_DIFF_ELEMENTS,
				UML2DiffFactory.eINSTANCE.createUMLStereotypeUpdateAttribute()));

		newChildDescriptors.add(createChildParameter(DiffPackage.Literals.DIFF_ELEMENT__SUB_DIFF_ELEMENTS,
				UML2DiffFactory.eINSTANCE.createUMLStereotypeApplicationAddition()));

		newChildDescriptors.add(createChildParameter(DiffPackage.Literals.DIFF_ELEMENT__SUB_DIFF_ELEMENTS,
				UML2DiffFactory.eINSTANCE.createUMLStereotypeApplicationRemoval()));

		newChildDescriptors.add(createChildParameter(DiffPackage.Literals.DIFF_ELEMENT__SUB_DIFF_ELEMENTS,
				UML2DiffFactory.eINSTANCE.createUMLStereotypeReferenceChangeLeftTarget()));

		newChildDescriptors.add(createChildParameter(DiffPackage.Literals.DIFF_ELEMENT__SUB_DIFF_ELEMENTS,
				UML2DiffFactory.eINSTANCE.createUMLStereotypeReferenceChangeRightTarget()));

		newChildDescriptors.add(createChildParameter(DiffPackage.Literals.DIFF_ELEMENT__SUB_DIFF_ELEMENTS,
				UML2DiffFactory.eINSTANCE.createUMLStereotypeUpdateReference()));

		newChildDescriptors.add(createChildParameter(DiffPackage.Literals.DIFF_ELEMENT__SUB_DIFF_ELEMENTS,
				UML2DiffFactory.eINSTANCE.createUMLStereotypeReferenceOrderChange()));

		newChildDescriptors.add(createChildParameter(DiffPackage.Literals.DIFF_ELEMENT__SUB_DIFF_ELEMENTS,
				UML2DiffFactory.eINSTANCE.createUMLProfileApplicationAddition()));

		newChildDescriptors.add(createChildParameter(DiffPackage.Literals.DIFF_ELEMENT__SUB_DIFF_ELEMENTS,
				UML2DiffFactory.eINSTANCE.createUMLProfileApplicationRemoval()));

		newChildDescriptors.add(createChildParameter(DiffPackage.Literals.DIFF_ELEMENT__SUB_DIFF_ELEMENTS,
				UML2DiffFactory.eINSTANCE.createUMLElementChangeLeftTarget()));

		newChildDescriptors.add(createChildParameter(DiffPackage.Literals.DIFF_ELEMENT__SUB_DIFF_ELEMENTS,
				UML2DiffFactory.eINSTANCE.createUMLElementChangeRightTarget()));
	}

	/**
	 * Return the resource locator for this item provider's resources. <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @generated
	 */
	@Override
	public ResourceLocator getResourceLocator() {
		return UML2DiffEditPlugin.INSTANCE;
	}

}