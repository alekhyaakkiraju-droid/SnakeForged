# terraform-aws-cloudwatch-log-group

Terraform module to create and manage an AWS CloudWatch Log Group with optional metric filters and subscription filters.

## Features

- Creates a CloudWatch Log Group with configurable retention and optional KMS encryption
- Supports multiple CloudWatch metric filters
- Supports up to 2 subscription filters (AWS limit)
- Consistent tagging via `tags` variable

## Usage

hcl
module "log_group" {
  source = "./modules/cloudwatch-log-group"

  name              = "/app/my-service"
  retention_in_days = 90
  kms_key_id        = "arn:aws:kms:us-east-1:123456789012:key/mrk-abc123"

  metric_filters = [
    {
      name    = "error-count"
      pattern = "ERROR"
      metric_transformation = {
        name      = "ErrorCount"
        namespace = "MyApp"
        value     = "1"
      }
    }
  ]

  subscription_filters = [
    {
      name            = "forward-to-lambda"
      filter_pattern  = ""
      destination_arn = "arn:aws:lambda:us-east-1:123456789012:function:my-function"
    }
  ]

  tags = {
    Environment = "production"
    Team        = "platform"
  }
}


## Inputs

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|----------|
| `name` | Name of the CloudWatch Log Group | `string` | n/a | yes |
| `retention_in_days` | Days to retain log events (0 = never expire) | `number` | `30` | no |
| `kms_key_id` | ARN of KMS key for encryption | `string` | `null` | no |
| `skip_destroy` | Skip deletion of log group on destroy | `bool` | `false` | no |
| `metric_filters` | List of metric filter configurations | `list(object)` | `[]` | no |
| `subscription_filters` | List of subscription filter configurations (max 2) | `list(object)` | `[]` | no |
| `tags` | Map of tags to assign to resources | `map(string)` | `{}` | no |

## Outputs

| Name | Description |
|------|-------------|
| `log_group_name` | Name of the CloudWatch Log Group |
| `log_group_arn` | ARN of the CloudWatch Log Group |
| `log_group_retention_in_days` | Configured retention period in days |
| `metric_filter_ids` | Map of metric filter names to IDs |
| `subscription_filter_names` | List of subscription filter names |

## Requirements

| Name | Version |
|------|--------|
| terraform | >= 1.3.0 |
| aws | >= 4.0.0 |
