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

app.listen(3001, () => console.log('Proxy server listening on port 3001'));

