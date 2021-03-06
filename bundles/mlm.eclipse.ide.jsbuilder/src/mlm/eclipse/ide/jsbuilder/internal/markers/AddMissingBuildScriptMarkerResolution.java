/****************************************************************************************
 * Copyright (c) 2011, 2015 Marco Lehmann-Mörz.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Marco Lehmann-Mörz - initial API and implementation and/or initial documentation
 ***************************************************************************************/


package mlm.eclipse.ide.jsbuilder.internal.markers;


import java.io.InputStream;
import java.net.URL;

import mlm.eclipse.ide.jsbuilder.internal.Activator;
import mlm.eclipse.ide.jsbuilder.internal.JavaScriptBuilder;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.IMarkerResolution;


/**
 *
 * Marker resolution for a missing build script.
 *
 * @author Marco Lehmann-Mörz
 *
 * @since mlm.eclipse.ide.jsbuilder 1.0
 *
 */

public final class AddMissingBuildScriptMarkerResolution implements IMarkerResolution {


	/**
	 *
	 * Constructs a new <code>AddMissingBuildScriptMarkerResolution</code>.
	 *
	 * @since mlm.eclipse.ide.jsbuilder 1.0
	 *
	 */

	public AddMissingBuildScriptMarkerResolution() {

		super();

	}


	@Override
    public String getLabel() {

		return "Add missing build script 'builder.js'.";

	}


	@Override
    public void run( final IMarker pMarker ) {

		final IProject project = (IProject) pMarker.getResource();
		final IFile scriptFile = project.getFile(JavaScriptBuilder.BUILDER_SCRIPT_NAME);
		if (scriptFile.exists()) {

			// script already exists
			return;

		}

		// TODO keep in sync with ToggleNatureHandler --> use wizard with templates for different use cases
		final URL url = FileLocator.find(Activator.getDefault().getBundle(), new Path("resources/simple.js"), null); //$NON-NLS-1$
		if (url != null) {

			try {

				InputStream is = null;

				try {

					is = url.openStream();
					scriptFile.create(is, true, null);

				} finally {

					is.close();

				}

			} catch (final Exception ex) {

				// TODO log?
				ex.printStackTrace();

			}

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

		final String reason = pMarker.getAttribute(Activator.ID_PROBLEM_MARKER, null);
		if (!"missingBuilderJS".equals(reason)) {

			return null;

		}

		return new AddMissingBuildScriptMarkerResolution();

	}


}
