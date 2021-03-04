package com.cxa;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.NetworkResponse;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.cxa.search.SearchActivity;
import com.cxa.util.UIUtil;
import com.ibm.eo.EOCore;
import com.roughike.bottombar.BottomBar;
import com.roughike.bottombar.OnTabSelectListener;
import com.tl.uic.Tealeaf;
import com.tl.uic.TealeafEOLifecycleObject;
import com.tl.uic.model.Connection;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;

public class MoreActivity extends AppCompatActivity {
    EditText ibm_id_text;
    EditText post_url_text;
    EditText killswitch_url_text;
    EditText appkey_text;
    BottomBar bottomBar;

    Button saveSettings_button;

    // Test Tealeaf Connection type
    RequestQueue volleyQueue;
    OkHttpClient okHttpClient;

    String url = "https://jsonplaceholder.typicode.com/todos/1";


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_more);

        UIUtil.setupHeader(getSupportActionBar());

        getWindow().getDecorView().setBackgroundColor(Color.WHITE);

        ibm_id_text = (EditText) findViewById(R.id.ibm_id_text);
        ibm_id_text.setText(EOCore.getConfigItemString("ibmId", TealeafEOLifecycleObject.getInstance()));

        post_url_text = (EditText) findViewById(R.id.editText_post_url);
        post_url_text.setText(EOCore.getConfigItemString(Tealeaf.TLF_POST_MESSAGE_URL, TealeafEOLifecycleObject.getInstance()));

        killswitch_url_text = (EditText) findViewById(R.id.editText_killswitch_url);
        killswitch_url_text.setText(EOCore.getConfigItemString(Tealeaf.TLF_KILL_SWITCH_URL, TealeafEOLifecycleObject.getInstance()));

        appkey_text = (EditText) findViewById(R.id.editText_appkey);
        appkey_text.setText(EOCore.getConfigItemString(Tealeaf.TLF_APP_KEY, TealeafEOLifecycleObject.getInstance()));

        saveSettings_button = (Button) findViewById(R.id.button_save_settings);
        saveSettings_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean configModified = false;
                if (!post_url_text.getText().equals((EOCore.getConfigItemString(Tealeaf.TLF_POST_MESSAGE_URL, TealeafEOLifecycleObject.getInstance())))) {
                    EOCore.updateConfig(Tealeaf.TLF_POST_MESSAGE_URL, post_url_text.getText().toString(), TealeafEOLifecycleObject.getInstance());
                    configModified = true;
                }
                if (!killswitch_url_text.getText().equals((EOCore.getConfigItemString(Tealeaf.TLF_KILL_SWITCH_URL, TealeafEOLifecycleObject.getInstance())))) {
                    EOCore.updateConfig(Tealeaf.TLF_KILL_SWITCH_URL, killswitch_url_text.getText().toString(), TealeafEOLifecycleObject.getInstance());
                    configModified = true;
                }
                if (!appkey_text.getText().equals((EOCore.getConfigItemString(Tealeaf.TLF_APP_KEY, TealeafEOLifecycleObject.getInstance())))) {
                    EOCore.updateConfig(Tealeaf.TLF_APP_KEY, appkey_text.getText().toString(), TealeafEOLifecycleObject.getInstance());
                    configModified = true;
                }

                if (configModified) {
                    Tealeaf.disable();
                    Tealeaf.enable();
                }

                Tealeaf.logFormCompletion(true);

                Intent intent = new Intent(MoreActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });

        /***
         * Log Connection type using HTTPUrlConnection Client
         */
        Button logConnectionButton = findViewById(R.id.button_log_connection);
        logConnectionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        HttpURLConnection httpClient = null;
                        URL uri;

                        // Tealeaf Log Connection
                        Connection connection = new Connection();

                        try {
                            uri = new URL(url);
                            httpClient = (HttpURLConnection) uri.openConnection();

                            connection.setUrl(url);
                            connection.setInitTime(new Date().getTime());
                            // Send request
                            httpClient.connect();

                            String inputLine;
                            StringBuffer response = new StringBuffer();
                            if (httpClient.getResponseCode() == HttpURLConnection.HTTP_OK) { //success
                                BufferedReader in = new BufferedReader(new InputStreamReader(httpClient.getInputStream()));

                                while ((inputLine = in.readLine()) != null) {
                                    response.append(inputLine);
                                }
                                in.close();

                                // print result
                                System.out.println(response.toString());
                            } else {
                                System.out.println("POST request not worked");
                            }


                            connection.setLoadTime(new Date().getTime());
                            connection.setStatusCode(httpClient.getResponseCode());
                            connection.setResponseDataSize(response.length());
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            if (connection.getLoadTime() <= 0) {
                                connection.setLoadTime(new Date().getTime());
                            }

                            // Called in finally clause in case error
                            Tealeaf.logConnection(connection.getUrl(), connection.getInitTime(), connection.getLoadTime(), connection.getResponseDataSize(), connection.getStatusCode());
                            httpClient.disconnect();
                        }
                    }
                }).start();
            }
        });


        /***
         * Log Connection type using Volley Client
         */
        Button logConnectionVolleyButton = findViewById(R.id.button_log_connection_volley);
        okHttpClient = new OkHttpClient();
        logConnectionVolleyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /***
                 *  Tealeaf Log connection API
                 */
                Connection connection = new Connection();
                connection.setUrl(url);
                connection.setInitTime(new Date().getTime());

                // Instantiate the RequestQueue.
                volleyQueue = Volley.newRequestQueue(MoreActivity.this);
                Response.ErrorListener errorListener = new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        connection.setLoadTime(new Date().getTime());
                        connection.setStatusCode(error.networkResponse.statusCode);
                        connection.setResponseDataSize(error.networkResponse.data.length);
                        connection.setResponseTime(connection.getLoadTime() - connection.getInitTime());
                        Tealeaf.logConnection(connection);

                        VolleyLog.wtf(error.toString(), "utf-8");
                    }
                };

                // Request a JSON response from the provided URL.
                JSONObject jsonObject = new JSONObject();

                JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(url, jsonObject, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // Network response
                    }
                }, errorListener) {

                    @Override
                    protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
                        connection.setLoadTime(new Date().getTime());
                        connection.setStatusCode(response.statusCode);
                        connection.setResponseDataSize(response.data.length);
                        Tealeaf.logConnection(connection);

                        return super.parseNetworkResponse(response);
                    }

                    @Override
                    public int getMethod() {
                        return Method.GET;
                    }

                    @Override
                    public Priority getPriority() {
                        return Priority.NORMAL;
                    }
                };


                // Add the request to the RequestQueue.
                volleyQueue.add(jsonObjectRequest);
            }
        });

        /***
         * Log Connection type using Volley Client
         */
        Button logConnectionOkhttpButton = findViewById(R.id.button_log_connection_okhttp);
        logConnectionOkhttpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /***
                 *  Tealeaf Log connection API
                 */
                Connection connection = new Connection();
                connection.setUrl(url);
                connection.setInitTime(new Date().getTime());

                Request request = new Request.Builder()
                        .url(url)
                        .build();

                okHttpClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onResponse(Call call, okhttp3.Response response) throws IOException {
                        try (ResponseBody responseBody = response.body()) {
                            if (!response.isSuccessful())
                                throw new IOException("Unexpected code " + response);

                            connection.setLoadTime(new Date().getTime());
                            connection.setStatusCode(Integer.valueOf(response.networkResponse().code()));

                            connection.setResponseDataSize(responseBody.string().length());
                            Tealeaf.logConnection(connection);
//
//                            Headers responseHeaders = response.headers();
//                            for (int i = 0, size = responseHeaders.size(); i < size; i++) {
//                                System.out.println(responseHeaders.name(i) + ": " + responseHeaders.value(i));
//                            }
//
//                            System.out.println(responseBody.string());
                        }
                    }
                });
            }
        });

        bottomBar = (BottomBar) findViewById(R.id.bottomBar);
        loadBottomBar();
    }


    @Override
    public void onResume() {
        super.onResume();
        bottomBar.selectTabAtPosition(3);
        overridePendingTransition(0, 0);
    }


    public void loadBottomBar() {
        bottomBar.selectTabAtPosition(3);

        bottomBar.setOnTabSelectListener(new OnTabSelectListener() {
            @Override
            public void onTabSelected(int tabId) {
                if (tabId == R.id.tab_search) {
                    Intent intent = new Intent(MoreActivity.this, SearchActivity.class);
                    startActivity(intent);
                }
                if (tabId == R.id.tab_shopping) {
                    Intent intent = new Intent(MoreActivity.this, CartActivity.class);
                    startActivity(intent);
                } else if (tabId == R.id.tab_home) {
                    Intent intent = new Intent(MoreActivity.this, MainActivity.class);
                    startActivity(intent);

                }
            }
        });

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent e) {
        return super.dispatchTouchEvent(e);
    }
}

