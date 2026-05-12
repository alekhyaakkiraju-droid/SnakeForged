# ── SSM Parameter Store ───────────────────────────────────────────────────────
# All secrets are stored as SecureString parameters and referenced by the ECS
# task definition so that plain-text values never appear in Terraform state or
# CloudWatch logs.
#
# IMPORTANT: The actual secret values are NOT managed by Terraform to avoid
# storing secrets in state.  Create them out-of-band before the first
# `terraform apply`:
#
#   aws ssm put-parameter \
#     --name /snakeforged/operator-token \
#     --type SecureString \
#     --value "$(openssl rand -hex 32)" \
#     --overwrite
#

# Data source — reads the existing parameter to verify it exists; Terraform
# will fail-fast during plan if the secret has not been pre-created.
data "aws_ssm_parameter" "operator_token" {
  name            = var.operator_token_ssm_path
  with_decryption = false # task execution role decrypts at container startup
}

# Alias resource used by the ECS task definition for IAM ARN references
resource "aws_ssm_parameter" "operator_token" {
  # lifecycle prevents Terraform from managing (or overwriting) the value
  lifecycle {
    ignore_changes = [value]
  }

  name  = var.operator_token_ssm_path
  type  = "SecureString"
  value = "REPLACE_BEFORE_APPLY" # placeholder — overwritten out-of-band

  tags = { Purpose = "Prometheus operator auth token" }
}
