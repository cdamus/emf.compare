/*
 * Copyright (c) 2017 Christian W. Damus and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Christian W. Damus - Initial API and implementation
 */
package org.eclipse.emf.compare.rcp.internal.merger;

import com.google.common.collect.Maps;

import java.util.Map;

import org.eclipse.core.expressions.EvaluationContext;
import org.eclipse.core.expressions.EvaluationResult;
import org.eclipse.core.expressions.Expression;
import org.eclipse.core.expressions.ExpressionConverter;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.emf.compare.merge.ICopier;
import org.eclipse.emf.compare.rcp.extension.AbstractRegistryEventListener;
import org.eclipse.emf.compare.rcp.internal.EMFCompareRCPMessages;
import org.eclipse.emf.ecore.EObject;

/**
 * Listener for changes on the <em>Copiers</em> extension point.
 * 
 * @author Christian W. Damus
 * @since 2.6
 */
public class CopierExtensionRegistryListener extends AbstractRegistryEventListener {

	/** The copier XML element name. */
	private static final String COPIER = "copier"; //$NON-NLS-1$

	/** The class XML attribute name. */
	private static final String CLASS = "class"; //$NON-NLS-1$

	/** The ranking XML attribute name. */
	private static final String RANKING = "ranking"; //$NON-NLS-1$

	/** The enablement XML element name. */
	private static final String ENABLEMENT = "enablement"; //$NON-NLS-1$

	/** The package namespace URI expression variable name. */
	private static final String NS_URI = "nsURI"; //$NON-NLS-1$

	/** Local reference to the log used by the superclass. */
	private final ILog log;

	/** Descriptors registered on the extension point. */
	private final Map<String, ICopier.Descriptor> descriptors = Maps.newConcurrentMap();

	/**
	 * Initializes me.
	 * 
	 * @param pluginID
	 *            the plug-in ID that owns the extension point
	 * @param extensionPointID
	 *            the relative extension point ID
	 * @param log
	 *            a log in which to report problems with the extensions
	 */
	public CopierExtensionRegistryListener(String pluginID, String extensionPointID, ILog log) {
		super(pluginID, extensionPointID, log);

		this.log = log;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.rcp.extension.AbstractRegistryEventListener#validateExtensionElement(org.eclipse.core.runtime.IConfigurationElement)
	 */
	@Override
	protected boolean validateExtensionElement(IConfigurationElement element) {
		boolean result;

		if (COPIER.equals(element.getName())) {
			if (element.getAttribute(CLASS) == null) {
				logMissingAttribute(element, CLASS);
				result = false;
			} else if (element.getAttribute(RANKING) == null) {
				logMissingAttribute(element, RANKING);
				result = false;
			} else {
				String rankingStr = element.getAttribute(RANKING);

				try {
					Integer.parseInt(rankingStr);
					result = true;
				} catch (NumberFormatException nfe) {
					log(IStatus.ERROR, element,
							EMFCompareRCPMessages.getString("malformed.extension.attribute", //$NON-NLS-1$
									RANKING));
					result = false;
				}
			}
		} else {
			result = false;
		}

		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected boolean addedValid(IConfigurationElement element) {
		if (element.getName().equals(COPIER)) {
			String className = element.getAttribute(CLASS);
			int ranking = Integer.parseInt(element.getAttribute(RANKING));

			ICopier.Descriptor newDescriptor = new DescriptorImpl(element, ranking);

			ICopier.Descriptor oldDescriptor = descriptors.put(className, newDescriptor);
			if (oldDescriptor != null) {
				ICopier.Registry.INSTANCE.remove(oldDescriptor);
				log(IStatus.WARNING, element,
						EMFCompareRCPMessages.getString("duplicate.extension", className)); //$NON-NLS-1$
			}

			ICopier.Registry.INSTANCE.add(newDescriptor);
		}

		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected boolean removedValid(IConfigurationElement element) {
		if (element.getName().equals(COPIER)) {
			// We can trust the validation step
			String className = element.getAttribute(CLASS);
			ICopier.Descriptor oldDescriptor = descriptors.remove(className);
			if (oldDescriptor != null) {
				ICopier.Registry.INSTANCE.remove(oldDescriptor);
			}
		}

		return true;
	}

	//
	// Nested types
	//

	/**
	 * A descriptor for XMI ID copiers registered on the extension point.
	 *
	 * @author Christian W. Damus
	 */
	private class DescriptorImpl implements ICopier.Descriptor {
		/** The extension element. */
		private final IConfigurationElement config;

		/** The ranking indicated on the extension point. */
		private final int ranking;

		/** The optional enablement expression (implied as TRUE if missing). */
		private Expression enablement;

		/** The XMI ID copier, once it has been created. */
		private ICopier copierInstance;

		/**
		 * Initializes me.
		 * 
		 * @param config
		 *            my extension configuration element
		 * @param ranking
		 *            my ranking from the {@code config} element
		 */
		DescriptorImpl(IConfigurationElement config, int ranking) {
			super();

			this.config = config;
			this.ranking = ranking;
		}

		/**
		 * Obtains my lazily-initialized XML enablement expression.
		 * 
		 * @return my XML enablement expression, or {@link Expression#TRUE} if none.
		 */
		protected Expression getEnablement() {
			if (enablement == null) {
				IConfigurationElement[] xml = config.getChildren(ENABLEMENT);
				if ((xml != null) && (xml.length > 0)) {
					try {
						enablement = ExpressionConverter.getDefault().perform(xml[0]);
					} catch (CoreException e) {
						log.log(e.getStatus());
						enablement = Expression.FALSE;
					}
				} else {
					enablement = Expression.TRUE;
				}
			}

			return enablement;
		}

		/**
		 * {@inheritDoc}
		 */
		public boolean isApplicableTo(EObject object) {
			boolean result;

			Expression expr = getEnablement();

			if (expr == Expression.FALSE) {
				result = false;
			} else if (expr == Expression.TRUE) {
				result = true;
			} else {
				EvaluationContext ctx = new EvaluationContext(null, object);
				ctx.addVariable(NS_URI, object.eClass().getEPackage().getNsURI());

				try {
					result = EvaluationResult.TRUE.equals(expr.evaluate(ctx));
				} catch (CoreException e) {
					log.log(e.getStatus());
					result = false;
				}
			}

			return result;
		}

		/**
		 * {@inheritDoc}
		 */
		public int getRank() {
			return ranking;
		}

		/**
		 * {@inheritDoc}
		 */
		public ICopier getCopier() {
			if (copierInstance == null) {
				try {
					copierInstance = (ICopier)config.createExecutableExtension(CLASS);
				} catch (CoreException e) {
					log.log(e.getStatus());
					copierInstance = ICopier.DEFAULT;
				} catch (ClassCastException e) {
					log(config, "Compare/merge copier extension does not implement ICopier interface", e); //$NON-NLS-1$
					copierInstance = ICopier.DEFAULT;
				}
			}

			return copierInstance;
		}

	}
}
