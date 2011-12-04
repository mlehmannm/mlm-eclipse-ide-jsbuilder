/*
 * Copyright (c) 2011 Marco Lehmann-Mörz. All rights reserved.
 */


package mlm.eclipse.ide.jsbuilder.internal;


import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;


/**
 *
 * Nature for the JavaScript builder.
 *
 * @author Marco Lehmann-Mörz
 *
 * @since mlm.eclipse.ide.jsbuilder 1.0
 *
 */

public class JavaScriptNature implements IProjectNature {


	/**
	 *
	 * The id of the nature.
	 *
	 */

	public static final String ID = "mlm.eclipse.ide.jsbuilder.nature"; //$NON-NLS-1$


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


	/**
	 *
	 * {@inheritDoc}
	 *
	 * @see IProjectNature#getProject()
	 *
	 */

    public IProject getProject() {

		return mProject;

	}


	/**
	 *
	 * {@inheritDoc}
	 *
	 * @see IProjectNature#setProject(IProject)
	 *
	 */

    public void setProject( final IProject pProject ) {

		mProject = pProject;

	}


	/**
	 *
	 * {@inheritDoc}
	 *
	 * @see IProjectNature#configure()
	 *
	 */

    public void configure() throws CoreException {

		final IProjectDescription desc = mProject.getDescription();
		final ICommand[] commands = desc.getBuildSpec();

		for (final ICommand command : commands) {

			if (JavaScriptBuilder.ID.equals(command.getBuilderName())) {

				return;

			}

		}

		// add builder to list of builders
		final ICommand[] newCommands = new ICommand[commands.length + 1];
		System.arraycopy(commands, 0, newCommands, 0, commands.length);
		final ICommand command = desc.newCommand();
		command.setBuilderName(JavaScriptBuilder.ID);
		newCommands[newCommands.length - 1] = command;
		desc.setBuildSpec(newCommands);
		mProject.setDescription(desc, null);

	}


	/**
	 *
	 * {@inheritDoc}
	 *
	 * @see IProjectNature#deconfigure()
	 *
	 */

    public void deconfigure() throws CoreException {

		final IProjectDescription description = getProject().getDescription();
		final ICommand[] commands = description.getBuildSpec();

		for (int i = 0; i < commands.length; ++i) {

			if (JavaScriptBuilder.ID.equals(commands[i].getBuilderName())) {

				// remove builder from list of builders
				final ICommand[] newCommands = new ICommand[commands.length - 1];
				System.arraycopy(commands, 0, newCommands, 0, i);
				System.arraycopy(commands, i + 1, newCommands, i, commands.length - i - 1);
				description.setBuildSpec(newCommands);
				mProject.setDescription(description, null);

				return;

			}

		}

	}


}
