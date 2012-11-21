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

public final class Tweet extends TwitterEntity implements Comparable<Tweet> {
	
	@SerializedName("id_str")
	private String id_;
	
	@SerializedName("created_at")
	private Date createdAt_;
	
	@SerializedName("text")
	private String text_;
	
	public Tweet(String id, Date createdAt, String text) {
		id_ = id;
		createdAt_ = createdAt;
		text_ = text;
	}
	
	public Tweet() {
		this(null, null, null);
	}
	
	public String getId() {
		return id_;
	}

	public Tweet setId(String id) {
		id_ = id;
		return this;
	}
	
	public Date getCreatedAt() {
		return createdAt_;
	}

	public Tweet setCreatedAt(Date createdAt) {
		createdAt_ = createdAt;
		return this;
	}

	public String getText() {
		return text_;
	}

	public Tweet setText(String text) {
		text_ = text;
		return this;
	}
	
	/*
	public String getHtml() {
		return (text_ != null) ? makeHyperlinks(text_) : null;
	}
	*/
	
	// Straight from Eclipse
	// Uses only the id field
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id_ == null) ? 0 : id_.hashCode());
		return result;
	}

	// Straight from Eclipse
	// Uses only the id field
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Tweet other = (Tweet) obj;
		if (id_ == null) {
			if (other.id_ != null)
				return false;
		} else if (!id_.equals(other.id_))
			return false;
		return true;
	}

	@Override
	public int compareTo(Tweet t) {
		return t.getCreatedAt().compareTo(getCreatedAt());
	}

}
