package com.thoughtpower.archive;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/diaries")
public class DiaryReadController {

    private final DiaryReadService diaryReadService;

    public DiaryReadController(DiaryReadService diaryReadService) {
        this.diaryReadService = diaryReadService;
    }

    @GetMapping
    public DiaryReadService.DiaryListResponse getDiaries(
            @RequestParam("date")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {
        return diaryReadService.getDiaries(date);
    }

    @GetMapping("/{diaryId}")
    public DiaryReadService.DiaryDetailResponse getDiary(
            @PathVariable("diaryId") long diaryId
    ) {
        return diaryReadService.getDiary(diaryId);
    }
}
