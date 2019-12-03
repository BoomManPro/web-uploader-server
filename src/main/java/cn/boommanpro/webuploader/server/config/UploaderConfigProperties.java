package cn.boommanpro.webuploader.server.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author wangqimeng
 * @date 2019/11/1 18:13
 */
@Data
@ConfigurationProperties(prefix = "fileupload.upload")
public class UploaderConfigProperties {

    /**
     * 文件存放目录  (服务器目录)
     */
    private String dir;

    /**
     * chunkSize
     */
    private Integer chunkSize;

    /**
     * 清空所有数据
     */
    private boolean deleteAll = false;

}
