package com.thoughtpower.archive;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/diaries")
public class DiaryDeleteController {
    private final DiaryDeleteService diaryDeleteService;

    public DiaryDeleteController(DiaryDeleteService diaryDeleteService) {
        this.diaryDeleteService = diaryDeleteService;
    }

    @DeleteMapping("/{diaryId}")
    public ResponseEntity<Void> deleteDiary(
            @PathVariable("diaryId") long diaryId
    ){
        diaryDeleteService.deleteDiary(diaryId);
        return ResponseEntity.noContent().build();
    }
}
