package com.bignerdranch.android.photogallery;

import android.net.Uri;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jermiedomingo on 2/5/16.
 */


public class FlickrFetcher {


    private static final String TAG = "FlickrFetcher";
    public static final String API_KEY = "bfb722dafb7941a3c5678f2630ad8b0a";

    public static final String FETCH_RECENT_METHOD = "flickr.photos.getRecent";
    public static final String SEARCH_METHOD = "flickr.photos.search";

    public static final Uri ENDPOINT = Uri.parse("https://api.flickr.com/services/rest/")
            .buildUpon()
            .appendQueryParameter("api_key", API_KEY)
            .appendQueryParameter("format", "json")
            .appendQueryParameter("nojsoncallback", "1")
            .appendQueryParameter("extras", "url_s")
            .build();

    List<GalleryItem> mGalleryItems;
    private static FlickrFetcher mFlickrFetcher;
    private static String mquery;


    private FlickrFetcher() {

        mGalleryItems = new ArrayList<>();
    }

    public static FlickrFetcher newInstance() {

        if (mFlickrFetcher == null) {
            mFlickrFetcher = new FlickrFetcher();
        }
        return mFlickrFetcher;
    }


    public byte[] getUrlBytes(String urlSpec) throws IOException {
        URL url = new URL(urlSpec);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream in = connection.getInputStream();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException(connection.getResponseMessage() + " : with " + urlSpec);
            }

            int bytesRead = 0;
            byte[] buffer = new byte[1024];
            while ((bytesRead = in.read(buffer)) > 0) {
                out.write(buffer, 0, bytesRead);
            }
            out.close();
            return out.toByteArray();
        } finally {
            connection.disconnect();
        }
    }

    public String getUrlString(String urlSpec) throws IOException {
        return new String(getUrlBytes(urlSpec));
    }


    private List<GalleryItem> downloadGalleryItems(String url) {

        try {

            String jsonString = getUrlString(url);
            Log.i(TAG, "Received JSON: " + jsonString);

            JSONObject jsonBody = new JSONObject(jsonString);
            parseItems(mGalleryItems, jsonBody);

        } catch (IOException ioe) {
            Log.e(TAG, "Failed to fetch Items", ioe);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse json", e);
        }

        return mGalleryItems;
    }

    private String buildUrl(String method, String query, int pageCount) {
        Uri.Builder uriBuilder = ENDPOINT.buildUpon()
                .appendQueryParameter("method", method)
                .appendQueryParameter("page", String.valueOf(pageCount));

        if (method.equals(SEARCH_METHOD)) {
            uriBuilder.appendQueryParameter("text", query);
        }

        Log.d(TAG, uriBuilder.build().toString());
        return uriBuilder.build().toString();
    }

    public List<GalleryItem> searchPhotos(String query, int pageCount) {
        if (!query.equals(mquery)) {
            mGalleryItems = new ArrayList<>();
            mquery = query;
        }
        String url = buildUrl(SEARCH_METHOD, mquery, pageCount);
        return downloadGalleryItems(url);
    }

    public List<GalleryItem> fetchRecentPhotos(int pageCount) {
        String url = buildUrl(FETCH_RECENT_METHOD, null, pageCount);
        return downloadGalleryItems(url);
    }

    private void parseItems(List<GalleryItem> galleryItems, JSONObject jsonBody) throws IOException, JSONException {

        Gson gson = new GsonBuilder().create();


        JSONObject photosJSONObject = jsonBody.getJSONObject("photos");
        JSONArray photoJSONArray = photosJSONObject.getJSONArray("photo");

        for (int i = 0; i < photoJSONArray.length(); i++) {

            GalleryItem galleryItem = gson.fromJson(photoJSONArray.getJSONObject(i).toString(), GalleryItem.class);


            if (!photoJSONArray.getJSONObject(i).has("url_s") || galleryItem.getUrl() == null) {
                continue;
            }


            //OLD IMPLEMENTATION - USING JSON PARSING

//            GalleryItem galleryItem = new GalleryItem();
//            galleryItem.setId(photoJSONObject.getString("id"));
//            galleryItem.setCaption(photoJSONObject.getString("title"));
//            galleryItem.setUrl(photoJSONObject.getString("url_s"));

            galleryItems.add(galleryItem);
        }
    }
}
