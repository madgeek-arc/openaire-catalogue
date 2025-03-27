/**
 * Copyright 2021-2025 OpenAIRE AMKE
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gr.madgik.catalogue.openaire.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Arrays;

@Configuration
@EnableCaching
public class CacheConfig {
    private static final Logger logger = LogManager.getLogger(CacheConfig.class);

    public static final String CACHE_PROVIDERS = "providers";
    public static final String CACHE_UI_VOCABULARIES = "ui_vocabularies";
    public static final String CACHE_VOCABULARIES = "vocabularies";
    public static final String CACHE_VOCABULARY_MAP = "vocabulary_map";
    public static final String CACHE_VOCABULARY_TREE = "vocabulary_tree";
    public static final String CACHE_FEATURED = "featuredServices";
    public static final String CACHE_EVENTS = "events";
    public static final String CACHE_SERVICE_EVENTS = "service_events";
    public static final String CACHE_VISITS = "visits";

    @Bean
    @Primary
    public CacheManager openaireCacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();

        cacheManager.setCaches(Arrays.asList(
//
//                new ConcurrentMapCache(CACHE_VISITS,
//                        CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).maximumSize(2000).build().asMap(), false),
//                new ConcurrentMapCache(CACHE_FEATURED,
//                        CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.DAYS).maximumSize(50).build().asMap(), false),
//                new ConcurrentMapCache(CACHE_PROVIDERS),
                new ConcurrentMapCache(CACHE_UI_VOCABULARIES)
//                new ConcurrentMapCache(CACHE_EVENTS),
//                new ConcurrentMapCache(CACHE_SERVICE_EVENTS),
//                new ConcurrentMapCache(CACHE_VOCABULARIES),
//                new ConcurrentMapCache(CACHE_VOCABULARY_MAP),
//                new ConcurrentMapCache(CACHE_VOCABULARY_TREE),
//
//                // NEEDED FOR registry-core
//                new ConcurrentMapCache("resourceTypes"),
//                new ConcurrentMapCache("resourceTypesIndexFields")
        ));
        return cacheManager;
    }
}
