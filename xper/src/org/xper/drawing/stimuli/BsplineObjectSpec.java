package org.xper.drawing.stimuli;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.xper.utils.SachMathUtil;

//import org.xper.sach.util.SachMathUtil;

import com.thoughtworks.xstream.XStream;


public class BsplineObjectSpec {
	//fields:
	boolean animation = false;

	String stimType;					// Behavioral (medial-axis or curvature defined) or GA stimuli
	long stimObjId;
	double xCenter = 0;
	double yCenter = 0;
	double size = BsplineObject.DEFAULT_SIZE;
	double global_ori = 0;
	boolean doMorph = false;			// morph stim obj?
	// JK 6 Oct 2016
	boolean morphLengthWidthOnly = false;  // restrict morphs to length and width dimensions only
	
	double[][] ctrlPts;
		
	// JK 18 April 2016 
	double foreColor[] = {0.0, 0.0, 0.0, 1.0};
	double backColor[] = {0.0, 0.0, 0.0, 1.0};
	double fader = 1.0; 								// fade to black color factor
	Random rng = new Random();
		
	// JK 25 April 2016 - changing to category based colors
	//							 r     g     b         a             cat
	static double Colors[] = {	0.8, 0.9, 0.8,      1.0,   		// 0
								1.0, 0.0, 0.0,     	1.0,		// 1
								0.0, 1.0, 0.0,      1.0,   		// 2
								0.0, 0.0, 1.0,     	1.0,		// 3
								1.0, 0.0, 1.0,      1.0,   		// 4
								1.0, 1.0, 0.0,     	1.0,		// 5
								0.0, 1.0, 1.0,      1.0,   		// 6
								1.0, 0.5, 0.5,     	1.0,		// 7
								0.5, 1.0, 0.5,      1.0,   		// 8
								0.5, 0.5, 1.0,     	1.0,		// 9
								1.0, 0.5, 1.0,      1.0,   		// 10
								1.0, 0.5, 0.5,     	1.0,		// 11
								0.0, 0.8, 0.2,     	1.0,		// 12
								0.5, 0.9, 0.9,      1.0,   		// 13
								1.0, 0.3, 0.5,     	1.0,		// 14
								0.7, 0.1, 0.3,      1.0,     	// 15	
								// JK 2 May 2016  added 2 training shapes 
								0.0, 0.8, 0.2,    	1.0,		// 16
								0.5, 0.9, 0.9,      1.0,   		// 17
	};

	// JK 25 April 2016 - changing to category based sizes
	static double StepSize = 0.4;  		// difference between sizes for categories 8 thru 15
	static double BaseSize = 4.0; 			// the standard size
	
	// an array of offset size  values : the final BSO size = BaseSize + growthFactor * SizeGrowthValues[Category]
	static double [] SizeGrowthValues = new double[ BsplineObject.getNumCategories()];
	double growthFactor = 0.0;
	static double upperSizeBound = 6;
	static double lowerSizeBound = 2;		

		// Beh-specific:
	int category = -1;					// there are 8 categories for each stimulus type: 0-7, -1 means not a behavioral trial or no category set; NOTE: there are actually 16 beh stim categories, we just train on 8 of them
	String behavioralClass;				// sample, match, or non-match
	boolean doRandom = false;			// randomly change things like limb lengths and width
	double morphLim = -1;				// amount to morph behavioral stim along morph line (only used for Beh stimuli), should range from 0 to 1
	
	// JK 9 Sept 2016 
	//   morph gain and bias used to control "random" morphs
	double morphLengthGain = -1.0;
	double morphWidthGain = -1.0;	
	double morphBias = -1.0;
	
	List<LimbSpec> limbs = new ArrayList<LimbSpec>();

	transient static XStream s;
	
	static {
		s = new XStream();
		s.alias("StimSpec", BsplineObjectSpec.class);
		s.alias("limb", LimbSpec.class);
		s.useAttributeFor("animation", boolean.class);
		s.addImplicitCollection(BsplineObjectSpec.class, "limbs", "limb", LimbSpec.class);
		
	}
	
	// JK 10 May 2016
	public static void setSizeGrowthParameters(int numActiveCategories, double lowerBound, double upperBound){
		lowerSizeBound = lowerBound;
		upperSizeBound = upperBound;
	}
	
	public void addLimbSpec(LimbSpec spec) {
		limbs.add(spec);
	}
	
	public LimbSpec getLimbSpec(int index) {
		if (index < 0 || index >= limbs.size()) {
			return null;
		} else {
//			return new LimbSpec(limbs.get(index));
			return (LimbSpec) limbs.get(index);
		}
	}
	
	public int getLimbSpecCount() {
		return limbs.size();
	}
	
	public List<LimbSpec> getAllLimbSpecs() {
//		List<LimbSpec> ll = new ArrayList<LimbSpec>();
//		 for (int n=0;n<getLimbSpecCount();n++) {
//			 ll.add(getLimbSpec(n));
//		 }
//		 return ll;
		
		if (limbs == null) {
			return new ArrayList<LimbSpec>();
		} else {	
			return limbs;
		}
	}
	
	public void setAllLimbSpecs(List<LimbSpec> limbs) {
		this.limbs = limbs;
	}
	
	public String toXml() {
		return BsplineObjectSpec.toXml(this);
	}
	
	public static String toXml(BsplineObjectSpec spec) {
		return s.toXML(spec);
	}
	
	public static BsplineObjectSpec fromXml(String xml) {
		BsplineObjectSpec g = (BsplineObjectSpec)s.fromXML(xml);
		
		
		
		return g;
	}
	
	public BsplineObjectSpec() {
		// JK 17 May 2016
//		occluderSpec = null;
		// JK 10 May 2016  removed smart size in favor of random sizes

//		// JK 25 April 2016  - added a smarter size
//		// only using the last 8 categories
//		final int StartNdx = 8;
//		final int StopNdx = BsplineObject.getNumCategories();
//		double step = ( (double)(StopNdx - StartNdx) / 2.0 ) * StepSize - (StepSize / 2); 		
//		step *= -1;
//		
//		for(int n = 0; n < StopNdx; n++ ){
//			if(n >= StartNdx){
//				SizeGrowthValues[n] = step;
//				step += StepSize;
//				//System.out.println("BSOSpec() : SizeGrowthValues[" + n + "] == " + SizeGrowthValues[n]);
//			} else {
//				SizeGrowthValues[n] = 0;
//			}
//		}
//
//		// JK 27 April 2016  - hardcoding categories 8, 11, 13
//		SizeGrowthValues[11] =  -0.6;

	}
	
	public BsplineObjectSpec(BsplineObjectSpec d) {
		stimObjId = d.getStimObjId();
		
		xCenter = d.getXCenter();
		yCenter = d.getYCenter();
		size = d.getSize();
		global_ori = d.getGlobalOri();

		animation = d.isAnimation();
		category = d.getCategory();
		stimType = d.getStimType();
		behavioralClass = d.getBehavioralClass();
		
		doRandom = d.isDoRandom();
		doMorph = d.isDoMorph();
		morphLim = d.getMorphLim();
		
		morphLengthGain = d.morphLengthGain;
		morphBias = d.morphBias;
		
		limbs = d.getAllLimbSpecs();

		// JK 18 April 2016 - proper deep copy
		int len = d.foreColor.length;
		foreColor = new double[len]; 
		System.arraycopy(d.foreColor, 0, foreColor, 0, len);
		
//		if (limbs == null) {
//			limbs = new ArrayList<LimbSpec>();
//		}
	}
	
	
	public long getStimObjId() {
		return stimObjId;
	}
	public void setStimObjId(long id) {
		stimObjId = id;
	}
	public double getXCenter() {
		return xCenter;
	}
	public void setXCenter(double center) {
		xCenter = center;
	}
	public double getYCenter() {
		return yCenter;
	}
	public void setYCenter(double center) {
		yCenter = center;
	}
	public double getSize() {
		return size;
	}
	public void setSize(double size) {
		this.size = size;
	}
	public double getGlobalOri() {
		return global_ori;
	}
	public void setGlobalOri(double global_ori) {
		this.global_ori = global_ori;
	}
	public boolean isAnimation() {
		return animation;
	}
	public void setAnimation(boolean animation) {
		this.animation = animation;
	}

	public int getCategory() {
		return category;
	}
	public void setCategory(int category) {
		this.category = category;
	}

	// JK 10 May 2016 - replaced smart size with random size
	// JK 25 April 2016
	public void setCategory(int category, boolean shouldUseColorCues, boolean shouldUseSizeCues) {
		this.category = category;
		int n = 4;  // entries per category: rgba
		
		if(shouldUseColorCues){
			if(category > -1 && category < BsplineObject.getNumCategories()){
				// JK 31 May 2016 cheap hack to generate single color set
				//setRgbaVec(0.50, 0.9, 0.40, 1.0);
				
				setForeColor(Colors[category * n + 0] * fader, Colors[category * n + 1] * fader, Colors[category * n + 2] * fader, Colors[category * n + 3]);
			}
			//System.out.println("BSOSpec::setCategory(int, bool) : " + category + " : color == " + getRedVal() + ", " + getGreenVal() + ", " + getBlueVal() + ", " + getAlphaVal());
		}
		else {
			setForeColor(0.0, 0.0, 0.0, 1.0);
		}
		SachMathUtil s = new SachMathUtil();
		s.setSeed(rng.nextLong());
		
		if(shouldUseSizeCues){
			if(category > -1 && category < BsplineObject.getNumCategories()){
				//setSize(BaseSize + (SizeGrowthValues[category] * growthFactor));
				setSize(s.randRange(upperSizeBound, lowerSizeBound));
			}
			//System.out.println("BSOSpec::setCategory(int, bool) : " + category + " : size = = " + getSize());
		}
		else {
			setSize(BaseSize * BsplineObject.zoom);  // the previous hardcoded value ...
		}		
	}

	public String getStimType() {
		return stimType;
	}
	public void setStimType(String type) {
		this.stimType = type;
	}
	
	public String getBehavioralClass() {
		return behavioralClass;
	}
	public void setBehavioralClass(String behavioralClass) {
		this.behavioralClass = behavioralClass;
	}

	public boolean isDoRandom() {
		return doRandom;
	}

	public void setDoRandom(boolean doRandom) {
		this.doRandom = doRandom;
	}

	public boolean isDoMorph() {
		return doMorph;
	}

	public void setDoMorph(boolean doMorph) {
		this.doMorph = doMorph;
	}
	
	
	public boolean isMorphLengthWidthOnly() {
		return morphLengthWidthOnly;
	}

	public void setMorphLengthWidthOnly(boolean morphLengthWidthOnly) {
		this.morphLengthWidthOnly = morphLengthWidthOnly;
	}
	
	
	public double getMorphLim() {
		return morphLim;
	}

	public void setMorphLim(double morphLim) {
		this.morphLim = morphLim;
	}
	
	public boolean isBlankStim() {
		if (stimType == "BLANK") {
			return true;
		} else {
			return false;
		}
	}

	// JK 20 April 2016
	public void setForeColor(double redVal, double greenVal, double blueVal, double alphaVal){
		if(redVal <= 1.0 && redVal >= 0.0){
			this.foreColor[0] = redVal;
		}
		if(greenVal <= 1.0 && greenVal >= 0.0){
			this.foreColor[1] = greenVal;
		}
		if(blueVal <= 1.0 && blueVal >= 0.0){
			this.foreColor[2] = blueVal;
		}
		if(alphaVal <= 1.0 && alphaVal >= 0.0){
			this.foreColor[3] = alphaVal;
		}
	}
	
	public void setBackColor(double redVal, double greenVal, double blueVal, double alphaVal){
		if(redVal <= 1.0 && redVal >= 0.0){
			this.backColor[0] = redVal;
		}
		if(greenVal <= 1.0 && greenVal >= 0.0){
			this.backColor[1] = greenVal;
		}
		if(blueVal <= 1.0 && blueVal >= 0.0){
			this.backColor[2] = blueVal;
		}
		if(alphaVal <= 1.0 && alphaVal >= 0.0){
			this.backColor[3] = alphaVal;
		}
	}
	
	public void setCtrlPts(double[][] objCtrlPts) {
		this.ctrlPts = objCtrlPts;
	}
	
	public double getRedVal(){
		return foreColor[0];
	}
	
	public double getGreenVal(){
		return foreColor[1];
	}
	
	public double getBlueVal(){
		return foreColor[2];
	}
	
	public double getAlphaVal(){
		return  foreColor[3];
	}

	public double getGrowthFactor(){
		return growthFactor;
	}
	

	public void setGrowthFactor(double gf){
		growthFactor = gf;
	}
	
	
	public double getFader(){
		return fader;
	}

	public void setFader(double f){
		fader = f;
	}
	
	// JK 9 Sept 2016
	public double getMorphLengthGain(){
		return morphLengthGain;
	}

	public void setMorphLengthGain(double morphLengthGain){
		this.morphLengthGain = morphLengthGain;
	}

	// JK 6 Oct 2016
	public double getMorphWidthGain(){
		return morphWidthGain;
	}

	public void setMorphWidthGain(double morphWidthGain){
		this.morphWidthGain = morphWidthGain;
	}

	
	public double getMorphBias(){
		return morphBias;
	}

	public void setMorphBias(double morphBias){
		this.morphBias = morphBias;
	}

	
	public boolean isCanonical(){
		if (limbs.isEmpty())
			return false;
		
		boolean out = (limbs.get(0).getWidth()==1) && (limbs.get(0).getWidth2()==1);
		for (int ll=1;ll<limbs.size();ll++){
			if (!out)
				break;
			out = out && (limbs.get(ll).getWidth()==1);
		}
		return out;
	}
	
	public Random getRng() {
		return this.rng;
	}
	public void setRng(Random rng) {
		this.rng = rng;
	}
}
