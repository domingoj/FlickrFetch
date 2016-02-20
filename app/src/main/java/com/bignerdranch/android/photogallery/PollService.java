package com.bignerdranch.android.photogallery;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.SystemClock;
import android.util.Log;

import java.util.List;

/**
 * Created by jermiedomingo on 2/18/16.
 */
public class PollService extends IntentService {

    private static final String TAG = PollService.class.getSimpleName();

    public static final int POLL_INTERVAL = 1000 * 5; //60 seconds


    public PollService() {
        super(TAG);
    }

    public static Intent newIntent(Context context) {
        return new Intent(context, PollService.class);
    }

    public static void setServiceAlarm(Context context, boolean isOn) {

        Intent i = PollService.newIntent(context);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, i, 0);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if (isOn) {
            alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime(), POLL_INTERVAL, pendingIntent);
        } else {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        if (!isNetworkAvailableAndConnected()) {
            return;
        }

        String query = QueryPreferences.getStoredQuery(this);
        String lastRequestId = QueryPreferences.getLastResultId(this);

        List<GalleryItem> galleryItems;

        if (query == null) {
            galleryItems = FlickrFetcher.newInstance().fetchRecentPhotos(1);
        } else {
            galleryItems = FlickrFetcher.newInstance().searchPhotos(query, 1);
        }


        if (galleryItems.size() == 0) {
            return;
        }

        String resultId = galleryItems.get(0).getId();

        if (resultId.equals(lastRequestId)) {
            Log.i(TAG, "Got an old result: " + resultId);
        } else
            Log.i(TAG, "Got a new result: " + resultId);

        QueryPreferences.setLastResultId(this, resultId);
    }

    private boolean isNetworkAvailableAndConnected() {

        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        boolean isNetworkAvailable = cm.getActiveNetworkInfo() != null;
        boolean isNetworkConnected = isNetworkAvailable && cm.getActiveNetworkInfo().isConnected();
        return isNetworkConnected;
    }


}
