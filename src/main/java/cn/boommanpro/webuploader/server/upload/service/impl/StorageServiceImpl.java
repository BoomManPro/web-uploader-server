package cn.boommanpro.webuploader.server.upload.service.impl;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import cn.boommanpro.webuploader.server.common.ApplicationConstants;
import cn.boommanpro.webuploader.server.config.UploaderConfigProperties;
import cn.boommanpro.webuploader.server.form.MultipartFileParam;
import cn.boommanpro.webuploader.server.upload.service.StorageService;
import cn.boommanpro.webuploader.server.utils.FileMd5Util;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

/**
 * @author wangqimeng
 * @date 2019/9/20 17:01
 */
@Slf4j
@Service
public class StorageServiceImpl implements StorageService {

    private final UploaderConfigProperties uploaderConfigProperties;

    /**
     * 保存文件的根目录
     */
    private Path rootPath;

    private final StringRedisTemplate stringRedisTemplate;

    @Autowired
    public StorageServiceImpl(StringRedisTemplate stringRedisTemplate, UploaderConfigProperties uploaderConfigProperties) {
        this.rootPath = Paths.get(uploaderConfigProperties.getDir());
        this.stringRedisTemplate = stringRedisTemplate;
        this.uploaderConfigProperties = uploaderConfigProperties;
    }

    /**
     * 初始化方法
     * <p>
     * 如果没有文件夹 进项创建
     */
    @Override
    public void init() {
        try {
            Files.createDirectory(rootPath);
        } catch (FileAlreadyExistsException e) {
            log.debug("文件夹已经存在了，不用再创建。");
        } catch (IOException e) {
            log.error("初始化root文件夹失败。", e);
        }
    }

    @Override
    public void uploadFileByMappedByteBuffer(MultipartFileParam param) throws IOException {
        String fileName = param.getName();
        String uploadDirPath = uploaderConfigProperties.getDir() + param.getMd5();
        String tempFileName = fileName + "_tmp";
        File tmpDir = new File(uploadDirPath);
        File tmpFile = new File(uploadDirPath, tempFileName);
        if (!tmpDir.exists()) {
            tmpDir.mkdirs();
        }

        RandomAccessFile tempRaf = new RandomAccessFile(tmpFile, "rw");
        FileChannel fileChannel = tempRaf.getChannel();

        //写入该分片数据
        long offset = uploaderConfigProperties.getChunkSize() * param.getChunk();
        byte[] fileData = param.getFile().getBytes();
        MappedByteBuffer mappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_WRITE, offset, fileData.length);
        mappedByteBuffer.put(fileData);
        // 释放
        FileMd5Util.freedMappedByteBuffer(mappedByteBuffer);
        fileChannel.close();

        boolean isOk = checkAndSetUploadProgress(param, uploadDirPath);
        if (isOk) {
            boolean flag = renameFile(tmpFile, fileName);
            log.debug("upload complete {}!! name:{}", flag, fileName);
        }
    }

    /**
     * 删除所有逻辑
     */
    @Override
    public void deleteAll() {
        log.info("删除所有数据，start");
        FileSystemUtils.deleteRecursively(rootPath.toFile());
        stringRedisTemplate.delete(ApplicationConstants.FILE_UPLOAD_STATUS);
        stringRedisTemplate.delete(ApplicationConstants.FILE_MD5_KEY);
        log.info("删除所有数据，end");
    }

    /**
     * 检查并修改文件上传进度
     *
     * @param param
     * @param uploadDirPath
     * @return
     * @throws IOException
     */
    private boolean checkAndSetUploadProgress(MultipartFileParam param, String uploadDirPath) throws IOException {
        String fileName = param.getName();
        File confFile = new File(uploadDirPath, fileName + ".conf");
        RandomAccessFile accessConfFile = new RandomAccessFile(confFile, "rw");
        //把该分段标记为 true 表示完成
        log.debug("set part {} complete", param.getChunk());
        accessConfFile.setLength(param.getChunks());
        accessConfFile.seek(param.getChunk());
        accessConfFile.write(Byte.MAX_VALUE);

        //completeList 检查是否全部完成,如果数组里是否全部都是(全部分片都成功上传)
        byte[] completeList = FileUtils.readFileToByteArray(confFile);
        byte isComplete = Byte.MAX_VALUE;
        for (int i = 0; i < completeList.length && isComplete == Byte.MAX_VALUE; i++) {
            //与运算, 如果有部分没有完成则 isComplete 不是 Byte.MAX_VALUE
            isComplete = (byte) (isComplete & completeList[i]);
            log.debug("check part {} complete?: {}", i, completeList[i]);
        }

        accessConfFile.close();
        if (isComplete == Byte.MAX_VALUE) {
            stringRedisTemplate.opsForHash().put(ApplicationConstants.FILE_UPLOAD_STATUS, param.getMd5(), "true");
            stringRedisTemplate.opsForValue().set(ApplicationConstants.FILE_MD5_KEY + param.getMd5(), uploadDirPath + "/" + fileName);
            return true;
        } else {
            if (!stringRedisTemplate.opsForHash().hasKey(ApplicationConstants.FILE_UPLOAD_STATUS, param.getMd5())) {
                stringRedisTemplate.opsForHash().put(ApplicationConstants.FILE_UPLOAD_STATUS, param.getMd5(), "false");
            }
            if (!stringRedisTemplate.hasKey(ApplicationConstants.FILE_MD5_KEY + param.getMd5())) {
                stringRedisTemplate.opsForValue().set(ApplicationConstants.FILE_MD5_KEY + param.getMd5(), uploadDirPath + "/" + fileName + ".conf");
            }
            return false;
        }
    }

    /**
     * 文件重命名
     *
     * @param toBeRenamed   将要修改名字的文件
     * @param toFileNewName 新的名字
     * @return
     */
    public boolean renameFile(File toBeRenamed, String toFileNewName) {
        //检查要重命名的文件是否存在，是否是文件
        if (!toBeRenamed.exists() || toBeRenamed.isDirectory()) {
            log.info("File does not exist: {}", toBeRenamed.getName());
            return false;
        }
        String p = toBeRenamed.getParent();
        File newFile = new File(p + File.separatorChar + toFileNewName);
        //修改文件名
        return toBeRenamed.renameTo(newFile);
    }

}