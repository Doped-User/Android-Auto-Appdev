package com.example.HardwareInfo;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.CarText;
import androidx.car.app.model.MessageTemplate;
import androidx.car.app.model.Template;

import java.util.ArrayList;
import java.util.Locale;

public class SpeechRecognizerScreen extends Screen {

    private SpeechRecognizer speechRecognizer;
    private CountDownTimer timer;
    private static final String TAG = "SpeechRecognizerScreen";

    public SpeechRecognizerScreen(@NonNull CarContext carContext) {
        super(carContext);
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
}