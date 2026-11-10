package com.thoughtpower.archive;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/diaries")
public class CalendarController {

    private final CalendarService calendarService;

    public CalendarController(CalendarService calendarService) {
        this.calendarService = calendarService;
    }

    @GetMapping("/calendar")
    public CalendarService.CalendarResponse getCalendar(
            @RequestParam("year") int year,
            @RequestParam("month") int month
    ) {
        return calendarService.getCalendar(year, month);
    }

}
