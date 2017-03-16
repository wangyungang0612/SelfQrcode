package com.demo.selfqrcode;

import android.widget.Toast;

import com.google.zxing.Result;

public class Capture extends CaptureActivity{
	@Override
	public void doResult(Result rawResult) {
		Toast.makeText(this, rawResult.getText(), Toast.LENGTH_LONG).show();
	}
}
