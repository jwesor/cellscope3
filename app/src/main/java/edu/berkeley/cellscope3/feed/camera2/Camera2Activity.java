package edu.berkeley.cellscope3.feed.camera2;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

public class Camera2Activity extends FragmentActivity {

	private Fragment fragment;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState == null) {
			fragment = new Camera2Fragment();
			getSupportFragmentManager().beginTransaction()
					.add(android.R.id.content, fragment, "CAMERA2")
					.commit();
		} else {
			fragment = getSupportFragmentManager().findFragmentByTag("CAMERA2");
		}
	}
}
