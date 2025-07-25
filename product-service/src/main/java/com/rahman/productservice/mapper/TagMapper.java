package com.rahman.productservice.mapper;

import com.rahman.productservice.dto.tag.TagResponse;
import com.rahman.productservice.entity.Tag;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TagMapper {
    TagResponse toResponse(Tag tag);
}
