package org.xper.generate;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
        for (String line = br.readLine(); line != null; line = br.readLine()) {
            String[] args = line.split(";"); 	        

	    		/* args:
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
			
			folderPath = args[0].trim();
            long parentId = Long.parseLong(args[1].trim());
			long childId = Long.parseLong(args[2].trim());
			double size = Double.parseDouble(args[3].trim());
			boolean doCenter = Boolean.parseBoolean(args[4].trim());
			double contrast = Double.parseDouble(args[5].trim());
			boolean doSaveSpec = Boolean.parseBoolean(args[6].trim());
			doSaveImage = Boolean.parseBoolean(args[7].trim());
			
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
