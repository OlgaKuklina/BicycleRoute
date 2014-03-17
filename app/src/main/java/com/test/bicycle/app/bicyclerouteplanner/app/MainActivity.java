package com.test.bicycle.app.bicyclerouteplanner.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.w3c.dom.Document;

import java.util.ArrayList;


public class MainActivity extends FragmentActivity implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener,
        LocationListener {

    LocationClient mLocationClient;
    boolean mUpdatesRequested;
    LocationRequest mLocationRequest;
    private final static int
            CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    // Milliseconds per second
    private static final int MILLISECONDS_PER_SECOND = 1000;
    // Update frequency in seconds
    public static final int UPDATE_INTERVAL_IN_SECONDS = 10;
    // Update frequency in milliseconds
    private static final long UPDATE_INTERVAL =
            MILLISECONDS_PER_SECOND * UPDATE_INTERVAL_IN_SECONDS;
    // The fastest update frequency, in seconds
    private static final int FASTEST_INTERVAL_IN_SECONDS = 5;
    // A fast frequency ceiling in milliseconds
    private static final long FASTEST_INTERVAL =
            MILLISECONDS_PER_SECOND * FASTEST_INTERVAL_IN_SECONDS;
    private static final int REPRESENTATIVE_COUNT = 25;

    private final ElevationService elevationService = new ElevationService();

    private GoogleMap googleMap;
    private Marker currentLocationMarker;
    private Polyline line;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        setUpMapIfNeeded();

        // Create the LocationRequest object
        mLocationRequest = LocationRequest.create();
        // Use high accuracy
        mLocationRequest.setPriority(
                LocationRequest.PRIORITY_HIGH_ACCURACY);
        // Set the update interval to 5 seconds
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        // Set the fastest update interval to 1 second
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        /*
         * Create a new location client, using the enclosing class to
         * handle callbacks.
         */
        mLocationClient = new LocationClient(this, this, this);
        // Start with updates turned off
        mUpdatesRequested = true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        mLocationClient.connect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    private void setUpMapIfNeeded() {
        if (googleMap != null) {
            return;
        }
        googleMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
        if (googleMap == null) {
            return;
        }
        googleMap.setMyLocationEnabled(true);
        googleMap.setOnMapClickListener(new MapClickListener());
    }

    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case CONNECTION_FAILURE_RESOLUTION_REQUEST:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        break;
                }
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        // Display the connection status
        Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
        // If already requested, start periodic updates
        if (mUpdatesRequested) {
            mLocationClient.requestLocationUpdates(mLocationRequest, this);
        }
    }

    @Override
    public void onDisconnected() {

    }

    @Override
    public void onLocationChanged(Location location) {
        // Report to the UI that the location was updated
        String msg = "Updated Location: " +
                Double.toString(location.getLatitude()) + "," +
                Double.toString(location.getLongitude());
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        LatLng latlng = new LatLng(location.getLatitude(), location.getLongitude());
        if (currentLocationMarker != null) {
            currentLocationMarker.remove();
        }

        currentLocationMarker = googleMap.addMarker(new MarkerOptions()
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.bike))
                .anchor(0.5f, 1.0f) // Anchors the marker on the bottom left
                .position(latlng));
    }
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    protected void onStop() {
        if (mLocationClient.isConnected()) {
            mLocationClient.removeLocationUpdates(this);
        }
        mLocationClient.disconnect();
        super.onStop();
    }

    private class MapClickListener implements GoogleMap.OnMapClickListener {
        private LatLng origin;
        private LatLng dest;

        @Override
        public void onMapClick(LatLng latLng) {

            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(latLng);

            if (origin != null && dest != null) {
                origin = null;
                dest = null;
                googleMap.clear();
            }
            if (origin == null) {
                origin = latLng;
                markerOptions.title("Origin");
            } else if (dest == null) {
                dest = latLng;
                markerOptions.title("Destination");

                makeTrail(origin, dest);
            }
            googleMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
            googleMap.addMarker(markerOptions);
        }

        public void makeTrail(LatLng origin, LatLng dest) {

            if (android.os.Build.VERSION.SDK_INT > 9) {
                StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
                StrictMode.setThreadPolicy(policy);
            }

            TrailMaker md = new TrailMaker();

            Document doc = md.getDocument(origin, dest, TrailMaker.MODE_BICYCLING);
            ArrayList<LatLng> points = md.getDirection(doc);
            Log.i("MainActivity", "points # = " + points.size());
            if (points.size() == 0) {
                return;
            }

            ArrayList<LatLng> representatives = new ArrayList<LatLng>(REPRESENTATIVE_COUNT);

            int step = points.size() / REPRESENTATIVE_COUNT;
            if (step == 0) {
                representatives.addAll(points);
            } else {
                for (int i = 0; i < points.size(); i += step) {
                    representatives.add(points.get(i));
                }

                LatLng end = points.get(points.size() - 1);
                if (representatives.get(representatives.size() - 1) != end) {
                    representatives.add(end);
                }
            }

            double[] elevations = elevationService.getPath(representatives);

            int representativeIndex = 1;
            PolylineOptions options = new PolylineOptions().width(5);
            for (int i = 0; i < points.size(); i++) {
                options.add(points.get(i));
                if (points.get(i) != representatives.get(representativeIndex)) {
                    continue;
                }

                if (elevations[representativeIndex - 1] < elevations[representativeIndex]) {
                    options.color(Color.RED);
                } else {
                    options.color(Color.BLUE);
                }
                googleMap.addPolyline(options);
                options = new PolylineOptions().width(5);
                options.add(points.get(i));
                representativeIndex++;
            }

        }

    }
}
