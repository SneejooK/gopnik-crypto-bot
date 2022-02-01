package com.tg.bot.telegramcriptobot;

import com.tg.bot.telegramcriptobot.bot.TgBot;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class TelegramCryptoBotApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext run = SpringApplication.run(TelegramCryptoBotApplication.class, args);
        run.getBean(TgBot.class).serve();
    }

}
