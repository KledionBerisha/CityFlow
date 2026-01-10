# CityFlow - Deployment Guide (Grafana + Kubernetes)

This guide covers deploying CityFlow with Grafana monitoring and Kubernetes orchestration.

## 🚀 Quick Start

### Option 1: Docker Compose (Development)

```powershell
# Start all services including monitoring
docker-compose up -d

# Start monitoring stack separately
docker-compose -f docker-compose.yml -f docker-compose.monitoring.yml up -d
```

**Access Points:**
- Frontend: http://localhost:5173
- API Gateway: http://localhost:8000
- Grafana: http://localhost:3001 (admin/admin)
- Prometheus: http://localhost:9091

### Option 2: Kubernetes with Helm (Production)

```bash
# Install Helm chart
helm install cityflow ./infrastructure/kubernetes/helm-charts/cityflow \
  --namespace cityflow \
  --create-namespace \
  --set image.tag=latest

# Upgrade deployment
helm upgrade cityflow ./infrastructure/kubernetes/helm-charts/cityflow \
  --namespace cityflow \
  --set image.tag=v1.0.0
```

---

## 📊 Grafana Monitoring

### Setup Grafana (Docker Compose)

1. **Start monitoring stack:**
   ```powershell
   docker-compose -f docker-compose.monitoring.yml up -d
   ```

2. **Or add to main compose:**
   ```powershell
   docker-compose up -d prometheus grafana
   ```

3. **Access Grafana:**
   - URL: http://localhost:3001
   - Username: `admin`
   - Password: `admin` (change in production!)

### Grafana Dashboards

**Pre-configured Dashboards:**
- `CityFlow - System Overview` - Main system metrics
  - Service health status
  - HTTP request rate
  - Response time (95th percentile)
  - Error rate
  - JVM memory usage
  - ML predictions rate
  - Kafka messages/sec

**Location:** `infrastructure/monitoring/grafana/dashboards/`

### Adding Custom Dashboards

1. Create JSON dashboard file in `infrastructure/monitoring/grafana/dashboards/`
2. Restart Grafana or wait for auto-reload (10s interval)
3. Dashboard will appear in Grafana UI

### Prometheus Configuration

**Prometheus Config:** `infrastructure/monitoring/prometheus/prometheus.yml`

**Scraping Targets:**
- API Gateway (Port 8000): `/actuator/prometheus`
- Route Service (Port 8081): `/actuator/prometheus`
- Bus Service (Port 8082): `/actuator/prometheus`
- Traffic Service (Port 8083): `/actuator/prometheus`
- Analytics Service (Port 8084): `/actuator/prometheus`
- ML API (Port 9090): `/metrics`

**Alert Rules:** `infrastructure/monitoring/prometheus/alerts.yml`

- Service Down alerts
- High Error Rate alerts
- High Response Time alerts
- ML API No Models alerts
- Memory Usage alerts

---

## ☸️ Kubernetes Deployment

### Prerequisites

- Kubernetes cluster (v1.24+)
- kubectl configured
- Helm 3.x installed
- Container images pushed to registry

### Deploy with Helm (Recommended)

#### 1. Install Helm Chart

```bash
cd infrastructure/kubernetes/helm-charts/cityflow

# Install with default values
helm install cityflow . \
  --namespace cityflow \
  --create-namespace

# Install with custom values
helm install cityflow . \
  --namespace cityflow \
  --create-namespace \
  --set image.tag=v1.0.0 \
  --set image.registry=ghcr.io \
  --set apiGateway.replicaCount=3 \
  --set monitoring.enabled=true
```

#### 2. Verify Deployment

```bash
# Check all pods
kubectl get pods -n cityflow

# Check services
kubectl get svc -n cityflow

# Check deployments
kubectl get deployments -n cityflow

# View logs
kubectl logs -n cityflow -l app=api-gateway --tail=100
```

#### 3. Access Services

```bash
# Port forwarding (development)
kubectl port-forward -n cityflow svc/cityflow-api-gateway 8000:8000
kubectl port-forward -n cityflow svc/cityflow-grafana 3001:3000
kubectl port-forward -n cityflow svc/cityflow-prometheus 9090:9090

# Or get LoadBalancer external IP
kubectl get svc -n cityflow
```

### Deploy with Raw Kubernetes Manifests

#### 1. Create Namespace

```bash
kubectl apply -f infrastructure/kubernetes/k8s-manifest/namespace.yaml
```

#### 2. Deploy Infrastructure Services

```bash
# PostgreSQL (use Helm chart or StatefulSet)
# MongoDB (use Helm chart or StatefulSet)
# Redis (use Helm chart or Deployment)
# Kafka (use Helm chart or StatefulSet)
```

#### 3. Deploy Monitoring

```bash
# Prometheus
kubectl apply -f infrastructure/kubernetes/k8s-manifest/configmap-prometheus.yaml
kubectl apply -f infrastructure/kubernetes/k8s-manifest/prometheus-deployment.yaml

# Grafana
kubectl apply -f infrastructure/kubernetes/k8s-manifest/grafana-deployment.yaml
```

#### 4. Deploy Application Services

```bash
# ML API
kubectl apply -f infrastructure/kubernetes/k8s-manifest/ml-configmap.yaml
kubectl apply -f infrastructure/kubernetes/k8s-manifest/ml-api-deployment.yaml

# API Gateway
kubectl apply -f infrastructure/kubernetes/k8s-manifest/api-gateway-deployment.yaml
```

### Auto-Scaling Configuration

**Horizontal Pod Autoscaler (HPA)** is enabled for API Gateway:

```yaml
minReplicas: 2
maxReplicas: 5
targetCPUUtilizationPercentage: 70
```

**Enable for other services:**

```bash
# Create HPA for ML API
kubectl autoscale deployment cityflow-ml-api \
  --cpu-percent=70 \
  --min=1 \
  --max=3 \
  -n cityflow
```

### Resource Management

**Default Resource Limits:**

| Service | Requests | Limits |
|---------|----------|--------|
| API Gateway | 512Mi RAM, 250m CPU | 1Gi RAM, 500m CPU |
| ML API | 1Gi RAM, 500m CPU | 2Gi RAM, 1000m CPU |
| Backend Services | 256Mi RAM, 250m CPU | 512Mi RAM, 500m CPU |
| Prometheus | 512Mi RAM, 250m CPU | 2Gi RAM, 1000m CPU |
| Grafana | 256Mi RAM, 100m CPU | 1Gi RAM, 500m CPU |

**Adjust in Helm values.yaml:**

```yaml
apiGateway:
  resources:
    requests:
      memory: "1Gi"
      cpu: "500m"
    limits:
      memory: "2Gi"
      cpu: "1000m"
```

---

## 🔧 Configuration

### Helm Values Customization

Edit `infrastructure/kubernetes/helm-charts/cityflow/values.yaml`:

```yaml
# Image configuration
image:
  registry: ghcr.io
  tag: latest

# Service replicas
apiGateway:
  replicaCount: 3
  autoscaling:
    enabled: true
    minReplicas: 2
    maxReplicas: 5

# Monitoring
monitoring:
  enabled: true
  prometheus:
    retention: "30d"
  grafana:
    adminPassword: "secure-password"
```

### Environment Variables

All services use environment variables from Kubernetes Secrets/ConfigMaps:

- **PostgreSQL credentials:** From `postgres-secrets` Secret
- **MongoDB URI:** From ConfigMap
- **Redis connection:** From ConfigMap
- **Kafka brokers:** From ConfigMap
- **Keycloak settings:** From ConfigMap

### Secrets Management

Create secrets for sensitive data:

```bash
# PostgreSQL
kubectl create secret generic postgres-secrets \
  --from-literal=username=kledionberisha \
  --from-literal=password=kledion123 \
  -n cityflow

# Grafana admin password
kubectl create secret generic grafana-secrets \
  --from-literal=admin-password=your-secure-password \
  -n cityflow
```

---

## 📈 Monitoring & Observability

### Prometheus Metrics

**Available Metrics:**

- **HTTP Metrics:** `http_server_requests_seconds_count`, `http_server_requests_seconds_bucket`
- **JVM Metrics:** `jvm_memory_used_bytes`, `jvm_memory_max_bytes`, `jvm_gc_*`
- **ML Metrics:** `predictions_total`, `prediction_duration_seconds`, `ml_api_models_loaded`
- **Kafka Metrics:** `kafka_producer_record_send_total`, `kafka_consumer_lag`

**Access Prometheus:**
- Docker Compose: http://localhost:9091
- Kubernetes: `kubectl port-forward -n cityflow svc/cityflow-prometheus 9091:9090`

### Grafana Dashboards

**Pre-built Dashboards:**

1. **CityFlow - System Overview**
   - Service health status
   - Request rates and response times
   - Error rates
   - Memory usage
   - ML predictions metrics

**Custom Queries Examples:**

```promql
# Request rate per service
rate(http_server_requests_seconds_count[5m])

# 95th percentile response time
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))

# Error rate
rate(http_server_requests_seconds_count{status=~"5.."}[5m])

# ML predictions rate
rate(predictions_total[5m])
```

### Alerting

**Prometheus Alert Rules:** `infrastructure/monitoring/prometheus/alerts.yml`

**Configured Alerts:**
- ⚠️ Service Down (critical)
- ⚠️ High Error Rate (warning)
- ⚠️ High Response Time (warning)
- ⚠️ ML API No Models (warning)
- ⚠️ High Memory Usage (warning)
- ⚠️ Kafka Consumer Lag (warning)
- ⚠️ Redis Connection Failure (critical)

**To configure Alertmanager:**

1. Deploy Alertmanager service
2. Update `prometheus.yml` with Alertmanager URL
3. Configure notification channels (Slack, email, PagerDuty)

---

## 🚀 Deployment Workflows

### Development Workflow

```powershell
# 1. Start all services locally
docker-compose up -d

# 2. Start monitoring
docker-compose up -d prometheus grafana

# 3. View logs
docker-compose logs -f api-gateway

# 4. Test locally
Invoke-WebRequest -Uri "http://localhost:8000/api/health"
```

### Production Deployment

```bash
# 1. Build and push images
docker build -t ghcr.io/cityflow/api-gateway:v1.0.0 ./backend/api-gateway
docker push ghcr.io/cityflow/api-gateway:v1.0.0

# 2. Update Helm chart
cd infrastructure/kubernetes/helm-charts/cityflow
helm upgrade cityflow . \
  --namespace cityflow \
  --set image.tag=v1.0.0 \
  --set image.registry=ghcr.io

# 3. Verify deployment
kubectl rollout status deployment/cityflow-api-gateway -n cityflow
```

---

## 🔍 Troubleshooting

### Kubernetes Deployment Issues

**Problem:** Deployment fails
```bash
# Check Helm chart syntax
helm lint ./infrastructure/kubernetes/helm-charts/cityflow

# Dry-run deployment
helm install cityflow ./infrastructure/kubernetes/helm-charts/cityflow \
  --namespace cityflow \
  --dry-run \
  --debug
```

### Grafana Issues

**Problem:** Grafana can't connect to Prometheus
```bash
# Check Prometheus service
kubectl get svc -n cityflow | grep prometheus

# Check Grafana datasource configuration
kubectl get configmap -n cityflow | grep grafana
kubectl describe configmap cityflow-grafana-datasources -n cityflow
```

**Problem:** Dashboards not loading
```bash
# Check dashboard files exist
ls -la infrastructure/monitoring/grafana/dashboards/

# Check Grafana logs
docker-compose logs grafana
# Or in K8s:
kubectl logs -n cityflow -l app=grafana
```

### Kubernetes Deployment Issues

**Problem:** Pods stuck in Pending
```bash
# Check pod status
kubectl describe pod <pod-name> -n cityflow

# Check events
kubectl get events -n cityflow --sort-by='.lastTimestamp'

# Check resource quotas
kubectl describe quota -n cityflow
```

**Problem:** Services not accessible
```bash
# Check service endpoints
kubectl get endpoints -n cityflow

# Check service selector matches pod labels
kubectl get svc cityflow-api-gateway -n cityflow -o yaml
kubectl get pods -n cityflow --show-labels | grep api-gateway
```

**Problem:** Prometheus not scraping metrics
```bash
# Check Prometheus targets
kubectl port-forward -n cityflow svc/cityflow-prometheus 9090:9090
# Visit http://localhost:9090/targets

# Verify pod annotations
kubectl get pods -n cityflow -o yaml | grep prometheus.io

# Check Prometheus config
kubectl get configmap cityflow-prometheus-config -n cityflow -o yaml
```

---

## 📝 Next Steps

1. **Configure Ingress** for external access (NGINX, Traefik)
2. **Enable TLS/HTTPS** with cert-manager
3. **Set up Network Policies** for service isolation
4. **Configure Backup** for persistent volumes
5. **Set up Log Aggregation** (ELK Stack, Loki)
6. **Configure Distributed Tracing** (Jaeger, OpenTelemetry)
7. **Enable Pod Disruption Budgets** for high availability
8. **Set up Resource Quotas** and LimitRanges
9. **Configure Alertmanager** for alerting
10. **Set up GitOps** with ArgoCD or Flux

---

## 📚 Additional Resources

- **Kubernetes Documentation:** https://kubernetes.io/docs/
- **Helm Documentation:** https://helm.sh/docs/
- **Prometheus Documentation:** https://prometheus.io/docs/
- **Grafana Documentation:** https://grafana.com/docs/
- **GitHub Actions Documentation:** https://docs.github.com/en/actions
