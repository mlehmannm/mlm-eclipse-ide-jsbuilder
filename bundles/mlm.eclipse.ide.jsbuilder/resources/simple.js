//
// builder.js (https://github.com/mlehmannm/mlm-eclipse-ide-jsbuilder)
//
// Available global variables:
// 
//     builder - instance of org.eclipse.core.resources.IncrementalProjectBuilder
//     log     - instance of org.eclipse.core.runtime.ILog
//

importPackage(org.eclipse.core.resources);
importPackage(org.eclipse.core.runtime);

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

	// log
	var message = java.lang.String.format("clean for project '%s' (%s)", project.getName(), project.getFullPath());
	log.log(new Status(IStatus.OK, "jsbuilder", message));

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
 * @returns array of {org.eclipse.core.resources.IProject} projects to receive resource deltas for
 * 
 * @see org.eclipse.core.resources.IncrementalProjectBuilder.build(int, String, Map, IProgressMonitor)
 * 
 */

function build(pKind, pArgs, pMonitor) {

	var project = builder.getProject();

	if (pKind == IncrementalProjectBuilder.FULL_BUILD) {

		// log
		var message = java.lang.String.format("full build for project '%s' (%s)", project.getName(), project.getFullPath());
		log.log(new Status(IStatus.OK, "jsbuilder", message));

	} else {

		// log
		var startMessage = java.lang.String.format("start build for project '%s' (%s)", project.getName(), project.getFullPath());
		log.log(new Status(IStatus.OK, "jsbuilder", startMessage));

		var delta = builder.getDelta(project);
		if (delta != null) {

			delta.accept(new IResourceDeltaVisitor({

				visit : function(delta) {

					// log
					var message = java.lang.String.format("changed resource '%s'", delta.getResource());
					log.log(new Status(IStatus.OK, "jsbuilder", message));

					return true;

				}

			}))

		} else {

			log.log(new Status(IStatus.OK, "jsbuilder", "empty delta"));

		}

		// log
		var endMessage = java.lang.String.format("end build for project '%s' (%s)", project.getName(), project.getFullPath());
		log.log(new Status(IStatus.OK, "jsbuilder", endMessage));

	}

}
