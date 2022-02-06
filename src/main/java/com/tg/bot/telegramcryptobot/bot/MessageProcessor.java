package com.tg.bot.telegramcryptobot.bot;

import com.pengrad.telegrambot.model.User;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.request.EditMessageText;
import com.pengrad.telegrambot.request.SendMessage;
import com.tg.bot.telegramcryptobot.entities.Alert;
import com.tg.bot.telegramcryptobot.exceptions.IncorrectPriceException;
import com.tg.bot.telegramcryptobot.exceptions.NotFoundCurrencyException;
import com.tg.bot.telegramcryptobot.exceptions.NotFoundDirectionException;
import com.tg.bot.telegramcryptobot.services.AlertService;
import com.tg.bot.telegramcryptobot.util.Command;
import com.tg.bot.telegramcryptobot.util.Messenger;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

import static com.tg.bot.telegramcryptobot.util.CallbackDataBuilder.buildCallback;

@Component
public class MessageProcessor {

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
    public MessageProcessor(AlertService alertService, Messenger messenger) {
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
                    values[1].toUpperCase(),
                    getActualPrice(values[1]));
        } else {
            StringBuilder builder = new StringBuilder();
            builder.append(messenger.codeMessage("tg.message.price.start"));
            return createPriceList(builder, Set.of(topCurrency));
        }

    }

    public String doPrice(long chatId) {
        Set<String> currencies = alertService.getAllCurrenciesByChatId(chatId);
        StringBuilder builder = new StringBuilder();
        builder.append(messenger.codeMessage("tg.message.price-list.start"));
        return createPriceList(builder, currencies);
    }

    public SendMessage doList(long chatId) {
        Set<String> currencies = alertService.getAllCurrenciesByChatId(chatId);
        return new SendMessage(chatId, textForList(currencies)).replyMarkup(buildListMarkup(currencies));
    }

    public EditMessageText doList(long chatId, int messageId) {
        Set<String> currencies = alertService.getAllCurrenciesByChatId(chatId);
        return new EditMessageText(chatId, messageId, textForList(currencies)).replyMarkup(buildListMarkup(currencies));
    }

    public String doAdd(String[] values, long chatId, User user) {

        boolean verifiedDirection = getDirection(values[1]);
        String verifiedCurrency = getCurrency(values[2]);
        double verifiedPrice = getPrice(values[3]);

        Alert oldAlert = alertService.getAlertByChatIdAndCurrencyAndPositive(chatId, verifiedCurrency, verifiedDirection);

        if (oldAlert != null) {
            double oldPrice = oldAlert.getPrice();
            updateAlert(oldAlert, verifiedPrice);
            return messenger.codeMessage(
                    "tg.message.alert.update",
                    verifiedCurrency,
                    verifiedDirection ?
                            messenger.codeMessage("tg.message.alert.more") :
                            messenger.codeMessage("tg.message.alert.less"),
                    oldPrice,
                    verifiedPrice);
        }

        addAlert(verifiedDirection, verifiedCurrency, verifiedPrice, chatId, user);
        return messenger.codeMessage(
                "tg.message.alert.create",
                verifiedCurrency,
                verifiedDirection ?
                        messenger.codeMessage("tg.message.alert.more") :
                        messenger.codeMessage("tg.message.alert.less"),
                verifiedPrice);

    }

    public SendMessage doRemove(long chatId) {
        List<Alert> alerts = alertService.getAllByChatId(chatId);

        if (alerts.isEmpty()) {
            return new SendMessage(chatId, messenger.codeMessage("tg.message.list.remove.empty"));
        }

        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup(
                new InlineKeyboardButton(messenger.codeMessage("tg.button.yes")).callbackData(
                        buildCallback(Command.REMOVE, Command.EMPTY)
                ),

                new InlineKeyboardButton(messenger.codeMessage("tg.button.no")).callbackData(
                        buildCallback(Command.BACK, Command.ClOSE)
                )
        );

        return new SendMessage(chatId, messenger.codeMessage("tg.message.list.remove.question")).replyMarkup(keyboardMarkup);

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
                .language(LocaleContextHolder.getLocale())
                .created(LocalDateTime.now())
                .build();

        alertService.save(newAlert);
    }

    private void updateAlert(Alert oldAlert, double price) {
        oldAlert.setLanguage(LocaleContextHolder.getLocale());
        oldAlert.setPrice(price);
        alertService.save(oldAlert);
    }

    private String createPriceList(StringBuilder builder, Set<String> currencies) {
        for (String currency : currencies) {
            builder.append(" : ")
                    .append(currency)
                    .append(" - ")
                    .append(getActualPrice(currency))
                    .append("$\n");
        }
        builder.append(messenger.codeMessage("tg.message.price.end"));
        return builder.toString();
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

    private String textForList(Set<String> currencies) {
        return currencies.isEmpty() ?
                messenger.codeMessage("tg.message.list.currency.empty") :
                messenger.codeMessage("tg.message.list.currency");
    }

    private InlineKeyboardMarkup buildListMarkup(Set<String> currencies) {

        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
        List<InlineKeyboardButton> buttons = new ArrayList<>();

        if (currencies.isEmpty()) {
            return new InlineKeyboardMarkup();
        }

        int count = 0;
        for (String currency : currencies) {

            buttons.add(new InlineKeyboardButton(currency).callbackData(
                    buildCallback(Command.GET, Command.ClOSE, currency)
            ));

            if (count == 1) {
                inlineKeyboard.addRow(buttons.toArray(InlineKeyboardButton[]::new));
                buttons.clear();
                count = 0;
                continue;
            }

            count++;
        }

        if (!buttons.isEmpty()) {
            inlineKeyboard.addRow(buttons.toArray(InlineKeyboardButton[]::new));
        }

        inlineKeyboard.addRow(new InlineKeyboardButton(messenger.codeMessage("tg.button.exit")).callbackData(
                buildCallback(Command.BACK, Command.ClOSE)
        ));

        return inlineKeyboard;
    }

}
