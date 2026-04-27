#!/bin/bash
az login
az aks get-credentials -n K8-DEV-CS01-CL02 -g RG-DEV-CS01-CL02 --overwrite-existing
kubectl config use-context K8-DEV-CS01-CL02
kubectl get ns | grep ns-dev-amp-01


kubectl get pods -n ns-dev-amp-01

k9s -n ns-dev-amp-01
#
#
#service-cp-caseadmin-case-urn-mapper-springboot-app-76867db2rh9   2/2     Running   3 (10h ago)       2d12h
#service-cp-crime-hearing-case-event-subscription-springbookgb5l   2/2     Running   106 (6h49m ago)   4d13h
#service-cp-crime-prosecution-case-details-springboot-app-54qdp8   2/2     Running   8 (8h ago)        3d18h
#service-cp-crime-scheduleandlist-courtschedule-springboot-866vf   2/2     Running   6 (44m ago)       2d15h
#service-cp-refdata-courthearing-courthouses-springboot-app6c2tc   2/2     Running   9 (9h ago)        4d12h


# kubectl logs -n ns-dev-amp-01 service-cp-crime-hearing-case-event-subscription-springbookgb5l