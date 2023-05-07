import Head from 'next/head';
import { useState } from 'react';
import { Inter } from 'next/font/google'
import styles from '../styles/Home.module.css';

const inter = Inter({ subsets: ['latin'] })
const URL = "http://localhost:3001/push"

interface PresignedUrl {
  url: string;
  fields: { [key: string]: string };
}

export default function Home() {
  const [recording, setRecording] = useState(false);
  const [mediaRecorder, setMediaRecorder] = useState<MediaRecorder | null>(null);
  const [audioURL, setAudioURL] = useState('');

  const startRecording = async () => {
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
    const recorder = new MediaRecorder(stream);
    const chunks: BlobPart[] = [];

    recorder.ondataavailable = (e) => chunks.push(e.data);
    recorder.onstop = async () => {
      const blob = new Blob(chunks, { type: recorder.mimeType });
      const audioName = (Date.now() + Math.random()).toString();

      try {
        await uploadAudioToS3(audioName, blob);
        setRecording(false);
      } catch (error) {
        console.error('Error uploading and processing audio:', error);
      }
    };

    setMediaRecorder(recorder);
    recorder.start();
    setRecording(true);
  };

  const stopRecording = () => {
    if (mediaRecorder) {
      mediaRecorder.stop();
      setRecording(false);
    }
  };

  const playAudio = () => {
    if (audioURL) {
      const audio = new Audio(audioURL);
      audio.play();
    }
  };

  const uploadAudioToS3 = async (audioName: string, audioBlob: Blob): Promise<void> => {
    const response = await fetch(URL, {
      method: 'POST',
      headers: {'Content-Type': 'application/json'},
      body: JSON.stringify({key: audioName})
    });
    const data = await response.json();
    return data;
  }

  return (
    <div className={styles.container}>
      <Head>
        <title>Learn Dutch</title>
        <meta name="description" content="Learn Dutch with audio recordings and transcriptions" />
      </Head>
      <main className={styles.main}>
        <h1 className={styles.title}>lambota</h1>
        <div className={styles.flagsAndLions}>
          <img src="/dutch_flag.png" alt="Dutch flag" className={styles.flag} />
          <img src="/lion.png" alt="Lion" className={styles.lion} />
          <img src="/dutch_flag.png" alt="Dutch flag" className={styles.flag} />
        </div>
        <div className={styles.audioControls}>
          <button onClick={recording ? stopRecording : startRecording} className={styles.button}>
            {recording ? 'Stop Recording' : 'Start Recording'}
          </button>
          {audioURL && (
            <button onClick={playAudio} className={styles.button}>
              Play Processed Audio
            </button>
          )}
        </div>
      </main>
    </div>
  );
}
