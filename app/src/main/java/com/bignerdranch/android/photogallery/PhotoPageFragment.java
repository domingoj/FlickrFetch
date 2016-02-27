package com.bignerdranch.android.photogallery;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.util.List;

/**
 * Created by jermiedomingo on 2/27/16.
 */
public class PhotoPageFragment extends VisibleFragment {

    public static final String ARG_URI = "photo_page_uri";

    private Uri mUri;
    private WebView mWebView;
    private ProgressBar mProgressBar;

    public static PhotoPageFragment newInstance(Uri uri) {
        Bundle args = new Bundle();
        args.putParcelable(ARG_URI, uri);

        PhotoPageFragment fragment = new PhotoPageFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mUri = getArguments().getParcelable(ARG_URI);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable final Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_photo_page, container, false);

        mProgressBar = (ProgressBar) v.findViewById(R.id.fragment_photo_page_progress_bar);
        mProgressBar.setMax(100); //WebChromeClient reports in a range of 0-100

        mWebView = (WebView) v.findViewById(R.id.fragment_photo_page_webview);

        ((PhotoPageActivity) getActivity()).setWebView(mWebView);

        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {

                if (newProgress == 100) {
                    mProgressBar.setVisibility(View.GONE);
                } else {
                    mProgressBar.setVisibility(View.VISIBLE);
                    mProgressBar.setProgress(newProgress);
                }
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {

                AppCompatActivity activity = (AppCompatActivity) getActivity();
                activity.getSupportActionBar().setSubtitle(title);
            }
        });

        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {

                //checks if the scheme is not HTTP or HTTPS, if so, use implicit intent
                if (!Uri.parse(url).getScheme().equals("http") && !Uri.parse(url).getScheme().equals("https")) {
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));

                    PackageManager packageManager = getActivity().getPackageManager();
                    List activities = packageManager.queryIntentActivities(i,
                            PackageManager.MATCH_DEFAULT_ONLY);
                    boolean isIntentSafe = activities.size() > 0;
                    if (isIntentSafe) {
                        startActivity(i);
                    } else {
                        Toast.makeText(getActivity(), "Sorry, you don't have any app that could open this link.", Toast.LENGTH_SHORT).show();
                    }

                    return true;

                }
                return false;
            }
        });


        mWebView.loadUrl(mUri.toString());
        return v;
    }


}
