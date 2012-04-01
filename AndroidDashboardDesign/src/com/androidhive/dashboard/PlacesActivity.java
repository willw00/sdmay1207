package com.androidhive.dashboard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import org.osmdroid.ResourceProxy;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay.OnItemGestureListener;
import org.osmdroid.views.overlay.ItemizedOverlay;
import org.osmdroid.views.overlay.ItemizedOverlayWithFocus;
import org.osmdroid.views.overlay.MyLocationOverlay;
import org.osmdroid.views.overlay.OverlayItem;

import sdmay1207.ais.Device;
import sdmay1207.ais.NodeController;
import sdmay1207.ais.network.NetworkController.NetworkEvent;
import sdmay1207.ais.network.NetworkInterface.RoutingAlg;
import sdmay1207.ais.network.model.Node;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.TextView;
import androidhive.dashboard.R;

import com.androidhive.dashboard.DashboardApplication.Notification;

public class PlacesActivity extends Activity implements Observer
{
    /** Called when the activity is first created. */
    private MapView mapView;
    private TextView notificationView;

    private NodeController nc;
    boolean networkClicks=false;
    MyLocationOverlay my;
    private DashboardApplication da;


    private final int MAX_LINES_OF_NOTIFICATION_TEXT = 4;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.places_layout);

        mapView = (MapView) findViewById(R.id.map);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setClickable(true);
        mapView.setBuiltInZoomControls(true);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(15);
        mapView.getController().setCenter(new GeoPoint(42.024443, -93.656141));

        da = ((DashboardApplication) getApplication());
        nc = da.nc;
        notificationView = ((TextView) findViewById(R.id.notifications));
        notificationView.setLines(MAX_LINES_OF_NOTIFICATION_TEXT);

        // Set button listeners
        ((Button) findViewById(R.id.showNodeListButton))
                .setOnClickListener(new OnClickListener()
                {
                    public void onClick(View v)
                    {
                        startActivity(new Intent(PlacesActivity.this,
                                NodeListActivity.class));
                    }
                });

        ((Button) findViewById(R.id.initP2PButton))
                .setOnClickListener(new OnClickListener()
                {
                    public void onClick(View v)
                    {
                        startActivity(new Intent(PlacesActivity.this,
                                P2PSetupActivity.class));


                    }
                });

        ((Button) findViewById(R.id.stopButton))
                .setOnClickListener(new OnClickListener()
                {
                    public void onClick(View v)
                    {
                    	if(!networkClicks)
                    	{
                    		Device.doAndroidHardStop();
                            nc.start(RoutingAlg.AODV);
                            ((Button) findViewById(R.id.stopButton)).setText("Stop");
                            networkClicks=true;
                    	}
                    	else
                    	{
                    		nc.stop();
                    		((Button) findViewById(R.id.stopButton)).setText("Start");
                        	Device.doAndroidHardStop();
                        	
                        }
                        //finish();
                    }
                });

        notificationView.setOnClickListener(new OnClickListener()
        {
            public void onClick(View v)
            {
                startActivity(new Intent(PlacesActivity.this,
                        NotificationActivity.class));
            }
        });
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        updateMapObjects();
        updateNotificationView();

        // make sure we are observing when added
        da.nm.addObserver(this);
        nc.addNetworkObserver(this);
    }

    // http://code.google.com/p/osmdroid/issues/detail?id=267
    @Override
    protected void onPause()
    {
        super.onPause();
        System.out.println("onPause");
        mapView.getTileProvider().clearTileCache();

        // make sure refs are removed when we might die
        da.nm.deleteObserver(this);
        nc.removeNetworkObserver(this);
    }

    private void updateMapObjects()
    {
        mapView.getOverlays().clear();
        Collection<Node> nodes = nc.getNodesInNetwork().values();
        List<OverlayItem> items = new ArrayList<OverlayItem>();
        for (Node n : nodes)
        {
            if (n.lastLocation != null)
            {
                items.add(new OverlayItem("" + n.nodeNum, "title", "desc",
                        new GeoPoint(n.lastLocation.latitude,
                                n.lastLocation.longitude)));
            }
        }

        ItemizedOverlay<OverlayItem> overlay = new ItemizedOverlayWithFocus<OverlayItem>(
                this, items, new OnItemGestureListener<OverlayItem>()
                {
                    public boolean onItemLongPress(int arg0, OverlayItem arg1)
                    {
                        return false;
                    }

                    public boolean onItemSingleTapUp(int i, OverlayItem item)
                    {
                        int nodeNum = Integer.parseInt(item.getUid());
                        Intent intent = new Intent(PlacesActivity.this, NodeDetailsActivity.class);
                        intent.putExtra(NodeDetailsActivity.NODE_NUM_KEY,
                                nodeNum);
                        startActivity(intent);

                        System.out.println("tapped node # " + item.getUid());
                        return true;
                    }
                });
        mapView.setKeepScreenOn(true);
       my= new MyLocationOverlay(this, mapView);
        my.enableCompass();
        my.enableMyLocation();
        
        mapView.getOverlays().add(my);
    
        mapView.getOverlays().add(overlay);
        mapView.postInvalidate();
    }

    // don't need to know what the new one is, just that there is a new
    // Notification.
    // display as many as we can, pulling from the NotificationManager
    private void updateNotificationView()
    {
        runOnUiThread(new Runnable()
        {
            public void run()
            {
                notificationView.setText("");
                for (int i = 0; i < da.nm.notifications.size()
                        && i < MAX_LINES_OF_NOTIFICATION_TEXT; i++)
                {
                    String newText = "";
                    if (i != 0)
                        newText = "\n";

                    Notification n = da.nm.notifications.get(i);
                    newText += "+ " + n.shortDisplayString();
                    notificationView.setText(notificationView.getText()
                            + newText);
                }
            };
        });
    }

    public void update(Observable observable, Object obj)
    {
        if (observable == da.nm)
        {
            updateNotificationView();
            return;
        }

        NetworkEvent netEvent = (NetworkEvent) obj;
        switch (netEvent.event)
        {
        case RecvdHeartbeat:
            updateMapObjects();
            break;
        case NodeJoined:
            updateMapObjects();
            break;
        case NodeLeft:
            updateMapObjects();
            break;
        }
    }
}
