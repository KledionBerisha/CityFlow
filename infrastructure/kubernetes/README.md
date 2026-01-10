# CityFlow - Kubernetes Deployment Guide

This directory contains Kubernetes manifests and Helm charts for deploying CityFlow to a Kubernetes cluster.

## Directory Structure

```
infrastructure/kubernetes/
├── k8s-manifest/          # Raw Kubernetes YAML manifests
│   ├── namespace.yaml
│   ├── configmap-prometheus.yaml
│   ├── prometheus-deployment.yaml
│   ├── grafana-deployment.yaml
│   ├── api-gateway-deployment.yaml
│   ├── ml-api-deployment.yaml
│   └── ml-configmap.yaml
└── helm-charts/           # Helm charts for easier deployment
    └── cityflow/
        ├── Chart.yaml
        ├── values.yaml
        └── templates/
            ├── _helpers.tpl
            ├── namespace.yaml
            ├── api-gateway-deployment.yaml
            ├── ml-api-deployment.yaml
            ├── monitoring.yaml
            └── ml-configmap.yaml
```

## Prerequisites

- Kubernetes cluster (v1.24+)
- kubectl configured to access your cluster
- Helm 3.x installed
- Docker images pushed to container registry

## Quick Start with Helm (Recommended)

### 1. Install Helm Chart

```bash
# Add Helm repo (if using remote registry)
helm repo add cityflow https://your-registry/helm-charts
helm repo update

# Install CityFlow
helm install cityflow ./infrastructure/kubernetes/helm-charts/cityflow \
  --namespace cityflow \
  --create-namespace \
  --set image.tag=latest \
  --set image.registry=ghcr.io
```

### 2. Upgrade Deployment

```bash
helm upgrade cityflow ./infrastructure/kubernetes/helm-charts/cityflow \
  --namespace cityflow \
  --set image.tag=v1.0.0
```

### 3. Uninstall

```bash
helm uninstall cityflow --namespace cityflow
```

## Manual Deployment with Kubernetes Manifests

### 1. Create Namespace

```bash
kubectl apply -f infrastructure/kubernetes/k8s-manifest/namespace.yaml
```

### 2. Deploy Monitoring Stack

```bash
# Prometheus
kubectl apply -f infrastructure/kubernetes/k8s-manifest/configmap-prometheus.yaml
kubectl apply -f infrastructure/kubernetes/k8s-manifest/prometheus-deployment.yaml

# Grafana
kubectl apply -f infrastructure/kubernetes/k8s-manifest/grafana-deployment.yaml
```

### 3. Deploy Services

```bash
# ML API
kubectl apply -f infrastructure/kubernetes/k8s-manifest/ml-configmap.yaml
kubectl apply -f infrastructure/kubernetes/k8s-manifest/ml-api-deployment.yaml

# API Gateway
kubectl apply -f infrastructure/kubernetes/k8s-manifest/api-gateway-deployment.yaml
```

### 4. Verify Deployment

```bash
# Check pods
kubectl get pods -n cityflow

# Check services
kubectl get svc -n cityflow

# Check logs
kubectl logs -n cityflow -l app=api-gateway --tail=100
```

## Accessing Services

### Port Forwarding (Development)

```bash
# API Gateway
kubectl port-forward -n cityflow svc/api-gateway-service 8000:8000

# Grafana
kubectl port-forward -n cityflow svc/grafana-service 3001:3000

# Prometheus
kubectl port-forward -n cityflow svc/prometheus-service 9090:9090

# ML API
kubectl port-forward -n cityflow svc/ml-api-service 8090:8090
```

### LoadBalancer (Production)

If using LoadBalancer service type:

```bash
# Get external IP
kubectl get svc -n cityflow

# Access services
# API Gateway: http://<EXTERNAL-IP>:8000
# Grafana: http://<EXTERNAL-IP>:3000
```

### Ingress (Recommended for Production)

Configure Ingress controller (NGINX, Traefik, etc.) for external access.

## Monitoring

### Prometheus

- URL: `http://localhost:9090` (via port-forward) or `http://prometheus-service.cityflow:9090`
- Metrics from all services: `/actuator/prometheus` (Spring Boot) or `/metrics` (ML API)

### Grafana

- URL: `http://localhost:3001` (via port-forward)
- Default credentials: `admin` / `admin`
- Prometheus datasource: Auto-configured
- Dashboards: Auto-loaded from `infrastructure/monitoring/grafana/dashboards/`

## Auto-Scaling

### Horizontal Pod Autoscaler (HPA)

API Gateway has HPA enabled by default:

```yaml
minReplicas: 2
maxReplicas: 5
targetCPUUtilizationPercentage: 70
```

To enable for other services, update Helm values or create HPA manifests.

## Resource Limits

Default resource requests/limits are set in Helm values:

- **API Gateway**: 512Mi-1Gi RAM, 250m-500m CPU
- **ML API**: 1Gi-2Gi RAM, 500m-1000m CPU
- **Backend Services**: 256Mi-512Mi RAM, 250m CPU

Adjust in `values.yaml` based on your cluster capacity.

## Persistent Storage

Services using persistent storage:
- Prometheus: 10Gi PVC
- Grafana: 5Gi PVC
- ML Models: 1Gi PVC

Ensure your cluster has a StorageClass configured.

## Secrets Management

Create secrets for sensitive data:

```bash
# PostgreSQL credentials
kubectl create secret generic postgres-secrets \
  --from-literal=username=kledionberisha \
  --from-literal=password=kledion123 \
  -n cityflow

# Grafana admin password
kubectl create secret generic grafana-secrets \
  --from-literal=admin-password=your-secure-password \
  -n cityflow
```

## Troubleshooting

### Pods not starting

```bash
# Check pod status
kubectl describe pod <pod-name> -n cityflow

# Check logs
kubectl logs <pod-name> -n cityflow

# Check events
kubectl get events -n cityflow --sort-by='.lastTimestamp'
```

### Services not accessible

```bash
# Check service endpoints
kubectl get endpoints -n cityflow

# Check service selector matches pod labels
kubectl get svc -n cityflow -o yaml
kubectl get pods -n cityflow --show-labels
```

### Prometheus not scraping metrics

```bash
# Check Prometheus targets
kubectl port-forward -n cityflow svc/prometheus-service 9090:9090
# Then visit http://localhost:9090/targets

# Verify pod annotations
kubectl get pods -n cityflow -o yaml | grep prometheus.io
```

## Production Considerations

1. **Use Ingress Controller** for external access instead of LoadBalancer
2. **Enable TLS/HTTPS** for all external services
3. **Configure Resource Quotas** and LimitRanges
4. **Set up Pod Disruption Budgets** for high availability
5. **Use StatefulSets** for stateful services (PostgreSQL, MongoDB)
6. **Enable Network Policies** for service isolation
7. **Configure Backup** for persistent volumes
8. **Set up Horizontal Pod Autoscaler** for all services
9. **Use Secrets** instead of ConfigMaps for sensitive data
10. **Enable Pod Security Policies** or Pod Security Standards

## CI/CD Integration

The GitHub Actions workflow (`.github/workflows/ci-cd.yml`) automatically:
- Builds Docker images
- Pushes to container registry
- Deploys to Kubernetes using Helm (on main branch)

To enable automatic deployment:
1. Set up Kubernetes cluster credentials as GitHub secrets
2. Update `KUBECONFIG` secret in GitHub repository settings
3. Push to `main` branch to trigger deployment
