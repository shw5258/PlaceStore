package io.google.placestore;

import android.Manifest;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.algolia.instantsearch.helpers.InstantSearch;
import com.algolia.instantsearch.helpers.Searcher;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

import io.google.placestore.model.PlaceLatLng;

import static io.google.placestore.MapsActivity.PermissionInfoTask.DIALOG_FRAGMENT_TAG;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback,
        GeoQueryEventListener, GoogleMap.OnCameraIdleListener, GoogleMap.OnMarkerClickListener,
        View.OnClickListener, GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks, GoogleMap.OnMapClickListener{
    
    public static final String LAT_LNG = "LatLng";
    private static final int LOCATION_REQUEST = 1;
    private static final int PLACE_PICKER_REQUEST = 1;
    private static final String USED_LOCATION = "is location used before?";
    private static final String PERM_DENIED = "is permission denied?";
    private static final String FOR_DIRECTION = "is Starting Point needed?";
    private static final String SELECTED_PLACE = "selected place";
    private static final String TAG = MapsActivity.class.getSimpleName();
    private static final String MYLOCBUTTON = "is my location button clicked?";
    private GoogleMap mMap;
    private LatLngBounds.Builder mBounds = new LatLngBounds.Builder();
    private DatabaseReference mDatabase;
    private GeoFire mGeoFire;
    private GeoQuery mGeoQuery;
    private static final GeoLocation INITIAL_CENTER = new GeoLocation(37.546304, 126.995252);
    private static final int INITIAL_ZOOM_LEVEL = 14;
    private Map<String, Marker> mMarkers;
    private TextView mTitle, mContent;
    private Marker mCurrentMarker;
    private FloatingActionButton mDirectionFab, mMyLocFab;
    private GoogleApiClient mGoogleApiClient;
    private boolean mIsLocationUsedforPanning, mIsThisForDirection, mIsThisForMyLocButton,
            mIsRequestForPanDenied, mIsFabGrown;
    private String mCurrentPlaceId;
    private PlaceLatLng mSelectedPlaceLatLng;
    private BottomSheetBehavior mBottomSheetBehavior;
    private Animation mGrowAnimation, mShrinkAnimation;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mIsLocationUsedforPanning = savedInstanceState.getBoolean(USED_LOCATION);
            mIsRequestForPanDenied = savedInstanceState.getBoolean(PERM_DENIED);
            mIsThisForDirection = savedInstanceState.getBoolean(FOR_DIRECTION);
            mIsThisForMyLocButton = savedInstanceState.getBoolean(MYLOCBUTTON);
            mCurrentPlaceId = savedInstanceState.getString(SELECTED_PLACE);
        }
        setContentView(R.layout.activity_maps);
        mTitle = (TextView) findViewById(R.id.bottomSheetHeading);
        mContent = (TextView) findViewById(R.id.bottomSheetContent);
        mDirectionFab = (FloatingActionButton) findViewById(R.id.fabDirection);
        mMyLocFab = (FloatingActionButton) findViewById(R.id.fabMyLocation);
        mGrowAnimation = AnimationUtils.loadAnimation(this, R.anim.simple_grow);
        mShrinkAnimation = AnimationUtils.loadAnimation(this, R.anim.simple_shrink);
        if (getIntent().getStringExtra(SearchResultsActivity.EXTRA_MESSAGE) != null){
            mCurrentPlaceId = getIntent().getStringExtra(SearchResultsActivity.EXTRA_MESSAGE);
        }
    
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mDatabase = FirebaseDatabase.getInstance().getReference();
        if (mCurrentPlaceId != null) {
            fillinPersistentBottomSheetByDataId();
        }
        mGeoFire = new GeoFire(mDatabase.child("restaurants"));
        mGeoQuery = mGeoFire.queryAtLocation(INITIAL_CENTER, 1);
        mMarkers = new HashMap<>();
//        mDatabase.child(LAT_LNG).addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(DataSnapshot dataSnapshot) {
//                LatLng latLng = null;
//                for (DataSnapshot placeDataSnapshot : dataSnapshot.getChildren()) {
//                    PlaceLatLng placeLatLng = placeDataSnapshot.getValue(PlaceLatLng.class);
//                    latLng = new LatLng(placeLatLng.getLatitude(), placeLatLng.getLongitude());
//                    if (latLng != null) {
//                        mMap.addMarker(new MarkerOptions().position(latLng));
//                    }
//                }
//                mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
//            }
//
//            @Override
//            public void onCancelled(DatabaseError databaseError) {
//
//            }
//        });
        mBottomSheetBehavior = BottomSheetBehavior.from(findViewById(R.id.bottomSheetLayout));
        mBottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                switch (newState) {
                    case BottomSheetBehavior.STATE_DRAGGING:
                        if (mIsFabGrown) {
                            mDirectionFab.startAnimation(mShrinkAnimation);
                        }
                        break;
            
                    case BottomSheetBehavior.STATE_COLLAPSED:
                        mIsFabGrown = false;
                        mDirectionFab.setVisibility(View.INVISIBLE);
                        mDirectionFab.setOnClickListener(null);
                        break;
            
                    case BottomSheetBehavior.STATE_EXPANDED:
                        mDirectionFab.setVisibility(View.VISIBLE);
                        mDirectionFab.startAnimation(mGrowAnimation);
                        mDirectionFab.setOnClickListener(MapsActivity.this);
                        mIsFabGrown = true;
                        break;
                    case BottomSheetBehavior.STATE_HIDDEN:
                        mIsFabGrown = false;
                        mDirectionFab.setVisibility(View.INVISIBLE);
                        mDirectionFab.setOnClickListener(null);
                }
            }
    
            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                View view = findViewById(R.id.map);
                ViewGroup.LayoutParams params = view.getLayoutParams();
                view.getLayoutParams().height = (int) bottomSheet.getY();
                view.setLayoutParams(params);
            }
        });
        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }
    
    public void changeAnchorIDOfDirectionFabWithSingleValue(int gravity) {
        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) mDirectionFab.getLayoutParams();
        params.anchorGravity = gravity;
        mDirectionFab.setLayoutParams(params);
    }
    
    public void verifyClientAndDoAction() {
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .enableAutoManage(this /* FragmentActivity */,
                            this /* OnConnectionFailedListener */)
                    .addConnectionCallbacks(this)
                    .addApi(LocationServices.API)
                    .build();
            mGoogleApiClient.connect();
        } else {
            getDirectionOrPanToPosition();
        }
    }
    
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        
    }
    
    @Override
    public void onConnected(@Nullable Bundle bundle) {
    
        getDirectionOrPanToPosition();
    }
    
    private void getDirectionOrPanToPosition() {
        if (mGoogleApiClient != null) {
    
            Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            LatLng deviceLatLon = new LatLng(location.getLatitude(), location.getLongitude());
    
            if (mIsThisForDirection) {
                mIsThisForDirection = false;
                LatLng epLatLon = new LatLng(mSelectedPlaceLatLng.getLatitude(), mSelectedPlaceLatLng.getLongitude());
                LatLng spLatLon = deviceLatLon;
                Uri requestUri = Uri.parse(String.format("daummaps://route?sp=%f,%f&ep=%f,%f&by=PUBLICTRANSIT",
                        spLatLon.latitude, spLatLon.longitude, epLatLon.latitude,epLatLon.longitude));
                startActivity(new Intent(Intent.ACTION_VIEW, requestUri));
            }
    
            if (!mIsLocationUsedforPanning && !mIsThisForDirection) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(deviceLatLon, INITIAL_ZOOM_LEVEL));
                mIsLocationUsedforPanning = true;
            }
    
            if (mIsThisForMyLocButton) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(deviceLatLon, INITIAL_ZOOM_LEVEL));
                mIsThisForMyLocButton = false;
                mIsLocationUsedforPanning = true;
            }
        }
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(USED_LOCATION, mIsLocationUsedforPanning);
        outState.putBoolean(PERM_DENIED, mIsRequestForPanDenied);
        outState.putBoolean(FOR_DIRECTION, mIsThisForDirection);
        outState.putBoolean(MYLOCBUTTON, mIsThisForMyLocButton);
        outState.putString(SELECTED_PLACE, mCurrentPlaceId);
        super.onSaveInstanceState(outState);
    }
    
    @Override
    public void onConnectionSuspended(int i) {
        
    }
    
    public void requestPermReq(){
        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},LOCATION_REQUEST);
    }
    
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case LOCATION_REQUEST:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    verifyClientAndDoAction();
                    
                } else if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED){
                    mIsRequestForPanDenied = true;
                    if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                        new PermissionInfoTask().execute(getSupportFragmentManager());
                    } else {
                        requestPermReq();
                    }
                }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_action, menu);
    
        // Associate searchable configuration with the SearchView
//        final android.support.v7.widget.SearchView searchView = (android.support.v7.widget.SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.action_search));
//        searchView.setIconified(false);
//        searchView.setActivated(true);
//        SearchManager searchManager = (SearchManager) getSystemService(SEARCH_SERVICE);
//        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
//        searchView.setActivated(true);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_search:
                Intent searchIntent = new Intent(this, SearchResultsActivity.class);
                startActivity(searchIntent);
                return true;
            case R.id.pin_point:
                launchPlacePicker();
                return true;
            case R.id.action_change:
                CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) mDirectionFab.getLayoutParams();
                if(params.anchorGravity == Gravity.END){
                    params.anchorGravity = Gravity.START;
                }else {
                    params.anchorGravity = Gravity.END;
                }
                mDirectionFab.setLayoutParams(params);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    public void launchPlacePicker(){
        PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
        try {
            startActivityForResult(builder.build(this), PLACE_PICKER_REQUEST);
        } catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException e) {
            e.printStackTrace();
        }
    }
    
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PLACE_PICKER_REQUEST) {
            if (resultCode == RESULT_OK) {
                
                Place place = PlacePicker.getPlace(this, data);
                LatLng latLng = place.getLatLng();
                
                DatabaseReference placeRef = mDatabase.child(LAT_LNG).push();
                String key = placeRef.getKey();
                
                
                
                mGeoFire.setLocation(key, new GeoLocation(latLng.latitude,latLng.longitude), new GeoFire.CompletionListener() {
                    @Override
                    public void onComplete(String key, DatabaseError error) {
                        if (error != null) {
                            System.err.println("There was an error saving the location to GeoFire: " + error);
                        } else {
                            System.out.println("Location saved on server successfully!");
                        }
                    }
                });
                
                placeRef.setValue(new PlaceLatLng(
                        latLng.latitude,
                        latLng.longitude,
                        place.getAddress().toString(),
                        place.getName().toString()));
                mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
                String toastMsg = String.format("Place %s is added", place.getName());
                Toast.makeText(this, toastMsg, Toast.LENGTH_LONG).show();
            }
        }
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
        //만약 어플을 한국용으로만 출시한다면 이니셜센터를 한국으로 해야할 것이다. 현재는 글로벌 세팅
//            LatLng latLngCenter = new LatLng(INITIAL_CENTER.latitude, INITIAL_CENTER.longitude);
//            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLngCenter, INITIAL_ZOOM_LEVEL));
        mMap.setOnCameraIdleListener(this);
        mMap.setOnMarkerClickListener(this);
        mMap.setOnMapClickListener(this);
        if (!mIsRequestForPanDenied) {
            processPermissionRequestAndDoAction();
        }
    }
    
    private void processPermissionRequestAndDoAction() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            verifyClientAndDoAction();
        } else {
            requestPermReq();
        }
    }
    
    @Override
    public void onCameraIdle() {
        LatLng center = mMap.getCameraPosition().target;
        double radius = zoomLevelToRadius(mMap.getCameraPosition().zoom);
        mGeoQuery.setLocation(new GeoLocation(center.latitude, center.longitude), radius/700);
    }
    
    private double zoomLevelToRadius(double zoomLevel) {
        // Approximation to fit circle into view
        return 16384000/Math.pow(2, zoomLevel);
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        mGeoQuery.addGeoQueryEventListener(this);
        mMyLocFab.setOnClickListener(this);
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        mGeoQuery.removeAllListeners();
        for (Marker marker : mMarkers.values()) {
            marker.remove();
        }
        mMarkers.clear();
        mDirectionFab.setOnClickListener(null);
        mMyLocFab.setOnClickListener(null);
    }
    
    @Override
    protected void onDestroy() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(DIALOG_FRAGMENT_TAG);
        if (fragment != null) {
            Dialog dialog = ((MyLocationRequestDialogFragment) fragment).getmDialog();
            dialog.dismiss();
        }
        super.onDestroy();
    }
    
    @Override
    public void onKeyEntered(String key, GeoLocation location) {
        Marker marker = mMap.addMarker(new MarkerOptions()
                .position(new LatLng(location.latitude, location.longitude))
                .title(key)
                .snippet(key));
        mMarkers.put(key, marker);
    }
    
    @Override
    public void onKeyExited(String key) {
        Marker marker = mMarkers.get(key);
        if (marker != null) {
            marker.remove();
            mMarkers.remove(key);
        }
    }
    
    @Override
    public void onKeyMoved(String key, GeoLocation location) {
        Marker marker = mMarkers.get(key);
        if (marker != null) {
            animateMarkerTo(marker, location.latitude, location.longitude);
        }
    }
    
    // Animation handler for old APIs without animation support
    private void animateMarkerTo(final Marker marker, final double lat, final double lng) {
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        final long DURATION_MS = 3000;
        final Interpolator interpolator = new AccelerateDecelerateInterpolator();
        final LatLng startPosition = marker.getPosition();
        handler.post(new Runnable() {
            @Override
            public void run() {
                float elapsed = SystemClock.uptimeMillis() - start;
                float t = elapsed/DURATION_MS;
                float v = interpolator.getInterpolation(t);
                
                double currentLat = (lat - startPosition.latitude) * v + startPosition.latitude;
                double currentLng = (lng - startPosition.longitude) * v + startPosition.longitude;
                marker.setPosition(new LatLng(currentLat, currentLng));
                
                // if animation is not finished yet, repeat
                if (t < 1) {
                    handler.postDelayed(this, 16);
                }
            }
        });
    }
    
    @Override
    public void onGeoQueryReady() {
        
    }
    
    @Override
    public void onGeoQueryError(DatabaseError error) {
        
    }
    
    @Override
    public boolean onMarkerClick(Marker marker) {
        mCurrentMarker = marker;
        mCurrentPlaceId = marker.getTitle();
        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        fillinPersistentBottomSheetByDataId();
        return true;
    }
    
    private void fillinPersistentBottomSheetByDataId() {
        mDatabase.child(LAT_LNG).child(mCurrentPlaceId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                mSelectedPlaceLatLng = dataSnapshot.getValue(PlaceLatLng.class);
                
                if (mSelectedPlaceLatLng.getName() != null) {
                    mTitle.setText(mSelectedPlaceLatLng.getName());
                }
                if (mSelectedPlaceLatLng.getAddress() != null) {
                    mContent.setText(mSelectedPlaceLatLng.getAddress());
                }
            }
    
            @Override
            public void onCancelled(DatabaseError databaseError) {
        
            }
        });
    }
    
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.fabDirection:
                if (mSelectedPlaceLatLng != null) {
                    mIsThisForDirection = true;
                    processPermissionRequestAndDoAction();
                }
                break;
            case R.id.fabMyLocation:
                mIsThisForMyLocButton = true;
                processPermissionRequestAndDoAction();
                break;
        }
    }
    
    @Override
    public void onMapClick(LatLng latLng) {
        int state = mBottomSheetBehavior.getState();
        switch (state) {
            case BottomSheetBehavior.STATE_EXPANDED:
                mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                if (mIsFabGrown) mDirectionFab.startAnimation(mShrinkAnimation);
                break;
            case BottomSheetBehavior.STATE_COLLAPSED:
                mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                break;
            case BottomSheetBehavior.STATE_HIDDEN:
                mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }
    }
    
    
    
    public class PermissionInfoTask extends AsyncTask<FragmentManager, Void, Void> {
    
        public static final String DIALOG_FRAGMENT_TAG = "myFragment";
        MyLocationRequestDialogFragment dialogFragment;
    
        @Override
        protected Void doInBackground(FragmentManager... params) {
            dialogFragment = new MyLocationRequestDialogFragment();
            dialogFragment.show(params[0], DIALOG_FRAGMENT_TAG);
            return null;
        }
    }
}
