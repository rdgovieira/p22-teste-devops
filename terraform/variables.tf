variable "aws_region" {
  description = "AWS region for the resources"
  type        = string
  default     = "us-east-2"
}

variable "image_tag" {
  description = "Docker image tag to deploy"
  type        = string
  default     = "latest"
}

