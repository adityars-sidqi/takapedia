package com.rahman.productservice.service;

import com.rahman.productservice.dto.tag.TagResponse;

import java.util.List;

public interface TagService {
    List<TagResponse> findAll();
}
