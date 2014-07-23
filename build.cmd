@setlocal
@call mvn --version
@call mvn org.eclipse.tycho:tycho-versions-plugin:update-pom -Dtycho.mode=maven -q
@call mvn verify %*
