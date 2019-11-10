#!/bin/bash
set -e

# import shared vars (non secret!)
source $SHARED_VARS_FILE && export $(cut -d= -f1 $SHARED_VARS_FILE)

sh $CI_SCRIPTS_PATH/print-environment.sh "build-core"

cd $PROJECT_ROOT_PATH/core

mvn -s $PROJECT_ROOT_PATH/.m2/settings.xml \
    --batch-mode \
    $MVN_STAGES \
    -Dgithub-deploy \
    -Dgithub-deploy-repo-owner=$GH_DEPLOY_REPO_OWNER \
    -Drevision=$REVISION \
    -Dskip.assemble-zip \
    $CORE_ADDITIONAL_OPTS

cd $PROJECT_ROOT_PATH

