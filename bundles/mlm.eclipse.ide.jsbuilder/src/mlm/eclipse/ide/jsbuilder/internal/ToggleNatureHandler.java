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


package mlm.eclipse.ide.jsbuilder.internal;


import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.osgi.framework.Bundle;


/**
 *
 * Handler to toggle the nature on a selected project.
 *
 * @author Marco Lehmann-Mörz
 *
 * @since mlm.eclipse.ide.jsbuilder 1.0
 *
 */

public final class ToggleNatureHandler extends AbstractHandler {


	/**
	 *
	 * Constructs a new <code>ToggleNatureHandler</code>.
	 *
	 * @since mlm.eclipse.ide.jsbuilder 1.0
	 *
	 */

	public ToggleNatureHandler() {

		super();

	}


	/**
	 *
	 * {@inheritDoc}
	 *
	 * @see IHandler#execute(ExecutionEvent)
	 *
	 */

	public Object execute( final ExecutionEvent pEvent ) throws ExecutionException {

		final ISelection selection = HandlerUtil.getActiveMenuSelectionChecked(pEvent);
		if (selection instanceof IStructuredSelection) {

			for (final Iterator<?> it = ((IStructuredSelection) selection).iterator(); it.hasNext();) {

				final Object element = it.next();
				IProject project = null;

				if (element instanceof IProject) {

					project = (IProject) element;

				} else if (element instanceof IAdaptable) {

					project = (IProject) ((IAdaptable) element).getAdapter(IProject.class);

				}

				if (project != null) {

					toggleNature(project);

				}

			}

		}

		return null;

	}


	/**
	 *
	 * Internal method to toggle the nature.
	 *
	 * @since mlm.eclipse.ide.jsbuilder 1.0
	 *
	 */

	private void toggleNature( final IProject pProject ) {

		try {

			if (JavaScriptNature.addNature(pProject)) {

				// TODO keep in sync with AddMissingBuildScriptMarkerResolution --> use wizard with templates for different use cases
				final IFile scriptFile = pProject.getFile(JavaScriptBuilder.BUILDER_SCRIPT_NAME);
				if (!scriptFile.exists()) {

					final Bundle bundle = Activator.getDefault().getBundle();
					final URL url = FileLocator.find(bundle, new Path("resources/simple.js"), null); //$NON-NLS-1$
					if (url != null) {

						InputStream is = null;

						try {

							is = url.openStream();
							scriptFile.create(is, true, null);

						} finally {

							is.close();

						}

					}

				}

			} else {

				JavaScriptNature.removeNature(pProject);

			}

		} catch (final Exception ex) {

			// TODO log?
			ex.printStackTrace();

		}

	}


}
