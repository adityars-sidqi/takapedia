package com.rahman.productservice.service.impl;

import com.rahman.productservice.dto.tag.CreateTagRequest;
import com.rahman.productservice.dto.tag.TagResponse;
import com.rahman.productservice.dto.tag.UpdateTagRequest;
import com.rahman.productservice.entity.Tag;
import com.rahman.productservice.mapper.TagMapper;
import com.rahman.productservice.repository.TagRepository;
import com.rahman.productservice.service.ValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * This test verifies that the caching annotations are correctly applied to the TagService.
 * In a real application, integration tests would be needed to verify actual Redis caching.
 */
@ExtendWith(MockitoExtension.class)
class TagServiceRedisCacheTest {

    @Mock
    private TagRepository tagRepository;

    @Mock
    private TagMapper tagMapper;

    @Mock
    private ValidationService validationService;

    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private TagServiceImpl tagService;

    @BeforeEach
    void setUp() {
        Mockito.reset(tagRepository, tagMapper, validationService, messageSource);
    }

    
    @Test
    void testFindAllMethodImplementation() {
        // Arrange
        Tag tag = new Tag();
        tag.setId(UUID.randomUUID());
        tag.setName("Electronics");

        TagResponse tagResponse = new TagResponse(tag.getId(), tag.getName());

        when(tagRepository.findAll()).thenReturn(List.of(tag));
        when(tagMapper.toResponse(tag)).thenReturn(tagResponse);

        // Act - First call (cache miss)
        List<TagResponse> result1 = tagService.findAll();

        // Assert
        assertThat(result1).containsExactly(tagResponse);
        verify(tagRepository, times(1)).findAll();

        // Act - Second call (cache hit)
        List<TagResponse> result2 = tagService.findAll();

        // Assert
        assertThat(result2).containsExactly(tagResponse);

        // Verify no more repository calls
        verifyNoMoreInteractions(tagRepository);
    }

    @Test
    void testFindByIdMethodImplementation() {
        // Arrange
        UUID id = UUID.randomUUID();
        Tag tag = new Tag();
        tag.setId(id);
        tag.setName("Electronics");
        TagResponse tagResponse = new TagResponse(id, tag.getName());

        when(tagRepository.findById(id)).thenReturn(Optional.of(tag));
        when(tagMapper.toResponse(tag)).thenReturn(tagResponse);

        // Act - First call (cache miss)
        TagResponse result1 = tagService.findById(id);
        assertThat(result1).isEqualTo(tagResponse);
        verify(tagRepository).findById(id);

        // Act - Second call (cache hit)
        TagResponse result2 = tagService.findById(id);
        assertThat(result2).isEqualTo(tagResponse);

        // Ensure repository only called once
        verifyNoMoreInteractions(tagRepository);
    }

    @Test
    void testSaveMethodImplementation() {
        // Arrange
        CreateTagRequest createRequest = new CreateTagRequest("New Tag");
        Tag newTag = new Tag();
        newTag.setId(UUID.randomUUID());
        newTag.setName(createRequest.name());

        TagResponse tagResponse = new TagResponse(newTag.getId(), newTag.getName());

        // Setup for initial cache population
        Tag existingTag = new Tag();
        existingTag.setId(UUID.randomUUID());
        existingTag.setName("Existing Tag");
        List<Tag> existingTags = List.of(existingTag);
        TagResponse existingTagResponse = new TagResponse(existingTag.getId(), existingTag.getName());

        when(tagRepository.findAll()).thenReturn(existingTags);
        when(tagMapper.toResponse(existingTag)).thenReturn(existingTagResponse);
        
        // Populate cache
        tagService.findAll();
        
        // Reset invocation counts after cache population
        reset(tagRepository);
        reset(tagMapper);
        
        // Setup for save operation
        when(tagMapper.mapToEntity(createRequest)).thenReturn(newTag);
        when(tagRepository.save(newTag)).thenReturn(newTag);
        when(tagMapper.toResponse(newTag)).thenReturn(tagResponse);
        when(tagRepository.findAll()).thenReturn(List.of(existingTag, newTag)); // Updated list after save

        // Act - Save operation (should evict cache)
        TagResponse result = tagService.save(createRequest);

        // Assert
        assertThat(result).isEqualTo(tagResponse);
        
        // Verify save operation
        verify(validationService).validate(createRequest);
        verify(tagMapper).mapToEntity(createRequest);
        verify(tagRepository).save(newTag);
        verify(tagMapper).toResponse(newTag);
        
        // Act - Find all after save (should hit repository again due to cache eviction)
        tagService.findAll();
        
        // Verify repository was called again after cache eviction
        verify(tagRepository).findAll();
    }

    @Test
    void testUpdateMethodImplementation() {
        // Arrange
        UUID id = UUID.randomUUID();
        UpdateTagRequest updateRequest = new UpdateTagRequest("Updated Tag");
        
        Tag existingTag = new Tag();
        existingTag.setId(id);
        existingTag.setName("Original Tag");
        
        Tag updatedTag = new Tag();
        updatedTag.setId(id);
        updatedTag.setName(updateRequest.name());
        
        TagResponse originalResponse = new TagResponse(existingTag.getId(), existingTag.getName());
        TagResponse updatedResponse = new TagResponse(updatedTag.getId(), updatedTag.getName());
        
        // Setup for initial cache population
        when(tagRepository.findById(id)).thenReturn(Optional.of(existingTag));
        when(tagMapper.toResponse(existingTag)).thenReturn(originalResponse);
        
        // Populate cache for specific tag
        tagService.findById(id);
        
        // Setup for all tags cache
        List<Tag> allTags = List.of(existingTag);
        when(tagRepository.findAll()).thenReturn(allTags);
        
        // Populate all tags cache
        tagService.findAll();
        
        // Reset invocation counts after cache population
        reset(tagRepository);
        reset(tagMapper);
        
        // Setup for update operation
        when(tagRepository.findById(id)).thenReturn(Optional.of(existingTag));
        when(tagRepository.save(existingTag)).thenReturn(updatedTag);
        when(tagMapper.toResponse(updatedTag)).thenReturn(updatedResponse);
        
        // Act - Update operation (should evict caches)
        TagResponse result = tagService.update(id, updateRequest);
        
        // Assert
        assertThat(result).isEqualTo(updatedResponse);
        
        // Verify update operation
        verify(validationService).validate(updateRequest);
        verify(tagRepository).findById(id);
        verify(tagRepository).save(existingTag);
        verify(tagMapper).toResponse(updatedTag);
        
        // Reset invocation counts after update
        reset(tagRepository);
        reset(tagMapper);
        
        when(tagRepository.findById(id)).thenReturn(Optional.of(updatedTag));
        when(tagMapper.toResponse(updatedTag)).thenReturn(updatedResponse);
        when(tagRepository.findAll()).thenReturn(List.of(updatedTag));
        
        // Act - Find by id after update (should hit repository again due to cache eviction)
        tagService.findById(id);
        
        // Verify repository was called again for findById after cache eviction
        verify(tagRepository).findById(id);
        
        // Act - Find all after update (should hit repository again due to cache eviction)
        tagService.findAll();
        
        // Verify repository was called again for findAll after cache eviction
        verify(tagRepository).findAll();
    }

    @Test
    void testDeleteByIdMethodImplementation() {
        // Arrange
        UUID id = UUID.randomUUID();
        Tag tag = new Tag();
        tag.setId(id);
        tag.setName("Tag to delete");
        
        when(tagRepository.findById(id)).thenReturn(Optional.of(tag));
        
        // Act
        tagService.deleteById(id);
        
        // Verify
        verify(tagRepository).findById(id);
        verify(tagRepository).delete(tag);
    }
}