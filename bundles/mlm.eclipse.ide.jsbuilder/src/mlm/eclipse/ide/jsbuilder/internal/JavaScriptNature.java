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


package mlm.eclipse.ide.jsbuilder.internal;


import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;


/**
 *
 * Nature for the JavaScript-based builder.
 *
 * @author Marco Lehmann-Mörz
 *
 * @since mlm.eclipse.ide.jsbuilder 1.0
 *
 */

public final class JavaScriptNature implements IProjectNature {


	/**
	 *
	 * The project.
	 *
	 */

	private IProject mProject;


	/**
	 *
	 * Constructs a new <code>JavaScriptNature</code>.
	 *
	 * @since mlm.eclipse.ide.jsbuilder 1.0
	 *
	 */

	public JavaScriptNature() {

		super();

	}


	@Override
	public IProject getProject() {

		return mProject;

	}


	@Override
	public void setProject( final IProject pProject ) {

		mProject = pProject;

	}


	@Override
	public void configure() throws CoreException {

		final IProjectDescription desc = mProject.getDescription();
		final ICommand[] commands = desc.getBuildSpec();

		for (final ICommand command : commands) {

			if (Activator.ID_BUILDER.equals(command.getBuilderName())) {

				return;

			}

		}

		// add builder to list of builders
		final ICommand[] newCommands = new ICommand[commands.length + 1];
		System.arraycopy(commands, 0, newCommands, 0, commands.length);
		final ICommand command = desc.newCommand();
		command.setBuilderName(Activator.ID_BUILDER);
		newCommands[newCommands.length - 1] = command;
		desc.setBuildSpec(newCommands);
		mProject.setDescription(desc, null);

	}


	@Override
	public void deconfigure() throws CoreException {

		final IProjectDescription description = mProject.getDescription();
		final ICommand[] commands = description.getBuildSpec();

		for (int i = 0; i < commands.length; ++i) {

			if (Activator.ID_BUILDER.equals(commands[i].getBuilderName())) {

				// remove builder from list of builders
				final ICommand[] newCommands = new ICommand[commands.length - 1];
				System.arraycopy(commands, 0, newCommands, 0, i);
				System.arraycopy(commands, i + 1, newCommands, i, commands.length - i - 1);
				description.setBuildSpec(newCommands);
				mProject.setDescription(description, null);

				try {

					// remove all markers
					mProject.deleteMarkers(Activator.ID_PROBLEM_MARKER, false, IResource.DEPTH_INFINITE);
					mProject.deleteMarkers(Activator.ID_MARKER, false, IResource.DEPTH_INFINITE);

				} catch (final CoreException ex) {

					// intentionally left empty

				}

				return;

			}

		}

	}


	/**
	 *
	 * Adds this nature to the given project, if not already present.
	 *
	 * @param pProject project
	 *
	 * @return <code>true</code> if added; <code>false</code> if nature is already present
	 *
	 * @throws CoreException if access to project description failed
	 *
	 * @see IProject#getDescription()
	 * @see IProject#setDescription(IProjectDescription, IProgressMonitor)
	 *
	 * @since mlm.eclipse.ide.jsbuilder 1.0
	 *
	 */

	public static final boolean addNature( final IProject pProject ) throws CoreException {

		// check if nature is already present
		final IProjectDescription description = pProject.getDescription();
		final String[] natures = description.getNatureIds();
		for (int i = 0; i < natures.length; ++i) {

			if (Activator.ID_NATURE.equals(natures[i])) {

				// already present
				return false;

			}

		}

		// add nature
		final String[] newNatures = new String[natures.length + 1];
		System.arraycopy(natures, 0, newNatures, 0, natures.length);
		newNatures[natures.length] = Activator.ID_NATURE;
		description.setNatureIds(newNatures);
		pProject.setDescription(description, null);

		return true;

	}


	/**
	 *
	 * Removes this nature from the given project, if present.
	 *
	 * @param pProject project
	 *
	 * @return <code>true</code> if removed; <code>false</code> if nature is not present
	 *
	 * @throws CoreException if access to project description failed
	 *
	 * @see IProject#getDescription()
	 * @see IProject#setDescription(IProjectDescription, IProgressMonitor)
	 *
	 * @since mlm.eclipse.ide.jsbuilder 1.0
	 *
	 */

	public static final boolean removeNature( final IProject pProject ) throws CoreException {

		// check if nature is present
		final IProjectDescription description = pProject.getDescription();
		final String[] natures = description.getNatureIds();
		for (int i = 0; i < natures.length; ++i) {

			if (Activator.ID_NATURE.equals(natures[i])) {

				// remove nature
				final String[] newNatures = new String[natures.length - 1];
				System.arraycopy(natures, 0, newNatures, 0, i);
				System.arraycopy(natures, i + 1, newNatures, i, natures.length - i - 1);
				description.setNatureIds(newNatures);
				pProject.setDescription(description, null);

				return true;

			}

		}

		return false;

	}


}
