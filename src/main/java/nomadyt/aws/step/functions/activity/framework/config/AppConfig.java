package nomadyt.aws.step.functions.activity.framework.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;

@Configuration
@Import({
        ActivityHeartbeatExecutorConfig.class
})
@ComponentScan(
        useDefaultFilters = false,
        basePackageClasses = {
                AppConfig.class
        },
        includeFilters = {
                @ComponentScan.Filter(
                        type = FilterType.ANNOTATION,
                        value = Component.class),
        }
)
public class AppConfig {
}
