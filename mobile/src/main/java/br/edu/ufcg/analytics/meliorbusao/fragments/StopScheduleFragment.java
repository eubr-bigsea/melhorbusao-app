package br.edu.ufcg.analytics.meliorbusao.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import com.parse.ParseException;

import java.util.ArrayList;
import java.util.List;

import br.edu.ufcg.analytics.meliorbusao.R;
import br.edu.ufcg.analytics.meliorbusao.SchedulesProbableParser;
import br.edu.ufcg.analytics.meliorbusao.adapters.ScheduleAdapter;
import br.edu.ufcg.analytics.meliorbusao.adapters.RoutesAdapter;
import br.edu.ufcg.analytics.meliorbusao.adapters.StopsAdapter;
import br.edu.ufcg.analytics.meliorbusao.listeners.FragmentTitleChangeListener;
import br.edu.ufcg.analytics.meliorbusao.listeners.OnStopTimesReadyListener;
import br.edu.ufcg.analytics.meliorbusao.models.NearStop;
import br.edu.ufcg.analytics.meliorbusao.models.Route;
import br.edu.ufcg.analytics.meliorbusao.models.StopHeadsign;
import br.edu.ufcg.analytics.meliorbusao.models.StopTime;


public class StopScheduleFragment extends Fragment implements OnStopTimesReadyListener {

    private static StopScheduleFragment instance;

    private View mView;

    private NearStop stop;
    private Route route;
    private StopHeadsign stopHeadsign;

    public static final String TAG = "STOP_SCHEDULE_FRAGMENT";
    private FragmentTitleChangeListener mCallback;
    private ListView mScheduleListView;
    private Spinner stopsSpinner;
    private StopsAdapter mAdapterStop;

    public static StopScheduleFragment getInstance() {
        if (instance == null) {
            StopScheduleFragment fragment = new StopScheduleFragment();
            Bundle args = new Bundle();
            fragment.setArguments(args);
            instance = fragment;
        }
        return instance;
    }

    public StopScheduleFragment() {
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.stop_schedule_fragment, container, false);

        Spinner routeSpinner = (Spinner) mView.findViewById(R.id.route_stop_time_frag);
        routeSpinner.setEnabled(false);

        ArrayList routeList= new ArrayList<Route>();
        routeList.add(route);

        RoutesAdapter adapter = new RoutesAdapter(getActivity(), routeList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        routeSpinner.setAdapter(adapter);

        ////////PARADA/////////
        ArrayList<StopHeadsign> listaStop = new ArrayList<StopHeadsign>();
        listaStop.add(stopHeadsign);
        mAdapterStop = new StopsAdapter(getActivity(),listaStop,route);

        //------StopHeadsign----//
        mScheduleListView = (ListView) mView.findViewById(R.id.schedule_list);

        Spinner stopSpinner = (Spinner) mView.findViewById(R.id.stop_time_stop_spinner);
        stopSpinner.setAdapter(mAdapterStop);
        stopSpinner.setEnabled(false);

        return mView;
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

        if (stop != null && route != null){
            //ParseUtils.getRouteSchedules(stop.getId(), route.getId(), this);
            SchedulesProbableParser.getRouteSchedules(getContext(),stop.getId(), route.getId(), this);
        }
        mCallback.onTitleChange(getResources().getString(R.string.stop_time_title));
        //updateListView();
    }

    public void setRouteToDisplay(StopHeadsign stopHeadsign){
        this.stopHeadsign = stopHeadsign;
        this.route = stopHeadsign.getRoute();
        this.stop = stopHeadsign.getNearStops();
    }


    public NearStop getStop() {
        return stop;
    }

    public Route getRoute() {
        return route;
    }

    @Override
    public void onStopTimesReady(List<StopTime> stopTimes) {
        if (stopTimes == null) {
            Toast.makeText(getContext(), getString(R.string.msg_no_bus_next_hour), Toast.LENGTH_LONG).show();
        } else {
            ScheduleAdapter scheduleAdapter = new ScheduleAdapter(getActivity(),
                    R.layout.schedule_item, stopTimes);
            mScheduleListView.setAdapter(scheduleAdapter);
        }
    }

    @Override
    public void onStopHeadsignReady(StopHeadsign stopHeadsignObj, ParseException e) {
    }
}
