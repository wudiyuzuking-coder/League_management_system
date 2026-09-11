package com.example.leagueticket.service.impl;

import com.example.leagueticket.entity.ClubInfo;
import com.example.leagueticket.exception.BusinessException;
import com.example.leagueticket.mapper.ClubInfoMapper;
import com.example.leagueticket.service.ClubLogoService;
import com.example.leagueticket.vo.AvatarResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.*;
import javax.imageio.stream.ImageInputStream;
import java.io.*;
import java.nio.file.*;
import java.util.*;

@Service @Profile("dev") @RequiredArgsConstructor
public class ClubLogoServiceImpl implements ClubLogoService {
    private static final long MAX=2L*1024*1024;
    private static final Map<String,String> EXT=Map.of("JPEG",".jpg","PNG",".png");
    private static final String PREFIX="/uploads/club-logos/";
    private final ClubInfoMapper mapper;
    @Value("${app.upload-dir:./uploads}") private String uploadDir;

    @Override @Transactional public AvatarResponse upload(Long clubId,MultipartFile file){
        ClubInfo club=mapper.findById(clubId);if(club==null)throw new BusinessException(HttpStatus.NOT_FOUND,"俱乐部不存在");
        if(file==null||file.isEmpty())throw new BusinessException("队徽文件不能为空");
        if(file.getSize()>MAX)throw new BusinessException(HttpStatus.PAYLOAD_TOO_LARGE,"队徽文件不能超过2MB");
        if(!Set.of("image/jpeg","image/png").contains(file.getContentType()))throw new BusinessException("队徽仅支持JPEG或PNG格式");
        String original=Optional.ofNullable(file.getOriginalFilename()).orElse("").toLowerCase(Locale.ROOT);
        if(!(original.endsWith(".jpg")||original.endsWith(".jpeg")||original.endsWith(".png")))throw new BusinessException("队徽文件扩展名必须为jpg、jpeg或png");
        String ext=detect(file);if((ext.equals(".png")&&!original.endsWith(".png"))||(ext.equals(".jpg")&&!(original.endsWith(".jpg")||original.endsWith(".jpeg"))))throw new BusinessException("队徽文件扩展名与实际内容不一致");
        Path dir=Path.of(uploadDir).toAbsolutePath().normalize().resolve("club-logos").normalize();
        try{Files.createDirectories(dir);}catch(IOException e){throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR,"队徽存储目录不可写");}
        Path target=dir.resolve(UUID.randomUUID()+ext).normalize();if(!target.startsWith(dir))throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR,"队徽保存路径无效");
        try(InputStream in=file.getInputStream();OutputStream out=Files.newOutputStream(target,StandardOpenOption.CREATE_NEW)){in.transferTo(out);}catch(IOException e){delete(target);throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR,"队徽保存失败");}
        String url=PREFIX+target.getFileName();try{if(mapper.updateLogoUrl(clubId,url)!=1)throw new BusinessException(HttpStatus.NOT_FOUND,"俱乐部不存在");}catch(RuntimeException e){delete(target);throw e;}
        Path old=managed(club.getLogoUrl(),dir);TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){@Override public void afterCompletion(int status){if(status==STATUS_COMMITTED)delete(old);else delete(target);}});
        return new AvatarResponse(url);
    }
    private String detect(MultipartFile file){try(InputStream in=file.getInputStream();ImageInputStream image=ImageIO.createImageInputStream(in)){if(image==null)throw new BusinessException("队徽文件不是有效图片");Iterator<ImageReader> readers=ImageIO.getImageReaders(image);if(!readers.hasNext())throw new BusinessException("队徽文件不是有效图片");ImageReader reader=readers.next();try{String ext=EXT.get(reader.getFormatName().toUpperCase(Locale.ROOT));reader.setInput(image,true,true);if(ext==null||reader.getWidth(0)<=0||reader.getHeight(0)<=0)throw new BusinessException("队徽仅支持JPEG或PNG格式");if((ext.equals(".png")&&!"image/png".equals(file.getContentType()))||(ext.equals(".jpg")&&!"image/jpeg".equals(file.getContentType())))throw new BusinessException("队徽文件类型与实际内容不一致");return ext;}finally{reader.dispose();}}catch(IOException e){throw new BusinessException("队徽文件不是有效图片");}}
    private Path managed(String url,Path dir){if(url==null||!url.startsWith(PREFIX))return null;String name=url.substring(PREFIX.length());try{Path part=Path.of(name);if(part.getNameCount()!=1)return null;Path target=dir.resolve(part).normalize();return target.startsWith(dir)?target:null;}catch(RuntimeException e){return null;}}
    private void delete(Path path){if(path==null)return;try{Files.deleteIfExists(path);}catch(IOException ignored){}}
}
