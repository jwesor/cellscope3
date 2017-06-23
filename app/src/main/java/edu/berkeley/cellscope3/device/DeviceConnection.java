package edu.berkeley.cellscope3.device;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * Interface for interacting with an external CellScope device.
 */
public interface DeviceConnection {

	ListenableFuture<Boolean> connect();

	boolean disconnect();

	boolean sendRequest(byte[] data);

	ConnectionStatus getStatus();

	void addListener(DeviceListener listener);

	enum ConnectionStatus {
		DISCONNECTED, CONNECTING, CONNECTED
	}

	interface DeviceListener {

		void onDeviceConnect();

		void onDeviceDisconnect();

		void onDeviceResponse(byte[] data);
	}

	abstract class AbstractDeviceListener implements DeviceListener {
		@Override
		public void onDeviceConnect() {
		}

		@Override
		public void onDeviceDisconnect() {
		}

		@Override
		public void onDeviceResponse(byte[] data) {
		}
	}
}
