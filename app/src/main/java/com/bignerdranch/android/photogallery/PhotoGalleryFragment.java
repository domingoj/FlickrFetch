package com.bignerdranch.android.photogallery;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.util.LruCache;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jermiedomingo on 2/5/16.
 */
public class PhotoGalleryFragment extends VisibleFragment {

    public static final String TAG = "PhotoGalleryFragment";

    private RecyclerView mPhotoRecyclerView;
    private PreCachingLayoutManager mPreCachingLayoutManager;
    private ProgressBar mProgressBar;
    private List<GalleryItem> mGalleryItems = new ArrayList<>();
    private ThumbnailDownloader<PhotoHolder> mThumbnailDownloader;
    private int pageCount = 1;
    private boolean mIsPollingOn;

    private boolean loading = true;
    private PhotoAdapter mPhotoAdapter;


    private LruCache<String, Bitmap> mMemoryCache;

    public static Fragment newInstance() {
        return new PhotoGalleryFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);

        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        final int cacheSize = maxMemory / 4;

        mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {

            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount() / 1024;
            }
        };


        Handler responseHandler = new Handler();
        mThumbnailDownloader = new ThumbnailDownloader<>(responseHandler);
        mThumbnailDownloader.setTThumbnailDownloadListener(new ThumbnailDownloader.ThumbnailDownloadListener<PhotoHolder>() {
            @Override
            public void onThumbnailDownloaded(PhotoHolder photoHolder, String url, Bitmap thumbnail) {
                Drawable drawable = new BitmapDrawable(getResources(), thumbnail);
                photoHolder.bindDrawable(drawable);
                //  addBitmapToCache(url, thumbnail);
            }
        });
        mThumbnailDownloader.start();
        mThumbnailDownloader.getLooper();
        Log.i(TAG, "Background thread started.");

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_photo_gallery, container, false);
        mPhotoRecyclerView = (RecyclerView) v.findViewById(R.id.fragment_photo_gallery_recycler_view);
        mPreCachingLayoutManager = new PreCachingLayoutManager(getActivity(), 3);
        mPreCachingLayoutManager.setExtraLayoutSpace(DeviceUtils.getScreenHeight(getActivity()));
        mPhotoRecyclerView.setLayoutManager(mPreCachingLayoutManager);


        mProgressBar = (ProgressBar) v.findViewById(R.id.content_load_progress_bar);

        updateItems();
        setUpAdapter();


        mPhotoRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {

                //check for scroll down
                if (dy > 0) {
                    int visibleItemCount = mPreCachingLayoutManager.getChildCount();
                    int totalItemCount = mPreCachingLayoutManager.getItemCount();
                    int pastVisibleItems = mPreCachingLayoutManager.findFirstVisibleItemPosition();

                    if (loading) {
                        if (visibleItemCount + pastVisibleItems >= totalItemCount) {
                            loading = false;
                            ++pageCount;
                            updateItems();

                        }
                    }
                }
            }
        });

        mPhotoRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mPhotoRecyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                int viewWidth = mPhotoRecyclerView.getMeasuredWidth();
                float scale = getActivity().getResources().getDisplayMetrics().density;
                int pixels = (int) (120 * scale + 0.5f);

                int newSpanCount = (int) Math.floor(viewWidth / pixels);
                mPreCachingLayoutManager.setSpanCount(newSpanCount);
                mPreCachingLayoutManager.requestLayout();
            }
        });


        return v;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mThumbnailDownloader.quit();
        Log.i(TAG, "Background thread destroyed.");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mThumbnailDownloader.clearQueue();

    }

    private void setUpAdapter() {

        if (isAdded()) {
            mPhotoAdapter = new PhotoAdapter(mGalleryItems);
            mPhotoRecyclerView.setAdapter(mPhotoAdapter);
            loading = true;
        }
    }

    private class FetchItemTask extends AsyncTask<Void, Void, List<GalleryItem>> {

        private String mQuery = null;

        public FetchItemTask(String query) {
            mQuery = query;
        }


        @Override
        protected void onPreExecute() {

            mProgressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected List<GalleryItem> doInBackground(Void... params) {

            if (mQuery == null) {

                return FlickrFetcher.newInstance().fetchRecentPhotos(pageCount);
            } else {

                return FlickrFetcher.newInstance().searchPhotos(mQuery, pageCount);
            }

        }


        @Override
        protected void onPostExecute(List<GalleryItem> galleryItems) {
            mProgressBar.setVisibility(View.GONE);

            mGalleryItems = galleryItems;
            if (!(pageCount > 1)) {
                setUpAdapter();
            } else if (isAdded()) {
                mPhotoAdapter.notifyItemRangeChanged(0, mGalleryItems.size());
                loading = true;
            }

        }
    }

    private class PhotoHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private ImageView mImageView;
        private GalleryItem mGalleryItem;

        public PhotoHolder(View itemView) {
            super(itemView);

            mImageView = (ImageView) itemView.findViewById(R.id.fragment_photo_gallery_image_view);
            itemView.setOnClickListener(this);
        }

        public void bindDrawable(Drawable drawable) {
            mImageView.setImageDrawable(drawable);
        }


        //Using libraries
        public void bindGalleryItem(GalleryItem galleryItem) {

            //Using glide
           /* Glide.with(PhotoGalleryFragment.this)
                    .load(galleryItem.getUrl())
                    .placeholder(R.drawable.bill_up_close)
                    .error(android.R.drawable.stat_notify_error)
                    .into(mImageView);*/


            //Using picasso
            Picasso.with(getActivity()).load(galleryItem.getUrl())
                    .placeholder(R.drawable.bill_up_close)
                    .error(android.R.drawable.stat_notify_error)
                    .into(mImageView);

            mGalleryItem = galleryItem;
        }

        @Override
        public void onClick(View v) {

            //Starts an implicit intent to open the link thru the browser
            // Intent i = new Intent(Intent.ACTION_VIEW, mGalleryItem.getPhotoPageUri());

            Intent i = PhotoPageActivity.newIntent(getActivity(), mGalleryItem.getPhotoPageUri());
            startActivity(i);
        }
    }

    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder> {

        private List<GalleryItem> mGalleryItemList;

        public PhotoAdapter(List<GalleryItem> galleryItems) {
            mGalleryItemList = galleryItems;
        }

        @Override
        public PhotoHolder onCreateViewHolder(ViewGroup parent, int viewType) {

            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
            View view = layoutInflater.inflate(R.layout.gallery_item, parent, false);

            return new PhotoHolder(view);
        }

        @Override
        public void onBindViewHolder(PhotoHolder holder, int position) {
            GalleryItem galleryItem = mGalleryItemList.get(position);

            //Using libries (Picasso/Glide)
            holder.bindGalleryItem(galleryItem);
//
//            Drawable placeHolder = ContextCompat.getDrawable(getActivity(), R.drawable.bill_up_close);
//            holder.bindDrawable(placeHolder);
//            final Bitmap bitmap = getBitmapFromMemoryCache(galleryItem.getUrl());
//            if (bitmap != null) {
//                Drawable drawable = new BitmapDrawable(getResources(), bitmap);
//                holder.bindDrawable(drawable);
//            } else {
//                mThumbnailDownloader.queueThumbnail(holder, galleryItem.getUrl());
//            }
        }

        @Override
        public int getItemCount() {
            return mGalleryItemList.size();
        }
    }

    public void addBitmapToCache(String url, Bitmap bitmap) {
        if (getBitmapFromMemoryCache(url) == null) {
            Log.i(TAG, "Adding bitmap to cache.");
            mMemoryCache.put(url, bitmap);
        }
    }

    public Bitmap getBitmapFromMemoryCache(String url) {
        Log.i(TAG, "Getting bitmap from cache.");
        return mMemoryCache.get(url);

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_photo_gallery, menu);

        MenuItem searchItem = menu.findItem(R.id.menu_item_search);
        final SearchView searchView = (SearchView) searchItem.getActionView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Log.d(TAG, "Query text submitted: " + query);
                pageCount = 1;
                QueryPreferences.setStoredQuery(getActivity(), query);
                searchView.clearFocus();
                updateItems();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                Log.d(TAG, "Query text changed: " + newText);
                return false;
            }
        });

        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String query = QueryPreferences.getStoredQuery(getActivity());
                searchView.setQuery(query, false);
            }
        });

        MenuItem toggleItem = menu.findItem(R.id.menu_item_toggle_polling);


        if (mIsPollingOn) {
            toggleItem.setTitle(R.string.stop_polling);
        } else {
            toggleItem.setTitle(R.string.start_polling);
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_clear: {
                QueryPreferences.setStoredQuery(getActivity(), null);
                pageCount = 1;
                updateItems();
                return true;
            }
            case R.id.menu_item_toggle_polling: {

                mIsPollingOn = !mIsPollingOn;

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

                    Log.i(TAG, "JOBSCHEDULER TOGGLED!!!");
                    final int JOB_ID = 1;
                    JobScheduler jobScheduler = (JobScheduler) getContext().getSystemService(Context.JOB_SCHEDULER_SERVICE);

                    if (!mIsPollingOn) {

                        jobScheduler.cancelAll();
                        Log.i(TAG, "JOBSCHEDULER CANCELED!");

                    } else {
                        boolean hasBeenScheduled = false;

                        for (JobInfo jobInfo : jobScheduler.getAllPendingJobs()) {
                            if (jobInfo.getId() == JOB_ID) {
                                hasBeenScheduled = true;
                            }
                        }

                        if (hasBeenScheduled == false) {
                            JobInfo jobInfo = new JobInfo.Builder(
                                    JOB_ID, new ComponentName(getContext(), PollJobService.class))
                                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
                                    .setPeriodic(1000 * 60)
                                    .setPersisted(true)
                                    .build();

                            jobScheduler.schedule(jobInfo);

                        }
                    }

                } else {

                    boolean shouldStartAlarm = !PollService.isServiceAlarmOn(getActivity());
                    PollService.setServiceAlarm(getActivity(), shouldStartAlarm);


                }

                //Update the toolbar options menu
                getActivity().invalidateOptionsMenu();
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);

        }
    }

    private void updateItems() {

        String query = QueryPreferences.getStoredQuery(getActivity());
        Log.d(TAG, "Preference Query: " + query);
        new FetchItemTask(query).execute();
    }


}
