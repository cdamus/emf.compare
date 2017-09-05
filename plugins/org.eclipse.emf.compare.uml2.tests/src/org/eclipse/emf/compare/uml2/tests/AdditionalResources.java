/*******************************************************************************
 * Copyright (c) 2017 Christian W. Damus and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Christian W. Damus - initial API and implementation
 *******************************************************************************/
package org.eclipse.emf.compare.uml2.tests;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotate a test method or class to indicate what kinds of additional resources (if any) should be included
 * in the comparison scope besides the starting resources.
 *
 * @author Christian W. Damus
 */
@Retention(RUNTIME)
@Target({TYPE, METHOD })
@Inherited
public @interface AdditionalResources {
	/**
	 * The kind of additional resources to include. The default is useful to override on a particular test
	 * method the kind specified on the test class.
	 */
	AdditionalResourcesKind value() default AdditionalResourcesKind.NONE;
}
