#!/bin/bash
#
# Deploy services to istio-system
# Assumes you are oc-login'd and istio is installed and istioctl available at $ISTIO_HOME
#
DEPLOYMENT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )/.." && pwd -P)"

# name of project in which we are working
PROJECT=fuse-test

oc delete project ${PROJECT}
sleep 5
oc new-project ${PROJECT} || true

oc adm policy add-scc-to-group anyuid system:serviceaccounts:${PROJECT}
oc adm policy add-scc-to-group nonroot system:serviceaccounts:${PROJECT}

oc -n ${PROJECT} create -f ${DEPLOYMENT_DIR}/istio-scripts/etc/istio-cni.yaml

cd ${DEPLOYMENT_DIR}/db-interaction-service
mvn clean oc:deploy -P ${PROJECT}

cd ${DEPLOYMENT_DIR}/user-api-service
mvn clean oc:deploy -P ${PROJECT}

oc label namespace ${PROJECT} istio-injection=enabled
