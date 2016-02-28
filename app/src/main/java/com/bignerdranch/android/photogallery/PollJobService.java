package com.bignerdranch.android.photogallery;

import android.annotation.TargetApi;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.AsyncTask;
import android.os.Build;

import java.util.List;

/**
 * Created by jermiedomingo on 2/27/16.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class PollJobService extends JobService {

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

            List<GalleryItem> galleryItems = PollServicesHelper.fetchNewItems(PollJobService.this);

            if (galleryItems.size() == 0) {
                jobFinished(jobParameters, false);
                return null;
            }

            String resultId = galleryItems.get(0).getId();

            PollServicesHelper.sendNotificationIfNewDataFetched(PollJobService.this, resultId);
            jobFinished(jobParameters, false);
            return null;
        }
    }


}
