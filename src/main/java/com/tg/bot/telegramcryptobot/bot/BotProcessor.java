package com.tg.bot.telegramcryptobot.bot;

import com.pengrad.telegrambot.model.User;
import com.tg.bot.telegramcryptobot.entities.Alert;
import com.tg.bot.telegramcryptobot.exceptions.IncorrectPriceException;
import com.tg.bot.telegramcryptobot.exceptions.NotFoundAlertException;
import com.tg.bot.telegramcryptobot.exceptions.NotFoundCurrencyException;
import com.tg.bot.telegramcryptobot.exceptions.NotFoundDirectionException;
import com.tg.bot.telegramcryptobot.services.AlertService;
import com.tg.bot.telegramcryptobot.util.Messenger;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Component
public class BotProcessor {

    private final AlertService alertService;
    private final Messenger messenger;
    private final OkHttpClient httpClient;

    @Value("${telegram.url-binance}")
    private String urlBinance;

    @Value("${telegram.binance-mark}")
    private String mark;

    private final String[] topCurrency = {
            "BTC",
            "ETH",
            "BNB",
            "ADA",
            "SOL",
            "XRP",
            "LUNA",
            "DOGE",
            "DOT",};

    @Autowired
    public BotProcessor(AlertService alertService, Messenger messenger) {
        this.alertService = alertService;
        this.messenger = messenger;
        this.httpClient = new OkHttpClient().newBuilder().build();
    }

    public double getActualPrice(String currency) {
        try {

            Request request = new Request.Builder().url(urlBinance + currency.toUpperCase() + mark).get().build();
            Response response = httpClient.newCall(request).execute();
            return new JSONObject(Objects.requireNonNull(response.body()).string()).getDouble("price");

        } catch (Exception ex) {
            throw new NotFoundCurrencyException(messenger.codeMessage("tg.message.incomprehensible-currency"));
        }

    }

    public String doPrice(String[] values) {
        if (values.length > 1) {
            return messenger.codeMessage(
                    "tg.message.rate",
                    values[1],
                    getActualPrice(values[1]));
        } else {
            StringBuilder builder = new StringBuilder();
            for (String currency : topCurrency) {
                builder.append(currency).append(" - ").append(getActualPrice(currency)).append("$\n");
            }
            return builder.toString();
        }

    }

    public String doGet(long chatId) {
        StringBuilder builder = new StringBuilder();
        List<Alert> alerts = alertService.getAllByChatId(chatId);

        if (!alerts.isEmpty()) {
            builder.append("Ваши уведомления: \n\n");

            for (int i = 0; i < alerts.size(); i++) {

                Alert alert = alerts.get(i);
                builder.append("id - ").append(i + 1).append(": Валюта - ").append(alert.getCurrency());

                if (alert.isPositive()) {
                    builder.append(": Больше - ");
                } else {
                    builder.append(": Меньше - ");
                }

                builder.append(alert.getPrice()).append("$\n\n");
            }

        } else {
            builder.append("Ваш список уведомлений пуст.\n");
        }

        builder.append(messenger.codeMessage("tg.message.command-list"));
        return builder.toString();
    }

    public String doAdd(String[] values, long chatId, User user) {

        boolean verifiedDirection = getDirection(values[1]);
        String verifiedCurrency = getCurrency(values[2]);
        double verifiedPrice = getPrice(values[3]);

        Alert oldAlert = alertService.getAlertByChatIdAndCurrencyAndPositive(chatId, verifiedCurrency, verifiedDirection);

        if (oldAlert != null) {
            updateAlert(oldAlert, verifiedPrice);
            return messenger.codeMessage(
                    "tg.message.updated.alert",
                    verifiedCurrency,
                    verifiedDirection ? ">" : "<",
                    verifiedPrice);
        }

        addAlert(verifiedDirection, verifiedCurrency, verifiedPrice, chatId, user);
        return messenger.codeMessage(
                "tg.message.create.alert",
                verifiedCurrency,
                verifiedDirection ? ">" : "<",
                verifiedPrice);

    }

    public String doRemove(String[] values, long chatId) {
        if (values.length > 1) {
            deleteAlert(chatId, values[1]);
            return messenger.codeMessage("tg.message.remove.alert", values[1]);
        }

        deleteAllAlerts(chatId);
        return messenger.codeMessage("tg.message.remove.alerts");

    }

    private void addAlert(boolean direction,
                          String currency,
                          double price,
                          long chatId,
                          User user) {

        Alert newAlert = Alert.builder()
                .chatId(chatId)
                .userId(user.id())
                .userName(user.username())
                .firstName(user.firstName())
                .lastName(user.lastName())
                .positive(direction)
                .currency(currency)
                .price(price)
                .created(LocalDateTime.now())
                .build();

        alertService.save(newAlert);
    }

    private void updateAlert(Alert oldAlert, double price) {
        oldAlert.setPrice(price);
        alertService.save(oldAlert);
    }

    private void deleteAlert(long chatId, String alertId) {
        try {

            int id = Integer.parseInt(alertId) - 1;
            List<Alert> alerts = alertService.getAllByChatId(chatId);
            for (int i = 0; i < alerts.size(); i++) {
                if (id == i) {
                    alertService.remove(alerts.get(i).getId());
                    return;
                }
            }

        } catch (Exception e) {
            throw new NotFoundAlertException(messenger.codeMessage("tg.message.incomprehensible-numbers"));
        }

        throw new NotFoundAlertException(messenger.codeMessage("tg.message.remove.alert.not-found", alertId));
    }

    private void deleteAllAlerts(long chatId) {
        alertService.removeAll(alertService.getAllByChatId(chatId));
    }

    private double getPrice(String price) {
        double doublePrice;
        try {
            doublePrice = Double.parseDouble(price.replaceFirst(",", "."));
        } catch (Exception ex) {
            throw new IncorrectPriceException(messenger.codeMessage("tg.message.incomprehensible-price"));
        }

        return doublePrice;
    }

    private String getCurrency(String currency) {
        getActualPrice(currency);
        return currency.toUpperCase();
    }

    private boolean getDirection(String direction) {

        if (direction.equalsIgnoreCase("MORE")) {
            return true;
        }

        if (direction.equalsIgnoreCase("LESS")) {
            return false;
        }

        throw new NotFoundDirectionException(messenger.codeMessage("tg.message.direction"));
    }

}
