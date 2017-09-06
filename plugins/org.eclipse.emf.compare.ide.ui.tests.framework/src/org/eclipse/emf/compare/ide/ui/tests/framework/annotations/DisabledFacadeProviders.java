/*******************************************************************************
 * Copyright (c) 2016, 2017 Obeo, Christian W. Damus, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Obeo - initial API and implementation
 *     Christian W. Damus - integration of façade providers
 *******************************************************************************/
package org.eclipse.emf.compare.ide.ui.tests.framework.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to specify the façade providers disabled for the test class. Each test will be run with the
 * given list of façade provider factories disabled. The other façade providers will be active for the
 * duration.
 * 
 * @author <a href="mailto:mathieu.cartaud@obeo.fr">Mathieu Cartaud</a>
 * @author Christian W. Damus
 * @since 1.1
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DisabledFacadeProviders {

	/**
	 * The list of façade provider factories disabled for the test. If the annotation is used empty, the
	 * default array will be returned. If the annotation is not used the façade providers defined in a default
	 * array in the class EMFCompareGitTestRunner will be used.
	 * 
	 * @return the disabled façade provider factory classes or the default array if user specifies nothing
	 */
	Class<?>[] value() default {};

}
