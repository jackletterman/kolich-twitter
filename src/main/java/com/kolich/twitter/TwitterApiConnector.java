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
import static com.kolich.twitter.entities.TwitterEntity.getTwitterGsonBuilder;
import static oauth.signpost.OAuth.decodeForm;
import static org.apache.http.HttpStatus.SC_OK;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.http.HttpParameters;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.kolich.http.HttpClient4Closure.HttpFailure;
import com.kolich.http.HttpClient4Closure.HttpResponseEither;
import com.kolich.http.helpers.ByteArrayOrHttpFailureClosure;
import com.kolich.http.helpers.GsonOrHttpFailureClosure;
import com.kolich.http.helpers.StringOrHttpFailureClosure;
import com.kolich.twitter.entities.Tweet;
import com.kolich.twitter.entities.TweetSearchResults;
import com.kolich.twitter.entities.User;
import com.kolich.twitter.entities.UserList;
import com.kolich.twitter.exceptions.TwitterApiException;
import com.kolich.twitter.signpost.TwitterApiCommonsHttpOAuthConsumer;

public final class TwitterApiConnector {
	
	private static final Logger logger__ = 
		LoggerFactory.getLogger(TwitterApiConnector.class);
		
	private static final String API_BEGIN_CURSOR = "-1";
	private static final String API_CURSOR_PARAM = "cursor";
	private static final String API_COUNT_PARAM = "count";
	private static final String API_MAXID_PARAM = "max_id";
	private static final String API_SINCEID_PARAM = "since_id";
	private static final String API_STATUS_PARAM = "status";
	private static final String API_QUERY_PARAM = "q";
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
	private static final int API_SEARCH_TWEETS_MAX_COUNT = 100;
	
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
	
	// Standard API calls, to be used once OAuth authenticated	
	private static final String FRIENDS_LIST_API_URL =
		"https://api.twitter.com/1.1/friends/list.json";
	
	private static final String FOLLOWERS_LIST_API_URL =
		"https://api.twitter.com/1.1/followers/list.json";	
	
	private static final String USERS_SHOW_URL =
		"https://api.twitter.com/1.1/users/show.json";
	private static final String USERS_SEARCH_URL =
		"https://api.twitter.com/1.1/users/search.json";
	
	private static final String TWEET_SEARCH_URL =
		"https://api.twitter.com/1.1/search/tweets.json";
	
	private static final String STATUSES_USER_TIMELINE_URL =
		"https://api.twitter.com/1.1/statuses/user_timeline.json";	
	private static final String STATUSES_UPDATE_URL =
		"https://api.twitter.com/1.1/statuses/update.json";
	
	// OAuth specific API resources
	private static final String OAUTH_REQUEST_TOKEN_URL =
		"https://api.twitter.com/oauth/request_token";
	private static final String OAUTH_ACCESS_TOKEN_URL = 
		"https://api.twitter.com/oauth/access_token";
	private static final String OAUTH_AUTHENTICATE_URL = 
		"https://api.twitter.com/oauth/authenticate";
	
	private final HttpClient httpClient_;
	private final GsonBuilder gson_;
	
	private String consumerKey_;
	private String consumerKeySecret_;
	private String apiToken_;
	private String apiTokenSecret_;
		
	public TwitterApiConnector(final HttpClient httpClient,
		final GsonBuilder gson) {
		httpClient_ = httpClient;
		gson_ = gson;
	}
	
	public TwitterApiConnector(final HttpClient httpClient) {
		this(httpClient, getTwitterGsonBuilder());
	}
	
	private abstract class TwitterApiGsonClosure<T>
		extends GsonOrHttpFailureClosure<T> {
		private final OAuthConsumer consumer_;
		private final int expectStatus_;
		public TwitterApiGsonClosure(final HttpClient client, final Gson gson,
			final Type type, final OAuthConsumer consumer,
			final int expectStatus) {
			super(client, gson, type);
			// If consumer is null, then we need to generate a default one
			// using the key, secret, token and token secret.
			consumer_ = (consumer == null) ? oAuthBuildConsumer() : consumer;
			expectStatus_ = expectStatus;
		}
		public TwitterApiGsonClosure(final HttpClient client, final Gson gson,
			final Class<T> clazz, final OAuthConsumer consumer,
			final int expectStatus) {
			this(client, gson, TypeToken.get(clazz).getType(), consumer,
				expectStatus);
		}
		@Override
		public void before(final HttpRequestBase request) throws Exception {
			request.setURI(getFinalURI(request.getURI()));
			// OAuth sign the request.
			consumer_.sign(request);
		}
		/**
		 * Override this method if you need to modify the request URI
		 * before execution.  Allows the appending/inclusion of query
		 * parameters (if a GET), etc.
		 */
		public URI getFinalURI(final URI uri) throws Exception {
			// Default behavior is no modifications to final URI.
			return uri;
		}
		@Override
		public boolean check(final HttpResponse response,
			final HttpContext context) {
			return expectStatus_ == response.getStatusLine().getStatusCode();
		}
	}
	
	private abstract class TwitterApiStringOrHttpFailureClosure
		extends StringOrHttpFailureClosure {
		private final OAuthConsumer consumer_;
		private final int expectStatus_;
		public TwitterApiStringOrHttpFailureClosure(final HttpClient client,
			final OAuthConsumer consumer, final int expectStatus) {
			super(client);
			// If consumer is null, then we need to generate a default one
			// using the key, secret, token and token secret.
			consumer_ = (consumer == null) ? oAuthBuildConsumer() : consumer;
			expectStatus_ = expectStatus;
		}
		@Override
		public void before(final HttpRequestBase request) throws Exception {
			request.setURI(getFinalURI(request.getURI()));
			// OAuth sign the request.
			consumer_.sign(request);
		}
		/**
		 * Override this method if you need to modify the request URI
		 * before execution.  Allows the appending/inclusion of query
		 * parameters (if a GET), etc.
		 */
		public URI getFinalURI(final URI uri) throws Exception {
			// Default behavior is no modifications to final URI.
			return uri;
		}
		@Override
		public boolean check(final HttpResponse response,
			final HttpContext context) {
			return expectStatus_ == response.getStatusLine().getStatusCode();
		}
	}
	
	public HttpResponseEither<HttpFailure,User> getUser(final String username) {
		return getUser(username, null);
	}
	
	public HttpResponseEither<HttpFailure,User> getUser(final String username,
		final OAuthConsumer consumer) {
		checkNotNull(username, "Username cannot be null!");
		return new TwitterApiGsonClosure<User>(httpClient_, gson_.create(),
			User.class, consumer, SC_OK) {
			@Override
			public URI getFinalURI(final URI uri) throws Exception {
				return new URIBuilder(uri)
					.addParameter(API_SCREEN_NAME_PARAM, username).build();
			}
		}.get(USERS_SHOW_URL);
	}
	
	public HttpResponseEither<HttpFailure,UserList> getFriends(
		final String username) {
		return getFriends(username, API_BEGIN_CURSOR, null);
	}
		
	public HttpResponseEither<HttpFailure,UserList> getFriends(
		final String username, final String cursor,
		final OAuthConsumer consumer) {
		checkNotNull(username, "Username cannot be null!");
		checkNotNull(cursor, "Cursor cannot be null!");
		return new TwitterApiGsonClosure<UserList>(httpClient_, gson_.create(),
			UserList.class, consumer, SC_OK) {
			@Override
			public URI getFinalURI(final URI uri) throws Exception {
				return new URIBuilder(uri)
					.addParameter(API_SCREEN_NAME_PARAM, username)
					// Cursor can be null, if so then the default value is -1
					.addParameter(API_CURSOR_PARAM,
						(cursor == null) ? API_BEGIN_CURSOR : cursor)
					.build();
			}
		}.get(FRIENDS_LIST_API_URL);
	}
	
	public HttpResponseEither<HttpFailure,UserList> getFollowers(
		final String username) {
		return getFollowers(username, API_BEGIN_CURSOR);
	}
	
	public HttpResponseEither<HttpFailure,UserList> getFollowers(
		final String username, final String cursor) {
		return getFollowers(username, cursor, null);
	}
		
	public HttpResponseEither<HttpFailure,UserList> getFollowers(
		final String username, final String cursor,
		final OAuthConsumer consumer) {
		checkNotNull(username, "Username cannot be null!");
		return new TwitterApiGsonClosure<UserList>(httpClient_, gson_.create(),
			UserList.class, consumer, SC_OK) {
			@Override
			public URI getFinalURI(final URI uri) throws Exception {
				return new URIBuilder(uri)
					.addParameter(API_SCREEN_NAME_PARAM, username)
					// Cursor can be null, if so then the default value is -1
					.addParameter(API_CURSOR_PARAM,
						(cursor == null) ? API_BEGIN_CURSOR : cursor)
					.build();
			}
		}.get(FOLLOWERS_LIST_API_URL);
	}
	
	public HttpResponseEither<HttpFailure,List<Tweet>> getTweets(
		final String username) {
		return getTweets(username, API_TWEETS_DEFAULT_COUNT, 0L, 0L);
	}
	
	public HttpResponseEither<HttpFailure,List<Tweet>> getTweets(
		final String username, final int count, final long maxId,
		final long sinceId) {
		return getTweets(username,
			// Count cannot be <= zero nor can it be greater
			// than the API max we self-inforce on ourselves.
			(count <= 0 || count > API_TWEETS_MAX_COUNT) ?
				API_TWEETS_DEFAULT_COUNT : count,
			maxId, sinceId,
			// Use a default OAuthConsumer
			null);
	}
	
	public HttpResponseEither<HttpFailure,List<Tweet>> getTweets(
		final String username, final int count, final long maxId,
		final long sinceId, final OAuthConsumer consumer) {
		checkNotNull(username, "Username cannot be null!");		
		return new TwitterApiGsonClosure<List<Tweet>>(httpClient_, gson_.create(),
			new TypeToken<List<Tweet>>(){}.getType(), consumer, SC_OK) {
			@Override
			public URI getFinalURI(final URI uri) throws Exception {
				final URIBuilder builder = new URIBuilder(uri)
					.addParameter(API_SCREEN_NAME_PARAM, username)
					.addParameter(API_COUNT_PARAM, Integer.toString(count));
				if(maxId > 0L) {
					builder.addParameter(API_MAXID_PARAM,
						Long.toString(maxId - 1L));
				}
				if(sinceId > 0L) {
					builder.addParameter(API_SINCEID_PARAM,
						Long.toString(sinceId));
				}
				return builder.build();
			}
		}.get(STATUSES_USER_TIMELINE_URL);
	}
	
	public HttpResponseEither<HttpFailure,TweetSearchResults> searchTweets(
		final String query, final int count, final long sinceId,
		final OAuthConsumer consumer) {
		checkNotNull(query, "Query cannot be null!");
		return new TwitterApiGsonClosure<TweetSearchResults>(httpClient_,
			gson_.create(), TweetSearchResults.class, consumer, SC_OK) {
			@Override
			public URI getFinalURI(final URI uri) throws Exception {
				final URIBuilder builder = new URIBuilder(uri)
					.addParameter(API_QUERY_PARAM, query)
					.addParameter(API_COUNT_PARAM, 
						(count <= 0 || count > API_SEARCH_TWEETS_MAX_COUNT) ?
							Integer.toString(API_TWEETS_DEFAULT_COUNT) :
							Integer.toString(count));
				if(sinceId > 0L) {
					builder.addParameter(API_SINCEID_PARAM,
						Long.toString(sinceId));
				}
				return builder.build();
			}
		}.get(TWEET_SEARCH_URL);
	}	
	
	public HttpResponseEither<HttpFailure,TweetSearchResults> searchTweets(
		final String query) {
		return searchTweets(query, API_TWEETS_DEFAULT_COUNT, 0L, null);
	}
	
	public HttpResponseEither<HttpFailure,byte[]> userGetProfileImage(
		final String url) {
		checkNotNull(url, "Avatar URL cannot be null!");
		return new ByteArrayOrHttpFailureClosure(httpClient_).get(url);
	}
	
	public HttpResponseEither<HttpFailure,List<User>> userSearch(
		final String query) {
		return userSearch(query, API_USER_SEARCH_PERPAGE_DEFAULT,
			// Default OAuthConsumer
			null);
	}
	
	public HttpResponseEither<HttpFailure,List<User>> userSearch(
		final String query, final OAuthConsumer consumer) {
		return userSearch(query, API_USER_SEARCH_PERPAGE_DEFAULT, consumer);
	}
	
	public HttpResponseEither<HttpFailure,List<User>> userSearch(
		final String query, final int perPage, final OAuthConsumer consumer) {
		checkNotNull(query, "Query cannot be null!");
		return new TwitterApiGsonClosure<List<User>>(httpClient_, gson_.create(),
			new TypeToken<List<User>>(){}.getType(), consumer, SC_OK) {
			@Override
			public URI getFinalURI(final URI uri) throws Exception {
				return new URIBuilder(uri)
					.addParameter(API_USER_SEARCH_QUERY_PARAM, query)
					.addParameter(API_USER_SEARCH_PERPAGE_PARAM, 
						(perPage <= 0 || perPage > API_USER_SEARCH_PERPAGE_MAX) ?
							Integer.toString(API_USER_SEARCH_PERPAGE_DEFAULT) :
							Integer.toString(perPage))
					.build();
			}
		}.get(USERS_SEARCH_URL);
	}
	
	public HttpResponseEither<HttpFailure,Tweet> statusUpdate(final String text) {
		return statusUpdate(text, null);
	}
		
	public HttpResponseEither<HttpFailure,Tweet> statusUpdate(final String text,
		final OAuthConsumer consumer) {
		checkNotNull(text, "Tweet text cannot be null!");
		return new TwitterApiGsonClosure<Tweet>(httpClient_, gson_.create(),
			Tweet.class, consumer, SC_OK) {
			@Override
			public void before(final HttpRequestBase request) throws Exception {
				final List<NameValuePair> params = new ArrayList<NameValuePair>();
				params.add(new BasicNameValuePair(API_STATUS_PARAM, text));
				// Build the request entity.
				try {
					((HttpPost)request).setEntity(new UrlEncodedFormEntity(
						params, UTF_8));
				} catch (UnsupportedEncodingException e) {
					throw new TwitterApiException("Failed to UTF-8 " +
						"encode POST body.", e);
				}
				// OAuth sign the request.
				super.before(request);
			}
		}.post(STATUSES_UPDATE_URL);
	}
	
	public OAuthConsumer xAuthRetrieveAccessTokenConsumer(
		final String username, final String password) {
		checkNotNull(username, "Username cannot be null!");
		checkNotNull(password, "Password cannot be null!");
		final OAuthConsumer consumer = new CommonsHttpOAuthConsumer(
			consumerKey_, consumerKeySecret_);
		final HttpParameters params = new HttpParameters();
		params.put(API_XAUTH_MODE_PARAM, API_XAUTH_MODE_CLIENT_AUTH, true);
		params.put(API_XAUTH_USERNAME_PARAM, username, true);
		params.put(API_XAUTH_PASSWORD_PARAM, password, true);
		consumer.setAdditionalParameters(params);
		// Grab an OAuth access token from the API.
		final HttpResponseEither<HttpFailure,String> response = 
			new TwitterApiStringOrHttpFailureClosure(httpClient_, consumer, SC_OK) {
			@Override
			public void before(final HttpRequestBase request) throws Exception {
				// xAuth authentication requires that we add the mode, username,
				// and password parameters to the POST body as well.
				final List<NameValuePair> params = new ArrayList<NameValuePair>();
				params.add(new BasicNameValuePair(API_XAUTH_MODE_PARAM,
					API_XAUTH_MODE_CLIENT_AUTH));
				params.add(new BasicNameValuePair(API_XAUTH_USERNAME_PARAM,
					username));
				params.add(new BasicNameValuePair(API_XAUTH_PASSWORD_PARAM,
					password));
				try {
					((HttpPost)request).setEntity(new UrlEncodedFormEntity(
						params, UTF_8));
				} catch (UnsupportedEncodingException e) {
					throw new TwitterApiException("Failed to UTF-8 " +
						"encode xAuthApiAuthenticate POST body.", e);
				}
				// OAuth sign the request.
				super.before(request);
			}
		}.post(OAUTH_ACCESS_TOKEN_URL);
		// If the initial request of a proper OAuth access token failed,
		// bail and reject the operation.
		if(!response.success()) {
			throw new TwitterApiException("Failed to retreive xAuth " +
				"access token!", response.left().getCause());
		}
		// OK, it worked.
		// Get the response body as a string, we'll need to parse it
		// out manually to retreive the params we want from the body.
		final HttpParameters decodedParams = decodeForm(response.right());
		return xAuthBuildOAuthConsumer(
			decodedParams.getFirst(API_OAUTH_TOKEN_PARAM),
			decodedParams.getFirst(API_OAUTH_TOKEN_SECRET_PARAM),
			decodedParams.getFirst(API_OAUTH_SCREEN_NAME_PARAM));
	}
	
	public OAuthConsumer xAuthBuildOAuthConsumer(final String token,
		final String secret, final String username) {
		final OAuthConsumer consumer = new TwitterApiCommonsHttpOAuthConsumer(
			consumerKey_, consumerKeySecret_,
			// Set the username if we have one.
			(username != null) ? username : null);
		consumer.setTokenWithSecret(token, secret);
		return consumer;
	}
	
	public OAuthConsumer xAuthRetrieveConsumer(final String token,
		final String secret) {
		return xAuthBuildOAuthConsumer(token, secret, null);
	}
	
	/**
	 * Given a callback URL, retreive an OAuth request token for
	 * OAuth authentication against the Twitter API.
	 * @param callbackUrl
	 * @return
	 */
	public HttpResponseEither<HttpFailure,String> oAuthRetrieveRequestToken(
		final String callbackUrl) {
		checkNotNull(callbackUrl, "Callback URL cannot be null!");
		final OAuthConsumer consumer = new CommonsHttpOAuthConsumer(
			consumerKey_, consumerKeySecret_);
		// The callback URL is encoded as one of the many parameters in
		// the OAuth Http Authorization header that's sent to Twitter
		// to kick off the OAuth dance.  Note that the Http POST body
		// MUST BE EMPTY.
		final HttpParameters params = new HttpParameters();
		params.put(API_OAUTH_CALLBACK_URL_PARAM, callbackUrl, true);
		consumer.setAdditionalParameters(params);
		return new TwitterApiStringOrHttpFailureClosure(httpClient_,
			consumer, SC_OK){}.post(OAUTH_REQUEST_TOKEN_URL);
	}
	
	/**
	 * Requests an OAuth request token builds a valid OAuth authorize
	 * URL then returns it.  The user can be re-directed to this URL
	 * to authorize/deny access.
	 * @param callbackUrl
	 * @return
	 */
	public String oAuthGetAuthorizeUrl(final String callbackUrl) {
		final HttpResponseEither<HttpFailure,String> response =
			oAuthRetrieveRequestToken(callbackUrl);
		if(!response.success()) {
			throw new TwitterApiException("Failed to build Twitter OAuth " +
				"authorize URL.", response.left().getCause());
		}
		// Get the response body as a string, we'll need to parse it
		// out manually to retreive the params we want from the body.
		final HttpParameters params = decodeForm(response.right());
		final String token = params.getFirst(API_OAUTH_TOKEN_PARAM);
		logger__.debug("Retreived OAuth token: " + token);
		return String.format("%s?%s=%s", OAUTH_AUTHENTICATE_URL,
			API_OAUTH_TOKEN_PARAM, token);
	}
	
	public OAuthConsumer oAuthBuildConsumer(final String consumerKey,
		final String consumerSecret, final String apiToken,
		final String apiTokenSecret, final String username) {
		final OAuthConsumer consumer = new TwitterApiCommonsHttpOAuthConsumer(
			consumerKey_, consumerKeySecret_, username);
		consumer.setTokenWithSecret(apiToken_, apiTokenSecret_);
		return consumer;
	}
	
	/**
	 * Given a token, a token secret, and a username returns a pre-loaded
	 * {@link TwitterApiCommonsHttpOAuthConsumer} for the caller.  Has the 
	 * default consumer key and consumer key secret pre-set.
	 * @param apiToken
	 * @param apiTokenSecret
	 * @param username
	 * @return
	 */
	public OAuthConsumer oAuthBuildConsumer(final String apiToken,
		final String apiTokenSecret, final String username) {
		// Generate a new OAuthConsumer with the fetched token and token
		// secret pre-set for the caller.
		return oAuthBuildConsumer(consumerKey_, consumerKeySecret_,
			apiToken, apiTokenSecret, username);
	}
	
	public OAuthConsumer oAuthBuildConsumer() {
		return oAuthBuildConsumer(consumerKey_, consumerKeySecret_,
			apiToken_, apiTokenSecret_, null);
	}
	
	/**
	 * Retreives an OAuth access token once a valid token and token verifier
	 * have been received.  Returns a new {@link TwitterApiCommonsHttpOAuthConsumer}
	 * pre-loaded with the resulting token and token secret as
	 * authenticated by the user.
	 * @param token
	 * @param verifier
	 * @return
	 */
	public OAuthConsumer oAuthGetAccessTokenConsumer(final String token,
		final String verifier) {
		final HttpParameters params = oAuthGetAccessTokenParams(token,
			verifier);
		final String username = params.getFirst(API_OAUTH_SCREEN_NAME_PARAM),
			newToken = params.getFirst(API_OAUTH_TOKEN_PARAM),
			newSecret = params.getFirst(API_OAUTH_TOKEN_SECRET_PARAM);
		// Generate a new OAuthConsumer with the fetched token and token
		// secret pre-set for the caller.
		final OAuthConsumer consumer = new TwitterApiCommonsHttpOAuthConsumer(
			consumerKey_, consumerKeySecret_, username);
		consumer.setTokenWithSecret(newToken, newSecret);
		return consumer;
	}
	
	private final HttpParameters oAuthGetAccessTokenParams(
		final String token, final String verifier) {
		final HttpResponseEither<HttpFailure,String> response =
			oAuthGetAccessToken(token, verifier);
		if(!response.success()) {
			throw new TwitterApiException("Failed to retrieve OAuth " +
				"access token parameters.", response.left().getCause());
		}
		return decodeForm(response.right());
	}
	
	private final HttpResponseEither<HttpFailure,String> oAuthGetAccessToken(
		final String token, final String verifier) {
		checkNotNull(token, "Token cannot be null!");
		checkNotNull(verifier, "Verifier cannot be null!");
		final OAuthConsumer consumer = new CommonsHttpOAuthConsumer(
			consumerKey_, consumerKeySecret_);
		final HttpParameters params = new HttpParameters();
		params.put(API_OAUTH_TOKEN_PARAM, token, true);
		params.put(API_OAUTH_VERIFIER_PARAM, verifier, true);
		consumer.setAdditionalParameters(params);
		return new TwitterApiStringOrHttpFailureClosure(httpClient_,
			consumer, SC_OK){}.post(OAUTH_ACCESS_TOKEN_URL);
	}
			
	public TwitterApiConnector setConsumerKey(String consumerKey) {
		consumerKey_ = consumerKey;
		return this;
	}
	
	public TwitterApiConnector setConsumerKeySecret(String consumerKeySecret) {
		consumerKeySecret_ = consumerKeySecret;
		return this;
	}
	
	public TwitterApiConnector setApiToken(String apiToken) {
		apiToken_ = apiToken;
		return this;
	}
	
	public TwitterApiConnector setApiTokenSecret(String apiTokenSecret) {
		apiTokenSecret_ = apiTokenSecret;
		return this;
	}
		
}
