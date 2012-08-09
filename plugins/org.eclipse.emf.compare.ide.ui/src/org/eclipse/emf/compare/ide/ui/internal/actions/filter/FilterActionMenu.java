/*******************************************************************************
 * Copyright (c) 2012 Obeo.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Obeo - initial API and implementation
 *******************************************************************************/
package org.eclipse.emf.compare.ide.ui.internal.actions.filter;

import org.eclipse.emf.compare.DifferenceKind;
import org.eclipse.emf.compare.ide.ui.internal.EMFCompareIDEUIPlugin;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * This will be displayed atop the structure viewer as the "filters" menu.
 * 
 * @author <a href="mailto:laurent.goubet@obeo.fr">Laurent Goubet</a>
 */
public class FilterActionMenu extends Action implements IMenuCreator {
	/** The Filter that will be modified by this menu's actions. */
	private final DifferenceFilter differenceFilter;

	/** Menu Manager that will contain our menu. */
	private MenuManager menuManager;

	/**
	 * Constructs our filtering menu.
	 * 
	 * @param differenceFilter
	 *            The filter for which we'll create actions.
	 */
	public FilterActionMenu(DifferenceFilter differenceFilter) {
		super("", IAction.AS_DROP_DOWN_MENU);
		this.menuManager = new MenuManager();
		this.differenceFilter = differenceFilter;
		setMenuCreator(this);
		setToolTipText("Filters");
		setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(EMFCompareIDEUIPlugin.PLUGIN_ID,
				"icons/full/toolb16/filter.gif")); //$NON-NLS-1$
		createActions(menuManager);
	}

	/**
	 * Create all of our filtering actions into the given menu.
	 * 
	 * @param menu
	 *            The menu in which we are to create filter actions.
	 */
	public void createActions(MenuManager menu) {
		menu.add(new FilterAction("Changed Elements", DifferenceKind.CHANGE, differenceFilter));
		menu.add(new FilterAction("Added Elements", DifferenceKind.ADD, differenceFilter));
		menu.add(new FilterAction("Removed Elements", DifferenceKind.DELETE, differenceFilter));
		menu.add(new FilterAction("Moved Elements", DifferenceKind.MOVE, differenceFilter));
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see IMenuCreator#dispose()
	 */
	public void dispose() {
		menuManager.dispose();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see IMenuCreator#getMenu(Control)
	 */
	public Menu getMenu(Control parent) {
		return menuManager.createContextMenu(parent);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see IMenuCreator#getMenu(Menu)
	 */
	public Menu getMenu(Menu parent) {
		return menuManager.getMenu();
	}
}