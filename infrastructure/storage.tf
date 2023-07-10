resource "google_storage_bucket" "prompts_bucket" {
  name          = "lambota-prompts"
  location      = "EU"
  force_destroy = true

  uniform_bucket_level_access = true
}

resource "google_storage_bucket" "responses_bucket" {
  name          = "lambota-responses"
  location      = "EU"
  force_destroy = true

  uniform_bucket_level_access = true
}
