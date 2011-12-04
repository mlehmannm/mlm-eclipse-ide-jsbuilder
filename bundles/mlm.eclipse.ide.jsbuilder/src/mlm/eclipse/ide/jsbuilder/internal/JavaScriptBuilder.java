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
 * JavaScript builder.
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
	 * The (compiled) builder script.
	 *
	 */

	private Script mScript;


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

			// enter context
			final Context cx = mContextFactory.enterContext();

			// create (and configure) global script scope (sealed)
			mGlobalScriptScope = new ImporterTopLevel(cx, true);
			ScriptableObject.putProperty(mGlobalScriptScope, "builder", Context.javaToJS(this, mGlobalScriptScope)); //$NON-NLS-1$
			// TODO add marker constants that can be used by the builder script to global scope

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
				Activator.getDefault().getLog().log(status);

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

		// check script
		if (mScript == null) {

			// missing script and/or compile errors have been already reported
			return;

		}

		// find named function "build"
		final Function func = findFunction("clean"); //$NON-NLS-1$
		if (func == null) {

			// TODO log? add marker to builder script? quick fix?
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
			func.call(cx, mScriptScope, mScriptScope, funcArgs);

		} catch (final Exception ex) {

			// TODO log? add problem marker to project?
			ex.printStackTrace();

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
		initialiseBuilderScript();

		// check builder script
		if (mScript == null) {

			// missing script and/or compile errors have been already reported
			return null;

		}

		// find named function "build"
		final Function func = findFunction("build"); //$NON-NLS-1$
		if (func == null) {

			// TODO log? add marker to builder script? quick fix?
			return null;

		}

		try {

			// enter context
			final Context cx = mContextFactory.enterContext();

			// define function arguments
			final Object funcArgs[] = {
			    Integer.valueOf(pKind), //
			    pArgs, //
			    SubMonitor.convert(pMonitor), //
			};

			// call function
			final Object retVal = func.call(cx, mScriptScope, mScriptScope, funcArgs);

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

			ex.printStackTrace();

			try {

				final IMarker marker = mScriptFile.createMarker(ID_PROBLEM_MARKER);
				marker.setAttribute(IMarker.MESSAGE, ex.details());
				marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
				marker.setAttribute(IMarker.LINE_NUMBER, ex.lineNumber());

			} catch (final CoreException cex) {

				// intentionally left empty

			}

		} catch (final Exception ex) {

			// TODO log? add problem marker to project?
			ex.printStackTrace();

		} finally {

			// exit context
			Context.exit();

		}

		return null;

	}


	/**
	 *
	 * Internal method to find a function with a given name.
	 *
	 * @since mlm.eclipse.ide.jsbuilder 1.0
	 *
	 */

	private Function findFunction( final String pName ) {

		final Object functionObj = mScriptScope.get(pName, mScriptScope);
		if (functionObj instanceof Function) {

			return (Function) functionObj;

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

	private void initialiseBuilderScript() {

		if (mScriptScope != null && mScriptFile != null && mScript != null) {

			return;

		}

		resetBuilderScript();

		final IFile scriptFile = getProject().getFile(BUILDER_SCRIPT_NAME);
		if (!scriptFile.exists()) {

			// TODO log? add problem marker to project? quick fix?

			return;

		}

		try {

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

			// remember script scope and (compiled) script
			mScriptScope = scriptScope;
			mScriptFile = scriptFile;
			mScript = script;

		} catch (final RhinoException ex) {

			ex.printStackTrace();

			try {

				final IMarker marker = scriptFile.createMarker(ID_PROBLEM_MARKER);
				marker.setAttribute(IMarker.MESSAGE, ex.details());
				marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
				marker.setAttribute(IMarker.LINE_NUMBER, ex.lineNumber());

			} catch (final CoreException cex) {

				// intentionally left empty

			}

		} catch (final Exception ex) {

			ex.printStackTrace();

			try {

				final IMarker marker = scriptFile.createMarker(ID_PROBLEM_MARKER);
				marker.setAttribute(IMarker.MESSAGE, "Unknown error");
				marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);

			} catch (final CoreException cex) {

				// intentionally left empty

			}

		} finally {

			close(isr);

			// exit context
			Context.exit();

		}

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
		mScript = null;

	}


	/**
	 *
	 * TODO
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
