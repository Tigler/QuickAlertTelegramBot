package com.quickalert.telegram.bot;

import com.quickalert.telegram.model.Notify;
import com.quickalert.telegram.model.TelegramSubscriber;
import com.quickalert.telegram.repo.TelegramSubscriberRepo;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;
import java.util.stream.Collectors;

@Setter
@Slf4j
@Component
public class Bot extends TelegramLongPollingBot {

    @Value("${bot.name:}")
    private String botName;
    @Value("${bot.token:}")
    private String botToken;
    private final TelegramBotsApi telegramBotsApi;
    private final SendMessage response = new SendMessage();
    private final MessageSource messageSource;
    private final TelegramSubscriberRepo telegramSubscriberRepo;
    private final Map<String, String> cache = new HashMap<>();


    public Bot(TelegramBotsApi telegramBotsApi,
               MessageSource messageSource,
               TelegramSubscriberRepo telegramSubscriberRepo) {
        super();
        this.telegramBotsApi = telegramBotsApi;
        this.messageSource = messageSource;
        this.telegramSubscriberRepo = telegramSubscriberRepo;
    }

    @PostConstruct
    public void init() throws TelegramApiException {
        telegramBotsApi.registerBot(this);
    }

    /**
     * Этот метод вызывается при получении обновлений через метод GetUpdates.
     *
     * @param request Получено обновление
     */
    @SneakyThrows
    @Override
    public void onUpdateReceived(Update request) {
        Message requestMessage = request.getMessage();
        response.setChatId(requestMessage.getChatId().toString());

        switch (requestMessage.getText()) {
            case "/start": {
                startCommand(requestMessage);
            }
            case "/help": {
                helpCommand(requestMessage);
                break;
            }
            case "/addFilter": {
                cache.put(requestMessage.getChatId().toString(), "/addFilter");
                addFilterCommand(requestMessage);
                break;
            }
            case "/showFilters": {
                showFilterCommand(requestMessage);
                break;
            }
            case "/removeFilter": {
                cache.put(requestMessage.getChatId().toString(), "/removeFilter");
                removeFilterCommand(requestMessage);
                break;
            }
        }

        String command = cache.get(requestMessage.getChatId().toString());
        if (command != null && command.equals("/addFilter")
                && !requestMessage.getText().equals("/addFilter")) {
            if (addFilter(requestMessage.getChatId().toString(), requestMessage.getText())) {
                cache.remove(requestMessage.getChatId().toString());
                sendMessage(requestMessage.getChatId(),
                        requestMessage.getFrom().getUserName(),
                        messageSource.getMessage("command.added-filter",
                                null,
                                new Locale(requestMessage.getFrom().getLanguageCode())));
            }
        }

        if (command != null && command.equals("/removeFilter")
                && !requestMessage.getText().equals("/removeFilter")) {
            if (removeFilter(requestMessage.getChatId().toString(), requestMessage.getText())) {
                cache.remove(requestMessage.getChatId().toString());
                sendMessage(requestMessage.getChatId(),
                        requestMessage.getFrom().getUserName(),
                        messageSource.getMessage("command.removed-filter",
                                null,
                                new Locale(requestMessage.getFrom().getLanguageCode())));
            }
        }
    }

    @Transactional
    public void addSubscriber(String chatId, String userName, String languageCode) {
        TelegramSubscriber subscriber = telegramSubscriberRepo.findByChatId(chatId);

        if (subscriber == null) {
            subscriber = new TelegramSubscriber();
            subscriber.setChatId(chatId);
            subscriber.setUserName(userName);
            subscriber.setLanguageCode(languageCode);
            telegramSubscriberRepo.save(subscriber);
        }
    }

    public Collection<String> getFilters(String chatId) {
        TelegramSubscriber subscriber = telegramSubscriberRepo.findByChatId(chatId);
        if (subscriber.getFilter() != null) {
            return Arrays.asList(subscriber.getFilter().split(","));
        }
        return Collections.emptyList();
    }

    @Transactional
    public boolean addFilter(String chatId, String login) {
        TelegramSubscriber subscriber = telegramSubscriberRepo.findByChatId(chatId);
        if (subscriber != null) {
            if (subscriber.getFilter() != null) {
                subscriber.setFilter(subscriber.getFilter() + "," + login);
            } else {
                subscriber.setFilter(login);
            }
            telegramSubscriberRepo.save(subscriber);
            return true;
        }
        return false;
    }

    @Transactional
    public boolean removeFilter(String chatId, String login) {
        TelegramSubscriber subscriber = telegramSubscriberRepo.findByChatId(chatId);
        if (subscriber != null) {
            if (subscriber.getFilter() != null) {
                String result = Arrays.stream(subscriber.getFilter().split(","))
                        .filter(v -> !v.equals(login)).collect(Collectors.joining(","));
                subscriber.setFilter(result.isEmpty() ? null : result);
            } else {
                subscriber.setFilter(login);
            }
            telegramSubscriberRepo.save(subscriber);
            return true;
        }
        return false;
    }

    private void startCommand(Message message) {
        if (message.getFrom().getUserName() == null) {
            sendMessage(message.getChatId(),
                    message.getFrom().getUserName(),
                    messageSource.getMessage("command.start.need-username",
                            null,
                            new Locale(message.getFrom().getLanguageCode())));
        } else {
            addSubscriber(message.getChatId().toString(), message.getFrom().getUserName(), message.getFrom().getLanguageCode());
            sendMessage(message.getChatId(),
                    message.getFrom().getUserName(),
                    messageSource.getMessage("command.start.success",
                            null,
                            new Locale(message.getFrom().getLanguageCode())));
        }
    }

    private void helpCommand(Message message) {
        sendMessage(message.getChatId(),
                message.getFrom().getUserName(),
                messageSource.getMessage("command.help",
                        null,
                        new Locale(message.getFrom().getLanguageCode())));
    }

    private void addFilterCommand(Message message) {
        sendMessage(message.getChatId(),
                message.getFrom().getUserName(),
                messageSource.getMessage("command.add-filter",
                        null,
                        new Locale(message.getFrom().getLanguageCode())));
    }

    private void showFilterCommand(Message message) {
        Collection<String> filters = getFilters(message.getChatId().toString());
        if (!filters.isEmpty()) {
            sendMessage(message.getChatId(),
                    message.getFrom().getUserName(),
                    messageSource.getMessage("command.show-filter",
                            new Object[]{String.join(", ", filters)},
                            new Locale(message.getFrom().getLanguageCode())));
        } else {
            sendMessage(message.getChatId(),
                    message.getFrom().getUserName(),
                    messageSource.getMessage("command.empty-filter",
                            null,
                            new Locale(message.getFrom().getLanguageCode())));
        }
    }

    private void removeFilterCommand(Message message) {
        sendMessage(message.getChatId(),
                message.getFrom().getUserName(),
                messageSource.getMessage("command.remove-filter",
                        null,
                        new Locale(message.getFrom().getLanguageCode())));
    }

    public void notifyMessage(Notify notify) {
        if (notify.getGeo() != null) {
            sendMessage(notify.getChatId(),
                    notify.getUserName(),
                    messageSource.getMessage("command.notify.geo",
                            new Object[]{
                                    notify.getInitiatorName(),
                                    String.format(Locale.US, "%.4f", notify.getGeo().getLatitude()),
                                    String.format(Locale.US, "%.4f", notify.getGeo().getLongitude())
                            },
                            new Locale(notify.getLanguageCode())));
        } else {
            sendMessage(notify.getChatId(),
                    notify.getUserName(),
                    messageSource.getMessage("command.notify",
                            new Object[]{
                                    notify.getInitiatorName()
                            },
                            new Locale(notify.getLanguageCode())));
        }
    }


    private void sendMessage(Long chatId, String userName, String text) {
        SendMessage answer = new SendMessage();
        answer.setText(text);
        answer.setChatId(chatId.toString());
        try {
            execute(answer);
        } catch (TelegramApiException e) {
            log.error("Error sending message for user " + userName, e);
        }
    }


    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public String getBotUsername() {
        return botName;
    }
}
