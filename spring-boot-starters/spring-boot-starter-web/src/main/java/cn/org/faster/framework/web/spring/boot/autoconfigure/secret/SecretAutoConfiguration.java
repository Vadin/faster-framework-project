package cn.org.faster.framework.web.spring.boot.autoconfigure.secret;

import cn.org.faster.framework.web.spring.boot.autoconfigure.secret.advice.SecretRequestAdvice;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * 目前仅引入了请求加密
 *
 * @author zhangbowen
 * @since 2018/12/13
 */
@Configuration
@Import({SecretRequestAdvice.class})
@ConditionalOnProperty(prefix = "app.secret", name = "enabled", havingValue = "true")
public class SecretAutoConfiguration {
}
