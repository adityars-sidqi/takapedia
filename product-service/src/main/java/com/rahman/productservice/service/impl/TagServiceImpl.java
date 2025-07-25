package com.rahman.productservice.service.impl;

import com.rahman.commonlib.exception.ResourceNotFoundException;
import com.rahman.productservice.dto.tag.CreateTagRequest;
import com.rahman.productservice.dto.tag.TagResponse;
import com.rahman.productservice.entity.Tag;
import com.rahman.productservice.mapper.TagMapper;
import com.rahman.productservice.repository.TagRepository;
import com.rahman.productservice.service.TagService;
import com.rahman.productservice.service.ValidationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

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

    @Override
    @Transactional
    public TagResponse save(CreateTagRequest createTagRequest) {
        validationService.validate(createTagRequest);

        Tag tag = tagMapper.mapToEntity(createTagRequest);
        Tag tagSaved = tagRepository.save(tag);
        return tagMapper.toResponse(tagSaved);
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                messageSource.getMessage("tag.not_found", null, LocaleContextHolder.getLocale())
                        ));

        tagRepository.delete(tag);
    }
}
