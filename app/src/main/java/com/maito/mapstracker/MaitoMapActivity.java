package com.maito.mapstracker;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MaitoMapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap googleMapInstance;
    private Marker userCurrentMarker;
    private static final int LOCATION_PERMISSION_CODE = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Initialisation de la carte via le fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_fragment_view);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMapInstance = googleMap;

        // Configuration du point de départ par défaut (Paris, au lieu de Sydney pour le rendre unique)
        LatLng defaultLocation = new LatLng(48.8566, 2.3522);
        googleMapInstance.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 5f));

        checkPermissionsAndTrack();
    }

    private void checkPermissionsAndTrack() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocationTracking();
        } else {
            // Demande de permission au runtime
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_CODE
            );
        }
    }

    private void startLocationTracking() {
        LocationManager locManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        try {
            locManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    1000,
                    50,
                    new LocationListener() {
                        @Override
                        public void onLocationChanged(@NonNull Location location) {
                            updateMapLocation(location);
                        }

                        @Override
                        public void onProviderEnabled(@NonNull String provider) {
                            // Provider activé
                        }

                        @Override
                        public void onProviderDisabled(@NonNull String provider) {
                            checkGpsStatusAndPrompt();
                        }
                    }
            );
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private void updateMapLocation(Location location) {
        LatLng userPosition = new LatLng(location.getLatitude(), location.getLongitude());

        // Version propre : on recycle le marker au lieu d'en créer une infinité
        if (userCurrentMarker == null) {
            userCurrentMarker = googleMapInstance.addMarker(new MarkerOptions()
                    .position(userPosition)
                    .title(getString(R.string.marker_title)));
        } else {
            userCurrentMarker.setPosition(userPosition);
        }

        // Animation fluide de la caméra
        googleMapInstance.animateCamera(CameraUpdateFactory.newLatLngZoom(userPosition, 16f));
    }

    private void checkGpsStatusAndPrompt() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle(getString(R.string.gps_alert_title));
        dialogBuilder.setMessage(getString(R.string.gps_alert_message))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.gps_alert_yes), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton(getString(R.string.gps_alert_no), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        dialogBuilder.create().show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == LOCATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, getString(R.string.permission_granted), Toast.LENGTH_SHORT).show();
                // On relance le tracking puisque la permission est désormais accordée
                if (googleMapInstance != null) {
                    startLocationTracking();
                }
            } else {
                Toast.makeText(this, getString(R.string.permission_denied), Toast.LENGTH_LONG).show();
            }
        }
    }
}
