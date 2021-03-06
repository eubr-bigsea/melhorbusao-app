package br.edu.ufcg.analytics.meliorbusao.activities;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.res.AssetManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.PersistableBundle;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;


import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.roughike.bottombar.BottomBar;
import com.roughike.bottombar.BottomBarTab;
import com.roughike.bottombar.OnMenuTabClickListener;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;

import br.edu.ufcg.analytics.meliorbusao.EndpointCheckoutDataBigsea;
import br.edu.ufcg.analytics.meliorbusao.db.CityDataManager;
import br.edu.ufcg.analytics.meliorbusao.Constants;
import br.edu.ufcg.analytics.meliorbusao.MeliorBusaoApplication;
import br.edu.ufcg.analytics.meliorbusao.NotificationTrigger;
import br.edu.ufcg.analytics.meliorbusao.db.MeliorDBOpenHelper;
import br.edu.ufcg.analytics.meliorbusao.exceptions.NoDataForCityException;
import br.edu.ufcg.analytics.meliorbusao.fragments.ChangePasswordFragment;
import br.edu.ufcg.analytics.meliorbusao.fragments.ItinerariesListFragment;
import br.edu.ufcg.analytics.meliorbusao.fragments.ItineraryMapFragment;
import br.edu.ufcg.analytics.meliorbusao.listeners.BigseaLoginListener;
import br.edu.ufcg.analytics.meliorbusao.listeners.OnFinishedParseListener;
import br.edu.ufcg.analytics.meliorbusao.R;
import br.edu.ufcg.analytics.meliorbusao.fragments.RoutesMapFragment;
import br.edu.ufcg.analytics.meliorbusao.fragments.NearStopsFragment;
import br.edu.ufcg.analytics.meliorbusao.fragments.StopScheduleFragment;
import br.edu.ufcg.analytics.meliorbusao.fragments.SearchScheduleFragment;
import br.edu.ufcg.analytics.meliorbusao.fragments.TopBusFragment;
import br.edu.ufcg.analytics.meliorbusao.models.Route;
import br.edu.ufcg.analytics.meliorbusao.models.StopHeadsign;
import br.edu.ufcg.analytics.meliorbusao.models.otp.Itinerary;
import br.edu.ufcg.analytics.meliorbusao.services.LocationService;
import br.edu.ufcg.analytics.meliorbusao.services.RatingsService;
import br.edu.ufcg.analytics.meliorbusao.utils.ParseUtils;
import br.edu.ufcg.analytics.meliorbusao.utils.SharedPreferencesUtils;

public class MelhorBusaoActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnFinishedParseListener,
        TopBusFragment.OnTopBusSelectedListener, NearStopsFragment.NearStopListener, SearchScheduleFragment.SearchScheduleListener,
        FragmentManager.OnBackStackChangedListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, SearchScheduleFragment.GetDirectionsListener,
        ItinerariesListFragment.OnItinerarySelectedListener, BigseaLoginListener,
        ChangePasswordFragment.OnPasswordChangedListener{

    public static final String TAG = "MelhorBusaoActivity";
    private static final int RC_SIGN_IN = 0;

    private TopBusFragment topBusFragment;
    private NearStopsFragment nearStopsFragment;
    private RoutesMapFragment routesMapFragment;
    private SearchScheduleFragment searchScheduleFragment;
    private StopScheduleFragment stopScheduleFragment;
    private ItinerariesListFragment itinerariesListFragment;
    private ItineraryMapFragment itineraryMapFragment;
    private ChangePasswordFragment changePasswordFragment;

    /* Client used to interact with Google APIs. */
    private GoogleApiClient mGoogleApiClient;
    /* Should we automatically resolve ConnectionResults when possible? */
    private boolean mShouldResolve = false;
    /* Is there a ConnectionResult resolution in progress? */
    private boolean mIsResolving = false;
    private boolean mSignedIn = false;
    private LocationCallback locationCalback;
    protected Location mLastLocation;

    private BottomBar mBottomBar;
    private NavigationView mDrawerNav;
    private DrawerLayout mDrawerLayout;
    private Toolbar mToolbar;

    private int pendingDBOperations;
    private static ProgressDialog mDialog;
    private String cityName;
  //  private static ProgressDialog requestingLocationDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_melior_busao);

        topBusFragment = TopBusFragment.getInstance();
        nearStopsFragment = NearStopsFragment.getInstance();
        routesMapFragment = RoutesMapFragment.getInstance();
        searchScheduleFragment = SearchScheduleFragment.getInstance();
        stopScheduleFragment = StopScheduleFragment.getInstance();
        itinerariesListFragment = ItinerariesListFragment.getInstance();
        itineraryMapFragment = ItineraryMapFragment.newInstance();
        changePasswordFragment = ChangePasswordFragment.newInstance();

        mGoogleApiClient = ((MeliorBusaoApplication) getApplication()).getGoogleApiClientInstance(this);
        mGoogleApiClient.registerConnectionCallbacks(this);
        mGoogleApiClient.registerConnectionFailedListener(this);
        mGoogleApiClient.connect();

        startService(new Intent(this, LocationService.class));

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mToolbar.setTitleTextColor(getResources().getColor(R.color.toolbar_text_color));
        setSupportActionBar(mToolbar);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, mDrawerLayout, mToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mDrawerLayout.setDrawerListener(toggle);
        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        toggle.setDrawerIndicatorEnabled(false);
        toggle.syncState();

        mDrawerNav = (NavigationView) findViewById(R.id.nav_view);
        mDrawerNav.setCheckedItem(R.id.nav_top_bus);

        mDrawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
            }

            @Override
            public void onDrawerOpened(View drawerView) {
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                FragmentManager.BackStackEntry backEntry = getBackStackEntry();
                String currentFrag = backEntry.getName();
                changeBottomBarItem(currentFrag);
            }

            @Override
            public void onDrawerStateChanged(int newState) {
            }
        });

        getNavigationView().setNavigationItemSelectedListener(this);

        getSupportFragmentManager().beginTransaction().replace(R.id.container_layout,
                topBusFragment, TopBusFragment.TAG).addToBackStack(TopBusFragment.TAG).commit();

        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.addOnBackStackChangedListener(this);

        // Bottom bar navigation menu
        mBottomBar = BottomBar.attach(this, savedInstanceState);
        mBottomBar.noTopOffset();
        mBottomBar.noNavBarGoodness();
        mBottomBar.setItems(R.menu.bottombar_menu);
        mBottomBar.setTextAppearance(R.style.bottom_bar_text);
        mBottomBar.setDefaultTabPosition(0);

        // Start service to send local ratings to server
        RatingsService service = new RatingsService();
        service.sendLocalRatings(this);
    }

    private void loadCityData() {
        if (!SharedPreferencesUtils.isShapesOnDatabase(this)) {
            AssetManager assetManager = getAssets();
            CityDataManager.downloadDB(this, this, assetManager);
            pendingDBOperations++;

            mDialog = ProgressDialog.show(this, getString(R.string.msg_wait), getString(R.string.msg_loading_data), true, false);
        }
    }

    public static void dismissLoadingDialog() {
        mDialog.dismiss();
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        // Necessary to restore the BottomBar's state, otherwise we would
        // lose the current tab on orientation change.
        mBottomBar.onSaveInstanceState(outState);
    }

    /**
     * Connect to Google Services
     */
    @Override
    protected void onStart() {
        super.onStart();
        if (!mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
            Log.d("status con", String.valueOf(mGoogleApiClient.isConnected()));
        }
        Log.d(TAG, SharedPreferencesUtils.getUserToken(this));
        initializeBottomNavigation();
    }

    /**
     * Navigate between fragments when clicking the bottom bar navigation
     */
    private void initializeBottomNavigation() {
        mBottomBar.setOnMenuTabClickListener(new OnMenuTabClickListener() {
            @Override
            public void onMenuTabSelected(@IdRes int menuItemId) {
                MenuItem selectedItem = null;
                switch (menuItemId) {
                    case R.id.bb_topbus:
                        selectedItem = mDrawerNav.getMenu().findItem(R.id.nav_top_bus);
                        break;
                    case R.id.bb_near_stops:
                        selectedItem = mDrawerNav.getMenu().findItem(R.id.nav_near_stops);
                        break;
                    case R.id.bb_routes_map:
                        selectedItem = mDrawerNav.getMenu().findItem(R.id.nav_routes_map);
                        break;
                    case R.id.bb_predicted_schedule:
                        selectedItem = mDrawerNav.getMenu().findItem(R.id.nav_predicted_schedule);
                        break;
                    case R.id.bb_menu:
                        mDrawerLayout.openDrawer(Gravity.LEFT);
                        break;
                }
                if (selectedItem != null) {
                    onNavigationItemSelected(selectedItem);
                }
            }

            @Override
            public void onMenuTabReSelected(@IdRes int menuItemId) {
                Fragment nextFrag = null;
                String nextFragTag = null;
                switch (menuItemId) {
                    case R.id.bb_topbus:
                        topBusFragment.resetTopBusList();
                        nextFrag = topBusFragment;
                        nextFragTag = TopBusFragment.TAG;
                        break;
                    case R.id.bb_near_stops:
                        nextFrag = nearStopsFragment;
                        nextFragTag = NearStopsFragment.TAG;
                        break;
                    case R.id.bb_routes_map:
                        routesMapFragment.setRoute(null);
                        nextFrag = routesMapFragment;
                        nextFragTag = RoutesMapFragment.TAG;

                        break;
                    case R.id.bb_predicted_schedule:
                        nextFrag = searchScheduleFragment;
                        nextFragTag = SearchScheduleFragment.TAG;
                        break;
                    case R.id.bb_menu:
                        mDrawerLayout.closeDrawer(Gravity.LEFT);
                        FragmentManager.BackStackEntry backEntry = getBackStackEntry();
                        String currentFrag = backEntry.getName();
                        changeBottomBarItem(currentFrag);
                }


                if (nextFrag != null && nextFragTag != null) {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.container_layout, nextFrag, nextFragTag)
                            .commit();

                }
            }
        });
    }

    /**
     * Returns the back Stack entry;
     */
    private FragmentManager.BackStackEntry getBackStackEntry() {
        return getSupportFragmentManager().getBackStackEntryAt(
                getSupportFragmentManager().getBackStackEntryCount() - 1);
    }

    private void changeBottomBarItem(String currentFragment) {
        switch (currentFragment) {
            case TopBusFragment.TAG:
                mBottomBar.selectTabAtPosition(0, true);
                break;
            case NearStopsFragment.TAG:
                mBottomBar.selectTabAtPosition(1, true);
                break;
            case RoutesMapFragment.TAG:
                mBottomBar.selectTabAtPosition(2, true);
                break;
            case SearchScheduleFragment.TAG:
                mBottomBar.selectTabAtPosition(3, true);
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        getSupportActionBar().setTitle(getResources().getString(R.string.app_name));
    }

    /**
     * Disconnect from Google Services.
     */
    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
        if (mDialog != null && mDialog.isShowing()){
            mDialog.dismiss();
        }
    }

    /**
     * Define o fluxo das telas dando um override no butão 'back'
     */
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            FragmentManager fm = getSupportFragmentManager();
            if (fm.getBackStackEntryCount() > 0) {
                fm.popBackStack();
            }
        }
    }

    /**
     * Criar o menu de notificações falsas no modo debug (quando deseja-se gerar notificação)
     */
    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.melior_busao_action_bar, menu);

        if (Constants.DEBUG_NOTIFICATION) {
            menu.findItem(R.id.action_show_notification_single).setVisible(true);
            menu.findItem(R.id.action_show_notification).setVisible(true);
        }
        menu.findItem(R.id.action_search).setVisible(false);
        switchMonitoring(getBaseContext());
//        showSignedInUI();
        return true;
    }

    /**
     * Define se a detecção automática esta ativada ou não (está ou não no ônibus)
     */
    private void switchMonitoring(final Context context) {
        if (findViewById(R.id.swicth_bus_monitoring) != null) {
            Switch sw = (Switch) findViewById(R.id.swicth_bus_monitoring);
            sw.setChecked(SharedPreferencesUtils.isBusMonitoring(context));
            sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        SharedPreferencesUtils.setBusMonitoring(context, true);
                    } else {
                        SharedPreferencesUtils.setBusMonitoring(context, false);
                    }
                }
            });
        }
    }

    /**
     * Cria o evento pra quando o item de notificação falsa for selecionado (do menu)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.action_show_notification_single:
                new NotificationTrigger(this).fakeRatingNotification(1);
                return true;

            case R.id.action_show_notification:
                new NotificationTrigger(this).fakeRatingNotification(3);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Menu da aplicação
     *
     * @param item
     * @return
     */
    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        Fragment nextFrag = null;
        String prevFragTag, nextFragTag = null;

        FragmentManager.BackStackEntry backEntry = getBackStackEntry();
        prevFragTag = backEntry.getName();

        switch (id) {
            case R.id.nav_sign_out:
                onSignOutClicked();
                break;

            case R.id.nav_about:
                Toast.makeText(getBaseContext(), "Desculpe, ainda não está pronto :(", Toast.LENGTH_LONG).show();
                break;

            case R.id.nav_favorites:
                Toast.makeText(getBaseContext(), "Desculpe, ainda não está pronto :(", Toast.LENGTH_LONG).show();
                break;

            case R.id.nav_top_bus:
                topBusFragment.resetTopBusList();
                nextFrag = topBusFragment;
                nextFragTag = TopBusFragment.TAG;
                break;

            case R.id.nav_near_stops:
                nextFrag = nearStopsFragment;
                nextFragTag = NearStopsFragment.TAG;
                break;

            case R.id.nav_routes_map:
                routesMapFragment.setRoute(null);
                nextFrag = routesMapFragment;
                nextFragTag = RoutesMapFragment.TAG;
                break;

            case R.id.nav_predicted_schedule:
                nextFrag = searchScheduleFragment;
                nextFragTag = SearchScheduleFragment.TAG;
                break;

            case R.id.nav_change_password:
                nextFrag = changePasswordFragment;
                nextFragTag = ChangePasswordFragment.TAG;
                break;

        }

        if (nextFrag != null) {
            if (nextFragTag.equals(prevFragTag)) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.container_layout, nextFrag, nextFragTag)
                        .commit();
            } else {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.container_layout, nextFrag, nextFragTag)
                        .addToBackStack(nextFragTag)
                        .commit();
            }
            changeBottomBarItem(nextFragTag);
        }

        mDrawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onConnected(Bundle bundle) {
        mShouldResolve = false;
        try {
            showSignedInUI();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        if (isLocationEnabled()) {
            startLocationUpdates();
        } else {
            buildAlertMessageNoGps();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.w(TAG, "onConnectionSuspended:" + i);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        // Could not connect to Google Play Services.  The user needs to select an account,
        // grant permissions or resolve an error in order to sign in. Refer to the javadoc for
        // ConnectionResult to see possible error codes.
        Log.d(TAG, "onConnectionFailed:" + connectionResult);

        if (!mIsResolving && mShouldResolve) {
            if (connectionResult.hasResolution()) {
                try {
                    connectionResult.startResolutionForResult(this, RC_SIGN_IN);
                    mIsResolving = true;
                } catch (IntentSender.SendIntentException e) {
                    Log.e(TAG, "Could not resolve ConnectionResult.", e);
                    mIsResolving = false;
                    mGoogleApiClient.connect();
                }
            } else {
                // Could not resolve the connection result, show the user an error dialog.
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        System.out.print("works outside!");
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                mIsResolving = false;
                mGoogleApiClient.connect();
            }
            if (resultCode == RESULT_CANCELED) {
                mShouldResolve = false;
            }
        }

    }

    private NavigationView getNavigationView() {
        return (NavigationView) findViewById(R.id.nav_view);
    }

    /**
     * View quando está logado na conta do Google
     */
    private void showSignedInUI(){
        mSignedIn = true;

        getNavigationView().findViewById(R.id.signed_in_header).setVisibility(View.VISIBLE);

        updateSignInOutMenus();

        if (SharedPreferencesUtils.getUsername(getBaseContext())!= ""){
            TextView nameTextView = (TextView) getNavigationView().findViewById(R.id.nameTextView);
            nameTextView.setText(getResources().getString(R.string.saudation_message) + SharedPreferencesUtils.getUsername(getBaseContext()) + " :-)");
        }


        if (mGoogleApiClient.isConnected()){
            OptionalPendingResult<GoogleSignInResult> opr = Auth.GoogleSignInApi.silentSignIn(mGoogleApiClient);
            if (opr.isDone()){
                GoogleSignInResult result = opr.get();
                if (result.getSignInAccount() != null){
                    TextView nameTextView = (TextView) getNavigationView().findViewById(R.id.nameTextView);
                    nameTextView.setText(getResources().getString(R.string.saudation_message) + result.getSignInAccount().getDisplayName() + " :-)");

                    String personPhoto = String.valueOf(result.getSignInAccount().getPhotoUrl());
                    ImageView userImageView = (ImageView) getNavigationView().findViewById(R.id.userImageView);
                   // ProfileImageLoader loader = new ProfileImageLoader(userImageView);
                  //  loader.execute(personPhoto);
                }
            }
        }
    }


    /**
     * Serviços de login / logout do Google
     */
    private void updateSignInOutMenus() {
        getNavigationView().getMenu().findItem(R.id.nav_sign_out).setVisible(mSignedIn);
        // hack to update menu (appcompat v23 has bugs)
        getNavigationView().inflateMenu(R.menu.empty_menu);
    }

    /**
     * Logouts the user from de application
     */
    private void onSignOutClicked() {


        String authService = SharedPreferencesUtils.getAuthService(this);

        if (authService.equals(Constants.GOOGLE_SERVICE)) {

            if (mGoogleApiClient.isConnected()) {
                signOutWithGoogle();
            } else {
                mGoogleApiClient.connect();
                mGoogleApiClient.registerConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(@Nullable Bundle bundle) {
                        signOutWithGoogle();
                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                        Log.d(TAG, "onConnectionSuspended: It was not possible to sign out");
                        Toast.makeText(MelhorBusaoActivity.this, getString(R.string.error_could_not_signout), Toast.LENGTH_SHORT).show();
                    }
                });
            }

        } else if (authService.equals(Constants.BIG_SEA_SERVICE)) {
            if(SharedPreferencesUtils.getUserToken(MelhorBusaoActivity.this.getBaseContext())!=""){
                EndpointCheckoutDataBigsea checkoutDataBigsea = new EndpointCheckoutDataBigsea(this,SharedPreferencesUtils.getUserToken(MelhorBusaoActivity.this.getBaseContext()));
                checkoutDataBigsea.execute();
            }

        }




    }

    /**
     * Sign outs  the user from the application using Google Sign In API and goes to the sign in screen.
     */
    private void signOutWithGoogle() {
        Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(Status status) {
                if (status.isSuccess()) {
                    Intent intent = new Intent(getApplicationContext(), MelhorLoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                }
            }
        });
    }

    @Override
    public void finishedParse(int kind) {
        pendingDBOperations--;
        if (pendingDBOperations == 0) {
            if (mDialog != null && mDialog.isShowing()) {
                mDialog.dismiss();
            }
            ParseUtils.getSumario(this, topBusFragment);
        }
    }

    /**
     * No card, ao clicar o botão de visualizar a rota
     */
    @Override
    public void onBusCardClickListener(String routeShortName) {

        getSupportFragmentManager().beginTransaction().replace(R.id.container_layout,
                routesMapFragment, RoutesMapFragment.TAG).addToBackStack(RoutesMapFragment.TAG).commit();

        changeBottomBarItem(RoutesMapFragment.TAG);

        routesMapFragment.setRoute(routeShortName);

    }

    /**
     * No card, ao clicar o botão de pegar o busão
     */
    @Override
    public void onTakeBusButtonClickListener(Route routeSelected) {
        Bundle args = searchScheduleFragment.getArguments();
        args.putString(searchScheduleFragment.SELECTED_ROUTE_KEY, routeSelected.getShortName());
        getSupportFragmentManager().beginTransaction().replace(R.id.container_layout,
                searchScheduleFragment, SearchScheduleFragment.TAG).addToBackStack(SearchScheduleFragment.TAG).commit();
        changeBottomBarItem(SearchScheduleFragment.TAG);
    }

    /**
     * Muda o titulo das activities
     */
    @Override
    public void onTitleChange(String newTitle) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(newTitle);
        }
    }

    public void setActionBarTitle(String newTitle) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(newTitle);
        }
    }

    /**
     * Em paradas próximas, exibe as informações das rotas e das paradas
     */
    @Override
    public void onInfoWindowClick(HashSet<Route> routes, String stopName) {

        getSupportFragmentManager().beginTransaction().replace(R.id.container_layout,
                topBusFragment, TopBusFragment.TAG).addToBackStack(TopBusFragment.TAG).commit();

        changeBottomBarItem(TopBusFragment.TAG);
        topBusFragment.setStopRoutesToDisplay(stopName, routes);

    }


    /**
     * Indica qual fragment será exibido/adicionado na pilha
     */
    @Override
    public void onBackStackChanged() {
        FragmentManager fm = getSupportFragmentManager();
        int backEntryCount = fm.getBackStackEntryCount();
        if (backEntryCount == 0) {
            finish();
        } else {
            FragmentManager.BackStackEntry backEntry = fm.getBackStackEntryAt(backEntryCount - 1);
            String str = backEntry.getName();
            Fragment fragment = fm.findFragmentByTag(str);
            NavigationView drawer = (NavigationView) findViewById(R.id.nav_view);

            if (fragment instanceof TopBusFragment) {
                drawer.setCheckedItem(R.id.nav_top_bus);
            } else if (fragment instanceof NearStopsFragment) {
                drawer.setCheckedItem(R.id.nav_near_stops);
            } else if (fragment instanceof RoutesMapFragment) {
                drawer.setCheckedItem(R.id.nav_routes_map);
            } else if (fragment instanceof SearchScheduleFragment) {
                drawer.setCheckedItem(R.id.nav_predicted_schedule);
            }
        }
    }

    /**
     * Verifica a conexão do usuário na interwebs
     *
     * @return
     */
    public boolean checkInternetConnection() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        return (cm.getActiveNetworkInfo() != null
                && cm.getActiveNetworkInfo().isAvailable()
                && cm.getActiveNetworkInfo().isConnected());
    }

    /**
     * Verifica se a localização está disponível
     */
    public boolean isLocationEnabled() {
        LocationManager mlocManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return mlocManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                mlocManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    /**
     * Na activity Pegar Busão faz o redirecionamento para a tela dos próximos horários do bus
     *
     * @param stopHeadsign
     */
    @Override
    public void onClickTakeBusButton(StopHeadsign stopHeadsign) {
        getSupportFragmentManager().beginTransaction().replace(R.id.container_layout,
                stopScheduleFragment, stopScheduleFragment.TAG).addToBackStack(StopScheduleFragment.TAG).commit();
        changeBottomBarItem(StopScheduleFragment.TAG);
        stopScheduleFragment.setRouteToDisplay(stopHeadsign);

    }


    protected void startLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create()
                .setInterval(Constants.LOCATION_REQUEST_INTERVAL)
                .setFastestInterval(Constants.DETECTION_INTERVAL_IN_MILLISECONDS);

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, locationRequest, getLocationCallback(), null);

    }

    private void stopRequestLocationUpdates() {
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, getLocationCallback());
        }
    }

    private LocationCallback getLocationCallback() {
        if (locationCalback == null) {
            locationCalback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult result) {
                    super.onLocationResult(result);
                    onMeliorLocationAvaliable(result.getLastLocation());
                    identifyUserCity(result.getLastLocation());
                    stopRequestLocationUpdates();
                }

                @Override
                public void onLocationAvailability(LocationAvailability locationAvailability) {
                    super.onLocationAvailability(locationAvailability);
                    if (!locationAvailability.isLocationAvailable()) {
//                        requestLocationUpdates();
                        Toast.makeText(getApplicationContext(), getString(R.string.fail_retrieving_location), Toast.LENGTH_SHORT).show();
                        Log.d("OnLocationAvailability", "Não foi possível localizar");
                    }
                }
            };
        }
        return locationCalback;
    }

    public void onMeliorLocationAvaliable(Location result) {
        mLastLocation = result;
    }

    public void identifyUserCity(Location lastLocation) {
        AssetManager assetManager = getAssets();
        String savedCity = SharedPreferencesUtils.getCityNameOnDatabase(this);

        try {
            cityName = CityDataManager.checkCity(assetManager, lastLocation);
            if (!savedCity.equals(cityName)) {
                askUserCity();
            }
        } catch (NoDataForCityException e) {
            Toast.makeText(this, getString(R.string.msg_no_data_for_city), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private boolean askUserCity() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage(getString(R.string.msg_ask_user_city) + cityName + "?");

        final boolean[] positiveButtomClicked = new boolean[1];
        builder.setPositiveButton(getString(R.string.msg_yes), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
                deleteDatabase(MeliorDBOpenHelper.DB_NAME);
                SharedPreferencesUtils.setCityNameOnDatabase(getApplicationContext(), cityName);

                loadCityData();
            }
        });

        builder.setNegativeButton(getString(R.string.msg_no), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
                Toast.makeText(getApplicationContext(), getString(R.string.msg_failed_detect_city), Toast.LENGTH_SHORT).show();
            }
        });

        AlertDialog alert = builder.create();
        alert.show();
        return positiveButtomClicked[0];
    }

    public void buildAlertMessageNoGps() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(MelhorBusaoActivity.this);
        builder.setMessage(getString(R.string.msg_gps_disabled))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.msg_yes), new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton(getString(R.string.msg_no), new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        dialog.cancel();
                    }
                });
        android.app.AlertDialog alert = builder.create();
        alert.show();
    }

    public String getCityName() {
        return cityName;
    }



    // TO check baterry states
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            /*
                BatteryManager
                    The BatteryManager class contains strings and constants used for values in the
                    ACTION_BATTERY_CHANGED Intent, and provides a method for querying battery
                    and charging properties.
            */
            /*
                public static final String EXTRA_SCALE
                    Extra for ACTION_BATTERY_CHANGED: integer containing the maximum battery level.
                    Constant Value: "scale"
            */
            // Get the battery scale
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE,-1);
            String battery_scale = String.valueOf(scale);

            /*
                public static final String EXTRA_LEVEL
                    Extra for ACTION_BATTERY_CHANGED: integer field containing the current battery
                    level, from 0 to EXTRA_SCALE.

                    Constant Value: "level"
            */
            // get the battery level
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL,-1);
            String battery_lvl = String.valueOf(level);

            // Calculate the battery charged percentage
            float percentage = level/ (float) scale;
            String current_battery_percentage = String.valueOf((int)((percentage)*100));

            gravarNoArquivo(battery_scale, battery_lvl , current_battery_percentage);
        }
    };

    // TO check baterry states
    private void gravarNoArquivo(String battery_scale, String battery_lvl, String current_battery_percentage) {
        File externalStorage = Environment.getExternalStorageDirectory();
        File busMonitorPath = new File(externalStorage, Constants.LOG_PATH);

        if (!busMonitorPath.exists()){
            busMonitorPath.mkdirs();
        }

        String time = String.valueOf(Calendar.getInstance().getTimeInMillis());
        File file = new File(busMonitorPath, "battery_status.txt");

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));
            if (!file.exists()){
                String header = "time,battery_scale, battery_lvl,current_battery_percentage ";
                writer.write(header);
            }

            writer.append(time+ "," + battery_scale + "," + battery_lvl + "," + current_battery_percentage);


            writer.flush();
            writer.close();
        } catch (IOException e) {
            Toast.makeText(this, "Não foi possível gravar o arquivo '" + file.getName() + "'", Toast.LENGTH_LONG).show();
        }

    }

    @Override
    public void onGetDirectionsButtonClick(List<Itinerary> itineraries) {
        getSupportFragmentManager().beginTransaction().replace(R.id.container_layout,
                itinerariesListFragment, itinerariesListFragment.TAG).addToBackStack(itinerariesListFragment.TAG).commit();
        changeBottomBarItem(itinerariesListFragment.TAG);
        itinerariesListFragment.setItinerariesList(itineraries);
    }

    @Override
    public void onSelected(Itinerary itinerary) {
        itineraryMapFragment.getArguments().putParcelable(itineraryMapFragment.ITINERARY, itinerary);
        getSupportFragmentManager().beginTransaction().replace(R.id.container_layout,
                itineraryMapFragment, itineraryMapFragment.TAG).addToBackStack(itineraryMapFragment.TAG).commit();
    }

    public void OnCheckoutData(boolean finished) {
        if(SharedPreferencesUtils.getUserToken(MelhorBusaoActivity.this.getBaseContext())!=""){
            SharedPreferencesUtils.setUserToken(getApplicationContext(),"" ,Constants.BIG_SEA_SERVICE,"");
            Intent intent = new Intent(getApplicationContext(), MelhorLoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
        Auth.GoogleSignInApi.signOut(mGoogleApiClient);
    }

    @Override
    public void OnLogged(boolean logged) {}

    public void onPasswordChanged() {
        onBackPressed();
    }
}