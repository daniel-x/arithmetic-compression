/*
 * DateTimeFormatter 2013-04-22
 * 
 * This is free and unencumbered software released into the public domain.
 * 
 * Anyone is free to copy, modify, publish, use, compile, sell, or distribute
 * this software, either in source code form or as a compiled binary, for any
 * purpose, commercial or non-commercial, and by any means.
 * 
 * In jurisdictions that recognize copyright laws, the author or authors of this
 * software dedicate any and all copyright interest in the software to the
 * public domain. We make this dedication for the benefit of the public at large
 * and to the detriment of our heirs and successors. We intend this dedication
 * to be an overt act of relinquishment in perpetuity of all present and future
 * rights to this software under copyright law.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * 
 * For more information, please refer to <http://unlicense.org/>
 */

package arithmetic_compression;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

/**
 * A formatter for durations and for instants (= a date+time). All formatting by
 * this class is done the same way everywhere, no matter what locale and
 * environment settings you have.
 * 
 * @author Daniel Strecker
 */
public class TimeDurationFormatter {

	/**
	 * Formats the specified milliseconds since epoch 1970-01-01 00:00 to a human
	 * readable format in UTC time.
	 */
	public static String formatTime(long millis) {
		return UTC_DATE_FORMAT.get().format(millis);
	}

	/**
	 * Formats the specified milliseconds since epoch 1970-01-01 00:00 to a human
	 * readable format in UTC time, including milliseconds.
	 */
	public static String formatTimeMs(long millis) {
		return UTC_DATE_FORMAT_MS.get().format(millis);
	}

	/**
	 * Takes a nanosecond duration, formats it to seconds in the format '#.000'
	 * without unit indication, always, fast. Good for generating data tables for
	 * later use.
	 * <p>
	 * You can call <code>System.nanoTime()</code> once before some processing and
	 * once afterwards and feed the difference into this method.
	 * </p>
	 */
	public static String formatDuraS(long duration) {
		duration = (duration + (500L * 1000L)) / (1000L * 1000L);

		long s = duration / 1000L;
		String sStr = Long.toString(s);
		int ms = (int) (duration % 1000L);
		String msStr = Integer.toString(ms);

		char[] ca = new char[sStr.length() + 4];
		int pos = sStr.length();
		sStr.getChars(0, pos, ca, 0);
		ca[pos++] = '.';
		if (ms >= 100) {
			msStr.getChars(0, 3, ca, pos);
		} else if (ms >= 10) {
			ca[pos++] = '0';
			ca[pos++] = msStr.charAt(0);
			ca[pos] = msStr.charAt(1);
		} else {
			ca[pos++] = '0';
			ca[pos++] = '0';
			ca[pos] = msStr.charAt(0);
		}

		return new String(ca);
	}

	/**
	 * Takes nanosecond duration and formats it to a human readable String by
	 * converting it to useful time units.<br/>
	 * You can use <code>System.nanoTime()</code> and feed it into this method.<br/>
	 * <br/>
	 * Time units are:<br/>
	 * <ul>
	 * <li>ps (picosecond, 0.000000000001s)</li>
	 * <li>ns (nanosecond, 0.000000001s)</li>
	 * <li>us (microsecond, 0.000001s, like µs, but more compatible as us)</li>
	 * <li>ms (mllisecond, 0.001s)</li>
	 * <li>s (second, 1s)</li>
	 * <li>m (minute, 60s)</li>
	 * <li>h (hour, 3600s)</li>
	 * <li>d (day, 86400s)</li>
	 * </ul>
	 */
	public static String formatDura(double duration) {
		boolean pos;
		if (duration >= 0) {
			pos = true;
		} else {
			duration = -duration;
			pos = false;
		}

		String result;
		if (duration < 1.0D) {
			// less than a nanosecond, so output in ps
			duration = Math.round(duration * (1000.0D * 1000.0D)) / 1000.0D;
			result = String.format(LOCALE_US, "%.3f", duration) + "ps";

		} else if (duration < 1000.0D) {
			// less than a microsecond, so output in ns
			duration = Math.round(duration * 1000.0D) / 1000.0D;
			result = String.format(LOCALE_US, "%.3f", duration) + "ns";

		} else if (duration < 1000.0D * 1000.0D) {
			// less than a millisecond, so output in us
			duration = Math.round(duration) / 1000.0D;
			result = String.format(LOCALE_US, "%.3f", duration) + "us";

		} else if (duration < 1000.0D * 1000.0D * 1000.0D) {
			// less than a second, so output in ms
			duration = Math.round(duration / 1000.0D) / 1000.0D;
			result = String.format(LOCALE_US, "%.3f", duration) + "ms";

		} else if (duration < 1000.0D * 1000.0D * 1000.0D * 60.0D) {
			// less than a minute
			duration = Math.round(duration / (1000.0D * 1000.0D)) / 1000.0D;
			result = String.format(LOCALE_US, "%.3f", duration) + "s";

		} else if (duration < 1000.0D * 1000.0D * 1000.0D * 60.0D * 60.0D) {
			// less than an hour
			duration = Math.round(duration / (1000.0D * 1000.0D)) / 1000.0D;

			double s = duration - ((long) (duration / 60)) * 60;
			long duraInSec = (long) duration;
			long m = (duraInSec / 60L) % 60L;

			result = String.format(LOCALE_US, "%dm%02.1fs", m, s);

		} else if (duration < 1000.0D * 1000.0D * 1000.0D * 60.0D * 60.0D * 24.0D) {
			// less than a day
			duration = Math.round(duration / (1000.0D * 1000.0D * 1000.0D));

			long duraInSec = (long) duration;

			long s = duraInSec % 60L;
			long m = (duraInSec / 60L) % 60L;
			long h = (duraInSec / 3600L);

			result = String.format(LOCALE_US, "%dh%02dm%02ds", h, m, s);

		} else {
			// a day or more
			duration = Math.round(duration / (1000.0D * 1000.0D * 1000.0D));

			long duraInSec = (long) duration;

			long s = duraInSec % 60L;
			long m = (duraInSec / 60L) % 60L;
			long h = (duraInSec / (60L * 60L)) % 24L;
			long d = (duraInSec / (60L * 60L * 24L));

			result = String.format(LOCALE_US, "%dd%02dh%02dm%02ds", d, h, m, s);
		}

		if (pos) {
			return result;
		} else {
			return "-" + result;
		}
	}

	private static Locale LOCALE_US = Locale.US;

	private static class ThreadLocalUtcDateFormat extends ThreadLocal<SimpleDateFormat> {

		private SimpleDateFormat prototype;

		public ThreadLocalUtcDateFormat(SimpleDateFormat prototype) {
			this.prototype = prototype;
		}

		@Override
		public SimpleDateFormat initialValue() {
			return (SimpleDateFormat) prototype.clone();
		}
	}

	private static ThreadLocalUtcDateFormat UTC_DATE_FORMAT;

	private static ThreadLocalUtcDateFormat UTC_DATE_FORMAT_MS;

	static {
		SimpleDateFormat fmt;

		fmt = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
		fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
		UTC_DATE_FORMAT = new ThreadLocalUtcDateFormat(fmt);

		fmt = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss.SSS");
		fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
		UTC_DATE_FORMAT_MS = new ThreadLocalUtcDateFormat(fmt);
	}
}
