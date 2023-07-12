terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 4.0"
    }
    google = {
      source  = "hashicorp/google"
      version = "~> 4.51"
    }
  }

  backend "s3" {
    bucket = "lambota-terraform-state-bucket"
    key    = "state/terraform_state.tfstate"
    region = "eu-central-1"
  }
}

provider "aws" {
  region = "eu-central-1"
}

provider "google" {
  credentials = file("lambota-gcp-09623d71c911.json")

  project = "lambota-gcp"
  region  = "europe-west3"
  zone    = "europe-west3-c" 
}
