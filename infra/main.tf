terraform {
  required_version = ">= 1.7.0"
  backend "s3" {
    bucket       = "siase-terraform-state"
    key          = "siase-app/terraform.tfstate"
    region       = "us-east-1"
    use_lockfile = true
    encrypt      = true
  }
  required_providers {
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "2.37.1"
    }
  }
}

provider "kubernetes" {
  config_path = var.kubeconfig_path
}

locals {
  app_image = "${var.ecr_repository}:${var.image_tag}"
  namespace = "siase"
}

resource "kubernetes_config_map" "siase_config" {
  metadata {
    name      = "siase-config"
    namespace = local.namespace
  }
  data = {
    DB_HOST                = var.db_host
    DB_PORT                = var.db_port
    DB_NAME                = var.db_name
    SERVER_PORT            = "8080"
    MANAGEMENT_SERVER_PORT = "8081"
    JWT_EXPIRATION_MS      = "3600000"
    JWT_ISSUER             = var.jwt_issuer
    CORS_ALLOWED_ORIGINS   = var.cors_allowed_origins
    CLIENTE_PORTAL_URL     = var.cliente_portal_url
  }
}

resource "kubernetes_secret" "ecr_pull_secret" {
  metadata {
    name      = "ecr-pull-secret"
    namespace = local.namespace
  }
  type = "kubernetes.io/dockerconfigjson"
  data = {
    ".dockerconfigjson" = var.ecr_dockerconfigjson
  }
}

resource "kubernetes_secret" "siase_secret" {
  metadata {
    name      = "siase-secret"
    namespace = local.namespace
  }
  data = {
    DB_USER           = var.db_user
    DB_PASSWORD       = var.db_password
    JWT_SECRET        = var.jwt_secret
    WEBHOOK_TOKEN     = var.webhook_token
    MECANICO_PASSWORD = var.mecanico_password
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
    selector { match_labels = { app = "siase-app" } }
    template {
      metadata { labels = { app = "siase-app" } }
      spec {
        image_pull_secrets {
          name = kubernetes_secret.ecr_pull_secret.metadata[0].name
        }
        container {
          name  = "siase-app"
          image = local.app_image
          port {
            name           = "http"
            container_port = 8080
          }
          port {
            name           = "management"
            container_port = 8081
          }
          env_from {
            config_map_ref { name = kubernetes_config_map.siase_config.metadata[0].name }
          }
          env_from {
            secret_ref { name = kubernetes_secret.siase_secret.metadata[0].name }
          }
          resources {
            requests = { memory = "512Mi", cpu = "250m" }
            limits   = { memory = "768Mi", cpu = "500m" }
          }
          readiness_probe {
            http_get {
              path = "/actuator/health/readiness"
              port = 8081
            }
            initial_delay_seconds = 60
            period_seconds        = 15
          }
          liveness_probe {
            http_get {
              path = "/actuator/health/liveness"
              port = 8081
            }
            initial_delay_seconds = 90
            period_seconds        = 30
          }
        }
      }
    }
  }
}

resource "kubernetes_service" "app_service" {
  metadata {
    name      = "app-service"
    namespace = local.namespace
    annotations = {
      "service.beta.kubernetes.io/aws-load-balancer-scheme" = "internet-facing"
    }
  }
  spec {
    selector = { app = "siase-app" }
    port {
      name        = "http"
      port        = 80
      target_port = 8080
    }
    type = "LoadBalancer"
  }
}

resource "kubernetes_service" "app_metrics" {
  metadata {
    name      = "app-metrics"
    namespace = local.namespace
    labels = {
      "siase-metrics" = "true"
    }
  }

  spec {
    selector = { app = "siase-app" }
    port {
      name        = "management"
      port        = 8081
      target_port = 8081
    }
    type = "ClusterIP"
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
        select_policy                = "Max"
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
