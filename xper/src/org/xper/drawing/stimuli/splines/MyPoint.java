package org.xper.drawing.stimuli.splines;

public class MyPoint 
{
	public double x, y;

	MyPoint(MyPoint p)
	{
		x = p.x;
		y = p.y;
	}
	public MyPoint(double _x, double _y)
	{
		x = _x;
		y = _y;
	}
	MyPoint()
	{
		x = 0;
		y = 0;
	}
	void copy(MyPoint p)
	{
		x = p.x;
		y = p.y;
	}
}