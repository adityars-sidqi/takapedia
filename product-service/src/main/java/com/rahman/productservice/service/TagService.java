package com.rahman.productservice.service;

import com.rahman.productservice.dto.tag.CreateTagRequest;
import com.rahman.productservice.dto.tag.TagResponse;
import com.rahman.productservice.dto.tag.UpdateTagRequest;

import java.util.List;
import java.util.UUID;

public interface TagService {
    List<TagResponse> findAll();
    TagResponse findById(UUID id);
    TagResponse save(CreateTagRequest createTagRequest);
    TagResponse update(UUID id, UpdateTagRequest updateTagRequest);
    void deleteById(UUID id);
}
