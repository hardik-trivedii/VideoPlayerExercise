package com.silverorange.videoplayer;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DateTimeException;
import java.util.ArrayList;

import Models.VideoDetail;

public class MainActivity extends AppCompatActivity {
    // constants
    final String VIDEO_ID = "id", TITLE = "title", HLS_URL = "hlsURL", FULL_URL = "fullURL",
            DESCRIPTION = "description", PUBLISHED_AT = "publishedAt", AUTHOR = "author", AUTHOR_NAME = "name";
    // TODO: Change this DATA_URL as per your device/network configuration
    final String DATA_URL = "http://172.16.1.80:4000/videos";

    // private UI variables
    private ConstraintLayout mRootView;
    private ProgressBar mProgressBar;
    private TextView mVideoDetailsTextView;

    // private data variables
    private ArrayList<VideoDetail> videos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeUIAndDataVariables();
        fetchAPIData();
    }

    private void initializeUIAndDataVariables(){
        mRootView = findViewById(R.id.root_view);
        mProgressBar = findViewById(R.id.progressBar);
        mProgressBar.setVisibility(View.INVISIBLE);
        mVideoDetailsTextView = findViewById(R.id.video_description);

        videos = new ArrayList<>();
    }

    private void fetchAPIData(){
        mProgressBar.setVisibility(View.VISIBLE);
        videos.clear();
        StringRequest dataRequest = new StringRequest(Request.Method.GET, DATA_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d("VideoPlayer", "onResponse: " + response);
                        mProgressBar.setVisibility(View.INVISIBLE);
                        parseAPIData(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        mProgressBar.setVisibility(View.INVISIBLE);
                        Snackbar.make(mRootView, getString(R.string.error_message_api_fetch), Snackbar.LENGTH_INDEFINITE)
                                .setAction("Retry", new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        fetchAPIData();
                                    }
                                })
                                .show();
                    }
                });
        Volley.newRequestQueue(this).add(dataRequest);
    }

    private void parseAPIData(String response){
        try{
            JSONArray responseData = new JSONArray(response);
            for (int i = 0; i < responseData.length(); i++){
                JSONObject videoData = responseData.getJSONObject(i);
                VideoDetail video = new VideoDetail();
                video.id = videoData.getString(VIDEO_ID);
                video.title = videoData.getString(TITLE);
                video.hlsURL = videoData.getString(HLS_URL);
                video.fullURL = videoData.getString(FULL_URL);
                video.description = videoData.getString(DESCRIPTION);
                video.publishedAt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(videoData.getString(PUBLISHED_AT));
                video.author = videoData.getJSONObject(AUTHOR).getString(TITLE);
            }
        }catch(JSONException e){

        }catch (ParseException e){
            Log.e("Parse E", e.getLocalizedMessage());
        }
    }
}
