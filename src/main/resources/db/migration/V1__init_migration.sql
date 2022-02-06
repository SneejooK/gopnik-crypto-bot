-- Mysql
CREATE TABLE alert
(
    id         int            NOT NULL AUTO_INCREMENT,
    chat_id    bigint         NOT NULL,
    user_id    bigint         NOT NULL DEFAULT '0',
    first_name varchar(45)             DEFAULT NULL,
    last_name  varchar(45)             DEFAULT NULL,
    user_name  varchar(45)             DEFAULT NULL,
    currency   varchar(45)    NOT NULL,
    positive   TINYINT        NOT NULL,
    price      decimal(20, 8) NOT NULL,
    language   varchar(5)              DEFAULT NULL,
    next_alert DATETIME                DEFAULT NULL,
    created    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY unique_chat_currency_positive (chat_id, currency, positive)
)
    ENGINE = InnoDB
    AUTO_INCREMENT = 23
    DEFAULT CHARSET = utf8