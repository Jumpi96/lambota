import os
import time
import hashlib
import boto3
import requests
import tempfile
from configparser import ConfigParser
from datetime import datetime

config = ConfigParser()
config.read('conf.ini')

SIGNED_URL_API = "https://h7po4qxpq3acpdeirdqcduojju0kyedr.lambda-url.eu-central-1.on.aws/"
BUCKET_NAME = "lambota-polly-transcribe"

AWS_ACCESS_KEY_ID = config.get('AWS', 'ACCESS_KEY_ID')
AWS_SECRET_ACCESS_KEY = config.get('AWS', 'SECRET_ACCESS_KEY')

def record_audio(filename):
    print("Recording your message...")
    os.system(f"sox -d -t mp3 {filename}")
    print("Recording complete!")

def get_signed_url(filename: str):
    response = requests.post(
        SIGNED_URL_API,
        json={"key": filename}
    )
    signed_url_data = response.json()
    return signed_url_data

def upload_to_s3(client, file_path):
    try:
        client.upload_file(file_path, BUCKET_NAME, f"prompts/{file_path}")
        os.remove(file_path)
    except Exception as e:
        print(f"Error uploading file to S3: {e}")

def play_audio_from_s3_with_backoff(s3, key, max_retries=5, initial_delay=10):
    for attempt in range(max_retries + 1):
        try:
            response = s3.list_objects_v2(Bucket=BUCKET_NAME, Prefix="response/")
            mp3_file = [obj['Key'] for obj in response.get('Contents', []) if obj['Key'] == key]

            if mp3_file:
                print("Playing audio response...")
                with tempfile.NamedTemporaryFile(suffix='.mp3', delete=True) as temp_file:
                    s3.download_file(BUCKET_NAME, mp3_file[0], temp_file.name)
                    os.system(f"afplay {temp_file.name}")
                print("Finished playing audio response!")
                break
            else:
                print("No audio file found in the S3 bucket")
                if attempt < max_retries:
                    sleep_time = initial_delay * (2 ** attempt)
                    print(f"Retrying in {sleep_time} seconds...")
                    time.sleep(sleep_time)
                else:
                    print("Reached the maximum number of retries. Giving up.")
        except Exception as e:
            print(f"Error: {e}")
            if attempt < max_retries:
                sleep_time = initial_delay * (2 ** attempt)
                print(f"Retrying in {sleep_time} seconds...")
                time.sleep(sleep_time)
            else:
                print("Reached the maximum number of retries. Giving up.")
                break

def clean_prompt_file_from_s3(s3, key):
    try:
        s3.delete_object(Bucket=BUCKET_NAME, Key=f"prompts/{key}")
        print(f"Object '{key}' successfully deleted from bucket '{BUCKET_NAME}'")
    except Exception as e:
        print(f"Error deleting object '{key}' from bucket '{BUCKET_NAME}': {e}")

def main():
    print("Hi! I am your bot teacher. Let's start practicing. Record your message: (cut with CTRL+C)")
    s3 = boto3.client(
        's3',
        aws_access_key_id=AWS_ACCESS_KEY_ID,
        aws_secret_access_key=AWS_SECRET_ACCESS_KEY
    )

    while True:
        timestamp = datetime.now().strftime("%Y%m%d%H%M%S")
        short_hash = hashlib.md5(os.urandom(8)).hexdigest()[:6]
        audio_file = f"audio_{timestamp}_{short_hash}.mp3"
        record_audio(audio_file)

        upload_to_s3(s3, audio_file)
        play_audio_from_s3_with_backoff(s3, f"response/{audio_file}")
        clean_prompt_file_from_s3(s3, audio_file)

        print("Continue recording...  (cut with CTRL+C)")

if __name__ == "__main__":
    main()
