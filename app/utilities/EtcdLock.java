package utilities;

import akka.http.javadsl.model.MediaTypes;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.typesafe.config.Config;
import play.Logger;
import play.libs.Json;
import play.libs.ws.WSClient;
import play.libs.ws.WSResponse;

import java.time.Duration;
import java.util.concurrent.CompletionStage;

import static java.time.temporal.ChronoUnit.SECONDS;
import static play.mvc.Results.internalServerError;

/**
 * ${CLASS_NAME}.
 * Created at 4/6/2018 11:53 AM by
 * @author Timur Isachenko
 */
@Singleton
public class EtcdLock {

    private static WSClient wsClient;
    private static Config config;
    private static String urlToLock;
    private static String urlToUnlock;

    @Inject
    public EtcdLock(WSClient wsClient, Config config) {
        this.wsClient = wsClient;
        this.config = config;
        this.urlToLock = config.getString(Constants.ETCD_URL_LOCK);
        this.urlToUnlock = config.getString(Constants.ETCD_URL_UNLOCK);
    }

    public CompletionStage<String> aquireLock(byte[] lockName) {
        return wsClient.url(urlToLock)
                .setContentType(MediaTypes.APPLICATION_JSON.mainType())
                .setRequestTimeout(Duration.of(1L, SECONDS))
                .post(Json.newObject().put("name", lockName))
                .thenApply(response -> {
                    Logger.debug("response status = {}", response.getStatus());
                    Logger.debug("response body = {}", response.getBodyAsBytes().utf8String());
                    return response;
                })
                .thenApply(WSResponse::asJson)
                .thenApply(jsonNode -> jsonNode.get("key").asText());

    }

    public CompletionStage<String> releaseLock(String key) {
        return wsClient.url(urlToUnlock)
                .setContentType(MediaTypes.APPLICATION_JSON.mainType())
                .post(Json.newObject().put("key", key))
                .thenApply(response -> {
                    Logger.debug("response status = {}", response.getStatus());
                    Logger.debug("response body = {}", response.getBodyAsBytes().utf8String());
                    return response;
                })
                .thenApply(WSResponse::asJson)
                .thenApply(jsonNode -> jsonNode.asText());

    }
}
