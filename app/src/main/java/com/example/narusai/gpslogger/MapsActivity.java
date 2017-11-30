package com.example.narusai.gpslogger;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatCallback;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApi;
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

import java.text.DateFormat;
import java.util.Date;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback/*, CompoundButton.OnCheckedChangeListener*/ {

    private FusedLocationProviderClient fusedLocationClient;
    private SettingsClient settingsClient;
    private LocationSettingsRequest locationSettingsRequest;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;
    private Location location;

    private GoogleMap mMap;
    private String lastUpdateTime;
    private int priority = 0;
    private Boolean requestingLocationUpdates;
    private static final int REQUEST_CHECK_SETTINGS = 0x1;
    private TextView textView;
    private String textLog;

    // テスト用の座標
    private LatLng mydaigaku = new LatLng(39.802802, 141.137441);

    private LatLng genzai; // サイトでいうLocation

    // CompoundButton cmap = (CompoundButton)findViewById(R.id.changeMap);
    // private ToggleButton cmap = (ToggleButton)findViewById(R.id.changeMap);

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

        /*
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,}, 1000);
        }
        else {
            locationStart();
        }
        */
        mapFragment.getMapAsync(this);



        // ToggleButtonの取得
       // cmap.setOnCheckedChangeListener(this);
    }

    /*public void onCheckedChanged(CompoundButton buttonView, boolean isChecked){
        if(isChecked) {
            mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        }else {
            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        }
    }*/

    private void createLocationCallback() {
        locationCallback = new LocationCallback() {
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);

                location = locationResult.getLastLocation();
                lastUpdateTime = DateFormat.getTimeInstance().format(new Date());
                genzai = new LatLng(location.getLatitude(), location.getLongitude());
                TextView textView = (TextView)findViewById(R.id.textView1);
                textView.setText("緯度:"+location.getLatitude()+" "+"経度:"+location.getLongitude());
                po.add(genzai);
                mMap.addPolyline(po);
            }
        };
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
    /*
    private void locationStart(){
        Log.d("debag", "locationStart");

        // LocationMnager インスタンス生成
        locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);

        final boolean gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (!gpsEnabled) {
            // GPSを設定するように促す
            Intent settingsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(settingsIntent);
            Log.d("debug", "not gpsEnable, startActivity");
        } else {
            Log.d("debug", "gpsEnabled");
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,}, 1000);

            Log.d("debug", "checkSelfPermission false");
            return;
        }

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10, 1, this);
    }
    */




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
        } else {
            // Show rationale and request permission.
        }

        // mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

       /* mMap.setOnMyLocationChangeListener(new GoogleMap.OnMyLocationChangeListener() {
            @Override
            public void onMyLocationChange(Location location) {
                TextView textView = (TextView)findViewById(R.id.textView1);

            }
        });*/

        // LocationManager locationManager = (LocationManager)this.getSystemService(Context.LOCATION_SERVICE);
        // Location myLocate = locationManager.getLastKnownLocation("gps");

        // 線色
        po.color(Color.MAGENTA);
        // 線幅
        po.width(4);

        // 点を追加
        // po.add(genzai);

        // Google Mapsに追加する
        // mMap.addPolyline(po);

        // CameraPosition cameraPos = new CameraPosition.Builder().target(jitku).zoom(10.0f).bearing(0).build();

        // マーカーセット
        mMap.addMarker(new MarkerOptions().position(mydaigaku).title("岩手県立大学"));
        // 初期カメラ視点を
        mMap.moveCamera(CameraUpdateFactory.newLatLng(mydaigaku));
        // mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPos));
        mMap.setTrafficEnabled(true);

    }

}
