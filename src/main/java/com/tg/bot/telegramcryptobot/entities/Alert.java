package com.tg.bot.telegramcryptobot.entities;

import lombok.*;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.time.LocalDateTime;
import java.util.Locale;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(of = {"id", "positive", "price", "chatId"})
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private long chatId;

    private long userId;

    private String firstName;

    private String lastName;

    private String userName;

    private String currency;

    private boolean positive;

    private double price;

    private Locale language;

    private LocalDateTime nextAlert;

    private LocalDateTime created;

}
