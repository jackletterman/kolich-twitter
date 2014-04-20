import com.kolich.common.functional.either.Either;
import com.kolich.http.KolichDefaultHttpClient.KolichHttpClientFactory;
import com.kolich.http.common.response.HttpFailure;
import com.kolich.twitter.TwitterApiClient;
import com.kolich.twitter.entities.Tweet;
import com.kolich.twitter.entities.User;
import com.kolich.twitter.entities.UserList;
import org.apache.http.client.HttpClient;

import java.util.List;

public class Testing {

	public static void main(String[] args) {
		
		final HttpClient client = KolichHttpClientFactory.getNewInstanceNoProxySelector("@your username");
		final TwitterApiClient twitter = new TwitterApiClient(client,
			"your key here", "your secret here",
			"your api token here", "your api token secret here");
				
		final Either<HttpFailure,List<Tweet>> tweets =
			twitter.getTweets("markkolich");
		if(tweets.success()) {
			for(final Tweet t : tweets.right()) {
				System.out.println(t.getId() + " -> " + t.getText());
			}
		}
		
		final Either<HttpFailure,UserList> friends =
			twitter.getFriends("markkolich");
		if(friends.success()) {
			for(final User u : friends.right().getUsers()) {
				System.out.println(u.getScreenName());
			}
		}
		
		final Either<HttpFailure,Tweet> tweet =
			twitter.statusUpdate("test test test, move along.. nothing to see here");
		if(tweet.success()) {
			System.out.println(tweet.right().getId());
		} else {
			System.out.println("Posted tweet failed, your app probably " +
				"doesn't have write permissions.");
		}
		
	}

}
