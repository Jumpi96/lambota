resource "google_storage_bucket" "prompts_bucket" {
  name          = "lambota-audio-prompts"
  location      = "EU"
  force_destroy = true
}

data "google_iam_policy" "admin" {
  binding {
    role = "roles/storage.objectAdmin"
    members = [
      "serviceAccount:${google_service_account.cli.email}"
    ] 
  }
}

resource "google_storage_bucket_iam_policy" "admin_prompts" {
  bucket = "${google_storage_bucket.prompts_bucket.name}"
  policy_data = "${data.google_iam_policy.admin.policy_data}"
}

resource "google_storage_bucket" "responses_bucket" {
  name          = "lambota-audio-responses"
  location      = "EU"
  force_destroy = true
}

data "google_iam_policy" "responses_admin" {
  binding {
    role = "roles/storage.objectAdmin"
    members = [
      "serviceAccount:${google_service_account.cli.email}",
      "serviceAccount:lambota-gcp@appspot.gserviceaccount.com"
    ] 
  }
}

resource "google_storage_bucket_iam_policy" "admin_responses" {
  bucket = "${google_storage_bucket.responses_bucket.name}"
  policy_data = "${data.google_iam_policy.responses_admin.policy_data}"
}
