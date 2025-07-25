package com.rahman.productservice.mapper;

import com.rahman.productservice.dto.tag.TagResponse;
import com.rahman.productservice.entity.Tag;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TagMapperTest {

    private final TagMapper tagMapper = Mappers.getMapper(TagMapper.class);

    @Test
    void testToResponse() {
        // given
        Tag tag = new Tag();
        tag.setId(UUID.randomUUID());
        tag.setName("Elektronik");

        // when
        TagResponse response = tagMapper.toResponse(tag);

        // then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(tag.getId());
        assertThat(response.name()).isEqualTo("Elektronik");
    }
}