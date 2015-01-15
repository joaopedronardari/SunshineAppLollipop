package com.example.android.sunshine.app;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class ForecastFragment extends Fragment {

    ArrayAdapter<String> mForecastAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        // Data List
        ArrayList<String> weekForecast = new ArrayList<String>();
        weekForecast.add("Today - Sunny - 88/63");
        weekForecast.add("Tomorrow - Foggy - 70/46");
        weekForecast.add("Weds - Cloudy - 72/63");
        weekForecast.add("Thurs - Rainy - 64/51");
        weekForecast.add("Fri - Foggy - 70/46");
        weekForecast.add("Sat - Sunny - 76/48");

        // ListView Adapter
        mForecastAdapter = new ArrayAdapter<String>(
                getActivity(),
                R.layout.list_item_forecast,
                R.id.list_item_forecast_textview,
                weekForecast);

        // Search listview in tree
        ListView listView = (ListView) rootView.findViewById(R.id.listview_forecast);
        // Set Adapter
        listView.setAdapter(mForecastAdapter);
        // Set click
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Calling new activity
                Intent intent = new Intent(getActivity(),DetailActivity.class);
                intent.putExtra(Intent.EXTRA_TEXT, mForecastAdapter.getItem(position));
                startActivity(intent);
            }
        });

        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_refresh:
                updateWeather();
                return true;
            case R.id.action_map:
                Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
                        Uri.parse("geo:0,0?q=" + getPreferenceLocation()));
                startActivity(intent);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showMap(Uri geoLocation) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(geoLocation);
        if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    private void updateWeather() {
        FetchWeatherTask weatherTask = new FetchWeatherTask();
        weatherTask.execute(getPreferenceLocation());
    }

    private String getPreferenceLocation() {
        return PreferenceManager.
                getDefaultSharedPreferences(getActivity()).
                getString(getString(R.string.pref_location_key), "");
    }

    @Override
    public void onStart() {
        super.onStart();
        updateWeather();
    }

    public class FetchWeatherTask extends AsyncTask<String,Void,String[]> {

        private String TAG = FetchWeatherTask.class.getSimpleName();

        private String postCode = null;

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
                    getDefaultSharedPreferences(getActivity()).
                    getString(getString(R.string.pref_unit_key),"metric");
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
}
