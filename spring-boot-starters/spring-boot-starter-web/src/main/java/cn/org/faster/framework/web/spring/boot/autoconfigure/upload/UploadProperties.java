package cn.org.faster.framework.web.spring.boot.autoconfigure.upload;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


/**
 * @author zhangbowen
 */
@Data
@ConfigurationProperties(prefix = "app.upload")
@Component
public class UploadProperties {
    /**
     * 是否开启
     */
    private boolean enabled = true;
    /**
     * 模式（默认local）
     */
    private String mode = "local";
    /**
     * 本地模式配置
     */
    private LocalUploadProperties local = new LocalUploadProperties();


    @ConfigurationProperties(prefix = "app.upload.local")
    @Data
    @Component
    public static class LocalUploadProperties {
        /**
         * 文件的存储目录
         */
        private String fileDir;
        /**
         * 请求图片时的网址前缀
         */
        private String urlPrefix;
    }
}
