package edu.berkeley.cellscope3.feed.camera2;


import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.graphics.Point;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.util.Size;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.List;

public final class CameraSizes {

	public static Size[] withAspectRatio(Size[] sizes, int width, int height) {
		List<Size> outputSizes = new ArrayList<>();
		for (Size size: sizes) {
			if (size.getHeight() == size.getWidth() * height / width) {
				outputSizes.add(size);
			}
		}
		return outputSizes.toArray(sizes);
	}

	public static Size[] largerThan(Size[] sizes, int width, int height) {
		List<Size> outputSizes = new ArrayList<>();
		for (Size size: sizes) {
			if (size.getHeight() >= height && size.getWidth() >= width) {
				outputSizes.add(size);
			}
		}
		return outputSizes.toArray(sizes);
	}

	public static Size[] smallerThan(Size[] sizes, int width, int height) {
		List<Size> outputSizes = new ArrayList<>();
		for (Size size: sizes) {
			if (size.getHeight() <= height && size.getWidth() <= width) {
				outputSizes.add(size);
			}
		}
		return outputSizes.toArray(sizes);
	}

	public static Size withGreatestArea(Size[] sizes) {
		Size maxSize = null;
		int maxArea = 0;
		for (Size size: sizes) {
			int area = size.getWidth() * size.getHeight();
			if (area > maxArea) {
				maxArea = area;
				maxSize = size;
			}
		}
		return maxSize;
	}

	public static Size withSmallestArea(Size[] sizes) {
		Size minSize = null;
		int minArea = Integer.MAX_VALUE;
		for (Size size: sizes) {
			int area = size.getWidth() * size.getHeight();
			if (area < minArea) {
				minArea = area;
				minSize = size;
			}
		}
		return minSize;
	}

	public static Size[] validPreviewSize(Size[] sizes) {
		return smallerThan(sizes, 1920 /* width */, 1080 /* height */);
	}

	public static Size largestWindowFit(Size[] sizes, Activity activity) {
		Point windowSize = new Point();
		activity.getWindowManager().getDefaultDisplay().getSize(windowSize);
		int width = windowSize.x;
		int height = windowSize.y;
		sizes = withAspectRatio(sizes, width, height);
		sizes = smallerThan(sizes, width, height);
		sizes = validPreviewSize(sizes);
		return withGreatestArea(sizes);
	}

	public static Size[] get(CameraCharacteristics characteristics,) {

	}

	private CameraSizes() {}
}
