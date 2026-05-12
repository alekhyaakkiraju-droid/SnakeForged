terraform {
  required_version = ">= 1.5.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

# Primary region for all resources (ECS, ALB, SSM, etc.)
provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = "snakeforged"
      Environment = var.environment
      ManagedBy   = "terraform"
    }
  }
}

# ACM certificates for CloudFront must be in us-east-1 regardless of primary region
provider "aws" {
  alias  = "us_east_1"
  region = "us-east-1"

  default_tags {
    tags = {
      Project     = "snakeforged"
      Environment = var.environment
      ManagedBy   = "terraform"
    }
  }
}

# ── Data sources ──────────────────────────────────────────────────────────────

data "aws_caller_identity" "current" {}

data "aws_availability_zones" "available" {
  state = "available"
}

# ── Locals ────────────────────────────────────────────────────────────────────

locals {
  name_prefix = "snakeforged-${var.environment}"
  account_id  = data.aws_caller_identity.current.account_id

  # Two AZs for ALB requirement (ALB needs >= 2 subnets in different AZs)
  azs = slice(data.aws_availability_zones.available.names, 0, 2)

  container_name = "snakeforged-api"
  container_port = 8080
}
