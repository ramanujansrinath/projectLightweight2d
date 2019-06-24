package org.xper.drawing.stimuli;

import java.util.Arrays;

import org.xper.utils.SachMathUtil;


public class Node {
	
	int nodeID;
	double width;							// width for this node (this sets up the distances for the original 4 control points at each node)
	double ori;								// ori used to create node
	double[] xy;							// x,y position
	double[] curv = new double[]{1,1,1,1};	// default curvature
	boolean isSmoother = false;				// this dictates whether a node starts off having 4 (standard) or 3 (smoother) control points

	int numCxdNodes = 0;
	int[] cxdNodes = new int[4];			// max number of connected nodes is 4 (like +)
	double[] oris = new double[4];			// ori from this node to each connected node (this will not necessarily be the same as that used during creation!)
	double[] lengths = new double[4];		// distance to connected node (not sure I need this)
	double[] widths = new double[4];		// widths of connected nodes
	
	double[][] ctrlPts = new double[8][2];	// max # ctrl pts per node is 8
	
	
	public Node(int nodeID,double w,double ori,double[] xy) {
		createNode(nodeID,w,w,ori,xy,1,1,1,1,false);	// creates ctrlPts for new bare node
	}
	
	public Node(int nodeID,double wx,double wy,double ori,double[] xy) {
		createNode(nodeID,wx,wy,ori,xy,1,1,1,1,false);	// creates ctrlPts for new bare node, but can take different x (end node length) and y (end node width) widths
	}
	
	public Node(int nodeID,double w,double ori,double[] xy,double curv,double elong) {
		createNode(nodeID,w,w,ori,xy,curv,curv,elong,elong,false);	// creates ctrlPts for new bare node, but can take different x (end node length) and y (end node width) widths
	}
	
	public Node(int nodeID,double w,double ori,double[] xy,double curvB,double curvC,double elongB,double elongC) {
		createNode(nodeID,w,w,ori,xy,curvB,curvC,elongB,elongC,false);	// creates ctrlPts for new bare node, but can take different x (end node length) and y (end node width) widths
	}
	
	public Node(int nodeID,double w,double ori,double[] xy,double curvB,double curvC,double elongB,double elongC,boolean isSmoother) {
		createNode(nodeID,w,w,ori,xy,curvB,curvC,elongB,elongC,isSmoother);	// creates ctrlPts for new bare node, but can take different x (end node length) and y (end node width) widths
	}
	
	public Node(int nodeID,double w,double ori,double[] xy,double curv,double elong,boolean isSmoother) {
		createNode(nodeID,w,w,ori,xy,curv,curv,elong,elong,isSmoother);	// creates ctrlPts for new bare node, but can take different x (end node length) and y (end node width) widths
	}
	
	public Node(Node n) {
		double[] c = n.getCurv();
		createNode(n.getNodeID(),n.getWidth(),n.getWidth(),n.getOri(),n.getXy(),c[0],c[1],c[2],c[3],n.isSmoother());
	}

	void createNode(int nodeID,double wx,double wy,double ori,double[] xy,double cyB,double cyC,double cxB,double cxC,boolean isSmoother) {
		// -- length, width, (x y) position
		// the following is for developing independent choosing of limbs and configurations
		// also: want to potential have two width values (in original x and y directions) - adds more flexibilty in creating objects
		// curvature changes: cx elongates node, cy changes the end pointiness
		
		//  A---B-----------C---D		: this is the arrangement of the control point for a limb (2 connected nodes)
		//  |	  	          	|		: ends will be rounded, stars show the node locations
		//  | *----length-----*	|
		//  |                 	|
		//  H---G-----------F---E
		
		//    A-----B		: this is the arrangement of the control points for a node
		//      	|		: end will be rounded, star shows the node centroid location
		//    	 *	|
		//  	  	|
		//    D-----C
		
		
		// TODO: need to allow for the creation of nodes w only 3 control points:
			//	  A	
			//      \
			//     * B
			//  	/ 
			//    C
			// : this will then set up connections between nodes that only have one control point per crotch
		
		
		// set globals:
		this.isSmoother = isSmoother;
		this.nodeID = nodeID;
		this.xy = xy.clone();
		this.width = wy;
		this.ori = ori;
		this.curv = new double[]{cyB,cyC,cxB,cxC};
		
		
		// specs:
		double x = xy[0];				// x,y position
		double y = xy[1];
		
		// base limb: straight sided, rounded ends -- 
		if (!isSmoother) {
			ctrlPts = new double[][] {	    
					{  x-wx/2, 		y+wy/2		}, 		//  0-A
					{  x+wx/2*cxB, 	y+wy/2*cyB	}, 		//  1-B
					{  x+wx/2*cxC, 	y-wy/2*cyC	},		//  2-C	
					{  x-wx/2, 		y-wy/2		} 		//  3-D	
				};
		} else {
			ctrlPts = new double[][] {	    
					{  x-wx/2, 		y+wy/2		}, 		//  0-A
					{  x+wx/2*cyB, 	y+wy*(cyC-1)}, 		//  1-B
					{  x-wx/2, 		y-wy/2		} 		//  3-D	
					// testing (to get full gamut of curvature types):
//					{  x, 		y+wy/2		}, 			//  0-A
//					{  x+wx/3*cxB, 	y+wy*(cyB-1)}, 		//  1-B
//					{  x, 		y-wy/2		} 			//  3-D	
				};
		}
		
		ctrlPts = SachMathUtil.rotateAxis2D(ori,xy,ctrlPts);
	}
	
	void addNode(int nodeID,double l,double w,double ori) {	// add cxdNode to this node
		cxdNodes[numCxdNodes] = nodeID;
		oris[numCxdNodes] = ori;		// add ori
		lengths[numCxdNodes] = l;
		widths[numCxdNodes] = w;
		// update ctrl pts? -- no, doing this in BsplineObject for now
		
		numCxdNodes++;
	}
	
	double oriToNode(int nodeID) {
		int idx = Arrays.binarySearch(cxdNodes,nodeID);
		return oris[idx];
	}
	
	// track node poisitions?
	
	
	// --- getters and setters
	
	public int getNodeID() {
		return nodeID;
	}

	public int getNumCxdNodes() {
		return numCxdNodes;
	}

	public int[] getCxdNodes() {
		return cxdNodes.clone();
	}

	public double[][] getCtrlPts() {
		// deep clone this
		int numCtrlPts = ctrlPts.length;
		double[][] outCtrlPts = new double[numCtrlPts][2];
		for (int n=0;n<numCtrlPts;n++) {
			outCtrlPts[n] = ctrlPts[n].clone();
		}
		return outCtrlPts;
	}

	public double[] getOris() {
		return oris.clone();
	}
	
	public double getOri() {
		return ori;
	}
	
	public double getWidth() {
		return width;
	}
	
	public double[] getXy() {
		return xy.clone();
	}
	
	public double[] getCurv() {
		return curv.clone();
	}
	
	public boolean isSmoother() {
		return isSmoother;
	}
	
}
