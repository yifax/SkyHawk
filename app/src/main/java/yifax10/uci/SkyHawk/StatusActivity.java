package yifax10.uci.SkyHawk;

import android.Manifest;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StatusActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.OnConnectionFailedListener{
    private static final String TAG = "StatusActivity";
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;

    // vars
    private Boolean mLocationPermissionGranted = false;
    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private LatLng savedLocation;
    // widgets
    private ImageView mGPS;
    private Button mButton;
    private PlacesClient placesClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.status_and_maps);
        // Locate and Config Widgets
        mGPS = findViewById(R.id.ic_gpsLocation);
        mButton = findViewById(R.id.bt_launch);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder=new Builder(StatusActivity.this);
                builder.setIcon(R.drawable.ic_gpslocation);
                builder.setTitle("Destination");
                builder.setMessage("Direct Drone to "+savedLocation.latitude+", "+savedLocation.longitude);
                builder.setPositiveButton("Okay", new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        Toast.makeText(StatusActivity.this, "Launching...", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(StatusActivity.this, OpenCVActivity.class);
                        startActivity(intent);
                    }
                });
                builder.setNegativeButton("Cancel", new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        Toast.makeText(StatusActivity.this, "Cancelled",Toast.LENGTH_SHORT).show();
                    }
                });
                AlertDialog b=builder.create();
                b.show();
            }
        });

        //Set up Google Places API
        String apiKey = getString(R.string.google_maps_API_key);
        if (apiKey.equals("")) {
            Toast.makeText(this, getString(R.string.google_maps_API_key), Toast.LENGTH_LONG).show();
            return;
        }
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), apiKey);
        }
        Places.initialize(getApplicationContext(), apiKey);
        placesClient = Places.createClient(this);

        //Get Current Location
        getLocationPermission();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        Toast.makeText(this, "Map is Loaded", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "initMap: Map is ready");
        if (mLocationPermissionGranted) {
            getDeviceLocation();
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            // Map UI Configuration
            mMap.setMyLocationEnabled(true);    // Display blue dot for current location
            mMap.getUiSettings().setMyLocationButtonEnabled(false); // Disable default home button

        }
    }

    private void initMap(){
        Toast.makeText(this, "Loading Map...", Toast.LENGTH_SHORT).show();
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.google_map_fragment);
        mapFragment.getMapAsync(this);
    }

    private void initSearchBar(){
        Log.d(TAG, "init: initializing search bar...");
        // Initialize the AutocompleteSupportFragment.
        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);

        // Specify the types of place data to return.
        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME));

        // Set up a PlaceSelectionListener to handle the response.
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                Log.i(TAG, "Place: " + place.getName() + ", " + place.getId());
                geoLocate(place.getName());
            }

            @Override
            public void onError(Status status) {
                Log.i(TAG, "An error occurred: " + status);
            }
        });

        mGPS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: GPS button Clicked");
                getDeviceLocation();
            }
        });
    }

    private void geoLocate(String searchString){
        Log.d(TAG, "geoLocate: geo locating...");
        Geocoder geocoder = new Geocoder(StatusActivity.this);
        List<Address> list = new ArrayList<>();
        try {
            list = geocoder.getFromLocationName(searchString, 1);
        }catch (IOException e){
            Log.e(TAG,"geoLocate: IOException:" + e.getMessage());
        }
        if(list.size()>0){
            Address address = list.get(0);
            Log.d(TAG,"geoLocate: found a location:" + address.toString());
            savedLocation = new LatLng(address.getLatitude(),address.getLongitude());
            moveCamera(savedLocation, Constants.DEFAULT_ZOOM,address.getAddressLine(0));
        }
    }


    private void getDeviceLocation(){
        Log.d(TAG, "getDeviceLocation: getting the current location...");
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        try {
            if (mLocationPermissionGranted) {
                final Task location = mFusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener() {
                            public void onComplete(@NonNull Task task){
                                if (task.isSuccessful()) {
                                    Log.d(TAG, "onComplete: found location!");
                                    Location currentLocation = (Location) task.getResult();
                                    savedLocation = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                                    moveCamera(savedLocation, Constants.DEFAULT_ZOOM, "Current Location");
                                    refreshGPSData(currentLocation.getLatitude(), currentLocation.getLongitude());
                                }else {
                                    Log.d(TAG, "onComplete: current location is null");
                                    Toast.makeText(StatusActivity.this, "Unable to get current location", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        }catch (SecurityException e){
            Log.e(TAG, "getDeviceLocation: SecurityException:" + e.getMessage());
        }
    }

    private void refreshGPSData(Double lat, Double lng){
        TextView latData = (TextView) findViewById(R.id.current_gps_lat);
        TextView lngData = (TextView) findViewById(R.id.current_gps_lng);
        latData.setText("Latitude: " + lat);
        lngData.setText("Longitude: " + lng);
    }


    private void moveCamera(LatLng latlng, float zoom, String title) {
        Log.d(TAG,"moveFocus: moving the camera to: lat:"+latlng.latitude+", lng:"+latlng.longitude);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, zoom));
        MarkerOptions options = new MarkerOptions().position(latlng).title(title);
        mMap.addMarker(options);
    }

    private void getLocationPermission(){
        //Toast.makeText(this, "Getting Location Permission...", Toast.LENGTH_SHORT).show();
        String[] permissions = {FINE_LOCATION};
        if(ContextCompat.checkSelfPermission(this.getApplicationContext(), FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
            initMap();
            initSearchBar();
        }else{
            ActivityCompat.requestPermissions(this, permissions, Constants.LOCATION_PERMISSION_REQUEST_CODE);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;

        switch (requestCode){
            case Constants.LOCATION_PERMISSION_REQUEST_CODE: {
                if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)){
                    mLocationPermissionGranted = true;
                    Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
                    initMap();
                }
                else{
                    mLocationPermissionGranted = false;
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}
