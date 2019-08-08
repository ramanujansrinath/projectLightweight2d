package org.xper.generate;

import java.util.ArrayList;
import java.util.List;

import org.xper.drawing.drawables.PNGmaker;
import org.xper.drawing.stimuli.BsplineObject;
import org.xper.utils.RGBColor;

public class generateStimuli {
	public static void main(String[] args) {
		/* args:
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
		String folderPath = args[0];
		int startStimNum = Integer.parseInt(args[1]);
		int nStim = Integer.parseInt(args[2]);
		double size = Double.parseDouble(args[3]);
		boolean doCenter = Boolean.parseBoolean(args[4]);
		double contrast = Double.parseDouble(args[5]);
		boolean doSaveSpec = Boolean.parseBoolean(args[6]);
		boolean doSaveImage = Boolean.parseBoolean(args[7]);
		RGBColor foreColor;
		RGBColor backColor;
		if (doSaveImage){
			foreColor = new RGBColor(Float.parseFloat(args[ 8]),Float.parseFloat(args[ 9]),Float.parseFloat(args[10]));
			backColor = new RGBColor(Float.parseFloat(args[11]),Float.parseFloat(args[12]),Float.parseFloat(args[13]));
		}
		else{
			foreColor = new RGBColor(1,1,1);
			backColor = new RGBColor(0,0,0);
		}
		List<Long> ids = new ArrayList<Long>();
		List<BsplineObject> objs = new ArrayList<BsplineObject>();

		for (int i=0; i<nStim; i++) {		
			ids.add((long)(startStimNum+i));
			objs.add(new BsplineObject());
			
			// set object properties
			objs.get(i).setDoCenterObj(doCenter);
			objs.get(i).setSize(size);
			objs.get(i).setColor(foreColor);
			objs.get(i).setBackColor(backColor);
			objs.get(i).setContrast(contrast);

			objs.get(i).makeRandObj();
			
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