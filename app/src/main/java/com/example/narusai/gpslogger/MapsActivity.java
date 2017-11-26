package com.example.narusai.gpslogger;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.common.api.GoogleApi;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener/*, CompoundButton.OnCheckedChangeListener*/ {

    private GoogleMap mMap;
    private LocationManager locationManager;
    // テスト用の座標
    private LatLng mydaigaku = new LatLng(39.802802, 141.137441);

    private LatLng genzai;

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

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,}, 1000);
        }
        else {
            locationStart();
        }
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

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        switch (status) {
            case LocationProvider.AVAILABLE:
                Log.d("debug", "LocationProvider.AVAILABLE");
                break;
            case LocationProvider.OUT_OF_SERVICE:
                Log.d("debug", "LocationProvider.OUT_OF_SERVICE");
                break;
            case LocationProvider.TEMPORARILY_UNAVAILABLE:
                Log.d("debug", "LocationProvider.TEMPORARILY_UNAVAILABLE");
                break;
        }
    }

    public void onLocationChanged(Location location) {
        genzai = new LatLng(location.getLatitude(), location.getLongitude());
        TextView textView = (TextView)findViewById(R.id.textView1);
        textView.setText("緯度:"+location.getLatitude()+" "+"経度:"+location.getLongitude());
        po.add(genzai);
        mMap.addPolyline(po);
    }

    public void onProviderEnabled(String provider) {

    }

    public void onProviderDisabled(String provider) {

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
