terraform {
  required_version = ">= 1.3.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = ">= 4.0.0"
    }
  }
}

resource "aws_cloudwatch_log_group" "this" {
  name              = var.name
  retention_in_days = var.retention_in_days
  kms_key_id        = var.kms_key_id
  skip_destroy      = var.skip_destroy

  tags = merge(
    var.tags,
    {
      Name = var.name
    }
  )
}

resource "aws_cloudwatch_log_metric_filter" "this" {
  for_each = { for f in var.metric_filters : f.name => f }

  name           = each.value.name
  pattern        = each.value.pattern
  log_group_name = aws_cloudwatch_log_group.this.name

  metric_transformation {
    name          = each.value.metric_transformation.name
    namespace     = each.value.metric_transformation.namespace
    value         = each.value.metric_transformation.value
    default_value = lookup(each.value.metric_transformation, "default_value", null)
    unit          = lookup(each.value.metric_transformation, "unit", null)
  }
}

resource "aws_cloudwatch_log_subscription_filter" "this" {
  for_each = { for f in var.subscription_filters : f.name => f }

  name            = each.value.name
  log_group_name  = aws_cloudwatch_log_group.this.name
  filter_pattern  = each.value.filter_pattern
  destination_arn = each.value.destination_arn
  distribution    = lookup(each.value, "distribution", null)
  role_arn        = lookup(each.value, "role_arn", null)
}
