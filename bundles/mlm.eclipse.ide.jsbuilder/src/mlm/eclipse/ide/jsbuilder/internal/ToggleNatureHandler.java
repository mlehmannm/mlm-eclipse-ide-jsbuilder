/*
 * Copyright (c) 2011 Marco Lehmann-Mörz. All rights reserved.
 */


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
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;


/**
 *
 * Handler to toggle the nature on a selected project.
 *
 * @author Marco Lehmann-Mörz
 *
 * @since mlm.eclipse.ide.jsbuilder 1.0
 *
 */

public class ToggleNatureHandler extends AbstractHandler {

	// TODO cleanup


	/**
	 * Toggles sample nature on a project
	 *
	 * @param project to have sample nature added or removed
	 */
	private void toggleNature( final IProject project ) {

		try {
			final IProjectDescription description = project.getDescription();
			final String[] natures = description.getNatureIds();

			for (int i = 0; i < natures.length; ++i) {
				if (JavaScriptNature.ID.equals(natures[i])) {
					// Remove the nature
					final String[] newNatures = new String[natures.length - 1];
					System.arraycopy(natures, 0, newNatures, 0, i);
					System.arraycopy(natures, i + 1, newNatures, i, natures.length - i - 1);
					description.setNatureIds(newNatures);
					project.setDescription(description, null);
					return;
				}
			}

			// Add the nature
			final String[] newNatures = new String[natures.length + 1];
			System.arraycopy(natures, 0, newNatures, 0, natures.length);
			newNatures[natures.length] = JavaScriptNature.ID;
			description.setNatureIds(newNatures);
			project.setDescription(description, null);

			final IFile scriptFile = project.getFile(JavaScriptBuilder.BUILDER_SCRIPT_NAME);
			if (!scriptFile.exists()) {

				final URL url = FileLocator.find(Activator.getDefault().getBundle(), new Path("resources/template.js"), null); //$NON-NLS-1$
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

		} catch (final Exception ex) {
			// TODO log?
			ex.printStackTrace();
		}

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


}
