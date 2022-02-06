package com.tg.bot.telegramcryptobot.services;

import com.tg.bot.telegramcryptobot.entities.Alert;
import com.tg.bot.telegramcryptobot.repositories.AlertRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class AlertService {

    private final AlertRepository alertRepository;

    @Autowired
    public AlertService(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    public Alert getAlertById(int id) {
        return alertRepository.getAlertById(id);
    }

    public List<Alert> getAll() {
        return alertRepository.findAll();
    }

    public List<Alert> getAllByChatId(long chatId) {
        return alertRepository.getAlertByChatIdOrderByCreatedDesc(chatId);
    }

    public List<Alert> getAllByChatIdAndCurrency(long chatId, String currency) {
        return alertRepository.getAlertByChatIdAndCurrency(chatId, currency);
    }

    public Alert getAlertByChatIdAndCurrencyAndPositive(long chatId, String currency, boolean positive) {
        return alertRepository.getAlertByChatIdAndCurrencyAndPositive(chatId, currency, positive);
    }

    public Set<String> getAllCurrenciesByChatId(long chatId) {
        return alertRepository.getCurrenciesByChatId(chatId);
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
    public void remove(Alert alert) {
        alertRepository.delete(alert);
    }

    @Transactional
    public void removeAllByChatId(long chatId) {
        alertRepository.deleteAlertsByChatId(chatId);
    }

}
