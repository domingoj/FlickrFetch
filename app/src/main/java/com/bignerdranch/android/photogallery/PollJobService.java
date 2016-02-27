package com.bignerdranch.android.photogallery;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import java.util.List;

/**
 * Created by jermiedomingo on 2/27/16.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class PollJobService extends JobService {

    public static final String TAG = PollJobService.class.getSimpleName();
    private PollTask mPollTask;

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

                NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(PollJobService.this);

                //ID (e.g. 0) should be unique across your application. If you post a second notification with this same ID,
                // it will replace the last notification you posted with that ID.
                notificationManagerCompat.notify(0, notification);
            }

            QueryPreferences.setLastResultId(PollJobService.this, resultId);
            jobFinished(jobParameters, false);
            return null;
        }
    }
}
