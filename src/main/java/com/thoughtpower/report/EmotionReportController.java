package com.thoughtpower.report;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
public class EmotionReportController {

    private final EmotionReportService emotionReportService;

    public EmotionReportController(EmotionReportService emotionReportService) {
        this.emotionReportService = emotionReportService;
    }

    @GetMapping("/emotions")
    public EmotionReportService.ReportResponse getReport(
            @RequestParam("startDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam("endDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate
    ) {
        return emotionReportService.getReport(startDate, endDate);
    }
}
