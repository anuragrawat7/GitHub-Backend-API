package com.lattice.git;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.Banner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@SpringBootApplication
@EnableSwagger2
@Slf4j
public class GitToGoogleSheet {

    public static void main(String[] args) {
        new SpringApplicationBuilder(GitToGoogleSheet.class).bannerMode(Banner.Mode.OFF).build().run(args);

    }
}
