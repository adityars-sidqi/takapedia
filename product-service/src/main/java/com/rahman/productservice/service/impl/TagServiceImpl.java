package com.rahman.productservice.service.impl;

import com.rahman.productservice.dto.tag.TagResponse;
import com.rahman.productservice.entity.Tag;
import com.rahman.productservice.mapper.TagMapper;
import com.rahman.productservice.repository.TagRepository;
import com.rahman.productservice.service.TagService;
import com.rahman.productservice.service.ValidationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TagServiceImpl implements TagService {

    private final TagRepository tagRepository;
    private final TagMapper tagMapper;
    private final ValidationService validationService;
    private final MessageSource messageSource;

    @Autowired
    public TagServiceImpl(TagRepository tagRepository, TagMapper tagMapper, ValidationService validationService, MessageSource messageSource) {
        this.tagRepository = tagRepository;
        this.tagMapper = tagMapper;
        this.validationService = validationService;
        this.messageSource = messageSource;
    }

    @Override
    public List<TagResponse> findAll() {
        List<Tag> tags = tagRepository.findAll();
        return tags.stream().map(tagMapper::toResponse).toList();
    }
}
