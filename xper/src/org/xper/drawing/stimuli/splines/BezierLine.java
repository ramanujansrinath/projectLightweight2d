package org.xper.drawing.stimuli.splines;

import java.awt.Color;
import java.awt.Graphics;

public class BezierLine extends PointList {

	public MyPoint bpt[];			// spline points
	int bnum;						// # of spline points
	boolean ready;
	final int MAXPOINT = 1800;		// max # of spline points
//	final int ENOUGH = 1;
	final double ENOUGH = 0.1;
	final int RECURSION = 900;
	int nPointAlloc;
//	int enough;						// control how well we draw the curve.
	double enough;						// control how well we draw the curve.
	int nRecur;						// counter of number of recursion
	MyPoint buffer[][];
	int nBuf, nBufAlloc;

	public BezierLine() {
		bpt = new MyPoint[MAXPOINT];
		nPointAlloc = MAXPOINT;
		bnum = 0;
		enough = ENOUGH;
		showLine = true;
		ready = false;
		buffer = null;
	}

	protected double distance(MyPoint p0,MyPoint p1,MyPoint p2)
	{
		double a,b,y1,x1,d1,d2;

		if (p1.x==p2.x && p1.y==p2.y) return Math.min(Math.abs(p0.x-p1.x),Math.abs(p0.y-p1.y));
		a=p2.x-p1.x;    
		b=p2.y-p1.y;
		y1=b*(p0.x-p1.x)+a*p1.y;
		x1=a*(p0.y-p1.y)+b*p1.x;
		d1=Math.abs(y1-a*p0.y);
		d2=Math.abs(x1-b*p0.x);
		if (a==0) return Math.abs(d2/b);
		if (b==0) return Math.abs(d1/a);
		return Math.min(Math.abs(d1/a),Math.abs(d2/b));
	}

	protected void curve_split(MyPoint p[],MyPoint q[],MyPoint r[],int num)
	{
		int i,j;

		//      for (i=0;i<num;i++) q[i] = new Point(p[i]);
		for (i=0;i<num;i++) q[i].copy(p[i]);
		for (i=1;i<=num-1;i++) {
			//         r[num-i] = new Point(q[num-1]);
			r[num-i].copy(q[num-1]);
			for (j=num-1;j>=i;j--) {
				//	    q[j] = new Point((q[j-1].x+q[j].x)/2, (q[j-1].y+q[j].y)/2);
				q[j].x = (q[j-1].x+q[j].x)/2;
				q[j].y = (q[j-1].y+q[j].y)/2;
			}
		}
		//      r[0] = new Point(q[num-1]);
		r[0].copy(q[num-1]);
	}

	// reuse buffer
	private MyPoint get_buf(int num)[] {
		MyPoint b[];
		if (buffer == null) {
			buffer = new MyPoint[500][num];
			nBufAlloc = 500;
			nBuf = 0;
		}
		if (nBuf == 0) {
			b = new MyPoint[num];
			for (int i=0; i< num; i++) b[i] = new MyPoint();
			return b;
		}
		else {
			nBuf --;
			b = buffer[nBuf];
			return b;
		}
	}

	private void put_buf(MyPoint b[]) {
		if (nBuf >= nBufAlloc) {
			MyPoint newBuf[][] = new MyPoint[nBufAlloc + 500][num];
			for (int i=0; i<nBuf; i++) newBuf[i] = buffer[i];
			nBufAlloc += 500;
			buffer = newBuf;
		}
		buffer[nBuf] = b;
		nBuf++;
	}

	protected boolean bezier_generation(MyPoint pt[], int num, MyPoint result[], int n[]) {
		MyPoint qt[],rt[];	// for split
		int i;	//d[],,max
		double d[],max;

		nRecur++;
		if (nRecur > RECURSION) return false;

		//d = new int[MAXCNTL];
		d = new double[MAXCNTL];
		for (i=1;i<num-1;i++) d[i]=distance(pt[i],pt[0],pt[num-1]);
		max=d[1];
		for (i=2;i<num-1;i++) if (d[i]>max) max=d[i];
		if (max <= enough || nRecur > RECURSION) {
			if (n[0]==0) {
				if (bnum > 0) 
					result[0].copy(pt[0]);
				else
					result[0] = new MyPoint(pt[0]);
				n[0]=1;
			}
			//reuse
			if (bnum > n[0])
				result[n[0]].copy(pt[num-1]);
			else
				result[n[0]] = new MyPoint(pt[num-1]);
			n[0]++;
			if (n[0] == MAXPOINT-1) return false;
		}
		else {
			//	   qt = new Point[num];
			//	   rt = new Point[num];
			qt = get_buf(num);
			rt = get_buf(num);
			curve_split(pt,qt,rt,num);
			if (!bezier_generation(qt,num,result,n)) return false;
			put_buf(qt);
			if (!bezier_generation(rt,num,result,n)) return false;
			put_buf(rt);
		}
		return true;
	}

	public boolean try_bezier_generation(MyPoint pt[], int num, MyPoint result[], int n[])
	{
		int oldN = n[0];

		if (enough == ENOUGH && num > 6) enough += 3;
		//       if (enough > ENOUGH) enough -= 5;
		nRecur = 0;
		// in case of recursion stack overflow, relax "enough" and keep trying
		while (!bezier_generation(pt, num, bpt, n))
		{
			n[0] = oldN;
			enough += 5;
			nRecur = 0;
		}
		return true;
	}

	public boolean createFinal()
	{
		int n[];
		n = new int[1];
		if (!try_bezier_generation(pt, num, bpt, n)) 
		{
			bnum = 0;
			return false;
		}
		else {
			bnum = n[0];
			return true;
		}
	}

	public boolean done()
	{
		//num --;			// don't need to do this here. only for when creating splines using mouse clicks
		showLine = false;
		ready = true;
		return createFinal();
	}

	public void draw(Graphics g)
	{
		g.setColor(new Color(rColor,gColor,bColor));
		if (showLine)
		{
			super.draw(g);		// PointList drawing routine (crosses and dashed lines)
			if (curPt != -1)
				g.drawRect((int)(pt[curPt].x-range), (int)(pt[curPt].y-range), (int)(2*range+1),(int)(2*range+1));	// box around selected point
		}

		if (ready)
			for (int i=0; i< bnum-1; i++)
			{
				g.drawLine((int)bpt[i].x,(int)bpt[i].y,(int)bpt[i+1].x,(int)bpt[i+1].y);   // draws spline
			}
	}

	public boolean isReady() {
		return ready;
	}

	public int getBnum() {
		return bnum;
	}
}