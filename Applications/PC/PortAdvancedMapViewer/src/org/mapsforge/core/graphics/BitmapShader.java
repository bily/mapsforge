package org.mapsforge.core.graphics;

/*
 * Copyright (C) 2010 The Android Open Source Project
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

//import android.graphics.Shader.TileMode;
/**
 * Delegate implementing the native methods of android.graphics.BitmapShader
 * 
 * Through the layoutlib_create tool, the original native methods of
 * BitmapShader have been replaced by calls to methods of the same name in this
 * delegate class.
 * 
 * This class behaves like the original native implementation, but in Java,
 * keeping previously native data into its own objects and mapping them to int
 * that are sent back and forth between it and the original BitmapShader class.
 * 
 * Because this extends {@link Shader_Delegate}, there's no need to use a
 * {@link DelegateManager}, as all the Shader classes will be added to the
 * manager owned by {@link Shader_Delegate}.
 * 
 * @see Shader_Delegate
 * 
 */
public class BitmapShader extends Shader {

	private java.awt.Paint mJavaPaint;
	private final Bitmap mBitmap;

	public BitmapShader(Bitmap mBitmap, TileMode tileX, TileMode tileY) {
		this.mBitmap = mBitmap;
	}

	public Bitmap getBitmap() {
		return mBitmap;
	}

	@Override
	public java.awt.Paint getJavaPaint() {
		return mJavaPaint;
	}

	public int getTransparency() {
		return java.awt.Paint.TRANSLUCENT;
	}
}