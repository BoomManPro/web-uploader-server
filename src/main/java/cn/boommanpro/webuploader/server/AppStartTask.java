package cn.boommanpro.webuploader.server;

import cn.boommanpro.webuploader.server.config.UploaderConfigProperties;
import cn.boommanpro.webuploader.server.upload.service.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * @author wangqimeng
 * @date 2019/9/20 16:58
 */
@Component
public class AppStartTask implements CommandLineRunner {

    private final UploaderConfigProperties uploaderConfigProperties;

    private final StorageService storageService;

    public AppStartTask(StorageService storageService, UploaderConfigProperties uploaderConfigProperties) {
        this.storageService = storageService;
        this.uploaderConfigProperties = uploaderConfigProperties;
    }

    @Override
    public void run(String... args) throws Exception {
        storageService.init();
        if (uploaderConfigProperties.isDeleteAll()) {
            storageService.deleteAll();
        }
    }

}
