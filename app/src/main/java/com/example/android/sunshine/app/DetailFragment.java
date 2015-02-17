package com.example.android.sunshine.app;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.android.sunshine.app.data.WeatherContract;

/**
 * A placeholder fragment containing a simple view.
 */
public class DetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String LOG_TAG = DetailFragment.class.getSimpleName();

    private static final String FORECAST_SHARE_HASHTAG = "#SunshineApp";
    private String mForecastStr;
    private String mSelectedDate;

    private View rootView;

    private static final String[] WEATHER_COLUMNS = {
            // In this case the id needs to be fully qualified with a table name, since
            // the content provider joins the location & weather tables in the background
            // (both have an _id column)
            // On the one hand, that's annoying.  On the other, you can search the weather table
            // using the location set by the user, which is only in the Location table.
            // So the convenience is worth it.
            WeatherContract.WeatherEntry.TABLE_NAME + "." + WeatherContract.WeatherEntry._ID,
            WeatherContract.WeatherEntry.COLUMN_DATETEXT,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING,
            WeatherContract.WeatherEntry.COLUMN_HUMIDITY,
            WeatherContract.WeatherEntry.COLUMN_WIND_SPEED,
            WeatherContract.WeatherEntry.COLUMN_DEGREES,
            WeatherContract.WeatherEntry.COLUMN_PRESSURE
    };

    private static final int WEATHER_LOADER = 0;
    private String mLocation;

    public static final int COL_WEATHER_ID = 0;
    public static final int COL_WEATHER_DATE = 1;
    public static final int COL_WEATHER_DESC = 2;
    public static final int COL_WEATHER_MAX_TEMP = 3;
    public static final int COL_WEATHER_MIN_TEMP = 4;
    public static final int COL_LOCATION_SETTING = 5;

    // Other info
    public static final int COL_WEATHER_HUMIDITY = 6;
    public static final int COL_WEATHER_WIND_SPEED = 7;
    public static final int COL_WEATHER_DEGREES = 8;
    public static final int COL_WEATHER_PRESSURE = 9;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(WEATHER_LOADER, null, this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.detailfragment, menu);

        MenuItem menuItem = menu.findItem(R.id.action_share);

        ShareActionProvider mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(menuItem);

        if (mShareActionProvider != null) {
            mShareActionProvider.setShareIntent(createShareIntent());
        } else {
            Log.d(LOG_TAG, "ShareActionProvider is null");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_detail, container, false);
        return rootView;
    }

    private Intent createShareIntent() {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);

        // FIXME - Deprecated
        sendIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        sendIntent.putExtra(Intent.EXTRA_TEXT, mForecastStr + " " + FORECAST_SHARE_HASHTAG);
        sendIntent.setType("text/plain");
        return sendIntent;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Intent intent = getActivity().getIntent();

        if (intent != null && intent.hasExtra(WeatherContract.WeatherEntry.COLUMN_DATETEXT)) {
            mLocation = Util.getPreferenceLocation(getActivity());
            Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(
                    mLocation, intent.getStringExtra(WeatherContract.WeatherEntry.COLUMN_DATETEXT));

            return new CursorLoader(
                    getActivity(),
                    weatherForLocationUri,
                    WEATHER_COLUMNS,
                    null,
                    null,
                    null
            );
        }

        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor.moveToFirst()) {

            // Get Layout References
            TextView dateTextView      = (TextView) rootView.findViewById(R.id.fragment_detail_date_textview);
            TextView forecastTextView  = (TextView) rootView.findViewById(R.id.fragment_detail_forecast_textview);
            TextView highTextView      = (TextView) rootView.findViewById(R.id.fragment_detail_high_textview);
            TextView lowTextView       = (TextView) rootView.findViewById(R.id.fragment_detail_low_textview);

            ImageView icon             = (ImageView) rootView.findViewById(R.id.fragment_detail_icon);

            TextView humidityTextView  = (TextView) rootView.findViewById(R.id.fragment_detail_humidity_textview);
            TextView windTextView      = (TextView) rootView.findViewById(R.id.fragment_detail_wind_textview);
            TextView pressureTextView  = (TextView) rootView.findViewById(R.id.fragment_detail_pressure_textview);

            // Get Data from cursor
            String weatherDate =  cursor.getString(COL_WEATHER_DATE);
            String weatherDesc = cursor.getString(COL_WEATHER_DESC);
            float weatherMaxTemp = cursor.getFloat(COL_WEATHER_MAX_TEMP);
            float weatherMinTemp = cursor.getFloat(COL_WEATHER_MIN_TEMP);
            float weatherHumidity = cursor.getFloat(COL_WEATHER_HUMIDITY);
            float weatherWindSpeed = cursor.getFloat(COL_WEATHER_WIND_SPEED);
            float weatherDegrees = cursor.getFloat(COL_WEATHER_DEGREES);
            float weatherPressure = cursor.getFloat(COL_WEATHER_PRESSURE);

            // Share String
            mForecastStr = weatherDate + " - " +
                    weatherDesc + " - " +
                    weatherMaxTemp + "/" +
                    weatherMinTemp;

            Context context = getActivity();

            // Populate views
            dateTextView.setText(Util.getFriendlyDayString(context, weatherDate));
            forecastTextView.setText(weatherDesc);

            boolean isMetric = Util.isMetric(context);
            highTextView.setText(Util.formatTemperature(context, weatherMaxTemp, isMetric));
            lowTextView.setText(Util.formatTemperature(context, weatherMinTemp, isMetric));

            humidityTextView.setText(context.getString(R.string.format_humidity, weatherHumidity));
            windTextView.setText(Util.getFormattedWind(context, weatherWindSpeed, weatherDegrees));
            pressureTextView.setText(context.getString(R.string.format_pressure, weatherPressure));
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }
}