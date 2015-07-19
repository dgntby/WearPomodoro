package com.tasomaniac.android.pomodoro.shared.service;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;
import com.tasomaniac.android.pomodoro.shared.Constants;
import com.tasomaniac.android.pomodoro.shared.PomodoroMaster;
import com.tasomaniac.android.pomodoro.shared.model.ActivityType;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import hugo.weaving.DebugLog;
import timber.log.Timber;

public class BaseWearableListenerService extends WearableListenerService implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private boolean mConnected = false;

    @Inject GoogleApiClient mGoogleApiClient;
    @Inject PomodoroMaster pomodoroMaster;

    @Override
    public void onCreate() {
        super.onCreate();
        mGoogleApiClient.registerConnectionCallbacks(this);
        mGoogleApiClient.registerConnectionFailedListener(this);
        mGoogleApiClient.connect();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mGoogleApiClient.unregisterConnectionCallbacks(this);
        mGoogleApiClient.unregisterConnectionFailedListener(this);
    }

    @DebugLog
    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {

        final List<DataEvent> events = FreezableUtils
                .freezeIterable(dataEvents);

        if (!mConnected) {
            ConnectionResult connectionResult =
                    mGoogleApiClient.blockingConnect(30, TimeUnit.SECONDS);

            if (!connectionResult.isSuccess()) {
                Timber.e("Failed to connect to GoogleApiClient.");
                return;
            }
        }
        NodeApi.GetLocalNodeResult nodes = Wearable.NodeApi.getLocalNode(mGoogleApiClient).await();
        final String localNodeId = nodes.getNode().getId();

        // Loop through the events.
        for (DataEvent event : events) {
            final Uri uri = event.getDataItem().getUri();
            String path = uri.getPath();
            String nodeId = uri.getHost();

            if (nodeId != null && nodeId.equals(localNodeId)) {
                continue;
            }

            if (event.getType() == DataEvent.TYPE_CHANGED) {
                if (Constants.PATH_POMODORO.equals(path)) {
                    DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                    DataMap dataMap = dataMapItem.getDataMap();

                    final ActivityType activityType = ActivityType.fromValue(dataMap.getInt(Constants.KEY_ACTIVITY_TYPE));
                    pomodoroMaster.setActivityType(activityType);
                    pomodoroMaster.setPomodorosDone(dataMap.getInt(Constants.KEY_POMODOROS_DONE));
                    pomodoroMaster.setOngoing(dataMap.getBoolean(Constants.KEY_POMODORO_ONGOING));

                    final Intent intent = new Intent(dataMap.getString(Constants.SYNC_ACTION));
                    intent.putExtra(BaseNotificationService.EXTRA_ACTIVITY_TYPE, activityType.value());
                    intent.putExtra(Constants.EXTRA_SYNC_NOTIFICATION, true);
                    intent.putExtra(Constants.KEY_NEXT_POMODORO,
                            dataMap.getLong(Constants.KEY_NEXT_POMODORO, -1));

                    sendBroadcast(intent);
                }
            } else if (event.getType() == DataEvent.TYPE_DELETED) {
                Timber.d(event.getDataItem().toString());
                final Intent intent = new Intent(BaseNotificationService.ACTION_DISMISS);
                intent.putExtra(Constants.EXTRA_SYNC_NOTIFICATION, true);
                sendBroadcast(intent);
            }
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Timber.d("Connected to Google API Client");
        mConnected = true;
    }

    @Override
    public void onConnectionSuspended(int i) {
        mConnected = false;
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Timber.e("Failed to connect to the Google API client");
        mConnected = false;
    }
}