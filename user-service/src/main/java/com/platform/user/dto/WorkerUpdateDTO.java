package com.platform.user.dto;

import lombok.Data;

@Data
public class WorkerUpdateDTO {

    private String name;
    private String phone;
    private String workType;
    private Long dealerId;
}
