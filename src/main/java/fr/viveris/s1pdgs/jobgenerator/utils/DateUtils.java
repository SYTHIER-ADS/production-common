package fr.viveris.s1pdgs.jobgenerator.utils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import fr.viveris.s1pdgs.jobgenerator.exception.InternalErrorException;

/**
 * Utilities around date
 * 
 * @author Cyrielle
 *
 */
public final class DateUtils {
	
	/**
	 * Default constructor
	 */
	private DateUtils() {
		
	}

	/**
	 * Convert a date in ISO format "yyyyMMdd'T'HHmmss"
	 * 
	 * @param dateStr
	 * @return
	 * @throws InternalErrorException
	 */
	public static Date convertDateIso(final String dateStr) throws InternalErrorException {
		return convertWithSimpleDateFormat(dateStr, "yyyyMMdd'T'HHmmss");
	}

	/**
	 * Convert a date from given format
	 * 
	 * @param dateStr
	 * @param dateFormat
	 * @return
	 * @throws InternalErrorException
	 */
	public static Date convertWithSimpleDateFormat(final String dateStr, final String dateFormat)
			throws InternalErrorException {
		try {
			DateFormat format = new SimpleDateFormat(dateFormat);
			return format.parse(dateStr);
		} catch (ParseException pe) {
			throw new InternalErrorException("Cannot convert date " + dateStr, pe);
		}
	}
}
