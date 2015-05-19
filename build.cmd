@setlocal
@call mvn --version
@rem call mvn -q org.eclipse.tycho:tycho-versions-plugin:update-pom -Dtycho.mode=maven
@call mvn -e verify %*
