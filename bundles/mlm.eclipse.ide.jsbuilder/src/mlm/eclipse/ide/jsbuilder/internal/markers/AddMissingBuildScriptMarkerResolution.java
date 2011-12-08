/*
 * Copyright (c) 2011 Marco Lehmann-Mörz. All rights reserved.
 */


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


	/**
	 *
	 * {@inheritDoc}
	 *
	 * @see IMarkerResolution#getLabel()
	 *
	 */

	public String getLabel() {

		return "Add missing build script 'builder.js'.";

	}


	/**
	 *
	 * {@inheritDoc}
	 *
	 * @see IMarkerResolution#run(IMarker)
	 *
	 */

	public void run( final IMarker pMarker ) {

		final IProject project = (IProject) pMarker.getResource();
		final IFile scriptFile = project.getFile(JavaScriptBuilder.BUILDER_SCRIPT_NAME);
		if (scriptFile.exists()) {

			// script already exists
			return;

		}

		// TODO keep in sync with ToggleNatureHandler --> use wizard with templates for different use cases
		final URL url = FileLocator.find(Activator.getDefault().getBundle(), new Path("resources/template.js"), null); //$NON-NLS-1$
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

		final String reason = pMarker.getAttribute(JavaScriptBuilder.ID_PROBLEM_MARKER, null);
		if (!"missingBuilderJS".equals(reason)) {

			return null;

		}

		return new AddMissingBuildScriptMarkerResolution();

	}


}
