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
			7. doSaveSpec:
			8. TEMP:
			9-11. foreColor:
			12-14. backColor:
		*/

		// parse args
		String folderPath = args[0];
		int category = Integer.parseInt(args[1]);
		int startStimNum = Integer.parseInt(args[2]);
		int nMorphs = Integer.parseInt(args[3]);
		double size = Double.parseDouble(args[4]);
		boolean doCenter = Boolean.parseBoolean(args[5]);
		double contrast = Double.parseDouble(args[6]);
		boolean doSaveSpec = Boolean.parseBoolean(args[7]);
		boolean doMorph = Boolean.parseBoolean(args[8]);
		RGBColor foreColor = new RGBColor(Float.parseFloat(args[ 9]),Float.parseFloat(args[10]),Float.parseFloat(args[11]));
		RGBColor backColor = new RGBColor(Float.parseFloat(args[12]),Float.parseFloat(args[13]),Float.parseFloat(args[14]));
		
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
		PNGmaker pngMaker = new PNGmaker();
		pngMaker.createAndSavePNGsfromObjs(objs, ids, folderPath);
	}
}