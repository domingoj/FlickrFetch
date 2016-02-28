package com.bignerdranch.android.photogallery;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.util.Log;

import java.util.List;

/**
 * Created by jermiedomingo on 2/28/16.
 */
public class PollServicesHelper {


    public static final String TAG = PollServicesHelper.class.getSimpleName();
    public static final String ACTION_SHOW_NOTIFICATION = "com.bignerdranch.android.photogallery.SHOW_NOTIFICATION";
    public static final String REQUEST_CODE = "REQUEST_CODE";
    public static final String NOTIFICATION = "NOTIFICATION";
    public static final String PERM_PRIVATE = "com.bignerdranch.android.photogallery.PRIVATE";

    public static List<GalleryItem> fetchNewItems(Context context) {

        String query = QueryPreferences.getStoredQuery(context);

        List<GalleryItem> galleryItems;

        if (query == null) {
            galleryItems = FlickrFetcher.newInstance().fetchRecentPhotos(1);
        } else {
            galleryItems = FlickrFetcher.newInstance().searchPhotos(query, 1);
        }

        return galleryItems;
    }

    public static void sendNotificationIfNewDataFetched(Context context, String resultId) {
        String lastRequestId = QueryPreferences.getLastResultId(context);

        if (resultId.equals(lastRequestId)) {
            Log.i(TAG, "Got an old result: " + resultId);
        } else {
            Log.i(TAG, "Got a new result: " + resultId);

            Resources resources = context.getResources();

            PendingIntent pi = PendingIntent.getActivity(context, 0, PhotoGalleryActivity.newIntent(context), 0);

            Notification notification = new Notification.Builder(context)
                    .setTicker(resources.getString(R.string.new_pictures_title))
                    .setSmallIcon(android.R.drawable.ic_menu_report_image)
                    .setContentTitle(resources.getString(R.string.new_pictures_title))
                    .setContentText(resources.getString(R.string.new_pictures_text))
                    .setContentIntent(pi)
                    .setAutoCancel(true)
                    .build();


            Intent i = new Intent(ACTION_SHOW_NOTIFICATION);
            i.putExtra(REQUEST_CODE, 0);
            i.putExtra(NOTIFICATION, notification);

            context.sendOrderedBroadcast(i, PERM_PRIVATE, null, null, Activity.RESULT_OK, null, null);

        }

        QueryPreferences.setLastResultId(context, resultId);
    }

}
