package edu.berkeley.cellscope3.device;

import java.util.Arrays;

/** Holds request/response data for a {@link DeviceRequestQueue} */
public final class DeviceData {

	public final byte[] data;

	public static DeviceData of(byte[] data) {
		return new DeviceData(data);
	}

	private DeviceData(byte[] data) {
		this.data = data;
	}

	@Override
	public boolean equals(Object object) {
		if (object == null || !(object instanceof DeviceData)) {
			return false;
		}
		return Arrays.equals(data, ((DeviceData) object).data);
	}
}
