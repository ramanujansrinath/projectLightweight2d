package org.xper.utils;

public class RGBColor {
	float red;

	float green;

	float blue;

	public RGBColor(float red, float green, float blue) {
		super();
		this.red = red;
		this.green = green;
		this.blue = blue;
	}
	
	public RGBColor(double red, double green, double blue) {
		super();
		this.red = (float)red;
		this.green = (float)green;
		this.blue = (float)blue;
	}

	public float getBlue() {
		return blue;
	}

	public void setBlue(float blue) {
		this.blue = blue;
	}

	public float getGreen() {
		return green;
	}

	public void setGreen(float green) {
		this.green = green;
	}

	public float getRed() {
		return red;
	}

	public void setRed(float red) {
		this.red = red;
	}

	public RGBColor() {
	}
}
