package com.example.mgiddaluri.casttest;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.MediaRouteActionProvider;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import java.io.IOException;


public class Webview extends ActionBarActivity {

    WebView webview;

    private static final String TAG = MainActivity.class.getSimpleName();
    private MediaRouter mMediaRouter;
    private MediaRouteSelector mMediaRouteSelector;
    private CastDevice mSelectedDevice;
    private GoogleApiClient mApiClient;
    private boolean start;
    String Url=null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);
        webview = (WebView) findViewById(R.id.webView);
        WebSettings webSettings = webview.getSettings();
        webSettings.setJavaScriptEnabled(true);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Intent intent = getIntent();
        this.Url = intent.getExtras().getString("SendUrl");
        start=intent.getExtras().getBoolean("Started");
        mMediaRouter = MediaRouter.getInstance(getApplicationContext());
        mMediaRouteSelector = new MediaRouteSelector.Builder().addControlCategory(CastMediaControlIntent.categoryForCast(getResources().getString(R.string.app_id))).build();

        webview.loadUrl(Url);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_main, menu);

        MenuItem mediaRouteMenuItem = menu.findItem(R.id.media_route_menu_item);
        MediaRouteActionProvider mediaRouteActionProvider = (MediaRouteActionProvider) MenuItemCompat.getActionProvider(mediaRouteMenuItem);
        mediaRouteActionProvider.setRouteSelector(mMediaRouteSelector);
        return true;
    }

    private final MediaRouter.Callback mMediaRouterCallback = new MediaRouter.Callback()    {

        @Override
        public void onRouteSelected(MediaRouter router, MediaRouter.RouteInfo info) {

            mSelectedDevice = CastDevice.getFromBundle(info.getExtras());
            setSelectedDevice(mSelectedDevice);
        }

        @Override
        public void onRouteUnselected(MediaRouter router, MediaRouter.RouteInfo info) {
            Log.d(TAG, "onRouteUnselected: info=" + info);
            mSelectedDevice = null;
            stopApplication();
            setSelectedDevice(null);
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        mMediaRouter.addCallback(mMediaRouteSelector, mMediaRouterCallback,
                MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY);
    }

    @Override
    protected void onStop() {
        mMediaRouter.removeCallback(mMediaRouterCallback);
        super.onStop();
    }
   private void sendMessage(String message) {
       if (mApiClient != null ) {
           try {
               Cast.CastApi.sendMessage(mApiClient,getResources().getString(R.string.namespace), message)
                       .setResultCallback(new ResultCallback<Status>() {
                           @Override
                           public void onResult(Status result) {
                               if (!result.isSuccess()) {
                                   Log.e(TAG, "Sending message failed");
                               }
                           }
                       });
           } catch (Exception e) {
               Log.e(TAG, "Exception while sending message", e);
           }
       } else {
           Toast.makeText(Webview.this, message, Toast.LENGTH_SHORT)
                   .show();
       }
   }
    private void setSelectedDevice(CastDevice device)
    {
        Log.d(TAG, "setSelectedDevice: " + device);
        mSelectedDevice = device;
        if (mSelectedDevice != null)
        {
            try
            {
                stopApplication();
                disconnectApiClient();
                connectApiClient();
            }
            catch (IllegalStateException e)
            {
                Log.w(TAG, "Exception while connecting API client", e);
                disconnectApiClient();
            }
        }
        else
        {
            disconnectApiClient();
            mMediaRouter.selectRoute(mMediaRouter.getDefaultRoute());
        }
    }

    private void connectApiClient()
    {
        Cast.CastOptions apiOptions = Cast.CastOptions.builder(mSelectedDevice, castClientListener).build();
        mApiClient = new GoogleApiClient.Builder(this)
                .addApi(Cast.API, apiOptions)
                .addConnectionCallbacks(connectionCallback)
                .addOnConnectionFailedListener(mConnectionFailedListener)
                .build();
        mApiClient.connect();
    }

    private void disconnectApiClient()
    {
        if (mApiClient != null)
        {
            mApiClient.disconnect();
            mApiClient = null;
        }
    }

    private void stopApplication(){
        if(mApiClient==null) return;
        if(start){
            Cast.CastApi.stopApplication(mApiClient);
            start=false;
        }
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public final Cast.MessageReceivedCallback incomingMsgHandler = new Cast.MessageReceivedCallback()
    {
        @Override
        public void onMessageReceived(CastDevice castDevice, String namespace, String message) {
            Log.d(TAG, String.format("message namespace: %s message: %s", namespace, message));
        }
    };

    private final GoogleApiClient.ConnectionCallbacks connectionCallback = new GoogleApiClient.ConnectionCallbacks()
    {
        @Override
        public void onConnected(Bundle bundle)
        {
            try
            {
                Cast.CastApi.launchApplication(mApiClient,getResources().getString(R.string.app_id), false).setResultCallback(connectionResultCallback);
            }
            catch (Exception e)
            {
                Log.e(TAG, "Failed to launch application", e);
            }
        }

        @Override
        public void onConnectionSuspended(int i)
        {
            Log.v(TAG, "Connection is suspended from Server\n Please Try again !!");
        }
    };


    private final GoogleApiClient.OnConnectionFailedListener mConnectionFailedListener = new GoogleApiClient.OnConnectionFailedListener()
    {
        @Override
        public void onConnectionFailed(ConnectionResult connectionResult)
        {
            setSelectedDevice(null);
        }
    };

    private final Cast.Listener castClientListener = new Cast.Listener()
    {
        @Override
        public void onApplicationDisconnected(int statusCode)
        {
            try
            {
                Cast.CastApi.removeMessageReceivedCallbacks(mApiClient,getResources().getString(R.string.namespace));
            }
            catch (IOException e)
            {
                Log.w(TAG, "Exception while launching application", e);
            }
            setSelectedDevice(null);
        }

        @Override
        public void onVolumeChanged()
        {
            if (mApiClient != null)
            {
                Log.d(TAG, "onVolumeChanged: " + Cast.CastApi.getVolume(mApiClient));
            }
        }
    };
    private final ResultCallback<Cast.ApplicationConnectionResult> connectionResultCallback = new ResultCallback<Cast.ApplicationConnectionResult>()
    {
        @Override
        public void onResult(Cast.ApplicationConnectionResult result)
        {
            Status status = result.getStatus();
            if (status.isSuccess())
            {
                start = true;

                try
                {
                    Cast.CastApi.setMessageReceivedCallbacks(mApiClient,getResources().getString(R.string.namespace), incomingMsgHandler);
                }
                catch (IOException e)
                {
                    Log.e(TAG, "Exception while creating channel", e);
                }
                sendMessage(Url);
            }
            else
            {
                setSessionStarted(false);
            }
        }
    };
}
