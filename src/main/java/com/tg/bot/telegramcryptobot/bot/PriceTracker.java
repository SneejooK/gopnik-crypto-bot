package com.tg.bot.telegramcryptobot.bot;

import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import com.tg.bot.telegramcryptobot.entities.Alert;
import com.tg.bot.telegramcryptobot.exceptions.BotException;
import com.tg.bot.telegramcryptobot.services.AlertService;
import com.tg.bot.telegramcryptobot.util.Command;
import com.tg.bot.telegramcryptobot.util.Messenger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static com.tg.bot.telegramcryptobot.util.CallbackDataBuilder.buildCallback;

@Component
public class PriceTracker {

    private static final Logger LOGGER = LoggerFactory.getLogger(PriceTracker.class);

    @Value("${telegram.alert-threshold}")
    private int alertThreshold;

    @Value("${telegram.alert-interval-min}")
    private int alertInterval;

    private final AlertService alertService;
    private final Messenger messenger;
    private final MessageProcessor messageProcessor;
    private final TgBot tgBot;

    private final Executor executor = Executors.newFixedThreadPool(6);
    private LocalDateTime adminNotification = LocalDateTime.now();

    @Autowired
    public PriceTracker(AlertService alertService, Messenger messenger, MessageProcessor messageProcessor, TgBot tgBot) {
        this.alertService = alertService;
        this.messenger = messenger;
        this.messageProcessor = messageProcessor;
        this.tgBot = tgBot;
    }

    @Scheduled(fixedDelay = 5000)
    private void checkAlerts() {
        List<Alert> alerts = alertService.getAll();
        ConcurrentHashMap<String, Double> mapAlerts = new ConcurrentHashMap();

        if (alerts.size() > alertThreshold && LocalDateTime.now().isAfter(adminNotification)) {
            tgBot.sendMessage(Long.parseLong(System.getenv("CHAT_Id")),
                    messenger.codeMessage("tg.admin.message.alert", alerts.size()));
            adminNotification = LocalDateTime.now().plus(alertInterval, ChronoUnit.MINUTES);
        }

        for (Alert alert : alerts) {
            executor.execute(() -> doAlert(mapAlerts, alert));
        }

    }

    private void doAlert(Map<String, Double> mapAlerts, Alert alert) {

        try {

            double actualPrice = mapAlerts.computeIfAbsent(
                    alert.getCurrency(),
                    p -> messageProcessor.getActualPrice(alert.getCurrency())
            );

            if (checkTrigger(alert, actualPrice)) {

                SendResponse sendResponse = tgBot.sendMessage(alert.getChatId(), messenger.codeMessage(
                        "tg.message.alert",
                        alert.getLanguage(),
                        alert.getCurrency(),
                        alert.isPositive() ?
                                messenger.codeMessage("tg.message.alert.positive") :
                                messenger.codeMessage("tg.message.alert.negative"),
                        mapAlerts.get(alert.getCurrency()),
                        messenger.getRandomEmoji()
                ));

                if (sendResponse.errorCode() == 403) {
                    LOGGER.info("Notification was removed due to user rejection, chat_id - {}", alert.getChatId());
                    alertService.remove(alert);
                    return;
                }

                alert.setNextAlert(LocalDateTime.now().plus(alertInterval, ChronoUnit.MINUTES));
                alertService.save(alert);
            }

        } catch (Exception ex) {
            LOGGER.warn(BotException.ERROR_ALERT_MESSAGE, ex);
            tgBot.sendMessage(Long.parseLong(System.getProperty("CHAT_ID")),
                    BotException.ERROR_ALERT_MESSAGE + " : " + messenger.codeMessage(
                            "tg.message.error",
                            ex.getMessage(),
                            Arrays.toString(ex.getStackTrace())
                    ));
        }

    }

    private boolean checkTrigger(Alert alert, double actualPrice) {
        return (alert.isPositive() && actualPrice >= alert.getPrice() || !alert.isPositive() && actualPrice <= alert.getPrice())
                && (alert.getNextAlert() == null || LocalDateTime.now().isAfter(alert.getNextAlert()));
    }

}
