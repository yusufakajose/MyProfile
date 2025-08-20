package com.linkgrove.api.service;

import com.linkgrove.api.dto.LinkVariantRequest;
import com.linkgrove.api.dto.LinkVariantResponse;
import com.linkgrove.api.exception.UnauthorizedException;
import com.linkgrove.api.model.Link;
import com.linkgrove.api.model.LinkVariant;
import com.linkgrove.api.repository.LinkRepository;
import com.linkgrove.api.repository.LinkVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LinkVariantService {

    private final LinkRepository linkRepository;
    private final LinkVariantRepository linkVariantRepository;

    @Transactional(readOnly = true)
    public List<LinkVariantResponse> listVariants(String username, Long linkId) {
        Link link = assertOwnership(username, linkId);
        io.micrometer.core.instrument.Metrics.counter("variants.listed").increment();
        return linkVariantRepository.findActiveByLink(link).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public LinkVariantResponse addVariant(String username, Long linkId, LinkVariantRequest request) {
        Link link = assertOwnership(username, linkId);
        LinkVariant v = LinkVariant.builder()
                .link(link)
                .title(request.getTitle())
                .url(request.getUrl())
                .description(request.getDescription())
                .weight(request.getWeight() == null ? 1 : Math.max(0, request.getWeight()))
                .isActive(request.getIsActive() == null ? true : request.getIsActive())
                .build();
        io.micrometer.core.instrument.Metrics.counter("variants.added").increment();
        return toResponse(linkVariantRepository.save(v));
    }

    @Transactional
    public LinkVariantResponse updateVariant(String username, Long linkId, Long variantId, LinkVariantRequest request) {
        Link link = assertOwnership(username, linkId);
        LinkVariant v = linkVariantRepository.findById(variantId).orElseThrow();
        if (!v.getLink().getId().equals(link.getId())) throw new UnauthorizedException("Variant does not belong to link");
        if (request.getTitle() != null) v.setTitle(request.getTitle());
        if (request.getUrl() != null) v.setUrl(request.getUrl());
        if (request.getDescription() != null) v.setDescription(request.getDescription());
        if (request.getWeight() != null) v.setWeight(Math.max(0, request.getWeight()));
        if (request.getIsActive() != null) v.setIsActive(request.getIsActive());
        io.micrometer.core.instrument.Metrics.counter("variants.updated").increment();
        return toResponse(linkVariantRepository.save(v));
    }

    @Transactional
    public void deleteVariant(String username, Long linkId, Long variantId) {
        Link link = assertOwnership(username, linkId);
        LinkVariant v = linkVariantRepository.findById(variantId).orElseThrow();
        if (!v.getLink().getId().equals(link.getId())) throw new UnauthorizedException("Variant does not belong to link");
        linkVariantRepository.delete(v);
        io.micrometer.core.instrument.Metrics.counter("variants.deleted").increment();
    }

    private Link assertOwnership(String username, Long linkId) {
        Link link = linkRepository.findById(linkId).orElseThrow();
        if (!link.getUser().getUsername().equals(username)) {
            throw new UnauthorizedException("Not your link");
        }
        return link;
    }

    private LinkVariantResponse toResponse(LinkVariant v) {
        return LinkVariantResponse.builder()
                .id(v.getId())
                .title(v.getTitle())
                .url(v.getUrl())
                .description(v.getDescription())
                .weight(v.getWeight())
                .isActive(v.getIsActive())
                .clickCount(v.getClickCount())
                .build();
    }
}


