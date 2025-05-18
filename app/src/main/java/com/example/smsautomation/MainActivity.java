package com.example.smsautomation;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.textfield.TextInputEditText;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int SMS_PERMISSION_CODE = 101;
    private Button sendSmsButton, fetchAndSendButton;
    private EditText urlInputField, bulkMessageInput;
    private OkHttpClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sendSmsButton = findViewById(R.id.sendSmsButton);
        fetchAndSendButton = findViewById(R.id.fetchAndSendButton);
        urlInputField = findViewById(R.id.urlInputField);
        bulkMessageInput = findViewById(R.id.bulkMessageInput);

        client = new OkHttpClient();

        checkPermission();

        sendSmsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String bulkInput = bulkMessageInput.getText().toString().trim();
                if (bulkInput.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please enter messages to send.", Toast.LENGTH_SHORT).show();
                    return;
                }

                String[] lines = bulkInput.split("\n");
                for (String line : lines) {
                    if (line.contains("|")) {
                        String[] parts = line.split("\\|", 2);
                        if (parts.length == 2) {
                            String number = parts[0].trim();
                            String message = parts[1].trim();
                            sendSms(number, message);
                        }
                    }
                }
            }
        });

        fetchAndSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchSmsDataFromServer();
            }
        });
    }

    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, SMS_PERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SMS_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void sendSms(String phoneNumber, String message) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);
            Log.d("SMS", "SMS sent to: " + phoneNumber);

            runOnUiThread(() -> Toast.makeText(MainActivity.this, "Sent: " + phoneNumber, Toast.LENGTH_SHORT).show());
        } catch (Exception e) {
            Log.e("SMS", "SMS failed: " + e.getMessage());
            runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed: " + phoneNumber, Toast.LENGTH_LONG).show());
        }
    }

    private void fetchSmsDataFromServer() {
        String url = urlInputField.getText().toString();
        if (url.isEmpty()) {
            Toast.makeText(this, "Please enter a valid URL.", Toast.LENGTH_SHORT).show();
            return;
        }

        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed to fetch SMS data: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    List<String> phoneNumbers = new ArrayList<>();
                    List<String> messages = new ArrayList<>();

                    String[] entries = responseData.split("\n");
                    for (String entry : entries) {
                        if (entry.contains("|")) {
                            String[] parts = entry.split("\\|", 2);
                            phoneNumbers.add(parts[0].trim());
                            messages.add(parts[1].trim());
                        }
                    }

                    for (int i = 0; i < phoneNumbers.size(); i++) {
                        sendSms(phoneNumbers.get(i), messages.get(i));
                    }

                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "SMS sent to all numbers.", Toast.LENGTH_SHORT).show());
                } else {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed to fetch SMS data.", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }
}
