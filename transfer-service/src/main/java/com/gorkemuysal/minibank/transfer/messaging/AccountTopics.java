package com.gorkemuysal.minibank.transfer.messaging;

/**
 * Names of account-service's topics. transfer-service does NOT create them (no NewTopic beans):
 * account-service owns them. With auto topic creation disabled, a typo here fails loudly.
 */
public final class AccountTopics {

    public static final String COMMANDS = "account.commands";
    public static final String REPLIES = "account.replies";

    private AccountTopics() {
    }
}