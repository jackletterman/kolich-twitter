import java.util.List;

import org.apache.http.client.HttpClient;

import com.kolich.http.HttpClient4Closure.HttpFailure;
import com.kolich.http.HttpClient4Closure.HttpResponseEither;
import com.kolich.http.KolichDefaultHttpClient.KolichHttpClientFactory;
import com.kolich.twitter.TwitterApiConnector;
import com.kolich.twitter.entities.Tweet;


public class Testing {

	public static void main(String[] args) {
		
		final HttpClient client = KolichHttpClientFactory.getNewInstanceNoProxySelector("@markkolich");
		final TwitterApiConnector twitter = new TwitterApiConnector(client);
		
		twitter
			.setConsumerKey("your key")
			.setConsumerKeySecret("your secret")
			.setApiToken("api token")
			.setApiTokenSecret("api token secret");
		
		final HttpResponseEither<HttpFailure,List<Tweet>> tweets = twitter.getTweets("markkolich");
		if(tweets.success()) {
			for(final Tweet t : tweets.right()) {
				System.out.println(t.getId() + " -> " + t.getText());
			}
		}
		
	}

}
