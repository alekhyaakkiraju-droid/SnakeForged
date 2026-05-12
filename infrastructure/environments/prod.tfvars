environment  = "prod"
aws_region   = "us-east-1"
docker_image = "REPLACE_WITH_ECR_URI:latest"

domain_name     = "snakeforged.example.com"
route53_zone_id = "REPLACE_WITH_ZONE_ID"

# Slightly higher resources for production; 2 tasks for redundancy
task_cpu      = 512
task_memory   = 1024
desired_count = 2
