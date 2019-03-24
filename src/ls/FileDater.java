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
	
	/** never returns null */
	public FileDate getFileDate (long fileTime, String fileName) {
		String source = null;
		Date date = null;
		
		// this could be extended to look at first line of file
		
		if (datePattern != null) {
			if (fileName.length() > 0) {
				Matcher mat = datePattern.matcher(fileName);
				
				if (mat.find()) {
					String dateStr = mat.group();
					
					try {
						if (mat.group(1) != null) {
							source = "filename hour pattern";
							date = dateFormatHour.parse(dateStr);
						} else {
							source = "filename day pattern";
							date = dateFormat.parse(dateStr);
						}
						
					} catch (Exception e) {
						System.out.println("could not parse date " + dateStr + ": " + e);
					}
					
				} else {
					System.out.println("could not match date in " + fileName);
				}
			}
		}
		
		if (date == null && fileTime > 0) {
			source = "file modified time";
			date = new Date(fileTime);
		}
		
		return new FileDate(source, date);
	}
}
