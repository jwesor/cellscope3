package edu.berkeley.cellscope3.feed.camera2;


import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.util.Pair;

import java.util.NoSuchElementException;

public final class CameraIds {

	public static Pair<String, CameraCharacteristics> getBackFacingCamera(
			CameraManager cameraManager) throws CameraAccessException {
		for (String cameraId : cameraManager.getCameraIdList()) {
			CameraCharacteristics characteristics =
					cameraManager.getCameraCharacteristics(cameraId);
			Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
			if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
				continue;
			}
			StreamConfigurationMap map = characteristics.get(
					CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
			if (map != null) {
				return Pair.create(cameraId, characteristics);
			}
		}
		throw new NoSuchElementException("No back-facing camera was found");
	}

	private CameraIds() {}
}
