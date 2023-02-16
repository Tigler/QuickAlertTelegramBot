package com.quickalert.telegram.model;

import javax.validation.constraints.NotNull;

import lombok.Data;

@Data
public class Notify {

    @NotNull
    private String initiatorName;
    @NotNull
    private Long chatId;
    @NotNull
    private String userName;
    @NotNull
    private String languageCode;
    private Geo geo;
}
