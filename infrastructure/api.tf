data "aws_caller_identity" "current" {}

resource "aws_iam_role" "api_lambda_role" {
  name               = "iam_role_api_lambda_function"
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

resource "aws_iam_policy" "api_lambda" {
  name        = "iam_policy_api_lambda_function"
  path        = "/"
  description = "IAM policy for the api lambda"
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

resource "aws_iam_role_policy_attachment" "api_policy_attach" {
  role       = aws_iam_role.api_lambda_role.name
  policy_arn = aws_iam_policy.api_lambda.arn
}

resource "aws_iam_role_policy_attachment" "api_polly_policy_attach" {
  role       = aws_iam_role.api_lambda_role.name
  policy_arn = aws_iam_policy.polly_transcribe_policy.arn
}

data "archive_file" "default_api" {
  type        = "zip"
  source_dir  = "${path.module}/api/"
  output_path = "${path.module}/zip/api.zip"
}

resource "aws_lambda_function" "lambdafunc" {
  filename         = "${path.module}/zip/api.zip"
  function_name    = "apiLambda"
  role             = aws_iam_role.api_lambda_role.arn
  handler          = "index.lambda_handler"
  source_code_hash = data.archive_file.default_api.output_base64sha256
  runtime          = "python3.8"
  timeout          = "60" 

  environment {
    variables = {
        BUCKET_NAME = aws_s3_bucket.polly_transcribe_bucket.bucket
        OPENAI_API_KEY = var.openai_api_key
    }
  }
}

resource "aws_lambda_function_url" "lambdafunc" {
  function_name      = aws_lambda_function.lambdafunc.function_name
  authorization_type = "NONE"

  cors {
    allow_credentials = true
    allow_origins     = ["*"]
    allow_methods     = ["*"]
    allow_headers     = ["date", "keep-alive"]
    expose_headers    = ["keep-alive", "date"]
    max_age           = 86400
  }
}

resource "aws_s3_bucket_notification" "api_trigger" {
  bucket = aws_s3_bucket.polly_transcribe_bucket.id
  lambda_function {
    lambda_function_arn = aws_lambda_function.lambdafunc.arn
    events              = ["s3:ObjectCreated:*"]
    filter_prefix       = "prompts/"
  }
}
resource "aws_lambda_permission" "api_trigger" {
  statement_id  = "AllowS3Invoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.lambdafunc.function_name
  principal     = "s3.amazonaws.com"
  source_arn    = "arn:aws:s3:::${aws_s3_bucket.polly_transcribe_bucket.id}"
}