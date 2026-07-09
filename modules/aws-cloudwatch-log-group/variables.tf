variable "name" {
  description = "The name of the CloudWatch Log Group. Must start with a forward slash if using a path-based name."
  type        = string

  validation {
    condition     = length(var.name) >= 1 && length(var.name) <= 512
    error_message = "The log group name must be between 1 and 512 characters."
  }

  validation {
    condition     = can(regex("^[\.\-_/#A-Za-z0-9]+$", var.name))
    error_message = "The log group name may only contain alphanumeric characters, hyphens, underscores, forward slashes, dots, and hash symbols."
  }
}

variable "retention_in_days" {
  description = "Number of days to retain log events. Possible values: 1, 3, 5, 7, 14, 30, 60, 90, 120, 150, 180, 365, 400, 545, 731, 1096, 1827, 2192, 2557, 2922, 3288, 3653. Set to 0 for never expire."
  type        = number
  default     = 30

  validation {
    condition = contains(
      [0, 1, 3, 5, 7, 14, 30, 60, 90, 120, 150, 180, 365, 400, 545, 731, 1096, 1827, 2192, 2557, 2922, 3288, 3653],
      var.retention_in_days
    )
    error_message = "retention_in_days must be one of the allowed values: 0, 1, 3, 5, 7, 14, 30, 60, 90, 120, 150, 180, 365, 400, 545, 731, 1096, 1827, 2192, 2557, 2922, 3288, 3653."
  }
}

variable "kms_key_id" {
  description = "The ARN of the KMS Key to use for encrypting log data. If not provided, log data will be encrypted using the default CloudWatch Logs service key."
  type        = string
  default     = null

  validation {
    condition     = var.kms_key_id == null || can(regex("^arn:aws[a-z\\-]*:kms:", var.kms_key_id))
    error_message = "kms_key_id must be a valid KMS key ARN or null."
  }
}

variable "skip_destroy" {
  description = "Set to true if you do not wish the log group to be deleted at destroy time, and instead just remove the log group from the Terraform state."
  type        = bool
  default     = false
}

variable "metric_filters" {
  description = "List of CloudWatch metric filter configurations to create on the log group."
  type = list(object({
    name    = string
    pattern = string
    metric_transformation = object({
      name          = string
      namespace     = string
      value         = string
      default_value = optional(string)
      unit          = optional(string)
    })
  }))
  default = []

  validation {
    condition     = length(var.metric_filters) <= 100
    error_message = "A log group can have at most 100 metric filters."
  }
}

variable "subscription_filters" {
  description = "List of CloudWatch log subscription filter configurations to create on the log group."
  type = list(object({
    name            = string
    filter_pattern  = string
    destination_arn = string
    distribution    = optional(string)
    role_arn        = optional(string)
  }))
  default = []

  validation {
    condition     = length(var.subscription_filters) <= 2
    error_message = "A log group can have at most 2 subscription filters."
  }

  validation {
    condition = alltrue([
      for f in var.subscription_filters :
      f.distribution == null || contains(["Random", "ByLogStream"], f.distribution)
    ])
    error_message = "subscription_filters[*].distribution must be either 'Random' or 'ByLogStream' when specified."
  }
}

variable "tags" {
  description = "A map of tags to assign to all resources created by this module."
  type        = map(string)
  default     = {}
}
