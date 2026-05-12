terraform {
  backend "s3" {
    # Bucket and key are environment-specific — pass via -backend-config or
    # a backend config file (e.g. environments/dev.s3.tfbackend).
    #
    # Example init for dev:
    #   terraform init \
    #     -backend-config="bucket=snakeforged-tfstate-<account-id>" \
    #     -backend-config="key=snakeforged/dev/terraform.tfstate" \
    #     -backend-config="region=us-east-1" \
    #     -backend-config="dynamodb_table=snakeforged-tfstate-lock"
    #
    # The S3 bucket and DynamoDB lock table must be pre-created (bootstrap step).
    # See docs/terraform-bootstrap.md for the one-time setup commands.

    encrypt        = true
    use_lockfile   = false # DynamoDB lock table used instead for S3 backend
  }
}
