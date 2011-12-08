/*
 * Copyright (c) 2011 Marco Lehmann-Mörz. All rights reserved.
 */


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

public class ProblemMarkerResolutionGenerator implements IMarkerResolutionGenerator {


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
