resource "aws_s3_bucket" "polly_transcribe_bucket" {
  bucket = "lambota-polly-transcribe"
}

resource "aws_s3_bucket_ownership_controls" "polly_transcribe_bucket" {
  bucket = aws_s3_bucket.polly_transcribe_bucket.id
  rule {
    object_ownership = "BucketOwnerPreferred"
  }
}

resource "aws_s3_bucket_acl" "polly_transcribe_bucket" {
  depends_on = [aws_s3_bucket_ownership_controls.polly_transcribe_bucket]

  bucket = aws_s3_bucket.polly_transcribe_bucket.id
  acl    = "private"
}

resource "aws_iam_policy" "polly_transcribe_policy" {
  name = "PollyTranscribePolicy"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = [
          "polly:SynthesizeSpeech",
          "transcribe:StartTranscriptionJob",
          "transcribe:GetTranscriptionJob"
        ]
        Effect   = "Allow"
        Resource = "*"
      },
      {
        Action = [
          "s3:GetObject",
          "s3:PutObject",
          "s3:ListBucket"
        ]
        Effect   = "Allow"
        Resource = [
          aws_s3_bucket.polly_transcribe_bucket.arn,
          "${aws_s3_bucket.polly_transcribe_bucket.arn}/*"
        ]
      }
    ]
  })
}
