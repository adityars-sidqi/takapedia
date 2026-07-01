package com.takapedia.productsearch.service;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.AggregationRange;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.takapedia.productsearch.document.ProductDocument;
import com.takapedia.productsearch.dto.ProductDetailResponse;
import com.takapedia.productsearch.dto.SearchResponse;
import com.takapedia.productsearch.exception.ProductNotFoundException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;



@Service
public class ProductSearchService {

    private final ElasticsearchOperations operations;

    public ProductSearchService(ElasticsearchOperations operations) {
        this.operations = operations;
    }

    public SearchResponse search(String q, String brand, String category,
                                 Long minPrice, Long maxPrice, int page, int size, String sort) {
        // 1. Query: full-text match di name. Kalau q kosong, match_all.
        Query query = Query.of(b -> b.bool(bool -> {
            // must: full-text search
            if (q != null && !q.isBlank()) {
                bool.must(m -> m.match(mm -> mm.field("name").query(q)));
            } else {
                bool.must(m -> m.matchAll(ma -> ma));
            }

            // filter: brand
            if (brand != null && !brand.isBlank()) {
                bool.filter(f -> f.term(t -> t.field("brand.keyword").value(brand)));
            }

            // filter: category
            if (category != null && !category.isBlank()) {
                bool.filter(f -> f.term(t -> t.field("category.keyword").value(category)));
            }

            // filter: price range
            if (minPrice != null || maxPrice != null) {
                bool.filter(f -> f.range(r -> {
                    r.field("price");
                    if (minPrice != null) r.gte(co.elastic.clients.json.JsonData.of(minPrice));
                    if (maxPrice != null) r.lte(co.elastic.clients.json.JsonData.of(maxPrice));
                    return r;
                }));
            }

            return bool;
        }));

        PageRequest pageRequest;
        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",");
            String field = parts[0];  // "price"
            Sort.Direction direction = (parts.length > 1 && parts[1].equalsIgnoreCase("desc"))
                    ? Sort.Direction.DESC
                    : Sort.Direction.ASC;
            pageRequest = PageRequest.of(page, size, Sort.by(direction, field));
        } else {
            pageRequest = PageRequest.of(page, size);  // tanpa sort → relevance
        }

        // 2. Bangun NativeQuery dengan query + dua terms aggregation (brand, category).
        NativeQuery searchQuery = NativeQuery.builder()
                .withQuery(query)
                .withPageable(pageRequest)
                .withAggregation("brands", Aggregation.of(a -> a
                        .terms(t -> t.field("brand.keyword"))))
                .withAggregation("categories", Aggregation.of(a -> a
                        .terms(t -> t.field("category.keyword"))))
                .withAggregation("price_ranges", Aggregation.of(a -> a
                        .range(r -> r
                                .field("price")
                                .ranges(
                                        AggregationRange.of(rg -> rg.key("< 1jt").to("1000000")),
                                        AggregationRange.of(rg -> rg.key("1jt - 5jt").from("1000000").to("5000000")),
                                        AggregationRange.of(rg -> rg.key("5jt - 20jt").from("5000000").to("20000000")),
                                        AggregationRange.of(rg -> rg.key("> 20jt").from("20000000"))
                                ))))
                .build();

        // 3. Eksekusi.
        SearchHits<ProductDocument> hits =
                operations.search(searchQuery, ProductDocument.class);

        List<ProductDocument> products = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .toList();

        // Metadata pagination
        long totalHits = hits.getTotalHits();
        int totalPages = (int) Math.ceil((double) totalHits / size);

        Map<String, Long> brands = extractFacet(hits, "brands");
        Map<String, Long> categories = extractFacet(hits, "categories");
        Map<String, Long> priceRanges = extractRangeFacet(hits, "price_ranges");

        return new SearchResponse(products, totalHits, page, size, totalPages,
                brands, categories, priceRanges);
    }

    public ProductDetailResponse getById(String id) {
        ProductDocument doc = operations.get(id, ProductDocument.class);
        if (doc == null) {
            throw new ProductNotFoundException(id);
        }
        return ProductDetailResponse.from(doc);
    }

    // Parsing hasil terms aggregation jadi Map<bucketKey, count>.
    private Map<String, Long> extractFacet(SearchHits<ProductDocument> hits, String aggName) {
        Map<String, Long> result = new HashMap<>();
        var aggregations = (ElasticsearchAggregations) hits.getAggregations();
        if (aggregations == null) return result;

        var agg = aggregations.aggregationsAsMap().get(aggName);
        if (agg == null) return result;

        var terms = agg.aggregation().getAggregate().sterms();
        terms.buckets().array().forEach(bucket ->
                result.put(bucket.key().stringValue(), bucket.docCount()));

        return result;
    }

    private Map<String, Long> extractRangeFacet(SearchHits<ProductDocument> hits, String aggName) {
        Map<String, Long> result = new LinkedHashMap<>();  // LinkedHashMap: jaga urutan rentang
        var aggregations = (ElasticsearchAggregations) hits.getAggregations();
        if (aggregations == null) return result;

        var agg = aggregations.aggregationsAsMap().get(aggName);
        if (agg == null) return result;

        var range = agg.aggregation().getAggregate().range();
        range.buckets().array().forEach(bucket ->
                result.put(bucket.key(), bucket.docCount()));

        return result;
    }
}