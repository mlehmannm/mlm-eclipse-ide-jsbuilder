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


import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator;


/**
 *
 * Resolution generator for markers of type <code>mlm.eclipse.ide.jsbuilder.problemmarker</code>.
 *
 * @author Marco Lehmann-Mörz
 *
 * @since mlm.eclipse.ide.jsbuilder 1.0
 *
 */

public final class ProblemMarkerResolutionGenerator implements IMarkerResolutionGenerator {


	/**
	 *
	 * Constructs a new <code>ProblemMarkerResolutionGenerator</code>.
	 *
	 * @since mlm.eclipse.ide.jsbuilder 1.0
	 *
	 */

	public ProblemMarkerResolutionGenerator() {

		super();

	}


	/**
	 *
	 * {@inheritDoc}
	 *
	 * @see IMarkerResolutionGenerator#getResolutions(IMarker)
	 *
	 */

	public IMarkerResolution[] getResolutions( final IMarker pMarker ) {

		final List<IMarkerResolution> resolutions = new ArrayList<IMarkerResolution>(1);

		// add missing build script 'builder.js' when it is missing
		final IMarkerResolution addMissingBuildScript = AddMissingBuildScriptMarkerResolution.create(pMarker);
		if (addMissingBuildScript != null) {

			resolutions.add(addMissingBuildScript);

		}

		// remove nature when build script 'builder.js' mising
		final IMarkerResolution removeNature = RemoveNatureMarkerResolution.create(pMarker);
		if (removeNature != null) {

			resolutions.add(removeNature);

		}

		return resolutions.toArray(new IMarkerResolution[resolutions.size()]);

	}


}
