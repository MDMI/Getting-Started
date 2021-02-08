package org.mdmi.core.engine.converter;

import java.util.Date;

/**
 * @author YMolodkov
 * @author jklann
 * @since 0.0.1
 */
final public class DateWrapper {
	private final Date date;

	private final String originalFormat;

	private final String originalString;

	public DateWrapper(Date date, String originalFormat, String originalString) {
		this.originalString = originalString;
		this.originalFormat = originalFormat;
		this.date = date;
	}

	public Date getDate() {
		return date;
	}

	public String getOriginalFormat() {
		return originalFormat;
	}

	public String getOriginalString() {
		return originalString;
	}

	@Override
	public String toString() {
		if (date != null) {
			return date.toString();
		} else {
			return originalString;
		}
	}

} // DateWrapper
