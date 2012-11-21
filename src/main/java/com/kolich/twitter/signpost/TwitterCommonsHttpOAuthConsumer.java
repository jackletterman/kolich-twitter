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

package com.kolich.twitter.signpost;

import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;

public final class TwitterCommonsHttpOAuthConsumer
	extends CommonsHttpOAuthConsumer {
	
	private static final long serialVersionUID = -6236727934196745589L;
	
	/**
	 * The authenticated username that this token and secret belongs to.
	 */
	private final String username_;

	public TwitterCommonsHttpOAuthConsumer(String consumerKey,
		String consumerSecret, String username) {
		super(consumerKey, consumerSecret);
		username_ = username;
	}
	
	public String getUsername() {
		return username_;
	}
		
	@Override
	public String toString() {
		return String.format("(token=%s, secret=%s, username=%s)",
			getToken(), getTokenSecret(), getUsername());
	}

}
