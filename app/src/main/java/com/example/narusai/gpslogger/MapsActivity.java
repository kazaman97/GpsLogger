package com.example.narusai.gpslogger;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback/*, CompoundButton.OnCheckedChangeListener*/ {

    private FusedLocationProviderClient fusedLocationClient;
    private SettingsClient settingsClient;
    private LocationSettingsRequest locationSettingsRequest;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;
    private Location location;
    private String msg;
    private boolean sokui = false;

    private GoogleMap mMap;
    private int priority = 0;
    private Boolean requestingLocationUpdates;
    private static final int REQUEST_CHECK_SETTINGS = 0x1;

    // マーカー用の座標
    private LatLng mydaigaku = new LatLng(39.802802, 141.137441);
    private LatLng inobe = new LatLng(39.8002526, 141.1372295);

    private LatLng genzai; // サイトでいうLocation

    // 描画内容の設定
    PolylineOptions po = new PolylineOptions();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        settingsClient = LocationServices.getSettingsClient(this);

        createLocationCallback();
        createLocationRequest();
        buildLocationSettingsRequest();

        locationUpdates(); // サイトで言うstartLocationUpdates()

        mapFragment.getMapAsync(this);

    }

    private void createLocationCallback() {
        locationCallback = new LocationCallback() {
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);

                if (!sokui) {
                    msg = "測位しました";
                    toastMake(msg);
                    sokui = true;
                }

                ToggleButton toggleButton = (ToggleButton) findViewById(R.id.changeMap);

                location = locationResult.getLastLocation();
                genzai = new LatLng(location.getLatitude(), location.getLongitude());
                final TextView textView = (TextView)findViewById(R.id.textView1);
                final TextView speed = (TextView)findViewById(R.id.textView2);

                textView.setText("緯度:"+String.valueOf(String.format("%.3f", location.getLatitude()))+" "
                        +"経度:"+String.valueOf(String.format("%.3f", location.getLongitude())) +" "+"高度:"+String.valueOf(String.format("%.2f", location.getAltitude())));
                speed.setText("速度:"+String.valueOf(String.format("%.1f", location.getSpeed()))+"m/s");

                po.add(genzai);
                mMap.addPolyline(po);
                toggleButton.setOnCheckedChangeListener(
                        new CompoundButton.OnCheckedChangeListener() {
                            @Override
                            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                if (isChecked) {
                                    mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                                    textView.setTextColor(Color.GRAY);
                                    speed.setTextColor(Color.GRAY);
                                    toastMake("通常地図");
                                } else {
                                    mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                                    textView.setTextColor(Color.WHITE);
                                    speed.setTextColor(Color.WHITE);
                                    toastMake("航空地図");
                                }
                            }
                        }
                );
            }
        };
    }

    private void toastMake(String text){
        Toast toast = Toast.makeText(this, text, Toast.LENGTH_SHORT);
        toast.show();
    }

    private void createLocationRequest() {
        locationRequest = new LocationRequest();

        priority = 0;

        if (priority == 0) {
            // 高い精度の位置情報を取得したいとき
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        }else if (priority == 1) {
            // バッテリー消費を抑えたい場合、精度は100mと悪くなる
            locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        }else if (priority == 2) {
            // バッテリー消費を抑えたい場合、精度は10kmと悪くなる
            locationRequest.setPriority(LocationRequest.PRIORITY_LOW_POWER);
        }else {
            // 受け身的な位置情報取得でアプリが自ら測位せず、他のアプリで得られた位置情報は入手できる
            locationRequest.setPriority(LocationRequest.PRIORITY_NO_POWER);
        }

        locationRequest.setInterval(60000);

        locationRequest.setFastestInterval(5000);
    }

    private void buildLocationSettingsRequest(){
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(locationRequest);
        locationSettingsRequest = builder.build();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            // Check for the integer request code originally supplied to startResolutionForResult().
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        Log.i("debug", "User agreed to make required location settings changes.");
                        // Nothing to do. startLocationupdates() gets called in onResume again.
                        break;
                    case Activity.RESULT_CANCELED:
                        Log.i("debug", "User chose not to make required location settings changes.");
                        requestingLocationUpdates = false;
                        break;
                }
                break;
        }
    }

    private void locationUpdates() {
        // Begin by checking if the device has the necessary location settings.
        settingsClient.checkLocationSettings(locationSettingsRequest)
                .addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                        Log.i("debug", "All location settings are satisfied.");

                        // パーミッションの確認
                        if (ActivityCompat.checkSelfPermission(MapsActivity.this,
                                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                                && ActivityCompat.checkSelfPermission(MapsActivity.this,
                                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            // TODO: Consider calling
                            //    ActivityCompat#requestPermissions
                            // here to request the missing permissions, and then overriding
                            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                            //                                          int[] grantResults)
                            // to handle the case where the user grants the permission. See the documentation
                            // for ActivityCompat#requestPermissions for more details.
                            return;
                        }
                        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());

                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        int statusCode = ((ApiException) e).getStatusCode();
                        switch (statusCode) {
                            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                Log.i("debug", "Location settings are not satisfied. Attempting to upgrade " +
                                        "location settings ");
                                try {
                                    // Show the dialog by calling startResolutionForResult(), and check the
                                    // result in onActivityResult().
                                    ResolvableApiException rae = (ResolvableApiException) e;
                                    rae.startResolutionForResult(MapsActivity.this, REQUEST_CHECK_SETTINGS);
                                } catch (IntentSender.SendIntentException sie) {
                                    Log.i("debug", "PendingIntent unable to execute request.");
                                }
                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                String errorMessage = "Location settings are inadequate, and cannot be " +
                                        "fixed here. Fix in Settings.";
                                Log.e("debug", errorMessage);
                                Toast.makeText(MapsActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                                requestingLocationUpdates = false;
                        }

                    }
                });

        requestingLocationUpdates = true;
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }

        // 線色
        po.color(Color.MAGENTA);
        // 線幅
        po.width(4);

        // CameraPosition cameraPos = new CameraPosition.Builder().target(jitku).zoom(10.0f).bearing(0).build();

        msg = "測位中";
        toastMake(msg);

        // マーカーセット
        mMap.addMarker(new MarkerOptions().position(mydaigaku).title("岩手県立大学"));
        mMap.addMarker(new MarkerOptions().position(inobe).title("滝沢市IPUイノベーションセンター"));
        // 初期カメラ視点を
        mMap.moveCamera(CameraUpdateFactory.newLatLng(inobe));
        // mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPos));
        mMap.setTrafficEnabled(true);

    }

}
