package org.devops

// docker容器直接build
def DockerBuild(buildShell){
    sh """
        ${buildShell}
    """
}
