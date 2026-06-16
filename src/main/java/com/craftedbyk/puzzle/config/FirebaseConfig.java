package com.craftedbyk.puzzle.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Activates {@link FirebaseSecurityProperties} for the App Check / Auth token filters. */
@Configuration
@EnableConfigurationProperties(FirebaseSecurityProperties.class)
public class FirebaseConfig {}
