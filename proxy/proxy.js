const express = require('express');
const cors = require('cors');
const request = require('request');

const app = express();
const API_URL = 'https://h7po4qxpq3acpdeirdqcduojju0kyedr.lambda-url.eu-central-1.on.aws/';

app.use(cors());
app.use(express.json()); // Add this line to parse JSON payloads in POST requests

app.post('/auth', (req, res) => {
  request.post(
    {
      url: API_URL,
      json: req.body,
    },
    (error, response, body) => {
      if (error) {
        res.status(500).send(error);
      } else {
        res.status(response.statusCode).send(body);
      }
    }
  );
});

app.post('/push', async (req, res) => {
  const response = await fetch(API_URL, {
    method: 'POST',
    headers: {'Content-Type': 'application/json'},
    body: JSON.stringify({key: res.json(req.body).get('key')})
  });
  const S3PresignedUrl = await response.json();
  const formData = new FormData();
  
  // Append fields from the presignedUrl object
  Object.keys(presignedUrl.fields).forEach((key) => {
    formData.append(key, presignedUrl.fields[key]);
  });

  // Append the audio file to the form data
  //formData.append("file", audioBlob);

  // POST request to the presigned URL
  const result = await fetch(presignedUrl.url, {
    method: "POST",
    body: formData,
  });

  if (result.status === 204) {
    console.log("Upload successful!");
  } else {
    console.error("Upload failed:", result);
  }

})

app.listen(3001, () => console.log('Proxy server listening on port 3001'));

