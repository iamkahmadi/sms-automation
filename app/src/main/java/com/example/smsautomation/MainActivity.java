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
    private EditText urlInputField;
    private OkHttpClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sendSmsButton = findViewById(R.id.sendSmsButton);
        fetchAndSendButton = findViewById(R.id.fetchAndSendButton);
        urlInputField = findViewById(R.id.urlInputField);

        client = new OkHttpClient(); // Initialize OkHttp client

        checkPermission(); // Check for SMS permission

        // Send SMS Button - Manually entered numbers and messages
        sendSmsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText phoneNumberInput = findViewById(R.id.phoneNumberInput);
                EditText messageInput = findViewById(R.id.messageInput);
                String phoneNumber = phoneNumberInput.getText().toString();
                String message = messageInput.getText().toString();

                if (!phoneNumber.isEmpty() && !message.isEmpty()) {
                    sendSms(phoneNumber, message);
                } else {
                    Toast.makeText(MainActivity.this, "Please enter a valid phone number and message.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Fetch SMS data from the server and send them
        fetchAndSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchSmsDataFromServer();
            }
        });
    }

    // Check and request SMS permission
    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, SMS_PERMISSION_CODE);
        }
    }

    // Handle the result of permission request
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

    // Method to send SMS manually
// Method to send SMS manually
    private void sendSms(String phoneNumber, String message) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);
            Log.d("SMS", "SMS sent to: " + phoneNumber);  // Log message

            // Ensure the Toast runs on the main thread
            runOnUiThread(() -> {
                Toast.makeText(MainActivity.this, "SMS Sent!", Toast.LENGTH_SHORT).show();
            });
        } catch (Exception e) {
            Log.e("SMS", "SMS failed: " + e.getMessage());  // Log error

            // Ensure the Toast runs on the main thread
            runOnUiThread(() -> {
                Toast.makeText(MainActivity.this, "SMS failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
        }
    }


    // Fetch SMS data from the server
    private void fetchSmsDataFromServer() {
        String url = urlInputField.getText().toString(); // URL from input field
        if (url.isEmpty()) {
            Toast.makeText(this, "Please enter a valid URL.", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("SMS", "Fetching SMS data from: " + url);  // Log URL
        // Create the HTTP request
        Request request = new Request.Builder().url(url).build();

        // Execute the request asynchronously
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Ensure that Toasts run on the main thread
                runOnUiThread(() -> {
                    Log.e("SMS", "Failed to fetch SMS data: " + e.getMessage());  // Log failure
                    Toast.makeText(MainActivity.this, "Failed to fetch SMS data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    List<String> phoneNumbers = new ArrayList<>();
                    List<String> messages = new ArrayList<>();

                    // Parse the response data to extract phone numbers and messages
                    String[] entries = responseData.split("\n");
                    for (String entry : entries) {
                        if (entry.contains("|")) {
                            String[] parts = entry.split("\\|");
                            String number = parts[0].trim();
                            String message = parts[1].trim();

                            phoneNumbers.add(number);
                            messages.add(message);

                            Log.d("SMS", "Parsed: " + number + " - " + message);  // Log parsed data
                        }
                    }

                    // Send SMS to all phone numbers
                    for (int i = 0; i < phoneNumbers.size(); i++) {
                        String phoneNumber = phoneNumbers.get(i);
                        String message = messages.get(i);
                        sendSms(phoneNumber, message); // Send SMS
                    }

                    // Ensure that Toasts run on the main thread
                    runOnUiThread(() -> {
                        Log.d("SMS", "SMS sent to all numbers.");  // Log success
                        Toast.makeText(MainActivity.this, "SMS sent to all numbers.", Toast.LENGTH_SHORT).show();
                    });
                } else {
                    // Ensure that Toasts run on the main thread
                    runOnUiThread(() -> {
                        Log.e("SMS", "Failed to fetch SMS data.");  // Log failure
                        Toast.makeText(MainActivity.this, "Failed to fetch SMS data.", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

}
