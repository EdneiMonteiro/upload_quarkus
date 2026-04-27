#!/usr/bin/env bash
# Disclaimer
# Notice: Any sample scripts, code, or commands comes with the following notification.
#
# This Sample Code is provided for the purpose of illustration only and is not intended to be used in a production
# environment. THIS SAMPLE CODE AND ANY RELATED INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER
# EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS FOR A
# PARTICULAR PURPOSE.
#
# We grant You a nonexclusive, royalty-free right to use and modify the Sample Code and to reproduce and distribute
# the object code form of the Sample Code, provided that You agree: (i) to not use Our name, logo, or trademarks to
# market Your software product in which the Sample Code is embedded; (ii) to include a valid copyright notice on Your
# software product in which the Sample Code is embedded; and (iii) to indemnify, hold harmless, and defend Us and Our
# suppliers from and against any claims or lawsuits, including attorneys' fees, that arise or result from the use or
# distribution of the Sample Code.
#
# Please note: None of the conditions outlined in the disclaimer above will supersede the terms and conditions
# contained within the Customers Support Services Description.
# deploy.sh — Full deploy: Terraform → ACR build → K8s apply
set -euo pipefail
cd "$(dirname "$0")/.."

echo "=== 1. Terraform Apply ==="
cd infra/terraform/environments/poc
terraform init -input=false
terraform apply -input=false -auto-approve
ACR_NAME=$(terraform output -raw acr_name)
ACR_SERVER=$(terraform output -raw acr_login_server)
AKS_NAME=$(terraform output -raw aks_name)
RG_NAME=$(terraform output -raw resource_group_name)
BLOB_URL=$(terraform output -raw blob_service_url)
QUEUE_URL=$(terraform output -raw queue_service_url)
TABLE_URL=$(terraform output -raw table_service_url)
WI_CLIENT_ID=$(terraform output -raw workload_identity_client_id)
STORAGE_CONN=$(terraform output -raw storage_connection_string)
cd ../../../..

echo "=== 2. Build & Push Images ==="
az acr build --registry "$ACR_NAME" --image backend:latest --file backend/Dockerfile . --platform linux/amd64
az acr build --registry "$ACR_NAME" --image web:latest --file web/Dockerfile . --platform linux/amd64
az acr build --registry "$ACR_NAME" --image worker:latest --file worker/Dockerfile . --platform linux/amd64

echo "=== 3. Get AKS Credentials ==="
az aks get-credentials --resource-group "$RG_NAME" --name "$AKS_NAME" --overwrite-existing

echo "=== 4. Apply K8s Manifests ==="
kubectl apply -f deploy/k8s/namespace.yaml

# Apply service account with actual client ID
sed "s|\${WORKLOAD_IDENTITY_CLIENT_ID}|${WI_CLIENT_ID}|g" deploy/k8s/service-account.yaml | kubectl apply -f -

# Apply configmap with actual URLs
sed -e "s|\${BLOB_SERVICE_URL}|${BLOB_URL}|g" \
    -e "s|\${QUEUE_SERVICE_URL}|${QUEUE_URL}|g" \
    -e "s|\${TABLE_SERVICE_URL}|${TABLE_URL}|g" \
    deploy/k8s/configmap.yaml | kubectl apply -f -

# Apply deployments with actual ACR server
sed "s|\${ACR_LOGIN_SERVER}|${ACR_SERVER}|g" deploy/k8s/backend.yaml | kubectl apply -f -

sed "s|\${ACR_LOGIN_SERVER}|${ACR_SERVER}|g" deploy/k8s/web.yaml | kubectl apply -f -

sed "s|\${ACR_LOGIN_SERVER}|${ACR_SERVER}|g" deploy/k8s/worker.yaml | kubectl apply -f -

# Apply KEDA ScaledObject with storage connection
sed "s|\${STORAGE_CONNECTION_STRING}|${STORAGE_CONN}|g" deploy/k8s/keda-scaledobject.yaml | kubectl apply -f -

echo "=== 5. Wait for Rollout ==="
kubectl -n upload rollout status deployment/backend --timeout=120s
kubectl -n upload rollout status deployment/web --timeout=120s

echo "=== 6. Get Web URL ==="
echo "Waiting for LoadBalancer IP..."
for i in $(seq 1 30); do
  IP=$(kubectl -n upload get svc web -o jsonpath='{.status.loadBalancer.ingress[0].ip}' 2>/dev/null || true)
  if [ -n "$IP" ]; then
    echo "Web URL: http://$IP"
    break
  fi
  sleep 5
done

echo "=== Deploy Complete ==="
