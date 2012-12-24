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

import java.util.Date;

import com.google.gson.annotations.SerializedName;

public class User extends TwitterEntity implements Comparable<User> {
	
	@SerializedName("screen_name")	
	private final String screenName_;
	
	@SerializedName("name")
	private final String name_;	
	
	@SerializedName("created_at")
	private final Date createdAt_;
	
	@SerializedName("profile_image_url")
	private final String profileImageUrl_;
	
	public User(String screenName, String name, Date createdAt,
		String profileImageUrl) {
		screenName_ = screenName;
		name_ = name;
		createdAt_ = createdAt;
		profileImageUrl_ = profileImageUrl;
	}
	
	public User() {
		this(null, null, null, null);
	}
	
	public String getScreenName() {
		return screenName_;
	}
	
	public String getName() {
		return name_;
	}
	
	public Date getCreatedAt() {
		return new Date(createdAt_.getTime());
	}

	public String getProfileImageUrl_() {
		return profileImageUrl_;
	}
	
	// Straight from Eclipse
	// Only uses the name (twitter ID/userame) field.
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((screenName_ == null) ? 0 : screenName_.hashCode());
		return result;
	}

	// Straight from Eclipse
	// Only uses the name (twitter ID/userame) field.
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		User other = (User) obj;
		if (screenName_ == null) {
			if (other.screenName_ != null)
				return false;
		} else if (!screenName_.equalsIgnoreCase(other.screenName_))
			return false;
		return true;
	}

	@Override
	public int compareTo(User t) {
		return getScreenName().compareToIgnoreCase(t.getScreenName());
	}
	
}
