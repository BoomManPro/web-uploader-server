package cn.boommanpro.webuploader.server.controller;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import cn.boommanpro.webuploader.server.common.ApplicationConstants;
import cn.boommanpro.webuploader.server.form.MultipartFileParam;
import cn.boommanpro.webuploader.server.upload.service.StorageService;
import cn.boommanpro.webuploader.server.vo.ResultStatus;
import cn.boommanpro.webuploader.server.vo.ResultVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileUtils;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author wangqimeng
 * @date 2019/9/20 16:59
 */
@Slf4j
@RestController
@RequestMapping("index")
public class IndexController {

    @Resource(name = "redisTemplate")
    private HashOperations<String, String, Boolean> hashOperations;

    @Resource(name = "redisTemplate")
    private ValueOperations<String, String> valueOperations;

    private final StorageService storageService;

    public IndexController(StorageService storageService) {
        this.storageService = storageService;
    }

    /**
     * 秒传判断，断点判断
     */
    @PostMapping(value = "checkFileMd5")
    public ResultVo checkFileMd5(String md5) throws IOException {

        Boolean processing = hashOperations.get(ApplicationConstants.FILE_UPLOAD_STATUS, md5);
        if (processing == null) {
            return new ResultVo<>(ResultStatus.NO_HAVE);
        }
        String value = valueOperations.get(ApplicationConstants.FILE_MD5_KEY + md5);
        if (Boolean.TRUE.equals(processing)) {
            return new ResultVo<>(ResultStatus.IS_HAVE, value);
        } else {
            if (value == null) {
                return new ResultVo<>(ResultStatus.NO_HAVE);
            }
            File confFile = new File(value);
            byte[] completeList = FileUtils.readFileToByteArray(confFile);
            List<String> missChunkList = new LinkedList<>();
            for (int i = 0; i < completeList.length; i++) {
                if (completeList[i] != Byte.MAX_VALUE) {
                    missChunkList.add(i + "");
                }
            }
            return new ResultVo<>(ResultStatus.ING_HAVE, missChunkList);
        }
    }

    /**
     * 上传文件
     */
    @PostMapping(value = "fileUpload")
    public ResponseEntity fileUpload(MultipartFileParam param, HttpServletRequest request) {
        //判断请求是否是Multipart
        boolean isMultipart = ServletFileUpload.isMultipartContent(request);
        if (isMultipart) {
            log.debug("param:{},message:{}", param, "上传文件start。");
            try {
                storageService.uploadFileByMappedByteBuffer(param);
            } catch (IOException e) {
                log.error("文件上传失败:" + param.toString(), e);
            }
            log.debug("param:{},message:{}", param, "上传文件end。");
        }
        return ResponseEntity.ok().body("上传成功。");
    }

}
