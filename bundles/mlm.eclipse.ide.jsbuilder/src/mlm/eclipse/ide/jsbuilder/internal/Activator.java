/*
 * Copyright (c) 2011 Marco Lehmann-Mörz. All rights reserved.
 */


package mlm.eclipse.ide.jsbuilder.internal;


import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;


/**
 *
 * The activator class controls the plug-in life cycle.
 *
 * @author Marco Lehmann-Mörz
 *
 * @since mlm.eclipse.ide.jsbuilder 1.0
 *
 */

public final class Activator extends AbstractUIPlugin {


	/**
	 *
	 * The id of the plug-in (value <code>"mlm.eclipse.ide.jsbuilder"</code>).
	 *
	 */

	public static final String ID_PLUGIN = "mlm.eclipse.ide.jsbuilder"; //$NON-NLS-1$


	/**
	 *
	 * Holds the shared instance.
	 *
	 */

	private static Activator sSingleton;


	/**
	 *
	 * Constructs a new <code>Activator</code>.
	 *
	 * @since mlm.eclipse.ide.jsbuilder 1.0
	 *
	 */

	public Activator() {

		super();

	}


	/**
	 *
	 * {@inheritDoc}
	 *
	 * @see AbstractUIPlugin#start(BundleContext)
	 *
	 */

	@Override
	public void start( final BundleContext pContext ) throws Exception {

		super.start(pContext);

		sSingleton = this;

	}


	/**
	 *
	 * {@inheritDoc}
	 *
	 * @see AbstractUIPlugin#stop(BundleContext)
	 *
	 */

	@Override
	public void stop( final BundleContext pContext ) throws Exception {

		sSingleton = null;

		super.stop(pContext);

	}


	/**
	 *
	 * Returns the shared instance.
	 *
	 * @return the shared instance
	 *
	 * @since mlm.eclipse.ide.jsbuilder 1.0
	 *
	 */

	public static Activator getDefault() {

		return sSingleton;

	}


}
