output "log_group_name" {
  description = "The name of the CloudWatch Log Group."
  value       = aws_cloudwatch_log_group.this.name
}

output "log_group_arn" {
  description = "The ARN of the CloudWatch Log Group."
  value       = aws_cloudwatch_log_group.this.arn
}

output "log_group_retention_in_days" {
  description = "The number of days log events are retained in the CloudWatch Log Group."
  value       = aws_cloudwatch_log_group.this.retention_in_days
}

output "metric_filter_ids" {
  description = "Map of metric filter names to their IDs."
  value       = { for k, v in aws_cloudwatch_log_metric_filter.this : k => v.id }
}

output "subscription_filter_names" {
  description = "List of subscription filter names created on the log group."
  value       = [for k, v in aws_cloudwatch_log_subscription_filter.this : v.name]
}
