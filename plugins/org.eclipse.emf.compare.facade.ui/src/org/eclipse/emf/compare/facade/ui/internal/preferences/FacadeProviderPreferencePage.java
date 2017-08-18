/*******************************************************************************
 * Copyright (c) 2014, 2017 Obeo, Christian W. Damus, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Obeo - initial API and implementation
 *     Christian W. Damus - integration of façade providers
 *******************************************************************************/
package org.eclipse.emf.compare.facade.ui.internal.preferences;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

import java.util.Set;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.emf.compare.facade.IFacadeProvider;
import org.eclipse.emf.compare.facade.internal.EMFCompareFacadePlugin;
import org.eclipse.emf.compare.facade.ui.internal.EMFCompareFacadeUIMessages;
import org.eclipse.emf.compare.rcp.EMFCompareRCPPlugin;
import org.eclipse.emf.compare.rcp.internal.extension.IItemDescriptor;
import org.eclipse.emf.compare.rcp.internal.extension.IItemRegistry;
import org.eclipse.emf.compare.rcp.internal.extension.impl.ItemUtil;
import org.eclipse.emf.compare.rcp.internal.preferences.EMFComparePreferences;
import org.eclipse.emf.compare.rcp.internal.tracer.TracingConstant;
import org.eclipse.emf.compare.rcp.ui.internal.preferences.DataHolder;
import org.eclipse.emf.compare.rcp.ui.internal.preferences.impl.InteractiveUIContent;
import org.eclipse.emf.compare.rcp.ui.internal.preferences.impl.InteractiveUIContent.InteractiveUIBuilder;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

/**
 * Preference page used to enable/disable façade providers.
 * 
 * @author <a href="mailto:arthur.daussy@obeo.fr">Arthur Daussy</a>
 * @author Christian W. Damus
 */
public class FacadeProviderPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	/** Data holder. */
	private DataHolder<IFacadeProvider.Factory> dataHolder = new DataHolder<IFacadeProvider.Factory>();

	/** {@link InteractiveUIContent}. */
	private InteractiveUIContent interactiveUI;

	/**
	 * Constructor.
	 */
	public FacadeProviderPreferencePage() {
		super();
	}

	/**
	 * Constructor.
	 * 
	 * @param title
	 *            .
	 * @param image
	 *            .
	 */
	public FacadeProviderPreferencePage(String title, ImageDescriptor image) {
		super(title, image);
	}

	/**
	 * Constructor.
	 * 
	 * @param title
	 *            .
	 */
	public FacadeProviderPreferencePage(String title) {
		super(title);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void init(IWorkbench workbench) {
		ScopedPreferenceStore store = new ScopedPreferenceStore(InstanceScope.INSTANCE,
				EMFCompareRCPPlugin.PLUGIN_ID);
		store.setSearchContexts(new IScopeContext[] {InstanceScope.INSTANCE, ConfigurationScope.INSTANCE });
		setPreferenceStore(store);
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().equalWidth(true).applyTo(container);
		Label introductionText = new Label(container, SWT.WRAP);
		introductionText.setText(
				EMFCompareFacadeUIMessages.getString("FacadeProviderPreferencePage.activeProviders.prompt")); //$NON-NLS-1$

		IItemRegistry<IFacadeProvider.Factory> registry = EMFCompareFacadePlugin.getDefault()
				.getFacadeProviderItemRegistry();
		Set<IItemDescriptor<IFacadeProvider.Factory>> activeProviders = ItemUtil.getActiveItems(registry,
				EMFCompareRCPPlugin.PLUGIN_ID, EMFComparePreferences.DISABLED_FACADE_PROVIDER);
		InteractiveUIBuilder<IFacadeProvider.Factory> uiBuilder = new InteractiveUIBuilder<IFacadeProvider.Factory>(
				container, registry);
		Set<IItemDescriptor<IFacadeProvider.Factory>> providers = Sets
				.newLinkedHashSet(registry.getItemDescriptors());
		uiBuilder.setConfigurationNodeKey(EMFComparePreferences.DISABLED_FACADE_PROVIDER)
				.setDefaultCheck(providers).setDefaultSelection(registry.getHighestRankingDescriptor())
				.setHoldingData(dataHolder).setDefaultCheck(activeProviders);
		interactiveUI = uiBuilder.build();

		return container;
	}

	@Override
	public boolean performOk() {
		Set<IItemDescriptor<IFacadeProvider.Factory>> providers = Sets.newLinkedHashSet(
				EMFCompareFacadePlugin.getDefault().getFacadeProviderItemRegistry().getItemDescriptors());
		SetView<IItemDescriptor<IFacadeProvider.Factory>> providersToDisable = Sets.difference(providers,
				dataHolder.getData());
		setFacadeProviderPreferences(EMFComparePreferences.DISABLED_FACADE_PROVIDER, providersToDisable);

		if (TracingConstant.CONFIGURATION_TRACING_ACTIVATED) {
			StringBuilder traceMessage = new StringBuilder("Facade providers preference serialization:\n"); //$NON-NLS-1$
			String prefDelimiter = " :\n"; //$NON-NLS-1$
			String newLine = "\n"; //$NON-NLS-1$
			traceMessage.append(EMFComparePreferences.DISABLED_FACADE_PROVIDER).append(prefDelimiter)
					.append(getPreferenceStore().getString(EMFComparePreferences.DISABLED_FACADE_PROVIDER))
					.append(newLine);
			EMFCompareRCPPlugin.getDefault().log(IStatus.INFO, traceMessage.toString());
		}

		return super.performOk();
	}

	@Override
	protected void performDefaults() {
		Set<IItemDescriptor<IFacadeProvider.Factory>> providers = Sets.newLinkedHashSet(
				EMFCompareFacadePlugin.getDefault().getFacadeProviderItemRegistry().getItemDescriptors());
		interactiveUI.checkElements(providers);
		dataHolder.setData(providers);
		super.performDefaults();
	}

	/**
	 * Store façade providers preferences into the preference store.
	 * 
	 * @param preferenceKey
	 *            key used in the preference store
	 * @param currentSelectedFacadeProviders
	 *            selected façade providers to store
	 */
	private void setFacadeProviderPreferences(String preferenceKey,
			Set<IItemDescriptor<IFacadeProvider.Factory>> currentSelectedFacadeProviders) {
		if ((currentSelectedFacadeProviders != null) && !currentSelectedFacadeProviders.isEmpty()) {
			Iterable<String> identifiers = Iterables.transform(currentSelectedFacadeProviders,
					IItemDescriptor::getID);
			String preferenceValue = Joiner.on(ItemUtil.PREFERENCE_DELIMITER).join(identifiers);
			getPreferenceStore().setValue(preferenceKey, preferenceValue);
		} else {
			getPreferenceStore().setToDefault(preferenceKey);
		}
	}

}
