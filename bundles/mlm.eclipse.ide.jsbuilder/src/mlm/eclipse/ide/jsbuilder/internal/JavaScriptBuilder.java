/*
 * Copyright (c) 2011 Marco Lehmann-Mörz. All rights reserved.
 */


package mlm.eclipse.ide.jsbuilder.internal;


import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;


/**
 *
 * JavaScript-based builder.
 * <p>
 * Calls to {@link #clean(IProgressMonitor)} and {@link #build(int, Map, IProgressMonitor)} will be delegated to a script named
 * <code>builder.js</code> in the root of the project.
 * </p>
 *
 * @author Marco Lehmann-Mörz
 *
 * @since mlm.eclipse.ide.jsbuilder 1.0
 *
 */

public final class JavaScriptBuilder extends IncrementalProjectBuilder {


	/**
	 *
	 * The id of the builder.
	 *
	 */

	public static final String ID = "mlm.eclipse.ide.jsbuilder.builder"; //$NON-NLS-1$


	/**
	 *
	 * The id of the marker.
	 *
	 */

	public static final String ID_MARKER = "mlm.eclipse.ide.jsbuilder.marker"; //$NON-NLS-1$


	/**
	 *
	 * The id of the problem marker.
	 *
	 */

	public static final String ID_PROBLEM_MARKER = "mlm.eclipse.ide.jsbuilder.problemmarker"; //$NON-NLS-1$


	/**
	 *
	 * The name of the builder script.
	 *
	 */

	public static final String BUILDER_SCRIPT_NAME = "builder.js"; //$NON-NLS-1$


	/**
	 *
	 * The context factory used to create script contexts.
	 *
	 */

	private final ContextFactory mContextFactory = new ContextFactory() {


		{
			// use bundle class loader to gain access to classes of dependent bundles (like PDE)
			initApplicationClassLoader(JavaScriptBuilder.class.getClassLoader());
		}


		@Override
		protected boolean hasFeature( final Context pContext, final int pFeatureIndex ) {

			// TODO is this necessary here?
			if (pFeatureIndex == Context.FEATURE_DYNAMIC_SCOPE) {

				return true;

			}

			return super.hasFeature(pContext, pFeatureIndex);

		}


	};


	/**
	 *
	 * The global script scope used to hold utility functions.
	 *
	 */

	private ScriptableObject mGlobalScriptScope;


	/**
	 *
	 * The script scope used to compile the builder script.
	 *
	 */

	private Scriptable mScriptScope;


	/**
	 *
	 * The builder script.
	 *
	 */

	private IFile mScriptFile;


	/**
	 *
	 * The "clean" function from the builder script.
	 *
	 */

	private Function mCleanFunc;


	/**
	 *
	 * The "build" function from the builder script.
	 *
	 */

	private Function mBuildFunc;


	/**
	 *
	 * The resource delta visitor to find out, if the builder script has been changed.
	 *
	 */

	private final IResourceDeltaVisitor mResourceDeltaVisitor = new IResourceDeltaVisitor() {


		public boolean visit( final IResourceDelta pDelta ) throws CoreException {

			if (mScriptFile != null && !mScriptFile.equals(pDelta.getResource())) {

				return true;

			}

			resetBuilderScript();

			return false;

		}


	};


	/**
	 *
	 * Constructs a new <code>JavaScriptBuilder</code>.
	 *
	 * @since mlm.eclipse.ide.jsbuilder 1.0
	 *
	 */

	public JavaScriptBuilder() {

		try {

			final ILog log = Activator.getDefault().getLog();

			// enter context
			final Context cx = mContextFactory.enterContext();

			// create (and configure) global script scope (sealed)
			mGlobalScriptScope = new ImporterTopLevel(cx, true);
			ScriptableObject.putConstProperty(mGlobalScriptScope, "builder", Context.javaToJS(this, mGlobalScriptScope)); //$NON-NLS-1$
			ScriptableObject.putConstProperty(mGlobalScriptScope, "log", Context.javaToJS(log, mGlobalScriptScope)); //$NON-NLS-1$
			ScriptableObject.putConstProperty(mGlobalScriptScope, "ID_MARKER", ID_MARKER); //$NON-NLS-1$
			ScriptableObject.putConstProperty(mGlobalScriptScope, "ID_PROBLEM_MARKER", ID_PROBLEM_MARKER); //$NON-NLS-1$

			URL url = null;
			InputStreamReader isr = null;

			// TODO load utilities via extension point?

			try {

				// load and compile script with utility functions
				url = FileLocator.find(Activator.getDefault().getBundle(), new Path("resources/utils.js"), null); //$NON-NLS-1$
				isr = new InputStreamReader(url.openStream());
				final Script script = cx.compileReader(isr, url.toString(), 1, null);
				script.exec(cx, mGlobalScriptScope);

			} catch (final Exception ex) {

				// log
				final String msg = "Error loading utils! Some things may not work."; //$NON-NLS-1$
				final MultiStatus status = new MultiStatus(Activator.ID_PLUGIN, 0, msg, ex);
				status.add(new Status(IStatus.ERROR, status.getPlugin(), "url = " + url)); //$NON-NLS-1$
				log.log(status);

			} finally {

				close(isr);

			}

		} finally {

			// exit context
			Context.exit();

		}

	}


	/**
	 *
	 * {@inheritDoc}
	 *
	 * @see IncrementalProjectBuilder#clean(IProgressMonitor)
	 *
	 */

	@Override
	protected void clean( final IProgressMonitor pMonitor ) throws CoreException {

		super.clean(pMonitor);

		resetBuilderScript();

		initialiseBuilderScript();

		// check for "clean" function
		if (mCleanFunc == null) {

			// missing script, compile errors or missing "build" function have been already reported
			return;

		}

		try {

			// enter context
			final Context cx = mContextFactory.enterContext();

			// define function arguments
			final Object funcArgs[] = {
				SubMonitor.convert(pMonitor)
			};

			// call function
			mCleanFunc.call(cx, mScriptScope, mScriptScope, funcArgs);

		} catch (final RhinoException ex) {

			// log
			final String msg = "Error during call to function 'clean' within builder script 'builder.js'!"; //$NON-NLS-1$
			Activator.getDefault().getLog().log(new Status(IStatus.ERROR, Activator.ID_PLUGIN, msg, ex));

			try {

				// report problem as marker
				final IMarker marker = mScriptFile.createMarker(ID_PROBLEM_MARKER);
				marker.setAttribute(IMarker.MESSAGE, ex.details());
				marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
				marker.setAttribute(IMarker.LINE_NUMBER, ex.lineNumber());

			} catch (final CoreException cex) {

				// intentionally left empty

			}

		} catch (final Exception ex) {

			// log
			final String msg = "Error during call to function 'clean' within builder script 'builder.js'!"; //$NON-NLS-1$
			Activator.getDefault().getLog().log(new Status(IStatus.ERROR, Activator.ID_PLUGIN, msg, ex));

			try {

				// report problem as marker
				final IMarker marker = mScriptFile.createMarker(ID_PROBLEM_MARKER);
				marker.setAttribute(IMarker.MESSAGE, Messages.JavaScriptBuilder_unknownErrorSeeErrorLogForDetails);
				marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);

			} catch (final CoreException cex) {

				// intentionally left empty

			}

		} finally {

			// exit context
			Context.exit();

		}

	}


	/**
	 *
	 * {@inheritDoc}
	 *
	 * @see IncrementalProjectBuilder#build(int, Map, IProgressMonitor)
	 *
	 */

	@Override
	@SuppressWarnings("rawtypes")
	protected IProject[] build( final int pKind, final Map pArgs, final IProgressMonitor pMonitor ) throws CoreException {

		if (pKind == FULL_BUILD) {

			// always reset builder script on full build
			resetBuilderScript();

			return delegateBuild(pKind, pArgs, pMonitor);

		}

		final IResourceDelta delta = getDelta(getProject());
		if (delta != null) {

			// check and reset builder script, if necessary
			delta.accept(mResourceDeltaVisitor);

			return delegateBuild(pKind, pArgs, pMonitor);

		}

		// always reset builder script on unknown build state
		resetBuilderScript();

		return delegateBuild(pKind, pArgs, pMonitor);

	}


	/**
	 *
	 * Internal method to delegate the build to the builder script.
	 *
	 * @since mlm.eclipse.ide.jsbuilder 1.0
	 *
	 */

	@SuppressWarnings("rawtypes")
	private IProject[] delegateBuild( final int pKind, final Map pArgs, final IProgressMonitor pMonitor ) {

		// initialise builder script, if necessary
		final boolean builderScriptChanged = initialiseBuilderScript();

		// change "kind" if script has changed
		final int kind = builderScriptChanged ? FULL_BUILD : pKind;

		// check for "build" function
		if (mBuildFunc == null) {

			// missing script, compile errors or missing "build" function have been already reported
			return null;

		}

		try {

			// enter context
			final Context cx = mContextFactory.enterContext();

			// define function arguments
			final Object funcArgs[] = {
			    Integer.valueOf(kind), //
			    pArgs, //
			    SubMonitor.convert(pMonitor), //
			};

			// call function
			final Object retVal = mBuildFunc.call(cx, mScriptScope, mScriptScope, funcArgs);

			// check for array of projects
			if (NativeJavaObject.canConvert(retVal, IProject[].class)) {

				return (IProject[]) Context.jsToJava(retVal, IProject[].class);

			}

			// check for project
			if (NativeJavaObject.canConvert(retVal, IProject.class)) {

				return new IProject[] {
					(IProject) Context.jsToJava(retVal, IProject.class)
				};

			}

		} catch (final RhinoException ex) {

			// log
			final String msg = "Error during call to function 'build' within builder script 'builder.js'!"; //$NON-NLS-1$
			Activator.getDefault().getLog().log(new Status(IStatus.ERROR, Activator.ID_PLUGIN, msg, ex));

			try {

				// report problem as marker
				final IMarker marker = mScriptFile.createMarker(ID_PROBLEM_MARKER);
				marker.setAttribute(IMarker.MESSAGE, ex.details());
				marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
				marker.setAttribute(IMarker.LINE_NUMBER, ex.lineNumber());

			} catch (final CoreException cex) {

				// intentionally left empty

			}

		} catch (final Exception ex) {

			// log
			final String msg = "Error during call to function 'build' within builder script 'builder.js'!"; //$NON-NLS-1$
			Activator.getDefault().getLog().log(new Status(IStatus.ERROR, Activator.ID_PLUGIN, msg, ex));

			try {

				// report problem as marker
				final IMarker marker = mScriptFile.createMarker(ID_PROBLEM_MARKER);
				marker.setAttribute(IMarker.MESSAGE, Messages.JavaScriptBuilder_unknownErrorSeeErrorLogForDetails);
				marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);

			} catch (final CoreException cex) {

				// intentionally left empty

			}

		} finally {

			// exit context
			Context.exit();

		}

		return null;

	}


	/**
	 *
	 * Internal method to initialise the builder script and associated data.
	 *
	 * @since mlm.eclipse.ide.jsbuilder 1.0
	 *
	 */

	private boolean initialiseBuilderScript() {

		if (mScriptScope != null && mScriptFile != null) {

			return false;

		}

		resetBuilderScript();

		final IProject project = getProject();

		try {

			// remove markers from script file
			project.deleteMarkers(ID_PROBLEM_MARKER, false, IResource.DEPTH_ZERO);

		} catch (final CoreException ex) {

			// intentionally left empty

		}

		// check if build script file exists
		final IFile scriptFile = project.getFile(BUILDER_SCRIPT_NAME);
		if (!scriptFile.exists()) {

			try {

				// report problem as marker
				final IMarker marker = project.createMarker(ID_PROBLEM_MARKER);
				marker.setAttribute(IMarker.MESSAGE, Messages.JavaScriptBuilder_builderScriptNotFound);
				marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
				marker.setAttribute(ID_PROBLEM_MARKER, "missingBuilderJS");

			} catch (final CoreException cex) {

				// intentionally left empty

			}

			return false;

		}

		try {

			// remove markers from script file
			scriptFile.deleteMarkers(ID_PROBLEM_MARKER, false, IResource.DEPTH_ZERO);

		} catch (final CoreException ex) {

			// intentionally left empty

		}

		InputStreamReader isr = null;

		try {

			// enter context
			final Context cx = mContextFactory.enterContext();

			// create (and configure) script scope
			final Scriptable scriptScope = cx.newObject(mGlobalScriptScope);
			scriptScope.setPrototype(mGlobalScriptScope);
			scriptScope.setParentScope(null);

			final String charset = scriptFile.getCharset();
			final InputStream is = scriptFile.getContents();
			isr = new InputStreamReader(is, charset);

			// compile script
			final Script script = cx.compileReader(isr, scriptFile.getFullPath().toString(), 1, null);

			// execute script (bind functions to scope)
			script.exec(cx, scriptScope);

			// find "clean" function
			Object cleanFuncObj = scriptScope.get("clean", scriptScope); //$NON-NLS-1$
			if (!(cleanFuncObj instanceof Function)) {

				cleanFuncObj = null;

				try {

					// report problem as marker
					final IMarker marker = scriptFile.createMarker(ID_PROBLEM_MARKER);
					marker.setAttribute(IMarker.MESSAGE, Messages.JavaScriptBuilder_functionCleanNotFound);
					marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);

				} catch (final CoreException cex) {

					// intentionally left empty

				}

			}

			// find "build" function
			Object buildFuncObj = scriptScope.get("build", scriptScope); //$NON-NLS-1$
			if (!(buildFuncObj instanceof Function)) {

				buildFuncObj = null;

				try {

					// report problem as marker
					final IMarker marker = scriptFile.createMarker(ID_PROBLEM_MARKER);
					marker.setAttribute(IMarker.MESSAGE, Messages.JavaScriptBuilder_functionBuildNotFound);
					marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);

				} catch (final CoreException cex) {

					// intentionally left empty

				}

			}

			// remember various variables
			mScriptScope = scriptScope;
			mScriptFile = scriptFile;
			mCleanFunc = (Function) cleanFuncObj;
			mBuildFunc = (Function) buildFuncObj;

			return true;

		} catch (final RhinoException ex) {

			// log
			final String msg = "Error during compile of 'builder.js'!"; //$NON-NLS-1$
			Activator.getDefault().getLog().log(new Status(IStatus.ERROR, Activator.ID_PLUGIN, msg, ex));

			try {

				// report problem as marker
				final IMarker marker = scriptFile.createMarker(ID_PROBLEM_MARKER);
				marker.setAttribute(IMarker.MESSAGE, ex.details());
				marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
				marker.setAttribute(IMarker.LINE_NUMBER, ex.lineNumber());

			} catch (final CoreException cex) {

				// intentionally left empty

			}

		} catch (final Exception ex) {

			// log
			final String msg = "Error during compile of builder script 'builder.js'!"; //$NON-NLS-1$
			Activator.getDefault().getLog().log(new Status(IStatus.ERROR, Activator.ID_PLUGIN, msg, ex));

			try {

				// report problem as marker
				final IMarker marker = scriptFile.createMarker(ID_PROBLEM_MARKER);
				marker.setAttribute(IMarker.MESSAGE, Messages.JavaScriptBuilder_unknownErrorSeeErrorLogForDetails);
				marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);

			} catch (final CoreException cex) {

				// intentionally left empty

			}

		} finally {

			close(isr);

			// exit context
			Context.exit();

		}

		return false;

	}


	/**
	 *
	 * Internal method to reset the builder script and associated data.
	 *
	 * @since mlm.eclipse.ide.jsbuilder 1.0
	 *
	 */

	private void resetBuilderScript() {

		mScriptScope = null;
		mScriptFile = null;
		mCleanFunc = null;
		mBuildFunc = null;

	}


	/**
	 *
	 * Internal method to close a {@link Closeable} silently.
	 *
	 * @since mlm.eclipse.ide.jsbuilder 1.0
	 *
	 */

	private void close( final Closeable pCloseable ) {

		if (pCloseable != null) {

			try {

				pCloseable.close();

			} catch (final IOException ex) {

				// intentionally left empty

			}

		}

	}


}
