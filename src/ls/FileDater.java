package ls;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.*;

/** get dates from file names */
public class FileDater {
	private final Pattern datePattern;
	private final DateFormat dateFormat;
	
	public FileDater (boolean parseDate, String dateFormatStr) {
		if (parseDate) {
			if (dateFormatStr.length() == 0) {
				dateFormatStr = "yyyy-MM-dd";
			}
			
			dateFormat = new SimpleDateFormat(dateFormatStr);
			String datePatStr = dateFormatStr.replaceAll("[A-Za-z]", Matcher.quoteReplacement("\\d"));
			System.out.println("date pattern " + datePatStr);
			datePattern = Pattern.compile(datePatStr);
			
		} else {
			dateFormat = null;
			datePattern = null;
		}
	}
	
	public Date getFileDate (long fileTime, String fileName) {
		Date date = null;
		
		if (datePattern != null) {
			if (fileName.length() > 0) {
				Matcher mat = datePattern.matcher(fileName);
				
				if (mat.find()) {
					String dateStr = mat.group(0);
					
					try {
						date = dateFormat.parse(dateStr);
						
					} catch (Exception e) {
						e.printStackTrace();
					}
					
				} else {
					System.out.println("could not match date in " + fileName);
				}
			}
		}
		
		if (date == null && fileTime > 0) {
			date = new Date(fileTime);
		}
		
		return date;
	}
}
