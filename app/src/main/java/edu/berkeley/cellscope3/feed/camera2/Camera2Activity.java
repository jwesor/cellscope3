package edu.berkeley.cellscope3.feed.camera2;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;

public class Camera2Activity extends Activity {

	private Fragment fragment;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState == null) {
			fragment = new Camera2Fragment();
			getFragmentManager().beginTransaction()
					.add(android.R.id.content, fragment, "CAMERA2")
					.commit();
		} else {
			fragment = getFragmentManager().findFragmentByTag("CAMERA2");
		}
	}
}
