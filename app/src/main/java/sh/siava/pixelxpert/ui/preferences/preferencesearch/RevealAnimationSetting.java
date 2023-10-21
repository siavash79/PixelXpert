package sh.siava.pixelxpert.ui.preferences.preferencesearch;

/*
 * https://github.com/ByteHamster/SearchPreference
 *
 * MIT License
 *
 * Copyright (c) 2018 ByteHamster
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

import android.os.Parcel;
import android.os.Parcelable;

public class RevealAnimationSetting implements Parcelable {
	public static final Creator<RevealAnimationSetting> CREATOR = new Creator<>() {
		@Override
		public RevealAnimationSetting createFromParcel(Parcel in) {
			return new RevealAnimationSetting(in);
		}

		@Override
		public RevealAnimationSetting[] newArray(int size) {
			return new RevealAnimationSetting[size];
		}
	};
	private final int centerX;
	private final int centerY;
	private final int width;
	private final int height;
	private final int colorAccent;

	public RevealAnimationSetting(int centerX, int centerY, int width, int height, int colorAccent) {
		this.centerX = centerX;
		this.centerY = centerY;
		this.width = width;
		this.height = height;
		this.colorAccent = colorAccent;
	}

	private RevealAnimationSetting(Parcel in) {
		centerX = in.readInt();
		centerY = in.readInt();
		width = in.readInt();
		height = in.readInt();
		colorAccent = in.readInt();
	}

	public int getCenterX() {
		return centerX;
	}

	public int getCenterY() {
		return centerY;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public int getColorAccent() {
		return colorAccent;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(centerX);
		dest.writeInt(centerY);
		dest.writeInt(width);
		dest.writeInt(height);
		dest.writeInt(colorAccent);
	}
}
