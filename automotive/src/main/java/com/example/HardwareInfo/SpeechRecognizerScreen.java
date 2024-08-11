package com.example.HardwareInfo;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.CarText;
import androidx.car.app.model.MessageTemplate;
import androidx.car.app.model.Template;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SpeechRecognizerScreen extends Screen {

    private SpeechRecognizer speechRecognizer;
    private CountDownTimer timer;
    private TextToSpeech textToSpeech;
    private static final String TAG = "SpeechRecognizerScreen";
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String API_KEY = "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"; // Replace with your OpenAI API key

    public SpeechRecognizerScreen(@NonNull CarContext carContext) {
        super(carContext);
        initTextToSpeech();
    }

    @NonNull
    @Override
    public Template onGetTemplate() {
        return getMessageTemplate();
    }

    private Template getMessageTemplate() {
        Action startButton = new Action.Builder()
                .setTitle(CarText.create("Start"))
                .setOnClickListener(this::startSpeechRecognition)
                .build();

        Action stopButton = new Action.Builder()
                .setTitle(CarText.create("Stop"))
                .setOnClickListener(this::stopSpeechRecognition)
                .build();

        return new MessageTemplate.Builder(CarText.create("Click Start to Speak"))
                .setHeaderAction(Action.BACK)
                .addAction(startButton)
                .addAction(stopButton)
                .build();
    }

    private void startSpeechRecognition() {
        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(getCarContext());
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {
                    Log.d(TAG, "onReadyForSpeech");
                    startTimer();
                }

                @Override
                public void onBeginningOfSpeech() {
                    Log.d(TAG, "onBeginningOfSpeech");
                }

                @Override
                public void onRmsChanged(float rmsdB) {
                }

                @Override
                public void onBufferReceived(byte[] buffer) {
                    Log.d(TAG, "onBufferReceived");
                }

                @Override
                public void onEndOfSpeech() {
                    Log.d(TAG, "onEndOfSpeech");
                    stopTimer();
                }

                @Override
                public void onError(int error) {
                    Log.e(TAG, "Error: " + error);
                    stopTimer();
                }

                @Override
                public void onResults(Bundle results) {
                    ArrayList<String> data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (data != null && !data.isEmpty()) {
                        String bestResult = data.get(0);
                        Log.d(TAG, "Best Result: " + bestResult);
                        sendToChatGPT(bestResult);
                    } else {
                        Log.d(TAG, "No results.");
                    }
                }

                @Override
                public void onPartialResults(Bundle partialResults) {
                    Log.d(TAG, "onPartialResults");
                }

                @Override
                public void onEvent(int eventType, Bundle params) {
                    Log.d(TAG, "onEvent: " + eventType);
                }
            });
        }

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);

        try {
            speechRecognizer.startListening(intent);
        } catch (Exception e) {
            Log.e(TAG, "Exception while starting listening: " + e.getMessage());
        }
    }

    private void stopSpeechRecognition() {
        if (speechRecognizer != null) {
            try {
                speechRecognizer.stopListening();
                speechRecognizer.destroy();
                speechRecognizer = null;
            } catch (Exception e) {
                Log.e(TAG, "Exception while stopping listening: " + e.getMessage());
            }
        }
        stopTimer();
    }

    private void startTimer() {
        timer = new CountDownTimer(10000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                Log.d(TAG, "Time remaining: " + millisUntilFinished / 1000 + "s");
            }

            @Override
            public void onFinish() {
                Log.d(TAG, "Timer finished.");
                stopSpeechRecognition();
            }
        }.start();
    }

    private void stopTimer() {
        if (timer != null) {
            timer.cancel();
        }
    }

    private void initTextToSpeech() {
        textToSpeech = new TextToSpeech(getCarContext(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(Locale.getDefault());
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "Language is not supported or data is missing");
                } else {
                    Log.d(TAG, "TextToSpeech initialized successfully");
                }
            } else {
                Log.e(TAG, "TextToSpeech initialization failed");
            }
        });
    }

    private void sendToChatGPT(String userMessage) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            OkHttpClient client = new OkHttpClient();

            ChatGPTRequest chatGPTRequest = new ChatGPTRequest(userMessage);
            String json = new Gson().toJson(chatGPTRequest);
            Log.d(TAG, "Request JSON: " + json); // Log the request JSON

            RequestBody body = RequestBody.create(json, MediaType.get("application/json; charset=utf-8"));
            Request request = new Request.Builder()
                    .url(API_URL)
                    .post(body)
                    .addHeader("Authorization", "Bearer " + API_KEY)
                    .build();

            Log.d(TAG, "Request Headers: " + request.headers()); // Log request headers

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e(TAG, "API call failed: " + e.getMessage());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        Log.e(TAG, "Unexpected response: " + response);
                    } else {
                        ChatGPTResponse chatGPTResponse = new Gson().fromJson(response.body().string(), ChatGPTResponse.class);
                        if (chatGPTResponse != null && chatGPTResponse.getChoices() != null && !chatGPTResponse.getChoices().isEmpty()) {
                            String reply = chatGPTResponse.getChoices().get(0).getText().trim();
                            speakReply(reply);
                        } else {
                            Log.e(TAG, "No response from ChatGPT.");
                        }
                    }
                }
            });
        });
    }

    private void speakReply(String reply) {
        if (reply != null && textToSpeech != null) {
            textToSpeech.speak(reply, TextToSpeech.QUEUE_FLUSH, null, null);
        } else {
            Log.e(TAG, "Reply is null or TextToSpeech is not initialized");
        }
    }

    static class ChatGPTRequest {
        private final String model = "gpt-3.5-turbo";
        private final String prompt;
        private final int max_tokens = 150;

        ChatGPTRequest(String prompt) {
            this.prompt = prompt;
        }

        public String getModel() {
            return model;
        }

        public String getPrompt() {
            return prompt;
        }

        public int getMaxTokens() {
            return max_tokens;
        }
    }

    static class ChatGPTResponse {
        private ArrayList<Choice> choices;

        ArrayList<Choice> getChoices() {
            return choices;
        }

        static class Choice {
            private String text;

            String getText() {
                return text;
            }
        }
    }
}