package net.praqma.hudson.scm;

import hudson.Extension;
import hudson.scm.SCM;
import javaposse.jobdsl.dsl.RequiresPlugin;
import javaposse.jobdsl.dsl.helpers.ScmContext;
import javaposse.jobdsl.plugin.ContextExtensionPoint;
import javaposse.jobdsl.plugin.DslExtensionMethod;

/*
job {
    scm {
        clearCaseUCM (String stream) {
            loadModules (String loadModules)                    // loadModules can be: 'ALL', 'MODIFIABLE'. Defaults to 'ALL'
            nameTemplate (String nameTemplate)                  // Defaults to '[project]_[date]_[time]'
            recommendBaseline (boolean recommend = true)        // Defaults to false
            makeTag (boolean makeTag = true)                    // Defaults to false
            setDescription (boolean setDescription = true)      // Defaults to true

            treatUnstableAsSuccessful (boolean success = true)  // Defaults to true
            forceDeliver (boolean forceDeliver = true)          // Defaults to false
            removeViewPrivateFiles (boolean remove = true)      // Defaults to true
            trimmedChangeset (boolean trim = true)              // Defaults to false
            ignoreUnmodifiableChanges (boolean ignore = true)   // Defaults to false
            buildProject (String project)
      
            pollingMode(String mode, String component) {                        //mode can be: 'CHILD','REBASE','SELF','SIBLING','SUBSCRIBE'. Defaults to 'CHILD'.
            pollingMode(String mode, String component, String promotionLevel){  //promotionLevel can be: 'ANY','INITIAL','BUILT','TESTED','RELEASED','REJECTED'. Defaults to lowest available.
                    //Applicable: All
                    promotionLevel (String promotionLevel)                      //promotionLevel can be: 'ANY','INITIAL','BUILT','TESTED','RELEASED','REJECTED'.

                    //Applicable: CHILD, REBASE, SIBLING
                    createBaseline (boolean create = true)      // Defaults to true

                    //Applicable: REBASE
                    excludeList (String excludeList)

                    //Applicable: SIBLING
                    hyperlinkPolling (String polling = true)    // Defaults to false

                    //Applicable: SUBSCRIBE, SELF, CHILD, SIBLING 
                    useNewest (boolean useNewest = true)        // Defaults to false

                    //Applicable: SUBSCRIBE
                    cascadePromotion (boolean cascade = true)   // Defaults to true
                    components {
                        component (String selection)
                    }
                    jobs {
                        job (String name, String ignores = null)
                    }
                }
            }
        }
    }
}

job('cc_gen') {
    scm {
        clearCaseUCM (/dev@\foo_bar/) {
            loadModules			'ALL'
            nameTemplate 		'[project]_[date]_[time]'
            recommendBaseline 	false
            makeTag 			false
            setDescription 		true
            treatUnstableAsSuccessful true
            forceDeliver 		false
            removeViewPrivateFiles true
            trimmedChangeset 	false
            ignoreUnmodifiableChanges false
            pollingMode('CHILD', /sys@\foo_bar/, 'TESTED'){
            	createBaseline true
            }
        }
    }
}
*/

@Extension(optional = true)
public class ClearCaseUcmJobDslExtension  extends ContextExtensionPoint {
    @RequiresPlugin(id = "clearcase-ucm-plugin", minimumVersion = "1.6.4")
    @DslExtensionMethod(context = ScmContext.class)
    public Object clearCaseUCM(String stream, Runnable closure){
        ClearCaseUcmJobDslContext context = new ClearCaseUcmJobDslContext();
        executeInContext(closure, context);

        SCM scm = new CCUCMScm(context.loadModules,
                false,
                context.pollingMode,
                stream,
                context.treatUnstableAsSuccessful,
                context.nameTemplate,
                context.forceDeliver,
                context.recommendBaseline,
                context.makeTag,
                context.setDescription,
                context.buildProject,
                context.removeViewPrivateFiles,
                context.trimmedChangeset,
                context.ignoreUnmodifiableChanges);
        return scm;
    }
}