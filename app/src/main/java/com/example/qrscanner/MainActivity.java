package com.example.qrscanner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.budiyev.android.codescanner.CodeScanner;
import com.budiyev.android.codescanner.CodeScannerView;
import com.budiyev.android.codescanner.DecodeCallback;
import com.google.zxing.Result;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;


public class MainActivity extends AppCompatActivity {

    private CodeScanner mCodeScanner;
    TextView name, temperature, status;
    Button save_btn;
    public int buildingId = 1;
    public String userId;
    JSONObject activityLog = new JSONObject();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[] {Manifest.permission.CAMERA}, 1);
            }
        }

        CodeScannerView scannerView = findViewById(R.id.scanner_view);
        name = findViewById(R.id.name);
        temperature = findViewById(R.id.temp);
        status = findViewById(R.id.status);
        save_btn = findViewById(R.id.save_button);

        save_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SaveLog();
            }
        });

        mCodeScanner = new CodeScanner(this, scannerView);
        mCodeScanner.setDecodeCallback(new DecodeCallback() {
            @Override
            public void onDecoded(@NonNull final Result result) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        GetJsonObject(result.getText());
                    }
                });
            }
        });
        scannerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCodeScanner.startPreview();
                //save_btn.setVisibility(View.GONE);
                activityLog = null;
            }
        });
    }
    @Override
    protected void onResume() {
        super.onResume();
        save_btn.setVisibility(View.GONE);
    }

    @Override
    protected void onPause() {
        mCodeScanner.releaseResources();
        super.onPause();
    }

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
    public void GetJsonObject(String object) {
        try {
            activityLog = new JSONObject(object);
            activityLog.put("activityDate", sdf.format(new Date()));
            GetUserFullName(activityLog.getString("userId"));
            activityLog.put("buildingId", buildingId);
            GetUserHealthStatus(activityLog.getString("userId"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        //save_btn.setVisibility(View.VISIBLE);
    }

    public void GetUserHealthStatus(String id) {
        String API_URL = "https://mclogapi20220308122258.azurewebsites.net/api/UserHealthStatus/" + id;
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET,
                API_URL,
                null,
                response -> {
                    try {
                        temperature.setText(response.get("temperature").toString());
                        activityLog.put("healthStatusId", response.get("id"));
                        CheckIfUserHasSymptoms(String.valueOf(response.get("id")));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> Log.e("Rest_Response", error.toString())
        );
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
                1500,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));
        requestQueue.add(jsonObjectRequest);
    }

    public void CheckIfUserHasSymptoms(String id) {
        Log.e("_healthStatusId", id);
        String API_URL = "https://mclogapi20220308122258.azurewebsites.net/api/Symptoms/check/" + id;
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        @SuppressLint("ResourceAsColor") JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET,
                API_URL,
                null,
                response -> {
                    try {
                        activityLog.put("status", response.getString("response"));
                        status.setText(response.getString("response"));
                        SaveLog();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> Log.e("Rest_ResponseSYm", error.toString())
        );
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
                1500,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));
        requestQueue.add(jsonObjectRequest);
    }

    public void GetUserFullName(String id) {
        String API_URL = "https://mclogapi20220308122258.azurewebsites.net/api/Users/" + id;

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET,
                API_URL,
                null,
                response -> {
                    try {
                        name.setText(response.get("firstName").toString() +" "+ response.get("lastName").toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> Log.e("Rest_Response", error.toString())
        );
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
                1000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));
        requestQueue.add(jsonObjectRequest);
    }

    public void SaveLog() {
        String API_URL = "https://mclogapi20220308122258.azurewebsites.net/api/ActivityLogs";
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.POST,
                API_URL,
                activityLog,
                response -> {
                    Log.e("SaveLog: ", response.toString());
                    Toast.makeText(this, "Saved to log", Toast.LENGTH_SHORT).show();
                    save_btn.setVisibility(View.GONE);
                    mCodeScanner.stopPreview();
                },
                error -> Log.e("Rest Response", "Failed")
        );
        requestQueue.add(jsonObjectRequest);
    }
}
