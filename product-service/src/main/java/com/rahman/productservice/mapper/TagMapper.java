package com.rahman.productservice.mapper;

import com.rahman.productservice.dto.tag.CreateTagRequest;
import com.rahman.productservice.dto.tag.TagResponse;
import com.rahman.productservice.entity.Tag;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TagMapper {

    TagResponse toResponse(Tag tag);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "productTags", ignore = true)
    Tag mapToEntity(CreateTagRequest createTagRequest);
}
