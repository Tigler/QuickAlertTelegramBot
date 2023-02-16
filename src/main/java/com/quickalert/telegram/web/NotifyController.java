package com.quickalert.telegram.web;

import com.quickalert.telegram.bot.Bot;
import com.quickalert.telegram.model.Notify;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class NotifyController {

    private final Bot bot;

    @PostMapping("/api/v1/notify")
    public void notify(@RequestBody Notify notify) {
        bot.notifyMessage(notify);
    }
}
