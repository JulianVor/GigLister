package com.giglister.web;

import com.giglister.service.GenreTaxonomy;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** The full canonical base-genre list (see GenreTaxonomy), for the profile's genre picker -
 * unlike /api/events/genres this is never narrowed to "only genres with an upcoming event",
 * since picking a preference shouldn't depend on what's currently scheduled. */
@RestController
@RequestMapping("/api/genres")
public class GenreController {

    @GetMapping
    public List<String> list() {
        return GenreTaxonomy.BASE_GENRES;
    }
}
