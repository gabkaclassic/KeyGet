package org.example.sender;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.UUID;

@Service
public class KeySender {
    private final List<String> subscribers;
    private final String secretKey;

    public KeySender(@Value("#{'${subscribers}'.split(',')}") List<String> subscribers,
                     @Value("${secret_key}") String secretKey) {

        this.subscribers = subscribers;
        this.secretKey = secretKey;
    }

    @Scheduled(cron = "@daily")
    public void sendKey() {

        var params = new LinkedMultiValueMap<String, String>();
        params.add("key", UUID.randomUUID().toString());
        params.add("secretKey", secretKey);
        var body = BodyInserters.fromFormData(params);

        subscribers.forEach(url -> WebClient.create(url)
                .post()
                .body(body)
                .retrieve()
        );
        params.clear();
    }

    public void sendKey(String secretKey) {

        if(this.secretKey.equals(secretKey))
            sendKey();
    }
}
