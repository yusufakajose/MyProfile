package com.linkgrove.api.controller;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.Authentication;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.List;

@TestConfiguration
public class TestMvcAuthConfig implements WebMvcConfigurer {

	@Override
	public void addArgumentResolvers(@NonNull List<HandlerMethodArgumentResolver> resolvers) {
		resolvers.add(new HandlerMethodArgumentResolver() {
			@Override
			public boolean supportsParameter(@NonNull MethodParameter parameter) {
				return Authentication.class.isAssignableFrom(parameter.getParameterType());
			}

			@Override
			public Object resolveArgument(@NonNull MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
					@NonNull NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) {
				return new UsernamePasswordAuthenticationToken(
					"alice",
					"N/A",
					AuthorityUtils.createAuthorityList("ROLE_USER")
				);
			}
		});
	}
}


