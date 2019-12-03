package cn.boommanpro.webuploader.server.upload.service;

import java.io.IOException;

import cn.boommanpro.webuploader.server.form.MultipartFileParam;

/**
 * 存储操作的service
 *
 * @author wangqimeng
 *
 * @date 2019/9/20 16:59
 */
public interface StorageService {


    /**
     * 初始化方法
     */
    void init();

    /**
     * 上传文件
     * 处理文件分块，基于MappedByteBuffer来实现文件的保存
     *
     * @param param 删除更换文件参数
     * @throws IOException io异常
     */
    void uploadFileByMappedByteBuffer(MultipartFileParam param) throws IOException;

    /**
     * 删除文件夹下的所有数据
     */
    void deleteAll();

}
