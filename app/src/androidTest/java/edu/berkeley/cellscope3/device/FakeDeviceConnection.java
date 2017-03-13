package edu.berkeley.cellscope3.device;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

final class FakeDeviceConnection implements DeviceConnection {

	private DeviceListener listener;

	@Override
	public ListenableFuture<Boolean> connect() {
		return Futures.immediateFuture(true);
	}

	@Override
	public boolean disconnect() {
		return true;
	}

	@Override
	public boolean sendRequest(byte[] data) {
		return true;
	}

	@Override
	public ConnectionStatus getStatus() {
		return ConnectionStatus.CONNECTED;
	}

	@Override
	public void addListener(DeviceListener listener) {
		this.listener = listener;
	}

	public void respond(byte[] response) {
		listener.onDeviceResponse(response);
	}
}
