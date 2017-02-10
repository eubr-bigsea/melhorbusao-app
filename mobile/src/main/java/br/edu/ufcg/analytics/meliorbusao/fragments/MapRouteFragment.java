package br.edu.ufcg.analytics.meliorbusao.fragments;


import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.FilterQueryProvider;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ZoomButtonsController;

import com.cocosw.bottomsheet.BottomSheet;
import com.google.android.gms.maps.model.BitmapDescriptor;

import org.osmdroid.bonuspack.overlays.Marker;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.PathOverlay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import br.edu.ufcg.analytics.meliorbusao.R;
import br.edu.ufcg.analytics.meliorbusao.adapters.RouteArrayAdapter;
import br.edu.ufcg.analytics.meliorbusao.adapters.SearchRouteResultsAdapter;
import br.edu.ufcg.analytics.meliorbusao.adapters.StopInfoAdapter;
import br.edu.ufcg.analytics.meliorbusao.db.DBUtils;
import br.edu.ufcg.analytics.meliorbusao.listeners.FragmentTitleChangeListener;
import br.edu.ufcg.analytics.meliorbusao.listeners.OnMapInformationReadyListener;
import br.edu.ufcg.analytics.meliorbusao.listeners.OnMeliorBusaoQueryListener;
import br.edu.ufcg.analytics.meliorbusao.listeners.OnRouteSuggestionListener;
import br.edu.ufcg.analytics.meliorbusao.models.Route;
import br.edu.ufcg.analytics.meliorbusao.models.RouteShape;
import br.edu.ufcg.analytics.meliorbusao.models.Stop;

public class MapRouteFragment extends Fragment implements OnMeliorBusaoQueryListener, OnRouteSuggestionListener,
        SearchView.OnQueryTextListener, FilterQueryProvider, SearchView.OnSuggestionListener,
        AdapterView.OnItemSelectedListener, OnMapInformationReadyListener, ZoomButtonsController.OnZoomListener {

    public static final String TAG = "MAP_ROUTE_FRAGMENT";
    private static final double DEFAULT_ZOOM_THRESHOLD = 15.0;
    private static MapRouteFragment instance;
    private FragmentTitleChangeListener mCallback;
    private String routeShortName;
    private Menu mMenu;
    private SearchView mSearchView;
    private List<String> routeSuggestionList = new ArrayList<>();
    private ArrayAdapter<String> itemsAdapter;
    private float previousZoomLevel;
    private boolean isZoomingIn;
    private ArrayList<Marker> stopsMarkers;
    private BitmapDescriptor mParadaBitmap;
    private ProgressBar progressSpinner;
    private MapFragment osmFragment;

    public MapRouteFragment() {

    }

    public static MapRouteFragment getInstance() {
        if (instance == null) {
            MapRouteFragment fragment = new MapRouteFragment();
            Bundle args = new Bundle();
            fragment.setArguments(args);
            instance = fragment;
        }
        return instance;
    }

    public String getRouteShortName() {
        return routeShortName;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //FrameLayout viewMain = (FrameLayout) super.onCreateView(inflater, container, savedInstanceState);
        View viewMain = inflater.inflate(R.layout.fragment_map_route, container, false);

        osmFragment = new MapFragment();
        osmFragment.setOnMapInformationReadyListener(this);

        getChildFragmentManager().beginTransaction().replace(R.id.melior_map_fragment, osmFragment).commit();

        itemsAdapter =
                new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, routeSuggestionList);
        //Enable Options Menu handling
        setHasOptionsMenu(true);
        if (stopsMarkers == null) {
            stopsMarkers = new ArrayList<>();
        }

        return viewMain;
    }


    private void showLinesMenu() {
        BottomSheet.Builder builder = new BottomSheet.Builder(getActivity());

        builder.title(R.string.lines);

        BottomSheet bottomSheet = builder.build();
        Menu m = bottomSheet.getMenu();

        for (String linha : DBUtils.getLinhas(getContext())) {
            m.add(linha).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    showRoutesLineMenu(item.getTitle());
                    return false;
                }
            });
        }

        bottomSheet.show();
    }

    private void showRoutesLineMenu(CharSequence linhaName) {
        BottomSheet.Builder builder = new BottomSheet.Builder(getActivity());
        builder.title("Linha " + linhaName);
        BottomSheet bottomSheet = builder.build();
        Menu m = bottomSheet.getMenu();

        for (final Route route : DBUtils.getRotasPorLinha(getContext(), (String) linhaName)) {
            m.add(route.getShortName()).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    mCallback.onTitleChange(buildScreenTitle(route.getShortName()));
                    setUpMap(route);
                    return false;
                }
            });
        }

        bottomSheet.show();
    }


    private void setUpMap(Route r) {
        osmFragment.clearMap();
        drawRoute(r);
        StopInfoAdapter stopInfo = new StopInfoAdapter();
        stopInfo.setActivity(getActivity());

        //getMap().setInfoWindowAdapter(stopInfo);
    }

    private void drawRoute(Route route) {
        List<RouteShape> shapes = DBUtils.getRouteShape(getContext(), route.getId());


        for (RouteShape shape : shapes) {
            PathOverlay pathOverlay = new PathOverlay(Color.parseColor("#"+ shape.getColor()), getContext());
            pathOverlay.addPoints((ArrayList) shape);
            osmFragment.drawRoute(pathOverlay);
            Paint pPaint = pathOverlay.getPaint();
            pPaint.setStrokeWidth(5);
            pathOverlay.setPaint(pPaint);
            osmFragment.animateTo(shape);

        }
        inicializarParadas(route);

    }


    private void inicializarParadas(Route rota) {
        HashSet<Stop> paradas = DBUtils.getParadasRota(getContext(), rota);
        if (stopsMarkers == null) {
            stopsMarkers = new ArrayList<>();
        } else {
            stopsMarkers.clear();
        }
        for (Stop parada : paradas) {

            stopsMarkers.add(osmFragment.addMarker(new GeoPoint(parada.getLatitude(), parada.getLongitude()), R.drawable.ic_bus_stop_sign));

        }
    }

    public void setRoute(String routeShortName) {
        this.routeShortName = routeShortName;
        if (routeShortName == null){
            stopsMarkers = null;
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mCallback = (FragmentTitleChangeListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnHeadlineSelectedListener");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (routeShortName != null) {
            mCallback.onTitleChange(buildScreenTitle(routeShortName));
            Route route = DBUtils.getRoute(getContext(), routeShortName);
            setUpMap(route);
            setRoute(null);
        } else {
            //progressSpinner.setVisibility(View.GONE);
            mCallback.onTitleChange(getResources().getString(R.string.map_routes_title));
            osmFragment.clearMap();
        }
    }

    private String buildScreenTitle(String routeName) {
        return getResources().getString(R.string.map_route_screen_base_title) + routeName;
    }

    @Override
    public void onMeliorBusaoQueryChange(String query) {

    }

    @Override
    public boolean onMeliorBusaoQuerySubmit(String query) {
        try {
            Route searchRoute = DBUtils.getRoute(getContext(), query);
            mCallback.onTitleChange(buildScreenTitle(searchRoute.getShortName()));
            setUpMap(searchRoute);
            return true;
        } catch (Exception e) {
            Toast.makeText(getActivity(), getString(R.string.msg_unable_to_find_route), Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    @Override
    public boolean onRouteSuggestionClick(Route selectedRoute) {
        try {
            mCallback.onTitleChange(buildScreenTitle(selectedRoute.getShortName()));
            setUpMap(selectedRoute);
            return true;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            return false;
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        mMenu = menu;

        menu.findItem(R.id.action_search).setVisible(true);
        mSearchView = (SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.action_search));
        mSearchView.setOnQueryTextListener(this);
        mSearchView.setQueryHint(getResources().getString(R.string.search_hint_map_route));
        mSearchView.setInputType(0x00000002);

        int autoCompleteTextViewID = getResources().getIdentifier("search_src_text", "id",
                getActivity().getPackageName());
        AutoCompleteTextView searchAutoCompleteTextView =
                (AutoCompleteTextView) mSearchView.findViewById(autoCompleteTextViewID);
        searchAutoCompleteTextView.setThreshold(0);

        SearchRouteResultsAdapter mSearchSuggestionAdapter = new SearchRouteResultsAdapter(getContext(),
                R.layout.search_suggestion_list_item, null, null, -1000);
        mSearchSuggestionAdapter.setFilterQueryProvider(this);
        mSearchView.setSuggestionsAdapter(mSearchSuggestionAdapter);
        mSearchView.setOnSuggestionListener(this);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        try {
            Route searchRoute = DBUtils.getRoute(getContext(), query);
            mCallback.onTitleChange(buildScreenTitle(searchRoute.getShortName()));
            setUpMap(searchRoute);
            mMenu.findItem(R.id.action_search).collapseActionView();
            return true;
        } catch (Exception e) {
            Toast.makeText(getActivity(), getString(R.string.msg_route_not_found), Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        return true;
    }

    @Override
    public Cursor runQuery(CharSequence constraint) {
        return DBUtils.getRouteCursor(getContext(), (String) constraint);
    }

    @Override
    public boolean onSuggestionSelect(int position) {
        return false;
    }

    @Override
    public boolean onSuggestionClick(int position) {
        Cursor c = mSearchView.getSuggestionsAdapter().getCursor();
        Route selectedRoute = new Route(c.getString(0), c.getString(1),
                c.getString(2), c.getString(3));
        onRouteSuggestionClick(selectedRoute);

        try {
            mMenu.findItem(R.id.action_search).collapseActionView();
            c.close();
            return true;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            return false;
        }
    }

    private void populateRouteSpinner(Spinner spinner) {
        List<Route> routes = new ArrayList<Route>(DBUtils.getTodasAsRotas(getActivity()));
        Collections.sort(routes);
        RouteArrayAdapter adapter = new RouteArrayAdapter(getActivity(), routes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(this);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (routeShortName == null) {
            Route selectedRoute = (Route) parent.getItemAtPosition(position);
            onMeliorBusaoQuerySubmit(selectedRoute.getShortName());
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }


/*
    public void onCameraChange(CameraPosition cameraPosition) {
        if (previousZoomLevel != cameraPosition.zoom && stopsMarkers!=null) {
            if (cameraPosition.zoom >= DEFAULT_ZOOM_THRESHOLD) {
                if (!isZoomingIn) {
                    for (Marker stopMarker : stopsMarkers) {
                        stopMarker.setIcon(getBitmapDescriptor(R.drawable.ic_bus_stop_sign, 52, 40));
                    }
                }
                isZoomingIn = true;
            } else {
                if (isZoomingIn) {
                    for (Marker stopMarker : stopsMarkers) {
                        stopMarker.setIcon(getBitmapDescriptor(R.drawable.ic_bus_stop, 5, 5));
                    }
                }
                isZoomingIn = false;
            }
        }
        previousZoomLevel = cameraPosition.zoom;
    } */



    @Override
    public void onMapAddressFetched(String mapAddres) {

    }

    @Override
    public void onMapLocationAvailable(Location mapLocation) {

    }

    @Override
    public void onMapClick(GeoPoint geoPoint) {

    }


    //ZoomButtonsController.OnZoomListener
    @Override
    public void onVisibilityChanged(boolean b) {
        /*if (b){
            if (MapFragment.getMapZoomLevel() >= DEFAULT_ZOOM_THRESHOLD){
                for (Marker stopMarker : stopsMarkers) {

                    osmFragment.addMarker(stopMarker,R.drawable.ic_bus_stop_sign);
                    stopMarker.setIcon(getBitmapDescriptor(R.drawable.ic_bus_stop_sign, 52, 40));
                }
            }
        }
        if (previousZoomLevel != cameraPosition.zoom && stopsMarkers!=null) {
            if (cameraPosition.zoom >= DEFAULT_ZOOM_THRESHOLD) {
                if (!isZoomingIn) {
                    for (Marker stopMarker : stopsMarkers) {
                        stopMarker.setIcon(getBitmapDescriptor(R.drawable.ic_bus_stop_sign, 52, 40));
                    }
                }
                isZoomingIn = true;
            } else {
                if (isZoomingIn) {
                    for (Marker stopMarker : stopsMarkers) {
                        stopMarker.setIcon(getBitmapDescriptor(R.drawable.ic_bus_stop, 5, 5));
                    }
                }
                isZoomingIn = false;
            }
        }
        previousZoomLevel = cameraPosition.zoom; */
    }

    //ZoomButtonsController.OnZoomListener
    @Override
    public void onZoom(boolean b) {

    }
}
