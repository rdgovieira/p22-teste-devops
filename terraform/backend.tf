terraform {
  backend "s3" {
    bucket         = "p22-terraform-state"
    key            = "ecs-fargate/terraform.tfstate"
    region         = "us-east-2"
    use_lockfile   = "true"
    encrypt        = true
  }
}