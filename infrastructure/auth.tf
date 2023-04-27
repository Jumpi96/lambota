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
    allow_origins     = ["*"]
    allow_methods     = ["*"]
    allow_headers     = ["date", "keep-alive"]
    expose_headers    = ["keep-alive", "date"]
    max_age           = 86400
  }
}


resource "aws_api_gateway_rest_api" "auth" {
  name        = "AuthFunctionAPI"
  description = "API for auth function"
}

resource "aws_api_gateway_resource" "auth" {
  rest_api_id = "${aws_api_gateway_rest_api.auth.id}"
  parent_id   = "${aws_api_gateway_rest_api.auth.root_resource_id}"
  path_part   = "{proxy+}"
}

resource "aws_api_gateway_method" "auth" {
  rest_api_id   = "${aws_api_gateway_rest_api.auth.id}"
  resource_id   = "${aws_api_gateway_resource.auth.id}"
  http_method   = "ANY"
  authorization = "NONE"
}

resource "aws_api_gateway_integration" "auth" {
  rest_api_id = "${aws_api_gateway_rest_api.auth.id}"
  resource_id = "${aws_api_gateway_method.auth.resource_id}"
  http_method = "${aws_api_gateway_method.auth.http_method}"

  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = "${aws_lambda_function.auth.invoke_arn}"
}

resource "aws_api_gateway_method" "auth_root" {
  rest_api_id   = "${aws_api_gateway_rest_api.auth.id}"
  resource_id   = "${aws_api_gateway_rest_api.auth.root_resource_id}"
  http_method   = "ANY"
  authorization = "NONE"
}

resource "aws_api_gateway_integration" "auth_root" {
  rest_api_id = "${aws_api_gateway_rest_api.auth.id}"
  resource_id = "${aws_api_gateway_method.auth_root.resource_id}"
  http_method = "${aws_api_gateway_method.auth_root.http_method}"

  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = "${aws_lambda_function.auth.invoke_arn}"
}

resource "aws_api_gateway_deployment" "example" {
  depends_on = [
    aws_api_gateway_integration.auth,
    aws_api_gateway_integration.auth_root,
  ]

  rest_api_id = "${aws_api_gateway_rest_api.auth.id}"
  stage_name  = "prod"
}

resource "aws_lambda_permission" "apigw" {
  statement_id  = "AllowAPIGatewayInvoke"
  action        = "lambda:InvokeFunction"
  function_name = "${aws_lambda_function.auth.function_name}"
  principal     = "apigateway.amazonaws.com"

  source_arn = "${aws_api_gateway_rest_api.auth.execution_arn}/*/*"
}
