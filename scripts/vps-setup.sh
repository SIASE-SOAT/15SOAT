#!/usr/bin/env bash
# Referência de setup inicial da VPS Hostinger (Ubuntu 24.04, 8 GB RAM).
# Execute manualmente via SSH como root. NÃO executar no CI/CD.

set -euo pipefail

echo "=== 1. Update system ==="
apt-get update && apt-get upgrade -y

echo "=== 2. Install Docker ==="
curl -fsSL https://get.docker.com | sh

echo "=== 3. Install Kind v0.23 ==="
curl -Lo /usr/local/bin/kind https://kind.sigs.k8s.io/dl/v0.23.0/kind-linux-amd64
chmod +x /usr/local/bin/kind

echo "=== 4. Install kubectl v1.30 ==="
curl -LO "https://dl.k8s.io/release/v1.30.0/bin/linux/amd64/kubectl"
chmod +x kubectl && mv kubectl /usr/local/bin/kubectl

echo "=== 5. Install Terraform ==="
apt-get install -y gnupg software-properties-common wget
wget -qO- https://apt.releases.hashicorp.com/gpg | gpg --dearmor -o /usr/share/keyrings/hashicorp-archive-keyring.gpg
echo "deb [signed-by=/usr/share/keyrings/hashicorp-archive-keyring.gpg] https://apt.releases.hashicorp.com $(lsb_release -cs) main" \
  | tee /etc/apt/sources.list.d/hashicorp.list
apt-get update && apt-get install terraform -y

echo "=== 6. Install Java 17 + Maven ==="
apt-get install -y openjdk-17-jdk maven

echo "=== 7. Create Kind cluster ==="
kind create cluster --name siase --config k8s/kind-cluster.yaml

echo "=== 8. Install metrics-server (required for HPA) ==="
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
kubectl patch deployment metrics-server -n kube-system \
  --type='json' \
  -p='[{"op":"add","path":"/spec/template/spec/containers/0/args/-","value":"--kubelet-insecure-tls"}]'
kubectl rollout status deployment/metrics-server -n kube-system --timeout=120s

echo "=== 9. Register self-hosted runner (manual step) ==="
echo "Acesse: https://github.com/ricardo-okuyama/15SOAT/settings/actions/runners/new"
echo "Selecione Linux x64, copie o token e execute os comandos gerados em /home/runner/actions-runner"

echo "=== Setup concluido ==="
