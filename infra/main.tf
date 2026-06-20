terraform {
  required_version = ">= 1.7.0"
  required_providers {
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "2.37.1"
    }
  }
}

provider "kubernetes" {
  config_path    = var.kubeconfig_path
  config_context = "kind-siase"
}

locals {
  app_image = "ghcr.io/${var.github_user}/siase-app:${var.image_tag}"
  namespace = "siase"
}

resource "kubernetes_config_map" "siase_config" {
  metadata {
    name      = "siase-config"
    namespace = local.namespace
  }
  data = {
    DB_HOST              = "postgres-service"
    DB_PORT              = "5432"
    DB_NAME              = "siase_db"
    SERVER_PORT          = "8080"
    JWT_EXPIRATION_MS    = "3600000"
    CORS_ALLOWED_ORIGINS = "http://localhost:4200"
    CLIENTE_PORTAL_URL   = "http://localhost:4200"
  }
}

resource "kubernetes_secret" "ghcr_pull_secret" {
  metadata {
    name      = "ghcr-pull-secret"
    namespace = local.namespace
  }
  type = "kubernetes.io/dockerconfigjson"
  data = {
    ".dockerconfigjson" = jsonencode({
      auths = {
        "ghcr.io" = {
          username = "ediwaldoneto"
          password = var.ghcr_token
          auth     = base64encode("ediwaldoneto:${var.ghcr_token}")
        }
      }
    })
  }
}

resource "kubernetes_secret" "siase_secret" {
  metadata {
    name      = "siase-secret"
    namespace = local.namespace
  }
  data = {
    DB_USER           = "siase_user"
    DB_PASSWORD       = var.db_password
    JWT_SECRET        = var.jwt_secret
    WEBHOOK_TOKEN     = var.webhook_token
    MECANICO_PASSWORD = var.mecanico_password
  }
}

resource "kubernetes_persistent_volume_claim" "postgres_pvc" {
  metadata {
    name      = "postgres-pvc"
    namespace = local.namespace
  }
  wait_until_bound = false
  spec {
    access_modes = ["ReadWriteOnce"]
    resources {
      requests = { storage = "2Gi" }
    }
  }
}

resource "kubernetes_deployment" "postgres" {
  metadata {
    name      = "postgres"
    namespace = local.namespace
  }
  wait_for_rollout = false
  spec {
    replicas = 1
    selector {
      match_labels = { app = "postgres" }
    }
    template {
      metadata {
        labels = { app = "postgres" }
      }
      spec {
        container {
          name  = "postgres"
          image = "postgres:16-alpine"
          port {
            container_port = 5432
          }
          env {
            name = "POSTGRES_DB"
            value_from {
              config_map_key_ref {
                name = kubernetes_config_map.siase_config.metadata[0].name
                key  = "DB_NAME"
              }
            }
          }
          env {
            name = "POSTGRES_USER"
            value_from {
              secret_key_ref {
                name = kubernetes_secret.siase_secret.metadata[0].name
                key  = "DB_USER"
              }
            }
          }
          env {
            name = "POSTGRES_PASSWORD"
            value_from {
              secret_key_ref {
                name = kubernetes_secret.siase_secret.metadata[0].name
                key  = "DB_PASSWORD"
              }
            }
          }
          volume_mount {
            name       = "postgres-data"
            mount_path = "/var/lib/postgresql/data"
          }
          resources {
            requests = {
              memory = "256Mi"
              cpu    = "250m"
            }
            limits = {
              memory = "512Mi"
              cpu    = "500m"
            }
          }
          readiness_probe {
            exec {
              command = ["pg_isready", "-U", "siase_user", "-d", "siase_db"]
            }
            initial_delay_seconds = 10
            period_seconds        = 10
          }
        }
        volume {
          name = "postgres-data"
          persistent_volume_claim {
            claim_name = kubernetes_persistent_volume_claim.postgres_pvc.metadata[0].name
          }
        }
      }
    }
  }
}

resource "kubernetes_service" "postgres_service" {
  metadata {
    name      = "postgres-service"
    namespace = local.namespace
  }
  spec {
    selector = { app = "postgres" }
    port {
      port        = 5432
      target_port = 5432
    }
    type = "ClusterIP"
  }
}

resource "kubernetes_deployment" "siase_app" {
  metadata {
    name      = "siase-app"
    namespace = local.namespace
  }
  wait_for_rollout = false
  spec {
    replicas = var.app_replicas
    selector {
      match_labels = { app = "siase-app" }
    }
    template {
      metadata {
        labels = { app = "siase-app" }
      }
      spec {
        image_pull_secrets {
          name = kubernetes_secret.ghcr_pull_secret.metadata[0].name
        }
        container {
          name  = "siase-app"
          image = local.app_image
          port {
            container_port = 8080
          }
          env_from {
            config_map_ref {
              name = kubernetes_config_map.siase_config.metadata[0].name
            }
          }
          env_from {
            secret_ref {
              name = kubernetes_secret.siase_secret.metadata[0].name
            }
          }
          resources {
            requests = {
              memory = "512Mi"
              cpu    = "250m"
            }
            limits = {
              memory = "768Mi"
              cpu    = "500m"
            }
          }
          readiness_probe {
            http_get {
              path = "/api/actuator/health"
              port = 8080
            }
            initial_delay_seconds = 60
            period_seconds        = 15
            failure_threshold     = 5
          }
          liveness_probe {
            http_get {
              path = "/api/actuator/health"
              port = 8080
            }
            initial_delay_seconds = 90
            period_seconds        = 30
          }
        }
      }
    }
  }
  depends_on = [kubernetes_deployment.postgres]
}

resource "kubernetes_service" "app_service" {
  metadata {
    name      = "app-service"
    namespace = local.namespace
  }
  spec {
    selector = { app = "siase-app" }
    port {
      port        = 8080
      target_port = 8080
      node_port   = 30080
    }
    type = "NodePort"
  }
}

resource "kubernetes_horizontal_pod_autoscaler_v2" "siase_app_hpa" {
  metadata {
    name      = "siase-app-hpa"
    namespace = local.namespace
  }
  spec {
    scale_target_ref {
      api_version = "apps/v1"
      kind        = "Deployment"
      name        = kubernetes_deployment.siase_app.metadata[0].name
    }
    min_replicas = 2
    max_replicas = 4
    metric {
      type = "Resource"
      resource {
        name = "cpu"
        target {
          type                = "Utilization"
          average_utilization = 70
        }
      }
    }
    behavior {
      scale_up {
        stabilization_window_seconds = 60
        policy {
          type           = "Pods"
          value          = 1
          period_seconds = 60
        }
      }
      scale_down {
        stabilization_window_seconds = 300
        policy {
          type           = "Pods"
          value          = 1
          period_seconds = 60
        }
      }
    }
  }
}
