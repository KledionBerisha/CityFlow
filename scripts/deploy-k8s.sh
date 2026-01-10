#!/bin/bash
# CityFlow - Kubernetes Deployment Script

set -e

NAMESPACE="cityflow"
RELEASE_NAME="cityflow"
CHART_PATH="./infrastructure/kubernetes/helm-charts/cityflow"
IMAGE_REGISTRY="${IMAGE_REGISTRY:-ghcr.io}"
IMAGE_TAG="${IMAGE_TAG:-latest}"

echo "🚀 Deploying CityFlow to Kubernetes..."
echo "Namespace: $NAMESPACE"
echo "Release: $RELEASE_NAME"
echo "Image Registry: $IMAGE_REGISTRY"
echo "Image Tag: $IMAGE_TAG"

# Check prerequisites
command -v kubectl >/dev/null 2>&1 || { echo "❌ kubectl not found. Please install kubectl."; exit 1; }
command -v helm >/dev/null 2>&1 || { echo "❌ helm not found. Please install Helm 3.x."; exit 1; }

# Check Kubernetes connection
echo "📡 Checking Kubernetes connection..."
kubectl cluster-info || { echo "❌ Cannot connect to Kubernetes cluster."; exit 1; }

# Create namespace if it doesn't exist
echo "📦 Creating namespace $NAMESPACE..."
kubectl create namespace $NAMESPACE --dry-run=client -o yaml | kubectl apply -f -

# Install or upgrade Helm chart
echo "📥 Installing/Upgrading Helm chart..."
helm upgrade --install $RELEASE_NAME $CHART_PATH \
  --namespace $NAMESPACE \
  --set image.registry=$IMAGE_REGISTRY \
  --set image.tag=$IMAGE_TAG \
  --set monitoring.enabled=true \
  --wait \
  --timeout 5m

# Wait for deployments to be ready
echo "⏳ Waiting for deployments to be ready..."
kubectl wait --for=condition=available \
  --timeout=300s \
  deployment/$RELEASE_NAME-api-gateway \
  -n $NAMESPACE || true

kubectl wait --for=condition=available \
  --timeout=300s \
  deployment/$RELEASE_NAME-ml-api \
  -n $NAMESPACE || true

# Show status
echo "✅ Deployment complete!"
echo ""
echo "📊 Deployment Status:"
kubectl get pods -n $NAMESPACE
echo ""
echo "🌐 Services:"
kubectl get svc -n $NAMESPACE
echo ""
echo "🔍 Access Services:"
echo "  - API Gateway: kubectl port-forward -n $NAMESPACE svc/$RELEASE_NAME-api-gateway 8000:8000"
echo "  - Grafana: kubectl port-forward -n $NAMESPACE svc/$RELEASE_NAME-grafana 3001:3000"
echo "  - Prometheus: kubectl port-forward -n $NAMESPACE svc/$RELEASE_NAME-prometheus 9090:9090"
echo ""
echo "📈 View Logs:"
echo "  - API Gateway: kubectl logs -n $NAMESPACE -l app=api-gateway -f"
echo "  - ML API: kubectl logs -n $NAMESPACE -l app=ml-api -f"
