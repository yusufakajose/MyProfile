package com.linkgrove.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkgrove.api.dto.CreateLinkRequest;
import com.linkgrove.api.dto.UpdateLinkRequest;
import com.linkgrove.api.service.LinkService;
import com.linkgrove.api.service.LinkVariantService;
import com.linkgrove.api.config.JwtAuthenticationFilter;
import com.linkgrove.api.config.RateLimitingConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;

 

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@WebMvcTest(controllers = LinkController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({com.linkgrove.api.exception.GlobalExceptionHandler.class, TestMvcAuthConfig.class})
@Disabled("Replaced by standalone controller tests")
@SuppressWarnings({"removal"})
class LinkControllerValidationTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	ObjectMapper objectMapper;

	@MockBean
	LinkService linkService; // we only test validation layer and error mapping

	@MockBean
	LinkVariantService linkVariantService;

	@MockBean
	JwtAuthenticationFilter jwtAuthenticationFilter;

	@MockBean
	RateLimitingConfig rateLimitingConfig;

	@MockBean
	StringRedisTemplate stringRedisTemplate;

	@WithMockUser(username = "alice", roles = {"USER"})
	@Test
	void createLink_invalidAliasAndUrl_returns400WithFieldErrors() throws Exception {
		CreateLinkRequest req = new CreateLinkRequest();
		req.setTitle("Hello");
		req.setUrl("not-a-url");
		req.setAlias("x"); // too short

		mockMvc.perform(post("/api/links").with(user("alice").roles("USER")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(req)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error", containsString("Validation")))
			.andExpect(jsonPath("$.fieldErrors.url", containsString("URL")))
			.andExpect(jsonPath("$.fieldErrors.alias", containsString("Alias")));
	}

	@WithMockUser(username = "alice", roles = {"USER"})
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

		mockMvc.perform(put("/api/links/{id}", 1).with(user("alice").roles("USER")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(req)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.isActive", is(false)))
			.andExpect(jsonPath("$.title", is("T")));
	}
}


