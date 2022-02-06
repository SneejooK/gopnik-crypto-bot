package com.tg.bot.telegramcryptobot.repositories;

import com.tg.bot.telegramcryptobot.entities.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Integer> {

    Alert getAlertById(int id);

    Alert getAlertByChatIdAndCurrencyAndPositive(long chatId, String currency, boolean positive);

    List<Alert> getAlertByChatIdOrderByCreatedDesc(long chatId);

    List<Alert> getAlertByChatIdAndCurrency(long chatId, String currency);

    @Query(value = "SELECT currency FROM Alert WHERE chatId = ?1 ORDER BY created ASC")
    Set<String> getCurrenciesByChatId(long chatId);

    void deleteAlertsByChatId(long chatId);

}
