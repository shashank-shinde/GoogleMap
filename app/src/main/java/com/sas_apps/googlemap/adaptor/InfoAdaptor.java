package com.sas_apps.googlemap.adaptor;
/*
 * Created by Shashank Shinde.
 */

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;
import com.sas_apps.googlemap.R;

public class InfoAdaptor implements GoogleMap.InfoWindowAdapter {


    private final View mWindow;
    private Context mContext;

    public InfoAdaptor(Context mContext) {
        this.mContext = mContext;
        mWindow = LayoutInflater.from(mContext).inflate(R.layout.layout_info, null);
    }


    private void randomWindowText(Marker marker, View view) {

        String title = marker.getTitle();
        TextView tvTitle = view.findViewById(R.id.text_title);

        if (!title.equals("")) {
            tvTitle.setText(title);
        }

        String snippet = marker.getSnippet();
        TextView tvSnippet = view.findViewById(R.id.text_snippet);

        if (!snippet.equals("")) {
            tvSnippet.setText(snippet);
        }
    }


    @Override
    public View getInfoWindow(Marker marker) {
        randomWindowText(marker, mWindow);
        return mWindow;
    }

    @Override
    public View getInfoContents(Marker marker) {
        randomWindowText(marker, mWindow);
        return mWindow;
    }
}
