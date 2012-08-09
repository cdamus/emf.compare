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
package org.eclipse.emf.compare.ide.ui.internal.contentmergeviewer.table;

import java.util.ResourceBundle;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.emf.common.notify.AdapterFactory;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.compare.ConflictKind;
import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.DifferenceSource;
import org.eclipse.emf.compare.DifferenceState;
import org.eclipse.emf.compare.ide.ui.internal.contentmergeviewer.EMFCompareContentMergeViewer;
import org.eclipse.emf.compare.ide.ui.internal.contentmergeviewer.IMergeViewer;
import org.eclipse.emf.compare.ide.ui.internal.contentmergeviewer.IMergeViewer.MergeViewerSide;
import org.eclipse.emf.compare.ide.ui.internal.contentmergeviewer.provider.IStructuralFeatureAccessor;
import org.eclipse.emf.compare.ide.ui.internal.contentmergeviewer.util.DiffInsertionPoint;
import org.eclipse.emf.edit.provider.ComposedAdapterFactory;
import org.eclipse.emf.edit.ui.provider.AdapterFactoryLabelProvider;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Scrollable;
import org.eclipse.swt.widgets.Table;

/**
 * @author <a href="mailto:mikael.barbero@obeo.fr">Mikael Barbero</a>
 */
public class TableContentMergeViewer extends EMFCompareContentMergeViewer {

	/**
	 * Bundle name of the property file containing all displayed strings.
	 */
	private static final String BUNDLE_NAME = TableContentMergeViewer.class.getName();

	private final AdapterFactory fAdapterFactory;

	/**
	 * Call the super constructor.
	 * 
	 * @see TableContentMergeViewer
	 */
	protected TableContentMergeViewer(Composite parent, CompareConfiguration config) {
		super(SWT.NONE, ResourceBundle.getBundle(BUNDLE_NAME), config);
		fAdapterFactory = new ComposedAdapterFactory(ComposedAdapterFactory.Descriptor.Registry.INSTANCE);
		buildControl(parent);
		setContentProvider(new TableContentMergeViewerContentProvider(config));
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.compare.contentmergeviewer.ContentMergeViewer#copy(boolean)
	 */
	@Override
	protected void copy(boolean leftToRight) {
		EList<Diff> differences = getComparison().getDifferences();
		boolean merged = false;
		for (Diff diff : differences) {
			if (leftToRight && diff.getSource() == DifferenceSource.LEFT
					&& diff.getState() == DifferenceState.UNRESOLVED) {
				if (diff.getConflict() == null || diff.getConflict().getKind() == ConflictKind.PSEUDO) {
					diff.copyLeftToRight();
					merged = true;
				}
			} else if (!leftToRight && diff.getSource() == DifferenceSource.RIGHT
					&& diff.getState() == DifferenceState.UNRESOLVED) {
				if (diff.getConflict() == null || diff.getConflict().getKind() == ConflictKind.PSEUDO) {
					diff.copyRightToLeft();
					merged = true;
				}
			}
		}
		if (merged) {
			if (leftToRight) {
				setRightDirty(true);
			} else {
				setLeftDirty(true);
			}
			refresh();
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.compare.contentmergeviewer.ContentMergeViewer#getContents(boolean)
	 */
	@Override
	protected byte[] getContents(boolean left) {
		return null;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.ide.ui.internal.contentmergeviewer.EMFCompareContentMergeViewer#copyDiffRightToLeft()
	 */
	@Override
	protected void copyDiffRightToLeft() {
		Diff diffToCopy = getDiffToCopy(getRightMergeViewer(), MergeViewerSide.RIGHT);
		if (diffToCopy != null) {
			diffToCopy.copyRightToLeft();
			setLeftDirty(true);
			refresh();
		}
	}

	private Diff getDiffToCopy(IMergeViewer<? extends Scrollable> mergeViewer, MergeViewerSide side) {
		Diff diffToCopy = null;
		ISelection selection = mergeViewer.getSelection();
		if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
			Object firstElement = ((IStructuredSelection)selection).getFirstElement();
			if (firstElement instanceof DiffInsertionPoint) {
				diffToCopy = ((DiffInsertionPoint)firstElement).getDiff();
			} else {
				Object mergeViewerInput = mergeViewer.getInput();
				if (mergeViewerInput instanceof IStructuralFeatureAccessor) {
					diffToCopy = ((IStructuralFeatureAccessor)mergeViewerInput).getDiff(firstElement, side);
				}
			}

		}
		return diffToCopy;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.ide.ui.internal.contentmergeviewer.EMFCompareContentMergeViewer#copyDiffLeftToRight()
	 */
	@Override
	protected void copyDiffLeftToRight() {
		Diff diffToCopy = getDiffToCopy(getLeftMergeViewer(), MergeViewerSide.LEFT);
		if (diffToCopy != null) {
			diffToCopy.copyLeftToRight();
			setRightDirty(true);
			refresh();
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.ide.ui.internal.contentmergeviewer.EMFCompareContentMergeViewer#createMergeViewer(org.eclipse.swt.widgets.Composite,
	 *      org.eclipse.emf.compare.ide.ui.internal.contentmergeviewer.IMergeViewer.MergeViewerSide)
	 */
	@Override
	protected IMergeViewer<? extends Composite> createMergeViewer(Composite parent, MergeViewerSide side) {
		IMergeViewer<Table> ret = new TableMergeViewer(parent, this, side);
		ret.setContentProvider(new ArrayContentProvider());
		ret.setLabelProvider(new AdapterFactoryLabelProvider(fAdapterFactory));
		return ret;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.ide.ui.internal.contentmergeviewer.EMFCompareContentMergeViewer#paintCenter(org.eclipse.swt.widgets.Canvas,
	 *      org.eclipse.swt.graphics.GC)
	 */
	@Override
	protected void paintCenter(Canvas canvas, GC g) {

	}

}