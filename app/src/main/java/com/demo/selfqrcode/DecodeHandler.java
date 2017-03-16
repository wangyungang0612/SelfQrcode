/*
 * Copyright (C) 2010 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.demo.selfqrcode;

import java.util.Map;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.demo.selfqrcode.camera.CameraManager;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

final class DecodeHandler extends Handler {

  private static final String TAG = DecodeHandler.class.getSimpleName();

  private final CaptureActivity activity;
  private final MultiFormatReader multiFormatReader;
  private boolean running = true;

  DecodeHandler(CaptureActivity activity, Map<DecodeHintType,Object> hints) {
    multiFormatReader = new MultiFormatReader();
    multiFormatReader.setHints(hints);
    this.activity = activity;
  }

  @Override
  public void handleMessage(Message message) {
    if (!running) {
      return;
    }
    switch (message.what) {
      case R.id.decode:
        decode((byte[]) message.obj, message.arg1, message.arg2);
        break;
      case R.id.quit:
        running = false;
        Looper.myLooper().quit();
        break;
    }
  }

  /**
   * Decode the data within the viewfinder rectangle, and time how long it took. For efficiency,
   * reuse the same reader objects from one decode to the next.
   *
   * @param data   The YUV preview frame.
   * @param width  The width of the preview frame.
   * @param height The height of the preview frame.
   */
  private void decode(byte[] data, int width, int height) {
    long start = System.currentTimeMillis();
    Result rawResult = null;
    
    // hack: rotate the image
    byte[] rotatedData = new byte[data.length];
  	byte[] rotatedData_front1 = new byte[data.length];
  	byte[] rotatedData_front2 = new byte[data.length];
	for (int y = 0; y < height; y++) {
	    for (int x = 0; x < width; x++){
	    	rotatedData[x * height + height - y - 1] = data[x + y * width];
	    	rotatedData_front1[x * height + height - y - 1] = data[width - 1 - x + y * width];
	    	rotatedData_front2[x * height + height - y - 1] = data[width - 1 - x + (height - 1 - y) * width];
	    }
	}
	PlanarYUVLuminanceSource source_front = null;
	PlanarYUVLuminanceSource source = null;
	if(CameraManager.isFront()){
		source_front = activity.getCameraManager().buildLuminanceSource(rotatedData_front2, height, width);
		if(source_front != null){
			rawResult = getRawResult(source_front);
		}
		source_front = activity.getCameraManager().buildLuminanceSource(rotatedData_front1, height, width);
		if(rawResult == null){
			if(source_front != null){
				rawResult = getRawResult(source_front);
			}
		}
	}else{
		source = activity.getCameraManager().buildLuminanceSource(rotatedData, height, width);
		if(source != null){
			rawResult = getRawResult(source);
		}
	}

    Handler handler = activity.getHandler();
    if (rawResult != null) {
      // Don't log the barcode contents for security.
      long end = System.currentTimeMillis();
      Log.d(TAG, "Found barcode in " + (end - start) + " ms");
      if (handler != null) {
        Message message = Message.obtain(handler, R.id.decode_succeeded, rawResult);
        Bundle bundle = new Bundle();
        Bitmap grayscaleBitmap = null;
        if(CameraManager.isFront()){
        	grayscaleBitmap = toBitmap(source_front, source_front.renderCroppedGreyscaleBitmap());
        }else{
        	grayscaleBitmap = toBitmap(source, source.renderCroppedGreyscaleBitmap());
        }
        bundle.putParcelable(DecodeThread.BARCODE_BITMAP, grayscaleBitmap);
        message.setData(bundle);
        message.sendToTarget();
      }
    } else {
      if (handler != null) {
        Message message = Message.obtain(handler, R.id.decode_failed);
        message.sendToTarget();
      }
    }
  }

  private static Bitmap toBitmap(LuminanceSource source, int[] pixels) {
    int width = source.getWidth();
    int height = source.getHeight();
    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
    return bitmap;
  }
  
  private Result getRawResult(LuminanceSource source){
	  Result rawResult = null;
	  if (source != null) {
	      BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
	      try {
	        rawResult = multiFormatReader.decodeWithState(bitmap);
	      } catch (ReaderException re) {
	        // continue
	      } finally {
	        multiFormatReader.reset();
	      }
	    }
	  return rawResult;
  }

}
