package com.takapedia.productsearch.document;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Data
@Document(indexName = "takapedia.public.products", createIndex = false)
public class ProductDocument {

    @Id
    private String id;

    @Field(type = FieldType.Text)
    private String name;

    @Field(type = FieldType.Text)
    private String description;

    @Field(type = FieldType.Double)
    private Long price;

    @Field(type = FieldType.Text)
    private String category;

    @Field(type = FieldType.Text)
    private String brand;

    @Field(name = "created_at", type = FieldType.Text)
    private String createdAt;

}
