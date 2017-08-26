package edu.berkeley.cellscope3.camera;


import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.util.Pair;

import java.util.NoSuchElementException;

public final class Camera2Ids {

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
			if (map == null) {
				continue;
			}
		}
		throw new NoSuchElementException("No back-facing camera was found");
	}

	private Camera2Ids() {}
}
