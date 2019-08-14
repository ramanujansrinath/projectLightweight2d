package org.xper.generate;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.xper.drawing.drawables.PNGmaker;
import org.xper.drawing.stimuli.BsplineObject;
import org.xper.utils.RGBColor;

public class morphStimuli {
	public static void main(String[] argsFile) throws IOException {
		String textPath = argsFile[0];
		
		FileReader fr = new FileReader(textPath); 
        BufferedReader br = new BufferedReader(fr);
        
		List<Long> ids = new ArrayList<Long>();
		List<BsplineObject> objs = new ArrayList<BsplineObject>();
		
		String folderPath = "";
		int obj_counter=0;
		boolean doSaveImage = false;
		Random rng = new Random();
        for (String line = br.readLine(); line != null; line = br.readLine()) {
            String[] args = line.split(";"); 	        

	    		/* 
	    		 * new args AWC 08/13/2019
	    		 *    (required args)
	    		 * 0. seed
	    		 * 1. folderPath
	    		 * 2. parentId
	    		 * 3. childId
	    		 * 4. doSaveSpec
	    		 * 5. doSaveImage
				 * 
				 *    (conditional to doSaveImage)
	    		 * 6. doCenter
	    		 * 7. size
	    		 * 8. contrast
	    		 * 9-11 foreColor
	    		 * 12-14 backColor
	    		 * 
	    		 * old args:
				0. folderPath: 
				1. parentId:
				2. childId:
				3. size:
				4. doCenter:
				5. contrast:
				6. doSaveSpec:
				7. doSaveImage:
				8-10. foreColor:
				11-13. backColor:
			*/
			
            long seed = Long.parseLong(args[0].trim());
			folderPath = args[1].trim();
            long parentId = Long.parseLong(args[2].trim());
			long childId = Long.parseLong(args[3].trim());
			boolean doSaveSpec = Boolean.parseBoolean(args[4].trim());
			doSaveImage = Boolean.parseBoolean(args[5].trim());
			

			boolean doCenter = false; 
			double size = 1.0; 
			double contrast = 1.0; 
			
			RGBColor foreColor = new RGBColor(1,1,1);;
			RGBColor backColor = new RGBColor(0,0,0);;
			if (doSaveImage){
				doCenter = Boolean.parseBoolean(args[6].trim());
				size = Double.parseDouble(args[7].trim());
				contrast = Double.parseDouble(args[8].trim());
				foreColor = new RGBColor(Float.parseFloat(args[ 9]),Float.parseFloat(args[10]),Float.parseFloat(args[11]));
				backColor = new RGBColor(Float.parseFloat(args[12]),Float.parseFloat(args[13]),Float.parseFloat(args[14]));
			}

			if (seed>0)
				rng.setSeed(seed);
			else
				rng.setSeed(rng.nextLong());
			
			// make centered morph obj
			ids.add(childId);
			
			objs.add(new BsplineObject());
			objs.get(obj_counter).setSize(size);
			objs.get(obj_counter).setContrast(contrast);
			objs.get(obj_counter).setDoCenterObj(doCenter);
			objs.get(obj_counter).setColor(foreColor);
			objs.get(obj_counter).setBackColor(backColor);
			
			objs.get(obj_counter).genShapeFromFile(folderPath,parentId);
			objs.get(obj_counter).setDoMorph(true);
			objs.get(obj_counter).setRng(rng);
			objs.get(obj_counter).createObj();
				
			if (doSaveSpec) {
				objs.get(obj_counter).writeInfo2File(folderPath, childId);
			}
			
//			objs.get(obj_counter).setDoCenterObj(true);
//			objs.get(obj_counter).setDoMorph(false);
//			objs.get(obj_counter).createObj();
//			if (doSaveSpec) {
//				objs.get(obj_counter).writeInfo2File(folderPath, childId);
//			}
			
			obj_counter++;
			
        }			
		fr.close();
		br.close();
		
		if (doSaveImage){
			PNGmaker pngMaker = new PNGmaker();
			pngMaker.createAndSavePNGsfromObjs(objs, ids, folderPath);
		}
	}
}
