package com.tg.bot.telegramcriptobot.services;

import com.tg.bot.telegramcriptobot.entities.Alert;
import com.tg.bot.telegramcriptobot.repositories.AlertRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AlertService {

    private final AlertRepository alertRepository;

    @Autowired
    public AlertService(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    public List<Alert> getAll() {
        return alertRepository.findAll();
    }

    public List<Alert> getAllByChatId(long chatId) {
        return alertRepository.getAlertByChatIdOrderByCreatedDesc(chatId);
    }

    public Alert getAlertByChatIdAndCurrencyAndPositive(long chatId, String currency, boolean positive) {
        return alertRepository.getAlertByChatIdAndCurrencyAndPositive(chatId, currency, positive);
    }

    @Transactional
    public void save(Alert alert) {
        alertRepository.save(alert);
    }

    @Transactional
    public void remove(int id) {
        alertRepository.deleteById(id);
    }

    @Transactional
    public void removeAll(List<Alert> alerts) {
        alertRepository.deleteAll(alerts);
    }

}
