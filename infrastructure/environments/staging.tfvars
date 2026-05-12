environment  = "staging"
aws_region   = "us-east-1"
docker_image = "REPLACE_WITH_ECR_URI:staging"

domain_name     = "staging.snakeforged.example.com"
route53_zone_id = "REPLACE_WITH_ZONE_ID"

task_cpu      = 256
task_memory   = 512
desired_count = 1
