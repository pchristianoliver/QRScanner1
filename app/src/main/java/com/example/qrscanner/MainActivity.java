package com.example.qrscanner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.budiyev.android.codescanner.CodeScanner;
import com.budiyev.android.codescanner.CodeScannerView;
import com.budiyev.android.codescanner.DecodeCallback;
import com.google.zxing.Result;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;


public class MainActivity extends AppCompatActivity {

    private CodeScanner mCodeScanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        CodeScannerView scannerView = findViewById(R.id.scanner_view);
        mCodeScanner = new CodeScanner(this, scannerView);
        mCodeScanner.setDecodeCallback(new DecodeCallback() {
            @Override
            public void onDecoded(@NonNull final Result result) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, result.getText(), Toast.LENGTH_SHORT).show();
                        getJsonObject(result.getText());
                    }
                });
            }
        });
        scannerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCodeScanner.startPreview();
            }
        });
    }
    @Override
    protected void onResume() {
        super.onResume();
        mCodeScanner.startPreview();
    }

    @Override
    protected void onPause() {
        mCodeScanner.releaseResources();
        super.onPause();
    }

    JSONObject activityLog = null;
    public void getJsonObject(String object) {
        try {
            activityLog = new JSONObject(object);
            activityLog.put("status", "Healthy");
            activityLog.put("activityDate", Calendar.getInstance().getTime().toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        saveLog();
    }

    public void saveLog() {
        String API_URL = "https://mclogapi20220308122258.azurewebsites.net/api/ActivityLogs";
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.POST,
                API_URL,
                activityLog,
                response -> Log.e("Rest Response", "Success"),
                error -> Log.e("Rest Response", "Failed")
        );

        requestQueue.add(jsonObjectRequest);
    }
}
