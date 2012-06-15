/*******************************************************************************
 * Copyright (c) 2011, 2012 Marco Lehmann-Mörz.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Marco Lehmann-Mörz - initial API and implementation and/or initial documentation
 *******************************************************************************/


package mlm.eclipse.ide.jsbuilder.internal.markers;


import mlm.eclipse.ide.jsbuilder.internal.JavaScriptBuilder;
import mlm.eclipse.ide.jsbuilder.internal.JavaScriptNature;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IMarkerResolution;


/**
 *
 * TODO
 *
 * @author Marco Lehmann-Mörz
 *
 * @since mlm.eclipse.ide.jsbuilder 1.0
 *
 */

public class RemoveNatureMarkerResolution implements IMarkerResolution {


	/**
	 *
	 * Constructs a new <code>RemoveNatureMarkerResolution</code>.
	 *
	 * @since mlm.eclipse.ide.jsbuilder 1.0
	 *
	 */

	public RemoveNatureMarkerResolution() {

		super();

	}


	/**
	 *
	 * {@inheritDoc}
	 *
	 * @see IMarkerResolution#getLabel()
	 *
	 */

	public String getLabel() {

		return "Remove Nature";

	}


	/**
	 *
	 * {@inheritDoc}
	 *
	 * @see IMarkerResolution#run(IMarker)
	 *
	 */

	public void run( final IMarker pMarker ) {

		try {

			final IProject project = (IProject) pMarker.getResource();
			JavaScriptNature.removeNature(project);

		} catch (final CoreException ex) {

			// TODO log?
			ex.printStackTrace();

		}

	}


	/**
	 *
	 * Convenience method to create (or not) a marker resolution for the given marker.
	 *
	 * @param pMarker marker
	 *
	 * @return the marker resolution or <code>null</code>
	 *
	 * @since mlm.eclipse.ide.jsbuilder 1.0
	 *
	 */

	public static final IMarkerResolution create( final IMarker pMarker ) {

		final IResource resource = pMarker.getResource();
		if (resource.getType() != IResource.PROJECT) {

			return null;

		}

		final String reason = pMarker.getAttribute(JavaScriptBuilder.ID_PROBLEM_MARKER, null);
		if (!"missingBuilderJS".equals(reason)) {

			return null;

		}

		return new RemoveNatureMarkerResolution();

	}


}
