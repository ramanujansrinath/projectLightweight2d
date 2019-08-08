package org.xper.generate;

import java.util.ArrayList;
import java.util.List;

import org.xper.drawing.drawables.PNGmaker;
import org.xper.drawing.stimuli.BsplineObject;
import org.xper.utils.RGBColor;

public class behStimuli {
	public static void main(String[] args) {
		/* args:
			0. folderPath: 
			1. category: 0 indexed
			2. startStimNum
			3. nMorphs:
			4. size:
			5. doCenter:
			6. contrast:
			7. doMorph:
			8. doSaveSpec:
			9. doSaveImage;
			10-12. foreColor:
			13-15. backColor:
		*/

		// parse args
		String folderPath = args[0];
		int category = Integer.parseInt(args[1]);
		int startStimNum = Integer.parseInt(args[2]);
		int nMorphs = Integer.parseInt(args[3]);
		double size = Double.parseDouble(args[4]);
		boolean doCenter = Boolean.parseBoolean(args[5]);
		double contrast = Double.parseDouble(args[6]);
		boolean doMorph = Boolean.parseBoolean(args[7]);
		boolean doSaveSpec = Boolean.parseBoolean(args[8]);
		boolean doSaveImage = Boolean.parseBoolean(args[9]);
		
		RGBColor foreColor;
		RGBColor backColor;
		if (doSaveImage){
			foreColor = new RGBColor(Float.parseFloat(args[10]),Float.parseFloat(args[11]),Float.parseFloat(args[12]));
			backColor = new RGBColor(Float.parseFloat(args[13]),Float.parseFloat(args[14]),Float.parseFloat(args[15]));
		}
		else{
			foreColor = new RGBColor(1,1,1);
			backColor = new RGBColor(0,0,0);
		}
		
		List<Long> ids = new ArrayList<Long>();
		List<BsplineObject> objs = new ArrayList<BsplineObject>();

		for (int i=0; i<nMorphs; i++) {		
			ids.add((long)(startStimNum+i));
			objs.add(new BsplineObject());
			
			// set object properties
			objs.get(i).setDoCenterObj(doCenter);
			objs.get(i).setSize(size);
			objs.get(i).setColor(foreColor);
			objs.get(i).setBackColor(backColor);
			objs.get(i).setContrast(contrast);

			objs.get(i).setDoMorph(doMorph);
			if (doMorph) {
				objs.get(i).getSpec().setMorphLengthGain(1.0);
				objs.get(i).getSpec().setMorphWidthGain(1.0);
				objs.get(i).getSpec().setMorphBias(0.0);
			}
			objs.get(i).setStimCat(category);
			objs.get(i).createObj();
			
			//s.getSpec().setCategory(16,false,false);
			//s.setGlobalOri(90);
			//s.getSpec().setGrowthFactor(2.0);
			
			// save spec, if necessary
			if (doSaveSpec) {
				objs.get(i).writeInfo2File(folderPath, ids.get(i));
			}
		}
		// make all the images
		if (doSaveImage){
			PNGmaker pngMaker = new PNGmaker();
			pngMaker.createAndSavePNGsfromObjs(objs, ids, folderPath);
		}
	}
}