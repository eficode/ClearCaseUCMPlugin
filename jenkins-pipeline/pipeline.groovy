BRANCH = 'master'
NAME = 'ClearCaseUCM_Plugin'
REPO = 'git@github.com:Praqma/ClearCaseUCMPlugin.git'
MAIL = 'man@praqma.net'
RELEASE_TARGET = 'git@github.com:jenkinsci/clearcase-ucm-plugin.git'


/***************************\
| VERIFY INTEGRATION BRANCH |
\***************************/
job("${NAME}_verify_${BRANCH}") {
    description("Runs compilation smoketest the ${BRANCH} branch.")
    logRotator(-1, 50, -1, -1)
  	label('cc-nightcrawler')
    scm {
        git {
            remote {
                url("${REPO}")
            }
            branch("${BRANCH}")
            configure {
                node ->
                    node / 'extensions' << 'hudson.plugins.git.extensions.impl.CleanBeforeCheckout' {}
            }
        }

    }
  	properties {
        environmentVariables {
          	keepSystemVariables(true)
          	keepBuildVariables(true)
          	env('REPO', "${REPO}")
            env('BRANCH', "${BRANCH}")
        }
  	}

    triggers {
    	//scm('H/3 * * * *')
    }
  
    steps {
      	maven{
          	goals('clean package -DskipTests')
      	}
        downstreamParameterized {
            trigger("${NAME}_static_${BRANCH}") {
              	parameters{
              		gitRevision(true)
                	predefinedProp('NAME', "${NAME}")
                	predefinedProp('REPO', "${REPO}")
                	predefinedProp('BRANCH', "${BRANCH}")
                	predefinedProp('UPSTREAM_JOB_NAME', '${JOB_NAME}')
                	predefinedProp('UPSTREAM_JOB_NO', '${BUILD_NUMBER}')
              	}
            }
        }
    }
  
    wrappers {
        buildName('''#${BUILD_NUMBER}#${GIT_REVISION,length=8}(${GIT_BRANCH}''')
    }
  
    publishers {
      archiveArtifacts("**/clearcase-ucm-plugin.hpi")
      mailer("${MAIL}", false, false)
    }
}

/***************************\
| STATIC INTEGRATION BRANCH |
\***************************/
job("${NAME}_static_${BRANCH}") {
    description("Runs static analysis on the ${BRANCH} branch.")
    logRotator(-1, 50, -1, -1)
  	label('cc-nightcrawler')
    scm {
        git {
            remote {
                url("${REPO}")
            }
            branch("${BRANCH}")
            configure {
                node ->
                    node / 'extensions' << 'hudson.plugins.git.extensions.impl.CleanBeforeCheckout' {}
            }
        }

    }
  
  	properties {
        environmentVariables {
          	keepSystemVariables(true)
          	keepBuildVariables(true)
          	env('REPO', "${REPO}")
            env('BRANCH', "${BRANCH}")
        }
  	}

    steps {
      	maven{
          	goals('clean package -Pstatic')
      	}
        downstreamParameterized {
              trigger("${NAME}_javadoc_${BRANCH}") {
                  parameters{
                      gitRevision(false)
                      predefinedProp('NAME', "${NAME}")
                      predefinedProp('REPO', "${REPO}")
                      predefinedProp('BRANCH', "${BRANCH}")
                      predefinedProp('UPSTREAM_JOB_NAME', '${JOB_NAME}')
                      predefinedProp('UPSTREAM_JOB_NO', '${BUILD_NUMBER}')
                  }
              }
          }

    }
  
    wrappers {
        buildName('''#${BUILD_NUMBER}#${GIT_REVISION,length=8}(${GIT_BRANCH}''')
    }
  
    publishers {
      
      tasks('**/.java', '', 'FIXME', 'TODO', '', true) {
        healthLimits(null, null)
        thresholdLimit('high')
        defaultEncoding('UTF-8')
        canRunOnFailed(true)
        useStableBuildAsReference(true)
        useDeltaValues(true)
        computeNew(true)
        shouldDetectModules(true)
      }
      
      pmd('**/pmd.xml') {
                healthLimits(null, null)
                thresholdLimit('high')
                defaultEncoding('UTF-8')
                canRunOnFailed(true)
                useStableBuildAsReference(true)
                useDeltaValues(true)
                computeNew(true)
                shouldDetectModules(true)
    
      }
      
      findbugs('**/findbugsXml.xml', false) {
        healthLimits(null, null)
        thresholdLimit('high')
        defaultEncoding('UTF-8')
        canRunOnFailed(true)
        useStableBuildAsReference(true)
        useDeltaValues(true)
        computeNew(true)
        shouldDetectModules(true)
      }
      
      mailer("${MAIL}", false, false)
    }  
  
}

/***************************\
| JAVADOC  |
\***************************/
job("${NAME}_javadoc_${BRANCH}") {
    description("Generates JavaDoc for ${BRANCH} branch.")
    logRotator(-1, 50, -1, -1)
  	label('cc-nightcrawler')
    scm {
        git {
            remote {
                url("${REPO}")
            }
            branch("${BRANCH}")
            configure {
                node ->
                    node / 'extensions' << 'hudson.plugins.git.extensions.impl.CleanBeforeCheckout' {}
            }
        }

    }
  	properties {
        environmentVariables {
          	keepSystemVariables(true)
          	keepBuildVariables(true)
          	env('REPO', "${REPO}")
            env('BRANCH', "${BRANCH}")
        }
  	}

    steps {
      	maven{
          	goals('clean package -DskipTests -Pjavadoc')
      	}
        downstreamParameterized {
            trigger("${NAME}_integrationtest_${BRANCH}") {
              	parameters{
              		gitRevision(true)
                	predefinedProp('NAME', "${NAME}")
                	predefinedProp('REPO', "${REPO}")
                	predefinedProp('BRANCH', "${BRANCH}")
                	predefinedProp('UPSTREAM_JOB_NAME', '${JOB_NAME}')
                	predefinedProp('UPSTREAM_JOB_NO', '${BUILD_NUMBER}')
              	}
            }
        }
    }
  
    wrappers {
        buildName('''#${BUILD_NUMBER}#${GIT_REVISION,length=8}(${GIT_BRANCH}''')
    }
  
    publishers {
      archiveJavadoc {
        javadocDir('target/site/apidocs')
      }
      mailer("${MAIL}", false, false)
    }
}

/***************************\
|  INTEGRATION TESTS |
\***************************/
job("${NAME}_integrationtest_${BRANCH}") {
    description("Runs integration tests on the ${BRANCH} branch.")
    logRotator(-1, 50, -1, -1)
  	label('cc-nightcrawler')
    scm {
        git {
            remote {
                url("${REPO}")
            }
            branch("${BRANCH}")
            configure {
                node ->
                    node / 'extensions' << 'hudson.plugins.git.extensions.impl.CleanBeforeCheckout' {}
            }
        }

    }
  	properties {
        environmentVariables {
          	keepSystemVariables(true)
          	keepBuildVariables(true)
          	env('REPO', "${REPO}")
            env('BRANCH', "${BRANCH}")
        }
  	}

    steps {
      	maven{
          	goals('clean verify')
      	}
    }
  
    wrappers {
        buildName('''#${BUILD_NUMBER}#${GIT_REVISION,length=8}(${GIT_BRANCH}''')
    }
  
    publishers {
        publishers {
        buildPipelineTrigger("${NAME}_release_${BRANCH}") {
            parameters {              	
              predefinedProp('BRANCH', '${BRANCH}')
              gitRevision(false)
            }
        }
    }
 
      mailer("${MAIL}", false, false)
    }
}

/***************************\
| RELEASE
\***************************/
job("${NAME}_release_${BRANCH}") {
    description("Releases the ${BRANCH} branch.")
    logRotator(-1, 50, -1, -1)
  	label('jenkinsubuntu')
    scm {
        git {
            remote {
                url("${REPO}")
            }
            branch("${BRANCH}")
            configure {
                node ->
                    node / 'extensions' << 'hudson.plugins.git.extensions.impl.WipeWorkspace' {}
            }
        }

    }
  	properties {
        environmentVariables {
          	keepSystemVariables(true)
          	keepBuildVariables(true)
          	env('REPO', "${REPO}")
            env('BRANCH', "${BRANCH}")
        }
  	}

    steps {
      	shell('''
git checkout ${BRANCH}
mvn release:prepare release:perform -B -Darguments="-DskipITs"
''')
      	shell("git push ${RELEASE_TARGET} ${BRANCH}")
        shell("git push --tags ${RELEASE_TARGET}")
    }
  
    wrappers {
        buildName('''#${BUILD_NUMBER}#${GIT_REVISION,length=8}(${GIT_BRANCH}''')
    }
  
    publishers {
      mailer("${MAIL}", false, false)
    }
}
