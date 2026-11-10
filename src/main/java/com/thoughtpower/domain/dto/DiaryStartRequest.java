package com.thoughtpower.domain.dto;


import com.thoughtpower.domain.entity.InputType;

import java.time.LocalDate;

public class DiaryStartRequest {
    LocalDate date;
    String emotion;
    InputType inputType;
}
