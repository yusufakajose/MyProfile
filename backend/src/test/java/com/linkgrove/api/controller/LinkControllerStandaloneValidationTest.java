package com.linkgrove.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkgrove.api.dto.CreateLinkRequest;
import com.linkgrove.api.dto.UpdateLinkRequest;
import com.linkgrove.api.exception.GlobalExceptionHandler;
import com.linkgrove.api.service.LinkService;
import com.linkgrove.api.service.LinkVariantService;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class LinkControllerStandaloneValidationTest {

	MockMvc mockMvc;
	ObjectMapper objectMapper = new ObjectMapper();
	LinkService linkService;
	LinkVariantService linkVariantService;
	LinkController controller;

	@BeforeEach
	void setup() {
		linkService = Mockito.mock(LinkService.class);
		linkVariantService = Mockito.mock(LinkVariantService.class);
		controller = new LinkController(linkService, linkVariantService);
		objectMapper.registerModule(new JavaTimeModule());
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();
		mockMvc = MockMvcBuilders.standaloneSetup(controller)
				.setControllerAdvice(new GlobalExceptionHandler())
				.setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
					@Override
					public boolean supportsParameter(@NonNull MethodParameter parameter) {
						return Authentication.class.isAssignableFrom(parameter.getParameterType());
					}

					@Override
					public Object resolveArgument(@NonNull MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer, @NonNull NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) {
						return new UsernamePasswordAuthenticationToken("alice", "N/A", AuthorityUtils.createAuthorityList("ROLE_USER"));
					}
				})
				.setValidator(validator)
				.build();
	}

	@Test
	void createLink_invalidAliasAndUrl_returns400WithFieldErrors() throws Exception {
		CreateLinkRequest req = new CreateLinkRequest();
		req.setTitle("Hello");
		req.setUrl("not-a-url");
		req.setAlias("x");

		mockMvc.perform(post("/api/links")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(req)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error", containsString("Validation")))
			.andExpect(jsonPath("$.fieldErrors.url", containsString("URL")))
			.andExpect(jsonPath("$.fieldErrors.alias", containsString("Alias")));
	}

	@Test
	void createLink_reservedAlias_returns400() throws Exception {
		CreateLinkRequest req = new CreateLinkRequest();
		req.setTitle("Hello");
		req.setUrl("https://example.com");
		req.setAlias("admin");

		mockMvc.perform(post("/api/links")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(req)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.fieldErrors.alias", containsString("reserved")));
	}

	@Test
	void createLink_startAfterEnd_returns400() throws Exception {
		java.time.LocalDateTime now = java.time.LocalDateTime.now();
		CreateLinkRequest req = new CreateLinkRequest();
		req.setTitle("Hello");
		req.setUrl("https://example.com");
		req.setAlias("validalias");
		req.setStartAt(now.plusDays(2));
		req.setEndAt(now.plusDays(1));

		mockMvc.perform(post("/api/links")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(req)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error", containsString("Validation")))
			.andExpect(jsonPath("$.fieldErrors.global", containsString("startAt")));
	}

	@Test
	void updateLink_toggleActive_onlyIsActiveChanges_validationOk() throws Exception {
		UpdateLinkRequest req = new UpdateLinkRequest();
		req.setTitle("T");
		req.setUrl("https://example.com");
		req.setDescription("");
		req.setIsActive(Boolean.FALSE);

		Mockito.when(linkService.updateLink(Mockito.anyString(), Mockito.eq(1L), Mockito.any()))
				.thenAnswer(inv -> {
					com.linkgrove.api.dto.LinkResponse resp = com.linkgrove.api.dto.LinkResponse.builder()
						.id(1L).title("T").url("https://example.com").description("")
						.isActive(false).displayOrder(1).clickCount(0L)
						.tags(java.util.List.of()).build();
					return resp;
				});

		Authentication auth = new UsernamePasswordAuthenticationToken("alice", "N/A", AuthorityUtils.createAuthorityList("ROLE_USER"));
		var response = controller.updateLink(auth, 1L, req);
		org.junit.jupiter.api.Assertions.assertEquals(200, response.getStatusCode().value());
		var body = java.util.Objects.requireNonNull(response.getBody());
		org.junit.jupiter.api.Assertions.assertEquals(false, body.getIsActive());
		org.junit.jupiter.api.Assertions.assertEquals("T", body.getTitle());
	}

	@Test
	void addVariant_invalidWeightAndUrl_returns400() throws Exception {
		com.linkgrove.api.dto.LinkVariantRequest v = new com.linkgrove.api.dto.LinkVariantRequest();
		v.setTitle("V");
		v.setUrl("bad-url");
		v.setWeight(-1);
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/links/1/variants")
				.contentType(org.springframework.http.MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(v)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.fieldErrors.url", containsString("URL")));
	}

	@Test
	void generic500Schema_isStable() throws Exception {
		// Valid request so validation does not intercept
		CreateLinkRequest req = new CreateLinkRequest();
		req.setTitle("T");
		req.setUrl("https://e.com");
		req.setAlias("validalias");

		Mockito.when(linkService.createLink(Mockito.anyString(), Mockito.any()))
				.thenThrow(new RuntimeException("boom"));

		Authentication auth = new UsernamePasswordAuthenticationToken("alice", "N/A", AuthorityUtils.createAuthorityList("ROLE_USER"));
		mockMvc.perform(post("/api/links").principal(auth)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(req)))
			.andExpect(status().isInternalServerError())
			.andExpect(jsonPath("$.status", is(500)))
			.andExpect(jsonPath("$.error", containsString("Internal")))
			.andExpect(jsonPath("$.message", containsString("boom")))
			.andExpect(jsonPath("$.path", containsString("/api/links")));
	}
}


