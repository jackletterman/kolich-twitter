/**
 * Copyright (c) 2012 Mark S. Kolich
 * http://mark.koli.ch
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package com.kolich.twitter.date;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import com.kolich.twitter.exceptions.TwitterApiException;

public final class TwitterDateFormat {

	// Thu May 13 14:24:37 +0000 2010
	// Wed May 12 22:32:30 +0000 2010	
	private static final String TWITTER_API_DATE_FORMAT =
		"EEE MMM d HH:mm:ss Z yyyy";
	
	// Mon, 21 Jun 2010 18:56:18 +0000
	private static final String TWITTER_SEARCH_API_FORMAT =
		"EEE, d MMM yyyy HH:mm:ss Z";
	
	private static final DateFormat twitterDateFormat__;
	private static final DateFormat twitterSearchApiDateFormat__;
	static {
		twitterDateFormat__ = new SimpleDateFormat(TWITTER_API_DATE_FORMAT);
		twitterDateFormat__.setTimeZone(TimeZone.getTimeZone("GMT"));
		twitterSearchApiDateFormat__ = new SimpleDateFormat(TWITTER_SEARCH_API_FORMAT);
		twitterSearchApiDateFormat__.setTimeZone(TimeZone.getTimeZone("GMT"));
	}
	
	// Cannot be instantiated.
	private TwitterDateFormat() { }
	
	public static synchronized final String format(Date d) {
		return twitterDateFormat__.format(d);
	}
	
	public static synchronized final Date parse(String date) {
		Date result = null;
		try {
			result = twitterDateFormat__.parse(date);
		} catch (ParseException e) {
			try {
				result = twitterSearchApiDateFormat__.parse(date);
			} catch (ParseException f) {
				throw new TwitterApiException("Failed to parse: " + date, f);
			}
		}
		return result;
	}
	
	public static final DateFormat getNewInstance() {
		return new SimpleDateFormat(TWITTER_API_DATE_FORMAT);
	}
	
	public static final String getTwitterApiFormatString() {
		return TWITTER_API_DATE_FORMAT;
	}
	
	public static final String getTwitterSearchApiFormatString() {
		return TWITTER_SEARCH_API_FORMAT;
	}
	
}
