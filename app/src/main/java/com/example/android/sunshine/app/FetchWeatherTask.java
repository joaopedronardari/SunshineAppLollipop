package com.example.android.sunshine.app;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.ArrayAdapter;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by anonymous on 18/01/2015.
 */
public class FetchWeatherTask extends AsyncTask<String,Void,String[]> {

    private String TAG = FetchWeatherTask.class.getSimpleName();

    private String postCode = null;

    private Context context;
    private ArrayAdapter<String>  mForecastAdapter;

    public FetchWeatherTask(Context context, ArrayAdapter<String> mForecastAdapter) {
        this.context = context;
        this.mForecastAdapter = mForecastAdapter;
    }

    @Override
    protected String[] doInBackground(String... params) {

        if (params.length == 0)
            return null;

        // CodeSnip - Lesson 2
        // These two need to be declared outside the try/catch
        // so that they can be closed in the finally block.
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        // Will contain the raw JSON response as a string.
        //String forecastJsonStr = null;
        String[] weatherForecasts = null;

        String format = "json";
        String unit = PreferenceManager.
                getDefaultSharedPreferences(context).
                getString(context.getString(R.string.pref_unit_key),"metric");
        int numOfDays = 7;

        try {
            // Construct the URL for the OpenWeatherMap query
            // Possible parameters are available at OWM's forecast API page, at
            // http://openweathermap.org/API#forecast

            final String QUERY_PARAM = "q";
            final String MODE_PARAM = "mode";
            final String UNITS_PARAM = "units";
            final String DAYS_PARAM = "cnt";

            Uri.Builder builder = new Uri.Builder();
            builder.scheme("http")
                    .authority("api.openweathermap.org")
                    .appendPath("data")
                    .appendPath("2.5")
                    .appendPath("forecast")
                    .appendPath("daily")
                    .appendQueryParameter(QUERY_PARAM, params[0])
                    .appendQueryParameter(MODE_PARAM, format)
                    .appendQueryParameter(UNITS_PARAM, unit)
                    .appendQueryParameter(DAYS_PARAM, String.valueOf(numOfDays));

            Log.i(TAG, builder.build().toString());
            URL url = new URL(builder.build().toString());

            // Create the request to OpenWeatherMap, and open the connection
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                return null;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                // But it does make debugging a *lot* easier if you print out the completed
                // buffer for debugging.
                buffer.append(line + "\n");
            }

            if (buffer.length() == 0) {
                return null;
            }

            return Util.getWeatherDataFromJson(buffer.toString(),numOfDays);
        } catch (IOException e) {
            Log.e(TAG, "Error ", e);
        } catch (JSONException e) {
            Log.e(TAG, "Error ", e);
        } finally{
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(TAG, "Error closing stream", e);
                }
            }
        }

        return null;
    }

    @Override
    protected void onPostExecute(String[] result) {
        if (result != null) {
            mForecastAdapter.clear();

            for (String weather : result) {
                mForecastAdapter.add(weather);
            }
        }
    }
}
