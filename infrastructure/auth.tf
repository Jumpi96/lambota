resource "aws_iam_role" "auth_lambda_role" {
  name               = "iam_role_auth_lambda_function"
  assume_role_policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": "sts:AssumeRole",
      "Principal": {
        "Service": "lambda.amazonaws.com"
      },
      "Effect": "Allow",
      "Sid": ""
    }
  ]
}
EOF
}

resource "aws_iam_policy" "auth_lambda" {
  name        = "iam_policy_auth_lambda_function"
  path        = "/"
  description = "IAM policy for the auth lambda"
  policy      = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": [
        "logs:CreateLogGroup",
        "logs:CreateLogStream",
        "logs:PutLogEvents"
      ],
      "Resource": "arn:aws:logs:*:*:*",
      "Effect": "Allow"
    }
  ]
}
EOF
}

resource "aws_iam_role_policy_attachment" "auth_policy_attach" {
  role       = aws_iam_role.auth_lambda_role.name
  policy_arn = aws_iam_policy.auth_lambda.arn
}

data "archive_file" "default_auth" {
  type        = "zip"
  source_dir  = "${path.module}/auth/"
  output_path = "${path.module}/zip/auth.zip"
}

resource "aws_lambda_function" "auth" {
  filename         = "${path.module}/zip/auth.zip"
  function_name    = "authLambda"
  role             = aws_iam_role.auth_lambda_role.arn
  handler          = "index.lambda_handler"
  source_code_hash = data.archive_file.default_auth.output_base64sha256
  runtime          = "python3.8"
  timeout          = "60" 

  environment {
    variables = {
        BUCKET_NAME = aws_s3_bucket.polly_transcribe_bucket.bucket
    }
  }
}

resource "aws_lambda_function_url" "auth" {
  function_name      = aws_lambda_function.auth.function_name
  authorization_type = "NONE"

  cors {
    allow_credentials = true
    allow_origins     = ["OPTIONS,POST"]
    allow_methods     = ["*"]
    allow_headers     = ["date", "keep-alive"]
    expose_headers    = ["keep-alive", "date"]
    max_age           = 86400
  }
}
