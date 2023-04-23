terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 4.0"
    }
  }

  backend "s3" {
    bucket = "lambota-terraform-state-bucket"
    key    = "state/terraform_state.tfstate"
    region = "eu-central-1"
  }
}

provider "aws" {}