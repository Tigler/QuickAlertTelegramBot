package com.quickalert.telegram.model;


import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;

@Data
@Entity
@Table
public class TelegramSubscriber implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "chat_id")
    private String chatId;
    @Column(name = "user_name")
    private String userName;
    @Column(name = "language_code")
    private String languageCode;
    private String filter;
}
