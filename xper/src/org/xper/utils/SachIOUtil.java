package org.xper.utils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Scanner;

//import org.xper.time.TimeUtil;


public class SachIOUtil {

	public static char prompt(String str) {
		Scanner inputReader = new Scanner(System.in);
		System.out.print(str + ": ");
		char c = inputReader.next().charAt(0);
		//System.out.println(c);
		return c;
		
	}
	
	public static int promptInt(String str) {
		Scanner inputReader = new Scanner(System.in);
		System.out.print(str + ": ");
		int c = inputReader.nextInt();
		//System.out.println(c);
		return c;
		
	}
	
// JK 15 August 2016
	public static double promptDouble(String str) {
		Scanner inputReader = new Scanner(System.in);
		System.out.print(str + ": ");
		double c = inputReader.nextDouble();
		//System.out.println(c);
		return c;
		
	}
	
	public static String promptString(String str) {
		Scanner inputReader = new Scanner(System.in);
		System.out.print(str + ": ");
		String s = inputReader.nextLine();
		return s;
	}
	
	public static boolean promptReturnBoolean(String str) {
		char c = SachIOUtil.prompt(str);
		if (c != 'y') return false;	
		return true;
	}
	
	public static char shortBool(boolean b) {
		if (b) return 'T';
		else return 'F';
	}
	
	private static final String YEARS_TO_MINUTES = "yyyy-MM-dd HH:mm";
	private static final SimpleDateFormat YEARS_TO_MINUTES_SDF = new SimpleDateFormat(YEARS_TO_MINUTES);

	public static String formatMicroSeconds(long timeMicroSeconds) {
	    String dateTime;
	    synchronized (YEARS_TO_MINUTES_SDF) {
	        dateTime = YEARS_TO_MINUTES_SDF.format(new Date(timeMicroSeconds / 1000));
	    }
	    long secs = timeMicroSeconds % 60000000;
	    return dateTime + String.format(":%09.6f", secs / 1e6);
	}
	
	public static long parseMicroSeconds(String text) throws ParseException {
	    long timeMS;
	    synchronized (YEARS_TO_MINUTES_SDF) {
	        timeMS = YEARS_TO_MINUTES_SDF.parse(text.substring(0, YEARS_TO_MINUTES.length())).getTime();
	    }
	    long microSecs = 0;
	    if (text.length() > YEARS_TO_MINUTES.length() + 1) {
	        double secs = Double.parseDouble(text.substring(YEARS_TO_MINUTES.length() + 1));
	        microSecs = (long) (secs * 1e6 + 0.5);
	    }
	    return timeMS * 1000 + microSecs;
	}
	
//	public static void main(String... args) throws ParseException {
//    String dateTime = "2011-01-17 19:27:59.999650";
//    long timeUS = parseMicroSeconds(dateTime);
//    for (int i = 0; i < 5; i++)
//        System.out.println(formatMicroSeconds(timeUS += 175));
//}
	
	public static String generateFileName(String identifier) {
		
		DateFormat dateTimeFormat = new SimpleDateFormat("yyyy.MM.dd-HH.mm.ss-z");	// date and time, for file name
		Calendar cal = Calendar.getInstance();
		String rootDir = System.getProperty("user.dir");
		String fileIdentifierName = dateTimeFormat.format(cal.getTime());	
		String summaryFileName = rootDir + "/RecordingSummaryFiles/" + fileIdentifierName + "_" + identifier;
		
		return summaryFileName;
	}
	
	public static BufferedWriter writeExptStartTime(BufferedWriter outBufferedSummaryFile, String summaryFileName, Long timeStamp) {
		try {	
			outBufferedSummaryFile = new BufferedWriter(new FileWriter(summaryFileName + ".txt"));
			outBufferedSummaryFile.write("Starting time: " + Long.toString(timeStamp));
			outBufferedSummaryFile.newLine();
			outBufferedSummaryFile.flush(); 
			// Don't close yet, we may write to it during experiment.
		}
		catch(Exception e) {
			System.out.println(e);
		}
		return outBufferedSummaryFile;
	}
	
	public static void writeExptStopTimeAndClose(BufferedWriter outBufferedSummaryFile, Long timeStamp) {
		try {
			outBufferedSummaryFile.write("Stopping time: " + Long.toString(timeStamp));
			outBufferedSummaryFile.close();
		}
		catch(Exception e) {
			System.out.println(e);
		}
	}
}


	
