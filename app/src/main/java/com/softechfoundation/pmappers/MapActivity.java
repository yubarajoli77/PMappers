package com.softechfoundation.pmappers;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.geojson.GeoJsonFeature;
import com.google.maps.android.geojson.GeoJsonLayer;
import com.google.maps.android.geojson.GeoJsonPointStyle;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mGoogleMap;
    private Marker marker;
    private Geocoder geocoder;
    private LatLng latLng;
    public static boolean isCalled = false;
    private List<Address> addresses = new ArrayList<>();
    private boolean isInfoWindowShown = false;
    private int PLACE_AUTOCOMPLETE_REQUEST_CODE = 1;
    private static final String TAG = MapActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (googleServicesAvailable()) {
            setContentView(R.layout.activity_map);

        } else {
            //No Google map layout
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        defineView();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.map_menu, menu);
        return true;
    }

    private void defineView() {
        geocoder = new Geocoder(this, Locale.getDefault());
        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.fragment);
        mapFragment.getMapAsync(this);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.menu_search) {
            googleSearchBox();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private void googleSearchBox() {
        try {
            AutocompleteFilter typeFilter = new AutocompleteFilter.Builder()
                    .setTypeFilter(AutocompleteFilter.TYPE_FILTER_ADDRESS)
                    .setCountry("NP")
                    .build();
            Intent intent =
                    new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_OVERLAY)
                            .setFilter(typeFilter)
                            .build(this);
            startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE);
        } catch (GooglePlayServicesRepairableException e) {
            // TODO: Handle the error.
        } catch (GooglePlayServicesNotAvailableException e) {
            // TODO: Handle the error.
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            GeoJsonLayer layer = new GeoJsonLayer(mGoogleMap, R.raw.pmapper_geojson,
                    getApplicationContext());
            layer.addLayerToMap();

            final GeoJsonPointStyle pointStyle = layer.getDefaultPointStyle();
            pointStyle.setDraggable(false);
            pointStyle.setTitle("Toilet");
            pointStyle.setSnippet("You can use this for emergency");
            Bitmap icon = BitmapFactory.decodeResource(this.getResources(), R.drawable.pmapper);
            Bitmap smallMarker = Bitmap.createScaledBitmap(icon, 120, 150, false);
            BitmapDescriptor icon1 = BitmapDescriptorFactory.fromBitmap(smallMarker);
            pointStyle.setIcon(icon1);

            layer.setOnFeatureClickListener(new GeoJsonLayer.GeoJsonOnFeatureClickListener() {
                @Override
                public void onFeatureClick(GeoJsonFeature geoJsonFeature) {
                    pointStyle.setTitle(geoJsonFeature.getProperty("name"));
                }
            });

        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }

        if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Place place = PlaceAutocomplete.getPlace(this, data);
                LatLng latLng = place.getLatLng();
                try {
                    addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                android.location.Address address = addresses.get(0);
                addMarker(latLng, address);
            } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                Status status = PlaceAutocomplete.getStatus(this, data);
                // TODO: Handle the error.
                Log.i(TAG, status.getStatusMessage());

            } else if (resultCode == RESULT_CANCELED) {
                // The user canceled the operation.
            }
        }
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;
        if (isCalled) {
            googleSearchBox();
            isCalled=false;
        }
        boolean success = googleMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                        this, R.raw.style_json));

        if (!success) {
            Log.e(TAG, "Style parsing failed.");
        }

        if (mGoogleMap != null) {

            //restrict the user to go outside the given latlng
            //get latlong for corners for specified place
            LatLng one = new LatLng(27.037782, 78.947213);
            LatLng two = new LatLng(29.615528, 88.627444);

            LatLngBounds.Builder builder = new LatLngBounds.Builder();

            //add them to builder
            builder.include(one);
            builder.include(two);

            LatLngBounds bounds = builder.build();

            //get width and height to current display screen
            int width = getResources().getDisplayMetrics().widthPixels;
            int height = getResources().getDisplayMetrics().heightPixels;

            // 20% padding
            int padding = (int) (width * 0.20);

            //set latlong bounds
            mGoogleMap.setLatLngBoundsForCameraTarget(bounds);

            //move camera to fill the bound to screen
            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, width, height, padding));

            //set zoom to level to current so that you won't be able to zoom out viz. move outside bounds
            //mGoogleMap.setMinZoomPreference(mGoogleMap.getCameraPosition().zoom);
            mGoogleMap.setMinZoomPreference(6.0f);
            setUpMap();
        }


    }

    private void goToLocationZoom(double lat, double lng, int zoom) {
        final LatLng[] latLng = {new LatLng(lat, lng)};
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng[0], zoom);
        //add animation to zoom
        mGoogleMap.animateCamera(cameraUpdate, 1000, null);

        //zoom without animation
//        mGoogleMap.moveCamera(cameraUpdate);
//        mGoogleMap.setMinZoomPreference(10.0f);

//        LatLngBounds ADELAIDE = new LatLngBounds(
//                new LatLng(26.726658, 88.242621), new LatLng(29.774378, 80.311209));
//        // Constrain the camera target to the Adelaide bounds.
//        mGoogleMap.setLatLngBoundsForCameraTarget(ADELAIDE);
    }

    private void setUpMap() {

        if (ActivityCompat.checkSelfPermission(this, android.Manifest
                .permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat
                .checkSelfPermission(this, android.Manifest
                        .permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            Toast.makeText(this, "Grant The permission first", Toast.LENGTH_LONG).show();
            return;
        }
        mGoogleMap.setMyLocationEnabled(true);
        mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mGoogleMap.getUiSettings().setMapToolbarEnabled(false);

        mGoogleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {

            @Override
            public void onMapClick(LatLng point) {
                //save current locationq
                latLng = point;
                Log.d("latlng::", latLng.toString());

                try {
                    addresses = geocoder.getFromLocation(point.latitude, point.longitude, 1);
                    android.location.Address address = addresses.get(0);
                    addMarker(point, address);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });

        mGoogleMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
            @Override
            public boolean onMyLocationButtonClick() {

                LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                if (ActivityCompat.checkSelfPermission(MapActivity.this, android.Manifest
                        .permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat
                        .checkSelfPermission(MapActivity.this, android.Manifest
                                .permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return false;
                }
                Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
//                Log.d("locationEmptyCheck:", location.toString());
                double latti, longi;
                if (location != null) {
                    latti = location.getLatitude();
                    longi = location.getLongitude();
                    Log.d("lattiAndLongi::", latti + ", " + longi);
                    LatLng latLng = new LatLng(latti, longi);

                    try {
                        addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
                        android.location.Address address = addresses.get(0);
                        addMarker(latLng, address);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return false;
            }
        });


    }

    public boolean googleServicesAvailable() {
        GoogleApiAvailability api = GoogleApiAvailability.getInstance();
        int isAvailable = api.isGooglePlayServicesAvailable(this);
        if (isAvailable == ConnectionResult.SUCCESS) {
            return true;
        } else if (api.isUserResolvableError(isAvailable)) {
            Dialog dialog = api.getErrorDialog(this, isAvailable, 0);
            dialog.show();
        } else {
            Toast.makeText(this, "Can not connect to google play services", Toast.LENGTH_LONG).show();
        }
        return false;
    }

    private void addMarker(LatLng point, Address address) {

        //remove previously placed Marker
        if (marker != null) {
            marker.remove();
            marker = null;
        }

        //place marker where user just clicked
        String locality = address.getLocality();
        String adminArea = address.getAdminArea();
        String subAdminArea = address.getSubAdminArea();
        String country = address.getCountryName();
        Log.d("address", locality + ", " + adminArea + ", " + subAdminArea + ", " + country);
        MarkerOptions markerOptions = new MarkerOptions()
                .title(locality)
                .position(point)
                .snippet(adminArea + ", " + subAdminArea + ", " + country)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)).draggable(true);

        marker = mGoogleMap.addMarker(markerOptions);
        // marker.showInfoWindow();
        goToLocationZoom(point.latitude, point.longitude, 15);

        Toast.makeText(MapActivity.this, locality + "\n" + adminArea + ","
                + subAdminArea + ", " + ", " + country, Toast.LENGTH_SHORT).show();


        mGoogleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {

                // isShowInfoWindow() always give false value so we use this method to show and hide infoWindow
                if (!isInfoWindowShown) {

                    marker.showInfoWindow();
                    mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(marker.getPosition()));
                    isInfoWindowShown = true;
                } else {

                    marker.hideInfoWindow();
                    isInfoWindowShown = false;
                }
                return true;
            }
        });
        mGoogleMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker arg0) {
            }

            @SuppressWarnings("unchecked")
            @Override
            public void onMarkerDragEnd(Marker arg0) {
                Log.d("System out", "onMarkerDragEnd...");
                mGoogleMap.animateCamera(CameraUpdateFactory.newLatLng(arg0.getPosition()));
                double latti, longi;
                if (arg0 != null) {
                    latti = arg0.getPosition().latitude;
                    longi = arg0.getPosition().longitude;
                    Log.d("lattiAndLongi::", latti + ", " + longi);
                    LatLng latLng = new LatLng(latti, longi);
                    try {
                        List<Address> addressess = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    android.location.Address address = addresses.get(0);
                    addMarker(latLng, address);
                }
            }

            @Override
            public void onMarkerDrag(Marker arg0) {

            }
        });

    }
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

}
