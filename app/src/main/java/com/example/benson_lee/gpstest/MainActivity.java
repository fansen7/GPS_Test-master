package com.example.benson_lee.gpstest;

import android.Manifest;
import android.app.IntentService;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Message;
import android.os.ResultReceiver;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONObject;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;


import static android.widget.Toast.makeText;

public class MainActivity extends AppCompatActivity implements ConnectionCallbacks
        , OnConnectionFailedListener, LocationListener, AddressResultReceiver.Receiver,OnMapReadyCallback {

    double latitude=0.0;
    double longitude = 0.0;
    double speedValue = 0.0;
    double AccuracyValue = 0.0;
    String License_plate = "test-1130";
    private String mAddressOutput;

    // Debug tag
    private static final String TAG = MainActivity.class.getSimpleName();

    // Request code
    private final static int PLAY_SERVICES_REQUEST = 1000;

    // Location
    private Location mLastLocation;

    // Google API Client
    private GoogleApiClient mGoogleApiClient;

    // boolean flag to toggle periodic location updates
    private boolean mRequestingLocationUpdates = false;
    // Location request
    private LocationRequest mLocationRequest;
    // Location updates intervals
    private static int UPDATE_INTERVAL = 800; // 0.8 sec
    private static int FATEST_INTERVAL = 500; // 0.5 sec
    private static int DISPLACEMENT = 3; // 3 meters


    int T = 0;


    //HTTP
    String urlString = "http://122.116.45.4/";
    public final String USER_AGENT = "Mozilla/5.0";
    private Date NowTime_ = new Date();






    // UI
    public TextView lblLocation, lblAddress, lblSpeed,Caution,Accuracy ,License,status;

    private Button  button1,btnStartLocationUpdates,UpdateLicense;
    private EditText Licenseedit1, Licenseedit2;

    // Media player

    MediaPlayer mMedia;
    private boolean isMediaPlaying;

    // Receive the address data
    private AddressResultReceiver mResultReceiver;

  //  private Handler mHandler ;
  private  GoogleMap MYmap ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        Thread t = new HttpPost();
        t.start();

        // initialization

        isMediaPlaying = false;


        // UI assign
        lblLocation = (TextView) findViewById(R.id.lblLocation);
        lblSpeed = (TextView) findViewById(R.id.lblSpeed);
        lblAddress = (TextView) findViewById(R.id.lblAddress);
        Caution = (TextView) findViewById(R.id.Caution);
        Accuracy= (TextView) findViewById(R.id.Accuracy);
        License= (TextView) findViewById(R.id.License);
        Licenseedit1 = (EditText) findViewById(R.id.Licenseedit1);
        Licenseedit2 = (EditText) findViewById(R.id.Licenseedit2);
        status = (TextView) findViewById(R.id.Status);

        UpdateLicense = (Button) findViewById(R.id.Update_License);
        button1 = (Button) findViewById(R.id.button1);
        btnStartLocationUpdates = (Button) findViewById(R.id.btnStartLocationUpdates);


        // Create Google Api Client object
        if (checkPlayServices()) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        // Create location request object
        if (checkPlayServices()) {
            mLocationRequest = new LocationRequest()
                    .setInterval(UPDATE_INTERVAL)
                    .setFastestInterval(FATEST_INTERVAL)
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                    .setSmallestDisplacement(DISPLACEMENT);

        }


        // Show Location


        btnStartLocationUpdates.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {togglePeriodicLocationUpdates();
            }
        });
        button1.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                displayLocation();

            }
        });
        UpdateLicense.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {UpdateLicense();
            }
        });
License.setText("License : "+ License_plate);



        // Address receiver
        mResultReceiver = new AddressResultReceiver(new Handler());
        mResultReceiver.setReceiver(this);
        displayLocation();

        //-------------------------
        MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        //  -----------------------------

        btnStartLocationUpdates.setText(getString(R.string.btn_stop_location_updates));
        mRequestingLocationUpdates = true;
status.setText("");


    }



    @Override
    protected void onStart() {
    
        super.onStart();

        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();



        checkPlayServices();

        // Resuming the periodic location updates
        if (mGoogleApiClient.isConnected() && mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        stopLocationUpdates();
    }

    // Method to verify Google Play services on the device
    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_REQUEST).show();
            } else {
                makeText(getApplicationContext(), "This device is not supported.", Toast.LENGTH_LONG)
                        .show();
                finish();
            }
            return false;
        }
        return true;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        displayLocation();

        if (mRequestingLocationUpdates)
        {
            startLocationUpdates();

        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + connectionResult.getErrorCode());
    }

    public void displayLocation() {
if(MYmap!= null)
{
    MYmap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), 17));

}


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        if (mLastLocation != null)
        {
            latitude = mLastLocation.getLatitude();
            longitude = mLastLocation.getLongitude();
            speedValue = mLastLocation.getSpeed();
            AccuracyValue = mLastLocation.getAccuracy();
            String location = getString(R.string.lbl_location, latitude, longitude);
            String speed = getString(R.string.lbl_speed, mLastLocation.getSpeed()*3600.0/1000.0);


            lblLocation.setText(location);
            lblSpeed.setText(speed+" km/h");
            Accuracy.setText("Accuracy :" + AccuracyValue + " m" );
            startIntentService();

            // Show the map
        /*    final double longOffset = 0.008959, latOffset = 0.004161;
            String westBorder = String.valueOf(longitude - longOffset);
            String eastBorder = String.valueOf(longitude + longOffset);
            String southBorder = String.valueOf(latitude - latOffset);
            String northBorder = String.valueOf(latitude + latOffset);

            webMap.getSettings().setJavaScriptEnabled(true);
            String html = "<iframe width=\"280\" height=\"400\" frameborder=\"0\" scrolling=\"no\" "
                    + "marginheight=\"0\" marginwidth=\"0\" src=\"http://www.openstreetmap.org/export/embed.html?bbox="
                    + westBorder + "%2C" + southBorder + "%2C" + eastBorder + "%2C" + northBorder
                    + "&amp;layer=mapnik&amp;marker=" + String.valueOf(latitude) + "%2C"
                    + String.valueOf(longitude) + "\" style=\"border: 1px solid black\"/>";
            webMap.loadData(html, "text/html", null);
          */
        }
        else
        {
            lblLocation.setText(R.string.err_couldnt_get_location);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        // Assign the new location
        mLastLocation = location;

      //  Toast.makeText(getApplicationContext(), "Location changed!",
      //         Toast.LENGTH_SHORT).show();

        // Displaying the new location on UI
        displayLocation();
    }

    // Method to toggle periodic location updates
    private void togglePeriodicLocationUpdates() {
        if (!mRequestingLocationUpdates) {
            // Changing the button text
            btnStartLocationUpdates
                    .setText(getString(R.string.btn_stop_location_updates));

            mRequestingLocationUpdates = true;

            // Starting the location updates
            startLocationUpdates();

            Log.d(TAG, "Periodic location updates started!");
        } else {
            // Changing the button text
            btnStartLocationUpdates
                    .setText(getString(R.string.btn_start_location_updates));

            mRequestingLocationUpdates = false;

            // Stopping the location updates
            stopLocationUpdates();

            Log.d(TAG, "Periodic location updates stopped!");
        }
    }

    // Starting the location updates
    protected void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    // Stopping location updates
    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    // Toggle playing media
    protected void toggleMedia() {
        if (!isMediaPlaying) {
            mMedia.seekTo(0);
            mMedia.start();
        }
        else {
            mMedia.stop();
            try {
                mMedia.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        isMediaPlaying = !isMediaPlaying;
    }

    // Geocoder intent
    protected void startIntentService() {
        Intent intent = new Intent(this, FetchAddressIntentService.class);
        intent.putExtra(Constants.RECEIVER, mResultReceiver);
        intent.putExtra(Constants.LOCATION_DATA_EXTRA, mLastLocation);

        startService(intent);
    }

    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
        mAddressOutput = resultData.getString(Constants.RESULT_DATA_KEY);

     /*   if(mAddressOutput.contains("弄"))
        {

        }
        else if(mAddressOutput.contains("巷")){

        }
        else if(mAddressOutput.contains("段")){

        }
        else if(mAddressOutput.contains("街")){

        }
        else if(mAddressOutput.contains("路")){

        }

*/




        lblAddress.setText(mAddressOutput);
    }

    @Override
    public void onMapReady(GoogleMap map) {
        this.MYmap = map;


        LatLng home = new LatLng(25.000320, 121.545608);
        LatLng NOW = new LatLng(latitude, longitude);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        map.setMyLocationEnabled(true);
//----------------------------------------------------------
    /*    map.addMarker(new MarkerOptions()
                .title("Home")
                .snippet("My Home.")
                .position(home));*/
//----------------------------------------------------------
        PolylineOptions Line = new PolylineOptions()
                .add(new LatLng(24.958333, 121.446849))
                .add(new LatLng(24.958092, 121.446582))
                .add(new LatLng(24.958000, 121.446561))
                .add(new LatLng(24.956974, 121.447006))
                .add(new LatLng(24.956813, 121.446985))
                .add(new LatLng(24.956691, 121.446872))
                .width(15);//南天母路65巷


        map.addPolyline(Line).setColor(Color.RED);

        Line = new PolylineOptions()
                .add(new LatLng(24.956151, 121.447473))
                .add(new LatLng(24.956996, 121.447421))
                .width(15);//南天母路91巷

        map.addPolyline(Line).setColor(Color.RED);

        Line = new PolylineOptions()
                .add(new LatLng(24.957101, 121.447818))
                .add(new LatLng(24.955754, 121.447914))

                .width(15);//南天母路97巷

        map.addPolyline(Line).setColor(Color.RED);

        Line = new PolylineOptions()
                .add(new LatLng(24.955601, 121.447307))
                .add(new LatLng(24.955857, 121.446983))
                .add(new LatLng(24.955893, 121.446755))
                .width(15);//南天母路52巷

        map.addPolyline(Line).setColor(Color.RED);


        Line = new PolylineOptions()
                .add(new LatLng(24.954852, 121.447900))
                .add(new LatLng(24.954860, 121.448114))
                .add(new LatLng(24.955000, 121.448406))

                .add(new LatLng(24.954961, 121.448511))
                .add(new LatLng(24.954956, 121.448583))
                .add(new LatLng(24.954968, 121.448659))
                .add(new LatLng(24.954994, 121.448719))
                .add(new LatLng(24.955030, 121.448762))
                .add(new LatLng(24.955470, 121.449090))
                .add(new LatLng(24.955672, 121.449345))
                .width(15);//南天母路111巷

        map.addPolyline(Line).setColor(Color.RED);


        Line = new PolylineOptions()
                .add(new LatLng(24.956398, 121.450724))
                .add(new LatLng(24.956371, 121.450679))
                .add(new LatLng(24.956277, 121.450627))
                .add(new LatLng(24.956065, 121.450562))
                .add(new LatLng(24.955866, 121.450461))
                .add(new LatLng(24.955680, 121.450231))
                .add(new LatLng(24.955324, 121.450127))
                .add(new LatLng(24.955214, 121.450017))
                .add(new LatLng(24.954855, 121.449288))

                .add(new LatLng(24.954167, 121.449513))
                .add(new LatLng(24.953892, 121.449435))
                .add(new LatLng(24.953729, 121.449341))
                .add(new LatLng(24.953549, 121.449341))
                .add(new LatLng(24.953085, 121.449572))
                .add(new LatLng(24.952735, 121.449709))
                .add(new LatLng(24.952419, 121.449739))
                .add(new LatLng(24.952196, 121.449706))

                .width(15);//南天母路171巷

        map.addPolyline(Line).setColor(Color.RED);

        Line = new PolylineOptions()
                .add(new LatLng(24.951099, 121.447953))
                .add(new LatLng(24.950779, 121.448304))
                .add(new LatLng(24.949619, 121.449324))

                .add(new LatLng(24.949546, 121.449385))
                .add(new LatLng(24.949454, 121.449431))
                .add(new LatLng(24.949283, 121.449496))
                .width(15);//承天路

        map.addPolyline(Line).setColor(Color.RED);

        Line = new PolylineOptions()
                .add(new LatLng(24.952966, 121.447696))
                .add(new LatLng(24.953235, 121.447448))
                .add(new LatLng(24.953389, 121.447169))
                .add(new LatLng(24.953496, 121.446877))
                .add(new LatLng(24.953664, 121.446711))
                .add(new LatLng(24.953888, 121.446708))
                .add(new LatLng(24.954262, 121.446544))
                .add(new LatLng(24.954284, 121.446260))


                .add(new LatLng(24.954128, 121.446226))
                .add(new LatLng(24.954323, 121.445905))
                .add(new LatLng(24.954374, 121.445897))
                .add(new LatLng(24.955021, 121.445948))
                .add(new LatLng(24.955035, 121.446071))
                .add(new LatLng(24.954999, 121.446184))
                .add(new LatLng(24.954398, 121.446181))
                .add(new LatLng(24.954272, 121.446259))

                .width(15);//承天路107巷

        map.addPolyline(Line).setColor(Color.RED);

        Line = new PolylineOptions()
                .add(new LatLng(24.954997, 121.4461817))
                .add(new LatLng(24.955150, 121.446331))
                .width(15);
        map.addPolyline(Line).setColor(Color.RED); //承天路107巷

        Line = new PolylineOptions()
                .add(new LatLng(24.955033, 121.445948))
                .add(new LatLng(24.955055, 121.445842))
                .add(new LatLng(24.955138, 121.445732))
                .add(new LatLng(24.955522, 121.445641))
                .add(new LatLng(24.955797, 121.445692))
                .add(new LatLng(24.955867, 121.445684))
                .add(new LatLng(24.956065, 121.445376))
                .add(new LatLng(24.956334, 121.445251))


                .add(new LatLng(24.956632, 121.445215))
                .add(new LatLng(24.956865, 121.445018))
                .add(new LatLng(24.957180, 121.444891))
                .add(new LatLng(24.957420, 121.444632))
                .add(new LatLng(24.957578, 121.444552))
                .add(new LatLng(24.958113, 121.444531))


                .width(15);//承天路(利益停車場)

        map.addPolyline(Line).setColor(Color.RED);

//--------------------------------------------------------

        Line = new PolylineOptions()
                .add(new LatLng(25.005379, 121.470312))
                .add(new LatLng(25.004325, 121.470705))
                .width(15);//民生街35巷

        map.addPolyline(Line).setColor(Color.RED);

        Line = new PolylineOptions()
                .add(new LatLng(25.004228, 121.470749))
                .add(new LatLng(25.003187, 121.471139))
                .width(15);//德光路27巷

        map.addPolyline(Line).setColor(Color.RED);


        Line = new PolylineOptions()
                .add(new LatLng(25.004767, 121.472040))
                .add(new LatLng(25.005764, 121.471911))
                .width(15);//莒光路

        map.addPolyline(Line).setColor(Color.RED);



        Line = new PolylineOptions()
                .add(new LatLng(25.003461, 121.468772))
                .add(new LatLng(25.003049, 121.467702))
                .add(new LatLng(25.002807, 121.466440))
                .add(new LatLng(25.002627, 121.465041))
                .add(new LatLng(25.002467, 121.463979))
                .add(new LatLng(25.002263, 121.462724))
                .width(15);//忠孝路

        map.addPolyline(Line).setColor(Color.RED);



        Line = new PolylineOptions()
                .add(new LatLng(25.002666, 121.465357))
                .add(new LatLng(25.002550, 121.465366))
                .add(new LatLng(25.001388, 121.468324))
                .add(new LatLng(25.001177, 121.468826))

                .width(15);//國光街

        map.addPolyline(Line).setColor(Color.RED);


        Line = new PolylineOptions()
                .add(new LatLng(25.001103, 121.468911))
                .add(new LatLng(25.001029, 121.469181))
                .add(new LatLng(25.000903, 121.469436))
                .add(new LatLng(25.000530, 121.470045))

                .add(new LatLng(25.000424, 121.470195))
                .add(new LatLng(25.000317, 121.470322))
                .add(new LatLng(25.000226, 121.470619))
                .add(new LatLng(25.000248, 121.470754))
                .add(new LatLng(25.000272, 121.471181))
                .add(new LatLng(25.000308, 121.472719))

                .width(15);//國光街

        map.addPolyline(Line).setColor(Color.RED);



        Line = new PolylineOptions()
                .add(new LatLng(24.998704, 121.465351))
                .add(new LatLng(24.998621, 121.465760))
                .add(new LatLng(24.998191, 121.466624))
                .add(new LatLng(24.997593, 121.467254))

                .add(new LatLng(24.997450, 121.467399))
                .add(new LatLng(24.996904, 121.467855))

                .width(15);//重慶路269巷 - 華安街

        map.addPolyline(Line).setColor(Color.RED);

        Line = new PolylineOptions()
                .add(new LatLng(24.996743, 121.467907))
                .add(new LatLng(24.996496, 121.468109))
                .add(new LatLng(24.994775, 121.469008))
                .width(15);// 華安街

        map.addPolyline(Line).setColor(Color.RED);



        Line = new PolylineOptions()
                .add(new LatLng(24.994648, 121.469033))
                .add(new LatLng(24.993748, 121.469153))
                .add(new LatLng(24.993344, 121.469072))

                .add(new LatLng(24.993095, 121.469100))
                .add(new LatLng(24.992809, 121.469126))
                .add(new LatLng(24.992559, 121.469233))
                .add(new LatLng(24.992385, 121.469414))
                .add(new LatLng(24.992144, 121.469832))
                .width(15);// 延壽路

        map.addPolyline(Line).setColor(Color.RED);



        Line = new PolylineOptions()
                .add(new LatLng(24.992349, 121.467948))
                .add(new LatLng(24.992955, 121.467788))
                .add(new LatLng(24.993501, 121.467499))

                .width(15);// 延平街

        map.addPolyline(Line).setColor(Color.RED);


        Line = new PolylineOptions()
                .add(new LatLng(24.992472, 121.464205))
                .add(new LatLng(24.993005, 121.465109))
                .add(new LatLng(24.993443, 121.465772))
                .add(new LatLng(24.993789, 121.466197))
                .add(new LatLng(24.994239, 121.466704))

                .width(15);// 民德路 - '廣權路

        map.addPolyline(Line).setColor(Color.RED);


        Line = new PolylineOptions()
                .add(new LatLng(24.994298, 121.466711))
                .add(new LatLng(24.994636, 121.466355))
                .add(new LatLng(24.995115, 121.465891))
                .add(new LatLng(24.995222, 121.465730))
                .add(new LatLng(24.995703, 121.464805))

                .width(15);//壽德街
        map.addPolyline(Line).setColor(Color.RED);


      /*  Line = new PolylineOptions()
                .add(new LatLng(25.000566, 121.545263))
                .add(new LatLng(25.000250, 121.545406)).width(25);
        map.addPolyline(Line).setColor(Color.YELLOW);

        Line = new PolylineOptions()
                .add(new LatLng(25.000007, 121.545520))
                .add(new LatLng(25.000250, 121.545406));
        Line.color(Color.GREEN).width(25);
        */

    }



    public void CautiondispA() {



        Toast.makeText(getApplicationContext(), "您即將超速",
                Toast.LENGTH_SHORT).show();


    }
    public void CautiondispB() {



        Toast.makeText(getApplicationContext(), "您已經超速",
                Toast.LENGTH_SHORT).show();

    }
    public void CautiondispC() {



        Toast.makeText(getApplicationContext(), "您已經逆向行駛",
                Toast.LENGTH_SHORT).show();

    }
    public void CautiondispD() {



        Toast.makeText(getApplicationContext(), "您已經進入禁行區域",
                Toast.LENGTH_SHORT).show();

    }
    public void CautiondispE() {



        Toast.makeText(getApplicationContext(), "您已經違規左轉",
                Toast.LENGTH_SHORT).show();

    }

    public void Cautiondisp(String M) {



        Toast.makeText(getApplicationContext(), M,
                Toast.LENGTH_SHORT).show();

    }
    public void UpdateLicense() {

if((!Licenseedit1.getText().equals(""))&&(!Licenseedit1.getText().equals(""))) {
    String newLicense = Licenseedit1.getText() + "-" + Licenseedit2.getText();
    License_plate =  newLicense;
    License.setText("License : "+ License_plate);

}

    }





   class HttpPost extends Thread
    {
        private MediaPlayer mMediaA = MediaPlayer.create(MainActivity.this,R.raw.eventa);
        private MediaPlayer mMediaB = MediaPlayer.create(MainActivity.this,R.raw.eventb);
        private MediaPlayer mMediaC = MediaPlayer.create(MainActivity.this,R.raw.eventc);
        private MediaPlayer mMediaD = MediaPlayer.create(MainActivity.this,R.raw.eventd);
        private MediaPlayer mMediaE = MediaPlayer.create(MainActivity.this,R.raw.evente);
        private MediaPlayer mMediaF = MediaPlayer.create(MainActivity.this,R.raw.eventf);
        private MediaPlayer mMediaG = MediaPlayer.create(MainActivity.this,R.raw.eventg);
        private MediaPlayer mMediaH = MediaPlayer.create(MainActivity.this,R.raw.eventh);
        private MediaPlayer mMediaI = MediaPlayer.create(MainActivity.this,R.raw.eventi);
        private MediaPlayer mMediaJ = MediaPlayer.create(MainActivity.this,R.raw.eventj);
        private boolean isplay = false;
        private Object A = false;
        private Object B= false;
        private Object C= false;
        private Object D= false;
        private Object E= false;
        private Object F= false;
        private Object G= false;
        private Object H= false;
        private Object I= false;
        private Object J= false;
       private int sampling = 500;
        private int get_time = 500;
       private String CautionMessege = "";
        int playdelay = 500;
        // String url = "http://10.0.2.2/";//host
       // String url = "http://122.116.45.4/";
         String url = "http://140.118.127.50/";

        private ArrayList DataBuffer = new ArrayList();


        @Override
        public void run() {

            for (;;) { //
          //  for (int i = 0;i<15;i++) { // infinite loop to print message
                try {


                  sendPost();
                // if(i<15)
                  //   sendPostTest(i%15);
                  Thread.sleep(sampling);
                  GetMessagebyPost();
                  Thread.sleep(get_time);

                } catch (Exception ex) {
                   Log.e(TAG,"123" + ex.getMessage());
        if(ex.toString().contains("connect")&&ex.toString().contains("fail"))
        {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        public void run() {

                            status.setText("disConnected");
                        }
                    });

                }
            }).start();
        }
                }

            }
        }
        // HTTP POST request
        private void sendPost() throws Exception {

            if(speedValue*3600.0/1000.0 <2.0)
            {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                String speed = getString(R.string.lbl_speed, speedValue*3600.0/1000.0);



                                lblSpeed.setText(speed+" km/h");
                           //     displayLocation();
                            }
                        });

                    }
                }).start();
            }


            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            try{

               con.setConnectTimeout(5000);
                //add reuqest header
                con.setRequestMethod("POST");
                con.setRequestProperty("User-Agent", USER_AGENT);
                con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

                //格式化時間字串
                SimpleDateFormat formatter =new SimpleDateFormat("yyyy/MM/dd  HH:mm:ss");
                NowTime_.setTime(System.currentTimeMillis());
                String serverTime =  formatter.format(NowTime_);


                Random ran = new Random();
                String jsondata;// = "{\"Data\":{\"Name\":\"MichaelChan\",\"Email\":\"XXXX@XXX.com\",\"Phone\":[1234567,0911123456]}}";

                jsondata= String.format("{\"Lic\": \"%s\" , \"LOG\" : %f , \"LAT\": %f, \"Speed\": %f ,\"Address\": \"%s\" ,\"Time\": \"%s\"}",
                       License_plate,longitude,latitude,speedValue*3600.0/1000.0,mAddressOutput,serverTime);
                jsondata = URLEncoder.encode(jsondata, "UTF-8");

                String urlParameters;// = "id=1234&pass=asd45";

                urlParameters ="Data "+jsondata;
               // DataBuffer.add(urlParameters);



                // Send post request
                con.setDoOutput(true);
                DataOutputStream wr = new DataOutputStream(con.getOutputStream());
                wr.writeBytes(urlParameters);
                wr.flush();
                wr.close();


                //取得回應訊息
                int responseCode = con.getResponseCode();


                BufferedReader in = new BufferedReader(
                        new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                if(response.toString().equals("ACK"))
               // if(con.getResponseCode() == 200)
                {
         //           DataBuffer.remove(urlParameters);
                   new Thread(new Runnable() {
                        @Override
                        public void run() {
                            runOnUiThread(new Runnable() {
                                public void run() {

                                    status.setText("Connected");
                                }
                            });

                        }
                    }).start();
                }





            }catch (Exception ex)
            {

                throw ex;
            }

        }
        private void GetMessagebyPost() throws Exception {

            try{

                URL obj = new URL(url);
                HttpURLConnection con = (HttpURLConnection) obj.openConnection();

                //add reuqest header
                con.setConnectTimeout(5000);
                con.setRequestMethod("POST");
                con.setRequestProperty("User-Agent", USER_AGENT);
                con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

                String urlParameters;// = "id=1234&pass=asd45";

                urlParameters ="Get Message";

                // Send post request
                con.setDoOutput(true);
                DataOutputStream wr = new DataOutputStream(con.getOutputStream());
                wr.writeBytes(urlParameters);
                wr.flush();
                wr.close();



                //取得回應訊息
                int responseCode = con.getResponseCode();



                BufferedReader in = new BufferedReader(
                        new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
               if(responseCode == 200)
                {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                runOnUiThread(new Runnable() {
                                    public void run() {

                                        status.setText("Connected");
                                    }
                                });

                            }
                        }).start();

                    CautionMessege = "";
                    JSONObject j;
                    j = new JSONObject(response.toString());
                    if(j.get("A").equals(true))
                    {
                        CautionMessege+=  " 即將超速";
                        A = true;
                    }
                    if(j.get("B").equals(true))
                    {
                        CautionMessege+=  " 已經超速";
                        B = true;
                    }
                    if(j.get("C").equals(true))
                    {
                        CautionMessege+=  " 逆向行駛";
                        C = true;
                    }
                    if(j.get("D").equals(true))
                    {
                        CautionMessege+=  " 進入禁行區域 請離開";
                        D = true;
                    }
                    if(j.get("E").equals(true))
                    {
                        CautionMessege+=  " 違規左轉";
                        E = true;
                    }
                    if(j.get("F").equals(true))
                    {
                        CautionMessege+=  " 違規右轉";
                        F = true;
                    }
                    if(j.get("G").equals(true))
                    {
                        CautionMessege+=  " 前方路段禁止進入";
                       G = true;
                    }
                    if(j.get("H").equals(true))
                    {
                        CautionMessege+=  " 前方禁止左轉";
                        H = true;
                    }
                    if(j.get("I").equals(true))
                    {
                        CautionMessege+=  " 前方禁止右轉";
                        I = true;
                    }

                    if(j.get("J").equals(true))
                    {
                        CautionMessege+=  " 前方禁止轉彎";
                       J = true;
                    }

                     if(!CautionMessege.equals(""))
                     {
                         new Thread(new Runnable() {
                             @Override
                             public void run() {
                                 runOnUiThread(new Runnable() {
                                     public void run() {
                                         Caution.setText(CautionMessege);
                        //                 Cautiondisp(CautionMessege);
                                     }
                                 });

                             }
                         }).start();
                     }


                    if(A.equals(false)&&B.equals(false)&&C.equals(false)&&
                            D.equals(false)&&E.equals(false)
                            &&F.equals(false)&&G.equals(false)
                            &&H.equals(false)&&I.equals(false)
                            &&J.equals(false))
                    {
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    runOnUiThread(new Runnable() {
                                        public void run() {
                                            Caution.setText("");

                                        }
                                    });

                                }
                            }).start();
                    }
        /*            isplay = (mMediaA.isPlaying()||mMediaB.isPlaying()||
                            mMediaC.isPlaying()||mMediaD.isPlaying()||
                            mMediaE.isPlaying()||mMediaF.isPlaying()||
                            mMediaG.isPlaying());*/
                    new Thread(new Runnable() {

                                public void run() {
                                    if(A.equals(true))
                                    {
                                        try {
                                            Thread.sleep(playdelay);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                        mMediaA.start();
                                        A = false;
                                    }
                                    if(B.equals(true))
                                    {
                                        try {
                                            Thread.sleep(playdelay);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                        mMediaB.start();
                                        B = false;
                                    }
                                    if(C.equals(true))
                                    {
                                        try {
                                            Thread.sleep(playdelay);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                        mMediaC.start();
                                        C = false;
                                    }
                                    if(D.equals(true))
                                    {
                                        try {
                                            Thread.sleep(playdelay);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                        mMediaD.start();
                                        D = false;
                                    }
                                    if(E.equals(true))
                                    {
                                        try {
                                            Thread.sleep(playdelay);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                        mMediaE.start();
                                        E = false;
                                    }
                                    if(F.equals(true))
                                    {
                                        try {
                                            Thread.sleep(playdelay);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                        mMediaF.start();
                                        F = false;
                                    }

                                    if(G.equals(true))
                                    {
                                        try {
                                            Thread.sleep(playdelay);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                        mMediaG.start();
                                        G = false;
                                    }
                                  if(H.equals(true))
                                    {
                                        try {
                                            Thread.sleep(playdelay);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                        mMediaH.start();
                                        H = false;
                                    }
                                    if(I.equals(true))
                                    {
                                        try {
                                            Thread.sleep(playdelay);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                        mMediaI.start();
                                        I = false;
                                    }
                                    if(J.equals(true))
                                    {
                                        try {
                                            Thread.sleep(playdelay);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                        mMediaJ.start();
                                        J = false;
                                    }

                        }
                    }).start();

     /*              if(A.equals(true))
                    {
                        if(!isplay)
                        {
                          mMediaA.start();
                          A = false;
                            isplay  =mMediaA.isPlaying();
                           new Thread(new Runnable() {
                                @Override
                               public void run() {
                                    runOnUiThread(new Runnable() {
                                        public void run() {
                                            Caution.setText("您即將超速");
                                            CautiondispA();
                                        }
                                    });

                                }
                            }).start();

                        }
                    }

                    if(B.equals(true))
                    {
                        if(!isplay)
                        {
                            mMediaB.start();
                            B = false;
                            isplay  =mMediaB.isPlaying();
                           new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    runOnUiThread(new Runnable() {
                                        public void run() {

                                            CautiondispB();
                                            Caution.setText("您已經超速");
                                        }
                                    });

                                }
                            }).start();
                        }
                    }
                    if(C.equals(true))
                    {
                        if(!isplay)
                        {
                            mMediaC.start();
                            C = false;
                            isplay  =mMediaC.isPlaying();
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    runOnUiThread(new Runnable() {
                                        public void run() {

                                            CautiondispC();
                                            Caution.setText("您已逆向行駛");
                                        }
                                    });

                                }
                            }).start();
                        }
                    }
                    if(D.equals(true))
                    {
                        if(!isplay)
                        {
                            mMediaD.start();
                            D = false;
                            isplay  =mMediaD.isPlaying();
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    runOnUiThread(new Runnable() {
                                        public void run() {

                                            CautiondispD();
                                            Caution.setText("您已進入禁止區域");
                                        }
                                    });

                                }
                            }).start();
                        }
                    }
                    if(E.equals(true))
                    {
                        if(!isplay)
                        {
                            mMediaE.start();
                            E = false;
                            isplay  =mMediaE.isPlaying();
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    runOnUiThread(new Runnable() {
                                        public void run() {

                                            CautiondispE();
                                            Caution.setText("您已違規左轉");
                                        }
                                    });

                                }
                            }).start();
                        }
                    }
*/
                }

                in.close();

            }
            catch (Exception ex) {
                throw ex;
            }
        }




        private void sendPostTest(int i) throws Exception {

            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            try{
                //add reuqest header
                con.setRequestMethod("POST");
                con.setRequestProperty("User-Agent", USER_AGENT);
                con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

                //格式化時間字串
                SimpleDateFormat formatter =new SimpleDateFormat("yyyy/MM/dd  HH:mm:ss");
                NowTime_.setTime(System.currentTimeMillis());
                String serverTime =  formatter.format(NowTime_);
Test T1 = new Test();


                String jsondata;// = "{\"Data\":{\"Name\":\"MichaelChan\",\"Email\":\"XXXX@XXX.com\",\"Phone\":[1234567,0911123456]}}";

                jsondata= String.format("{\"Lic\": \"%s\" , \"LOG\" : %f , \"LAT\": %f, \"Speed\": %f ,\"Address\": \"%s\" ,\"Time\": \"%s\"}",
                        License_plate,T1.TestLon[i],T1.TestLat[i],T1.TestSpeed[i],T1.TestAddress[i],serverTime);
                  jsondata = URLEncoder.encode(jsondata, "UTF-8");

                String urlParameters;// = "id=1234&pass=asd45";

                urlParameters ="Data "+jsondata;
                //  DataBuffer.add(urlParameters);



                // Send post request
                con.setDoOutput(true);
                DataOutputStream wr = new DataOutputStream(con.getOutputStream());
                wr.writeBytes(urlParameters);
                wr.flush();
                wr.close();


                //取得回應訊息
                int responseCode = con.getResponseCode();


                BufferedReader in = new BufferedReader(
                        new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                if(response.toString().equals("ACK"))
                {
                    DataBuffer.remove(urlParameters);
                }





            }catch (Exception ex)
            {

                throw ex;
            }

        }

    }


}
