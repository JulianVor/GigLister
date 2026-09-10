package com.giglister.web;

import com.giglister.dto.SearchResults;
import com.giglister.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    public SearchResults search(@RequestParam String q, @RequestParam(required = false) String type) {
        return searchService.search(q, type);
    }
}
