output "app_image" {
  description = "Imagem ECR deployada"
  value       = "${var.ecr_repository}:${var.image_tag}"
}

output "service_name" {
  description = "Service LoadBalancer da aplicacao"
  value       = kubernetes_service.app_service.metadata[0].name
}

output "service_hostname" {
  description = "Hostname do Load Balancer atribuido ao Service, quando provisionado"
  value       = try(kubernetes_service.app_service.status[0].load_balancer[0].ingress[0].hostname, null)
}

output "namespace" {
  value = "siase"
}
