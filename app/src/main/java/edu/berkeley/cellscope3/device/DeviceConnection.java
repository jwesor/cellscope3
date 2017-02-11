package edu.berkeley.cellscope3.device;

/** Interface for interacting with an external CellScope device. */
public interface DeviceConnection {

	boolean connect();

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

		void onDeviceConnectFail();

		void onDeviceResponse(byte[] data);
	}

	abstract class DefaultDeviceListener implements DeviceListener {
		@Override
		public void onDeviceConnect() {}

		@Override
		public void onDeviceDisconnect() {}

		@Override
		public void onDeviceConnectFail() {}

		@Override
		public void onDeviceResponse(byte[] data) {}
	}
}
