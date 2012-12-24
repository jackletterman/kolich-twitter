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

package com.kolich.twitter.entities;

import java.util.LinkedList;
import java.util.List;

import com.google.gson.annotations.SerializedName;

public class TweetSearchResults extends TwitterEntity {
	
	@SerializedName("statuses")
	private final List<Tweet> statuses_;
		
	public TweetSearchResults(List<Tweet> tweets) {
		statuses_ = tweets;
	}
	
	public TweetSearchResults() {
		this(new LinkedList<Tweet>());
	}
	
	public List<Tweet> getResults() {
		return new LinkedList<Tweet>(statuses_);
	}

	// Straight from Eclipse
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((statuses_ == null) ? 0 : statuses_.hashCode());
		return result;
	}

	// Straight from Eclipse
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TweetSearchResults other = (TweetSearchResults) obj;
		if (statuses_ == null) {
			if (other.statuses_ != null)
				return false;
		} else if (!statuses_.equals(other.statuses_))
			return false;
		return true;
	}

}
