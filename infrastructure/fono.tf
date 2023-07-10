resource "google_storage_bucket" "fono" {
  name     = "lambota-function"
  location      = "EU"
}

data "archive_file" "fono" {
  type        = "zip"
  source_dir  = "${path.module}/fono/"
  output_path = "${path.module}/zip/fono.zip"
}

resource "google_storage_bucket_object" "fono" {
  source       = data.archive_file.fono.output_path
  content_type = "application/zip"

  name         = "src-${data.archive_file.fono.output_md5}.zip"
  bucket       = google_storage_bucket.fono.name
}

resource "google_cloudfunctions_function" "fono" {
  name                  = "fono-lambota"
  runtime               = "python37"

  source_archive_bucket = google_storage_bucket.fono.name
  source_archive_object = google_storage_bucket_object.fono.name

  entry_point           = "cloud_function_handler"

  environment_variables = {
    OPENAI_API_KEY = var.openai_api_key
  }
    
  event_trigger {
    event_type = "google.storage.object.finalize"
    resource   = google_storage_bucket.prompts_bucket.name
  }
}