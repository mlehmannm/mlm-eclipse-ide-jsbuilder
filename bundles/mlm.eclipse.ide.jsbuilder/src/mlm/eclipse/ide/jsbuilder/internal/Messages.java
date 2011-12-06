/*
 * Copyright (c) 2011 Marco Lehmann-Mörz. All rights reserved.
 */


package mlm.eclipse.ide.jsbuilder.internal;


import org.eclipse.osgi.util.NLS;


/**
 *
 * Messages.
 *
 * @author Marco Lehmann-Mörz
 *
 * @since mlm.eclipse.ide.jsbuilder 1.0
 *
 */

public class Messages extends NLS {


	private static final String BUNDLE_NAME = "mlm.eclipse.ide.jsbuilder.internal.messages"; //$NON-NLS-1$


	public static String JavaScriptBuilder_builderScriptNotFound;


	public static String JavaScriptBuilder_functionBuildNotFound;


	public static String JavaScriptBuilder_functionCleanNotFound;


	public static String JavaScriptBuilder_unknownErrorSeeErrorLogForDetails;


	static {

		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);

	}


	private Messages() {

		super();

	}


}
