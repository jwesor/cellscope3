package edu.berkeley.cellscope3.feed.camera2;

import android.app.Activity;
import android.graphics.Point;
import android.util.Log;
import android.util.Size;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class CameraSizes {

	private static final String TAG = CameraSizes.class.getSimpleName();

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
			if (ratio(size) <= targetRatio) {
				outputSizes.add(size);
			}
		}
		return outputSizes.toArray(new Size[outputSizes.size()]);
	}

	public static Size[] withNarrowerAspectRatio(Size[] sizes, int width, int height) {
        List<Size> outputSizes = new ArrayList<>();
        float targetRatio = ratio(width, height);
        for (Size size : sizes) {
            if (ratio(size) >= targetRatio) {
                outputSizes.add(size);
            }
        }
        return outputSizes.toArray(new Size[outputSizes.size()]);
    }

	private static float ratio(Size size) {
	    return ratio(size.getWidth(), size.getHeight());
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
		Log.d(TAG,Arrays.toString(sizes));
		sizes = smallerThan(sizes, width, height);
		Log.d(TAG,Arrays.toString(sizes));
		sizes = validPreviewSize(sizes);
		Log.d(TAG,Arrays.toString(sizes));
		Size size = withGreatestArea(sizes);
		return reorient(size, width, height);
	}

	public static Size reorient(Size size, int width, int height) {
	    if ( (width - height) * (size.getWidth() - size.getHeight()) > 0) {
	        return size;
        }
        return new Size(size.getHeight(), size.getWidth());
    }

	private CameraSizes() {}
}
