environment  = "dev"
aws_region   = "us-east-1"
docker_image = "REPLACE_WITH_ECR_URI:dev"

domain_name     = "dev.snakeforged.example.com"
route53_zone_id = "REPLACE_WITH_ZONE_ID"

# Minimal resources for dev
task_cpu      = 256
task_memory   = 512
desired_count = 1
