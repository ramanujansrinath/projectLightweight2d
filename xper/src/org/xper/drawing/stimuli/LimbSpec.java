package org.xper.drawing.stimuli;

import com.thoughtworks.xstream.XStream;


public class LimbSpec {
	
	// fields:
	int nodeId;
	double length;
	double width;
	double width2;						// used only for first limb (width for each node)
	double ori;
	double[] curv;						// this will be length 4, except for first limb it will be length 8 (two nodes)
	double[] xy = new double[]{0,0};	// x,y position will only be used for the first limb and will default to [0 0]
	boolean isSmoother = false;			// this dictates whether a node starts off having 4 (standard) or 3 (smoother) control points
	boolean isSmoother2 = false;
	
	transient static XStream s;
	
	static {
		s = new XStream();
		s.alias("LimbSpec", LimbSpec.class);
	}
	
	public String toXml() {
		return LimbSpec.toXml(this);
	}
	
	public static String toXml(LimbSpec spec) {
		return s.toXML(spec);
	}
	
	public static LimbSpec fromXml(String xml) {
		LimbSpec g = (LimbSpec)s.fromXML(xml);
		return g;
	}
	
	public LimbSpec() {}
	
	public LimbSpec(LimbSpec d) {
		nodeId = d.getNodeId();
		length = d.getLength();
		width = d.getWidth();
		width2 = d.getWidth2();
		ori = d.getOri();
		curv = d.getCurv().clone();
		xy = d.getXy().clone();
		isSmoother = d.isSmoother();
	}

	public LimbSpec(int n,double l,double w,double o,double[] c) {
		nodeId = n;
		length = l;
		width = w;
		width2 = w;
		ori = o;
		curv = c.clone();
	}

	public LimbSpec(int n,double l,double w,double o,double[] c,double[] xy) {
		nodeId = n;
		length = l;
		width = w;
		width2 = w;
		ori = o;
		curv = c.clone();
		this.xy = xy.clone();
	}
	
	public LimbSpec(int n,double l,double w,double o,double[] c,boolean isSmoother) {
		nodeId = n;
		length = l;
		width = w;
		width2 = w;
		ori = o;
		curv = c.clone();
		this.isSmoother = isSmoother;
	}

	public LimbSpec(int n,double l,double w,double o,double[] c,double[] xy,boolean isSmoother) {
		nodeId = n;
		length = l;
		width = w;
		width2 = w;
		ori = o;
		curv = c.clone();
		this.xy = xy.clone();
		this.isSmoother = isSmoother;
	}
	
	public LimbSpec(int n,double l,double w,double w2,double o,double[] c) {
		// this should only be used for the first limb created
		nodeId = n;
		length = l;
		width = w;
		width2 = w2;
		ori = o;
		curv = c.clone();
	}
	
	public LimbSpec(int n,double l,double w,double w2,double o,double[] c, double[] xy) {
		// this should only be used for the first limb created
		nodeId = n;
		length = l;
		width = w;
		width2 = w2;
		ori = o;
		curv = c.clone();
		this.xy = xy.clone();
	}
	
	public LimbSpec(int n,double l,double w,double w2,double o,double[] c,boolean isSmoother) {
		// this should only be used for the first limb created
		nodeId = n;
		length = l;
		width = w;
		width2 = w2;
		ori = o;
		curv = c.clone();
		this.isSmoother = isSmoother;
	}
	
	public LimbSpec(int n,double l,double w,double w2,double o,double[] c, double[] xy,boolean isSmoother) {
		// this should only be used for the first limb created
		nodeId = n;
		length = l;
		width = w;
		width2 = w2;
		ori = o;
		curv = c.clone();
		this.xy = xy.clone();
		this.isSmoother = isSmoother;
	}
	
	public LimbSpec(int n,double l,double w,double w2,double o,double[] c,boolean isSmoother,boolean isSmoother2) {
		// this should only be used for the first limb created
		nodeId = n;
		length = l;
		width = w;
		width2 = w2;
		ori = o;
		curv = c.clone();
		this.isSmoother = isSmoother;
		this.isSmoother2 = isSmoother2;
	}
	
	public LimbSpec(int n,double l,double w,double w2,double o,double[] c, double[] xy,boolean isSmoother,boolean isSmoother2) {
		// this should only be used for the first limb created
		nodeId = n;
		length = l;
		width = w;
		width2 = w2;
		ori = o;
		curv = c.clone();
		this.xy = xy.clone();
		this.isSmoother = isSmoother;
		this.isSmoother2 = isSmoother2;
	}
	
	public int getNodeId() {
		return nodeId;
	}

	public void setNodeId(int nodeId) {
		this.nodeId = nodeId;
	}

	public double getLength() {
		return length;
	}

	public void setLength(double length) {
		this.length = length;
	}

	public double getWidth() {
		return width;
	}

	public void setWidth(double width) {
		this.width = width;
	}

	public double getWidth2() {
		return width2;
	}

	public void setWidth2(double width) {
		this.width2 = width;
	}

	public double getOri() {
		return ori;
	}

	public void setOri(double ori) {
		this.ori = ori;
	}

	public double[] getCurv() {
		return curv;
	}

	public void setCurv(double[] curv) {
		this.curv = curv;
	}

	public double[] getXy() {
		return xy;
	}

	public void setXy(double[] xy) {
		this.xy = xy;
	}
	
	public boolean isSmoother() {
		return isSmoother;
	}

	public void setIsSmoother(boolean isSmoother) {
		this.isSmoother = isSmoother;
	}
	
	public boolean isSmoother2() {
		return isSmoother2;
	}

	public void setIsSmoother2(boolean isSmoother2) {
		this.isSmoother2 = isSmoother2;
	}

}

