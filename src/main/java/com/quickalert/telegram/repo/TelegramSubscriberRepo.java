package com.quickalert.telegram.repo;

import com.quickalert.telegram.model.TelegramSubscriber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TelegramSubscriberRepo extends JpaRepository<TelegramSubscriber, Integer> {

    TelegramSubscriber findByChatId(String chatId);
}
