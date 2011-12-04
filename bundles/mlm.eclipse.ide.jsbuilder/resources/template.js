/**
 * 
 * Clean.
 * 
 * @param pMonitor
 *            {org.eclipse.core.runtime.IProgressMonitor} progress monitor
 * 
 * @see org.eclipse.core.resources.IncrementalProjectBuilder.clean(IProgressMonitor)
 * 
 */

function clean(pMonitor) {

	var project = builder.getProject();

	System.out.println("clean for " + project.getName());
	System.out.println("\tfull path = " + project.getFullPath());

}

/**
 * 
 * Build.
 * 
 * @param pKind
 *            {java.lang.Integer} kind of build
 * @param pArgs
 *            {java.util.Map} builder arguments (may be <code>null</code>)
 * @param pMonitor
 *            {org.eclipse.core.runtime.IProgressMonitor} progress monitor
 * 
 * @see org.eclipse.core.resources.IncrementalProjectBuilder.build(int, String, Map, IProgressMonitor)
 * 
 */

function build(pKind, pArgs, pMonitor) {

	var project = builder.getProject();

	System.out.println("build for " + project.getName());
	System.out.println("\tfull path = " + project.getFullPath());
	System.out.println("\tkind = " + pKind);
	System.out.println("\targs = " + pArgs);

}
