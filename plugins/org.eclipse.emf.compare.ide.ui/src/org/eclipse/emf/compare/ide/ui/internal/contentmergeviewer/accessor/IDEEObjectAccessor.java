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
package org.eclipse.emf.compare.ide.ui.internal.contentmergeviewer.accessor;

import org.eclipse.compare.IStreamContentAccessor;
import org.eclipse.compare.ITypedElement;
import org.eclipse.emf.common.notify.AdapterFactory;
import org.eclipse.emf.compare.Match;
import org.eclipse.emf.compare.rcp.ui.mergeviewer.IMergeViewer.MergeViewerSide;
import org.eclipse.emf.compare.rcp.ui.mergeviewer.accessor.EObjectAccessor;

/**
 * @author <a href="mailto:mikael.barbero@obeo.fr">Mikael Barbero</a>
 */
public class IDEEObjectAccessor extends EObjectAccessor implements ITypedElement, IStreamContentAccessor {

	/**
	 * @param adapterFactory
	 * @param eObject
	 * @param match
	 * @param side
	 */
	public IDEEObjectAccessor(AdapterFactory adapterFactory, Match match, MergeViewerSide side) {
		super(adapterFactory, match, side);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.compare.ITypedElement#getType()
	 */
	public String getType() {
		return ContentMergeViewerConstants.EOBJECT_NODE_TYPE;
	}

}
