package com.silverorange.videoplayer;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.ui.StyledPlayerControlView;
import com.google.android.exoplayer2.ui.StyledPlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import Models.VideoDetail;
import io.noties.markwon.Markwon;

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
    private StyledPlayerView mVideoPlayer;
    private ImageView mPlayPauseButton, mNextButton, mPreviousButton;
    private ExoPlayer mPlayer;

    // private data variables
    private ArrayList<VideoDetail> videos;
    private int currentVideoIndex = 0;
    private Markwon markwon;

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
        mVideoPlayer = findViewById(R.id.video_view);
        mVideoPlayer.setUseController(false);
        mPlayer = new ExoPlayer.Builder(this).build();
        mVideoPlayer.setPlayer(mPlayer);

        mPlayPauseButton = findViewById(R.id.play_pause_button);
        mNextButton = findViewById(R.id.next_button);
        mPreviousButton = findViewById(R.id.previous_button);

        mPlayPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mVideoPlayer.getPlayer().isPlaying()){
                    mVideoPlayer.getPlayer().pause();
                    mPlayPauseButton.setImageResource(R.drawable.ic_play);
                }else{
                    mVideoPlayer.getPlayer().play();
                    mPlayPauseButton.setImageResource(R.drawable.ic_pause);
                }
            }
        });

        mPreviousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(currentVideoIndex > 0){
                    currentVideoIndex -= 1;
                    setUIAsPerVideoIndex(currentVideoIndex);
                }
            }
        });

        mNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(currentVideoIndex < videos.size() - 1){
                    currentVideoIndex += 1;
                    setUIAsPerVideoIndex(currentVideoIndex);
                }
            }
        });

        videos = new ArrayList<>();
        markwon = Markwon.create(this);
    }

    private void fetchAPIData(){
        mProgressBar.setVisibility(View.VISIBLE);
        videos.clear();
        StringRequest dataRequest = new StringRequest(Request.Method.GET, DATA_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        parseAPIData(response);
                        mProgressBar.setVisibility(View.INVISIBLE);
                        showDataInUI();
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
                video.author = videoData.getJSONObject(AUTHOR).getString(AUTHOR_NAME);
                videos.add(video);
            }
        }catch(JSONException e){
            Log.e("Parse Error", e.getLocalizedMessage());
        }catch (ParseException e){
            Log.e("Parse Error", e.getLocalizedMessage());
        }
    }

    private void showDataInUI(){
        sortVideosUsingDate();
        setUIAsPerVideoIndex(0);
    }

    private void sortVideosUsingDate(){
        Collections.sort(videos, new Comparator<VideoDetail>() {
            @Override
            public int compare(VideoDetail lhs, VideoDetail rhs) {
                return lhs.publishedAt.compareTo(rhs.publishedAt);
            }
        });
    }

    private void setUIAsPerVideoIndex(int videoIndex){
        DataSource.Factory dataSourceFactory = new DefaultHttpDataSource.Factory();
        HlsMediaSource hlsMediaSource =
                new HlsMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(MediaItem.fromUri(Uri.parse(videos.get(videoIndex).hlsURL)));

        mPlayer.setMediaSource(hlsMediaSource);
        mPlayer.prepare();

        String prepareMarkdown = "# " + videos.get(videoIndex).title + "\n#### By " + videos.get(videoIndex).author + "\n" + videos.get(videoIndex).description;
        markwon.setMarkdown(mVideoDetailsTextView, prepareMarkdown);
    }
}
