import boto3
import json
import os

def lambda_handler(event, context):
    s3_client = boto3.client('s3')
    bucket_name = os.getenv("BUCKET_NAME")
    body = json.loads(event.get("body"))
    object_key = body.get("key")

    presigned_post = s3_client.generate_presigned_post(
        Bucket=bucket_name,
        Key=object_key,\
        ExpiresIn=3600  # The URL will expire in 1 hour (3600 seconds)
    )

    return {
        'statusCode': 200,
        'headers': {'Content-Type': 'application/json', "Access-Control-Allow-Origin" : "*"},
        'body': json.dumps(presigned_post)
    }
