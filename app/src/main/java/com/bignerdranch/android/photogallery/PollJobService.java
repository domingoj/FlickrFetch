package com.bignerdranch.android.photogallery;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import java.util.List;

/**
 * Created by jermiedomingo on 2/27/16.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class PollJobService extends JobService {

    public static final String TAG = PollJobService.class.getSimpleName();
    public static final String ACTION_SHOW_NOTIFICATION = "com.bignerdranch.android.photogallery.SHOW_NOTIFICATION";

    private PollTask mPollTask;


    public static final String REQUEST_CODE = "REQUEST_CODE";

    public static final String NOTIFICATION = "NOTIFICATION";

    public static final String PERM_PRIVATE = "com.bignerdranch.android.photogallery.PRIVATE";

    @Override
    public boolean onStartJob(JobParameters params) {

        mPollTask = new PollTask();
        mPollTask.execute(params);
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        if (mPollTask != null) {
            mPollTask.cancel(true);
        }
        return true;
    }

    private class PollTask extends AsyncTask<JobParameters, Void, Void> {


        @Override
        protected Void doInBackground(JobParameters... params) {

            JobParameters jobParameters = params[0];


            String query = QueryPreferences.getStoredQuery(PollJobService.this);
            String lastRequestId = QueryPreferences.getLastResultId(PollJobService.this);

            List<GalleryItem> galleryItems;

            if (query == null) {
                galleryItems = FlickrFetcher.newInstance().fetchRecentPhotos(1);
            } else {
                galleryItems = FlickrFetcher.newInstance().searchPhotos(query, 1);
            }


            if (galleryItems.size() == 0) {
                jobFinished(jobParameters, false);
                return null;
            }

            String resultId = galleryItems.get(0).getId();

            if (resultId.equals(lastRequestId)) {
                Log.i(TAG, "Got an old result: " + resultId);
            } else {
                Log.i(TAG, "Got a new result: " + resultId);

                Resources resources = getResources();
                Intent i = PhotoGalleryActivity.newIntent(PollJobService.this);
                PendingIntent pi = PendingIntent.getActivity(PollJobService.this, 0, i, 0);

                Notification notification = new Notification.Builder(PollJobService.this)
                        .setTicker(resources.getString(R.string.new_pictures_title))
                        .setSmallIcon(android.R.drawable.ic_menu_report_image)
                        .setContentTitle(resources.getString(R.string.new_pictures_title))
                        .setContentText(resources.getString(R.string.new_pictures_text))
                        .setContentIntent(pi)
                        .setAutoCancel(true)
                        .build();

                sendNotification(0, notification);

                    }

            QueryPreferences.setLastResultId(PollJobService.this, resultId);
            jobFinished(jobParameters, false);
            return null;
        }
    }

    private void sendNotification(int requestCode, Notification notification) {

        Intent i = new Intent(ACTION_SHOW_NOTIFICATION);
        i.putExtra(REQUEST_CODE, requestCode);
        i.putExtra(NOTIFICATION, notification);

        sendOrderedBroadcast(i, PERM_PRIVATE, null, null, Activity.RESULT_OK, null, null);

    }
}
