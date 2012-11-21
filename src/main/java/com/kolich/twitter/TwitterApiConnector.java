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

package com.kolich.twitter;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.kolich.common.DefaultCharacterEncoding.UTF_8;
import static oauth.signpost.OAuth.decodeForm;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.http.HttpParameters;

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kolich.http.HttpClient4Closure;
import com.kolich.http.HttpClient4Closure.HttpResponseEither;
import com.kolich.twitter.exceptions.TwitterApiException;
import com.kolich.twitter.signpost.TwitterCommonsHttpOAuthConsumer;

public final class TwitterApiConnector {
	
	private static final Logger logger__ = 
		LoggerFactory.getLogger(TwitterApiConnector.class);
	
	private static final String API_JSON_FORMAT = "json";
	
	private static final String API_BEGIN_CURSOR = "-1";
	private static final String API_CURSOR_PARAM = "cursor";
	private static final String API_COUNT_PARAM = "count";
	private static final String API_MAXID_PARAM = "max_id";
	private static final String API_SINCEID_PARAM = "since_id";
	private static final String API_STATUS_PARAM = "status";
	private static final String API_TAG_PARAM = "tag";
	private static final String RPP_TAG_PARAM = "rpp";
	private static final String API_SCREEN_NAME_PARAM = "screen_name";
	private static final String API_USER_SEARCH_QUERY_PARAM = "q";
	private static final String API_USER_SEARCH_PERPAGE_PARAM = "per_page";
	
	/**
	 * This value must be "client_auth" (referring to the xAuth process.)
	 */
	private static final String API_XAUTH_MODE_CLIENT_AUTH = "client_auth";
	
	/**
	 * The xAuth mode of authentication against the Twitter API.
	 */
	private static final String API_XAUTH_MODE_PARAM = "x_auth_mode";
	
	/**
	 * The login credential of the User the client is obtaining a
	 * token on behalf of
	 */
	private static final String API_XAUTH_USERNAME_PARAM = "x_auth_username";
	
	/**
	 * The password credential of the User the client is
	 * obtaining a token on behalf of
	 */
	private static final String API_XAUTH_PASSWORD_PARAM = "x_auth_password";
	
	/**
	 * The OAuth parameter that defines the URL the user will be redirected
	 * back to once the OAuth authentication was successful.
	 */
	private static final String API_OAUTH_CALLBACK_URL_PARAM = "oauth_callback";
	
	/**
	 * Once we receive an OAuth token, this is the parameter name we use
	 * when sending it back to the Twitter.
	 */
	private static final String API_OAUTH_TOKEN_PARAM = "oauth_token";
	
	/**
	 * Once we receive an OAuth token, this is the parameter name we use
	 * when sending it back to the Twitter.
	 */
	private static final String API_OAUTH_TOKEN_SECRET_PARAM =
		"oauth_token_secret";
	
	/**
	 * Once we receive an OAuth token, this is the parameter name we use
	 * when sending it back to the Twitter.
	 */
	private static final String API_OAUTH_VERIFIER_PARAM = "oauth_verifier";
	
	/**
	 * Once we've completed the OAuth dance with Twitter, we'll receive
	 * a token, a token secret, a user_id and a username.
	 */
	private static final String API_OAUTH_SCREEN_NAME_PARAM = "screen_name";
	
	/**
	 * The default number of Tweets to retreive from the Twitter API
	 * if a requested count was not specified.
	 */
	private static final int API_TWEETS_DEFAULT_COUNT = 20;
	
	/**
	 * Specifies the number of statuses to retrieve. May not be greater
	 * than 200. (Note the the number of statuses returned may be smaller
	 * than the requested count as retweets are stripped out of the result
	 * set for backwards compatibility.) 
	 */
	private static final int API_TWEETS_MAX_COUNT = 200;
	
	/**
	 * When using Twitter's search API, you can't ask for more than
	 * 100 Tweets that contain a certain hash tag.
	 */
	private static final int API_TAG_TWEETS_MAX_COUNT = 100;
	
	/**
	 * Specifies the number of search results to retrieve.
	 * May not be greater than 20.  
	 */
	private static final int API_USER_SEARCH_PERPAGE_MAX = 20;
	
	/**
	 * If the number of desired user search results is not specified,
	 * defaults to this.
	 */
	private static final int API_USER_SEARCH_PERPAGE_DEFAULT =
		API_USER_SEARCH_PERPAGE_MAX;
	
	// Standard API calls, to be used once OAuth authenticated.
	private static final String USER_API_URL =
		"http://api.twitter.com/1/users/show.%s";
	
	private static final String FRIENDS_API_URL =
		"http://api.twitter.com/1/statuses/friends/%s.%s";
	
	private static final String FOLLOWERS_API_URL =
		"http://api.twitter.com/1/statuses/followers/%s.%s";
	
	private static final String TWEETS_API_URL =
		"http://api.twitter.com/1/statuses/user_timeline/%s.%s";
	private static final String TWEET_USER_SEARCH_API_URL =
		"http://api.twitter.com/1/users/search.%s";
	private static final String TWEET_SEARCH_API_URL =
		"http://search.twitter.com/search.json";
		
	private static final String STATUSES_POST_UPDATE_URL =
		"http://api.twitter.com/1/statuses/update.%s";
	
	// OAuth specific init API calls
	private static final String OAUTH_REQUEST_TOKEN_URL =
		"https://api.twitter.com/oauth/request_token";
	private static final String OAUTH_ACCESS_TOKEN_URL = 
		"https://api.twitter.com/oauth/access_token";
	private static final String OAUTH_AUTHENTICATE_URL = 
		"https://api.twitter.com/oauth/authenticate";
	
	private final HttpClient httpClient_;
	
	private String consumerKey_;
	private String consumerKeySecret_;
	private String apiToken_;
	private String apiTokenSecret_;
	
	public TwitterApiConnector(final HttpClient httpClient) {
		httpClient_ = httpClient;
	}
	
	public HttpResponseEither<Exception,String> getUser(final String username) {
		return getUser(username, API_JSON_FORMAT, null);
	}
	
	public HttpResponseEither<Exception,String> getUser(final String username,
		final String format, final OAuthConsumer consumer) {
		checkNotNull(username, "Username cannot be null!");
		checkNotNull(format, "Format cannot be null!");
		// Build the URL we will issue requests against.
		String url = String.format(USER_API_URL, format);
		final List<NameValuePair> params = new ArrayList<NameValuePair>();		
		params.add(new BasicNameValuePair(API_SCREEN_NAME_PARAM, username));
		// Encode the query string parameters into something useful.
		final String query = URLEncodedUtils.format(params, UTF_8);		
		// Add the query string to the URL.
		url = String.format("%s?%s", url, query);
		logger__.debug("getUser() URL: " + url);
		return doOAuthSignedMethod(new HttpGet(url), consumer);
	}
	
	public HttpResponseEither<Exception,String> getFriends(
		final String username) {
		return getFriends(username, API_BEGIN_CURSOR, null);
	}
	
	public HttpResponseEither<Exception,String> getFriends(
		final String username, final String cursor,
		final OAuthConsumer consumer) {
		return getFriends(username, API_JSON_FORMAT,
			// Cursor can be null, if so then the default value is -1
			(cursor == null) ? API_BEGIN_CURSOR : cursor,
			// The OAuthConsumer object		
			consumer);
	}
	
	public HttpResponseEither<Exception,String> getFriends(final String username,
		final String format, final String cursor,
		final OAuthConsumer consumer) {
		checkNotNull(username, "Username cannot be null!");
		checkNotNull(format, "Format cannot be null!");
		checkNotNull(cursor, "Cursor cannot be null!");
		// Build the URL we will issue requests against.
		String url = String.format(FRIENDS_API_URL,	username, format);
		// Build the list of parameters, right now just the cursor position.
		final List<NameValuePair> params = new ArrayList<NameValuePair>();		
		params.add(new BasicNameValuePair(API_CURSOR_PARAM, cursor));
		// Encode the query string parameters into something useful.
		final String query = URLEncodedUtils.format(params, UTF_8);
		url = String.format("%s?%s", url, query);
		logger__.debug("getFriends() URL: " + url);
		return doOAuthSignedMethod(new HttpGet(url), consumer);
	}
	
	public HttpResponseEither<Exception,String> getFollowers(
		final String username) {
		return getFollowers(username, API_BEGIN_CURSOR, null);
	}
	
	public HttpResponseEither<Exception,String> getFollowers(
		final String username, final String cursor,
		final OAuthConsumer consumer) {
		return getFollowers(username, API_JSON_FORMAT,
			// Cursor can be null, if so then the default value is -1
			(cursor == null) ? API_BEGIN_CURSOR : cursor,
			// The OAuthConsumer object		
			consumer);
	}
	
	public HttpResponseEither<Exception,String> getFollowers(
		final String username, final String format, final String cursor,
		final OAuthConsumer consumer) {
		checkNotNull(username, "Username cannot be null!");
		checkNotNull(format, "Format cannot be null!");
		checkNotNull(cursor, "Cursor cannot be null!");
		// Build the URL we will issue requests against.
		String url = String.format(FOLLOWERS_API_URL, username, format);
		// Build the list of parameters, right now just the cursor position.
		final List<NameValuePair> params = new ArrayList<NameValuePair>();		
		params.add(new BasicNameValuePair(API_CURSOR_PARAM, cursor));
		// Encode the query string parameters into something useful.
		final String query = URLEncodedUtils.format(params, UTF_8);
		url = String.format("%s?%s", url, query);
		logger__.debug("getFollowers() URL: " + url);
		return doOAuthSignedMethod(new HttpGet(url), consumer);
	}
	
	public HttpResponseEither<Exception,String> getTweets(final String username) {
		return getTweets(username, API_JSON_FORMAT,
			API_TWEETS_DEFAULT_COUNT, 0L, 0L,
			// Use a default OAuthConsumer
			null);
	}
	
	public HttpResponseEither<Exception,String> getTweets(final String username,
		final int count, final long maxId, final long sinceId) {
		return getTweets(username, API_JSON_FORMAT,
			// Count cannot be <= zero nor can it be greater
			// than the API max we self-inforce on ourselves.
			(count <= 0 || count > API_TWEETS_MAX_COUNT) ?
				API_TWEETS_DEFAULT_COUNT : count,
			maxId, sinceId,
			// Use a default OAuthConsumer
			null);
	}
	
	public HttpResponseEither<Exception,String> getTweets(final String username,
		final String format, final int count, final long maxId,
		final long sinceId, final OAuthConsumer consumer) {
		checkNotNull(username, "Username cannot be null!");
		checkNotNull(format, "Format cannot be null!");
		// Build the URL we will issue requests against.
		String url = String.format(TWEETS_API_URL, username, format);
		// Build the list of parameters, right now just the
		// Tweet count and page ID.
		final List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair(API_COUNT_PARAM,
			Integer.toString(count)));
		if(maxId > 0L) {
			params.add(new BasicNameValuePair(API_MAXID_PARAM,
				Long.toString(maxId - 1L)));
		}
		if(sinceId > 0L) {
			params.add(new BasicNameValuePair(API_SINCEID_PARAM,
				Long.toString(sinceId)));
		}
		// Encode the query string parameters into something useful.
		final String query = URLEncodedUtils.format(params, UTF_8);
		url = String.format("%s?%s", url, query);
		logger__.debug("getTweets() URL: " + url);
		return doOAuthSignedMethod(new HttpGet(url), consumer);
	}
	
	public HttpResponseEither<Exception,String> getTagTweets(final String tag,
		final int count, final long maxId, final long sinceId) {
		checkNotNull(tag, "Tag cannot be null!");
		// Build the URL we will issue requests against.
		String url = String.format(TWEET_SEARCH_API_URL);
		// Build the list of parameters.
		final List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair(API_TAG_PARAM, tag));
		params.add(new BasicNameValuePair(RPP_TAG_PARAM,
				(count <= 0 || count > API_TAG_TWEETS_MAX_COUNT) ?
					Integer.toString(API_TWEETS_DEFAULT_COUNT) :
						Integer.toString(count)
			));
		if(maxId > 0L) {
			params.add(new BasicNameValuePair(API_MAXID_PARAM,
				Long.toString(maxId - 1L)));
		}
		if(sinceId > 0L) {
			params.add(new BasicNameValuePair(API_SINCEID_PARAM,
				Long.toString(sinceId)));
		}
		// Encode the query string parameters into something useful.
		final String query = URLEncodedUtils.format(params, UTF_8);
		url = String.format("%s?%s", url, query);
		logger__.debug("getTagTweets() URL: " + url);
		return doGet(url);
	}
	
	public HttpResponseEither<Exception,String> getTagTweets(final String tag) {
		return getTagTweets(tag, API_TWEETS_DEFAULT_COUNT, 0L, 0L);
	}
	
	public HttpResponseEither<Exception,String> userGetProfileImageFromUrl(
		final String url) {
		checkNotNull(url, "Avatar URL cannot be null!");
		return doGet(url);
	}
	
	public HttpResponseEither<Exception,String> userSearch(final String query) {
		return userSearch(query, API_JSON_FORMAT,
			API_USER_SEARCH_PERPAGE_DEFAULT,
			// Default OAuthConsumer
			null);
	}
	
	public HttpResponseEither<Exception,String> userSearch(final String query,
		final OAuthConsumer consumer) {
		return userSearch(query, API_JSON_FORMAT,
			API_USER_SEARCH_PERPAGE_DEFAULT, consumer);
	}
	
	public HttpResponseEither<Exception,String> userSearch(final String query,
		final String format, final int perPage,
		final OAuthConsumer consumer) {
		checkNotNull(query, "Query cannot be null!");
		checkNotNull(format, "Format cannot be null!");
		// Build the URL we will issue requests against.
		String url = String.format(TWEET_USER_SEARCH_API_URL, format);
		// Build the list of parameters.
		final List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair(API_USER_SEARCH_QUERY_PARAM, query));
		params.add(new BasicNameValuePair(API_USER_SEARCH_PERPAGE_PARAM,
				(perPage <= 0 || perPage > API_USER_SEARCH_PERPAGE_MAX) ?
					Integer.toString(API_USER_SEARCH_PERPAGE_DEFAULT) :
						Integer.toString(perPage)
			));
		// Encode the query string parameters into something useful.
		final String queryString = URLEncodedUtils.format(params, UTF_8);
		url = String.format("%s?%s", url, queryString);
		logger__.debug("userSearch() URL: " + url);
		return doOAuthSignedMethod(new HttpGet(url), consumer);
	}
	
	public HttpResponseEither<Exception,String> postTweet(final String tweet,
		final OAuthConsumer consumer) {
		return postTweet(tweet, API_JSON_FORMAT, consumer);
	}
	
	public HttpResponseEither<Exception,String> postTweet(final String tweet,
		final String format, final OAuthConsumer consumer) {
		checkNotNull(tweet, "Tweet cannot be null!");
		checkNotNull(format, "Format cannot be null!");
		// Build the URL we will issue requests against.
		String url = String.format(STATUSES_POST_UPDATE_URL, format);
		final HttpPost post = new HttpPost(url);
		final List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair(API_STATUS_PARAM, tweet));
		// Build the request entity.
		try {
			post.setEntity(new UrlEncodedFormEntity(params, UTF_8));
		} catch (UnsupportedEncodingException e) {
			throw new TwitterApiException("Failed to UTF-8 " +
				"encode POST body!", e);
		}
		return doOAuthSignedMethod(post, consumer);
	}
	
	public OAuthConsumer xAuthRetrieveAccessTokenConsumer(
		final String username, final String password) {
		checkNotNull(username, "Username cannot be null!");
		checkNotNull(password, "Password cannot be null!");
		final HttpPost post = new HttpPost(OAUTH_ACCESS_TOKEN_URL);
		OAuthConsumer consumer = new CommonsHttpOAuthConsumer(
				consumerKey_, consumerKeySecret_);
		HttpParameters params = new HttpParameters();
		params.put(API_XAUTH_MODE_PARAM, API_XAUTH_MODE_CLIENT_AUTH, true);
		params.put(API_XAUTH_USERNAME_PARAM, username, true);
		params.put(API_XAUTH_PASSWORD_PARAM, password, true);
		consumer.setAdditionalParameters(params);
		logger__.debug("xAuthApiAuthenticate() URL: " + post.getURI());
		// xAuth authentication requires that we add the mode, username,
		// and password parameters to the POST body as well.
		final List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new BasicNameValuePair(API_XAUTH_MODE_PARAM,
			API_XAUTH_MODE_CLIENT_AUTH));
		postParams.add(new BasicNameValuePair(API_XAUTH_USERNAME_PARAM,
			username));
		postParams.add(new BasicNameValuePair(API_XAUTH_PASSWORD_PARAM,
			password));
		try {
			post.setEntity(new UrlEncodedFormEntity(postParams, UTF_8));
		} catch (UnsupportedEncodingException e) {
			throw new TwitterApiException("Failed to UTF-8 " +
				"encode xAuthApiAuthenticate POST body!", e);
		}
		// Actually send the request to Twitter.
		final HttpResponseEither<Exception,String> response =
			doOAuthSignedMethod(post, consumer);
		if(!response.success()) {
			throw new TwitterApiException("Failed to retreive xAuth " +
				"access token!", response.left());
		}
		// Get the response body as a string, we'll need to parse it
		// out manually to retreive the params we want from the body.
		params = decodeForm(response.right());
		final String token = params.getFirst(API_OAUTH_TOKEN_PARAM);
		final String secret = params.getFirst(API_OAUTH_TOKEN_SECRET_PARAM);
		final String screenName = params.getFirst(API_OAUTH_SCREEN_NAME_PARAM);
		// Create and return a consumer with our newly discovered token,
		// secret, and username.
		return xAuthRetrieveConsumer(token, secret, screenName);
	}
	
	public OAuthConsumer xAuthRetrieveConsumer(final String token,
		final String secret, final String username) {
		final OAuthConsumer consumer = new TwitterCommonsHttpOAuthConsumer(
			consumerKey_, consumerKeySecret_,
			// Set the username if we have one.
			(username != null) ? username : null);
		consumer.setTokenWithSecret(token, secret);
		return consumer;
	}
	
	public OAuthConsumer xAuthRetrieveConsumer(
		final String token, final String secret) {
		return xAuthRetrieveConsumer(token, secret, null);
	}
	
	/**
	 * Given a callback URL, retreive an OAuth request token for
	 * OAuth authentication against the Twitter API.
	 * @param callbackUrl
	 * @return
	 */
	public HttpResponseEither<Exception,String> oAuthRetrieveRequestToken(
		final String callbackUrl) {
		checkNotNull(callbackUrl, "Callback URL cannot be null!");
		final HttpPost post = new HttpPost(OAUTH_REQUEST_TOKEN_URL);
		final OAuthConsumer consumer = new CommonsHttpOAuthConsumer(
			consumerKey_, consumerKeySecret_);
		// The callback URL is encoded as one of the many parameters in
		// the OAuth Http Authorization header that's sent to Twitter
		// to kick off the OAuth dance.  Note that the Http POST body
		// MUST BE EMPTY.
		final HttpParameters params = new HttpParameters();
		params.put(API_OAUTH_CALLBACK_URL_PARAM, callbackUrl, true);
		consumer.setAdditionalParameters(params);
		logger__.debug("oAuthRetrieveRequestToken() URL: " + post.getURI());
		return doOAuthSignedMethod(post, consumer);
	}
	
	/**
	 * Requests an OAuth request token builds a valid OAuth authorize
	 * URL then returns it.  The user can be re-directed to this URL
	 * to authorize/deny access.
	 * @param callbackUrl
	 * @return
	 */
	public String oAuthGetAuthorizeUrl(final String callbackUrl) {
		final HttpResponseEither<Exception,String> response =
			oAuthRetrieveRequestToken(callbackUrl);
		if(!response.success()) {
			throw new TwitterApiException("Failed to build Twitter OAuth " +
				"authorize URL.", response.left());
		}
		// Get the response body as a string, we'll need to parse it
		// out manually to retreive the params we want from the body.
		final HttpParameters params = decodeForm(response.right());
		final String token = params.getFirst(API_OAUTH_TOKEN_PARAM);
		logger__.debug("Retreived OAuth token: " + token);
		return String.format("%s?%s=%s", OAUTH_AUTHENTICATE_URL,
			API_OAUTH_TOKEN_PARAM, token);
	}
	
	/**
	 * Given a token, a token secret, and a username returns a pre-loaded
	 * {@link OAuthConsumer} for the caller.  Has the apps default consumer
	 * key and consumer key secret pre-set.
	 * @param token
	 * @param secret
	 * @param username
	 * @return
	 */
	public OAuthConsumer oAuthGetConsumer(final String token,
		final String secret, final String username) {
		// Generate a new OAuthConsumer with the fetched token and token
		// secret pre-set for the caller.
		final OAuthConsumer consumer = new TwitterCommonsHttpOAuthConsumer(
			consumerKey_, consumerKeySecret_, username);
		consumer.setTokenWithSecret(token, secret);
		return consumer;
	}
	
	/**
	 * Retreives an OAuth access token once a valid token and token verifier
	 * have been received.  Returns a new {@link TwitterCommonsHttpOAuthConsumer}
	 * pre-loaded with the resulting token and token secret as
	 * authenticated by the user.
	 * @param token
	 * @param verifier
	 * @return
	 */
	public OAuthConsumer oAuthRetrieveAccessTokenConsumer(final String token,
		final String verifier) {
		final HttpParameters params = oAuthRetrieveAccessTokenParams(token,
				verifier);
		final String username = params.getFirst(API_OAUTH_SCREEN_NAME_PARAM);
		final String newToken = params.getFirst(API_OAUTH_TOKEN_PARAM);
		final String newSecret = params.getFirst(API_OAUTH_TOKEN_SECRET_PARAM);
		// Generate a new OAuthConsumer with the fetched token and token
		// secret pre-set for the caller.
		final OAuthConsumer consumer = new TwitterCommonsHttpOAuthConsumer(
			consumerKey_, consumerKeySecret_, username);
		consumer.setTokenWithSecret(newToken, newSecret);
		return consumer;
	}
	
	private final HttpParameters oAuthRetrieveAccessTokenParams(
		final String token, final String verifier) {
		final HttpResponseEither<Exception,String> response =
			oAuthRetrieveAccessToken(token, verifier);
		if(!response.success()) {
			throw new TwitterApiException("Failed to retrieve O-Auth " +
				"access token parameters.", response.left());
		}
		return decodeForm(response.right());
	}
	
	private final HttpResponseEither<Exception,String> oAuthRetrieveAccessToken(
		final String token, final String verifier) {
		checkNotNull(token, "Token cannot be null!");
		checkNotNull(verifier, "Verifier cannot be null!");
		final HttpPost post = new HttpPost(OAUTH_ACCESS_TOKEN_URL);
		final OAuthConsumer consumer = new CommonsHttpOAuthConsumer(
			consumerKey_, consumerKeySecret_);
		final HttpParameters params = new HttpParameters();
		params.put(API_OAUTH_TOKEN_PARAM, token, true);
		params.put(API_OAUTH_VERIFIER_PARAM, verifier, true);
		consumer.setAdditionalParameters(params);
		logger__.debug("oAuthRetrieveAccessToken() URL: " + post.getURI());
		return doOAuthSignedMethod(post, consumer);
	}
			
	private final HttpResponseEither<Exception,String> doOAuthSignedMethod(
		final HttpRequestBase base, final OAuthConsumer consumer) {
		return new HttpClient4Closure<Exception,String>(httpClient_) {
			@Override
			public void before(final HttpRequestBase request) throws Exception {
				// If consumer is null, then we need to generate a default one
				// using the key, secret, token and token secret.
				final OAuthConsumer signWith;
				if(consumer == null) {
					signWith = new CommonsHttpOAuthConsumer(consumerKey_,
						consumerKeySecret_);
					signWith.setTokenWithSecret(apiToken_, apiTokenSecret_);
				} else {
					signWith = consumer;
				}
				signWith.sign(request);
			}
			@Override
			public String success(final HttpSuccess success) throws Exception {
				return EntityUtils.toString(success.getResponse().getEntity(),
					UTF_8);
			}
			@Override
			public Exception failure(final HttpFailure failure) {
				return failure.getCause();
			}
		}.request(base);
	}
	
	private final HttpResponseEither<Exception,String> doGet(final String url) {
		return new HttpClient4Closure<Exception,String>(httpClient_) {
			@Override
			public String success(final HttpSuccess success) throws Exception {
				return EntityUtils.toString(success.getResponse().getEntity(),
					UTF_8);
			}
			@Override
			public Exception failure(final HttpFailure failure) {
				return failure.getCause();
			}
		}.get(url);
	}
		
	public void setConsumerKey(String consumerKey) {
		consumerKey_ = consumerKey;
	}
	
	public void setConsumerKeySecret(String consumerKeySecret) {
		consumerKeySecret_ = consumerKeySecret;
	}
	
	public void setApiToken(String apiToken) {
		apiToken_ = apiToken;
	}
	
	public void setApiTokenSecret(String apiTokenSecret) {
		apiTokenSecret_ = apiTokenSecret;
	}
		
}
