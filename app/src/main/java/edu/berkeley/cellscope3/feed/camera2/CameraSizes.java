package edu.berkeley.cellscope3.feed.camera2;

import android.app.Activity;
import android.graphics.Point;
import android.util.Size;

import java.util.ArrayList;
import java.util.List;

public final class CameraSizes {

	public static Size[] withClosestAspectRatio(Size[] sizes, int width, int height) {
		List<Size> outputSizes = new ArrayList<>();
		float targetRatio = ratio(width, height);
		float bestDiff = Float.MAX_VALUE;
		for (Size size : sizes) {
			float currentRatio = ratio(size.getWidth(), size.getHeight());
			float diff = Math.abs(targetRatio - currentRatio);
			if (diff < bestDiff) {
				bestDiff = diff;
				outputSizes.clear();
			}
			if (diff == bestDiff) {
				outputSizes.add(size);
			}
		}
		return outputSizes.toArray(new Size[outputSizes.size()]);
	}

	public static Size[] withWiderAspectRatio(Size[] sizes, int width, int height) {
		List<Size> outputSizes = new ArrayList<>();
		float targetRatio = ratio(width, height);
		for (Size size : sizes) {
			if (ratio(size.getWidth(), size.getHeight()) <= targetRatio) {
				outputSizes.add(size);
			}
		}
		return outputSizes.toArray(new Size[outputSizes.size()]);
	}

	private static float ratio(int width, int height) {
		// Normalize ratio so that it's always > 1
		float ratio = (float) width / height;
		return ratio > 1 ? ratio : 1f / ratio;
	}

	public static Size[] largerThan(Size[] sizes, int width, int height) {
		List<Size> outputSizes = new ArrayList<>();
		for (Size size : sizes) {
			if (size.getHeight() >= height && size.getWidth() >= width) {
				outputSizes.add(size);
			}
		}
		return outputSizes.toArray(new Size[outputSizes.size()]);
	}

	public static Size[] smallerThan(Size[] sizes, int width, int height) {
		List<Size> outputSizes = new ArrayList<>();
		for (Size size : sizes) {
			if (size.getHeight() <= height && size.getWidth() <= width) {
				outputSizes.add(size);
			}
		}
		return outputSizes.toArray(new Size[outputSizes.size()]);
	}

	public static Size withGreatestArea(Size[] sizes) {
		Size maxSize = null;
		int maxArea = 0;
		for (Size size : sizes) {
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
		for (Size size : sizes) {
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
		sizes = withWiderAspectRatio(sizes, width, height);
		sizes = withClosestAspectRatio(sizes, width, height);
		sizes = smallerThan(sizes, width, height);
		sizes = validPreviewSize(sizes);
		return withGreatestArea(sizes);
	}

	private CameraSizes() {
	}
}
