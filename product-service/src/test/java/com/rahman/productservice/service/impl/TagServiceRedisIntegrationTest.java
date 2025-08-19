package com.rahman.productservice.service.impl;

import com.rahman.productservice.dto.tag.CreateTagRequest;
import com.rahman.productservice.dto.tag.TagResponse;
import com.rahman.productservice.dto.tag.UpdateTagRequest;
import com.rahman.productservice.entity.Tag;
import com.rahman.productservice.repository.TagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.interceptor.SimpleKey;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TagServiceRedisIntegrationTest extends BaseServiceIntegrationTest {

    @Autowired
    private TagServiceImpl tagService;

    @Autowired
    private TagRepository tagRepository;

    @BeforeEach
    void setup() {
        clearAllCaches();
        tagRepository.deleteAll();
    }

    @Test
    void testFindAll_ShouldCacheResult() {
        Tag tag1 = new Tag();
        tag1.setName("Redis Test Tag");
        tagRepository.save(tag1);

        Tag tag2 = new Tag();
        tag2.setName("Redis Test Tag2");
        Tag savedTag2 = tagRepository.save(tag2);

        // First call hits DB
        List<TagResponse> firstCall = tagService.findAll();
        assertThat(firstCall).hasSize(2);

        // Delete DB entity -> cache should still hold data
        tagRepository.deleteById(savedTag2.getId());

        List<TagResponse> secondCall = tagService.findAll();
        assertThat(secondCall).hasSize(2);

        // Verify cached
        assertCachePresent("tags", SimpleKey.EMPTY);
    }

    @Test
    void testSave_ShouldPersistEntity_AndEvictCache() {
        Tag existing = new Tag();
        existing.setName("Existing Tag");
        tagRepository.save(existing);

        tagService.findAll();
        assertCachePresent("tags", SimpleKey.EMPTY);

        // Save new
        var req = new CreateTagRequest("New Tag");
        TagResponse savedRes = tagService.save(req);

        var fromDb = tagRepository.findById(savedRes.id());
        assertThat(fromDb).isPresent();
        assertThat(fromDb.get().getName()).isEqualTo("New Tag");

        // Cache must be evicted
        assertCacheEvicted("tags", SimpleKey.EMPTY);
    }

    @Test
    void testUpdate_ShouldEvictCacheAndUpdateData() {
        Tag tag = new Tag();
        tag.setName("Old Name");
        Tag saved = tagRepository.save(tag);

        // Warm up caches
        tagService.findById(saved.getId());
        tagService.findAll();

        assertCachePresent("tag", saved.getId());
        assertCachePresent("tags", SimpleKey.EMPTY);

        // Update
        var updateRequest = new UpdateTagRequest("New Name");
        TagResponse updated = tagService.update(saved.getId(), updateRequest);

        assertThat(updated.name()).isEqualTo("New Name");

        // Cache evicted
        assertCacheEvicted("tag", saved.getId());
        assertCacheEvicted("tags", SimpleKey.EMPTY);

        // Rebuild cache
        TagResponse afterUpdateById = tagService.findById(saved.getId());
        assertThat(afterUpdateById.name()).isEqualTo("New Name");

        List<TagResponse> afterUpdateAll = tagService.findAll();
        assertThat(afterUpdateAll).hasSize(1);
        assertThat(afterUpdateAll.getFirst().name()).isEqualTo("New Name");
    }

    @Test
    void testDelete_ShouldRemoveEntity_AndEvictCache() {
        Tag tag = new Tag();
        tag.setName("Tag To Delete");
        Tag savedTag = tagRepository.save(tag);

        tagService.findAll();
        assertCachePresent("tags", SimpleKey.EMPTY);

        // Delete
        tagService.deleteById(savedTag.getId());

        var fromDb = tagRepository.findById(savedTag.getId());
        assertThat(fromDb).isNotPresent();

        assertCacheEvicted("tags", SimpleKey.EMPTY);
    }
}
