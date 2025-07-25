package com.rahman.productservice.service.impl;

import com.rahman.productservice.dto.tag.TagResponse;
import com.rahman.productservice.entity.Tag;
import com.rahman.productservice.mapper.TagMapper;
import com.rahman.productservice.repository.TagRepository;
import com.rahman.productservice.service.ValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;


class TagServiceImplTest {

    private TagRepository tagRepository;
    private TagMapper tagMapper;
    private ValidationService validationService;
    private MessageSource messageSource;
    private TagServiceImpl tagService;

    @BeforeEach
    void setUp() {
        tagRepository = mock(TagRepository.class);
        tagMapper = mock(TagMapper.class);
        validationService = mock(ValidationService.class);
        messageSource = mock(MessageSource.class);

        tagService = new TagServiceImpl(tagRepository, tagMapper, validationService, messageSource);
    }

    @Test
    void testFindAll_ReturnsListOfTagResponses() {
        // Arrange
        Tag tag = new Tag();
        tag.setId(UUID.randomUUID());
        tag.setName("Elektronik");

        TagResponse tagResponse = new TagResponse(tag.getId(), tag.getName());

        when(tagRepository.findAll()).thenReturn(List.of(tag));
        when(tagMapper.toResponse(tag)).thenReturn(tagResponse);

        // Act
        List<TagResponse> result = tagService.findAll();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().id()).isEqualTo(tag.getId());
        assertThat(result.getFirst().name()).isEqualTo("Elektronik");

        verify(tagRepository).findAll();
        verify(tagMapper).toResponse(tag);
        verifyNoMoreInteractions(tagRepository, tagMapper, validationService, messageSource);
    }
}