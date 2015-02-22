package ls;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.*;

/** get dates from file names */
public class FileDater {
	private final Pattern datePattern;
	private final DateFormat dateFormat;
	private final DateFormat dateFormatHour;
	
	public FileDater (boolean parseDate) {
		if (parseDate) {
			dateFormat = new SimpleDateFormat("yyyy-MM-dd");
			dateFormatHour = new SimpleDateFormat("yyyy-MM-dd-HH");
			datePattern = Pattern.compile("\\d{4}-\\d{2}-\\d{2}(-\\d{2})?");
			
		} else {
			dateFormat = null;
			dateFormatHour = null;
			datePattern = null;
		}
	}
	
	public Date getFileDate (long fileTime, String fileName) {
		Date date = null;
		
		if (datePattern != null) {
			if (fileName.length() > 0) {
				Matcher mat = datePattern.matcher(fileName);
				
				if (mat.find()) {
					String dateStr = mat.group();
					
					try {
						if (mat.group(1) != null) {
							date = dateFormatHour.parse(dateStr);
						} else {
							date = dateFormat.parse(dateStr);
						}
						
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
