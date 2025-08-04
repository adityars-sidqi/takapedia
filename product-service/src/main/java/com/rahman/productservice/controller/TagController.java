package com.rahman.productservice.controller;

import com.rahman.commonlib.ApiResponse;
import com.rahman.productservice.dto.tag.CreateTagRequest;
import com.rahman.productservice.dto.tag.TagResponse;
import com.rahman.productservice.dto.tag.UpdateTagRequest;
import com.rahman.productservice.service.TagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/tag")
public class TagController {

    private final TagService tagService;

    @Autowired
    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<List<TagResponse>> findAll() {
        return ApiResponse.success(tagService.findAll());
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TagResponse> save(@RequestBody CreateTagRequest  createTagRequest) {
        return ApiResponse.success(tagService.save(createTagRequest));
    }

    @PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TagResponse> update(@PathVariable("id") UUID id, @RequestBody UpdateTagRequest updateTagRequest) {
        return ApiResponse.success(tagService.update(id, updateTagRequest));
    }

    @DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable("id") UUID id) {
        tagService.deleteById(id);
        return ApiResponse.success(null);
    }
}
