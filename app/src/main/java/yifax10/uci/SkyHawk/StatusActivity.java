package yifax10.uci.SkyHawk;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.nfc.Tag;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.w3c.dom.Text;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class StatusActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.OnConnectionFailedListener{
    private static final String TAG = "StatusActivity";
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 123;
    private static final float DEFAULT_ZOOM = 15f;
    private static final LatLngBounds LAT_LNG_BOUNDS = new LatLngBounds(
            new LatLng(-40, -168), new LatLng(71,136));

    // vars
    private Boolean mLocationPermissionGranted = false;
    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private PlaceAutocompleteAdapter mPlaceAutocompleteAdapter;
    private GoogleApiClient mGoogleApiClient;

    // widgets
    private AutoCompleteTextView mSearchText;
    private ImageView mGPS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.status_and_maps);
        mSearchText = (AutoCompleteTextView) findViewById(R.id.map_Search_Bar);
        mGPS = (ImageView) findViewById(R.id.ic_gpsLocation);
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

        mGoogleApiClient = new GoogleApiClient
                .Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .enableAutoManage(this,this)
                .build();
        mPlaceAutocompleteAdapter = new PlaceAutocompleteAdapter(this, mGoogleApiClient, LAT_LNG_BOUNDS, null);
        mSearchText.setAdapter(mPlaceAutocompleteAdapter);
        mSearchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH
                    || actionId == EditorInfo.IME_ACTION_DONE
                    || event.getAction() == event.ACTION_DOWN
                    || event.getAction() == event.KEYCODE_ENTER) {
                    geoLocate();
                }
                return false;
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

    private void geoLocate(){
        Log.d(TAG, "geoLocate:geolocating the input string...");
        String searchString = mSearchText.getText().toString();
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
            moveCamera(new LatLng(address.getLatitude(),address.getLongitude()),DEFAULT_ZOOM,address.getAddressLine(0));
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
                                    moveCamera(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()), DEFAULT_ZOOM, "Current Location");
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
            ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;

        switch (requestCode){
            case LOCATION_PERMISSION_REQUEST_CODE: {
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
