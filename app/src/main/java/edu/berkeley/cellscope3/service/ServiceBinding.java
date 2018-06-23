package edu.berkeley.cellscope3.service;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

import edu.berkeley.cellscope3.device.ble.BleService;

/** Handles binding/unbinding a service. */
public abstract class ServiceBinding<S extends Service> {

	private final Context context;

	private S service;
	private SettableFuture<Boolean> bindFuture;

	public ServiceBinding(Context context) {
		this.context = context;
	}

	public ListenableFuture<Boolean> bindService() {
		if (service != null) {
			return Futures.immediateFuture(true);
		}
		Intent intent = new Intent(context, BleService.class);
		if (!context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)) {
			return Futures.immediateFuture(false);
		}
		onBindService();
		bindFuture = SettableFuture.create();
		return bindFuture;
	}

	public void unbindService() {
		if (service != null) {
			context.unbindService(serviceConnection);
			service = null;
			onUnbindService();
		}
	}

	public S getService() {
		return service;
	}

	public boolean isServiceBound() {
		return service != null;
	}

	protected abstract S getConnectedService(IBinder iBinder);

	protected abstract void onBindService();

	protected abstract void onUnbindService();

	private final ServiceConnection serviceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
			service = getConnectedService(iBinder);
			bindFuture.set(true);
		}

		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			service = null;
		}
	};
}
