import json
import os
import urllib.parse
import urllib.request
from google.cloud import storage
from google.cloud import texttospeech
from google.cloud import speech_v1p1beta1 as speech

OPENAI_API = 'https://api.openai.com/v1/completions'
RESPONSES_BUCKET = 'lambota-responses'
API_KEY = os.getenv("OPENAI_API_KEY")


def transcribe_audio(bucket, key):
    # Create a Speech-to-Text client
    client = speech.SpeechClient()

    # Configure the audio source
    audio_source_uri = f"gs://{bucket}/{key}"
    audio = speech.RecognitionAudio(uri=audio_source_uri)

    # Configure the transcription request
    config = speech.RecognitionConfig(
        encoding=speech.RecognitionConfig.AudioEncoding.MP3,
        language_code="nl-NL",
    )

    # Start the transcription
    operation = client.long_running_recognize(config=config, audio=audio)
    response = operation.result(timeout=90)

    # Get the transcript text
    transcript = ""
    for result in response.results:
        transcript += result.alternatives[0].transcript + " "
    return transcript.strip()


def ask_openai(input_prompt):
    data = {
        "model": "text-davinci-003",
        "prompt": f"You're a Dutch teacher and you are practicing speaking with a student. Your answer should only be what the Dutch teacher answers. The student says: {input_prompt}",
        "max_tokens": 100
    }

    data = json.dumps(data).encode("utf-8")
    headers = {
        "Content-Type": "application/json",
        "Authorization": f"Bearer {API_KEY}",
    }

    req = urllib.request.Request(OPENAI_API, data=data, headers=headers, method="POST")
    response = urllib.request.urlopen(req)
    response_data = response.read().decode("utf-8")
    response_json = json.loads(response_data)
    print(response_json)
    return response_json["choices"][0]["text"]


def synthesize_speech_and_upload_to_bucket(bucket, text, key):
    # Create a Text-to-Speech client
    client = texttospeech.TextToSpeechClient()

    # Set the text input configuration
    synthesis_input = texttospeech.SynthesisInput(text=text)

    # Set the voice parameters
    voice = texttospeech.VoiceSelectionParams(
        language_code="nl-NL",
        name="nl-NL-Wavenet-B",
    )

    # Set the audio format
    audio_config = texttospeech.AudioConfig(
        audio_encoding=texttospeech.AudioEncoding.MP3
    )

    # Perform the text-to-speech synthesis
    response = client.synthesize_speech(
        input=synthesis_input,
        voice=voice,
        audio_config=audio_config
    )

    # Upload the audio file to the bucket
    storage_client = storage.Client()
    bucket = storage_client.bucket(bucket)
    blob = bucket.blob(key)
    blob.upload_from_string(response.audio_content, content_type="audio/mpeg")


def cloud_function_handler(event, context):
    # Get the Cloud Storage bucket and object details from the event
    prompts_bucket = event["bucket"]
    object_name = event["name"]

    # Transcribe the audio
    transcript_text = transcribe_audio(prompts_bucket, object_name)

    # Ask OpenAI
    response = ask_openai(transcript_text)

    # Synthesize speech and upload to the bucket
    synthesize_speech_and_upload_to_bucket(RESPONSES_BUCKET, response, object_name)

    return {
        'statusCode': 200,
        'body': json.dumps(f'Transcribed: {response}')
    }
