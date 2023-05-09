resource "aws_iam_user" "cli" {
  name = "lambota-cli"
}

resource "aws_iam_policy" "cli_s3_put" {
  name        = "lambota-cli-s3-put"
  path        = "/"
  policy      = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": [
        "s3:PutObject*",
        "s3:GetObject",
        "s3:ListBucket",
        "s3:DeleteObject"
      ],
      "Resource": [
        "${aws_s3_bucket.polly_transcribe_bucket.arn}",
        "${aws_s3_bucket.polly_transcribe_bucket.arn}/*"
      ],
      "Effect": "Allow"
    }
  ]
}
EOF
}

resource "aws_iam_user_policy_attachment" "cli_s3_put" {
  user       = aws_iam_user.cli.name
  policy_arn = aws_iam_policy.cli_s3_put.arn
}
