package edu.berkeley.cellscope3.device.ble;

import android.os.Bundle;

import java.util.UUID;

/**
 * Information about a CellScope's Bluetooth LE gatt server. The device should have a service
 * that contains a characteristic for transmission and reception.
 */
public final class BleProfile {

	private static final String KEY_SERVICE_UUID = "BLE_PROFILE_SERVICE_UUID";
	private static final String KEY_CLIENT_CONFIG = "BLE_PROFILE_CLIENT_CONFIG";
	private static final String KEY_TX_UUID = "BLE_PROFILE_TX_UUID";
	private static final String KEY_RX_UUID = "BLE_PROFILE_RX_UUID";

	public final UUID clientConfig;

	public final UUID serviceUuid;

	public final UUID txUuid;

	public final UUID rxUuid;

	public BleProfile(String serviceUuid, String clientConfig, String txUuid, String rxUuid) {
		this(UUID.fromString(serviceUuid), UUID.fromString(clientConfig), UUID.fromString(txUuid), UUID.fromString(rxUuid));
	}

	public BleProfile(UUID serviceUuid, UUID clientConfig, UUID txUuid, UUID rxUuid) {
		this.serviceUuid = serviceUuid;
		this.clientConfig = clientConfig;
		this.txUuid = txUuid;
		this.rxUuid = rxUuid;
	}

	public static BleProfile fromBundle(Bundle bundle) {
		return new BleProfile(
				bundle.getString(KEY_SERVICE_UUID),
				bundle.getString(KEY_CLIENT_CONFIG),
				bundle.getString(KEY_TX_UUID),
				bundle.getString(KEY_RX_UUID));
	}

	public static Bundle toBundle(BleProfile profile) {
		Bundle bundle = new Bundle();
		bundle.putString(KEY_SERVICE_UUID, profile.serviceUuid.toString());
		bundle.putString(KEY_CLIENT_CONFIG, profile.clientConfig.toString());
		bundle.putString(KEY_TX_UUID, profile.txUuid.toString());
		bundle.putString(KEY_RX_UUID, profile.rxUuid.toString());
		return bundle;
	}
}
