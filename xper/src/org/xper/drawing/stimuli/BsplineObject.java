package org.xper.drawing.stimuli;

import java.util.Arrays;
import java.util.ArrayList;
//import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.glu.GLUtessellator;
import org.lwjgl.util.glu.GLUtessellatorCallbackAdapter;
import org.lwjgl.util.glu.tessellation.GLUtessellatorImpl;
import org.xper.drawing.drawables.Drawable;
import org.xper.drawing.stimuli.splines.BsplineLine;
import org.xper.drawing.stimuli.splines.MyPoint;
import org.xper.utils.BiasRandom;
import org.xper.utils.RGBColor;
import org.xper.utils.SachIOUtil;
import org.xper.utils.SachMathUtil;
import org.xper.utils.SachStringUtil;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

public class BsplineObject implements Drawable {
	//for creating multi-limb objects from sets of nodes
	BsplineObjectSpec spec = new BsplineObjectSpec();
		
	boolean doCenterObject = 	true;				// re-centers object
	boolean solid =  			true; 				// for draw filled rather than outline objects
	public boolean drawCtrlPts =  		false;				// (for debugging)
	public boolean drawNodes =  		false;				// (for debugging)
	public boolean drawNums =  		false; 	 			// (for debugging)
	boolean printStimSpecs = 	false;				// (for debugging) 
	double jointAngleCurvFactor = 0.3;				// from 0 to 1, roundedness of joint angles, *** need to re-work this, want to ensure fixed medial-axis with changes in width, etc. ***
	
	static int maxNumNodes = 20;
	Node[] nodes = new Node[maxNumNodes];			// up to 20 nodes, use this to track node info
	int numNodes = 0;
	
	static public enum StimType { BEH_MedialAxis, BEH_Curvature, BEH, GA, GAManual, BLANK, NA };
	
	BsplineLine spline;// = new BsplineLine();
	double[][] objCtrlPts;
	int[] ctrlPtNodeIDs;							// track source node for each ctrl point
	//									     0     	1       2        		3       	4       	5       6     		7      
	static public enum StimCategories { SEVEN_h, 	Y_h, 	downL_downT, 	I_downT, 	SEVEN_t, 	Y_t,	downL_J, 	I_J, 	
										
										//	8		9		10				11			12			13		14			15
										downL_h,	I_h, 	SEVEN_downT, 	Y_downT, 	downL_t, 	I_t, 	SEVEN_J, 	Y_J, 
										
										// 16 		17		18				19			20			21		22			23
										SEVEN_h_ud,	Y_h_ud,	downL_downT_ud,	I_downT_ud,	SEVEN_t_ud,	Y_t_ud,	downL_J_ud,	I_J_ud, 	
										
										//	24		25		26				27			28			29		30			31
										downL_h_ud,	I_h_ud,	SEVEN_downT_ud,	Y_downT_ud,	downL_t_ud,	I_t_ud,	SEVEN_J_ud,	Y_J_ud,
										
										// 32		33
										square, 	triangle}; // JK 10 August 2016 removed these -> occluded_square, occluded_triangle }; 
	StimCategories stimCat = null;					// stimulus category
	double xPos = 0;								// global position (used to introduce jitter)
	double yPos = 0;
	static final double DEFAULT_SIZE = 4;
	double size = DEFAULT_SIZE;						// object size scaling
	double global_ori = 0;							// global ori
	List<LimbSpec> limbs = new ArrayList<LimbSpec>();	// keeps all limb info for re-creating and morphing stimuli
	double xDiff = 0, yDiff = 0;					// if doCenterObject is true, these will be the offsets
	
	boolean doRand = false;							// randomize the overall width and lengths of limbs
	boolean doMorph = false; 						// if true, we will do a morph on the current limbs (if they exist), or morph a beh stimuli (w/ category 0-7)
	
	double morphLim = -1;							// when morphing a behav stim, this controls how far along a morph line it is morphed 
	enum MorphTypes { ADD, SUBTRACT, LENGTH, WIDTH, ORI, CURV, SMOOTH, GLOBAL_ORI, SIZE, CANONICAL };
	
	boolean flagSplineViolation = false;			// this flag will be set if final object (spline) checks fail
	boolean flagLimbAddViolation = false;			// flags when there is a violation when adding a limb
	boolean cantFail = false;						// when set to true, limb additions can't fail on problems
	//boolean flagReMorph = false;					// flag for when morph fails, need to re-do morph
	
	// --- object variable limits ----
	
	static int lim_maxNumLimbs = 6, lim_minNumLimbs = 1;		// number of limbs
	static double lim_propSmoothNodes = 0.70;					// proportion of "smooth" nodes (3 control points instead of 4)
	static double lim_maxLength = 6, lim_minLength = 2;			// length 		-- decrease low end to 1?
	static double lim_maxWidth = 3.5, lim_minWidth = 0.4;		// width 		-- could increase high end a bit	
	static double lim_maxCurv = 2, lim_minCurv = 0;				// curvature 	-- increase this range? morph-lines go to ?
	static double lim_maxOri = 359.9, lim_minOri = 0.0;			// orientation 	-- 
	static double lim_propParallelLimbs = 0.2;					// 20% of new limbs have the same ori as connecting limb
	static double lim_branchingBias = 0.8;						// this helps bias the addition of new limbs to non-end nodes 
	static double lim_maxSize = DEFAULT_SIZE*1.5, lim_minSize = DEFAULT_SIZE*0.75;		// max and min sizes
	
	// JK 9 Sept 2016 morph gain/bias
	static double universalLengthMax = 10.0;
	static double universalWidthMax = 10.0;
	// morphLengthWidthOnly  boolean to restrict morphing
	boolean morphLengthWidthOnly = true;
		
	// JK
	int program = 0;
	static float zoom = 1.0f;
	static float zoomDivider = 2.5f;
	
	// JK 26 August 2016 need access to manifold nodes
	Set<Integer> manifoldNodes;
	double manifoldMorphMax = 0.0;
	double manifoldMorphMin = 0.0;
	List<Integer> limbsToSimpleMorph = new ArrayList<Integer>();
	
	double contrast = 1;
	RGBColor color = new RGBColor(0.5,0.5,0.5);
	RGBColor backColor = new RGBColor(0,0,0);

	// 26 April
	private static final int numCategories = StimCategories.values().length;
	
	public static int getNumCategories(){
		return numCategories;
	}
	
	public  String getStimCatName(){
		return stimCat.name();
	}
	
	public Node[] getNodes(){
		return nodes;
	}
	public Node getNodes(int i){
		return nodes[i];
	}
	
	public BsplineObject() {
		super();
	}
	
			
	public void createObj() {	// chooses which createObj routine to run
		
//		// for all but straight up createObj, re-save stim info to StimSpec table in DB
//			// need StimSpecID for this!
		
		// if it's a blank stim, just return (using default values):
		String stimTypeString = spec.getStimType();
		if (stimTypeString == null) stimTypeString = "NA";
		StimType stimType = StimType.valueOf(stimTypeString);
		if (stimType.equals(StimType.BLANK)) return;

		// JK 25 April 2016 added shouldUseSizeCues to have category based sizes so use the spec size here
		// size = spec.getSize();
//		size = 100.0;
//		System.out.println(size);

		if (limbs.size() > 0) {	// if limbs are passed initially
			if (doMorph) {
				// create morph obj from current limbs, for GA
				if (stimType == StimType.GA) {
					createMorphObj();
				} else {
					System.err.println("ERROR! -- trying to morph beh object that has limbs already!");
				}
			} else {
				// just create obj using current limbs
				//cantFail = true;		// this allows us to create even "invalid" stimuli for testing, etc.
				if (!createObjFromSpec()) {
					System.out.println("WARNING:object was not properly created due to some creation error.");
				}
			}
		} else {
			
			switch(stimType) {
			case BEH_MedialAxis:
				
				createBehObj();			// creates limbs
				
				
				// JK adding morphs
				if (doMorph) {
//				if(this.getSpec().isMorphLengthWidthOnly()){	
					createLengthWidthMorphObj();
				}

				break;
				
			case GA:
				
				createRandObj();		// create random obj
				break;
				
			case BLANK:

				return;					// if it's a blank stim, just return (using default values)
			default:
				break;
			}
		}		
		
		//-- close spline: (copies first 3 control points to the end to close the spline)
		closeObj();
		
		boolean printCtrlPts = false; //for debug purposes. Added May/31/2017 AWC
		
		//-- center the object: (find center of bounding box around obj and subtract from pts)
		if (doCenterObject) {
			xDiff = (SachMathUtil.max(0,objCtrlPts)+SachMathUtil.min(0,objCtrlPts))/2;
			yDiff = (SachMathUtil.max(1,objCtrlPts)+SachMathUtil.min(1,objCtrlPts))/2;
			
			if (printCtrlPts) {
				System.out.println("[...");
				for (int n = 0; n< objCtrlPts.length; n++) {
					System.out.println(objCtrlPts[n][0] + " " + objCtrlPts[n][1] + ";...");
				}
				System.out.println("]");
//			System.out.println("xDiff = " + xDiff + ", yDiff" + yDiff);
			}
		}
		double[] diffVec = new double[2];
		diffVec[0] = xDiff;
		diffVec[1] = yDiff;
		//-- initialize spline:
		spline = new BsplineLine();
		
		//-- add control points to spline: (for now, this converts control points to be used by spline methods)
		//This has been updated May/31/2017 AWC. cells after 70 are recentered "correctly"
		double[][] objCtrlPts_spline = SachMathUtil.scale(SachMathUtil.rotate2D(global_ori,SachMathUtil.subtractVectors(objCtrlPts,diffVec)),size);
		if (printCtrlPts) {
			System.out.println("[...");
			for (int n = 0; n< objCtrlPts_spline.length; n++) {
				System.out.println(objCtrlPts_spline[n][0] + " " + objCtrlPts_spline[n][1] + ";...");
			}
			System.out.println("]");
//		System.out.println("xDiff = " + xDiff + ", yDiff" + yDiff);
		}
		for (int n = 0; n < objCtrlPts_spline.length; n++) { 
			//OLD CODE...
//			double[][] objCtrlPts_final = SachMathUtil.deepCopyMatrix(objCtrlPts);
//			objCtrlPts_final[n][0] = objCtrlPts_final[n][0]-xDiff;
//			objCtrlPts_final[n][1] = objCtrlPts_final[n][1]-yDiff;
//			objCtrlPts_final = SachMathUtil.rotate2D(global_ori)
////			SachMathUtil.rotate2D(global_ori, objCtrlPts_rotated);
			
//			spline.addPoint((objCtrlPts[n][0]-xDiff)*size,(objCtrlPts[n][1]-yDiff)*size);
			
			//the May/31/2017 update
			spline.addPoint(objCtrlPts_spline[n][0],objCtrlPts_spline[n][1]);
		}
		//-- render spline: 
		spline.done();		// creates spline from control points

	}
	
	public boolean createObjFromSpec() {
		// **this method will create a multi-limb object from a LimbSpec**
		
		// JK 25 April 2016 added shouldUseSizeCues to have category based sizes so use the spec size here
		//AWC commented out July2017
//		size = spec.getSize();
						
		//-- unpack LimbSpecs and create limbs: 
		int numLimbs = limbs.size();
//		double [] bla = limbs.get(0).getXy();
//		System.out.println("[" + bla[0] + " " + bla[1] + "]");
		for (int n=0;n<numLimbs;n++) {
			LimbSpec l = limbs.get(n);
			
			if (n==0) {	// if first limb (could also check this by seeing that l.getNodeId == -1)

				firstLimb(l.getLength(),l.getWidth(),l.getWidth2(),l.getOri(),l.getXy(),l.getCurv(),l.isSmoother(),l.isSmoother2());
			} else {
				if (!addLimb(l.getNodeId(),l.getLength(),l.getWidth(),l.getOri(),l.getCurv(),l.isSmoother())) {
//					// if addLimb complains, set fail flag to send back (to createMorphObject) so it re-morphs:
//					flagReMorph = true;
//					// if not cantFail, return
//					if (!cantFail) return;
					
					if (!cantFail) return false;
				}
					
			}
		}
		return true;
	}
	
	void createBehObj() {
		// **this method will create a behavioral multi-limb object (using the stimCat value)**
		
		//-- choose a category: 
//		stimChooser(); // this makes pre-set multi-limb objects
		stimChooser_allCategories(); 
		
		cantFail = true;		// all beh stimuli should be pre-checked for violations anyways
		createObjFromSpec();	// creates obj from limbs
	}
	
	void createRandObj() {
		// **use this to create randomly generated spline objects for the GA**
		
		//-- generate multi-limb object

		//- generate random specs:	
			// biased number of limbs:     // 0, 1, 2, 3, 4, 5, 6, 7, 8	
		double[] numLimbProps = new double[]{ 0, 5,10,15,15,15,15, 0, 0 }; 			// proportions for each # of limbs
		BiasRandom br = new BiasRandom(numLimbProps);
		int limbsToMake = br.selectEvent();
//		int limbsToMake = SachMathUtil.randRange(lim_maxNumLimbs,lim_minNumLimbs);	// rand # limbs between minNumLimbs and maxNumLimbs

		double l = SachMathUtil.randRange(lim_maxLength,lim_minLength);				// random lengths
		double w = SachMathUtil.randRange(lim_maxWidth,lim_minWidth);				// random widths
		double w2 = SachMathUtil.randRange(lim_maxWidth,lim_minWidth);				// random second width
		double ori = SachMathUtil.randRange(lim_maxOri,lim_minOri);					// random oris
		double[] curv = SachMathUtil.randRange(lim_maxCurv,lim_minCurv,8);			// random curvature
		boolean isSmoother = SachMathUtil.randBoolean(lim_propSmoothNodes);			// randomly choose where to represent each node with 4 or 3 control points (less = more smooth) [make isSmoother (3vs4 ctrl pt node) contingent on how many limbs will be created?]
		boolean isSmoother2 = SachMathUtil.randBoolean(lim_propSmoothNodes);
		
		//- first limb:

		firstLimb(l,w,w2,ori,new double[]{xPos,yPos},curv,isSmoother,isSmoother2);	// create first limb
		limbs.add(new LimbSpec(-1,l,w,w2,ori,curv,isSmoother,isSmoother2));			// add limbSpec

		//- additional limbs:
		for (int n=1;n<limbsToMake;n++) {
			int nodeID = nodeIdChooser();											// random node 			
			//int nodeID = SachMathUtil.randRange(numNodes-1,0);					// random node 
			if (!addRandomLimb(nodeID)) n--;										// try limb add, if it fails: repeat
		}
		spec.setAllLimbSpecs(limbs);												// add limbs to spec (for later saving this info in db)
	}
	
	int nodeIdChooser() {
		int nodeId = 0;
		int localNumLimbs = limbs.size();											// using limbs (not numNodes) so we can use the same function for morphs
		
		if (localNumLimbs>1 && SachMathUtil.randBoolean(lim_branchingBias)) {
			// find which nodes are not end-nodes: each cxdNode is a non-endNode
			int[] nodesCxd = new int[localNumLimbs-1];
			for (int n=0;n<localNumLimbs-1;n++) { 									// for each limb, find the nodeId (that they connect to), skip first limb
				nodesCxd[n] = limbs.get(n+1).getNodeId();
			}
			nodesCxd = SachMathUtil.unique(nodesCxd);								// remove any duplicates
			nodeId = nodesCxd[SachMathUtil.randRange(nodesCxd.length-1,0)];			// choose among non-endnodes
		} else {
			nodeId = SachMathUtil.randRange(numNodes-1,0); 							// just choose among all nodes
		}
		
		return nodeId;
	}
	
	public void createMorphObj() {
		createMorphObj(-1);
	}
	
	public void createMorphObj(int mType) {
		// **use this to morph parent objects for the GA**
		// this only changes the list of limb specs (in most cases), the stim is later reconstructed from this spec
		
		// choose which dimension to morph:
			// --add limb, subtract limb, add limb between nodes
			// --change a limb's: length, width, ori, curv, node smoothness, global ori, size, canonical
			//   (node smoothness- changes number of node control points; canonical- changes all limbs and end points to unifor, widths)

		// --shared info needed:
		boolean flagReMorph = false;			// redo morphing
		boolean flagDoDiffMorph = false;		// perform a different morph type
		
		boolean runCreateObjFromSpec = true;	// for most morphs we want to do this
		int localNumLimbs = limbs.size();
		
		// --make a deep copy of limbs (in case we need to revert changes)
		List<LimbSpec> old_limbs = new ArrayList<LimbSpec>();
		for (LimbSpec limb : limbs) {
			old_limbs.add(new LimbSpec(limb));
		}

		// --find which nodes are end-nodes: (using limbs, as nodes doesn't exist yet)
		int[] nodesCxd = new int[localNumLimbs];
		// for each limb, find the nodeId (that they connect to)
		for (int n=0;n<localNumLimbs;n++) { 
			nodesCxd[n] = limbs.get(n).getNodeId();
		}
		// find nodes that aren't cxd nodes = end nodes
		List<Integer> endNodes = new ArrayList<Integer>();
		boolean match;
		for (int n=0;n<localNumLimbs+1;n++) {	
			match = false;
			for (int m=0;m<nodesCxd.length;m++) {
				if (n == nodesCxd[m]) {
					match = true;
					break;
				}
			}
			if (!match) endNodes.add(n);
		}

		MorphTypes morphType;
		if (mType==-1){
			// --choose which type of morph to apply:
			morphType = chooseMorphType();
		}
		else{
			morphType = MorphTypes.values()[mType];	
			System.out.print(morphType);
		}

		// --perform chosen morph type:
		int morphCounter = 0;
		
		do {	// while reMorphFlag = true; (this will try to apply the same type of morph if it fails)
			
			flagReMorph = false;		// re-set flags
			
			if (flagDoDiffMorph) {
				morphType = chooseMorphType();
				flagDoDiffMorph = false;
			}
			
			// which limb to morph (for length, width, ori, etc):
			int limbToMorph = SachMathUtil.randRange(localNumLimbs-1,0);

			// apply chosen morph type:
			switch (morphType) {

			case ADD:	// __add limb__
				// try this: cycle here over nodeID and doSplit
				runCreateObjFromSpec = false;	// (already created here, don't want to create again later)

				boolean doSplit = false; //SachMathUtil.randBoolean();			// 50% of time a limb is added between nodes, not at the node
				int nodeID = nodeIdChooser();

				System.out.println("morph: add to node("+nodeID+") "+"split="+SachIOUtil.shortBool(doSplit));

				if (!addRandLimbWithSplit(nodeID,doSplit)) {	// if add limb fails: reset limbs, set flag
					// undo limb changes:
					limbs = new ArrayList<LimbSpec>();
					for (LimbSpec limb : old_limbs) {
						limbs.add(new LimbSpec(limb));
					}
					flagReMorph = true;
					break;
				}
				localNumLimbs++;	// update num limbs
				break;

			case SUBTRACT:	// __subtract limb__ -- can only remove an end node!
				// pick end-node to remove:
				int idx = SachMathUtil.randRange(endNodes.size()-1,0);
				int nodeToRemove = endNodes.get(idx);
				System.out.println("morph: remove node("+nodeToRemove+")");
				removeEndLimb(nodeToRemove);
				localNumLimbs--;
				break;

			case LENGTH:	// __length__
				System.out.println("morph: length of limb("+limbToMorph+")");
				double l0 = limbs.get(limbToMorph).getLength();
				double l = SachMathUtil.randUshaped(lim_minLength, lim_maxLength, l0);	// selects length by biasing away from old value
				//double l = SachMathUtil.randRange(lim_maxLength,lim_minLength);			// random length
				limbs.get(limbToMorph).setLength(l);
				break;

			case WIDTH:	// __width__
				System.out.println("morph: width of limb("+limbToMorph+")");
				if (limbToMorph == 0 && SachMathUtil.randRange(1,0) == 1) {	// first limb, randomly choose node
					double w0 = limbs.get(limbToMorph).getWidth2();						
					double w = SachMathUtil.randUshaped(lim_minWidth,lim_maxWidth,w0);	// random width by biasing away from old value
					limbs.get(limbToMorph).setWidth2(w);
				} else {
					double w0 = limbs.get(limbToMorph).getWidth();
					double w = SachMathUtil.randUshaped(lim_minWidth,lim_maxWidth,w0);	// random width by biasing away from old value
					limbs.get(limbToMorph).setWidth(w);
				}
				break;

			case ORI:	// __ori__
				System.out.println("morph: ori of limb("+limbToMorph+")");
				double ori0 = limbs.get(limbToMorph).getOri();
				double ori = SachMathUtil.normAngle(SachMathUtil.randBoundedGauss(360/5, ori0+180, ori0, ori0+360));	// random ori by biasing away from other oris
				// double ori = SachMathUtil.randRange(359,0);				// random ori
				limbs.get(limbToMorph).setOri(ori);				
				break;

			case CURV:	// __curv__ (only for end-nodes)
				// pick end-node to morph:
				int idx2 = SachMathUtil.randRange(endNodes.size()-1,0);
				int nodeToMorph = endNodes.get(idx2);
				System.out.println("morph: curv of node("+nodeToMorph+")");

				double[] curv = SachMathUtil.randRange(lim_maxCurv,lim_minCurv,4);	// random curvature
				//double curv = SachMathUtil.randRange(cMax,cMin);		// random curvature -- morph one at a time?

				if (nodeToMorph < 2) {	// node in 1st limb
					double[] oldCurv = limbs.get(0).getCurv().clone();
					if (nodeToMorph == 0) {	// first node
						for (int n=0;n<curv.length;n++) {
							oldCurv[n] = curv[n];
						}
					} else {	// second node
						for (int n=0;n<curv.length;n++) {
							oldCurv[n+4] = curv[n];
						}
					}
					limbs.get(0).setCurv(oldCurv);
				} else {				// other limb
					double[] curv0 = limbs.get(nodeToMorph-1).getCurv();
					curv = SachMathUtil.randUshaped(lim_minCurv, lim_maxCurv, curv0, curv0.length); 
					limbs.get(nodeToMorph-1).setCurv(curv);
				}
				break;

			case SMOOTH: // __isSmoother__ (number of 3 vs 4 ctrl pt nodes)
				System.out.println("morph: smoothness of limb("+limbToMorph+")");
				// options: change 1 node, change all nodes
				if (limbToMorph == 0 && SachMathUtil.randRange(1,0) == 1) {	// first limb, 2nd node
					boolean isSm0 = limbs.get(limbToMorph).isSmoother2();
					limbs.get(limbToMorph).setIsSmoother2(!isSm0);
				} else {													// any other node
					boolean isSm0 = limbs.get(limbToMorph).isSmoother();
					limbs.get(limbToMorph).setIsSmoother(!isSm0);
				}
				break;

			case GLOBAL_ORI: // __global ori__
				global_ori = SachMathUtil.normAngle(SachMathUtil.randRange(global_ori+25, global_ori-25));
				System.out.println("morph: global ori (" + global_ori + ")");
				break;

			case SIZE: // __size__ (global)
				size = SachMathUtil.randRange(lim_maxSize, lim_minSize);
				System.out.println("morph: global size (" + size + ")");
				break;
				
			case CANONICAL: // __canonical__ (switches to a canonical medial axis view where surface curvature is minimized)
				// this will need to set all widths to the same value and also all endNode points 
				// so, for each limb, set width to w? and set curv values according to w
				System.out.println("morph: make canonical");
				
				canonicalize();
				
				break;
				

			}	
			
			// --create obj from spec:
			if (runCreateObjFromSpec) {
				if (!createObjFromSpec()) {	// if this fails: a flag is set to re-do morph
					flagReMorph = true;
				}
			}
			
			morphCounter++;
			if (morphCounter>20) {	// if a morph type fails 20 times, choose a diff morph type
				if (mType==-1){
					flagDoDiffMorph = true;
					morphCounter = 0;
				}
				else {
					System.out.print("failed");
					return;
				}
			}
			
			// --set new spec or repeat if it fails
			if(flagReMorph) {
				// limbs have been changed, change back:
				limbs = new ArrayList<LimbSpec>();
				for (LimbSpec limb : old_limbs) {
					limbs.add(new LimbSpec(limb));
				}
				localNumLimbs = limbs.size();
			} else {
				updateSpecParams();		// add limbs to spec (for later saving this info in db)
			}
						
		} while (flagReMorph);
	}
	
	public void canonicalize(){
		double w = 1;
		for (int nLimb=0;nLimb<limbs.size();nLimb++) {
			LimbSpec spec = limbs.get(nLimb);
			spec.setWidth(w);						// reset limb width
			spec.setIsSmoother(false);				// set limb to NOT isSmoother
			spec.setCurv(new double[]{1,1,1,1});	// reset curv values
			
			if (nLimb==0) {							// for first limb, also reset width2 & isSmoother2& curv2
				spec.setWidth2(w);
				spec.setIsSmoother2(false);	
				spec.setCurv(new double[]{1,1,1,1,1,1,1,1});
			}
		}
	}
	
	public void updateSpecParams(){
		spec.setAllLimbSpecs(limbs);
		spec.setSize(size);
		spec.setGlobalOri(global_ori);
		this.setSpec_dontCreate(spec.toXml());
	}
	
	
	//
	// JK 18 Aug 2016
	//	Change the length of limbToSimpleMorph.  
	//
	void createSimpleMorphObj() {	
		int morphNode1;
		int limbToSimpleMorph = -1;
		double morphLengthGain = this.getSpec().getMorphLengthGain();
		@SuppressWarnings("unused")
		double bias = this.getSpec().getMorphBias();
		double gainExp = Math.pow(2.0, morphLengthGain);
		
			// AWC Aug27
//			TreeSet stupid = new TreeSet<Integer>(manifoldNodes);
//			System.out.println("manifoldNodes: " + (Integer)stupid.first() + " " + (Integer)stupid.last());
//			int limbToMorph;
//			if ((Integer)stupid.first()>(Integer)stupid.last()){
//				limbToMorph = (Integer)stupid.first() - 1;
//			}
//			else{
//				limbToMorph = (Integer)stupid.last() - 1;	
//			}

		// iterate through limbsToSimpleMorph
		for(Integer limbNum : limbsToSimpleMorph){
			limbToSimpleMorph = limbNum;
			
			// validate limbToMorph
			if(limbToSimpleMorph >= limbs.size()){
//				System.out.println("createSimpleMorphObj() : limbToSimpleMorph = " + limbToSimpleMorph + ", limbs.size() + " + limbs.size());
				limbToSimpleMorph = limbs.size() - 1;
			}
								
			// do the length morph
			// double l = 0.0;
			double l0 = limbs.get(limbToSimpleMorph).getLength();			
			// use the morphLengthGain to set limits
			double lMin = l0 / gainExp;
			double lMax = l0 * gainExp;
			
			if (l0>6) lMin=6;
			
			if(lMin == lMax){
				lMin -= 0.001;
			}
			
			if(lMax > universalLengthMax){
				lMax = universalLengthMax;
			}			
			
			// is this limb used to define the manifold?
			morphNode1 = limbs.get(limbToSimpleMorph).getNodeId();
			if (morphNode1 == -1){
				morphNode1 = 0;
			}
//			System.out.println("morphNode1 = " + morphNode1);
			if(manifoldNodes.contains(morphNode1) && manifoldNodes.contains(limbToSimpleMorph + 1)){
				// AWC Aug 29
				if (l0 < manifoldMorphMin){
					l0 = manifoldMorphMin;
				}
				if (l0 > manifoldMorphMax){
					l0 = manifoldMorphMax;
				}
				// l = SachMathUtil.randUshapedPoly(manifoldMorphMin, manifoldMorphMax, l0, bias);
//				System.out.println("BsplineObject.createSimpleMorphObj() : morphing manifold nodes: " + manifoldNodes + 
//							", between [" + manifoldMorphMin + ", " + manifoldMorphMax + "], l = " + l + ", bias = " + bias); 
			} else {
				// AWC Aug 29
				if (l0 < lMin){
					l0 = lMin;
				}
				if (l0 > lMax){
					l0 = lMax;
				}
				// l = SachMathUtil.randUshapedPoly(lMin, lMax, l0, bias);	// selects length by biasing away from old value
			}
						
//			limbs.get(limbToSimpleMorph).setLength(l);
			limbs.get(limbToSimpleMorph).setLength(l0);
			
//				System.out.println("createSimpleMorphObj() : morphing limb " + limbToSimpleMorph + " : current length = " + l0 );
//				System.out.println("createSimpleMorphObj() : nodeId = " + limbs.get(limbToSimpleMorph).getNodeId() + ", new length = " + l);
		}
	
		
		createObjFromSpec();
				
		spec.setAllLimbSpecs(limbs);
		spec.setSize(size);
		spec.setGlobalOri(global_ori);

	} // end JK createSimpleMorphObj();
	
	
	
	
	
	//
	// JK 6 Oct 2016
	//	Morphing length and/or width
	//
	void createLengthWidthMorphObj() {	
		double dim0 = -1.0;
		double dMax = -1.0;
		double dMin = -1.0;
		double newDim = -1.0;
		double morphLengthGain = this.getSpec().getMorphLengthGain();
		double morphWidthGain = this.getSpec().getMorphWidthGain();
		double bias = this.getSpec().getMorphBias();
		double lGainExp = Math.pow(2.0, morphLengthGain);
		double wGainExp = Math.pow(2.0, morphWidthGain);
		int numLimbs;
		int numLimbsTotal = this.getLimbs().size();
		List<Integer> limbsToMorph = new ArrayList<Integer>();

	
//System.out.println("createLengthWidthMorphObj: " + numLimbsTotal + " total limbs ");
//System.out.format("\t%s : %4s %6s %6s\n", "Dim", "limb", "dim0", "dim1");
		
		// first length, then width dimension
		for(int dim = 0; dim < 2; dim++){
			
			limbsToMorph.clear();
			// randomly select how  limbs to morph between 1 and numLimbs
			numLimbs = SachMathUtil.randRange(this.getLimbs().size(), 2);
			// 
			//if(numLimbs == 1) numLimbs = 2;
			
			List<Integer> indices = SachMathUtil.randUniqueRange(this.getLimbs().size() - 1, 0, numLimbs);
	
			for(int i = 0; i < numLimbs; i++){
				limbsToMorph.add(indices.get(i));
//				System.out.format("%d  " , morphList.get(i));
			}
			
			// iterate through limbsToSimpleMorph
			for(Integer limbNum : limbsToMorph){
								
				// validate limbToMorph
				if(limbNum >= limbs.size()){
					System.out.println("createLengthWidthMorphObj() : limbNum = " + limbNum + ", limbs.size() + " + limbs.size());
					limbNum = limbs.size() - 1;
				}
									
				// do the length morph
				if(morphLengthGain > 0 && dim == 0){
					dim0 = limbs.get(limbNum).getLength();	
					dMin = dim0 / lGainExp;
					dMax = dim0 * lGainExp;
					
					if(dMin == dMax){
						dMin -= 0.001;
					}
					
					if(dMax > universalLengthMax){
						dMax = universalLengthMax;
					}			
					
					// AWC Aug 29
					if (dim0 < dMin){
						dim0 = dMin;
					}
					if (dim0 > dMax){
						dim0 = dMax;
					}
					
					newDim = SachMathUtil.randUshapedPoly(dMin, dMax, dim0, bias);	// selects length by biasing away from old value
					limbs.get(limbNum).setLength(newDim);
										
					System.out.format("\tlength : %4d %6.4f %6.4f\n", limbNum, dim0, newDim);
					
				} else if(morphWidthGain > 0 && dim == 1) {
					// do width morph
					dim0 = limbs.get(limbNum).getWidth();	
					dMin = dim0 / wGainExp;
					dMax = dim0 * wGainExp;
					
					if(dMin == dMax){
						dMin -= 0.001;
					}
					
					if(dMax > universalWidthMax){
						dMax = universalWidthMax;
					}			
					
					// AWC Aug 29
					if (dim0 < dMin){
						dim0 = dMin;
					}
					if (dim0 > dMax){
						dim0 = dMax;
					}
					newDim = SachMathUtil.randUshapedPoly(dMin, dMax, dim0, bias);	// selects length by biasing away from old value
					limbs.get(limbNum).setWidth(newDim);
					System.out.format("\twidth : %4d %6.4f %6.4f\n", limbNum, dim0, newDim);
				}
				
	//				System.out.println("createSimpleMorphObj() : morphing limb " + limbToSimpleMorph + " : current length = " + l0 );
	//				System.out.println("createSimpleMorphObj() : nodeId = " + limbs.get(limbToSimpleMorph).getNodeId() + ", new length = " + l);
			}
		} // for each dimension
		
		createObjFromSpec();
				
		spec.setAllLimbSpecs(limbs);
		spec.setSize(size);
		spec.setGlobalOri(global_ori);

	} // end JK createLengthWidthMorphObj();
	
	private MorphTypes chooseMorphType() {
		// --choose which type of morph to apply:
		int localNumLimbs = limbs.size();

		double propAddSub, propAdd;								// proportions that are add/subtract morphs	
		MorphTypes morphType = MorphTypes.ADD;					// default: add limb

		if (localNumLimbs<=1) {									// if only 1 limb, very high probability of add limb
			propAddSub = 0.90;
			propAdd = 1.0;
		} else if (localNumLimbs == 2) {						// if only 2 limbs, pretty high probability of add limb
			propAddSub = 0.70;
			propAdd = 0.80;
		} else if (localNumLimbs >= lim_maxNumLimbs) {			// if max num limbs, do not add 
			propAddSub = 0.50;
			propAdd = 0;
		} else {												// otherwise, equal probabilities
			propAddSub = 0.50;
			propAdd = 0.50;
		}

		if (SachMathUtil.randBoolean(propAddSub)) {	// if add/subtract limb
			morphType = SachMathUtil.randBoolean(propAdd) ? MorphTypes.ADD : MorphTypes.SUBTRACT;
		} else {									// do one of the other morphs
			// limb morph types & proportions:  (l,w,ori,curv,smooth,global ori,size,canonical)
			BiasRandom br = new BiasRandom( new double[]{ 1,1,1,1,1,2,1,1 });
			//					BiasRandom br = new BiasRandom( new double[]{ 0,0,0,0,0,1,1,1 });	// testing
			morphType = MorphTypes.values()[br.selectEvent()+2];
		}

		return morphType;
	}
	
	
	//---- limb constructors ----
	
	void firstLimb(double l, double w, double ori, double[] ctr) {
//		System.out.println(1);
		firstLimbCreator(l,w,w,ori,ctr,new double[]{1,1,1,1,1,1,1,1},false,false);
	}
	
	void firstLimb(double l, double w, double ori) {
//		System.out.println(2);
		firstLimbCreator(l,w,w,ori,new double[]{0,0},new double[]{1,1,1,1,1,1,1,1},false,false);
	}
	
	void firstLimb(double l, double w1,double w2,double ori,double[] ctr,double[] curv) {
//		System.out.println(3);
		firstLimbCreator(l,w1,w2,ori,ctr,curv,false,false);
	}
	
	void firstLimb(double l, double w1,double w2,double ori,double[] ctr,double[] curv,boolean isSmoother) {
//		System.out.println(4);
		firstLimbCreator(l,w1,w2,ori,ctr,curv,isSmoother,isSmoother);
	}
	
	public void firstLimb(double l, double w1,double w2,double ori,double[] ctr,double[] curv,boolean isSmoother,boolean isSmoother2) {
//      THIS IS THE (ONLY) ONE CALLED FROM createObjFromSpec()  , ctr is from l.getXY()
//		System.out.println(5);
		firstLimbCreator(l,w1,w2,ori,ctr,curv,isSmoother,isSmoother2);
	}
	
	void firstLimb(double l, double w1,double w2,double ori,double[] curv) {
//		System.out.println(6);
		firstLimbCreator(l,w1,w2,ori,new double[]{0,0},curv,false,false);
	}
	
	void firstLimbCreator(double l,double w1,double w2,double ori,double[] ctr,double[] curv,boolean isSmoother,boolean isSmoother2) {
		// create two nodes and put them together
		
//		// check for previous nodes:
//		if (numNodes != 0) {
//			System.out.println(" **WARNING: Nodes already exist. Clearing previous nodes!");
//		}
		
		// re-set nodes: (objCtrlPts and ctrlPtNodeIds re-set below)
		numNodes = 0;
		nodes = new Node[maxNumNodes];	
		
		// ctr will be center of first node
		ori = rescaleOri(ori);											// rescale to between 0 and 360
		double ori180 = rescaleOri(ori+180);
		createNode(w1,ori180,ctr,curv[0],curv[1],curv[2],curv[3],isSmoother);		// first node [0]
//		System.out.println("[" + ctr[0] + " " + ctr[1] + "]");
		
		// calculate center of second node and create node
		double[] newCtr = SachMathUtil.rotateAxis2D(ori,ctr,new double[]{ctr[0]+l,ctr[1]});
		createNode(w2,ori,newCtr,curv[4],curv[5],curv[6],curv[7],isSmoother2);		// second node [1]
		
		// init objCtrlPts:
		objCtrlPts = SachMathUtil.mergeArrays(nodes[0].ctrlPts,nodes[1].ctrlPts);
		
//		System.out.println("[...");
//		for (int n = 0; n< nodes[0].ctrlPts.length; n++) {
//			System.out.println(nodes[0].ctrlPts[n][0] + " " + nodes[0].ctrlPts[n][1] + ";...");
//		}
//		System.out.println("]");
		
		if (!isSmoother&!isSmoother2) {
			ctrlPtNodeIDs = new int[] {0,0,0,0,1,1,1,1};
		} else if (isSmoother&!isSmoother2) { 
			ctrlPtNodeIDs = new int[] {0,0,0,1,1,1,1};
		} else if (!isSmoother&isSmoother2) { 
			ctrlPtNodeIDs = new int[] {0,0,0,0,1,1,1};
		} else {
			ctrlPtNodeIDs = new int[] {0,0,0,1,1,1};
		}
		
		// set node properties:
		nodes[0].addNode(1,l,w1,ori);
		nodes[1].addNode(0,l,w2,ori180);
		
		if (printStimSpecs) {
			// print out limb specs:
			System.out.println("limb#0:"+"l,w1,w2,ori,curv--("+-1+","+SachStringUtil.format(l,2)+","+SachStringUtil.format(w1,2) 
					+","+SachStringUtil.format(w2,2)+","+SachStringUtil.format(ori,0)+",new double[]{"+SachStringUtil.format(curv,2)+"},"
					+SachIOUtil.shortBool(isSmoother)+","+SachIOUtil.shortBool(isSmoother2)+")");
		}
	}

	void createNode(double w,double ori,double[] ctr) {
		nodes[numNodes] = new Node(numNodes,w,ori,ctr);
		numNodes++;
	}
	
	void createNode(double w,double ori,double[] ctr,double curv,double elong) {
		nodes[numNodes] = new Node(numNodes,w,ori,ctr,curv,elong);
		numNodes++;
	}
	
	void createNode(double w,double ori,double[] ctr,double curvB,double curvC,double elongB,double elongC) {
		nodes[numNodes] = new Node(numNodes,w,ori,ctr,curvB,curvC,elongB,elongC);
		numNodes++;
	}
	
	void createNode(double w,double ori,double[] ctr,double curvB,double curvC,double elongB,double elongC,boolean isSmoother) {
		nodes[numNodes] = new Node(numNodes,w,ori,ctr,curvB,curvC,elongB,elongC,isSmoother);
		numNodes++;
	}
	
	void createNode(double w,double ori,double[] ctr,double curv,double elong,boolean isSmoother) {
		nodes[numNodes] = new Node(numNodes,w,ori,ctr,curv,elong,isSmoother);
		numNodes++;
	}
	
	void createNode(double wx,double wy,double ori,double[] ctr) {
		nodes[numNodes] = new Node(numNodes,wx,wy,ori,ctr);
		numNodes++;
	}
	
	void createNode(Node n) {
		nodes[numNodes] = new Node(n);
		numNodes++;
	}
	
	void removeLastNode() {
		numNodes--;
		nodes[numNodes] = null;
	}
	
	boolean addLimb(int nodeID, double l, double w, double ori) { 
		return addLimbCreator(nodeID,l,w,ori,new double[]{1,1,1,1},false);
	}
	
	boolean addLimb(int nodeID,double l,double w,double ori,double[] curv) { 
		return addLimbCreator(nodeID,l,w,ori,curv,false);
	}
	
	boolean addLimb(int nodeID, double l, double w, double ori,boolean isSmoother) { 
		return addLimbCreator(nodeID,l,w,ori,new double[]{1,1,1,1},isSmoother);
	}
	
	public boolean addLimb(int nodeID,double l,double w,double ori,double[] curv,boolean isSmoother) { 
		return addLimbCreator(nodeID,l,w,ori,curv,isSmoother);
	}
		
	boolean addLimbCreator(int nodeID, double l, double w, double ori,double[] curv,boolean isSmoother) { 
		// === add new node to current multi-limb object ===		
		// nodeID = node to which new node will be attached, l = length, w = width, ori of new node
		// curvB & curvC: change node pointiness/bluntness, elongB & elongC: change node elongation
		// isSmoother: flag to use 3 control points per end node, instead of 4; when adding nodes the # of control pts = numCxns+1
		// output: returns true if a limb was successfully added, false otherwise
		
		// check to make sure at least the first limb exists:
		if (limbs.size() < 1) {
			if (printStimSpecs) System.err.println("ERROR: Cannot add limb when no first limb.");
		}
		
		// re-set violation flag:
		flagLimbAddViolation = false;
		
//		// (for debugging):
//		System.out.println(" nodeId="+nodeID+", numNodes="+numNodes+", numCtrlPts="+objCtrlPts.length);
		
		// --create new node:
		ori = rescaleOri(ori);																	// rescale to between 0 and 360
		double[] ctr = nodes[nodeID].xy;														// center of source node
		double w0 = nodes[nodeID].width;														// source node width
		double l0 = nodes[nodeID].lengths[0];													// source node length
		double[] newCtr = SachMathUtil.rotateAxis2D(ori,ctr,new double[]{ctr[0]+l,ctr[1]});		// center of new node
		int newNodeID = numNodes;
		int[] sourceNode_CtrlPtIdxs = SachMathUtil.findAllValues(ctrlPtNodeIDs,nodeID);			// indices of the source node's ctrl pts (so they can be manipulated)
		boolean isSourceSmoother = nodes[nodeID].isSmoother();
			// *** a node will be initialized with either 3 or 4 control points
			// when it's 3, any connection made to this node will always have only one control point per crotch
			// for source nodes that alraedy have connections, the number of control points = numCxns +1
		double[][] objCtrlPts_saved = SachMathUtil.deepCopyMatrix(objCtrlPts);	// copy control points in case I need to revert changes later!
		int[] ctrlPtNodeIDs_saved = ctrlPtNodeIDs.clone();
		
		// -- first, check if new limb will overlap old limbs:
		Node testNewNode = new Node(999,w,ori,newCtr,curv[0],curv[1],curv[2],curv[3]);
		boolean flagLimbAddViolation1 = checkIfPointsInPoly(testNewNode.getCtrlPts(),objCtrlPts);
		boolean flagLimbAddViolation2 = checkIfPointsInPoly(objCtrlPts,testNewNode.getCtrlPts());
		boolean flagLimbAddViolation3 = SachMathUtil.doesLineCrossPolygon(objCtrlPts,ctr,newCtr);	// does new limb length cross old outline more than once?
		//boolean flagLimbAddViolation4 = SachMathUtil.doesLineCrossPolygon(objCtrlPts,ctr,newCtr);	// does new limb cross old skeleton? need to get skeleton
		flagLimbAddViolation = flagLimbAddViolation || flagLimbAddViolation1 || flagLimbAddViolation2 || flagLimbAddViolation3;
		if (flagLimbAddViolation && !cantFail) {
			if (printStimSpecs) System.out.println(" **WARNING: Attempted new limb overlaps old limb! Limb not added. (checkIfPointsInPoly)");
			if (printStimSpecs) System.out.println("   FAILED--limb#"+(newNodeID-1)+":toNode,l,w,ori,curv--("+nodeID+","+SachStringUtil.format(l,2)+","+SachStringUtil.format(w,2) 
					+","+SachStringUtil.format(ori,0)+",new double[]{"+SachStringUtil.format(curv,2)+"},"+SachIOUtil.shortBool(isSmoother)+")");
			return !flagLimbAddViolation;	// return "limb add success = false"
		}		
		
		// -- how to connect new node:
		int addIdx = 1;																			// default position to add new node
		int numCxns = nodes[nodeID].numCxdNodes; 
		double[][] newPtLocs = new double[4][2];
		
		if (numCxns == 1) {	// number of connected nodes already attached to nodeID = 1 (an end node)
			
			// if source (end) node has less than 4 pts, use only 1 pt per crotch:
			//if (numSourcePts < 4) use2ptsPerCrotch = false; 
			double ori0 = nodes[nodeID].oris[0];									// ori from source node to its single cxd node
			
				// --position pts at middle of angles:		
//			newPtLocs = SachMathUtil.mergeArrays(
//					makeSmoothJointPt(ori0,w0,l0,ori,w,l,ctr),
//					makeSmoothJointPt(ori,w,l,ori0,w0,l0,ctr) );
			// *** this version just uses the source width! ***
			double[][] newPts_cw  = makeSmoothJointPt(ori0,w0,l0,ori,w0,l,ctr);	// output spread pts and center pt
			double[][] newPts_ccw = makeSmoothJointPt(ori,w0,l,ori0,w0,l0,ctr);
			
			if (!isSourceSmoother) {	// use first two point (the spread points)
				newPtLocs = SachMathUtil.mergeArrays(
						Arrays.copyOfRange(newPts_cw, 0, 2), Arrays.copyOfRange(newPts_ccw, 0, 2) );
			} else {	// use only the last point (the center point -- one per crotch)
				newPtLocs = SachMathUtil.mergeArrays(
						Arrays.copyOfRange(newPts_cw, 2, 3), Arrays.copyOfRange(newPts_ccw, 2, 3) );
			}
			//Arrays.copyOfRange(newPts_cw, 0, 1); // use use2ptsPerCrotch to select which pts to add/modify
			// *** even if we have 2,3, or 4 cntrlpts, use numSourcePts to deftly choose whether to move, add, or delete 
			// current cntrlPts. HERE
			
			// *** check if makeSmoothJointPt had a violation ***
			if (flagLimbAddViolation && !cantFail) {
				if (printStimSpecs) System.out.println(" **WARNING: Attempted new limb overlaps old limb! Limb not added. (makeSmoothJointPt)");
				if (printStimSpecs) System.out.println("   FAILED--limb#"+(newNodeID-1)+":toNode,l,w,ori,curv--("+nodeID+","+SachStringUtil.format(l,2)+","+SachStringUtil.format(w,2) 
						+","+SachStringUtil.format(ori,0)+",new double[]{"+SachStringUtil.format(curv,2)+"},"+SachIOUtil.shortBool(isSmoother)+")");
				return false;	// return "limb add success = false" -- new node violation, node not added
			}
			
				// now move pts to these locations:
			if (!isSourceSmoother) { 
				for (int n=0;n<4;n++) {
					moveCtrlPt(sourceNode_CtrlPtIdxs[n],newPtLocs[n]);
				}
			} else {	// if using 1 pt per crotch!
				addIdx = 0;	// change add index!
				// remove a control point from the source node:
				removeCtrlPt(sourceNode_CtrlPtIdxs[2]);
				// move remaining ctrl pts to new locations:
				for (int n=0;n<2;n++) {
					moveCtrlPt(sourceNode_CtrlPtIdxs[n],newPtLocs[n]);
				}
			}
					
		} else if (numCxns == 2 | numCxns == 3) {
			
			// --add/move pts
				// find oris of other cxd nodes: (oris range from 0 to 359)
			double[] oris    = Arrays.copyOf(nodes[nodeID].oris,numCxns);
			//double[] widths  = Arrays.copyOf(nodes[nodeID].widths,numCxns);
			double[] lengths = Arrays.copyOf(nodes[nodeID].lengths,numCxns);

			if (numCxns==3)
				sort3Limbs(oris,lengths);
			
				// find flanking node orientations (and find flanking node widths and lengths)	
			int[] lowHighIdxs = findFlankingNodes(ori,oris); 
			double lowOri  = oris[lowHighIdxs[0]];
			double highOri = oris[lowHighIdxs[1]];		
			//double lowW  = widths[lowHighIdxs[0]];
			//double highW = widths[lowHighIdxs[1]];
			double lowL  = lengths[lowHighIdxs[0]];
			double highL = lengths[lowHighIdxs[1]];
			
				// find add index:
			if (!isSourceSmoother) {
				addIdx = SachMathUtil.findFirst(oris, highOri)*2+1;
			} else {
				addIdx = SachMathUtil.findFirst(oris, highOri);
			}
//			newPtLocs = SachMathUtil.mergeArrays(
//					makeSmoothJointPt(highOri,highW,highL,   ori,w ,l ,ctr),
//					makeSmoothJointPt(    ori,w ,l ,lowOri,lowW,lowL,ctr) );
//			newPtLocs = SachMathUtil.mergeArrays(		// this version just uses the source width! ***
//					makeSmoothJointPt(highOri,w0,highL,   ori,w0,l ,ctr),
//					makeSmoothJointPt(    ori,w0,l ,lowOri,w0,lowL,ctr) );
			double[][] newPts_cw  = makeSmoothJointPt(highOri,w0,highL,ori,w0,l,ctr);	// output spread pts and center pt
			double[][] newPts_ccw = makeSmoothJointPt(ori,w0,l,lowOri,w0,lowL,ctr);
			
			if (!isSourceSmoother) {	// use first two point (the spread points)
				newPtLocs = SachMathUtil.mergeArrays(
						Arrays.copyOfRange(newPts_cw, 0, 2), Arrays.copyOfRange(newPts_ccw, 0, 2) );
			} else {	// use only the last point (the center point -- one per crotch)
				newPtLocs = SachMathUtil.mergeArrays(
						Arrays.copyOfRange(newPts_cw, 2, 3), Arrays.copyOfRange(newPts_ccw, 2, 3) );
			}
			
			
			// *** check if makeSmoothJointPt had a violation ***
			if (flagLimbAddViolation && !cantFail) {
				if (printStimSpecs) System.out.println(" **WARNING: Attempted new limb overlaps old limb! Limb not added. (makeSmoothJointPt)");
				if (printStimSpecs) System.out.println("   FAILED--limb#"+(newNodeID-1)+":toNode,l,w,ori,curv--("+nodeID+","+SachStringUtil.format(l,2)+","+SachStringUtil.format(w,2) 
						+","+SachStringUtil.format(ori,0)+",new double[]{"+SachStringUtil.format(curv,2)+"},"+SachIOUtil.shortBool(isSmoother)+")");
				return false;	// return "limb add success = false" -- new node violation, node not added
			}
			
				// now move pts to these locations:
			if (!isSourceSmoother) {
				moveCtrlPt(sourceNode_CtrlPtIdxs[addIdx-1],newPtLocs[0]);
				moveCtrlPt(sourceNode_CtrlPtIdxs[addIdx],  newPtLocs[1]);
				addCtrlPt( sourceNode_CtrlPtIdxs[addIdx],  newPtLocs[2],nodeID);
				addCtrlPt( sourceNode_CtrlPtIdxs[addIdx]+1,newPtLocs[3],nodeID);
			} else {
				moveCtrlPt(sourceNode_CtrlPtIdxs[addIdx],  newPtLocs[0]);
				addCtrlPt( sourceNode_CtrlPtIdxs[addIdx],  newPtLocs[1],nodeID);
			}
			
		} else {
			if (printStimSpecs) System.out.println("  **ERROR: Cannot have more than 4 connections per node! Limb not added.");
			return false;	// return "limb add success = false" -- new node violation, node not added
		}
		
//		// begin testing block
////		// remove any control points labeled "null" -- *** this still needs work when using more than 2 limbs/node, may actually not use this
//		int numNullsToShift = 0;
//		for (int n=3;n>-1;n--) {	// remove in reverse order
//			if (newPtLocs[n] == null) {
//				removeCtrlPt(sourceNode_CtrlPtIdxs[n]);
//				if (n<2) numNullsToShift++;		// only do shifts for first two pts
//			} 
//		}
//			// shift sourceNode_CtrlPtIdxs by the number of "null" points
//		for (int i=0;i<numNullsToShift;i++) {
//			for (int n=0;n<4;n++) {
//				sourceNode_CtrlPtIdxs[n]--;
//			}
//		}
//		// end testing block
		
		// create and add new node:
		int addPt = sourceNode_CtrlPtIdxs[addIdx];
		createNode(w,ori,newCtr,curv[0],curv[1],curv[2],curv[3],isSmoother);		
		
		// add new node to object:
		addNodeCtrlPts(addPt,newNodeID,isSmoother);			// attach ctrl pts of new node after addIdx ctrl pt of old node		
				
		// check to see if any edges of new shape overlap:
		if (SachMathUtil.doAnySegmentsCross(objCtrlPts)) flagLimbAddViolation = true;
		if (flagLimbAddViolation && !cantFail) {
			if (printStimSpecs) System.out.println(" **WARNING: Attempted new limb overlaps old limb! Limb not added. (doAnySegmentsCross)");
			if (printStimSpecs) System.out.println("   FAILED--limb#"+(newNodeID-1)+":toNode,l,w,ori,curv--("+nodeID+","+SachStringUtil.format(l,2)+","+SachStringUtil.format(w,2) 
					+","+SachStringUtil.format(ori,0)+",new double[]{"+SachStringUtil.format(curv,2)+"},"+SachIOUtil.shortBool(isSmoother)+")");
			
			// revert changes made above if re-doing:
			removeLastNode();						// remove the new node created above
			objCtrlPts = objCtrlPts_saved;			// revert changes to objCtrlPts
			ctrlPtNodeIDs = ctrlPtNodeIDs_saved;	// revert changes to ctrlPtNodeIDs

			return false;	// return "limb add success = false" -- new node violation, node not added
		}		
		
		// update node info:
		nodes[nodeID].addNode(newNodeID,l,w,ori);
		nodes[newNodeID].addNode(nodeID,l0,w0,rescaleOri(ori+180));
		
		// If needed: do additional smoothness check directly on spline points:
			// create vertices from control pts
			// check all vertices with boolean b = checkIfPointsInPoly(objCtrlPts,objCtrlPts)
			// BUT! need to change this? so that points on the line are ok?
			// make sure spline pts are located only near local control points and not distant control points
		
		if (printStimSpecs) {
			// print out limb specs:
			System.out.println("limb#"+(newNodeID-1)+":toNode,l,w,ori,curv--("+nodeID+","+SachStringUtil.format(l,2)+","+SachStringUtil.format(w,2) 
					+","+SachStringUtil.format(ori,0)+",new double[]{"+SachStringUtil.format(curv,2)+"},"+SachIOUtil.shortBool(isSmoother)+")");
		}
		
		if (flagLimbAddViolation && cantFail) {
			if (printStimSpecs) System.out.println(" **WARNING: Attempted new limb overlaps old limb! But limb was added anyway.");
		}
		return true;
	}
	
	void sort3Limbs(double[] oris, double[] lengths){
		oris[1] = oris[0] - (360 - ( (360 - (oris[0]-oris[1]) ) % 360) ) %360 ;
		oris[2] = oris[0] - (360 - ( (360 - (oris[0]-oris[2]) ) % 360) ) %360 ;
		
		if (oris[1] < oris[2]){
			double dummy = oris[1];
			oris[1] = oris[2];
			oris[2] = dummy;
			dummy = lengths[1];
			lengths[1] = lengths[2];
			lengths[2] = dummy;
		}
		for (int oo : new int[]{0 , 1 , 2})
			oris[oo] = (360- ( (360 - oris[oo]) % 360) ) % 360;
			
	}
	
	boolean addLimbCreator_old(int nodeID, double l, double w, double ori,double[] curv) { 
		// === add new node to current multi-limb object ===		
		// nodeID = node to which new node will be attached, l = length, w = width, ori of new node
		// curvB & curvC: change node pointiness/bluntness, elongB & elongC: change node elongation
		
		// re-set violation flag:
		flagLimbAddViolation = false;
		
//		// (for debugging):
//		System.out.println(" nodeId="+nodeID+", numNodes="+numNodes+", numCtrlPts="+objCtrlPts.length);
		
		// --create new node:
		ori = rescaleOri(ori);																	// rescale to between 0 and 360
		double[] ctr = nodes[nodeID].xy;														// center of source node
		double w0 = nodes[nodeID].width;														// source node width
		double l0 = nodes[nodeID].lengths[0];													// source node length
		double[] newCtr = SachMathUtil.rotateAxis2D(ori,ctr,new double[]{ctr[0]+l,ctr[1]});		// center of new node
		int newNodeID = numNodes;
		int[] sourceNode_CtrlPtIdxs = SachMathUtil.findAllValues(ctrlPtNodeIDs,nodeID);			// indices of the source node's ctrl pts (so they can be manipulated)

		// -- first, check if new limb will overlap old limbs:
		Node testNewNode = new Node(999,w,ori,newCtr,curv[0],curv[1],curv[2],curv[3]);
		boolean flagLimbAddViolation1 = checkIfPointsInPoly(testNewNode.getCtrlPts(),objCtrlPts);
		boolean flagLimbAddViolation2 = checkIfPointsInPoly(objCtrlPts,testNewNode.getCtrlPts());
		boolean flagLimbAddViolation3 = SachMathUtil.doesLineCrossPolygon(objCtrlPts,ctr,newCtr);	// does new limb length cross old outline more than once?
		//boolean flagLimbAddViolation4 = SachMathUtil.doesLineCrossPolygon(objCtrlPts,ctr,newCtr);	// does new limb cross old skeleton? need to get skeleton 
		flagLimbAddViolation = flagLimbAddViolation1 || flagLimbAddViolation2 || flagLimbAddViolation3;
		if (flagLimbAddViolation && !cantFail) {
			if (printStimSpecs) System.out.println(" **WARNING: Attempted new limb overlaps old limb! Limb not added. (checkIfPointsInPoly)");
			if (printStimSpecs) System.out.println("FAILED--limb#"+(newNodeID-1)+" toNode:"+nodeID+" -- l="+SachStringUtil.format(l,2)+", w="+SachStringUtil.format(w,2) 
					+", ori="+SachStringUtil.format(ori,0)+", curv="+SachStringUtil.format(curv,2));
			return !flagLimbAddViolation;	// return "limb add success = false"
		}
		
		
		// -- how to connect new node:
		int addIdx = 1;																			// default position to add new node
		int numCxns = nodes[nodeID].numCxdNodes; 
		double[][] newPtLocs = new double[4][2];
		
		if (numCxns == 1) {	// number of connected nodes already attached to nodeID = 1 (an end node)

			double ori0 = nodes[nodeID].oris[0];									// ori from source node to its single cxd node
			
				// --position pts at middle of angles:		
//			newPtLocs = SachMathUtil.mergeArrays(
//					makeSmoothJointPt(ori0,w0,l0,ori,w,l,ctr),
//					makeSmoothJointPt(ori,w,l,ori0,w0,l0,ctr) );
			// *** this version just uses the source width! ***
			newPtLocs = SachMathUtil.mergeArrays(
					makeSmoothJointPt(ori0,w0,l0,ori,w0,l,ctr),
					makeSmoothJointPt(ori,w0,l,ori0,w0,l0,ctr) );
			
			// *** check if makeSmoothJointPt had a violation ***
			if (flagLimbAddViolation && !cantFail) {
				if (printStimSpecs) System.out.println(" **WARNING: Attempted new limb overlaps old limb! Limb not added. (makeSmoothJointPt)");
				if (printStimSpecs) System.out.println("FAILED--limb#"+(newNodeID-1)+" toNode:"+nodeID+" -- l="+SachStringUtil.format(l,2)+", w="+SachStringUtil.format(w,2) 
						+", ori="+SachStringUtil.format(ori,0)+", curv="+SachStringUtil.format(curv,2));
				return !flagLimbAddViolation;	// return "limb add success" -- new node violation, node not added
			}
			
				// now move pts to these locations:
			for (int n=0;n<4;n++) {
				moveCtrlPt(sourceNode_CtrlPtIdxs[n],newPtLocs[n]);
			}
					
		} else if (numCxns == 2 | numCxns == 3) {
			
			// --add/move pts
				// find oris of other cxd nodes: (oris range from 0 to 359)
			double[] oris    = Arrays.copyOf(nodes[nodeID].oris,numCxns);
			//double[] widths  = Arrays.copyOf(nodes[nodeID].widths,numCxns);
			double[] lengths = Arrays.copyOf(nodes[nodeID].lengths,numCxns);

				// find flanking node orientations (and find flanking node widths and lengths)	
			int[] lowHighIdxs = findFlankingNodes(ori,oris); 
			double lowOri  = oris[lowHighIdxs[0]];
			double highOri = oris[lowHighIdxs[1]];		
			//double lowW  = widths[lowHighIdxs[0]];
			//double highW = widths[lowHighIdxs[1]];
			double lowL  = lengths[lowHighIdxs[0]];
			double highL = lengths[lowHighIdxs[1]];
			
				// find add index:
			addIdx = SachMathUtil.findFirst(oris, highOri)*2+1;
			
//			newPtLocs = SachMathUtil.mergeArrays(
//					makeSmoothJointPt(highOri,highW,highL,   ori,w ,l ,ctr),
//					makeSmoothJointPt(    ori,w ,l ,lowOri,lowW,lowL,ctr) );
			newPtLocs = SachMathUtil.mergeArrays(		// this version just uses the source width! ***
					makeSmoothJointPt(highOri,w0,highL,   ori,w0,l ,ctr),
					makeSmoothJointPt(    ori,w0,l ,lowOri,w0,lowL,ctr) );
			
			// *** check if makeSmoothJointPt had a violation ***
			if (flagLimbAddViolation && !cantFail) {
				if (printStimSpecs) System.out.println(" **WARNING: Attempted new limb overlaps old limb! Limb not added. (makeSmoothJointPt)");
				if (printStimSpecs) System.out.println("   FAILED--limb#"+(newNodeID-1)+" toNode:"+nodeID+" -- l="+SachStringUtil.format(l,2)+", w="+SachStringUtil.format(w,2) 
						+", ori="+SachStringUtil.format(ori,0)+", curv="+SachStringUtil.format(curv,2));
				return !flagLimbAddViolation;	// return "limb add success" -- new node violation, node not added
			}
			
				// now move pts to these locations:
			moveCtrlPt(sourceNode_CtrlPtIdxs[addIdx-1],newPtLocs[0]);
			moveCtrlPt(sourceNode_CtrlPtIdxs[addIdx],  newPtLocs[1]);
			addCtrlPt( sourceNode_CtrlPtIdxs[addIdx],  newPtLocs[2],nodeID);
			addCtrlPt( sourceNode_CtrlPtIdxs[addIdx]+1,newPtLocs[3],nodeID);
			
		} else {
			if (printStimSpecs) System.out.println("  **ERROR: Cannot have more than 4 connections per node! Limb not added.");
			return !flagLimbAddViolation;	// return "limb add success" -- new node violation, node not added
		}
		
		// create and add new node:
		int addPt = sourceNode_CtrlPtIdxs[addIdx];
//		createNode(w,ori,newCtr);							// use absolute ori here
		createNode(w,ori,newCtr,curv[0],curv[1],curv[2],curv[3]);	
		addNodeCtrlPts(addPt,newNodeID);					// attach ctrl pts of new node after addIdx ctrl pt of old node		
		
		// update node info:
		nodes[nodeID].addNode(newNodeID,l,w,ori);
		nodes[newNodeID].addNode(nodeID,l0,w0,rescaleOri(ori+180));
		
		// spline smoothness check:
			// create vertices from control pts
			// check all vertices with boolean b = checkIfPointsInPoly(objCtrlPts,objCtrlPts)
			// BUT! need to change this? so that points on the line are ok?
		
		// print out limb specs:
//		System.out.println("limb#"+(newNodeID-1)+" toNode:"+nodeID+" -- l="+SachStringUtil.format(l,2)+", w="+SachStringUtil.format(w,2) 
//				+", ori="+SachStringUtil.format(ori,0)+", curv="+SachStringUtil.format(curv,2));
		if (printStimSpecs) System.out.println("limb#"+(newNodeID-1)+":toNode,l,w,ori,curv--("+nodeID+","+SachStringUtil.format(l,2)+","+SachStringUtil.format(w,2) 
				+","+SachStringUtil.format(ori,0)+",new double[]{"+SachStringUtil.format(curv,2)+"})");
		
		if (flagLimbAddViolation && cantFail) {
			if (printStimSpecs) System.out.println(" **WARNING: Attempted new limb overlaps old limb! But limb was added anyway.");
		}
		return !flagLimbAddViolation;
	}
	
	boolean splitLimb(int nodeID) {
		// -- this splits the limb preceeding nodeID -- 		
		// how?: take limb, make two limbs from it (split it) by reducing length of the limb by 1/2, 
		// and then adding a duplicate of it (at 1/2 length) to itself
		// since limbs are created first (for checking), need to correct each subsequent node's nodeIDs	
		// INPUT: nodeID -- identifies the limb to be split, goes from 1 to x (limbID=nodeID-1, unless its the first limb)
		// OUTPUT: returns TRUE if splitting is successful, and FALSE if it led to overlap errors
		
		int limbID = nodeID>0 ? nodeID-1 : 0;	// get limbID from nodeID
		
		// split limb:		
			// first, reduce length of current limb by half:
		// TODO: check that this is valid (l/2), also might want to randomly place split node??
		double half_length = limbs.get(limbID).getLength()/2;	// get length and divide in half
		limbs.get(limbID).setLength(half_length);
		double[] old_curv = limbs.get(limbID).getCurv();
		double old_ori = limbs.get(limbID).getOri();
		
		// update all the other "from" node IDs in subsequent limbs:
		int thisNodeId;
		for (int n=0;n<limbs.size();n++) {	// change nodesIds that follow the duplicate node to be added
			thisNodeId = limbs.get(n).getNodeId(); 
			if (thisNodeId >= nodeID) {
				limbs.get(n).setNodeId(thisNodeId+1);
			}
		}
		
		// then add duplicate limb: (put if here on nodeID 0 or 1, or else)
		if (nodeID == 0) {			// first limb, first node
//			double[] new_curv = new double[4];
//			for (int n=0;n<4;n++) {
//				new_curv[n] = old_curv[n];	// first 4 values
//			}			
//			limbs.add(limbID+1, new LimbSpec(nodeID,half_length,limbs.get(limbID).getWidth(),rescaleOri(old_ori+180),new_curv,limbs.get(limbID).isSmoother()));
			if (printStimSpecs) System.out.println(" ***ERROR: split nodeID 0: this should not happen!");
			return false;
		} else if (nodeID == 1) {	// first limb, second node
			double[] new_curv = new double[4];
			for (int n=0;n<4;n++) {
				new_curv[n] = old_curv[n+4];	// second 4 values
			}
			limbs.add(limbID+1, new LimbSpec(nodeID,half_length,limbs.get(limbID).getWidth2(),old_ori,new_curv,limbs.get(limbID).isSmoother2()));
			
		} else {	// any other but the first limb
			limbs.add(limbID+1, new LimbSpec(nodeID,half_length,limbs.get(limbID).getWidth(),old_ori,old_curv,limbs.get(limbID).isSmoother()));
		}
		
		
		// recreate limbs:
		if (!createObjFromSpec()) {
			if (printStimSpecs) System.out.println(" **FAILED: split");
			return false;
		}
		spec.setAllLimbSpecs(limbs);	// add limbs to spec (for later saving this info in db)
		return true;
	}
	
	public boolean addRandLimbWithSplit(int nodeID, boolean doSplit) {
								
		// if adding between nodes, split here:
		if (doSplit) {	// split the limb that the node forms
			nodeID = nodeID>0 ? nodeID : 1;	// set nodeID of 0 to 1 (for special cases where the first limb needs to be split) 
			if (!splitLimb(nodeID)) {
				return false;
			} 
			
		} else { // otherwise, to keep the addLimb check, I need to first add all the existant limbs:
			if (!createObjFromSpec()) {
				return false;	// this should not fail, indicates a failure not caught earlier
			}
		}
		
		// then try to add limb:	
		for (int n=0;n<20;n++) {	// try adding limb at most X times
			if (addRandomLimb(nodeID)) return true;
		}
		
		return false;
				
		// issues:	some failures (can't get length from nodeID?)
		//			need to bias oris used (away from parallels, toward orthogonals)

	}
	
	public boolean addRandomLimb(int nodeID) {
		// this method adds a limb with random specs (l,w,ori,curv,isSmoother) to a given node.
		// if it fails, then no limb is added and we return false.

		// okay, have nodeID adding to, so get ori from that node
		int limbID = nodeID>0 ? nodeID-1 : 0;	// get limbID from nodeID
		double ori0 = limbs.get(limbID).getOri();
		
		double l = SachMathUtil.randRange(lim_maxLength,lim_minLength);			// random length
		double w = SachMathUtil.randRange(lim_maxWidth,lim_minWidth);			// random width
		double ori = 0;	//SachMathUtil.randRange(lim_maxOri,lim_minOri);				// random ori
		double[] curv = SachMathUtil.randRange(lim_maxCurv,lim_minCurv,4);		// random curvature
		boolean isSmoother = SachMathUtil.randBoolean(lim_propSmoothNodes);		// random 3 or 4 control pt limb
		
		// ori: (20% of time, just pick ori_source (same ori), otherwise bias away from near oris
		if (SachMathUtil.randBoolean(lim_propParallelLimbs)) {
			ori = rescaleOri(ori0);	// parallel angle
		} else {
			double sigma = 45; 	//deg; % within 20deg of ori0: sigma=30: ~1.65%, 45: ~7.5%, 60: ~11%, [flat dist: 22%] 
			double mu = ori0+90;//deg
			double vMin = ori0, vMax = ori0+180;
			ori = SachMathUtil.randBoundedGauss(sigma, mu, vMin, vMax);
			ori = SachMathUtil.randBoolean() ? ori : rescaleOri(-ori); // equal dist of +/- oris
		}
		
		if (!addLimb(nodeID,l,w,ori,curv,isSmoother)) return false; // add limb; if addLimb fails return false
		//System.out.println();
		limbs.add(new LimbSpec(nodeID,l,w,ori,curv,isSmoother));	// add limbSpec
		
		return true;		
	}
	
	public boolean addManualLimbHandler(int nodeID,double l,double w,double ori){
		boolean isCanonical = true;
		for (int nLimb=0;nLimb<limbs.size();nLimb++) {
			LimbSpec specL = limbs.get(nLimb);
			if (specL.getWidth()!=1){
				isCanonical = false;
				break;
			}
		}
		return addManualLimbHandler(nodeID, l, w, ori, isCanonical);
	}
	public boolean addManualLimbHandler(int nodeID,double l,double w,double ori,boolean isCanonical){
		
		if (!createObjFromSpec()) {
			return false;	// this should not fail, indicates a failure not caught earlier
		}
		for (int n=0;n<20;n++) {	// try adding limb at most X times
			if (addManualLimb(nodeID,l,w,ori,isCanonical)) return true;
		}
		w = -1;
		for(int n=0;n<20;n++){
			if (addManualLimb(nodeID,l,w,ori,isCanonical)) return true;
		}
		l = -1;
		for(int n=0;n<20;n++){
			if (addManualLimb(nodeID,l,w,ori,isCanonical)) return true;
		}
		ori = -1;
		for(int n=0;n<20;n++){
			if (addManualLimb(nodeID,l,w,ori,isCanonical)) return true;
		}
		
		return false;
	}
	public boolean addManualLimb(int nodeID,double l,double w,double ori,boolean isCanonical){
		int limbID = nodeID>0 ? nodeID-1 : 0;	// get limbID from nodeID
		
		if (l==-1){
			l = SachMathUtil.randRange(lim_maxLength,lim_minLength);
		}
		if (w==-1){
			w = SachMathUtil.randRange(lim_maxWidth,lim_minWidth);
		}
		if (ori==-1){
			double ori0 = limbs.get(limbID).getOri();
			double sigma = 45; 	//deg; % within 20deg of ori0: sigma=30: ~1.65%, 45: ~7.5%, 60: ~11%, [flat dist: 22%] 
			double mu = ori0+90;//deg
			double vMin = ori0, vMax = ori0+180;
			ori = SachMathUtil.randBoundedGauss(sigma, mu, vMin, vMax);
			ori = SachMathUtil.randBoolean() ? ori : rescaleOri(-ori);
		}
		double[] curv = new double[]{1,1,1,1};
		boolean isSmoother = false;
		if(!isCanonical){
			curv = SachMathUtil.randRange(lim_maxCurv,lim_minCurv,4);		// random curvature
			isSmoother = SachMathUtil.randBoolean(lim_propSmoothNodes);		// random 3 or 4 control pt limb
		}
		
		if (!addLimb(nodeID,l,w,ori,curv,isSmoother)) return false; // add limb; if addLimb fails return false
		//System.out.println();
		limbs.add(new LimbSpec(nodeID,l,w,ori,curv,isSmoother));	// add limbSpec
		return true;
	}
	
	
	public void removeEndLimb(int nodeID) {
		// can only remove an end node!
		int localNumLimbs = limbs.size();

		if (nodeID < 2) {	// remove node in first limb
			// replace values in limb 0 by those in limb 1:
			limbs.get(0).setLength(limbs.get(1).getLength());
			limbs.get(0).setOri(limbs.get(1).getOri());
			double[] curv0 = limbs.get(0).getCurv().clone();
			double[] curv1 = limbs.get(1).getCurv().clone();
			double[] newCurv = new double[8];

			if (nodeID == 0) {	// node 0
				limbs.get(0).setWidth(limbs.get(0).getWidth2());
				limbs.get(0).setWidth2(limbs.get(1).getWidth());
				for (int n=0;n<curv1.length;n++) {
					newCurv[n] = curv0[n+4];
					newCurv[n+4] = curv1[n];
				}
				limbs.get(0).setIsSmoother(limbs.get(0).isSmoother2());
				limbs.get(0).setIsSmoother2(limbs.get(1).isSmoother());
			} else {					// node 1
				limbs.get(0).setWidth2(limbs.get(1).getWidth());
				for (int n=0;n<curv1.length;n++) {
					newCurv[n] = curv0[n];
					newCurv[n+4] = curv1[n];
				}	
				limbs.get(0).setIsSmoother2(limbs.get(1).isSmoother());
			}

			// remove limb 1 and fix nodeIds:
			limbs.remove(1);

		} else {	// remove any other end-node
			limbs.remove(nodeID-1);	
		}
		
		localNumLimbs--;
		int thisNodeId;
		for (int n=0;n<localNumLimbs;n++) { // change nodesIds that follow nodeToRemove
			thisNodeId = limbs.get(n).getNodeId(); 
			if (thisNodeId > nodeID) {
				limbs.get(n).setNodeId(thisNodeId-1);
			}
		}
	}
	
	
	double[][] makeSmoothJointPt(double oriA,double wA,double lA,double oriB,double wB,double lB,double[] ctr) {
			
		// turn ori and length into vectors, with ctr as origin -- vecA is source, vecB is new
		double[] vecA = SachMathUtil.pol2vect(oriA,1);		// defined where ctr = [0,0], and 0deg is to the right
		double[] vecB = SachMathUtil.pol2vect(oriB,1);

//		// convert vectors to points
//		double[] ptA = SachMathUtil.addVectors(vecA,ctr);
//		double[] ptB = SachMathUtil.addVectors(vecB,ctr);
	
		// in which quadrant is the difference angle?
		double diffAng = rescaleOri(oriA-oriB); 
		boolean ang0to90 = 0 <= diffAng & diffAng <= 90;
//		boolean ang0to180 = 0 <= diffAng & diffAng <= 180;
		boolean ang270to360 = 270 <= diffAng & diffAng <= 360;
		
		// find bisecting angle/ori (angle of (vecB - vecA) + 90deg)
		double bisAng = SachMathUtil.vectorAngle(SachMathUtil.subtractVectors(vecB,vecA)) + 90;

		double w = (wA > wB) ? wA : wB; // use largest width for calculating (for non-acute angles)
		// why not just use source width?
//		w = wB;
//		w = (wA < wB) ? wA : wB;	// use smallest -- also no good
//		wA = w;
//		wB = w;
		// really want to square it off
			
		// find initial positions for joint smoothing and calculate the spread and spread angle
		double[] startPt, offPtA, offPtB;
		if (ang0to90) { // || ang270to360) { // acute angles -- startPt is where angles cross (*** added "|| ang270to360" ***)
			offPtA = SachMathUtil.findPointAlongLine(ctr,oriA-90,wA/2);
			offPtB = SachMathUtil.findPointAlongLine(ctr,oriB+90,wB/2);
			startPt  = SachMathUtil.findLineIntersection(offPtA,oriA,offPtB,oriB);	// for acute angles, find intersetion of the two oris
		} else { // non-acute -- startPt is fixed when > 180deg
				// calculate distance joint point(s) will be from ctr
			double diffAng2Use = ang270to360 ? 270 : diffAng;									// if difference angle is between 270 & 360 , use difference angle of 270 to restrict the length of the "elbow"
			double d = (w/2)/Math.sin(diffAng2Use/2 * Math.PI/180)*(10-Math.abs(Math.sin(diffAng2Use*Math.PI/180)))/10;		// added factor (10-abs(sin(diffAng)))/10 is to make joint angle look tighter (pulls in more at 90deg)
			startPt  = SachMathUtil.findPointAlongLine(ctr,bisAng,d);
		}
			
		// point spread -- creates curvature at joint	
		double curv = jointAngleCurvFactor;
		double spread = ang270to360 ? (curv+(1-curv)*SachMathUtil.cosd(diffAng))*w/2 : curv*w/2;	// this allows more obtuse angles to have less elongated joints/elbows
		spread = ang0to90 ? curv*w/Math.sqrt(2) : spread;											// for acute angles, spread curvature along ori radiations (instead of orthogonal to bisAng)
		double spreadAngle1 = ang0to90 ? oriA : bisAng+90;
		double spreadAngle2 = ang0to90 ? oriB : bisAng-90;
		
		// *** need to make sure that curve cntl pts don't encroach on node (end) cntl pts:
			// check that dist <= l-w/2, where dist = spread + dist of startPt from ctr (along ori) -- *** may want to play with this ***
		if (ang0to90) {
			double[] strtPtVec = SachMathUtil.subtractVectors(startPt,ctr);
			double lenLimA = lA - wA/2 - SachMathUtil.doDotProd(strtPtVec,vecA);
			double lenLimB = lB - wB/2 - SachMathUtil.doDotProd(strtPtVec,vecB);
			double lenLim = (lenLimA < lenLimB) ? lenLimA : lenLimB;
			if (spread > lenLim) { 
				spread = lenLim; 	
				flagLimbAddViolation = true;	// set limb add violation flag so that limbAdd can be redone if needed
			}
		}
		
		// checks (above): need to set flag if it fails some tests (like if the new crotch or node points will overlap
		// the source node points...). the flag set above can be used in addLimbCreator to set output boolean if needed
		double[] outPt1 = SachMathUtil.findPointAlongLine(startPt,spreadAngle1,spread);
		double[] outPt2 = SachMathUtil.findPointAlongLine(startPt,spreadAngle2,spread);
		double[] outPt3 = SachMathUtil.findPointAlongLine(startPt,spreadAngle1,0);
		
		return new double[][]{outPt1,outPt2,outPt3};
		
//		double[] outPt1 = SachMathUtil.findPointAlongLine(startPt,spreadAngle1,spread);
//		double[] outPt2 = SachMathUtil.findPointAlongLine(startPt,spreadAngle2,spread);
//		
//		return new double[][]{outPt1,outPt2};
	}
	
	void addCtrlPt(int addIdx, double[] xyValue,int nodeID) {
		// add ctrl pt to objCntlPts after addIdx for nodeID and update nodeID info
		int numPts_newObj = objCtrlPts.length;
		objCtrlPts = SachMathUtil.mergeArrays(
			Arrays.copyOfRange(objCtrlPts,0,addIdx+1),
			new double[][]{xyValue},
			Arrays.copyOfRange(objCtrlPts,addIdx+1,numPts_newObj) 
			);
		ctrlPtNodeIDs = SachMathUtil.mergeArrays(
			Arrays.copyOfRange(ctrlPtNodeIDs,0,addIdx+1),
			new int[]{nodeID},
			Arrays.copyOfRange(ctrlPtNodeIDs,addIdx+1,numPts_newObj) 
			);
	}
	
	void addNodeCtrlPts(int addIdx,int newNodeID,boolean isSmoother) {
		// add ctrl pts of newNodeID to objCntlPts and update nodeID info
		int numPts_newObj = objCtrlPts.length;
		int[] newNodeIDs;
		if (!isSmoother) {
			newNodeIDs = new int[]{newNodeID,newNodeID,newNodeID,newNodeID};
		} else {
			newNodeIDs = new int[]{newNodeID,newNodeID,newNodeID};
		}
		objCtrlPts = SachMathUtil.mergeArrays(
			Arrays.copyOfRange(objCtrlPts,0,addIdx+1),
			nodes[newNodeID].ctrlPts,
			Arrays.copyOfRange(objCtrlPts,addIdx+1,numPts_newObj) 
			);
		ctrlPtNodeIDs = SachMathUtil.mergeArrays(
			Arrays.copyOfRange(ctrlPtNodeIDs,0,addIdx+1),
			newNodeIDs,
			Arrays.copyOfRange(ctrlPtNodeIDs,addIdx+1,numPts_newObj) 
			);
	}
	
	void addNodeCtrlPts(int addIdx,int newNodeID) {
		// add ctrl pts of newNodeID to objCntlPts and update nodeID info
		int numPts_newObj = objCtrlPts.length;
		objCtrlPts = SachMathUtil.mergeArrays(
			Arrays.copyOfRange(objCtrlPts,0,addIdx+1),
			nodes[newNodeID].ctrlPts,
//			new double[][]{nodes[newNodeID].ctrlPts[1],
//			nodes[newNodeID].ctrlPts[2],
//			nodes[newNodeID].ctrlPts[3]},
			Arrays.copyOfRange(objCtrlPts,addIdx+1,numPts_newObj) 
			);
		ctrlPtNodeIDs = SachMathUtil.mergeArrays(
			Arrays.copyOfRange(ctrlPtNodeIDs,0,addIdx+1),
			new int[]{newNodeID,newNodeID,newNodeID,newNodeID},
			Arrays.copyOfRange(ctrlPtNodeIDs,addIdx+1,numPts_newObj) 
			);
	}
	
	void removeCtrlPt(int removeIdx) {
		// remove ctrl pts of newNodeID to objCntlPts and update nodeID info
		int numPts_newObj = objCtrlPts.length;
		objCtrlPts = SachMathUtil.mergeArrays(
			Arrays.copyOfRange(objCtrlPts,0,removeIdx),
			Arrays.copyOfRange(objCtrlPts,removeIdx+1,numPts_newObj) 
			);
		ctrlPtNodeIDs = SachMathUtil.mergeArrays(
			Arrays.copyOfRange(ctrlPtNodeIDs,0,removeIdx),
			Arrays.copyOfRange(ctrlPtNodeIDs,removeIdx+1,numPts_newObj) 
			);
	}
	
	void moveCtrlPt(int moveIdx, double[] xyValue) {
		// change the x,y coordinates of a certain control point
		int numPts_newObj = objCtrlPts.length;
		objCtrlPts = SachMathUtil.mergeArrays(
			Arrays.copyOfRange(objCtrlPts,0,moveIdx),
			new double[][]{xyValue},
			Arrays.copyOfRange(objCtrlPts,moveIdx+1,numPts_newObj) 
			);
	}
	
	void closeObj() {
		// copies first 3 control points to the end to close the spline
		objCtrlPts = SachMathUtil.mergeArrays(objCtrlPts,Arrays.copyOfRange(objCtrlPts, 0, 3)); 
	}

	
	//---- drawing methods ----
	
	public void draw() {
		// draw splines:
		if (spline == null) return;	// if null/blank don't attempt to draw
		GL11.glClearColor(backColor.getRed(),backColor.getGreen(),backColor.getBlue(),1);
		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
		if(!solid) {
			// for outline:
			GL11.glColor3f(0f,0f,0f);
			
			GL11.glEnable(GL11.GL_LINE_SMOOTH);
			GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
//			GL11.glEnable(GL11.GL_POINT_SMOOTH);
//			GL11.glHint(GL11.GL_POINT_SMOOTH_HINT, GL11.GL_NICEST);
			GL11.glEnable(GL11.GL_BLEND);
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			GL11.glLineWidth(2.3f);

			for (int i=0; i< spline.getBnum()-1; i++) {
				drawLine(spline.bpt[i],spline.bpt[i+1]);
			}
				
		} else {
			// for solid:
//			GL11.glColor3f(0.3f,0.3f,0.3f);
// JK			GL11.glColor3f(0.3f,0.3f,0.3f);
			// JK set the color from the spec
			//GL11.glColor4d(spec.getRedVal(), spec.getGreenVal(), spec.getBlueVal(),  spec.getAlphaVal());
			GL11.glColor3d(color.getRed(),color.getGreen(),color.getBlue());
			
			// create tessellator
			GLUtessellator tess = GLUtessellatorImpl.gluNewTess();
			
			// register callback functions
			tess.gluTessCallback(GLU.GLU_TESS_BEGIN, tessCB);
			tess.gluTessCallback(GLU.GLU_TESS_END, tessCB);
			tess.gluTessCallback(GLU.GLU_TESS_VERTEX, tessCB);
			tess.gluTessCallback(GLU.GLU_TESS_COMBINE, tessCB);
			tess.gluTessCallback(GLU.GLU_TESS_ERROR, tessCB);
			
			GL11.glEnable(GL11.GL_BLEND);
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);
			GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK,GL11.GL_FILL);
//			GL11.glEnable(GL11.GL_POLYGON_SMOOTH);
//			GL11.glHint(GL11.GL_POLYGON_SMOOTH_HINT, GL11.GL_NICEST);
//			GL11.glEnable(GL11.GL_LINE_SMOOTH);
//			GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);

			// smoothing/anitaliasing using multisample? -- just added "4" for the samples part in StimTestWindow 
			//				PixelFormat pixelFormat = new PixelFormat(0, 8, 1,4);
			// 				window.setPixelFormat(pixelFormat);
			
			//System.out.println("bool? " + GLContext.getCapabilities().GL_ARB_multisample);
			//GL11.glEnable(ARBMultisample.GL_MULTISAMPLE_ARB);

			//describe non-convex ploygon
			tess.gluTessBeginPolygon(null);
				// first contour
				tess.gluTessBeginContour();
					for (int i=0; i< spline.getBnum(); i++) {
						double[] coords = new double[] {spline.bpt[i].x+xPos, spline.bpt[i].y+yPos,0};	// only add x,y position changes when drawing!
//						double[] coords = new double[] {spline.bpt[i].x, spline.bpt[i].y,0};
						tess.gluTessVertex(coords,0,coords);
					}
				tess.gluTessEndContour();
				// second contour ...
			tess.gluTessEndPolygon();
			
			// delete tessellator after processing
			tess.gluDeleteTess();
			GL11.glDisable(GL11.GL_BLEND);
		}

		
		//  JK
		//Commented out for now (May 9th 2017)
//		if(spec.isOccluded()){
//			drawOccluder(context);
//		}

	}
  
	
	void drawLine(MyPoint p1, MyPoint p2) {
	    GL11.glBegin(GL11.GL_LINES);
	    	GL11.glVertex2d(p1.x+xPos, p1.y+yPos);		// only add x,y position changes when drawing!
	    	GL11.glVertex2d(p2.x+xPos, p2.y+yPos);
//	    	GL11.glVertex2d(p1.x, p1.y);
//	    	GL11.glVertex2d(p2.x, p2.y);
	    GL11.glEnd();
	    GL11.glFlush();
	}


	


	GLUtessellatorCallbackAdapter tessCB = new GLUtessellatorCallbackAdapter(){
		@Override
		public void begin(int type) {
			GL11.glBegin(type);
		}

		@Override
		public void vertex(Object vertexData) {
			double[] vert = (double[]) vertexData;
			//GL11.glVertex3d(vert[0], vert[1],vert[2]);
			GL11.glVertex2d(vert[0],vert[1]);
		}

		public void combine(double[] coords, Object[] data, float[] weight, Object[] outData) {
			for (int i = 0; i < outData.length; i++) {
				double[] combined = new double[20];
				combined[0] = (double) coords[0];
				combined[1] = (double) coords[1];
				//combined[2] = (double) coords[2];
				outData[i] = combined;
			}
		}

		@Override
		public void end() {
			GL11.glEnd();
		}

		public void error(int errnum) {
			String estring;
			estring = GLU.gluErrorString(errnum);
			System.err.println("Tessellation Error Number: " + errnum);
			System.err.println("Tessellation Error: " + estring);
		}
	};
	
	
	//---- specific behavioral stimulus creation ----

	void stimChooser() {
		//NOT CURRENTLY USED!!
		// pre-set stim specs:
		
//		xPos = xPos+SachMathUtil.randRange(1,-1); 				// jitter position (for debugging)
//		yPos = yPos+SachMathUtil.randRange(1,-1);

		double pL = 0.0, pW = 0.0; 								// percent change in lengths and widths (when randomized) (goes from 0 to 1)
		
		if (doRand) {
			pL = 0.4;	// TODO: these limit how much lengths and width are allowed to change!						
			pW = 0.4;
		}

		double lMax = 1*(1+pL), lMin = 0.9999*(1-pL);			// range for lengths
		double wMax = 1*(1+pW), wMin = 0.9999*(1-pW);			// range for widths
		double[] l = SachMathUtil.randRange(lMax,lMin,10);		// random lengths array (for up to 10 limbs)
		double w = SachMathUtil.randRange(wMax,wMin);			// random widths, across all limbs in stim
		// TODO: set these limits to global lims?
		
		// TODO: finish morph line stuff!
		// morphing:
		//doMorph = true;	// (for debugging)
		//doRand = false;	// (for debugging)

		// morphLim is passed from the SpecGenerator and will be used to specify the morphing limit for the current object category
//		double morphAmt = SachMathUtil.randRange(morphLim, 0);	// randomly chooses a morph-line value between 0 and morphLim, (morphLim should also be between 0 and 1)
		double m = morphLim; // (for debugging) -- test at max of morph lim
		//System.out.println("morphAmt="+m);
		
		// curvature:
		double[] curv1st = new double[]{1,1,1,1,1,1,1,1};		// default curvature
		double[] curv = new double[]{1,1,1,1};					// default curvature
			// for isSmoother nodes: curv[elong, bend, width???, ?]	
		doCenterObject = false;

		switch(stimCat) {
		case SEVEN_h:		// #0

			if (doMorph) {
				
				double dw = m/2+1; 	// delta width factor; or for more changes at joints: dw = m/1+1;
				double lr = m*1;	// lower right curvature change factor
				double ul = m*0;	// upper left curvature change factor
				limbs.add(new LimbSpec(-1,l[0]*4,w,w*dw,  0,curv1st)); 								// lr joint 	-- bulge	[maybe change ori slightly (to counter medial-axis change?): (ori +5*m)]
				limbs.add(new LimbSpec( 0,l[1]*4,w,270,curv));										// lower left
				limbs.add(new LimbSpec( 1,l[2]*4,w,270,new double[]{1-lr,1-lr,1+lr,1+lr}));			// lower right 	-- sharpens curvature and slightly lengthens end
				limbs.add(new LimbSpec( 0,l[3]*4,w/dw, 90,curv));									// ul joint 	-- pinch
				limbs.add(new LimbSpec( 4,l[4]*3,w+m*2,180,new double[]{1+ul,1+ul,1+ul,1+ul}));		// upper left 	-- broadens curvature (by increasing width only)			

			} else { // exemplar
				limbs.add(new LimbSpec(-1,l[0]*4,w,  0,curv1st));
				limbs.add(new LimbSpec( 0,l[1]*4,w,270,curv));
				limbs.add(new LimbSpec( 1,l[2]*4,w,270,curv));
				limbs.add(new LimbSpec( 0,l[3]*4,w, 90,curv));
				limbs.add(new LimbSpec( 4,l[4]*3,w,180,curv));
			}		
			break;

		case Y_h:			// #1

			if (doMorph) {

				double dw = m/2+1; 	// delta width factor
				double lr = m*1;	// lower right curvature change factor
				double ul = m*0;	// upper left curvature change factor
				limbs.add(new LimbSpec(-1,l[0]*4,w,w*dw,  0,curv1st)); 								// lr joint 	-- bulge	[maybe change ori slightly (to counter medial-axis change?): (ori +5*m)]
				limbs.add(new LimbSpec( 0,l[1]*4,w,270,curv));										// lower left
				limbs.add(new LimbSpec( 1,l[2]*4,w,270,new double[]{1-lr,1-lr,1+lr,1+lr}));			// lower right 	-- sharpens curvature and slightly lengthens end
				limbs.add(new LimbSpec( 0,l[3]*3,w/dw, 90,curv));									// ul joint 	-- pinch
				limbs.add(new LimbSpec( 4,l[4]*4,w, 45,curv));										// upper right
				limbs.add(new LimbSpec( 4,l[5]*4,w+m*2,135,new double[]{1+ul,1+ul,1+ul,1+ul}));		// upper left 	-- broadens curvature (by increasing width only)
//				limbs.add(new LimbSpec( 4,l[5]*4,w,135,new double[]{1+m*4,1+m*4,1+m*6,1+m*6},true));
//				limbs.add(new LimbSpec( 4,l[5]*4,w,135,new double[]{1+m*3,2,1,1},true));
			
			} else {
				limbs.add(new LimbSpec(-1,l[0]*4,w,  0,curv1st));
				limbs.add(new LimbSpec( 0,l[1]*4,w,270,curv));
				limbs.add(new LimbSpec( 1,l[2]*4,w,270,curv));
				limbs.add(new LimbSpec( 0,l[3]*3,w, 90,curv));
				limbs.add(new LimbSpec( 4,l[4]*4,w, 45,curv));
				limbs.add(new LimbSpec( 4,l[5]*4,w,135,curv));
			}
			break;

		case downL_downT:	// #2

			if (doMorph) {

				double dw = m/2+1; 		// delta width factor
//				double dw2 = m/1+1; 	// delta width factor2
				double ur = m*1-0.01;	// upper right curvature change factor

				limbs.add(new LimbSpec(-1,l[0]*8,w,w*dw, 90,curv1st,new double[]{0,-l[0]*8/2}));	// ur joint 	-- bulge; ll joint  -- pinch
				limbs.add(new LimbSpec( 0,l[1]*3,w*dw,  0,curv));									// lower right 	
				limbs.add(new LimbSpec( 0,l[2]*3,w*dw,180,curv));									// lower left   -- broadens curvature
				limbs.add(new LimbSpec( 1,l[3]*3,w,  0,new double[]{1-ur,1-ur,1+ur,1+ur}));			// upper right  -- shaprens curvature			

				// TODO: fix bottom morph, make the whole width of bottom enlarge, but keep upward branch small
				
			} else {
				//yPos = yPos-l[0]*8/2;	// don't change xPos, yPos as those are only used in drawing routines
				limbs.add(new LimbSpec(-1,l[0]*8,  w, 90,curv1st,new double[]{0,-l[0]*8/2}));
				limbs.add(new LimbSpec( 0,l[1]*3,  w,  0,curv));
				limbs.add(new LimbSpec( 0,l[2]*3,  w,180,curv));
				limbs.add(new LimbSpec( 1,l[3]*3,w,  0,curv));			
			}
			break;

		case I_downT:		// #3

			if (doMorph) {
				
				//double dw = m/2+1; 	// delta width factor
				double dw2 = m/1+1;		// delta width factor2
				double u = m*1-0.01;	// upper endpoint curvature change factor
				double ll = m*0;	// lower left curvature change factor
				
				limbs.add(new LimbSpec(-1,l[0]*8,w,w*dw2, 90,new double[]{1,1,1,1,1-u,1-u,1+u,1+u},new double[]{0,-l[0]*8/2}));	// ur joint 	-- bulge; ll joint  -- pinch
				limbs.add(new LimbSpec( 0,l[1]*3,w,  0,curv));										// lower right 	
				limbs.add(new LimbSpec( 0,l[2]*3,w+m*2,180,new double[]{1+ll,1+ll,1+ll,1+ll}));		// lower left   -- broadens curvature
				
				// TODO: finish bottom morph (see #2 above)
				
			} else {
				//yPos = yPos-l[0]*8/2;
				limbs.add(new LimbSpec(-1,l[0]*8,  w, 90,curv1st,new double[]{0,-l[0]*8/2}));
				limbs.add(new LimbSpec( 0,l[1]*3,  w,  0,curv));
				limbs.add(new LimbSpec( 0,l[2]*3,  w,180,curv));
			}
			break;

		case SEVEN_t:		// #4

			if (doMorph) {
				
				double dw = m/2+1; 	// delta width factor
				double lr = m*1;	// lower right curvature change factor
				double ul = m*0;	// upper left curvature change factor
			
				limbs.add(new LimbSpec(-1,l[0]*5,w*dw,w/dw, 90,curv1st));								// lr and ul joint	-- pinch & bulge
//				limbs.add(new LimbSpec( 0,l[1]*3,w,  0,new double[]{1-lr,1-lr,1+lr,1+lr}));				// lower right	-- sharpens curvature
				limbs.add(new LimbSpec( 0,l[1]*3,w,  0,curv));											// lower right	
				limbs.add(new LimbSpec( 0,l[2]*3,w,180,curv));
				limbs.add(new LimbSpec( 0,l[3]*4,w,270,new double[]{1-lr,1-lr,1+lr,1+lr}));				// lower down   -- sharpens curvature
				limbs.add(new LimbSpec( 1,l[4]*4,w+m*2,180,new double[]{1+ul,1+ul,1+ul,1+ul}));			// upper left	-- broadens curvature		
				
			} else {
				limbs.add(new LimbSpec(-1,l[0]*5,w, 90,curv1st));
				limbs.add(new LimbSpec( 0,l[1]*3,w,  0,curv));
				limbs.add(new LimbSpec( 0,l[2]*3,w,180,curv));
				limbs.add(new LimbSpec( 0,l[3]*4,w,270,curv));
				limbs.add(new LimbSpec( 1,l[4]*4,w,180,curv));
			}
			break;

		case Y_t:			// #5

			if (doMorph) {
				
				double dw = m/2+1; 	// delta width factor
				double lr = m*1;	// lower right curvature change factor
				double ul = m*0;	// upper left curvature change factor
			
				limbs.add(new LimbSpec(-1,l[0]*3,w*dw,w/dw, 90,curv1st));								// lr and ul joint	-- pinch & bulge
//				limbs.add(new LimbSpec( 0,l[1]*3,w,  0,new double[]{1-lr,1-lr,1+lr,1+lr}));				// lower right	-- sharpens curvature
				limbs.add(new LimbSpec( 0,l[1]*3,w,  0,curv));											// lower right	
				limbs.add(new LimbSpec( 0,l[2]*3,w,180,curv));
				limbs.add(new LimbSpec( 0,l[3]*4,w,270,new double[]{1-lr,1-lr,1+lr,1+lr}));				// lower down   -- sharpens curvature
				limbs.add(new LimbSpec( 1,l[4]*4,w, 45,curv));
				limbs.add(new LimbSpec( 1,l[4]*4,w+m*2,135,new double[]{1+ul,1+ul,1+ul,1+ul}));			// upper left	-- broadens curvature
				
			} else {
				limbs.add(new LimbSpec(-1,l[0]*3,w, 90,curv1st));
				limbs.add(new LimbSpec( 0,l[1]*3,w,  0,curv));
				limbs.add(new LimbSpec( 0,l[2]*3,w,180,curv));
				limbs.add(new LimbSpec( 0,l[3]*4,w,270,curv));
				limbs.add(new LimbSpec( 1,l[4]*4,w, 45,curv));
				limbs.add(new LimbSpec( 1,l[4]*4,w,135,curv));
			}
			break;

		case downL_J:		// #6
			
			if (doMorph) {
				
				double dw = m/2+1; 		// delta width factor
				double dw2 = m/1+1; 	// delta width factor2
				double ur = m*1-0.01;	// upper right curvature change factor
				
				limbs.add(new LimbSpec(-1,l[0]*8,w,w*dw, 90,curv1st,new double[]{0,-l[0]*8/2}));	// ur joint -- bulge
				limbs.add(new LimbSpec( 0,l[1]*4,w/dw,180,curv));									// j, lower left  -- pinch 
				limbs.add(new LimbSpec( 2,l[2]*2,w*dw2, 90,curv));									// j. lower left  -- broaden curvature
				limbs.add(new LimbSpec( 1,l[3]*3,w,  0,new double[]{1-ur,1-ur,1+ur,1+ur}));			// upper right  -- sharpens curvature
				
			} else {
				//yPos = yPos-l[0]*8/2;
				limbs.add(new LimbSpec(-1,l[0]*8,w, 90,curv1st,new double[]{0,-l[0]*8/2}));
				limbs.add(new LimbSpec( 0,l[1]*4,w,180,curv));
				limbs.add(new LimbSpec( 2,l[2]*2,w, 90,curv));
				limbs.add(new LimbSpec( 1,l[3]*3,w,  0,curv));	
			}
			break;

		case I_J:			// #7

			if (doMorph) {
				
				double dw = m/2+1; 		// delta width factor
				double dw2 = m/1+1;		// delta width factor2
				double u = m*1-0.01;	// upper endpoint curvature change factor

				limbs.add(new LimbSpec(-1,l[0]*8,w,w*dw2, 90,new double[]{1,1,1,1,1-u,1-u,1+u,1+u},new double[]{0,-l[0]*8/2}));	// top  -- sharpens curvature
				limbs.add(new LimbSpec( 0,l[1]*4,w/dw,180,curv));																	// j, bottom   
				limbs.add(new LimbSpec( 2,l[2]*2,w*dw2, 90,curv));																// j. lower left  -- broaden curvature	
			
			} else {
				//yPos = yPos-l[0]*8/2;
				limbs.add(new LimbSpec(-1,l[0]*8,w, 90,curv1st,new double[]{0,-l[0]*8/2}));
				limbs.add(new LimbSpec( 0,l[1]*4,w,180,curv));
				limbs.add(new LimbSpec( 2,l[2]*2,w, 90,curv));
			}
			break;
		default:
			break;
			
		} // end switch
		
		spec.setAllLimbSpecs(limbs);
		
		// re-save spec into StimObjData db table:
		// -- this is done in SachExptSpecGenerator:
		
		//dbUtil.writeStimObjData(stimObjId, s.toXml(), d.toXml());
		
	}
	
	public void setGlobalOri(double o) {
		spec.setGlobalOri(o);
		global_ori = spec.global_ori; 
	}
//	
//	void stimChooser_other_categories() {
//		//NOT CURRENTLY USED
//		// TODO: need to finish these up and add them to the regular stimChooser	
//		
//		// pre-set stim specs:
//		
////		xPos = xPos+SachMathUtil.randRange(1,-1); 				// jitter position (for debugging)
////		yPos = yPos+SachMathUtil.randRange(1,-1);
//
//		double pL = 0.0, pW = 0.0; 								// percent change in lengths and widths (when randomized) (goes from 0 to 1)
//		
//		if (doRand) {
//			pL = 0.4;						
//			pW = 0.4;
//		}
//
//		double lMax = 1*(1+pL), lMin = 0.9999*(1-pL);			// range for lengths
//		double wMax = 1*(1+pW), wMin = 0.9999*(1-pW);			// range for widths
//		double[] l = SachMathUtil.randRange(lMax,lMin,10);		// random lengths array (for up to 10 limbs)
//		double w = SachMathUtil.randRange(wMax,wMin);			// random widths, across all limbs in stim
//		
//		// morphing:
//		//doMorph = true;	// (for debugging)
//		//doRand = false;	// (for debugging)
//
//		// morphLim is passed from the SpecGenerator and will be used to specify the morphing limit for the current object category
////		double morphAmt = SachMathUtil.randRange(morphLim, 0);	// randomly chooses a morph-line value between 0 and morphLim, (morphLim should also be between 0 and 1)
//		double m = morphLim; // (for debugging) -- test at max of morph lim
//		//System.out.println("morphAmt="+m);
//		
//		// curvature:
//		double[] curv1st = new double[]{1,1,1,1,1,1,1,1};		// default curvature
//		double[] curv = new double[]{1,1,1,1};					// default curvature
//			// for isSmoother nodes: curv[elong, bend, width???, ?]	
//		doCenterObject = false;
//
//		switch(stimCat) {
//		case SEVEN_h:		// #8
//
//			if (doMorph) {
//				
////				double dw = m/2+1; 	// delta width factor; or for more changes at joints: dw = m/1+1;
////				double lr = m*1;	// lower right curvature change factor
////				double ul = m*0;	// upper left curvature change factor
////				limbs.add(new LimbSpec(-1,l[0]*4,w,w*dw,  0,curv1st)); 								// lr joint 	-- bulge	[maybe change ori slightly (to counter medial-axis change?): (ori +5*m)]
////				limbs.add(new LimbSpec( 0,l[1]*4,w,270,curv));										// lower left
////				limbs.add(new LimbSpec( 1,l[2]*4,w,270,new double[]{1-lr,1-lr,1+lr,1+lr}));			// lower right 	-- sharpens curvature and slightly lengthens end
////				limbs.add(new LimbSpec( 0,l[3]*4,w/dw, 90,curv));									// ul joint 	-- pinch
////				limbs.add(new LimbSpec( 4,l[4]*3,w+m*2,180,new double[]{1+ul,1+ul,1+ul,1+ul}));		// upper left 	-- broadens curvature (by increasing width only)			
//
//			} else { // exemplar
//				limbs.add(new LimbSpec(-1,l[0]*4,w,  0,curv1st));
//				limbs.add(new LimbSpec( 0,l[1]*4,w,270,curv));
//				limbs.add(new LimbSpec( 1,l[2]*4,w,270,curv));
//				limbs.add(new LimbSpec( 0,l[3]*4,w, 90,curv));
//				limbs.add(new LimbSpec( 4,l[4]*3,w,  0,curv));
//			}		
//			break;
//
//		case Y_h:			// #1
//
//			if (doMorph) {
//
////				double dw = m/2+1; 	// delta width factor
////				double lr = m*1;	// lower right curvature change factor
////				double ul = m*0;	// upper left curvature change factor
////				limbs.add(new LimbSpec(-1,l[0]*4,w,w*dw,  0,curv1st)); 								// lr joint 	-- bulge	[maybe change ori slightly (to counter medial-axis change?): (ori +5*m)]
////				limbs.add(new LimbSpec( 0,l[1]*4,w,270,curv));										// lower left
////				limbs.add(new LimbSpec( 1,l[2]*4,w,270,new double[]{1-lr,1-lr,1+lr,1+lr}));			// lower right 	-- sharpens curvature and slightly lengthens end
////				limbs.add(new LimbSpec( 0,l[3]*3,w/dw, 90,curv));									// ul joint 	-- pinch
////				limbs.add(new LimbSpec( 4,l[4]*4,w, 45,curv));										// upper right
////				limbs.add(new LimbSpec( 4,l[5]*4,w+m*2,135,new double[]{1+ul,1+ul,1+ul,1+ul}));		// upper left 	-- broadens curvature (by increasing width only)
//////				limbs.add(new LimbSpec( 4,l[5]*4,w,135,new double[]{1+m*4,1+m*4,1+m*6,1+m*6},true));
//				limbs.add(new LimbSpec( 4,l[5]*4,w,135,new double[]{1+m*3,2,1,1},true));
//			
//			} else {
//				limbs.add(new LimbSpec(-1,l[0]*4,w,  0,curv1st));
//				limbs.add(new LimbSpec( 0,l[1]*4,w,270,curv));
//				limbs.add(new LimbSpec( 1,l[2]*4,w,270,curv));
//				limbs.add(new LimbSpec( 0,l[3]*4,w, 90,curv));
//			}
//			break;
//
//		case downL_downT:	// #2
//
//			if (doMorph) {
//
////				double dw = m/2+1; 		// delta width factor
//////				double dw2 = m/1+1; 	// delta width factor2
////				double ur = m*1-0.01;	// upper right curvature change factor
////
////				limbs.add(new LimbSpec(-1,l[0]*8,w,w*dw, 90,curv1st,new double[]{0,-l[0]*8/2}));	// ur joint 	-- bulge; ll joint  -- pinch
////				limbs.add(new LimbSpec( 0,l[1]*3,w*dw,  0,curv));									// lower right 	
////				limbs.add(new LimbSpec( 0,l[2]*3,w*dw,180,curv));									// lower left   -- broadens curvature
////				limbs.add(new LimbSpec( 1,l[3]*3,w,  0,new double[]{1-ur,1-ur,1+ur,1+ur}));			// upper right  -- shaprens curvature			
//				
//			} else {
//				//yPos = yPos-l[0]*8/2;	// don't change xPos, yPos as those are only used in drawing routines
//				limbs.add(new LimbSpec(-1,l[0]*8,  w, 90,curv1st,new double[]{0,-l[0]*8/2}));
//				limbs.add(new LimbSpec( 0,l[1]*3,  w,  0,curv));
//				limbs.add(new LimbSpec( 0,l[2]*3,  w,180,curv));
//				limbs.add(new LimbSpec( 1,l[3]*3,w,180,curv));			
//			}
//			break;
//
//		case I_downT:		// #3
//
//			if (doMorph) {
//				
////				//double dw = m/2+1; 	// delta width factor
////				double dw2 = m/1+1;		// delta width factor2
////				double u = m*1-0.01;	// upper endpoint curvature change factor
////				double ll = m*0;	// lower left curvature change factor
////				
////				limbs.add(new LimbSpec(-1,l[0]*8,w,w*dw2, 90,new double[]{1,1,1,1,1-u,1-u,1+u,1+u},new double[]{0,-l[0]*8/2}));	// ur joint 	-- bulge; ll joint  -- pinch
////				limbs.add(new LimbSpec( 0,l[1]*3,w,  0,curv));										// lower right 	
////				limbs.add(new LimbSpec( 0,l[2]*3,w+m*2,180,new double[]{1+ll,1+ll,1+ll,1+ll}));		// lower left   -- broadens curvature
//								
//			} else {
//				//yPos = yPos-l[0]*8/2;
//				limbs.add(new LimbSpec(-1,l[0]*5,  w, 90,curv1st,new double[]{0,-l[0]*8/2}));
//				limbs.add(new LimbSpec( 0,l[1]*3,  w,  0,curv));
//				limbs.add(new LimbSpec( 0,l[2]*3,  w,180,curv));
//				limbs.add(new LimbSpec( 1,l[4]*4,w, 45,curv));
//				limbs.add(new LimbSpec( 1,l[5]*4,w,135,curv));
//			}
//			break;
//
//		case SEVEN_t:		// #4
//
//			if (doMorph) {
//				
////				double dw = m/2+1; 	// delta width factor
////				double lr = m*1;	// lower right curvature change factor
////				double ul = m*0;	// upper left curvature change factor
////			
////				limbs.add(new LimbSpec(-1,l[0]*5,w*dw,w/dw, 90,curv1st));								// lr and ul joint	-- pinch & bulge
//////				limbs.add(new LimbSpec( 0,l[1]*3,w,  0,new double[]{1-lr,1-lr,1+lr,1+lr}));				// lower right	-- sharpens curvature
////				limbs.add(new LimbSpec( 0,l[1]*3,w,  0,curv));											// lower right	
////				limbs.add(new LimbSpec( 0,l[2]*3,w,180,curv));
////				limbs.add(new LimbSpec( 0,l[3]*4,w,270,new double[]{1-lr,1-lr,1+lr,1+lr}));				// lower down   -- sharpens curvature
////				limbs.add(new LimbSpec( 1,l[4]*4,w+m*2,180,new double[]{1+ul,1+ul,1+ul,1+ul}));			// upper left	-- broadens curvature		
//				
//			} else {
//				limbs.add(new LimbSpec(-1,l[0]*5,w, 90,curv1st));
//				limbs.add(new LimbSpec( 0,l[1]*3,w,  0,curv));
//				limbs.add(new LimbSpec( 0,l[2]*3,w,180,curv));
//				limbs.add(new LimbSpec( 0,l[3]*4,w,270,curv));
//				limbs.add(new LimbSpec( 1,l[4]*4,w,  0,curv));
//			}
//			break;
//
//		case Y_t:			// #5
//
//			if (doMorph) {
//				
////				double dw = m/2+1; 	// delta width factor
////				double lr = m*1;	// lower right curvature change factor
////				double ul = m*0;	// upper left curvature change factor
////			
////				limbs.add(new LimbSpec(-1,l[0]*3,w*dw,w/dw, 90,curv1st));								// lr and ul joint	-- pinch & bulge
//////				limbs.add(new LimbSpec( 0,l[1]*3,w,  0,new double[]{1-lr,1-lr,1+lr,1+lr}));				// lower right	-- sharpens curvature
////				limbs.add(new LimbSpec( 0,l[1]*3,w,  0,curv));											// lower right	
////				limbs.add(new LimbSpec( 0,l[2]*3,w,180,curv));
////				limbs.add(new LimbSpec( 0,l[3]*4,w,270,new double[]{1-lr,1-lr,1+lr,1+lr}));				// lower down   -- sharpens curvature
////				limbs.add(new LimbSpec( 1,l[4]*4,w, 45,curv));
////				limbs.add(new LimbSpec( 1,l[4]*4,w+m*2,135,new double[]{1+ul,1+ul,1+ul,1+ul}));			// upper left	-- broadens curvature
//				
//			} else {
//				limbs.add(new LimbSpec(-1,l[0]*5,w, 90,curv1st));
//				limbs.add(new LimbSpec( 0,l[1]*3,w,  0,curv));
//				limbs.add(new LimbSpec( 0,l[2]*3,w,180,curv));
//				limbs.add(new LimbSpec( 0,l[3]*4,w,270,curv));
//			}
//			break;
//
//		case downL_J:		// #6
//			
//			if (doMorph) {
//				
////				double dw = m/2+1; 		// delta width factor
////				double dw2 = m/1+1; 	// delta width factor2
////				double ur = m*1-0.01;	// upper right curvature change factor
////				
////				limbs.add(new LimbSpec(-1,l[0]*8,w,w*dw, 90,curv1st,new double[]{0,-l[0]*8/2}));	// ur joint -- bulge
////				limbs.add(new LimbSpec( 0,l[1]*4,w/dw,180,curv));									// j, lower left  -- pinch 
////				limbs.add(new LimbSpec( 2,l[2]*2,w*dw2, 90,curv));									// j. lower left  -- broaden curvature
////				limbs.add(new LimbSpec( 1,l[3]*3,w,  0,new double[]{1-ur,1-ur,1+ur,1+ur}));			// upper right  -- sharpens curvature
//				
//			} else {
//				//yPos = yPos-l[0]*8/2;
//				limbs.add(new LimbSpec(-1,l[0]*8,w, 90,curv1st,new double[]{0,-l[0]*8/2}));
//				limbs.add(new LimbSpec( 0,l[1]*4,w,180,curv));
//				limbs.add(new LimbSpec( 2,l[2]*2,w, 90,curv));
//				limbs.add(new LimbSpec( 1,l[3]*3,w,180,curv));	
//			}
//			break;
//
//		case I_J:			// #7
//
//			if (doMorph) {
//				
////				double dw = m/2+1; 		// delta width factor
////				double dw2 = m/1+1;		// delta width factor2
////				double u = m*1-0.01;	// upper endpoint curvature change factor
////
////				limbs.add(new LimbSpec(-1,l[0]*8,w,w*dw2, 90,new double[]{1,1,1,1,1-u,1-u,1+u,1+u},new double[]{0,-l[0]*8/2}));	// top  -- sharpens curvature
////				limbs.add(new LimbSpec( 0,l[1]*4,w/dw,180,curv));																	// j, bottom   
////				limbs.add(new LimbSpec( 2,l[2]*2,w*dw2, 90,curv));																// j. lower left  -- broaden curvature	
//			
//			} else {
//				//yPos = yPos-l[0]*8/2;
//				limbs.add(new LimbSpec(-1,l[0]*5,w, 90,curv1st,new double[]{0,-l[0]*8/2}));
//				limbs.add(new LimbSpec( 0,l[1]*4,w,180,curv));
//				limbs.add(new LimbSpec( 2,l[2]*2,w, 90,curv));
//				limbs.add(new LimbSpec( 1,l[4]*4,w, 45,curv));
//				limbs.add(new LimbSpec( 1,l[4]*4,w,135,curv));
//			}
//			break;
//			
//		} // end switch
//		
//		spec.setAllLimbSpecs(limbs);
//		
//		// re-save spec into StimObjData db table:
//		// -- this is done in SachExptSpecGenerator:
//		
//		//dbUtil.writeStimObjData(stimObjId, s.toXml(), d.toXml());
//		
//	}
//	
//	
//	
	public void stimChooser_allCategories() {
		//THIS IS USED!!!
		
		// pre-set stim specs:
		
//		xPos = xPos+SachMathUtil.randRange(1,-1); 				// jitter position (for debugging)
//		yPos = yPos+SachMathUtil.randRange(1,-1);

		double pL = 0.0, pW = 0.0; 								// percent change in lengths and widths (when randomized) (goes from 0 to 1)
		
		if (doRand) {
			pL = 0.0;	//0.2; 0.4; TODO: these limit how much lengths and width are allowed to change!						
			pW = 0.0;	//0.4;
		}

		double lMax = 1*(1+pL), lMin = 0.9999*(1-pL);			// range for lengths
		double wMax = 1*(1+pW), wMin = 0.9999*(1-pW);			// range for widths
		double[] l = SachMathUtil.randRange(lMax,lMin,10);		// random lengths array (for up to 10 limbs)
		double w = SachMathUtil.randRange(wMax,wMin);			// random widths, across all limbs in stim
		// TODO: set these limits to global lims?
		
		// TODO: finish morph line stuff!
		// morphing:
		//doMorph = true;	// (for debugging)
		//doRand = false;	// (for debugging)

		// morphLim is passed from the SpecGenerator and will be used to specify the morphing limit for the current object category
//		double morphAmt = SachMathUtil.randRange(morphLim, 0);	// randomly chooses a morph-line value between 0 and morphLim, (morphLim should also be between 0 and 1)
		double m = morphLim; // (for debugging) -- test at max of morph lim
		//System.out.println("morphAmt="+m);
		
		// curvature:
		double[] curv1st = new double[]{1,1,1,1,1,1,1,1};		// default curvature
		double[] curv = new double[]{1,1,1,1};					// default curvature
			// for isSmoother nodes: curv[elong, bend, width???, ?]	
//	JK 11 Nov 2016	doCenterObject = false;
		doCenterObject = true;

		boolean rotateFlag = false;
		
		switch(stimCat) {
		case SEVEN_h:		// #0

//			if (doMorph) {
//				
//				double dw = m/2+1; 	// delta width factor; or for more changes at joints: dw = m/1+1;
//				double lr = m*1;	// lower right curvature change factor
//				double ul = m*0;	// upper left curvature change factor
//				limbs.add(new LimbSpec(-1,l[0]*4,w,w*dw,  0,curv1st)); 								// lr joint 	-- bulge	[maybe change ori slightly (to counter medial-axis change?): (ori +5*m)]
//				limbs.add(new LimbSpec( 0,l[1]*4,w,270,curv));										// lower left
//				limbs.add(new LimbSpec( 1,l[2]*4,w,270,new double[]{1-lr,1-lr,1+lr,1+lr}));			// lower right 	-- sharpens curvature and slightly lengthens end
//				limbs.add(new LimbSpec( 0,l[3]*4,w/dw, 90,curv));									// ul joint 	-- pinch
//				limbs.add(new LimbSpec( 4,l[4]*3,w+m*2,180,new double[]{1+ul,1+ul,1+ul,1+ul}));		// upper left 	-- broadens curvature (by increasing width only)			
//
//			} else { // exemplar
				limbs.add(new LimbSpec(-1,l[0]*4,w,  0,curv1st));
				limbs.add(new LimbSpec( 0,l[1]*4,w,270,curv));
				limbs.add(new LimbSpec( 1,l[2]*4,w,270,curv));
				limbs.add(new LimbSpec( 0,l[3]*4,w, 90,curv));
				limbs.add(new LimbSpec( 4,l[4]*3,w,180,curv));
//			}		
			break;

		case Y_h:			// #1

//			if (doMorph) {
//
//				double dw = m/2+1; 	// delta width factor
//				double lr = m*1;	// lower right curvature change factor
//				double ul = m*0;	// upper left curvature change factor
//				limbs.add(new LimbSpec(-1,l[0]*4,w,w*dw,  0,curv1st)); 								// lr joint 	-- bulge	[maybe change ori slightly (to counter medial-axis change?): (ori +5*m)]
//				limbs.add(new LimbSpec( 0,l[1]*4,w,270,curv));										// lower left
//				limbs.add(new LimbSpec( 1,l[2]*4,w,270,new double[]{1-lr,1-lr,1+lr,1+lr}));			// lower right 	-- sharpens curvature and slightly lengthens end
//				limbs.add(new LimbSpec( 0,l[3]*3,w/dw, 90,curv));									// ul joint 	-- pinch
//				limbs.add(new LimbSpec( 4,l[4]*4,w, 45,curv));										// upper right
//				limbs.add(new LimbSpec( 4,l[5]*4,w+m*2,135,new double[]{1+ul,1+ul,1+ul,1+ul}));		// upper left 	-- broadens curvature (by increasing width only)
////				limbs.add(new LimbSpec( 4,l[5]*4,w,135,new double[]{1+m*4,1+m*4,1+m*6,1+m*6},true));
////				limbs.add(new LimbSpec( 4,l[5]*4,w,135,new double[]{1+m*3,2,1,1},true));
//			
//			} else {
				limbs.add(new LimbSpec(-1,l[0]*4,w,  0,curv1st));
				limbs.add(new LimbSpec( 0,l[1]*4,w,270,curv));
				limbs.add(new LimbSpec( 1,l[2]*4,w,270,curv));
				limbs.add(new LimbSpec( 0,l[3]*3,w, 90,curv));
				limbs.add(new LimbSpec( 4,l[4]*4,w, 45,curv));
				limbs.add(new LimbSpec( 4,l[5]*4,w,135,curv));
//			}
			break;

		case downL_downT:	// #2

//			if (doMorph) {
//
//				double dw = m/2+1; 		// delta width factor
////				double dw2 = m/1+1; 	// delta width factor2
//				double ur = m*1-0.01;	// upper right curvature change factor
//
//				limbs.add(new LimbSpec(-1,l[0]*8,w,w*dw, 90,curv1st,new double[]{0,-l[0]*8/2}));	// ur joint 	-- bulge; ll joint  -- pinch
//				limbs.add(new LimbSpec( 0,l[1]*3,w*dw,  0,curv));									// lower right 	
//				limbs.add(new LimbSpec( 0,l[2]*3,w*dw,180,curv));									// lower left   -- broadens curvature
//				limbs.add(new LimbSpec( 1,l[3]*3,w,  0,new double[]{1-ur,1-ur,1+ur,1+ur}));			// upper right  -- shaprens curvature			
//
//				// TODO: fix bottom morph, make the whole width of bottom enlarge, but keep upward branch small
//				
//			} else {
				//yPos = yPos-l[0]*8/2;	// don't change xPos, yPos as those are only used in drawing routines
				limbs.add(new LimbSpec(-1,l[0]*8,  w, 90,curv1st,new double[]{0,-l[0]*8/2}));
				limbs.add(new LimbSpec( 0,l[1]*3,  w,  0,curv));
				limbs.add(new LimbSpec( 0,l[2]*3,  w,180,curv));
				limbs.add(new LimbSpec( 1,l[3]*3,w,  0,curv));			
//			}
			break;

		case I_downT:		// #3

//			if (doMorph) {
//				
//				//double dw = m/2+1; 	// delta width factor
//				double dw2 = m/1+1;		// delta width factor2
//				double u = m*1-0.01;	// upper endpoint curvature change factor
//				double ll = m*0;	// lower left curvature change factor
//				
//				limbs.add(new LimbSpec(-1,l[0]*8,w,w*dw2, 90,new double[]{1,1,1,1,1-u,1-u,1+u,1+u},new double[]{0,-l[0]*8/2}));	// ur joint 	-- bulge; ll joint  -- pinch
//				limbs.add(new LimbSpec( 0,l[1]*3,w,  0,curv));										// lower right 	
//				limbs.add(new LimbSpec( 0,l[2]*3,w+m*2,180,new double[]{1+ll,1+ll,1+ll,1+ll}));		// lower left   -- broadens curvature
//				
//				// TODO: finish bottom morph (see #2 above)
//				
//			} else {
				//yPos = yPos-l[0]*8/2;
				limbs.add(new LimbSpec(-1,l[0]*8,  w, 90,curv1st,new double[]{0,-l[0]*8/2}));
				limbs.add(new LimbSpec( 0,l[1]*3,  w,  0,curv));
				limbs.add(new LimbSpec( 0,l[2]*3,  w,180,curv));
//			}
			break;

		case SEVEN_t:		// #4

//			if (doMorph) {
//				
//				double dw = m/2+1; 	// delta width factor
//				double lr = m*1;	// lower right curvature change factor
//				double ul = m*0;	// upper left curvature change factor
//			
//				limbs.add(new LimbSpec(-1,l[0]*4,w*dw,w/dw, 90,curv1st));								// lr and ul joint	-- pinch & bulge
////				limbs.add(new LimbSpec( 0,l[1]*3,w,  0,new double[]{1-lr,1-lr,1+lr,1+lr}));				// lower right	-- sharpens curvature
//				limbs.add(new LimbSpec( 0,l[1]*3,w,  0,curv));											// lower right	
//				limbs.add(new LimbSpec( 0,l[2]*3,w,180,curv));
//				limbs.add(new LimbSpec( 0,l[3]*4,w,270,new double[]{1-lr,1-lr,1+lr,1+lr}));				// lower down   -- sharpens curvature
//				limbs.add(new LimbSpec( 1,l[4]*4,w+m*2,180,new double[]{1+ul,1+ul,1+ul,1+ul}));			// upper left	-- broadens curvature		
//				
//			} else {
				limbs.add(new LimbSpec(-1,l[0]*4,w, 90,curv1st));
				limbs.add(new LimbSpec( 0,l[1]*3,w,  0,curv));
				limbs.add(new LimbSpec( 0,l[2]*3,w,180,curv));
				limbs.add(new LimbSpec( 0,l[3]*4,w,270,curv));
				limbs.add(new LimbSpec( 1,l[4]*4,w,180,curv));
//			}
			break;

		case Y_t:			// #5

//			if (doMorph) {
//				
//				double dw = m/2+1; 	// delta width factor
//				double lr = m*1;	// lower right curvature change factor
//				double ul = m*0;	// upper left curvature change factor
//			
//				limbs.add(new LimbSpec(-1,l[0]*3,w*dw,w/dw, 90,curv1st));								// lr and ul joint	-- pinch & bulge
////				limbs.add(new LimbSpec( 0,l[1]*3,w,  0,new double[]{1-lr,1-lr,1+lr,1+lr}));				// lower right	-- sharpens curvature
//				limbs.add(new LimbSpec( 0,l[1]*3,w,  0,curv));											// lower right	
//				limbs.add(new LimbSpec( 0,l[2]*3,w,180,curv));
//				limbs.add(new LimbSpec( 0,l[3]*4,w,270,new double[]{1-lr,1-lr,1+lr,1+lr}));				// lower down   -- sharpens curvature
//				limbs.add(new LimbSpec( 1,l[4]*4,w, 45,curv));
//				limbs.add(new LimbSpec( 1,l[4]*4,w+m*2,135,new double[]{1+ul,1+ul,1+ul,1+ul}));			// upper left	-- broadens curvature
//				
//			} else {
				limbs.add(new LimbSpec(-1,l[0]*3,w, 90,curv1st));
				limbs.add(new LimbSpec( 0,l[1]*3,w,  0,curv));
				limbs.add(new LimbSpec( 0,l[2]*3,w,180,curv));
				limbs.add(new LimbSpec( 0,l[3]*4,w,270,curv));
				limbs.add(new LimbSpec( 1,l[4]*4,w, 45,curv));
				limbs.add(new LimbSpec( 1,l[4]*4,w,135,curv));
//			}
			break;

		case downL_J:		// #6
			
//			if (doMorph) {
//				
//				double dw = m/2+1; 		// delta width factor
//				double dw2 = m/1+1; 	// delta width factor2
//				double ur = m*1-0.01;	// upper right curvature change factor
//				
//				limbs.add(new LimbSpec(-1,l[0]*8,w,w*dw, 90,curv1st,new double[]{0,-l[0]*8/2}));	// ur joint -- bulge
//				limbs.add(new LimbSpec( 0,l[1]*4,w/dw,180,curv));									// j, lower left  -- pinch 
//				limbs.add(new LimbSpec( 2,l[2]*2,w*dw2, 90,curv));									// j. lower left  -- broaden curvature
//				limbs.add(new LimbSpec( 1,l[3]*3,w,  0,new double[]{1-ur,1-ur,1+ur,1+ur}));			// upper right  -- sharpens curvature
//				
//			} else {
				//yPos = yPos-l[0]*8/2;
				limbs.add(new LimbSpec(-1,l[0]*8,w, 90,curv1st,new double[]{0,-l[0]*8/2}));
				limbs.add(new LimbSpec( 0,l[1]*4,w,180,curv));
				limbs.add(new LimbSpec( 2,l[2]*2,w, 90,curv));
				limbs.add(new LimbSpec( 1,l[3]*3,w,  0,curv));	
//			}
			break;

		case I_J:			// #7

//			if (doMorph) {
//				
//				double dw = m/2+1; 		// delta width factor
//				double dw2 = m/1+1;		// delta width factor2
//				double u = m*1-0.01;	// upper endpoint curvature change factor
//
//				limbs.add(new LimbSpec(-1,l[0]*8,w,w*dw2, 90,new double[]{1,1,1,1,1-u,1-u,1+u,1+u},new double[]{0,-l[0]*8/2}));	// top  -- sharpens curvature
//				limbs.add(new LimbSpec( 0,l[1]*4,w/dw,180,curv));																	// j, bottom   
//				limbs.add(new LimbSpec( 2,l[2]*2,w*dw2, 90,curv));																// j. lower left  -- broaden curvature	
//			
//			} else {
				//yPos = yPos-l[0]*8/2;
				limbs.add(new LimbSpec(-1,l[0]*8,w, 90,curv1st,new double[]{0,-l[0]*8/2}));
				limbs.add(new LimbSpec( 0,l[1]*4,w,180,curv));
				limbs.add(new LimbSpec( 2,l[2]*2,w, 90,curv));
//			}
			break;
			
		
		// TODO: need to finish up the morph lines at some point for the second 8 stimuli	
		// (downL_h, I_h, SEVEN_downT, Y_downT, downL_t, I_t, SEVEN_J, Y-J)
		
		case downL_h:		// #8

//			if (doMorph) {
				
//				double dw = m/2+1; 	// delta width factor; or for more changes at joints: dw = m/1+1;
//				double lr = m*1;	// lower right curvature change factor
//				double ul = m*0;	// upper left curvature change factor
//				limbs.add(new LimbSpec(-1,l[0]*4,w,w*dw,  0,curv1st)); 								// lr joint 	-- bulge	[maybe change ori slightly (to counter medial-axis change?): (ori +5*m)]
//				limbs.add(new LimbSpec( 0,l[1]*4,w,270,curv));										// lower left
//				limbs.add(new LimbSpec( 1,l[2]*4,w,270,new double[]{1-lr,1-lr,1+lr,1+lr}));			// lower right 	-- sharpens curvature and slightly lengthens end
//				limbs.add(new LimbSpec( 0,l[3]*4,w/dw, 90,curv));									// ul joint 	-- pinch
//				limbs.add(new LimbSpec( 4,l[4]*3,w+m*2,180,new double[]{1+ul,1+ul,1+ul,1+ul}));		// upper left 	-- broadens curvature (by increasing width only)			

//			} else { // exemplar
				limbs.add(new LimbSpec(-1,l[0]*4,w,  0,curv1st));
				limbs.add(new LimbSpec( 0,l[1]*4,w,270,curv));
				limbs.add(new LimbSpec( 1,l[2]*4,w,270,curv));
				limbs.add(new LimbSpec( 0,l[3]*4,w, 90,curv));
				limbs.add(new LimbSpec( 4,l[4]*3,w,  0,curv));
//			}		
			break;

		case I_h:			// #9

//			if (doMorph) {

//				double dw = m/2+1; 	// delta width factor
//				double lr = m*1;	// lower right curvature change factor
//				double ul = m*0;	// upper left curvature change factor
//				limbs.add(new LimbSpec(-1,l[0]*4,w,w*dw,  0,curv1st)); 								// lr joint 	-- bulge	[maybe change ori slightly (to counter medial-axis change?): (ori +5*m)]
//				limbs.add(new LimbSpec( 0,l[1]*4,w,270,curv));										// lower left
//				limbs.add(new LimbSpec( 1,l[2]*4,w,270,new double[]{1-lr,1-lr,1+lr,1+lr}));			// lower right 	-- sharpens curvature and slightly lengthens end
//				limbs.add(new LimbSpec( 0,l[3]*3,w/dw, 90,curv));									// ul joint 	-- pinch
//				limbs.add(new LimbSpec( 4,l[4]*4,w, 45,curv));										// upper right
//				limbs.add(new LimbSpec( 4,l[5]*4,w+m*2,135,new double[]{1+ul,1+ul,1+ul,1+ul}));		// upper left 	-- broadens curvature (by increasing width only)
////				limbs.add(new LimbSpec( 4,l[5]*4,w,135,new double[]{1+m*4,1+m*4,1+m*6,1+m*6},true));
//				limbs.add(new LimbSpec( 4,l[5]*4,w,135,new double[]{1+m*3,2,1,1},true));
			
//			} else {
				limbs.add(new LimbSpec(-1,l[0]*4,w,  0,curv1st));
				limbs.add(new LimbSpec( 0,l[1]*4,w,270,curv));
				limbs.add(new LimbSpec( 1,l[2]*4,w,270,curv));
				limbs.add(new LimbSpec( 0,l[3]*4,w, 90,curv));
//			}
			break;

		case SEVEN_downT:	// #10

//			if (doMorph) {

//				double dw = m/2+1; 		// delta width factor
////				double dw2 = m/1+1; 	// delta width factor2
//				double ur = m*1-0.01;	// upper right curvature change factor
//
//				limbs.add(new LimbSpec(-1,l[0]*8,w,w*dw, 90,curv1st,new double[]{0,-l[0]*8/2}));	// ur joint 	-- bulge; ll joint  -- pinch
//				limbs.add(new LimbSpec( 0,l[1]*3,w*dw,  0,curv));									// lower right 	
//				limbs.add(new LimbSpec( 0,l[2]*3,w*dw,180,curv));									// lower left   -- broadens curvature
//				limbs.add(new LimbSpec( 1,l[3]*3,w,  0,new double[]{1-ur,1-ur,1+ur,1+ur}));			// upper right  -- shaprens curvature			
				
//			} else {
				//yPos = yPos-l[0]*8/2;	// don't change xPos, yPos as those are only used in drawing routines
				limbs.add(new LimbSpec(-1,l[0]*8,  w, 90,curv1st,new double[]{0,-l[0]*8/2}));
				limbs.add(new LimbSpec( 0,l[1]*3,  w,  0,curv));
				limbs.add(new LimbSpec( 0,l[2]*3,  w,180,curv));
				limbs.add(new LimbSpec( 1,l[3]*3,w,180,curv));			
//			}
			break;

		case Y_downT:		// #11

//			if (doMorph) {
				
//				//double dw = m/2+1; 	// delta width factor
//				double dw2 = m/1+1;		// delta width factor2
//				double u = m*1-0.01;	// upper endpoint curvature change factor
//				double ll = m*0;	// lower left curvature change factor
//				
//				limbs.add(new LimbSpec(-1,l[0]*8,w,w*dw2, 90,new double[]{1,1,1,1,1-u,1-u,1+u,1+u},new double[]{0,-l[0]*8/2}));	// ur joint 	-- bulge; ll joint  -- pinch
//				limbs.add(new LimbSpec( 0,l[1]*3,w,  0,curv));										// lower right 	
//				limbs.add(new LimbSpec( 0,l[2]*3,w+m*2,180,new double[]{1+ll,1+ll,1+ll,1+ll}));		// lower left   -- broadens curvature
								
//			} else {
				//yPos = yPos-l[0]*8/2;
				limbs.add(new LimbSpec(-1,l[0]*7,  w, 90,curv1st,new double[]{0,-l[0]*8/2}));
				limbs.add(new LimbSpec( 0,l[1]*3,  w,  0,curv));
				limbs.add(new LimbSpec( 0,l[2]*3,  w,180,curv));
				limbs.add(new LimbSpec( 1,l[4]*4,w, 45,curv));
				limbs.add(new LimbSpec( 1,l[5]*4,w,135,curv));
//			}
			break;

		case downL_t:		// #12

//			if (doMorph) {
				
//				double dw = m/2+1; 	// delta width factor
//				double lr = m*1;	// lower right curvature change factor
//				double ul = m*0;	// upper left curvature change factor
//			
//				limbs.add(new LimbSpec(-1,l[0]*5,w*dw,w/dw, 90,curv1st));								// lr and ul joint	-- pinch & bulge
////				limbs.add(new LimbSpec( 0,l[1]*3,w,  0,new double[]{1-lr,1-lr,1+lr,1+lr}));				// lower right	-- sharpens curvature
//				limbs.add(new LimbSpec( 0,l[1]*3,w,  0,curv));											// lower right	
//				limbs.add(new LimbSpec( 0,l[2]*3,w,180,curv));
//				limbs.add(new LimbSpec( 0,l[3]*4,w,270,new double[]{1-lr,1-lr,1+lr,1+lr}));				// lower down   -- sharpens curvature
//				limbs.add(new LimbSpec( 1,l[4]*4,w+m*2,180,new double[]{1+ul,1+ul,1+ul,1+ul}));			// upper left	-- broadens curvature		
				
//			} else {
				limbs.add(new LimbSpec(-1,l[0]*4,w, 90,curv1st));
				limbs.add(new LimbSpec( 0,l[1]*3,w,  0,curv));
				limbs.add(new LimbSpec( 0,l[2]*3,w,180,curv));
				limbs.add(new LimbSpec( 0,l[3]*4,w,270,curv));
				limbs.add(new LimbSpec( 1,l[4]*4,w,  0,curv));
//			}
			break;

		case I_t:			// #13

//			if (doMorph) {
				
//				double dw = m/2+1; 	// delta width factor
//				double lr = m*1;	// lower right curvature change factor
//				double ul = m*0;	// upper left curvature change factor
//			
//				limbs.add(new LimbSpec(-1,l[0]*3,w*dw,w/dw, 90,curv1st));								// lr and ul joint	-- pinch & bulge
////				limbs.add(new LimbSpec( 0,l[1]*3,w,  0,new double[]{1-lr,1-lr,1+lr,1+lr}));				// lower right	-- sharpens curvature
//				limbs.add(new LimbSpec( 0,l[1]*3,w,  0,curv));											// lower right	
//				limbs.add(new LimbSpec( 0,l[2]*3,w,180,curv));
//				limbs.add(new LimbSpec( 0,l[3]*4,w,270,new double[]{1-lr,1-lr,1+lr,1+lr}));				// lower down   -- sharpens curvature
//				limbs.add(new LimbSpec( 1,l[4]*4,w, 45,curv));
//				limbs.add(new LimbSpec( 1,l[4]*4,w+m*2,135,new double[]{1+ul,1+ul,1+ul,1+ul}));			// upper left	-- broadens curvature
				
//			} else {
				limbs.add(new LimbSpec(-1,l[0]*4,w, 90,curv1st));
				limbs.add(new LimbSpec( 0,l[1]*3,w,  0,curv));
				limbs.add(new LimbSpec( 0,l[2]*3,w,180,curv));
				limbs.add(new LimbSpec( 0,l[3]*4,w,270,curv));
//			}
			break;

		case SEVEN_J:		// #14
			
//			if (doMorph) {
				
//				double dw = m/2+1; 		// delta width factor
//				double dw2 = m/1+1; 	// delta width factor2
//				double ur = m*1-0.01;	// upper right curvature change factor
//				
//				limbs.add(new LimbSpec(-1,l[0]*8,w,w*dw, 90,curv1st,new double[]{0,-l[0]*8/2}));	// ur joint -- bulge
//				limbs.add(new LimbSpec( 0,l[1]*4,w/dw,180,curv));									// j, lower left  -- pinch 
//				limbs.add(new LimbSpec( 2,l[2]*2,w*dw2, 90,curv));									// j. lower left  -- broaden curvature
//				limbs.add(new LimbSpec( 1,l[3]*3,w,  0,new double[]{1-ur,1-ur,1+ur,1+ur}));			// upper right  -- sharpens curvature
				
//			} else {
				//yPos = yPos-l[0]*8/2;
				limbs.add(new LimbSpec(-1,l[0]*8,w, 90,curv1st,new double[]{0,-l[0]*8/2}));
				limbs.add(new LimbSpec( 0,l[1]*4,w,180,curv));
				limbs.add(new LimbSpec( 2,l[2]*2,w, 90,curv));
				limbs.add(new LimbSpec( 1,l[3]*3,w,180,curv));	
//			}
			break;

		case Y_J:			// #15

//			if (doMorph) {
				
//				double dw = m/2+1; 		// delta width factor
//				double dw2 = m/1+1;		// delta width factor2
//				double u = m*1-0.01;	// upper endpoint curvature change factor
//
//				limbs.add(new LimbSpec(-1,l[0]*8,w,w*dw2, 90,new double[]{1,1,1,1,1-u,1-u,1+u,1+u},new double[]{0,-l[0]*8/2}));	// top  -- sharpens curvature
//				limbs.add(new LimbSpec( 0,l[1]*4,w/dw,180,curv));																	// j, bottom   
//				limbs.add(new LimbSpec( 2,l[2]*2,w*dw2, 90,curv));																// j. lower left  -- broaden curvature	
			
//			} else {
				//yPos = yPos-l[0]*8/2;
				limbs.add(new LimbSpec(-1,l[0]*7,w, 90,curv1st,new double[]{0,-l[0]*8/2}));
				limbs.add(new LimbSpec( 0,l[1]*4,w,180,curv));
				limbs.add(new LimbSpec( 2,l[2]*2,w, 90,curv));
				limbs.add(new LimbSpec( 1,l[4]*4,w, 45,curv));
				limbs.add(new LimbSpec( 1,l[4]*4,w,135,curv));
//			}
			break;
		case SEVEN_h_ud:		// #16
			rotateFlag = true;
//			if (doMorph) {
				
				double dw = m/2+1; 	// delta width factor; or for more changes at joints: dw = m/1+1;
				double lr = m*1;	// lower right curvature change factor
				double ul = m*0;	// upper left curvature change factor
				limbs.add(new LimbSpec(-1,l[0]*4,w,w*dw,  0,curv1st)); 								// lr joint 	-- bulge	[maybe change ori slightly (to counter medial-axis change?): (ori +5*m)]
				limbs.add(new LimbSpec( 0,l[1]*4,w,270,curv));										// lower left
				limbs.add(new LimbSpec( 1,l[2]*4,w,270,new double[]{1-lr,1-lr,1+lr,1+lr}));			// lower right 	-- sharpens curvature and slightly lengthens end
				limbs.add(new LimbSpec( 0,l[3]*4,w/dw, 90,curv));									// ul joint 	-- pinch
				limbs.add(new LimbSpec( 4,l[4]*3,w+m*2,180,new double[]{1+ul,1+ul,1+ul,1+ul}));		// upper left 	-- broadens curvature (by increasing width only)			

//			} else { // exemplar
				limbs.add(new LimbSpec(-1,l[0]*4,w,  0,curv1st));
				limbs.add(new LimbSpec( 0,l[1]*4,w,270,curv));
				limbs.add(new LimbSpec( 1,l[2]*4,w,270,curv));
				limbs.add(new LimbSpec( 0,l[3]*4,w, 90,curv));
				limbs.add(new LimbSpec( 4,l[4]*3,w,180,curv));
//			}		
			break;

		case Y_h_ud:		// #17
			rotateFlag = true;

//			if (doMorph) {

//				double dw = m/2+1; 	// delta width factor
//				double lr = m*1;	// lower right curvature change factor
//				double ul = m*0;	// upper left curvature change factor
//				limbs.add(new LimbSpec(-1,l[0]*4,w,w*dw,  0,curv1st)); 								// lr joint 	-- bulge	[maybe change ori slightly (to counter medial-axis change?): (ori +5*m)]
//				limbs.add(new LimbSpec( 0,l[1]*4,w,270,curv));										// lower left
//				limbs.add(new LimbSpec( 1,l[2]*4,w,270,new double[]{1-lr,1-lr,1+lr,1+lr}));			// lower right 	-- sharpens curvature and slightly lengthens end
//				limbs.add(new LimbSpec( 0,l[3]*3,w/dw, 90,curv));									// ul joint 	-- pinch
//				limbs.add(new LimbSpec( 4,l[4]*4,w, 45,curv));										// upper right
//				limbs.add(new LimbSpec( 4,l[5]*4,w+m*2,135,new double[]{1+ul,1+ul,1+ul,1+ul}));		// upper left 	-- broadens curvature (by increasing width only)
////				limbs.add(new LimbSpec( 4,l[5]*4,w,135,new double[]{1+m*4,1+m*4,1+m*6,1+m*6},true));
////				limbs.add(new LimbSpec( 4,l[5]*4,w,135,new double[]{1+m*3,2,1,1},true));
//			
//			} else {
				limbs.add(new LimbSpec(-1,l[0]*4,w,  0,curv1st));
				limbs.add(new LimbSpec( 0,l[1]*4,w,270,curv));
				limbs.add(new LimbSpec( 1,l[2]*4,w,270,curv));
				limbs.add(new LimbSpec( 0,l[3]*3,w, 90,curv));
				limbs.add(new LimbSpec( 4,l[4]*4,w, 45,curv));
				limbs.add(new LimbSpec( 4,l[5]*4,w,135,curv));
//			}
			break;

		case downL_downT_ud:		// #18
			rotateFlag = true;

//			if (doMorph) {
//
//				double dw = m/2+1; 		// delta width factor
////				double dw2 = m/1+1; 	// delta width factor2
//				double ur = m*1-0.01;	// upper right curvature change factor
//
//				limbs.add(new LimbSpec(-1,l[0]*8,w,w*dw, 90,curv1st,new double[]{0,-l[0]*8/2}));	// ur joint 	-- bulge; ll joint  -- pinch
//				limbs.add(new LimbSpec( 0,l[1]*3,w*dw,  0,curv));									// lower right 	
//				limbs.add(new LimbSpec( 0,l[2]*3,w*dw,180,curv));									// lower left   -- broadens curvature
//				limbs.add(new LimbSpec( 1,l[3]*3,w,  0,new double[]{1-ur,1-ur,1+ur,1+ur}));			// upper right  -- shaprens curvature			
//
//				// TODO: fix bottom morph, make the whole width of bottom enlarge, but keep upward branch small
//				
//			} else {
				//yPos = yPos-l[0]*8/2;	// don't change xPos, yPos as those are only used in drawing routines
				limbs.add(new LimbSpec(-1,l[0]*8,  w, 90,curv1st,new double[]{0,-l[0]*8/2}));
				limbs.add(new LimbSpec( 0,l[1]*3,  w,  0,curv));
				limbs.add(new LimbSpec( 0,l[2]*3,  w,180,curv));
				limbs.add(new LimbSpec( 1,l[3]*3,w,  0,curv));			
//			}
			break;

		case I_downT_ud:		// #19
			rotateFlag = true;

//			if (doMorph) {
//				
//				//double dw = m/2+1; 	// delta width factor
//				double dw2 = m/1+1;		// delta width factor2
//				double u = m*1-0.01;	// upper endpoint curvature change factor
//				double ll = m*0;	// lower left curvature change factor
//				
//				limbs.add(new LimbSpec(-1,l[0]*8,w,w*dw2, 90,new double[]{1,1,1,1,1-u,1-u,1+u,1+u},new double[]{0,-l[0]*8/2}));	// ur joint 	-- bulge; ll joint  -- pinch
//				limbs.add(new LimbSpec( 0,l[1]*3,w,  0,curv));										// lower right 	
//				limbs.add(new LimbSpec( 0,l[2]*3,w+m*2,180,new double[]{1+ll,1+ll,1+ll,1+ll}));		// lower left   -- broadens curvature
//				
//				// TODO: finish bottom morph (see #2 above)
//				
//			} else {
				//yPos = yPos-l[0]*8/2;
				limbs.add(new LimbSpec(-1,l[0]*8,  w, 90,curv1st,new double[]{0,-l[0]*8/2}));
				limbs.add(new LimbSpec( 0,l[1]*3,  w,  0,curv));
				limbs.add(new LimbSpec( 0,l[2]*3,  w,180,curv));
//			}
			break;

		case SEVEN_t_ud:		// #20
			rotateFlag = true;

//			if (doMorph) {
//				
//				double dw = m/2+1; 	// delta width factor
//				double lr = m*1;	// lower right curvature change factor
//				double ul = m*0;	// upper left curvature change factor
//			
//				limbs.add(new LimbSpec(-1,l[0]*4,w*dw,w/dw, 90,curv1st));								// lr and ul joint	-- pinch & bulge
////				limbs.add(new LimbSpec( 0,l[1]*3,w,  0,new double[]{1-lr,1-lr,1+lr,1+lr}));				// lower right	-- sharpens curvature
//				limbs.add(new LimbSpec( 0,l[1]*3,w,  0,curv));											// lower right	
//				limbs.add(new LimbSpec( 0,l[2]*3,w,180,curv));
//				limbs.add(new LimbSpec( 0,l[3]*4,w,270,new double[]{1-lr,1-lr,1+lr,1+lr}));				// lower down   -- sharpens curvature
//				limbs.add(new LimbSpec( 1,l[4]*4,w+m*2,180,new double[]{1+ul,1+ul,1+ul,1+ul}));			// upper left	-- broadens curvature		
//				
//			} else {
				limbs.add(new LimbSpec(-1,l[0]*4,w, 90,curv1st));
				limbs.add(new LimbSpec( 0,l[1]*3,w,  0,curv));
				limbs.add(new LimbSpec( 0,l[2]*3,w,180,curv));
				limbs.add(new LimbSpec( 0,l[3]*4,w,270,curv));
				limbs.add(new LimbSpec( 1,l[4]*4,w,180,curv));
//			}
			break;

		case Y_t_ud:		// #21
			rotateFlag = true;

//			if (doMorph) {
//				
//				double dw = m/2+1; 	// delta width factor
//				double lr = m*1;	// lower right curvature change factor
//				double ul = m*0;	// upper left curvature change factor
//			
//				limbs.add(new LimbSpec(-1,l[0]*3,w*dw,w/dw, 90,curv1st));								// lr and ul joint	-- pinch & bulge
////				limbs.add(new LimbSpec( 0,l[1]*3,w,  0,new double[]{1-lr,1-lr,1+lr,1+lr}));				// lower right	-- sharpens curvature
//				limbs.add(new LimbSpec( 0,l[1]*3,w,  0,curv));											// lower right	
//				limbs.add(new LimbSpec( 0,l[2]*3,w,180,curv));
//				limbs.add(new LimbSpec( 0,l[3]*4,w,270,new double[]{1-lr,1-lr,1+lr,1+lr}));				// lower down   -- sharpens curvature
//				limbs.add(new LimbSpec( 1,l[4]*4,w, 45,curv));
//				limbs.add(new LimbSpec( 1,l[4]*4,w+m*2,135,new double[]{1+ul,1+ul,1+ul,1+ul}));			// upper left	-- broadens curvature
//				
//			} else {
				limbs.add(new LimbSpec(-1,l[0]*3,w, 90,curv1st));
				limbs.add(new LimbSpec( 0,l[1]*3,w,  0,curv));
				limbs.add(new LimbSpec( 0,l[2]*3,w,180,curv));
				limbs.add(new LimbSpec( 0,l[3]*4,w,270,curv));
				limbs.add(new LimbSpec( 1,l[4]*4,w, 45,curv));
				limbs.add(new LimbSpec( 1,l[4]*4,w,135,curv));
//			}
			break;

		case downL_J_ud:		// #22
			rotateFlag = true;
			
//			if (doMorph) {
//				
//				double dw = m/2+1; 		// delta width factor
//				double dw2 = m/1+1; 	// delta width factor2
//				double ur = m*1-0.01;	// upper right curvature change factor
//				
//				limbs.add(new LimbSpec(-1,l[0]*8,w,w*dw, 90,curv1st,new double[]{0,-l[0]*8/2}));	// ur joint -- bulge
//				limbs.add(new LimbSpec( 0,l[1]*4,w/dw,180,curv));									// j, lower left  -- pinch 
//				limbs.add(new LimbSpec( 2,l[2]*2,w*dw2, 90,curv));									// j. lower left  -- broaden curvature
//				limbs.add(new LimbSpec( 1,l[3]*3,w,  0,new double[]{1-ur,1-ur,1+ur,1+ur}));			// upper right  -- sharpens curvature
//				
//			} else {
				//yPos = yPos-l[0]*8/2;
				limbs.add(new LimbSpec(-1,l[0]*8,w, 90,curv1st,new double[]{0,-l[0]*8/2}));
				limbs.add(new LimbSpec( 0,l[1]*4,w,180,curv));
				limbs.add(new LimbSpec( 2,l[2]*2,w, 90,curv));
				limbs.add(new LimbSpec( 1,l[3]*3,w,  0,curv));	
//			}
			break;

		case I_J_ud:		// #23
			rotateFlag = true;

//			if (doMorph) {
//				
//				double dw = m/2+1; 		// delta width factor
//				double dw2 = m/1+1;		// delta width factor2
//				double u = m*1-0.01;	// upper endpoint curvature change factor
//
//				limbs.add(new LimbSpec(-1,l[0]*8,w,w*dw2, 90,new double[]{1,1,1,1,1-u,1-u,1+u,1+u},new double[]{0,-l[0]*8/2}));	// top  -- sharpens curvature
//				limbs.add(new LimbSpec( 0,l[1]*4,w/dw,180,curv));																	// j, bottom   
//				limbs.add(new LimbSpec( 2,l[2]*2,w*dw2, 90,curv));																// j. lower left  -- broaden curvature	
//			
//			} else {
				//yPos = yPos-l[0]*8/2;
				limbs.add(new LimbSpec(-1,l[0]*8,w, 90,curv1st,new double[]{0,-l[0]*8/2}));
				limbs.add(new LimbSpec( 0,l[1]*4,w,180,curv));
				limbs.add(new LimbSpec( 2,l[2]*2,w, 90,curv));
//			}
			break;
			
		
		// TODO: need to finish up the morph lines at some point for the second 8 stimuli	
		// (downL_h, I_h, SEVEN_downT, Y_downT, downL_t, I_t, SEVEN_J, Y-J)
		
		case downL_h_ud:		// #24
			rotateFlag = true;

//			if (doMorph) {
				
//				double dw = m/2+1; 	// delta width factor; or for more changes at joints: dw = m/1+1;
//				double lr = m*1;	// lower right curvature change factor
//				double ul = m*0;	// upper left curvature change factor
//				limbs.add(new LimbSpec(-1,l[0]*4,w,w*dw,  0,curv1st)); 								// lr joint 	-- bulge	[maybe change ori slightly (to counter medial-axis change?): (ori +5*m)]
//				limbs.add(new LimbSpec( 0,l[1]*4,w,270,curv));										// lower left
//				limbs.add(new LimbSpec( 1,l[2]*4,w,270,new double[]{1-lr,1-lr,1+lr,1+lr}));			// lower right 	-- sharpens curvature and slightly lengthens end
//				limbs.add(new LimbSpec( 0,l[3]*4,w/dw, 90,curv));									// ul joint 	-- pinch
//				limbs.add(new LimbSpec( 4,l[4]*3,w+m*2,180,new double[]{1+ul,1+ul,1+ul,1+ul}));		// upper left 	-- broadens curvature (by increasing width only)			

//			} else { // exemplar
				limbs.add(new LimbSpec(-1,l[0]*4,w,  0,curv1st));
				limbs.add(new LimbSpec( 0,l[1]*4,w,270,curv));
				limbs.add(new LimbSpec( 1,l[2]*4,w,270,curv));
				limbs.add(new LimbSpec( 0,l[3]*4,w, 90,curv));
				limbs.add(new LimbSpec( 4,l[4]*3,w,  0,curv));
//			}		
			break;

		case I_h_ud:		// #25
			rotateFlag = true;

//			if (doMorph) {

//				double dw = m/2+1; 	// delta width factor
//				double lr = m*1;	// lower right curvature change factor
//				double ul = m*0;	// upper left curvature change factor
//				limbs.add(new LimbSpec(-1,l[0]*4,w,w*dw,  0,curv1st)); 								// lr joint 	-- bulge	[maybe change ori slightly (to counter medial-axis change?): (ori +5*m)]
//				limbs.add(new LimbSpec( 0,l[1]*4,w,270,curv));										// lower left
//				limbs.add(new LimbSpec( 1,l[2]*4,w,270,new double[]{1-lr,1-lr,1+lr,1+lr}));			// lower right 	-- sharpens curvature and slightly lengthens end
//				limbs.add(new LimbSpec( 0,l[3]*3,w/dw, 90,curv));									// ul joint 	-- pinch
//				limbs.add(new LimbSpec( 4,l[4]*4,w, 45,curv));										// upper right
//				limbs.add(new LimbSpec( 4,l[5]*4,w+m*2,135,new double[]{1+ul,1+ul,1+ul,1+ul}));		// upper left 	-- broadens curvature (by increasing width only)
////				limbs.add(new LimbSpec( 4,l[5]*4,w,135,new double[]{1+m*4,1+m*4,1+m*6,1+m*6},true));
//				limbs.add(new LimbSpec( 4,l[5]*4,w,135,new double[]{1+m*3,2,1,1},true));
			
//			} else {
				limbs.add(new LimbSpec(-1,l[0]*4,w,  0,curv1st));
				limbs.add(new LimbSpec( 0,l[1]*4,w,270,curv));
				limbs.add(new LimbSpec( 1,l[2]*4,w,270,curv));
				limbs.add(new LimbSpec( 0,l[3]*4,w, 90,curv));
//			}
			break;

		case SEVEN_downT_ud:		// #26
			rotateFlag = true;

//			if (doMorph) {

//				double dw = m/2+1; 		// delta width factor
////				double dw2 = m/1+1; 	// delta width factor2
//				double ur = m*1-0.01;	// upper right curvature change factor
//
//				limbs.add(new LimbSpec(-1,l[0]*8,w,w*dw, 90,curv1st,new double[]{0,-l[0]*8/2}));	// ur joint 	-- bulge; ll joint  -- pinch
//				limbs.add(new LimbSpec( 0,l[1]*3,w*dw,  0,curv));									// lower right 	
//				limbs.add(new LimbSpec( 0,l[2]*3,w*dw,180,curv));									// lower left   -- broadens curvature
//				limbs.add(new LimbSpec( 1,l[3]*3,w,  0,new double[]{1-ur,1-ur,1+ur,1+ur}));			// upper right  -- shaprens curvature			
				
//			} else {
				//yPos = yPos-l[0]*8/2;	// don't change xPos, yPos as those are only used in drawing routines
				limbs.add(new LimbSpec(-1,l[0]*8,  w, 90,curv1st,new double[]{0,-l[0]*8/2}));
				limbs.add(new LimbSpec( 0,l[1]*3,  w,  0,curv));
				limbs.add(new LimbSpec( 0,l[2]*3,  w,180,curv));
				limbs.add(new LimbSpec( 1,l[3]*3,w,180,curv));			
//			}
			break;

		case Y_downT_ud:		// #27
			rotateFlag = true;

//			if (doMorph) {
				
//				//double dw = m/2+1; 	// delta width factor
//				double dw2 = m/1+1;		// delta width factor2
//				double u = m*1-0.01;	// upper endpoint curvature change factor
//				double ll = m*0;	// lower left curvature change factor
//				
//				limbs.add(new LimbSpec(-1,l[0]*8,w,w*dw2, 90,new double[]{1,1,1,1,1-u,1-u,1+u,1+u},new double[]{0,-l[0]*8/2}));	// ur joint 	-- bulge; ll joint  -- pinch
//				limbs.add(new LimbSpec( 0,l[1]*3,w,  0,curv));										// lower right 	
//				limbs.add(new LimbSpec( 0,l[2]*3,w+m*2,180,new double[]{1+ll,1+ll,1+ll,1+ll}));		// lower left   -- broadens curvature
								
//			} else {
				//yPos = yPos-l[0]*8/2;
				limbs.add(new LimbSpec(-1,l[0]*7,  w, 90,curv1st,new double[]{0,-l[0]*8/2}));
				limbs.add(new LimbSpec( 0,l[1]*3,  w,  0,curv));
				limbs.add(new LimbSpec( 0,l[2]*3,  w,180,curv));
				limbs.add(new LimbSpec( 1,l[4]*4,w, 45,curv));
				limbs.add(new LimbSpec( 1,l[5]*4,w,135,curv));
//			}
			break;

		case downL_t_ud:		// #28
			rotateFlag = true;

//			if (doMorph) {
				
//				double dw = m/2+1; 	// delta width factor
//				double lr = m*1;	// lower right curvature change factor
//				double ul = m*0;	// upper left curvature change factor
//			
//				limbs.add(new LimbSpec(-1,l[0]*5,w*dw,w/dw, 90,curv1st));								// lr and ul joint	-- pinch & bulge
////				limbs.add(new LimbSpec( 0,l[1]*3,w,  0,new double[]{1-lr,1-lr,1+lr,1+lr}));				// lower right	-- sharpens curvature
//				limbs.add(new LimbSpec( 0,l[1]*3,w,  0,curv));											// lower right	
//				limbs.add(new LimbSpec( 0,l[2]*3,w,180,curv));
//				limbs.add(new LimbSpec( 0,l[3]*4,w,270,new double[]{1-lr,1-lr,1+lr,1+lr}));				// lower down   -- sharpens curvature
//				limbs.add(new LimbSpec( 1,l[4]*4,w+m*2,180,new double[]{1+ul,1+ul,1+ul,1+ul}));			// upper left	-- broadens curvature		
				
//			} else {
				limbs.add(new LimbSpec(-1,l[0]*4,w, 90,curv1st));
				limbs.add(new LimbSpec( 0,l[1]*3,w,  0,curv));
				limbs.add(new LimbSpec( 0,l[2]*3,w,180,curv));
				limbs.add(new LimbSpec( 0,l[3]*4,w,270,curv));
				limbs.add(new LimbSpec( 1,l[4]*4,w,  0,curv));
//			}
			break;

		case I_t_ud:		// #29
			rotateFlag = true;

//			if (doMorph) {
				
//				double dw = m/2+1; 	// delta width factor
//				double lr = m*1;	// lower right curvature change factor
//				double ul = m*0;	// upper left curvature change factor
//			
//				limbs.add(new LimbSpec(-1,l[0]*3,w*dw,w/dw, 90,curv1st));								// lr and ul joint	-- pinch & bulge
////				limbs.add(new LimbSpec( 0,l[1]*3,w,  0,new double[]{1-lr,1-lr,1+lr,1+lr}));				// lower right	-- sharpens curvature
//				limbs.add(new LimbSpec( 0,l[1]*3,w,  0,curv));											// lower right	
//				limbs.add(new LimbSpec( 0,l[2]*3,w,180,curv));
//				limbs.add(new LimbSpec( 0,l[3]*4,w,270,new double[]{1-lr,1-lr,1+lr,1+lr}));				// lower down   -- sharpens curvature
//				limbs.add(new LimbSpec( 1,l[4]*4,w, 45,curv));
//				limbs.add(new LimbSpec( 1,l[4]*4,w+m*2,135,new double[]{1+ul,1+ul,1+ul,1+ul}));			// upper left	-- broadens curvature
				
//			} else {
				limbs.add(new LimbSpec(-1,l[0]*4,w, 90,curv1st));
				limbs.add(new LimbSpec( 0,l[1]*3,w,  0,curv));
				limbs.add(new LimbSpec( 0,l[2]*3,w,180,curv));
				limbs.add(new LimbSpec( 0,l[3]*4,w,270,curv));
//			}
			break;

		case SEVEN_J_ud:		// #30
			rotateFlag = true;
			
//			if (doMorph) {
				
//				double dw = m/2+1; 		// delta width factor
//				double dw2 = m/1+1; 	// delta width factor2
//				double ur = m*1-0.01;	// upper right curvature change factor
//				
//				limbs.add(new LimbSpec(-1,l[0]*8,w,w*dw, 90,curv1st,new double[]{0,-l[0]*8/2}));	// ur joint -- bulge
//				limbs.add(new LimbSpec( 0,l[1]*4,w/dw,180,curv));									// j, lower left  -- pinch 
//				limbs.add(new LimbSpec( 2,l[2]*2,w*dw2, 90,curv));									// j. lower left  -- broaden curvature
//				limbs.add(new LimbSpec( 1,l[3]*3,w,  0,new double[]{1-ur,1-ur,1+ur,1+ur}));			// upper right  -- sharpens curvature
				
//			} else {
				//yPos = yPos-l[0]*8/2;
				limbs.add(new LimbSpec(-1,l[0]*8,w, 90,curv1st,new double[]{0,-l[0]*8/2}));
				limbs.add(new LimbSpec( 0,l[1]*4,w,180,curv));
				limbs.add(new LimbSpec( 2,l[2]*2,w, 90,curv));
				limbs.add(new LimbSpec( 1,l[3]*3,w,180,curv));	
//			}
			break;

		case Y_J_ud:			// #31
			rotateFlag = true;

//			if (doMorph) {
				
//				double dw = m/2+1; 		// delta width factor
//				double dw2 = m/1+1;		// delta width factor2
//				double u = m*1-0.01;	// upper endpoint curvature change factor
//
//				limbs.add(new LimbSpec(-1,l[0]*8,w,w*dw2, 90,new double[]{1,1,1,1,1-u,1-u,1+u,1+u},new double[]{0,-l[0]*8/2}));	// top  -- sharpens curvature
//				limbs.add(new LimbSpec( 0,l[1]*4,w/dw,180,curv));																	// j, bottom   
//				limbs.add(new LimbSpec( 2,l[2]*2,w*dw2, 90,curv));																// j. lower left  -- broaden curvature	
			
//			} else {
				//yPos = yPos-l[0]*8/2;
				limbs.add(new LimbSpec(-1,l[0]*7,w, 90,curv1st,new double[]{0,-l[0]*8/2}));
				limbs.add(new LimbSpec( 0,l[1]*4,w,180,curv));
				limbs.add(new LimbSpec( 2,l[2]*2,w, 90,curv));
				limbs.add(new LimbSpec( 1,l[4]*4,w, 45,curv));
				limbs.add(new LimbSpec( 1,l[4]*4,w,135,curv));
//			}
			break;

// JK 2 May 2016 - added 2 new canonical shapes		
		case square:		// #16
			limbs.add(new LimbSpec(-1,l[0]*8,w,90,curv1st,new double[]{-l[0]*4,-l[0]*4}));
//			limbs.add(new LimbSpec( 1,l[1]*8,w,0,curv));
//			limbs.add(new LimbSpec( 2,l[1]*8,w,270,curv));
//			limbs.add(new LimbSpec( 3,l[1]*7,w,180,curv));
			
			break;
					
		case triangle:		// #17
			limbs.add(new LimbSpec(-1,l[0]*9,w,60,curv1st,new double[]{-l[0]*4.5,-l[0]*3.5}));
			limbs.add(new LimbSpec( 1,l[1]*9,w,300,curv));
			limbs.add(new LimbSpec( 2,l[1]*8,w,180,curv));
			
			break;

//// JK 19 May 2016 testing shape-specific occluder			
//		case occluded_square:
//			limbs.add(new LimbSpec(-1,l[0]*8,w,90,curv1st,new double[]{-l[0]*4,-l[0]*4}));
//			limbs.add(new LimbSpec( 1,l[1]*8,w,0,curv));
//			limbs.add(new LimbSpec( 2,l[1]*8,w,270,curv));
//			limbs.add(new LimbSpec( 3,l[1]*7,w,180,curv));
//				                        // hole x                hole y                   inner radius           outer radius
//			float[] specBuff = {(float) (20.0f * zoom), (float) (20.0f * zoom), (float) (12.0f * zoom), (float) (25.0f * zoom)};
//			spec.getOccluderSpec().setAlphaGain(0.48f);
//			spec.getOccluderSpec().setHeight(50 * (int)zoom);
//			spec.getOccluderSpec().setWidth(90 * (int)zoom);
//			spec.getOccluderSpec().setCenter(0 * zoom, 40 * zoom);
//			spec.getOccluderSpec().setSpecs(specBuff);
//			break;
//
//		case occluded_triangle:
//			limbs.add(new LimbSpec(-1,l[0]*9,w,60,curv1st,new double[]{-l[0]*4.5,-l[0]*3.5}));
//			limbs.add(new LimbSpec( 1,l[1]*9,w,300,curv));
//			limbs.add(new LimbSpec( 2,l[1]*8,w,180,curv));
//                                        // hole x                hole y                   inner radius           outer radius
//			float[] specBuff2 = {(float) (0.0f * zoom), (float) (30.0f * zoom), (float) (8.0f * zoom), (float) (9.0f * zoom)}; //,
//                                        // hole x                hole y                   inner radius           outer radius
////					              (float) (0.0f * zoom), (float) (18.0f * zoom), (float) (4.0f * zoom), (float) (9.0f * zoom)};
//			spec.getOccluderSpec().setAlphaGain(0.50f);
//			spec.getOccluderSpec().setHeight(50 * (int)zoom);
//			spec.getOccluderSpec().setWidth(90 * (int)zoom);
//			spec.getOccluderSpec().setCenter(0 * zoom, 40 * zoom);
//			spec.getOccluderSpec().setSpecs(specBuff2);			
//			break;
			
		} // end switch
		
		if (rotateFlag){
			global_ori = 180;
			spec.setGlobalOri(global_ori);
		}
		
		spec.setAllLimbSpecs(limbs);
		
		// re-save spec into StimObjData db table:
		// -- this is done in SachExptSpecGenerator:
		
		//dbUtil.writeStimObjData(stimObjId, s.toXml(), d.toXml());
		
	}
//	
//	
//	
	
	
	
	
			
	//---- helper functions ----
	
	double rescaleOri(double ori) {
		// keeps: 0 <= ori < 360
		//while (ori >= 360) ori-=360;		
		//while (ori < 0) ori+=360;	
		return ((ori % 360) + 360) % 360;
	}
	
	static double[] findFlankingOris(double ori,double[] orisIN) {
		// -- when adding a node (which can be thought of as a vector from the origin node) I want 
		// to know which nodes (orientations) flank this new node, so that I can appropriately place 
		// the control points at the new joint.  
		double[] oris = Arrays.copyOf(orisIN,orisIN.length);
		Arrays.sort(oris);

		int oriIdx = -1;
		for (int n = 0; n < oris.length; n++) {
			if (ori < oris[n]) { 
				oriIdx = n;
				break;
			}
		}
		
		int idxHigh = (oriIdx==-1) ? ++oriIdx : oriIdx;
		int idxLow  = (oriIdx==0)  ? oris.length-1 : oriIdx-1;

		double high = oris[idxHigh];	//oris[(oriIdx==-1) ? ++oriIdx : oriIdx];
		double low  = oris[idxLow];	//oris[(oriIdx==0)  ? oris.length-1 : oriIdx-1];
		
		return new double[]{low,high};	//,idxLow,idxHigh}; ** this doesn't work, only for sorted oris!
	}

	static int[] findFlankingNodes(double ori,double[] orisIn) {
		// -- when adding a node (which can be thought of as a vector from the origin node) I want 
		// to know which nodes (orientations) flank this new node, so that I can appropriately place 
		// the control points at the new joint.  
		double[] oris = Arrays.copyOf(orisIn,orisIn.length);
		int[] idxs = SachMathUtil.sortLowToHighIndx(oris);	// outputs sorted indices
		Arrays.sort(oris);
		
		int oriIdx = -1;
		for (int n = 0; n < oris.length; n++) {
			if (ori < oris[n]) { 
				oriIdx = n;
				break;
			}
		}		
		
		int idxHigh = (oriIdx==-1) ? ++oriIdx : oriIdx;
		int idxLow  = (oriIdx==0)  ? oris.length-1 : oriIdx-1;
		
		return new int[]{idxs[idxLow],idxs[idxHigh]};
	}
	
	
	boolean splineCheckOk() {
		// TODO: ** checks: non-overlapping **		
		// make sure spline pts are located only near local control points and not distant control points
		
		return true;
	}
	
	boolean checkIfPointsInPoly(double[][] newLimbCtrlPts, double[]...currentCtrlPts) {
		// ** checks for overlapping of new limb with current control points**
		// does this by checking if any current control point lies within the given quadrilateral
		// input: quad: the four control points defining a new limb [a b c d]
		// output: returns true if limbs do overlap 
		
		// first find any control points lie within a bounding box around the new limb control points:
		double[][] inPts = SachMathUtil.inBoundingBox(newLimbCtrlPts,currentCtrlPts);
		// if none, there aren't any overlaps:
		if(inPts.length == 0) {
			return false;
		}
		
		// for any points that do lie within the bounding box, perform a more rigorous test:
		// (find if these points lie within the polygon formed by the new limb control points)
		for(double[] pt : inPts) {
			if(SachMathUtil.isInPolygon(newLimbCtrlPts,pt)) {
				return true;
			}
		}	
		return false;
		
		// *** while this is nice, I may additionally need to find if any two polygons intersect 
		//	   (for some overlap cases all ctrl pts lie outside overlap area)
	}
	
	
	// compute medial axis from control points:
	public double[][] computeMedialAxis(double[][] pts) {
		double[][] maxisPts = null;
		
		// TODO: I may need this for checking the morph lines, but not sure...
		
		return maxisPts;
	}
	
	
	//---- setter & getters ----
	
	public int getStimCat() {
		return stimCat.ordinal();
	}
	public void setStimCat(int stimCat) {
		if (0 <= stimCat & stimCat < StimCategories.values().length) {
			this.stimCat = StimCategories.values()[stimCat];
			spec.setStimType(StimType.BEH_MedialAxis.toString());
		}	// else stimCat = null
	}
	public double getxPos() {
		return xPos;
	}
	public void setxPos(double xPos) {
		this.xPos = xPos;
	}
	public void shiftX(double x) {
		xPos = xPos + x;
	}
	public double getyPos() {
		return yPos;
	}
	public void setyPos(double yPos) {
		this.yPos = yPos;
	}
	public void shiftY(double y) {
		yPos = yPos + y;
	}
	public double getSize() {
		return size;
	}
//	public void setSize(double size) {
//		this.size = size;
//	}
	public void setSize(double size) {
		spec.setSize(size);
		this.size = spec.getSize();
	}
	
	public void setContrast(double contrast) {
		this.contrast = contrast;
	}
	
	public void setColor(RGBColor color) {
		this.color = color;
		this.spec.setForeColor(color.getRed(),color.getGreen(),color.getBlue(),1);;
	}
	
	public void setBackColor(RGBColor color) {
		this.backColor = color;
		this.spec.setBackColor(color.getRed(),color.getGreen(),color.getBlue(),1);;
	}
	
//	public void setGlobalOri(double o) {
//		spec.setGlobalOri(o);
//		global_ori = spec.global_ori; 
//	}
	
	public double getGlobalOri(){
		return global_ori;
	}
	public double[][] getCtrlPts() {
		return objCtrlPts;
	}
	public BsplineLine getSpline() {
		return spline;
	}
	public LimbSpec getLimbs(int limbID){
		return limbs.get(limbID);
	}
	public List<LimbSpec> getLimbs() {
		return limbs;
	}
	public void setLimbs(LimbSpec limb){
		limbs = new ArrayList<LimbSpec>();
		limbs.add(limb);
	}
	public void setLimbs(List<LimbSpec> limbs) {
		this.limbs = limbs;
	}
	public boolean isViolation() {
		return flagSplineViolation;
	}

	public boolean isPrintStimSpecs() {
		return printStimSpecs;
	}
	public void setPrintStimSpecs(boolean printStimSpecs) {
		this.printStimSpecs = printStimSpecs;
	}
	
	public boolean isCantFail() {
		return cantFail;
	}
	public void setCantFail(boolean cantFail) {
		this.cantFail = cantFail;
	}
	public void setDoMorph(boolean doMorph){
		this.doMorph = doMorph;
	}
	
	public void setDoCenterObj(boolean dco) {
		this.doCenterObject = dco;
	}
	
	public boolean isMorph(){
		return doMorph;
	}

	// make a random object
	public void makeRandObj() {
		spec.setStimType(StimType.GA.toString());
		createObj();
	}
	
	// use this to create an obj using its spec
	public void setSpec(String s) {
		setSpec_dontCreate(s);
				
		// create stimulus:
		createObj();

	}
	
	// use this to only UPDATE the spec (object has already been created)
	public void setSpec_dontCreate(String s) {
		spec = BsplineObjectSpec.fromXml(s);
		
		// pull variables needed for drawing:
		xPos = spec.getXCenter();
		yPos = spec.getYCenter();
		size = spec.getSize();
		global_ori = spec.getGlobalOri();
		
		// other variables:
		doRand = spec.isDoRandom();
		setStimCat(spec.getCategory()); // category: (0-15), or -1 for no category [0-7 are 1st set, 8-15 are 2nd set]
		doMorph = spec.isDoMorph();
		morphLim = spec.getMorphLim();
		
		limbs = spec.getAllLimbSpecs();
		if (limbs == null) limbs = new ArrayList<LimbSpec>();	// if limbs don't exist, create empty limbs
	}

	public BsplineObjectSpec getSpec() {
		return spec;
	}
	
	public void genShapeFromFile(String folderPath, long id) {
		String in_specStr;
        StringBuffer fileData = new StringBuffer(100000);
        try
        {
            BufferedReader reader = new BufferedReader(new FileReader(folderPath + "/" + id + "_spec.xml"));
            char[] buf = new char[1024];
            int numRead=0;
            while((numRead=reader.read(buf)) != -1){
                String readData = String.valueOf(buf, 0, numRead);
                //System.out.println(readData);
                fileData.append(readData);
                buf = new char[1024];

            }
            reader.close();
        }
        catch (Exception e)
        {
            System.out.println("error in read XML spec file");
            System.out.println(e);
        }

        in_specStr = fileData.toString();
        
        this.setSpec_dontCreate(in_specStr);
    }
	
	public void writeInfo2File(String folderPath, long id) {
		this.spec.setCtrlPts(objCtrlPts);
		String outStr = this.spec.toXml();
	    
	    try {
	    	BufferedWriter out = new BufferedWriter(new FileWriter(folderPath + "/" + id + "_spec.xml"));
	        
	        out.write(  outStr);
	        out.flush();
	        out.close();
	        	        
	    } catch (Exception e) { 
        	System.out.println(e);
    	}
		
	    outStr = vecToStr(this.spline.bpt);
	    try {
    		BufferedWriter out = new BufferedWriter(new FileWriter(folderPath + "/" + id + "_densePts.txt"));
	        
	        out.write(  outStr);
	        out.flush();
	        out.close();
	    } catch (Exception e) { 
			System.out.println(e);
		}
	    
		
	    

	}
	
	private String vecToStr(MyPoint[] pts) {
		String str = new String();
		int i = 0;
		while(pts[i]!=null){
			str = str + pts[i].x + "," + pts[i].y + "\n";
			i++;
		}
		return str;
	}
	
	
}



