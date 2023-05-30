package dev.mouradski.ftsopriceclient.client.crypto;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.Gson;
import dev.mouradski.ftsopriceclient.utils.Constants;
import dev.mouradski.ftsopriceclient.client.AbstractClientEndpoint;
import dev.mouradski.ftsopriceclient.model.Trade;
import dev.mouradski.ftsopriceclient.service.PriceService;
import jakarta.websocket.ClientEndpoint;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@ClientEndpoint
@Component
public class CryptoComClientEndpoint extends AbstractClientEndpoint {

    private int id = 1;

    protected CryptoComClientEndpoint(PriceService priceSender) {
        super(priceSender);
    }

    @Override
    protected String getUri() {
        return "wss://stream.crypto.com/v2/market";
    }

    @Override
    protected void subscribe() {
        Constants.SYMBOLS.stream().filter(v -> !v.equals("usdt")).map(String::toUpperCase).forEach(symbol -> {
            this.sendMessage("{\"id\": " + counter.getCount() + ",\"method\": \"subscribe\",\"params\": {\"channels\": [\"trade." + symbol   +  "_USDT\"]},\"nonce\": " + new Date().getTime() + "}");
            this.sendMessage("{\"id\": " + counter.getCount() + ",\"method\": \"subscribe\",\"params\": {\"channels\": [\"trade." + symbol   +  "_USD\"]},\"nonce\": " + new Date().getTime() + "}");
        });

    }

    @Override
    protected String getExchange() {
        return "crypto";
    }

    @Override
    protected List<Trade> mapTrade(String message) throws JsonProcessingException {

        if (!message.contains("result")) {
            return new ArrayList();
        }

        var gson = new Gson();
        var response = gson.fromJson(message, Response.class);

        var trades = new ArrayList<Trade>();

        var symbol = response.getResult().getSubscription().replace("trade.", "").split("_")[0];
        var quote = response.getResult().getSubscription().replace("trade.", "").split("_")[1];

        response.getResult().getData().forEach(cryptoTrade -> {
            trades.add(Trade.builder().exchange(getExchange()).symbol(symbol).quote(quote).price(cryptoTrade.getP()).amount(cryptoTrade.getQ()).build());
        });


        return trades;
    }

    @Override
    protected void pong(String message) {
        if (message.contains("public/heartbeat")) {
            this.sendMessage(message.replace("public/heartbeat", "public/respond-heartbeat"));
        }
    }
}