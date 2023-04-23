import json
import boto3
import io
import os
import uuid
from urllib.request import urlopen
from urllib import request, parse

BUCKET_NAME = os.getenv("BUCKET_NAME")
OPENAI_API = 'https://api.openai.com/v1/completions'
API_KEY = os.getenv("OPENAI_API_KEY")

def transcribe_audio(key):
    # Create a Transcribe client
    transcribe_client = boto3.client('transcribe')

    # Start transcription job
    job_name = str(uuid.uuid4())
    s3_uri = f"s3://{BUCKET_NAME}/{key}"
    response = transcribe_client.start_transcription_job(
        TranscriptionJobName=job_name,
        Media={"MediaFileUri": s3_uri},
        MediaFormat="mp3",  # Replace with the appropriate format of your audio file
        LanguageCode="nl-NL",  # Replace with the appropriate language code
    )

    # Wait for the transcription job to complete
    while True:
        status = transcribe_client.get_transcription_job(TranscriptionJobName=response["TranscriptionJob"]["TranscriptionJobName"])
        if status["TranscriptionJob"]["TranscriptionJobStatus"] in ["COMPLETED", "FAILED"]:
            break

    # Get the transcript text
    transcript_file_uri = status["TranscriptionJob"]["Transcript"]["TranscriptFileUri"]
    with urlopen(transcript_file_uri) as response:
        data = response.read()
        json_data = json.loads(data)
    return json_data["results"]["transcripts"][0]["transcript"]
    
def ask_openai(input_prompt):
    data = {
        "model": "text-davinci-003",
        "prompt": f"You're a Dutch teacher and you are practicing speaking with a student. The student says: {input_prompt}",
        "max_tokens": 100
    }
    
    data = json.dumps(data).encode("utf-8")
    headers = {
        "Content-Type": "application/json",
        "Authorization": f"Bearer {API_KEY}",
    }
    
    req = request.Request(OPENAI_API, data=data, headers=headers, method="POST")
    response = request.urlopen(req)
    response_data = response.read().decode("utf-8")
    response_json = json.loads(response_data)
    print(response_json)
    return response_json["choices"][0]["text"]
    
def synthesize_speech_and_upload_to_s3(text, key):
    polly_client = boto3.client('polly')
    s3_client = boto3.client('s3')
    
    response = polly_client.synthesize_speech(
        OutputFormat='mp3',
        Text=text,
        VoiceId='Laura', # Dutch voice
        LanguageCode='nl-NL',
        Engine='neural'
    )
    
    # Use an in-memory file to store the synthesized audio
    with io.BytesIO() as audio_stream:
        audio_stream.write(response['AudioStream'].read())
        audio_stream.seek(0)
        
        # Upload the audio file to S3
        s3_client.upload_fileobj(audio_stream, BUCKET_NAME, f"response/{key}")
    

def lambda_handler(event, context):
    # Get the S3 bucket and object details from the event
    key = event["Records"][0]["s3"]["object"]["key"]
    filename = key.replace("prompts/", "")
    
    transcript_text = transcribe_audio(key)
    response = ask_openai(transcript_text)
    synthesize_speech_and_upload_to_s3(response, filename)

    return {
        'statusCode': 200,
        'body': json.dumps(f'Transcripted: {response}')
    }
