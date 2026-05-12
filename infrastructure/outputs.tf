output "alb_dns_name" {
  description = "DNS name of the Application Load Balancer (used as CloudFront origin)."
  value       = aws_lb.main.dns_name
}

output "cloudfront_domain_name" {
  description = "CloudFront distribution domain name."
  value       = aws_cloudfront_distribution.main.domain_name
}

output "cloudfront_distribution_id" {
  description = "CloudFront distribution ID (needed for cache invalidations)."
  value       = aws_cloudfront_distribution.main.id
}

output "application_url" {
  description = "Public HTTPS URL of the application."
  value       = "https://${var.domain_name}"
}

output "ecs_cluster_name" {
  description = "ECS cluster name."
  value       = aws_ecs_cluster.main.name
}

output "ecs_service_name" {
  description = "ECS service name."
  value       = aws_ecs_service.main.name
}

output "acm_certificate_arn" {
  description = "ARN of the ACM certificate used by CloudFront."
  value       = aws_acm_certificate.main.arn
}
