package org.xper.drawing.stimuli;

import org.xper.utils.SachMathUtil;

//import org.xper.sach.expt.behavior.testing.StimTestWindow;
//import org.xper.sach.util.SachMathUtil;

public class LimbObject {

	public double[][] endPts;
	public double[] endPtCxns;
	public double[][] cntlPts;
	double[] center;
	double length;
	double width;
	double ori;

	

	//need to add curvature?
	
	public void init(double length, double width, double ori, double[] center) {
		this.length = length;
		this.width = width;
		this.ori = ori;
		this.center = center;
		
		createLimb();
	}
	
	void createLimb() {
		// the following is for developing independent choosing of limbs and configurations
		
		//  A---B-----------C---D		: this is the arrangement of the control point for a limb
		//  |	  	          	|		: ends will be rounded
		//  | *----length-----*	|
		//  |                 	|
		//  H---G-----------F---E
		
		// specs:
		double w = width;						// width
		double l = length;					// length
		double x = center[0];					// center: xPos yPos
		double y = center[1];
		
		// end points (for tracking limb later)
		endPts = new double[][] {{-l+x,y},{l+x,y},{x,y}}; // these mark the ends and middle of the stick figure version of the limb
		endPts = SachMathUtil.rotateAxis2D(ori,center,endPts);
		//endPtCxns = new double[] {0,0,0};
		
		// base limb: straight sided, rounded ends
		cntlPts = new double[][] {	   
				{ -l-w/2+x,  w/2+y},		//  0-A	
				{ -l+w/2+x,  w/2+y},		//  1-B 
				{  l-w/2+x,  w/2+y}, 		//  2-C
				{  l+w/2+x,  w/2+y}, 		//  3-D
				{  l+w/2+x, -w/2+y},		//  4-E	
				{  l-w/2+x, -w/2+y},		//  5-F 
				{ -l+w/2+x, -w/2+y}, 		//  6-G
				{ -l-w/2+x, -w/2+y}, 		//  7-H	
			};
		cntlPts = SachMathUtil.rotateAxis2D(ori,center,cntlPts);		
	}
	
	void createLimb_old() {
		// the following is for developing independent choosing of limbs and configurations
		
		//   K-L-----A-----B-C		: this is the arrangement of the control point for a limb
		//  /	  	          \		: ends will be rounded (as half circles)
		// J   <--length--->   D
		//  \                 /
		//   I-H-----G-----F-E
		
		// specs:
		double w = width/2;						// width
		double l = length/2;					// length
		double x = center[0];					// center: xPos yPos
		double y = center[1];
		final double BASE_CURV_A = 1.25; //1.25;
		final double BASE_CURV_B = 0.60; //2.20;
		double h = w*BASE_CURV_A;
		double d = w*BASE_CURV_B;
//		double ce1 = w*h;		// curve end 1
//		double ce2 = w*h;		// curve end 2
		double cs1 = 0;			// curve side 1
		double cs2 = 0;			// curve side 2
		
		// end points (for tracking limb later)
		endPts = new double[][] {{-l+x,y},{l+x,y},{x,y},{x,y}}; // these mark the ends and middle of the stick figure version of the limb
		endPts = SachMathUtil.rotateAxis2D(ori,center,endPts);
		endPtCxns = new double[] {0,0,0};
		
		// base limb: straight sided, rounded ends
		cntlPts = new double[][] {	   
				{ 0+x, w+y+cs1},			//  0-A	
				{ l+x, w+y},				//  1-B 
				{ l+x+h-d, w+y}, 		//  2-C
				{ l+x+h, 0+y}, 			//  3-D
				{ l+x+h-d,-w+y}, 		//  4-E
				{ l+x,-w+y}, 				//  5-F
				{ 0+x,-w+y-cs2}, 			//  6-G
				{-l+x,-w+y}, 				//  7-H
				{-l+x-h+d,-w+y}, 		//  8-I
				{-l+x-h, 0+y}, 			//  9-J
				{-l+x-h+d, w+y}, 		// 10-K
				{-l+x, w+y},				// 11-L
			};
		cntlPts = SachMathUtil.rotateAxis2D(ori,center,cntlPts);		
	}
	
	public static void main(String[] args) {   
//		// use StimTest to draw the stim here:
//		StimTestWindow testWindow = new StimTestWindow();
//		BsplineObject s = new BsplineObject();
//				
//		s.creatObj(0);	// create multi-limb object
//		testWindow.setStimObj(s);	// add object to be drawn
//		testWindow.testDraw();		// draw object
	}
	
}
