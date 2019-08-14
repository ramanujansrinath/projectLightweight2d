package org.xper.generate;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.xper.drawing.drawables.PNGmaker;
import org.xper.drawing.stimuli.BsplineObject;
import org.xper.utils.RGBColor;

public class generateStimuli {
	public static void main(String[] args) {
		/* 
		 * new args AWC 08/13/2019
		 *    (required args)
		 * 0. seed
		 * 1. folderPath
		 * 2. startStimNum
		 * 3. nStim
		 * 4. doSaveSpec
		 * 5. doSaveImage
		 * 
		 *    (conditional to doSaveImage)
		 * 6. doCenter
		 * 7. size
		 * 8. contrast
		 * 9-11 foreColor
		 * 12-14 back Color
		 * 
		 * 
		 * 
		 * old args
			0. folderPath: 
			1. startStimNum:
			2. nStim:
			3. size:
			4. doCenter:
			5. contrast:
			6. doSaveSpec:
			7. doSaveImage:
			8-10. foreColor:
			11-13. backColor:
		*/
		
		// parse args
		long seed = Long.parseLong(args[0]);
		String folderPath = args[1];
		int startStimNum = Integer.parseInt(args[2]);
		int nStim = Integer.parseInt(args[3]);
		boolean doSaveSpec = Boolean.parseBoolean(args[4]);
		boolean doSaveImage = Boolean.parseBoolean(args[5]);
		

		boolean doCenter = false; 
		double size = 1.0; 
		double contrast = 1.0; 
		RGBColor foreColor = new RGBColor(1,1,1);
		RGBColor backColor = new RGBColor(0,0,0);
		if (doSaveImage){
			doCenter = Boolean.parseBoolean(args[6]);
			size = Double.parseDouble(args[7]);
			contrast = Double.parseDouble(args[8]);
			foreColor = new RGBColor(Float.parseFloat(args[ 9]),Float.parseFloat(args[10]),Float.parseFloat(args[11]));
			backColor = new RGBColor(Float.parseFloat(args[12]),Float.parseFloat(args[13]),Float.parseFloat(args[14]));
		}
		List<Long> ids = new ArrayList<Long>();
		List<BsplineObject> objs = new ArrayList<BsplineObject>();

		Random rng;
		if (seed>0)
			rng = new Random(seed);
		else
			rng = new Random();
		
		for (int i=0; i<nStim; i++) {		
			ids.add((long)(startStimNum+i));
			objs.add(new BsplineObject());
			
			// set object properties
			objs.get(i).setDoCenterObj(doCenter);
			objs.get(i).setSize(size);
			objs.get(i).setColor(foreColor);
			objs.get(i).setBackColor(backColor);
			objs.get(i).setContrast(contrast);
			objs.get(i).setRng(rng);

			objs.get(i).makeRandObj();
			
			// save spec, if necessary
			if (doSaveSpec) {
				objs.get(i).writeInfo2File(folderPath, ids.get(i));
			}
			
			rng = new Random(rng.nextLong());
		}
		// make all the images
		if (doSaveImage){
			PNGmaker pngMaker = new PNGmaker();
			pngMaker.createAndSavePNGsfromObjs(objs, ids, folderPath);
		}
	}
}