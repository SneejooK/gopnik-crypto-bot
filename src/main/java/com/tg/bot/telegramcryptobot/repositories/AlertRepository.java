package com.tg.bot.telegramcryptobot.repositories;

import com.tg.bot.telegramcryptobot.entities.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Integer> {

    List<Alert> getAlertByChatIdOrderByCreatedDesc(long chatId);

    Alert getAlertByChatIdAndCurrencyAndPositive(long chatId, String currency, boolean positive);

}
