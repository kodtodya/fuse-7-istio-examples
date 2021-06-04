# Istio Distributed Tracing - Fuse Demo

## Overview

The Fuse Istio Distributed Tracing booster demonstrates Istio’s Distributed Tracing via a (minimally) instrumented set of Apache Camel/Spring Boot applications:

* A _user-api-service_ that returns a response from training service with the help of so-called db operations.
* A _db-interaction-service_ that stores the data and helps us with some storage operations.

When you run this booster, it injects Istio envoy proxy sidecars containers and both applications become part of a "service mesh". Istio automatically gathers tracing information by following the flow of a request across the services.

### Deployment options

You can run this booster in the following modes:

* Single-node OpenShift cluster
* OpenShift Online at link:{launchURL}[]


### Prerequisites

* Java 8 JDK (or later)
* Maven 3.3.x (or later)
* OpenShift 3.10 (or later) / Kubernetes 1.10 (or later) cluster
* Istio 1.0.x (or later) installed on the cluster

## IstioCtl installation on local machine for dev purpose
*`not recommended for higher environments`*

**Step-1:** Download `istioctl` using below command 
    
    curl -L https://istio.io/downloadIstio | sh -

**Step-2:** Change directory to `istioctl` downloaded folder

    cd istio-1.10.0

**Step-3:** Export `ISTIO_HOME` path using below command

    export ISTIO_HOME=<Istio_Path_Without_bin_folder>
    export PATH=$ISTIO_HOME/bin:$PATH

**Step-4:** Validate `istioctl` local installation using below command

    istioctl version

## Istio Installation

If Istio is not installed and you have admin privileges, you can install it using the below instructions:

**Step-1:** login to openshift with kubeadmin/admin user
    
    oc login -u kubeadmin -p <your_kubeadmin_password> https://api.crc.testing:6443

**Step-2:** create new project to setup Istio Infrastructure

    oc new-project istio-system
  
**Step-3:** Add `anyuid` and `nonroot` groups to security policy of newly created projects/namespaces.

    oc adm policy add-scc-to-group anyuid system:serviceaccounts:istio-system
    oc adm policy add-scc-to-group nonroot system:serviceaccounts:istio-system
  
**Step-4:** Install istio in the istio-system project

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

**Step-5:** Apply Grafana, Prometheus, Jaeger, Kiali and Zipkin add-ons to Istio infrastructure

    oc apply -f https://raw.githubusercontent.com/istio/istio/release-1.10/samples/addons/grafana.yaml
    oc apply -f https://raw.githubusercontent.com/istio/istio/release-1.10/samples/addons/jaeger.yaml
    oc apply -f https://raw.githubusercontent.com/istio/istio/release-1.10/samples/addons/kiali.yaml
    oc apply -f https://raw.githubusercontent.com/istio/istio/release-1.10/samples/addons/prometheus.yaml
    oc apply -f https://raw.githubusercontent.com/istio/istio/release-1.10/samples/addons/extras/zipkin.yaml

**Step-6:** Expose Ingress gateway

    oc -n istio-system expose svc/istio-ingressgateway --port=http2

**Step-7:** Apply network configuration interface to istio infrastructure

    oc -n istio-system create -f <path_to_copied_project_from_git>/greeting-service-istio-example/istio/deploy/istio-cni.yaml

## Deploying micro-services on a single-node OpenShift cluster

A single-node OpenShift cluster provides you with access to a cloud environment that is similar to a production environment.

If you have a single-node OpenShift cluster, such as Minishift/CodeReady Containers or the Red Hat Container Development Kit, link:http://appdev.openshift.io/docs/minishift-installation.html[installed and running], you can deploy your booster there.

For more details about running this booster on a single-node OpenShift cluster, CI/CD deployments, as well as the rest of the runtime, see the link:http://appdev.openshift.io/docs/spring-boot-runtime.html[Spring Boot Runtime Guide].

To deploy this booster to a running single-node OpenShift cluster:

**Step-1:** Download the booster project and extract the archive on your local file system

**Step-2:** Login to openshfit

    oc login -u kubeadmin -p <your_kubeadmin_password> https://api.crc.testing:6443

**Step-3:** Create new project/namespace for applications

    oc new-project fuse-test

**Step-4:** Add `anyuid` and `nonroot` groups to security policy of newly created projects/namespaces.

    oc adm policy add-scc-to-group anyuid system:serviceaccounts:fuse-test
    oc adm policy add-scc-to-group nonroot system:serviceaccounts:fuse-test

**Step-5:** Apply network configuration interface to istio infrastructure

    oc -n fuse-test create -f <path_to_copied_project_from_git>/greeting-service-istio-example/istio/deploy/istio-cni.yaml

**Step-5:** Change directory to `db-interaction-service`

    cd /home/kodtodya/_my-space/_system/_codeBase/istio-examples/training-service-istio-example/db-interaction-service

**Step-6:** Deploy `db-interaction-service` to `fuse-test`

    mvn clean package oc:deploy -P fuse-test

**Step-7:** Change directory to `user-api-service`

    cd /home/kodtodya/_my-space/_system/_codeBase/istio-examples/training-service-istio-example/user-api-service

**Step-8:** Deploy `user-api-service` to `fuse-test`

    mvn clean package oc:deploy -P fuse-test

**Step-9:** Inject `istio-proxy`/`sidecar` to `fuse-test`

    oc label namespace fuse-test istio-injection=enabled

**Step-10:** Increase the number of pods for `db-interaction-service` and `user-api-service`

    oc scale dc/db-interaction-service --replicas=0
    oc scale dc/user-api-service --replicas=0

    oc scale dc/db-interaction-service --replicas=2
    oc scale dc/user-api-service --replicas=2

## Observability

### ** Prometheus Metrics

**Step-1:** Check the number of services available for `Prometheus` in `istio-system` using below command:

    oc -n istio-system get svc prometheus

**Step-2:** Open the `Prometheus` dashboard using below command:

    istioctl dashboard prometheus

OR

**Step-1:** Expose the `prometheus` service through route

    oc expose svc/prometheus -n istio-system

### ** Grafana Metrics

**Step-1:** Check the number of services available for `Grafana` in `istio-system` using below command:

    oc -n istio-system get svc grafana

**Step-2:** Open the `Grafana` dashboard using below command:

    istioctl dashboard grafana

OR

**Step-1:** Expose the `Grafana` service through route

    oc expose svc/grafana -n istio-system

### ** Zipkin Distributed Tracing

**Step-1:** Check the number of services available for `Zipkin` in `istio-system` using below command:

    oc -n istio-system get svc zipkin

**Step-2:** Open the `Zipkin` dashboard using below command:

    istioctl dashboard zipkin

OR

**Step-1:** Expose the `Zipkin` service through route

    oc expose svc/zipkin -n istio-system

### ** Jaeger Distributed Tracing

**Step-1:** Check the number of services available for `Jaeger` in `istio-system` using below command:

    oc -n istio-system get svc jaeger-collector

**Step-2:** Open the `Jaeger` dashboard using below command:

    istioctl dashboard jaeger

OR

**Step-1:** Create `Jaeger` service

    oc create -f <path_to_copied_project_from_git>/istio/deploy/jaeger-svc.yaml -n istio-system

**Step-2:** Expose the `Jaeger` service through route

    oc expose svc/jaeger -n istio-system

### ** Visualize Service Mesh with Kiali

**Step-1:** Check the number of services available for `Kiali` in `istio-system` using below command:

    oc -n istio-system get svc kiali

**Step-2:** Open the `Kiali` dashboard using below command:

    istioctl dashboard kiali

OR

**Step-1:** Create `Kiali` service

    oc expose svc/kiali -n istio-system

## Run your services

open is browser ->  http://user-api-service-fuse-test.apps-crc.testing/