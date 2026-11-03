package com.ncu.gym.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    /**
     * 动作库外置目录。默认取运行目录下的 exercises-dataset/，可用环境变量 GYM_EXERCISES_DIR 覆盖，
     * 例如 file:/data/exercises-dataset/。媒体不打进 jar，便于单独更新。
     */
    @Value("${gym.exercises.dir:file:./exercises-dataset/}")
    private String exercisesDir;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/login", "/api/health", "/api/public/**");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String base = exercisesDir.endsWith("/") ? exercisesDir : exercisesDir + "/";
        File dir = resolveDir(base);
        if (dir == null || !dir.isDirectory()) {
            log.warn("[exercises] 未找到动作库目录 {}，/exercises/ 将返回 404；请把 exercises-dataset 放到运行目录或设置 gym.exercises.dir", base);
            return;
        }

        // 图片与动图：长缓存（内容不变，文件名带哈希）
        registry.addResourceHandler("/exercises/images/**", "/exercises/videos/**")
                .addResourceLocations(base + "images/", base + "videos/")
                .setCacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic());

        // JSON 数据：中等缓存
        registry.addResourceHandler("/exercises/data/**")
                .addResourceLocations(base + "data/")
                .setCacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic());

        // 页面本身：短缓存，便于更新后立即生效
        registry.addResourceHandler("/exercises/**")
                .addResourceLocations(base)
                .setCacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES));

        log.info("[exercises] 动作库目录已挂载：{} -> /exercises/", dir.getAbsolutePath());
    }

    private File resolveDir(String location) {
        try {
            String path = location;
            if (path.startsWith("file:")) {
                path = path.substring("file:".length());
            }
            return new File(path).getAbsoluteFile();
        } catch (Exception e) {
            return null;
        }
    }

    /** 目录访问 /exercises/ 时转发到模块首页 */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/exercises/").setViewName("forward:/exercises/index.html");
        registry.addViewController("/exercises").setViewName("forward:/exercises/index.html");
    }
}
