package com.linkgrove.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.linkgrove.api.dto.CreateLinkRequest;
import com.linkgrove.api.dto.LinkResponse;
import com.linkgrove.api.exception.GlobalExceptionHandler;
import com.linkgrove.api.service.LinkService;
import com.linkgrove.api.service.LinkVariantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LinkControllerStandaloneHappyPathTest {

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
 			.build();
 	}

 	@Test
 	void createLink_happyPath_returns201AndBody() throws Exception {
 		CreateLinkRequest req = new CreateLinkRequest();
 		req.setTitle("My Link");
 		req.setUrl("https://example.com");
 		req.setDescription("d");
 		req.setAlias("my-alias");

 		LinkResponse stub = LinkResponse.builder()
 			.id(42L)
 			.title("My Link")
 			.url("https://example.com")
 			.description("d")
 			.alias("my-alias")
 			.isActive(true)
 			.displayOrder(1)
 			.clickCount(0L)
 			.createdAt(LocalDateTime.now())
 			.updatedAt(LocalDateTime.now())
 			.tags(java.util.List.of("tag1", "tag2"))
 			.build();

 		Mockito.when(linkService.createLink(Mockito.eq("alice"), Mockito.any(CreateLinkRequest.class)))
 			.thenReturn(stub);

 		Authentication auth = new UsernamePasswordAuthenticationToken("alice", "N/A", AuthorityUtils.createAuthorityList("ROLE_USER"));

 		mockMvc.perform(post("/api/links").principal(auth)
 			.contentType(MediaType.APPLICATION_JSON)
 			.content(objectMapper.writeValueAsString(req)))
 			.andExpect(status().isCreated())
 			.andExpect(jsonPath("$.id", is(42)))
 			.andExpect(jsonPath("$.title", is("My Link")))
 			.andExpect(jsonPath("$.url", is("https://example.com")))
 			.andExpect(jsonPath("$.alias", is("my-alias")))
 			.andExpect(jsonPath("$.isActive", is(true)));
 	}

 	@Test
 	void getUserLinks_happyPath_returns200() throws Exception {
 		org.springframework.data.domain.Page<com.linkgrove.api.dto.LinkResponse> emptyPage =
 			new org.springframework.data.domain.PageImpl<>(java.util.List.of(), org.springframework.data.domain.PageRequest.of(0, 12), 0);
 		Mockito.when(linkService.getUserLinksPage(Mockito.eq("alice"), Mockito.any(), Mockito.any(), Mockito.any()))
 			.thenReturn(emptyPage);
 		Authentication auth = new UsernamePasswordAuthenticationToken("alice", "N/A", AuthorityUtils.createAuthorityList("ROLE_USER"));
 		mockMvc.perform(get("/api/links").principal(auth))
 			.andExpect(status().isOk())
 			.andExpect(jsonPath("$.content.length()", is(0)));
 	}

 	@Test
 	void getLinkById_happyPath_returns200() throws Exception {
 		LinkResponse link = LinkResponse.builder().id(7L).title("T").url("https://e.com").build();
 		Mockito.when(linkService.getLinkById(Mockito.eq("alice"), Mockito.eq(7L))).thenReturn(link);
 		Authentication auth = new UsernamePasswordAuthenticationToken("alice", "N/A", AuthorityUtils.createAuthorityList("ROLE_USER"));
 		mockMvc.perform(get("/api/links/7").principal(auth))
 			.andExpect(status().isOk())
 			.andExpect(jsonPath("$.id", is(7)))
 			.andExpect(jsonPath("$.title", is("T")));
 	}

 	@Test
 	void updateLink_happyPath_returns200() throws Exception {
 		com.linkgrove.api.dto.UpdateLinkRequest req = new com.linkgrove.api.dto.UpdateLinkRequest();
 		req.setTitle("New"); req.setUrl("https://e.com"); req.setDescription(""); req.setIsActive(true);
 		LinkResponse resp = LinkResponse.builder().id(5L).title("New").url("https://e.com").isActive(true).build();
 		Mockito.when(linkService.updateLink(Mockito.eq("alice"), Mockito.eq(5L), Mockito.any())).thenReturn(resp);
 		Authentication auth = new UsernamePasswordAuthenticationToken("alice", "N/A", AuthorityUtils.createAuthorityList("ROLE_USER"));
 		mockMvc.perform(put("/api/links/5").principal(auth)
 			.contentType(MediaType.APPLICATION_JSON)
 			.content(objectMapper.writeValueAsString(req)))
 			.andExpect(status().isOk())
 			.andExpect(jsonPath("$.title", is("New")))
 			.andExpect(jsonPath("$.isActive", is(true)));
 	}

 	@Test
 	void deleteLink_happyPath_returns204() throws Exception {
 		Authentication auth = new UsernamePasswordAuthenticationToken("alice", "N/A", AuthorityUtils.createAuthorityList("ROLE_USER"));
 		mockMvc.perform(delete("/api/links/9").principal(auth))
 			.andExpect(status().isNoContent());
 	}

 	@Test
 	void reorderLinks_happyPath_returns200() throws Exception {
 		Authentication auth = new UsernamePasswordAuthenticationToken("alice", "N/A", AuthorityUtils.createAuthorityList("ROLE_USER"));
 		java.util.List<Long> order = java.util.List.of(3L, 1L, 2L);
 		mockMvc.perform(put("/api/links/reorder").principal(auth)
 			.contentType(MediaType.APPLICATION_JSON)
 			.content(objectMapper.writeValueAsString(order)))
 			.andExpect(status().isOk());
 	}

 	@Test
 	void variantsCrud_happyPaths() throws Exception {
 		Authentication auth = new UsernamePasswordAuthenticationToken("alice", "N/A", AuthorityUtils.createAuthorityList("ROLE_USER"));
 		com.linkgrove.api.dto.LinkVariantResponse variant = com.linkgrove.api.dto.LinkVariantResponse.builder()
 			.id(1L).title("V").url("https://e.com").weight(1).isActive(true).clickCount(0L).build();
 		Mockito.when(linkVariantService.addVariant(Mockito.eq("alice"), Mockito.eq(10L), Mockito.any())).thenReturn(variant);
 		Mockito.when(linkVariantService.updateVariant(Mockito.eq("alice"), Mockito.eq(10L), Mockito.eq(1L), Mockito.any())).thenReturn(variant);
 		Mockito.when(linkVariantService.listVariants(Mockito.eq("alice"), Mockito.eq(10L))).thenReturn(java.util.List.of(variant));

 		com.linkgrove.api.dto.LinkVariantRequest vreq = new com.linkgrove.api.dto.LinkVariantRequest();
 		vreq.setTitle("V"); vreq.setUrl("https://e.com"); vreq.setDescription(""); vreq.setWeight(1); vreq.setIsActive(true);
 		mockMvc.perform(post("/api/links/10/variants").principal(auth)
 			.contentType(MediaType.APPLICATION_JSON)
 			.content(objectMapper.writeValueAsString(vreq)))
 			.andExpect(status().isCreated())
 			.andExpect(jsonPath("$.id", is(1)));

 		mockMvc.perform(get("/api/links/10/variants").principal(auth))
 			.andExpect(status().isOk())
 			.andExpect(jsonPath("$[0].id", is(1)));

 		mockMvc.perform(put("/api/links/10/variants/1").principal(auth)
 			.contentType(MediaType.APPLICATION_JSON)
 			.content(objectMapper.writeValueAsString(vreq)))
 			.andExpect(status().isOk())
 			.andExpect(jsonPath("$.id", is(1)));

 		mockMvc.perform(delete("/api/links/10/variants/1").principal(auth))
 			.andExpect(status().isNoContent());
 	}
}


