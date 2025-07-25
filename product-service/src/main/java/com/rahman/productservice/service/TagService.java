package com.rahman.productservice.service;

import com.rahman.productservice.dto.tag.CreateTagRequest;
import com.rahman.productservice.dto.tag.TagResponse;

import java.util.List;
import java.util.UUID;

public interface TagService {
    List<TagResponse> findAll();
    TagResponse save(CreateTagRequest createTagRequest);
    void deleteById(UUID id);
}
