package com.rahman.productservice.service.impl;

import com.rahman.productservice.dto.tag.CreateTagRequest;
import com.rahman.productservice.dto.tag.TagResponse;
import com.rahman.productservice.dto.tag.UpdateTagRequest;
import com.rahman.productservice.entity.Tag;
import com.rahman.productservice.exception.ResourceNotFoundException;
import com.rahman.productservice.mapper.TagMapper;
import com.rahman.productservice.repository.TagRepository;
import com.rahman.productservice.service.TagService;
import com.rahman.productservice.service.ValidationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.rahman.productservice.constants.MessagesCodeConstant.TAG_NOT_FOUND;

@Service
@Slf4j
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
    @Cacheable(value = "tags", unless = "#result.isEmpty()" )
    public List<TagResponse> findAll() {
        log.info("Fetching all tags from database");
        List<Tag> tags = tagRepository.findAll();
        return tags.stream().map(tagMapper::toResponse).toList();
    }

    @Override
    @Transactional
    @CacheEvict(value = "tags", allEntries = true)
    public TagResponse save(CreateTagRequest createTagRequest) {
        log.info("Saving new tag");
        validationService.validate(createTagRequest);

        Tag tag = tagMapper.mapToEntity(createTagRequest);
        Tag tagSaved = tagRepository.save(tag);
        return tagMapper.toResponse(tagSaved);
    }

    @Override
    @Cacheable(value = "tag", key = "#id")
    public TagResponse findById(UUID id) {
        log.info("Fetching tag with ID: {} from database", id);
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageSource.getMessage(TAG_NOT_FOUND, null, LocaleContextHolder.getLocale())
                ));

        return tagMapper.toResponse(tag);
    }

    @Override
    @Caching(evict = {
        @CacheEvict(value = "tags", allEntries = true),
        @CacheEvict(value = "tag", key = "#id")
    })
    public TagResponse update(UUID id, UpdateTagRequest updateTagRequest) {
        log.info("Updating tag with ID: {}", id);
        validationService.validate(updateTagRequest);

        Tag tag = tagRepository.findById(id).orElseThrow(() ->
                new ResourceNotFoundException(
                        messageSource.getMessage(TAG_NOT_FOUND, null, LocaleContextHolder.getLocale())
                ));

        // Update name jika tidak null dan tidak kosong
        Optional.ofNullable(updateTagRequest.name())
                .filter(name -> !name.isBlank())
                .ifPresent(tag::setName);

        Tag updated  = tagRepository.save(tag);

        return tagMapper.toResponse(updated);
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "tags", allEntries = true),
        @CacheEvict(value = "tag", key = "#id")
    })
    public void deleteById(UUID id) {
        log.info("Deleting tag with ID: {}", id);
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                messageSource.getMessage(TAG_NOT_FOUND, null, LocaleContextHolder.getLocale())
                        ));

        tagRepository.delete(tag);
    }
}
