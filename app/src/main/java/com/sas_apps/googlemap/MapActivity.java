package com.sas_apps.googlemap;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.sas_apps.googlemap.adaptor.InfoAdaptor;
import com.sas_apps.googlemap.adaptor.PlaceAutocompleteAdapter;
import com.sas_apps.googlemap.model.PlaceInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class MapActivity extends AppCompatActivity implements OnMapReadyCallback,
        OnFailureListener,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "MapActivity";
    private static final String PERMISSION_FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String PERMISSION_COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int PERMISSION_REQUEST_CODE = 12;

    private boolean isPermissionGranted = false;
    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private static final float DEFAULT_ZOOM = 15f;
    private static final int PLACE_PICKER_REQUEST = 1;
    private PlaceAutocompleteAdapter placeAutocompleteAdapter;
    private GoogleApiClient mGoogleApiClient;
    private LatLngBounds LAT_LANG_BOUNDS = new LatLngBounds(new LatLng(-40, -168), new LatLng(71, 168));
    private static final LatLngBounds BOUNDS_INDIA = new LatLngBounds(new LatLng(23.63936, 68.14712),
            new LatLng(28.20453, 97.34466));
    private PlaceInfo placeInfo;
    private Marker mMarker;

    AutoCompleteTextView editSearch;
    ImageView imageGps, imageInfo, imageMap;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_map);
        editSearch = findViewById(R.id.edit_search);
        imageGps = findViewById(R.id.image_gps);
        imageInfo = findViewById(R.id.image_info);
        imageMap = findViewById(R.id.image_map);
        locationPermission();

        mGoogleApiClient = new GoogleApiClient
                .Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .enableAutoManage(this, this)
                .build();
        placeAutocompleteAdapter = new PlaceAutocompleteAdapter(MapActivity.this,
                Places.getGeoDataClient(this, null),
                BOUNDS_INDIA, null);
        editSearch.setAdapter(placeAutocompleteAdapter);
        editSearch.setOnItemClickListener(onItemClickListener);

        editSearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

                if (actionId == EditorInfo.IME_ACTION_SEARCH
                        || actionId == EditorInfo.IME_ACTION_DONE
                        || event.getAction() == KeyEvent.ACTION_DOWN
                        || event.getAction() == KeyEvent.KEYCODE_ENTER) {

                    // move to search result
                    geoLocate();
                    hideKeyboard();
                }

                return false;
            }
        });

        imageGps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: moving to my location");
                getDeviceLocation();
            }
        });

        imageInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if ((mMarker.isInfoWindowShown())) {
                        mMarker.showInfoWindow();
                    } else {
                        mMarker.showInfoWindow();
                        Log.d(TAG, "onClick:    PLACE INFO: " + placeInfo.toString());
                    }
                } catch (NullPointerException e) {
                    Log.e(TAG, "onClick: " + e.getMessage());
                }
            }
        });

        imageMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
                try {
                    startActivityForResult(builder.build(MapActivity.this), PLACE_PICKER_REQUEST);
                } catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException e) {
                    Log.e(TAG, "onClick:  GooglePlayServicesRepairableException , " +
                            "GooglePlayServicesNotAvailableException" +
                            "" + e.getMessage());
                }
            }
        });
        hideKeyboard();
    }


    private void geoLocate() {
        String searchAddress = editSearch.getText().toString();
        Log.d(TAG, "Geo-locating to " + searchAddress);
        Geocoder geocoder = new Geocoder(this);
        List<Address> addressList = new ArrayList<>();
        try {
            addressList = geocoder.getFromLocationName(searchAddress, 1);
        } catch (IOException e) {
            Log.e(TAG, "geoLocate: " + e.getMessage());
        }

        if (addressList.size() > 0) {
            Address address = addressList.get(0);
            Log.d(TAG, "geoLocate: Found location " + address.toString());
            moveCamera(new LatLng(address.getLatitude(), address.getLongitude()), DEFAULT_ZOOM,
                    address.getLocality());
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
//        Toast.makeText(this, "onMapReady", Toast.LENGTH_SHORT).show();
        this.googleMap = googleMap;
        if (isPermissionGranted) {
            getDeviceLocation();

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                    PackageManager.PERMISSION_GRANTED && ActivityCompat
                    .checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) !=
                    PackageManager.PERMISSION_GRANTED) {
                return;
            }
            googleMap.setMyLocationEnabled(false);
            googleMap.getUiSettings().setMyLocationButtonEnabled(true);
        }
    }

    private void getDeviceLocation() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        try {
            if (isPermissionGranted) {
                Task location = fusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "onComplete: location found");
                            try {
                                Location currentLocation = (Location) task.getResult();
                                moveCamera(new LatLng(currentLocation.getLatitude(),
                                                currentLocation.getLongitude()), DEFAULT_ZOOM,
                                        "My location");
                            } catch (NullPointerException e) {
                                Toast.makeText(MapActivity.this, "No previous location found" +
                                        "\nPlease open google map and set your location", Toast.LENGTH_SHORT).show();
                                Log.e(TAG, "onComplete: NullPointerException No location found " + e.getMessage());
                            }
                        } else {
                            Log.d(TAG, "onComplete: Current location is null");
                            Toast.makeText(MapActivity.this, "Unable to get current location", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        } catch (SecurityException e) {
            Log.e(TAG, "getDeviceLocation: " + e.getMessage());
        }
    }


    private void moveCamera(LatLng latLng, float zoom, PlaceInfo mPlaceInfo) {
        Log.d(TAG, "moveCamera: moving cam to latitude= " + latLng.latitude + "     longitude= " + latLng.longitude);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
        googleMap.clear();
        googleMap.setInfoWindowAdapter(new InfoAdaptor(this));
        if (mPlaceInfo != null) {
            try {
                String info = "Address- " + mPlaceInfo.getAddress() + "\n" +
                        "Phone number- " + mPlaceInfo.getPhoneNumber() + "\n" +
                        "Website- " + mPlaceInfo.getWebsiteUri() + "\n" +
                        "Price rating- " + mPlaceInfo.getRating() + "\n";
                MarkerOptions markerOptions = new MarkerOptions()
                        .position(latLng)
                        .title(mPlaceInfo.getName())
                        .snippet(info);
                mMarker = googleMap.addMarker(markerOptions);
                googleMap.addMarker(markerOptions);
            } catch (NullPointerException e) {
                Log.e(TAG, "moveCamera: " + e.getMessage());
            }
        } else {
            googleMap.addMarker(new MarkerOptions().position(latLng));
        }
        hideKeyboard();
    }

    private void moveCamera(LatLng latLng, float zoom, String address) {
        Log.d(TAG, "moveCamera: moving cam to latitude= " + latLng.latitude + "     longitude= " + latLng.longitude);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
        MarkerOptions markerOptions = new MarkerOptions()
                .position(latLng)
                .title(address);
        googleMap.addMarker(markerOptions);
        hideKeyboard();
    }

    public void locationPermission() {
        String[] permission = {PERMISSION_FINE_LOCATION, PERMISSION_COURSE_LOCATION};
        if (ContextCompat.checkSelfPermission(MapActivity.this,
                PERMISSION_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            //  && ContextCompat.checkSelfPermission(MapActivity.this, PERMISSION_COURSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(MapActivity.this,
                    PERMISSION_COURSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                isPermissionGranted = true;
                initMap();
            } else {
                ActivityCompat.requestPermissions(this, permission, PERMISSION_REQUEST_CODE);
            }
        } else {
            ActivityCompat.requestPermissions(this, permission, PERMISSION_REQUEST_CODE);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PLACE_PICKER_REQUEST) {
            if (resultCode == RESULT_OK) {
                Place place = PlacePicker.getPlace(this, data);
                PendingResult<PlaceBuffer> placeBufferPendingResult = Places.GeoDataApi
                        .getPlaceById(mGoogleApiClient, place.getId());
                placeBufferPendingResult.setResultCallback(updatePlaceDetailsCallback);
            }
        }
    }

    @Override
    public void onFailure(@NonNull Exception e) {
        Log.e(TAG, "onFailure: " + e.getMessage());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        isPermissionGranted = false;
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE: {
                if (grantResults.length > 0) {
                    for (int grantResult : grantResults) {
                        if (grantResult != PackageManager.PERMISSION_GRANTED) {
                            isPermissionGranted = false;
                            return;
                        }
                    }
                    isPermissionGranted = true;
                    initMap();
                }
            }
        }
    }


    private void initMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    private void hideKeyboard() {
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }


    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e(TAG, "onConnectionFailed: " + connectionResult.getErrorMessage());
    }


//  -----------------------------------  Google Places  ----------------------------------------


    private AdapterView.OnItemClickListener onItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            hideKeyboard();
            final AutocompletePrediction item = placeAutocompleteAdapter.getItem(position);
            final String placeId = item.getPlaceId();
            PendingResult<PlaceBuffer> placeBufferPendingResult = Places.GeoDataApi
                    .getPlaceById(mGoogleApiClient, placeId);
            placeBufferPendingResult.setResultCallback(updatePlaceDetailsCallback);
        }
    };
    private ResultCallback<PlaceBuffer> updatePlaceDetailsCallback = new ResultCallback<PlaceBuffer>() {
        @Override
        public void onResult(@NonNull PlaceBuffer places) {
            if (!places.getStatus().isSuccess()) {
                Log.e(TAG, "onResult: Not success   " + places.getStatus().toString());
                places.release();
                return;
            } else {
                final Place place = places.get(0);
                placeInfo = new PlaceInfo();
                try {
                    Log.d(TAG, "onResult: Place Details  Address=   " + place.getAddress());
                    Log.d(TAG, "onResult: Place Details   Viewport=  " + place.getViewport());
                    Log.d(TAG, "onResult: Place Details   Website=  " + place.getWebsiteUri());
                    Log.d(TAG, "onResult: Place Details  PhoneNumber=   " + place.getPhoneNumber());
                    Log.d(TAG, "onResult: Place Details   Id=  " + place.getId());
                    //              Log.d(TAG, "onResult: Place Details   Attributions=  "+place.getAttributions());
                    Log.d(TAG, "onResult: Place Details   LatLng=  " + place.getLatLng());
                    Log.d(TAG, "onResult: Rating= " + place.getRating());

                    placeInfo.setName(place.getName().toString());
                    placeInfo.setAddress(place.getAddress().toString());
                    placeInfo.setPhoneNumber(place.getPhoneNumber().toString());
                    placeInfo.setId(place.getId().toString());
                    placeInfo.setLatlng(place.getLatLng());
                    //           placeInfo.setAttributions(place.getAttributions().toString());
                    placeInfo.setRating(place.getRating());

                } catch (NullPointerException e) {
                    Log.e(TAG, "onResult: " + e.getMessage());
                }
                Log.d(TAG, "onResult:       " + placeInfo.toString());

                moveCamera(new LatLng(place.getViewport().getCenter().latitude,
                        place.getViewport().getCenter().longitude), DEFAULT_ZOOM, placeInfo);
//
//                Log.d(TAG, "onResult: Place Details     " + place.getAddress());
//                Log.d(TAG, "onResult: Place Details     " + place.getViewport());
//                Log.d(TAG, "onResult: Place Details     " + place.getWebsiteUri());
//                Log.d(TAG, "onResult: Place Details     " + place.getPhoneNumber());
//                Log.d(TAG, "onResult: Place Details     " + place.getId());
//                Log.d(TAG, "onResult: Place Details     " + place.getAttributions());
//                Log.d(TAG, "onResult: Place Details     " + place.getLatLng());

            }
        }
    };

}
