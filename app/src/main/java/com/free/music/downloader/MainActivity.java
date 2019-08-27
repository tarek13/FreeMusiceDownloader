package com.free.music.downloader;


import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.Context;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.Bundle;


import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;

import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;


import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import at.huber.youtubeExtractor.VideoMeta;
import at.huber.youtubeExtractor.YouTubeExtractor;
import at.huber.youtubeExtractor.YtFile;


public class MainActivity extends AppCompatActivity implements YoutubeAdapter.Callback {

    //EditText for input search keywords
    private EditText searchInput;
    
    //YoutubeAdapter class that serves as a adapter for filling the 
    //RecyclerView by the CardView (video_item.xml) that is created in layout folder
    private YoutubeAdapter youtubeAdapter;
    
    //RecyclerView manages a long list by recycling the portion of view
    //that is currently visible on screen
    private RecyclerView mRecyclerView;
    
    //ProgressDialog can be shown while downloading data from the internet
    //which indicates that the query is being processed
    private ProgressDialog mProgressDialog;
    
    //Handler to run a thread which could fill the list after downloading data
    //from the internet and inflating the images, title and description
    private Handler handler;

    //results list of type VideoItem to store the results so that each item 
    //int the array list has id, title, description and thumbnail url
    private List<VideoItem> searchResults=new ArrayList<>();
    private String keywords;
    private LinearLayout mainLayout;
    private ScrollView scrollView;
    private LinearLayout mainView;


    //Overriding onCreate method(first method to be called) to create the activity 
    //and initialise all the variable to their respective views in layout file and 
    //adding listeners to required views
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //calling parent class to recall the app's last state
        super.onCreate(savedInstanceState);

        //method to fill the activity that is launched with  the activity_main.xml layout file
        setContentView(R.layout.activity_main);

        //initailising the objects with their respective view in activity_main.xml file
        mProgressDialog = new ProgressDialog(this);
        searchInput = (EditText)findViewById(R.id.search_input);
        mainLayout = (LinearLayout)findViewById(R.id.main_layout);
        scrollView = (ScrollView)findViewById(R.id.scrollView);
        mainView = (LinearLayout)findViewById(R.id.main_view);
        mRecyclerView = (RecyclerView) findViewById(R.id.videos_recycler_view);

        //setting title and and style for progress dialog so that users can understand
        //what is happening currently
        mProgressDialog.setTitle("Searching...");
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);

        //Fixing the size of recycler view which means that the size of the view
        //should not change if adapter or children size changes
        mRecyclerView.setHasFixedSize(true);
        //give RecyclerView a layout manager to set its orientation to vertical
        //by default it is vertical
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        handler = new Handler();

        //add listener to the EditText view which listens to changes that occurs when 
        //users changes the text or deletes the text
        //passing object of Textview's EditorActionListener to this method
        searchInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            
            //onEditorAction method called when user clicks ok button or any custom
            //button set on the bottom right of keyboard
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

                //actionId of the respective action is returned as integer which can
                //be checked with our set custom search button in keyboard
                if(actionId == EditorInfo.IME_ACTION_SEARCH){
                    
                    //setting progress message so that users can understand what is happening
                    mProgressDialog.setMessage("Finding videos for "+v.getText().toString());

                    //displaying the progress dialog on the top of activity for two reasons
                    //1.user can see what is going on
                    //2.User cannot click anything on screen for time being
                    mProgressDialog.show();

                    //calling our search method created below with input keyword entered by user
                    //by getText method which returns Editable type, get string by toString method
                    keywords=v.getText().toString();
                    searchOnYoutube();

                    //getting instance of the keyboard or any other input from which user types
                    InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                    //hiding the keyboard once search button is clicked
                    imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                            InputMethodManager.RESULT_UNCHANGED_SHOWN);

                    return false;
                }
                return true;
            }
        });

    }

    //custom search method which takes argument as the keyword for which videos is to be searched
    private void searchOnYoutube(){
        
        //A thread that will execute the searching and inflating the RecyclerView as and when
        //results are found
        new SearchYoutubeVideo().execute();

        //new AsyncTask<>().execute();
    }

    @Override
    public void onListOfItemClick(VideoItem videoItem, int position) {
        new YouTubeExtractor(this) {
            @Override
            public void onExtractionComplete(SparseArray<YtFile> ytFiles, VideoMeta vMeta) {
                scrollView.setVisibility(View.VISIBLE);
                mainView.setVisibility(View.GONE);
                if (ytFiles == null) {
                    // Something went wrong we got no urls. Always check this.
                    finish();
                    return;
                }
                // Iterate over itags
                for (int i = 0, itag; i < ytFiles.size(); i++) {
                    itag = ytFiles.keyAt(i);
                    // ytFile represents one file with its url and meta data
                    YtFile ytFile = ytFiles.get(itag);

                    // Just add videos in a decent format => height -1 = audio
                    if (ytFile.getFormat().getHeight() == -1 || ytFile.getFormat().getHeight() >= 360) {
                        addButtonToMainLayout(vMeta.getTitle(), ytFile);
                    }
                }
            }
        }.extract("https://www.youtube.com/watch?v=qOw44VFNk8Y", true, false);
    }
    private void addButtonToMainLayout(final String videoTitle, final YtFile ytfile) {
        // Display some buttons and let the user choose the format
        String btnText = (ytfile.getFormat().getHeight() == -1) ? "Audio " +
                ytfile.getFormat().getAudioBitrate() + " kbit/s" :
                ytfile.getFormat().getHeight() + "p";
        btnText += (ytfile.getFormat().isDashContainer()) ? " dash" : "";
        Button btn = new Button(this);
        btn.setText(btnText);
        btn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                String filename;
                if (videoTitle.length() > 55) {
                    filename = videoTitle.substring(0, 55) + "." + ytfile.getFormat().getExt();
                } else {
                    filename = videoTitle + "." + ytfile.getFormat().getExt();
                }
                filename = filename.replaceAll("[\\\\><\"|*?%:#/]", "");
                downloadFromUrl(ytfile.getUrl(), videoTitle, filename);
                finish();
            }
        });
        mainLayout.addView(btn);
    }

    private void downloadFromUrl(String youtubeDlUrl, String downloadTitle, String fileName) {
        Uri uri = Uri.parse(youtubeDlUrl);
        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setTitle(downloadTitle);

        request.allowScanningByMediaScanner();
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

        DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        manager.enqueue(request);
    }
    private class SearchYoutubeVideo extends AsyncTask{

        @Override
        protected Object doInBackground(Object[] objects) {
            YoutubeConnector yc = new YoutubeConnector(MainActivity.this);

            //calling the YoutubeConnector's search method by entered keyword
            //and saving the results in list of type VideoItem class
            searchResults = yc.search(keywords);
            return null;
        }

        @Override
        protected void onPreExecute() {
            //setting progress message so that users can understand what is happening
            mProgressDialog.setMessage("Finding videos for "+keywords);

            //displaying the progress dialog on the top of activity for two reasons
            //1.user can see what is going on
            //2.User cannot click anything on screen for time being
            mProgressDialog.show();
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Object o) {
            fillYoutubeVideos();

            //after the above has been done hiding the ProgressDialog
            mProgressDialog.dismiss();
            super.onPostExecute(o);
        }
    }

    //method for creating adapter and setting it to recycler view
    private void fillYoutubeVideos(){

        //object of YoutubeAdapter which will fill the RecyclerView
        youtubeAdapter = new YoutubeAdapter(getApplicationContext(),searchResults,this);

        //setAdapter to RecyclerView
        mRecyclerView.setAdapter(youtubeAdapter);

        //notify the Adapter that the data has been downloaded so that list can be updapted
        youtubeAdapter.notifyDataSetChanged();
    }
}