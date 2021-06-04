#!/bin/bash
#
# Deploy services to istio-system
# Assumes you are oc-login'd and istio is installed and istioctl available at $ISTIO_HOME
#
DEPLOYMENT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )/.." && pwd -P)"

# name of project in which we are working
PROJECT=istio-system

oc delete project ${PROJECT}
sleep 5
oc new-project ${PROJECT} || true

oc adm policy add-scc-to-group anyuid system:serviceaccounts:${PROJECT}
oc adm policy add-scc-to-group nonroot system:serviceaccounts:${PROJECT}

istioctl install --set profile=demo \
--set values.meshConfig.enableAutoMtls=true \
--set namespace=istio-system \
--set values.gateways.istio-egressgateway.enabled=true \
--set values.gateways.istio-ingressgateway.enabled=true \
--set values.global.meshID=mesh-default \
--set values.global.multiCluster.clusterName=cluster-default \
--set values.global.network=network-default \
--set components.cni.enabled=true \
--set components.cni.namespace=kube-system \
--set values.cni.cniBinDir=/var/lib/cni/bin \
--set values.cni.cniConfDir=/etc/cni/multus/net.d \
--set values.cni.chained=false \
--set values.cni.cniConfFileName=istio-cni.conf \
--set values.cni.repair.enabled=true \
--set values.sidecarInjectorWebhook.injectedAnnotations.k8s\.v1\.cni\.cncf\.io/networks=istio-cni

oc apply -f https://raw.githubusercontent.com/istio/istio/release-1.10/samples/addons/grafana.yaml
oc apply -f https://raw.githubusercontent.com/istio/istio/release-1.10/samples/addons/jaeger.yaml
oc apply -f https://raw.githubusercontent.com/istio/istio/release-1.10/samples/addons/kiali.yaml
oc apply -f https://raw.githubusercontent.com/istio/istio/release-1.10/samples/addons/prometheus.yaml
oc apply -f https://raw.githubusercontent.com/istio/istio/release-1.10/samples/addons/extras/zipkin.yaml

oc -n ${PROJECT} expose svc/istio-ingressgateway --port=http2

oc -n ${PROJECT} create -f ${DEPLOYMENT_DIR}/istio/deploy/istio-cni.yaml

#cd ${DEPLOYMENT_DIR}/name-service
#mvn clean oc:deploy -P ${PROJECT}

#cd ${DEPLOYMENT_DIR}/greetings-service
#mvn clean oc:deploy -P ${PROJECT}
