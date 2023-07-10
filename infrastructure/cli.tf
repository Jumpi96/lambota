resource "google_service_account" "cli" {
  account_id   = "lambota-cli"
  display_name = "Lambota CLI"
}
