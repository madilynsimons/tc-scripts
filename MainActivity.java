package com.simons.owner.traffickcam2;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.gun0912.tedpicker.ImagePickerActivity;

import org.w3c.dom.Text;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener {

    GoogleApiClient client;
    TextView hotelAddressText;
    EditText hotelNumberText;
    Button submitInfoButton;
    Button editHotelButton;
    ViewGroup selectedItemsContainer;
    ArrayList<Uri> image_uris = new ArrayList<Uri>();
    double longitude = -75.1494, latitude = 39.9816;
    ArrayList<String> nearbyHotels;
    ArrayList<String> hotelVicinities;
    String url;
    int PROXIMITY_RADIUS = 10000;
    AlertDialog dialog;
    String currentHotel = null;
    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        checkLocationPermission();
        setLocation();
        buildGoogleApiClient();

        nearbyHotels = new ArrayList<String>();
        hotelVicinities = new ArrayList<String>();

        selectedItemsContainer = (ViewGroup) findViewById(R.id.selected_photos_container);
        image_uris = getIntent().getParcelableArrayListExtra(ImagePickerActivity.EXTRA_IMAGE_URIS);
        if (image_uris != null) showMedia();
        initButtons();
        progressBar = (ProgressBar) findViewById(R.id.HotelAddProgressBar);
        progressBar.setVisibility(View.VISIBLE);

        hotelAddressText = (TextView) findViewById(R.id.HotelAddressText);
        hotelAddressText.setVisibility(View.INVISIBLE);

        hotelNumberText = (EditText) findViewById(R.id.HotelNumberText);
        hotelNumberText.setHint(getResources().getString(R.string.room_num_optional));
    }


    void setLocation() {
        try {
            LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,new String[] {Manifest.permission.ACCESS_FINE_LOCATION },98);
            }

            Criteria criteria = new Criteria();
            String bestProvider = String.valueOf(lm.getBestProvider(criteria, true));
            Location location = lm.getLastKnownLocation(bestProvider);

            android.location.LocationListener listener = new android.location.LocationListener() {

                @Override
                public void onLocationChanged(Location location) {
                    if(location != null)
                    {
                        longitude = location.getLongitude();
                        latitude = location.getLatitude();
                        searchNearbyHotels("");
                    }
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {

                }

                @Override
                public void onProviderEnabled(String provider) {

                }

                @Override
                public void onProviderDisabled(String provider) {

                }
            };
            if(location != null)
            {
                longitude = location.getLongitude();
                latitude = location.getLatitude();
                searchNearbyHotels("");
            }
            else{
                lm.requestLocationUpdates(bestProvider, (long)1000, (float)0, listener);
            }

        } catch (SecurityException e)
        {
            // TODO
        }
    }

    private void searchNearbyHotels(String keywords)
    {
        HotelData getNearbyPlacesData = new HotelData();

        url = getUrl(keywords, "lodging");

        getNearbyPlacesData.execute();
    }

    private String getUrl(String keywords, String nearbyPlace)
    {

        StringBuilder googlePlaceUrl = new StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json?");
        googlePlaceUrl.append("location="+latitude+","+longitude);
        googlePlaceUrl.append("&radius="+PROXIMITY_RADIUS);
        googlePlaceUrl.append("&type="+nearbyPlace);
        googlePlaceUrl.append("&sensor=true");
        googlePlaceUrl.append("&keyword=" + keywords);
        googlePlaceUrl.append("&key="+"AIzaSyDlTwcKRDanIkhKboghxUS22O79AlF0kfM");

        return googlePlaceUrl.toString();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        LocationRequest locationRequest;
        locationRequest = new LocationRequest();
        locationRequest.setInterval(100);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION ) == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(client, locationRequest, this);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }

    @Override
    public void onLocationChanged(Location location) {
        latitude = location.getLatitude();
        longitude = location.getLongitude();
    }

    protected synchronized void buildGoogleApiClient() {
        client = new GoogleApiClient.Builder(this).addConnectionCallbacks(this).addOnConnectionFailedListener(this).addApi(LocationServices.API).build();
        client.connect();
    }

    public void checkLocationPermission()
    {
        int REQUEST_LOCATION_CODE = 99;
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)  != PackageManager.PERMISSION_GRANTED )
        {
                ActivityCompat.requestPermissions(this,new String[] {Manifest.permission.ACCESS_FINE_LOCATION },REQUEST_LOCATION_CODE);
        }
    }

    private void showMedia() {
        // Remove all views before
        // adding the new ones.
        selectedItemsContainer.removeAllViews();
        if (image_uris.size() >= 1) {
            selectedItemsContainer.setVisibility(View.VISIBLE);
        }

        int wdpx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100, getResources().getDisplayMetrics());
        int htpx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100, getResources().getDisplayMetrics());


        for (Uri uri : image_uris) {

            View imageHolder = LayoutInflater.from(this).inflate(R.layout.image_item, null);
            ImageView thumbnail = (ImageView) imageHolder.findViewById(R.id.media_image);

            Glide.with(this)
                    .load(uri.toString())
                    .fitCenter()
                    .into(thumbnail);

            selectedItemsContainer.addView(imageHolder);

            thumbnail.setLayoutParams(new FrameLayout.LayoutParams(wdpx, htpx));
        }
    }

    public static final int NEWHOTEL = 100;

    void initButtons()
    {
        submitInfoButton = (Button) findViewById(R.id.SubmitInfoButton);
        editHotelButton = (Button) findViewById(R.id.ChangeHotelButton);

        submitInfoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("Photos Uploaded!")
                            .setMessage("Thank you for using TraffickCam!")
                            .setPositiveButton("Close", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    exit();
                                }
                            });
                    AlertDialog dialog = builder.create();
                    String msg = "Please select your hotel using \"Edit\"";
                    // check if hotel name is populated. If it is, show success message
                    if (hotelAddressText.getVisibility() == View.INVISIBLE)
                       Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                    else
                        dialog.show();
            }
        });

        editHotelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO HOTEL ACTIVITY
                if(nearbyHotels.size() == 0)
                {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("No nearby hotels found...")
                            .setMessage("It doesn't look like you're near any hotels.  Please try checking your internet connection or check that TraffickCam has Location permissions in Settings")
                            .setPositiveButton("Okay", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            });
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }else{
                    Intent intent = new Intent(MainActivity.this, ChangeHotelActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putStringArrayList("hotels", nearbyHotels);
                    intent.putExtras(bundle);
                    startActivityForResult(intent, NEWHOTEL);
                }
            }
        });

    }

    void exit()
    {
        Intent ret = new Intent();
        ret.putExtra("finished", 1);
        setResult(Activity.RESULT_OK, ret);
        finish();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == NEWHOTEL) {
            if(resultCode == Activity.RESULT_OK)
            {
                currentHotel = data.getStringExtra("hotelresult");
                hotelAddressText.setText(currentHotel);
            }
        }
    }

    public class HotelData extends AsyncTask<Object, String, String> {

        private String googlePlacesData;
        public List<HashMap<String, String>> nearbyPlaceList;

        @Override
        protected String doInBackground(Object... objects)
        {
            try
            {
                DownloadURL downloadURL = new DownloadURL();
                googlePlacesData = downloadURL.readUrl(url);
            }catch(IOException e)
            {
                // TODO
            }
            return googlePlacesData;
        }

        @Override
        protected void onPostExecute(String s){
            DataParser parser = new DataParser();
            nearbyPlaceList = parser.parse(s);
            showNearbyPlaces(nearbyPlaceList);
        }

        private void showNearbyPlaces(List<HashMap<String, String>> nearbyPlaceList)
        {
           // Toast.makeText(MainActivity.this, Integer.toString(nearbyPlaceList.size()), Toast.LENGTH_LONG).show();
            if(nearbyPlaceList.size() > 0)
            {
                if(currentHotel == null)
                {
                    currentHotel = nearbyPlaceList.get(0).get("place_name");
                    hotelAddressText.setText(currentHotel);
                    progressBar.setVisibility(View.INVISIBLE);
                    hotelAddressText.setVisibility(View.VISIBLE);
                }
            }

            for(int i = 0; i < nearbyPlaceList.size(); i++)
            {
                HashMap<String, String> googlePlace = nearbyPlaceList.get(i);
                String placeName = googlePlace.get("place_name");
                String vicinity = googlePlace.get("vicinity");
                nearbyHotels.add(placeName);
                hotelVicinities.add(vicinity);
            }
        }
    }



}
