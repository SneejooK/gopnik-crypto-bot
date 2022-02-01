package com.tg.bot.telegramcriptobot.bot;

import com.tg.bot.telegramcriptobot.entities.Alert;
import com.tg.bot.telegramcriptobot.services.AlertService;
import com.tg.bot.telegramcriptobot.util.Messenger;
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

@Component
public class PriceTracker {

    @Value("${telegram.alert-threshold}")
    private int alertThreshold;

    @Value("${telegram.alert-interval-min}")
    private int alertInterval;

    private final AlertService alertService;
    private final Messenger messenger;
    private final BotProcessor processor;
    private final TgBot tgBot;

    private final Executor executor = Executors.newFixedThreadPool(6);

    @Autowired
    public PriceTracker(AlertService alertService, Messenger messenger, BotProcessor processor, TgBot tgBot) {
        this.alertService = alertService;
        this.messenger = messenger;
        this.processor = processor;
        this.tgBot = tgBot;
    }

    @Scheduled(fixedDelay = 5000)
    private void checkAlerts() {
        List<Alert> alerts = alertService.getAll();
        ConcurrentHashMap<String, Double> mapAlerts = new ConcurrentHashMap();

        if (alerts.size() > alertThreshold) {
            tgBot.sendMessage(Long.parseLong(System.getenv("CHAT_Id")),
                    "Количество уведомлений на данный момент - " + alerts.size());
        }

        for (Alert alert : alerts) {
            executor.execute(() -> doAlert(mapAlerts, alert));
        }

    }

    private void doAlert(ConcurrentHashMap<String, Double> mapAlerts, Alert alert) {

        try {

            mapAlerts.computeIfAbsent(alert.getCurrency(), p -> processor.getActualPrice(alert.getCurrency()));

            if (checkTrigger(alert, mapAlerts.get(alert.getCurrency()))) {

                tgBot.sendMessage(alert.getChatId(), messenger.codeMessage(
                        "tg.message.alert",
                        alert.getCurrency(),
                        mapAlerts.get(alert.getCurrency()),
                        messenger.getRandomEmoji()
                ));

                alert.setNextAlert(LocalDateTime.now().plus(alertInterval, ChronoUnit.MINUTES));
                alertService.save(alert);
            }

        } catch (Exception ex) {
            tgBot.sendMessage(Long.parseLong(System.getenv("CHAT_Id")), messenger.codeMessage(
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
