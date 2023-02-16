package com.quickalert.telegram.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class Geo implements Serializable {

    private Double latitude;
    private Double longitude;
}
