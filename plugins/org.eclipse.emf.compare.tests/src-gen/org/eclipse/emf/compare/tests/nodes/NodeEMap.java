/**
 * Copyright (c) 2017 Christian W. Damus and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Christian W. Damus - initial API and implementation
 */
package org.eclipse.emf.compare.tests.nodes;

import org.eclipse.emf.common.util.EMap;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Node EMap</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.emf.compare.tests.nodes.NodeEMap#getNameTable <em>Name Table</em>}</li>
 * </ul>
 *
 * @see org.eclipse.emf.compare.tests.nodes.NodesPackage#getNodeEMap()
 * @model
 * @generated
 */
public interface NodeEMap extends Node {
	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	String copyright = "Copyright (c) 2011, 2017 Obeo and others.\r\nAll rights reserved. This program and the accompanying materials\r\nare made available under the terms of the Eclipse Public License v1.0\r\nwhich accompanies this distribution, and is available at\r\nhttp://www.eclipse.org/legal/epl-v10.html\r\n\r\nContributors:\r\n    Obeo - initial API and implementation"; //$NON-NLS-1$

	/**
	 * Returns the value of the '<em><b>Name Table</b></em>' map.
	 * The key is of type {@link java.lang.String},
	 * and the value is of type {@link org.eclipse.emf.compare.tests.nodes.Node},
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Name Table</em>' map isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Name Table</em>' map.
	 * @see org.eclipse.emf.compare.tests.nodes.NodesPackage#getNodeEMap_NameTable()
	 * @model mapType="org.eclipse.emf.compare.tests.nodes.StringToNodeMapEntry&lt;org.eclipse.emf.ecore.EString, org.eclipse.emf.compare.tests.nodes.Node&gt;"
	 * @generated
	 */
	EMap<String, Node> getNameTable();

} // NodeEMap
