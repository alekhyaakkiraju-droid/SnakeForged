# ── Environment ───────────────────────────────────────────────────────────────

variable "environment" {
  description = "Deployment environment name (dev | staging | prod)."
  type        = string

  validation {
    condition     = contains(["dev", "staging", "prod"], var.environment)
    error_message = "environment must be one of: dev, staging, prod."
  }
}

variable "aws_region" {
  description = "AWS region for all primary resources (e.g. us-east-1)."
  type        = string
  default     = "us-east-1"
}

# ── Docker image ──────────────────────────────────────────────────────────────

variable "docker_image" {
  description = "Full Docker image URI including tag (e.g. 123456789.dkr.ecr.us-east-1.amazonaws.com/snakeforged:latest)."
  type        = string
}

# ── Networking ────────────────────────────────────────────────────────────────

variable "vpc_cidr" {
  description = "CIDR block for the VPC."
  type        = string
  default     = "10.0.0.0/16"
}

variable "public_subnet_cidrs" {
  description = "CIDR blocks for the two public subnets (one per AZ)."
  type        = list(string)
  default     = ["10.0.1.0/24", "10.0.2.0/24"]
}

# ── ECS / compute ─────────────────────────────────────────────────────────────

variable "task_cpu" {
  description = "Fargate task CPU units (256 = 0.25 vCPU)."
  type        = number
  default     = 256
}

variable "task_memory" {
  description = "Fargate task memory in MiB."
  type        = number
  default     = 512
}

variable "desired_count" {
  description = "Desired number of ECS task instances."
  type        = number
  default     = 1
}

# ── TLS / DNS ─────────────────────────────────────────────────────────────────

variable "domain_name" {
  description = "Primary domain name for the application (e.g. snakeforged.example.com)."
  type        = string
}

variable "route53_zone_id" {
  description = "Route 53 hosted zone ID for the domain."
  type        = string
}

# ── Secrets ───────────────────────────────────────────────────────────────────

variable "operator_token_ssm_path" {
  description = "SSM Parameter Store path for the Prometheus operator token."
  type        = string
  default     = "/snakeforged/operator-token"
}
