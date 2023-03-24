package org.example.sender;

import org.example.crypto.Cryptographer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Random;

@Service
public class KeySender {
    private final List<String> subscribers;
    private final byte[] secretKey;

    private Date lastSendingDate;

    private final Cryptographer cryptographer;

    private final Long EXPIRATION;

    private byte[] cache;

    private static final Random random = new Random();

    public KeySender(@Value("#{'${subscribers}'.split(',')}") List<String> subscribers,
                     @Value("${expiration}") String expiration,
                     @Value("${secret_key}") String secretKey,
                     @Autowired Cryptographer cryptographer) {

        this.subscribers = subscribers;
        this.secretKey = cryptographer.encrypt(secretKey.getBytes());
        this.cryptographer = cryptographer;
        this.EXPIRATION = Long.parseLong(expiration);
    }

    @Scheduled(cron = "@daily")
    private void sendKey() {

        var params = new LinkedMultiValueMap<String, String>();

        var now = new Date();
        if(lastSendingDate == null || now.after(new Date(lastSendingDate.getTime() + EXPIRATION))) {
            cache = cryptographer.encrypt(getRandomKey().getBytes());
            lastSendingDate = now;
        }

        params.add("key", new String(cryptographer.decrypt(cache)));
        params.add("secretKey", new String(cryptographer.decrypt(secretKey)));
        var body = BodyInserters.fromFormData(params);

        subscribers.forEach(url -> WebClient.create(url)
                .post()
                .body(body)
                .retrieve()
                .bodyToMono(String.class)
                .retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(3)))
                .subscribe()
        );
        params.clear();
    }

    public void sendKey(String secretKey) {

        if(cryptographer.matches(this.secretKey, secretKey))
            sendKey();
    }

    private static String getRandomKey() {

        return random.ints(48, 123)
                .limit(34)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }
}
