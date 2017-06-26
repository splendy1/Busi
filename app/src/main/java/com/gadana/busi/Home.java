package com.gadana.busi;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.gadana.busi.Utils.ConnectionReceiver;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.PlaceDetectionApi;
import com.google.android.gms.location.places.PlaceFilter;
import com.google.android.gms.location.places.PlaceLikelihood;
import com.google.android.gms.location.places.PlaceLikelihoodBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class Home extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = Home.class.getSimpleName();
    protected static final int REQUEST_CHECK_SETTINGS = 0x1;
    protected static final int PLACE_PICKER_REQUEST = 1;
    protected static final int PLACE_AUTOCOMPLETE_REQUEST_CODE = 1;

    private GoogleApiClient mGoogleApiClient;
    private FusedLocationProviderClient mFusedLocationClient;
    private Location gpsLocation;
    private Snackbar networkNotify;

    private boolean locationNotificationActive = false;
    private boolean networkNotificationShown = false;
    private boolean locationActive = false;
    private boolean previousNetworkState = false;
    private boolean currentNetworkState = false;

    public String userOrign;
    public String userDest;

    public String countryCode;

    private Boolean settingsRequest = false, autocompleteRequest = false;
    private Button selectedBt;
    private Button orignBt;
    private Button destBt;
    private ProgressBar mapLoading;
    private GoogleMap gMap;
    private View floatBtLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        floatBtLayout = findViewById(R.id.floatBtLayout);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);

        //Network Connection Notification
        networkNotify = Snackbar.make(floatBtLayout, "Network Connectivity Unavailable!", Snackbar.LENGTH_INDEFINITE)
                .setAction("Action", null);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        //drawer.setDrawerListener(toggle);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        //Initialize Mapview
        // Retrieve the content view that renders the map.
        // Get the SupportMapFragment and register for the callback
        // when the map is ready for use.

        mapLoading = (ProgressBar) findViewById(R.id.mapProgressBar);

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.mapView);
        mapFragment.getMapAsync(this);

        mGoogleApiClient = new GoogleApiClient
                .Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .enableAutoManage(this, this)
                .build();

        orignBt = (Button) findViewById(R.id.orignBT);
        destBt = (Button) findViewById(R.id.destBT);

        //Get Orign location
        orignBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                selectedBt = orignBt;
                callAutoComplete();
            }
        });

        //Get Orign location
        destBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                selectedBt = destBt;
                callAutoComplete();
            }
        });

        //check the network connectivity when activity is created
        ScheduledExecutorService scheduler =
                Executors.newSingleThreadScheduledExecutor();

        scheduler.scheduleAtFixedRate
                (new Runnable() {
                    public void run() {
                        checkConnection();
                    }
                }, 2, 3, TimeUnit.SECONDS);

    }

    private void callAutoComplete() {
        try {
            autocompleteRequest = true;
            AutocompleteFilter filter = new AutocompleteFilter.Builder().setCountry(countryCode).setTypeFilter(AutocompleteFilter.TYPE_FILTER_ADDRESS | AutocompleteFilter.TYPE_FILTER_ESTABLISHMENT).build();
            Intent intent = new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_OVERLAY).setFilter(filter).build(this);
            startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE);
        } catch (GooglePlayServicesRepairableException e) {
            autocompleteRequest = false;
        } catch (GooglePlayServicesNotAvailableException e) {
            autocompleteRequest = false;
        }
    }


    //Map View
    @Override
    public void onMapReady(GoogleMap googleMap) {

        this.gMap = googleMap;

        try {
            // Customise the styling of the base map using a JSON object defined
            // in a raw resource file.
            boolean success = googleMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                            this, R.raw.map_style));

            if (!success) {
                Log.e(TAG, "Style parsing failed.");
            }

        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Can't find style. Error: ", e);
        }


        //Get User's Current Location
        createLocationRequest();
    }

    //Send Request to check GPS Availability
    protected void createLocationRequest() {

        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);
        SettingsClient client = LocationServices.getSettingsClient(Home.this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(Home.this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                // All location settings are satisfied. The client can initialize
                // location requests here.

                if (locationSettingsResponse.getLocationSettingsStates().isGpsUsable() ||
                        locationSettingsResponse.getLocationSettingsStates().isGpsPresent() ||
                        locationSettingsResponse.getLocationSettingsStates().isLocationPresent() ||
                        locationSettingsResponse.getLocationSettingsStates().isLocationUsable()) {
                    getUserLocation();

                }

            }
        });


        task.addOnFailureListener(Home.this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                int statusCode = ((ApiException) e).getStatusCode();
                switch (statusCode) {
                    case CommonStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied, but this can be fixed
                        // by showing the user a dialog.
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().

                            settingsRequest = true;

                            if (!locationNotificationActive) {
                                ResolvableApiException resolvable = (ResolvableApiException) e;
                                resolvable.startResolutionForResult(Home.this, REQUEST_CHECK_SETTINGS);

                            }

                            locationNotificationActive = true;
                            Toast.makeText(getApplicationContext(), "Please turn on GPS", Toast.LENGTH_LONG).show();


                        } catch (IntentSender.SendIntentException sendEx) {
                            settingsRequest = false;
                            createLocationRequest();
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        settingsRequest = false;
                        createLocationRequest();

                        break;
                }
            }
        });


    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (settingsRequest) {
            final LocationSettingsStates states = LocationSettingsStates.fromIntent(data);

            switch (requestCode) {
                case REQUEST_CHECK_SETTINGS:
                    switch (resultCode) {
                        case Activity.RESULT_OK:
                            // All required changes were successfully made
                            //Get GPS Location
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {

                                    createLocationRequest();
                                    locationNotificationActive = false;
                                }
                            });

                            break;
                        case Activity.RESULT_CANCELED:
                            locationNotificationActive = false;
                            break;
                        default:
                            break;
                    }
                    break;

            }

            settingsRequest = false;
        } else if (autocompleteRequest) {
            switch (requestCode) {
                case REQUEST_CHECK_SETTINGS:
                    switch (resultCode) {
                        case RESULT_OK:
                            Place place = PlaceAutocomplete.getPlace(this, data);
                            Log.i(TAG, "Place: " + place.getName());

                            if (selectedBt == orignBt) {
                                TextView orignTXT = (TextView) findViewById(R.id.orignTXT);
                                orignTXT.setText(place.getName());
                            } else if (selectedBt == destBt) {
                                TextView destTXT = (TextView) findViewById(R.id.destTXT);
                                destTXT.setText(place.getName());
                            }

                            break;
                        case PlaceAutocomplete.RESULT_ERROR:
                            Status status = PlaceAutocomplete.getStatus(this, data);
                            // TODO: Handle the error.
                            Log.i(TAG, status.getStatusMessage());

                            break;
                        case RESULT_CANCELED:
                            // The user canceled the operation.
                            break;
                    }

                    break;


            }
        }

        autocompleteRequest = false;
    }

    //Get GPS Location
    private void getUserLocation() {

        //Check Permission
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            gMap.setMyLocationEnabled(true);

            if (currentNetworkState)
                getLocationUsingGooglePlayServices();
        }


    }


    public LatLng userCoordinates = null;
    public Place userPlace;


    private void setOrignText() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                TextView orignTXT = (TextView) findViewById(R.id.orignTXT);
                orignTXT.setText(userOrign);

                mapAnimated = false;
                loadMap();
            }
        });


    }

    private void setDestinationText() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                TextView destTXT = (TextView) findViewById(R.id.destTXT);
                destTXT.setText(userDest);

                mapAnimated = false;
                loadMap();
            }
        });


    }

    public void getLocationUsingGoogleWepApi() {


        //Check Permission
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            try {
                //PlaceFilter filter = new PlaceFilter(true, Place.);
                PendingResult<PlaceLikelihoodBuffer> result = Places.PlaceDetectionApi.getCurrentPlace(mGoogleApiClient, null);

                result.setResultCallback(new ResultCallback<PlaceLikelihoodBuffer>() {
                    @Override
                    public void onResult(PlaceLikelihoodBuffer placeLikelihoods) {

                        userPlace = placeLikelihoods.get(placeLikelihoods.getCount() - 1).getPlace();

                        Status status = placeLikelihoods.getStatus();

                        if (status.isSuccess()) {
                            if (userCoordinates == null)
                                userCoordinates = userPlace.getLatLng();

                            countryCode = "MY";

                            userOrign = userPlace.getAddress().toString();

                            setOrignText();
                        } else {
                            getUserLocation();

                        }
                        placeLikelihoods.release();
                    }


                });
            } catch (Exception ex) {
                getUserLocation();

            }

        }


    }

    public void getLocationUsingGooglePlayServices() {

        // Location client
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        //Check Permission
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            try {

                mFusedLocationClient.getLastLocation()
                        .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                            @Override
                            public void onSuccess(Location location) {

                                if (location != null) {

                                    if (userCoordinates == null)
                                        userCoordinates = new LatLng(location.getLatitude(), location.getLongitude());

                                    getPlaceDetailsForLocation();
                                } else {
                                    getUserLocation();
                                }

                            }
                        });
            } catch (Exception ex) {
                getLocationUsingGoogleWepApi();

            }

        }

    }


    public void getPlaceDetailsForLocation() {
        //Get Country Code
        Geocoder geocoder = new Geocoder(getBaseContext(), Locale.getDefault());
        List<Address> addresses = null;
        try {
            addresses = geocoder.getFromLocation(userCoordinates.latitude, userCoordinates.longitude, 1);

            if (addresses != null && !addresses.isEmpty()) {

                countryCode = addresses.get(addresses.size() - 1).getCountryCode();
                userOrign = addresses.get(addresses.size() - 1).getSubLocality();

                setOrignText();
            } else getPlaceDetailsForLocationFromGoogle();
        } catch (Exception ex) {

            getPlaceDetailsForLocationFromGoogle();
        }

    }


    public void getPlaceDetailsForLocationFromGoogle() {
        String postURL = "https://maps.googleapis.com/maps/api/geocode/json?latlng="
                + userCoordinates.latitude + "," + userCoordinates.longitude + "&location_type=APPROXIMATE&" +
                "result_type=sublocality&sensor=true&key=" + getResources().getString(R.string.google_maps_key);
        Log.d("Place request", postURL);

        Ion.with(getApplicationContext())
                .load(postURL)
                .asJsonObject()
                .setCallback(new FutureCallback<JsonObject>() {

                    @Override
                    public void onCompleted(Exception e, JsonObject resultsDict) {

                        // do stuff with the result or error
                        String status = resultsDict.getAsJsonPrimitive("status").getAsString();
                        Log.d("Result Status", status);

                        if (status.equals("OK")) {
                            JsonArray results = resultsDict.getAsJsonArray("results");
                            JsonArray addressComponents = ((JsonObject) results.get(0)).getAsJsonArray("address_components");
                            for (JsonElement addressComponent : addressComponents) {
                                JsonArray addressTypes = ((JsonObject) addressComponent).getAsJsonArray("types");
                                for (JsonElement addressType : addressTypes) {
                                    if (addressType.getAsString().equals("country")) {
                                        JsonElement longName = ((JsonObject) addressComponent).getAsJsonPrimitive("short_name");
                                        countryCode = longName.getAsString();

                                        Log.d("Country Code", countryCode);

                                    }

                                    if (addressType.getAsString().equals("sublocality")) {

                                        JsonElement longName = ((JsonObject) addressComponent).getAsJsonPrimitive("long_name");
                                        userOrign = longName.getAsString();

                                        setOrignText();

                                        Log.d("User location", userOrign);

                                    }

                                }

                            }
                        } else {
                            getLocationUsingGoogleWepApi();
                        }

                    }
                });

    }

    public void getPlaceDetailsForId(String placeId) {

        //Google
        Places.GeoDataApi.getPlaceById(mGoogleApiClient, placeId)
                .setResultCallback(new ResultCallback<PlaceBuffer>() {
                    @Override
                    public void onResult(PlaceBuffer places) {

                        if (places.getStatus().isSuccess() && places.getCount() > 0) {

                            final Place myPlace = places.get(0);
                            Log.i(TAG, "Place found: " + myPlace.getName());

                        } else {
                            Log.e(TAG, "Place not found");
                        }
                        places.release();
                    }
                });


    }

    private boolean mapAnimated = false;

    public void loadMap() {
        gMap.clear();

        // Move the camera instantly to Sydney with a zoom of 15.
        gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userCoordinates, 15));
        if (!mapAnimated) {
            // Zoom in, animating the camera.
            gMap.animateCamera(CameraUpdateFactory.zoomIn());

            // Zoom out to zoom level 10, animating with a duration of 2 seconds.
            gMap.animateCamera(CameraUpdateFactory.zoomTo(14), 2000, null);

            mapAnimated = true;
        }


        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mapLoading.animate().alpha(0f).setDuration(1000).start();

            }
        });

        locationActive = true;

        //mapLoading.setVisibility(View.INVISIBLE);

        addMarker(userCoordinates);

    }

    //Add marker to map
    public void addMarker(LatLng coordinates) {


        MarkerOptions buses = new MarkerOptions();
        buses.position(coordinates);
        buses.title("U99");

        // Changing marker icon
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inMutable = true;

        Bitmap imageBitmap = BitmapFactory.decodeResource(getResources(),
                R.drawable.bus_my, opt);
        Bitmap resized = Bitmap.createScaledBitmap(imageBitmap, 90, 60, true);

        buses.icon(BitmapDescriptorFactory.fromBitmap(resized));

        // adding marker
        gMap.addMarker(buses);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            // Handle the camera action
        } else if (id == R.id.nav_reminders) {

        } else if (id == R.id.nav_settings) {

        } else if (id == R.id.nav_slideshow) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }


    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

        Toast.makeText(getApplicationContext(), "Location Service not started", Toast.LENGTH_LONG).show();

    }

    @Override
    protected void onResume() {
        super.onResume();

    }


    private void checkConnection() {
        boolean isConnected = ConnectionReceiver.isConnected(getApplicationContext());

        if (!isConnected) {

            if (!networkNotificationShown) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        floatBtLayout.animate().y(-100).setDuration(500).start();
                        networkNotify.show();
                    }
                });

                networkNotificationShown = true;

            }
            if (previousNetworkState != currentNetworkState) {

                networkNotificationShown = false;
                locationActive = false;
                previousNetworkState = isConnected;
            }

            previousNetworkState = currentNetworkState;
            currentNetworkState = isConnected;


        } else {

            if (networkNotify.isShown()) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        networkNotify.dismiss();
                        floatBtLayout.animate().y(0).setDuration(500).start();

                    }
                });
                networkNotificationShown = false;
            }
            if (!locationActive) {
                createLocationRequest();

            }

            previousNetworkState = currentNetworkState;
            currentNetworkState = isConnected;

        }
    }


}
