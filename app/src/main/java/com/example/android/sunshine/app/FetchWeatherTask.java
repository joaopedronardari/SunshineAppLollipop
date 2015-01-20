package com.example.android.sunshine.app;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import com.example.android.sunshine.app.data.WeatherContract;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by anonymous on 18/01/2015.
 */
public class FetchWeatherTask extends AsyncTask<String,Void,String[]> {

    private String TAG = FetchWeatherTask.class.getSimpleName();

    private String postCode = null;

    private Context context;

    public FetchWeatherTask(Context context) {
        this.context = context;
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
        int numOfDays = 14;
        String locationQuery = params[0];

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

            getWeatherDataFromJson(buffer.toString(), numOfDays, locationQuery);
        } catch (IOException e) {
            Log.e(TAG, "Error IOException" + e.getMessage(), e);
        } catch (JSONException e) {
            Log.e(TAG, "Error JSONEXCEPTION " + e.getMessage(), e);
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

    private long addLocation(String locationSetting, String cityName, double lat, double lng) {

        // Check if location exists in Database
        Cursor cursor = context.getContentResolver().query(
                WeatherContract.LocationEntry.CONTENT_URI,
                new String[]{WeatherContract.LocationEntry._ID},
                WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ?",
                new String[]{locationSetting},
                null
        );

        if (cursor.moveToFirst()) {
            // Exists
            int locationIdIndex = cursor.getColumnIndex(WeatherContract.LocationEntry._ID);
            return cursor.getLong(locationIdIndex);
        } else {

            // Adding values
            ContentValues values = new ContentValues();
            values.put(WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING,locationSetting);
            values.put(WeatherContract.LocationEntry.COLUMN_CITY_NAME,cityName);
            values.put(WeatherContract.LocationEntry.COLUMN_COORD_LAT,lat);
            values.put(WeatherContract.LocationEntry.COLUMN_COORD_LNG,lng);

            // Insert new Location
            Uri locationInsertUri = context.getContentResolver().insert(WeatherContract.LocationEntry.CONTENT_URI, values);
            return ContentUris.parseId(locationInsertUri);
        }
    }

    /**
     * Take the String representing the complete forecast in JSON Format and
     * pull out the data we need to construct the Strings needed for the wireframes.
     *
     * Fortunately parsing is easy:  constructor takes the JSON string and converts it
     * into an Object hierarchy for us.
     */
    private void getWeatherDataFromJson(String forecastJsonStr, int numDays,
                                            String locationSetting)
            throws JSONException {

        // These are the names of the JSON objects that need to be extracted.

        // Location information
        final String OWM_CITY = "city";
        final String OWM_CITY_NAME = "name";
        final String OWM_COORD = "coord";
        final String OWM_COORD_LAT = "lat";
        final String OWM_COORD_LONG = "lon";

        // Weather information.  Each day's forecast info is an element of the "list" array.
        final String OWM_LIST = "list";

        final String OWM_DATETIME = "dt";
        final String OWM_PRESSURE = "pressure";
        final String OWM_HUMIDITY = "humidity";
        final String OWM_WINDSPEED = "speed";
        final String OWM_WIND_DIRECTION = "deg";

        // All temperatures are children of the "temp" object.
        final String OWM_TEMPERATURE = "temp";
        final String OWM_MAX = "max";
        final String OWM_MIN = "min";

        final String OWM_WEATHER = "weather";
        final String OWM_DESCRIPTION = "main";
        final String OWM_WEATHER_ID = "id";

        JSONObject forecastJson = new JSONObject(forecastJsonStr);
        JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

        JSONObject cityJson = forecastJson.getJSONObject(OWM_CITY);
        String cityName = cityJson.getString(OWM_CITY_NAME);
        JSONObject coordJSON = cityJson.getJSONObject(OWM_COORD);
        double cityLatitude = coordJSON.getLong(OWM_COORD_LAT);
        double cityLongitude = coordJSON.getLong(OWM_COORD_LONG);

        Log.v("FetchWeatherTask", cityName + ", with coord: " + cityLatitude + " " + cityLongitude);

        // Insert the location into the database.
        long locationID = addLocation(locationSetting, cityName, cityLatitude, cityLongitude);

        ContentValues[] contentValuesArray = new ContentValues[weatherArray.length()];

        for(int i = 0; i < weatherArray.length(); i++) {
            // Get the JSON object representing the day
            JSONObject dayForecast = weatherArray.getJSONObject(i);

            // Create actual contentValues
            ContentValues values = new ContentValues();

            // Put Values
            values.put(WeatherContract.WeatherEntry.COLUMN_LOC_KEY, locationID);
            long dateTime = dayForecast.getLong(OWM_DATETIME);
            values.put(WeatherContract.WeatherEntry.COLUMN_DATETEXT,getReadableDateString(dateTime));
            values.put(WeatherContract.WeatherEntry.COLUMN_PRESSURE,dayForecast.getDouble(OWM_PRESSURE));
            values.put(WeatherContract.WeatherEntry.COLUMN_HUMIDITY,dayForecast.getDouble(OWM_HUMIDITY));
            values.put(WeatherContract.WeatherEntry.COLUMN_WIND_SPEED,dayForecast.getDouble(OWM_WINDSPEED));
            values.put(WeatherContract.WeatherEntry.COLUMN_DEGREES ,dayForecast.getDouble(OWM_WIND_DIRECTION));

            JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
            values.put(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,temperatureObject.getDouble(OWM_MAX));
            values.put(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,temperatureObject.getDouble(OWM_MIN));

            JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
            values.put(WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,weatherObject.getInt(OWM_WEATHER_ID));
            values.put(WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,weatherObject.getString(OWM_DESCRIPTION));

            contentValuesArray[i] = values;
        }

        context.getContentResolver().bulkInsert(WeatherContract.WeatherEntry.CONTENT_URI, contentValuesArray);
    }

    /* The date/time conversion code is going to be moved outside the asynctask later,
     * so for convenience we're breaking it out into its own method now.
     */
    private String getReadableDateString(long time){
        // Because the API returns a unix timestamp (measured in seconds),
        // it must be converted to milliseconds in order to be converted to valid date.
        Date date = new Date(time * 1000);
        SimpleDateFormat format = new SimpleDateFormat("E, MMM d");
        return format.format(date).toString();
    }

    private String getWritableDateString(long time){
        // Because the API returns a unix timestamp (measured in seconds),
        // it must be converted to milliseconds in order to be converted to valid date.
        Date date = new Date(time * 1000);
        SimpleDateFormat format = new SimpleDateFormat("E, MMM d");
        return format.format(date).toString();
    }
}
