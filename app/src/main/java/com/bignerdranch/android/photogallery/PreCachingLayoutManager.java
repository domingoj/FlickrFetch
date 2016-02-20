package com.bignerdranch.android.photogallery;

import android.content.Context;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;

/**
 * Created by jermiedomingo on 2/13/16.
 */

    public class PreCachingLayoutManager extends GridLayoutManager {
        private static final int DEFAULT_EXTRA_LAYOUT_SPACE = 600;
        private int extraLayoutSpace = -1;


        public PreCachingLayoutManager(Context context, int extraLayoutSpace) {
            super(context,extraLayoutSpace);
            this.extraLayoutSpace = extraLayoutSpace;
        }


        public void setExtraLayoutSpace(int extraLayoutSpace) {
            this.extraLayoutSpace = extraLayoutSpace;
        }

        @Override
        protected int getExtraLayoutSpace(RecyclerView.State state) {
            if (extraLayoutSpace > 0) {
                return extraLayoutSpace;
            }
            return DEFAULT_EXTRA_LAYOUT_SPACE;
        }
    }


