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
package org.eclipse.emf.compare.diagram.ide.ui.internal.contentmergeviewer.diagram;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.DifferenceKind;
import org.eclipse.emf.compare.diagram.DiagramDiff;
import org.eclipse.emf.compare.diagram.EdgeChange;
import org.eclipse.emf.compare.diagram.Hide;
import org.eclipse.emf.compare.diagram.LabelChange;
import org.eclipse.emf.compare.diagram.NodeChange;
import org.eclipse.emf.compare.diagram.Show;
import org.eclipse.emf.compare.diagram.ide.ui.GraphicalMergeViewer;
import org.eclipse.emf.compare.diagram.ide.ui.internal.accessor.IDiagramNodeAccessor;
import org.eclipse.emf.compare.diagram.ui.decoration.provider.DiffDecoratorProvider;
import org.eclipse.emf.compare.diagram.ui.decoration.provider.SelectedDiffAdapter;
import org.eclipse.emf.compare.diagram.util.DiagramCompareSwitch;
import org.eclipse.emf.compare.rcp.ui.mergeviewer.IMergeViewer.MergeViewerSide;
import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.MouseWheelHandler;
import org.eclipse.gef.MouseWheelZoomHandler;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gmf.runtime.diagram.ui.editparts.DiagramEditPart;
import org.eclipse.gmf.runtime.diagram.ui.editparts.DiagramRootEditPart;
import org.eclipse.gmf.runtime.diagram.ui.editparts.IGraphicalEditPart;
import org.eclipse.gmf.runtime.diagram.ui.editpolicies.EditPolicyRoles;
import org.eclipse.gmf.runtime.diagram.ui.parts.DiagramGraphicalViewer;
import org.eclipse.gmf.runtime.diagram.ui.services.editpart.EditPartService;
import org.eclipse.gmf.runtime.notation.Diagram;
import org.eclipse.gmf.runtime.notation.View;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

class DiagramMergeViewer extends GraphicalMergeViewer {

	private IDiagramNodeAccessor fInput;

	private DiagramGraphicalViewer fGraphicalViewer;

	/** the zoom factor of displayed diagrams. */
	private static final double ZOOM_FACTOR = 0.7;

	/** the current diagram used. */
	private Diagram currentDiag;

	/** the current diff selected. */
	private List<Diagram> openedDiags = new ArrayList<Diagram>();

	/**
	 * @param parent
	 */
	public DiagramMergeViewer(Composite parent, MergeViewerSide side) {
		super(parent, side);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.rcp.ui.mergeviewer.MergeViewer#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public Control createControl(Composite parent) {
		createDiagramGraphicalViewer(parent);
		return fGraphicalViewer.getControl();
	}

	private void createDiagramGraphicalViewer(Composite composite) {
		fGraphicalViewer = new DiagramGraphicalViewer();
		fGraphicalViewer.createControl(composite);
		fGraphicalViewer.setEditDomain(editDomain);
		fGraphicalViewer.setEditPartFactory(EditPartService.getInstance());
		fGraphicalViewer.getControl().setBackground(ColorConstants.listBackground);
		fGraphicalViewer.setProperty(MouseWheelHandler.KeyGenerator.getKey(SWT.CTRL),
				MouseWheelZoomHandler.SINGLETON);
	}

	@Override
	protected DiagramGraphicalViewer getGraphicalViewer() {
		return fGraphicalViewer;
	}

	@Override
	public void setInput(final Object input) {
		if (input instanceof IDiagramNodeAccessor) {
			fInput = (IDiagramNodeAccessor)input;
			View eObject = ((IDiagramNodeAccessor)input).getOwnedView();

			// FIXME
			ResourceSet resourceSet = null;
			if (eObject != null) {
				resourceSet = eObject.eResource().getResourceSet();
			}
			if (resourceSet != null
					&& TransactionalEditingDomain.Factory.INSTANCE.getEditingDomain(resourceSet) == null) {
				TransactionalEditingDomain.Factory.INSTANCE.createEditingDomain(resourceSet);
			}

			Diagram diagram = ((IDiagramNodeAccessor)input).getOwnedDiagram();
			if (diagram != null && !openedDiags.contains(diagram)) {
				openedDiags.add(diagram);
				Iterator<EObject> contents = diagram.eAllContents();
				while (contents.hasNext()) {
					EObject obj = contents.next();
					for (Diff diff : ((IDiagramNodeAccessor)input).getComparison().getDifferences(obj)) {
						if (diff instanceof DiagramDiff) {
							obj.eAdapters().add(new SelectedDiffAdapter((DiagramDiff)diff));
						}
					}
				}
			}

			if (eObject != null) {
				EditPart part = findEditPart(eObject);

				// Selection
				fGraphicalViewer.deselectAll();
				if (part != null) {
					while (!part.isSelectable()) {
						part = part.getParent();
					}
					select(part);
				}

			}
		}
	}

	public Object getInput() {
		return fInput;
	}

	public EditPart findObjectAtExcluding(Point location, Collection exclusionSet, Conditional conditional) {
		// TODO Auto-generated method stub
		return null;
	}

	public EditPart findEditPart(final EObject eobj) {
		// check viewer
		if (eobj instanceof View) {
			final Diagram d = ((View)eobj).getDiagram();
			checkAndDisplayDiagram(d);
		}
		return (EditPart)fGraphicalViewer.getEditPartRegistry().get(eobj);
	}

	private void checkAndDisplayDiagram(final Diagram d) {
		if (d != null && !d.equals(currentDiag)) {
			currentDiag = d;
			displayDiagram(d);
		}
	}

	protected final void displayDiagram(final Diagram diag) {
		if (diag == null) {
			return;
		}
		currentDiag = diag;
		// be sure the viewer will be correctly refreshed ( connections )
		fGraphicalViewer.getEditPartRegistry().clear();
		final DiagramRootEditPart rootEditPart = new DiagramRootEditPart(diag.getMeasurementUnit());
		fGraphicalViewer.setRootEditPart(rootEditPart);
		fGraphicalViewer.setContents(diag);
		disableEditMode((DiagramEditPart)fGraphicalViewer.getContents());
		rootEditPart.getZoomManager().setZoomAnimationStyle(ZoomManager.ANIMATE_NEVER);
		rootEditPart.getZoomManager().setZoom(ZOOM_FACTOR);
	}

	private void disableEditMode(DiagramEditPart diagEditPart) {
		diagEditPart.disableEditMode();
		for (Object obj : diagEditPart.getPrimaryEditParts()) {
			if (obj instanceof IGraphicalEditPart) {
				disableEditMode((IGraphicalEditPart)obj);
			}
		}
	}

	private void disableEditMode(IGraphicalEditPart obj) {
		obj.disableEditMode();
		obj.removeEditPolicy(EditPolicyRoles.OPEN_ROLE);
		for (Object child : obj.getChildren()) {
			if (child instanceof IGraphicalEditPart) {
				disableEditMode((IGraphicalEditPart)child);
			}
		}
	}

	/**
	 * Inner class for visit dagramDiffs.
	 * 
	 * @author <a href="mailto:stephane.bouchet@obeo.fr">Stephane Bouchet</a>
	 */
	private class DiagramdiffSwitchVisitor extends DiagramCompareSwitch<IStatus> {

		private View view;

		private boolean annotate;

		public DiagramdiffSwitchVisitor(View view, boolean annotate) {
			this.view = view;
			this.annotate = annotate;
		}

		/**
		 * {@inheritDoc}
		 * 
		 * @see org.eclipse.emf.compare.diagram.diagramdiff.util.DiagramdiffSwitch#caseDiagramLabelChange(org.eclipse.emf.compare.diagram.diagramdiff.DiagramLabelChange)
		 */
		@Override
		public IStatus caseLabelChange(LabelChange diff) {
			return checkResult(annotateNotation(view, DiffDecoratorProvider.DIFF_LABEL_MODIFIED));
		}

		/**
		 * {@inheritDoc}
		 * 
		 * @see org.eclipse.emf.compare.diagram.diagramdiff.util.DiagramdiffSwitch#caseDiagramShowElement(org.eclipse.emf.compare.diagram.diagramdiff.DiagramShowElement)
		 */
		@Override
		public IStatus caseShow(Show diff) {
			return checkResult(annotateNotation(view, DiffDecoratorProvider.DIFF_SHOWED));
		}

		/**
		 * {@inheritDoc}
		 * 
		 * @see org.eclipse.emf.compare.diagram.diagramdiff.util.DiagramdiffSwitch#caseDiagramHideElement(org.eclipse.emf.compare.diagram.diagramdiff.DiagramHideElement)
		 */
		@Override
		public IStatus caseHide(Hide diff) {
			return checkResult(annotateNotation(view, DiffDecoratorProvider.DIFF_SHOWED));
		}

		/**
		 * {@inheritDoc}
		 * 
		 * @see org.eclipse.emf.compare.diagram.diagramdiff.util.DiagramdiffSwitch#caseDiagramMoveNode(org.eclipse.emf.compare.diagram.diagramdiff.DiagramMoveNode)
		 */
		@Override
		public IStatus caseNodeChange(NodeChange diff) {
			boolean result = true;
			if (diff.getKind() == DifferenceKind.MOVE) {
				return checkResult(annotateNotation(view, DiffDecoratorProvider.DIFF_MOVED));
			} else if (diff.getKind() == DifferenceKind.ADD) {
				return checkResult(annotateNotation(view, DiffDecoratorProvider.DIFF_ADDED));
			} else if (diff.getKind() == DifferenceKind.DELETE) {
				return checkResult(annotateNotation(view, DiffDecoratorProvider.DIFF_REMOVED));
			}
			return checkResult(result);
		}

		/**
		 * {@inheritDoc}
		 * 
		 * @see org.eclipse.emf.compare.diagram.diagramdiff.util.DiagramdiffSwitch#caseDiagramEdgeChange(org.eclipse.emf.compare.diagram.diagramdiff.DiagramEdgeChange)
		 */
		@Override
		public IStatus caseEdgeChange(EdgeChange diff) {
			return checkResult(annotateNotation(view, DiffDecoratorProvider.DIFF_MOVED));
		}

		/**
		 * {@inheritDoc}
		 * 
		 * @see org.eclipse.emf.compare.diagram.diagramdiff.util.DiagramdiffSwitch#defaultCase(org.eclipse.emf.ecore.EObject)
		 */
		@Override
		public IStatus defaultCase(EObject diff) {
			// we don't care about generic diffs
			return Status.OK_STATUS;
		}

		/**
		 * Set the given annotation to the given notation element.
		 * 
		 * @param element
		 *            the notation element to annotate
		 * @param annotation
		 *            the diff annotation
		 * @return true if annotation has been added to the view
		 */
		protected boolean annotateNotation(View element, String annotation) {
			boolean result = false;
			if (annotate) {
				EAnnotation diffAnnotation = null;
				if (element.getEAnnotation(DiffDecoratorProvider.DIFF) == null) {
					diffAnnotation = EcoreFactory.eINSTANCE.createEAnnotation();
					diffAnnotation.setSource(DiffDecoratorProvider.DIFF);
					result = element.getEAnnotations().add(diffAnnotation);
				} else {
					diffAnnotation = element.getEAnnotation(DiffDecoratorProvider.DIFF);
				}
				// FIXME should this string be externalized?
				diffAnnotation.getDetails().put(annotation, "diffDetail"); //$NON-NLS-1$
				result = true;
			} else {
				if (element.getEAnnotation(DiffDecoratorProvider.DIFF) != null) {
					final EAnnotation diffAnnotation = element.getEAnnotation(DiffDecoratorProvider.DIFF);
					result = element.getEAnnotations().remove(diffAnnotation);
				}
			}
			return result;
		}

		/**
		 * Utility method to transform a boolean result into status.
		 * 
		 * @param ok
		 *            the boolean state
		 * @return a status corresponding to the state of the boolean
		 */
		protected IStatus checkResult(boolean ok) {
			if (ok) {
				return Status.OK_STATUS;
			}
			return Status.CANCEL_STATUS;
		}

	}

	private boolean hasSelectedDiffAdapter(View view) {
		Iterator<Adapter> adapters = view.eAdapters().iterator();
		while (adapters.hasNext()) {
			Adapter adapter = adapters.next();
			if (adapter.isAdapterForType(DiagramDiff.class)) {
				return true;
			}
		}
		return false;
	}

}
