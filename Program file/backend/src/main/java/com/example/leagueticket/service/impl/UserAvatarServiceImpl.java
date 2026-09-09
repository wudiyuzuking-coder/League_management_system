package com.example.leagueticket.service.impl;

import com.example.leagueticket.entity.SysUser;
import com.example.leagueticket.exception.BusinessException;
import com.example.leagueticket.mapper.SysUserMapper;
import com.example.leagueticket.service.SysUserService;
import com.example.leagueticket.service.UserAvatarService;
import com.example.leagueticket.vo.AvatarResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@Profile("dev")
@RequiredArgsConstructor
public class UserAvatarServiceImpl implements UserAvatarService {

    private static final long MAX_SIZE = 2L * 1024 * 1024;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png");
    private static final Map<String, String> FORMAT_EXTENSIONS = Map.of("JPEG", ".jpg", "PNG", ".png");
    private static final String PUBLIC_PREFIX = "/uploads/avatars/";

    private final SysUserService userService;
    private final SysUserMapper userMapper;

    @Value("${app.upload-dir:./uploads}")
    private String uploadDir;

    @Override
    @Transactional
    public AvatarResponse upload(Long userId, MultipartFile file) {
        SysUser user = userService.getById(userId);
        validateBasic(file);
        String extension = detectExtension(file);
        Path avatarDirectory = ensureAvatarDirectory();
        String filename = UUID.randomUUID() + extension;
        Path target = avatarDirectory.resolve(filename).normalize();
        requireInsideAvatarDirectory(avatarDirectory, target);
        save(file, target);

        String newUrl = PUBLIC_PREFIX + filename;
        try {
            if (userMapper.updateAvatarUrl(userId, newUrl) != 1) {
                throw new BusinessException(HttpStatus.NOT_FOUND, "用户不存在");
            }
        } catch (RuntimeException exception) {
            deleteQuietly(target);
            throw exception;
        }
        afterTransaction(target, resolveManagedAvatar(user.getAvatarUrl()), true);
        return new AvatarResponse(newUrl);
    }

    @Override
    @Transactional
    public void remove(Long userId) {
        SysUser user = userService.getById(userId);
        if (userMapper.updateAvatarUrl(userId, null) != 1) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "用户不存在");
        }
        afterTransaction(null, resolveManagedAvatar(user.getAvatarUrl()), false);
    }

    private void validateBasic(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("头像文件不能为空");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException(HttpStatus.PAYLOAD_TOO_LARGE, "头像文件不能超过2MB");
        }
        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new BusinessException("头像仅支持JPEG或PNG格式");
        }
        String originalName = file.getOriginalFilename();
        String lowerName = originalName == null ? "" : originalName.toLowerCase(Locale.ROOT);
        if (!(lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || lowerName.endsWith(".png"))) {
            throw new BusinessException("头像文件扩展名必须为jpg、jpeg或png");
        }
    }

    private String detectExtension(MultipartFile file) {
        try (InputStream input = file.getInputStream(); ImageInputStream imageInput = ImageIO.createImageInputStream(input)) {
            if (imageInput == null) {
                throw new BusinessException("头像文件不是有效图片");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
            if (!readers.hasNext()) {
                throw new BusinessException("头像文件不是有效图片");
            }
            ImageReader reader = readers.next();
            try {
                String format = reader.getFormatName().toUpperCase(Locale.ROOT);
                String extension = FORMAT_EXTENSIONS.get(format);
                if (extension == null) {
                    throw new BusinessException("头像仅支持JPEG或PNG格式");
                }
                if ((".png".equals(extension) && !"image/png".equals(file.getContentType()))
                        || (".jpg".equals(extension) && !"image/jpeg".equals(file.getContentType()))) {
                    throw new BusinessException("头像文件类型与实际内容不一致");
                }
                String originalName = file.getOriginalFilename().toLowerCase(Locale.ROOT);
                if ((".png".equals(extension) && !originalName.endsWith(".png"))
                        || (".jpg".equals(extension)
                        && !(originalName.endsWith(".jpg") || originalName.endsWith(".jpeg")))) {
                    throw new BusinessException("头像文件扩展名与实际内容不一致");
                }
                reader.setInput(imageInput, true, true);
                if (reader.getWidth(0) <= 0 || reader.getHeight(0) <= 0) {
                    throw new BusinessException("头像文件不是有效图片");
                }
                return extension;
            } finally {
                reader.dispose();
            }
        } catch (IOException exception) {
            throw new BusinessException("头像文件不是有效图片");
        }
    }

    private Path ensureAvatarDirectory() {
        Path directory = Path.of(uploadDir).toAbsolutePath().normalize().resolve("avatars").normalize();
        try {
            Files.createDirectories(directory);
        } catch (IOException | SecurityException exception) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "头像存储目录不可写");
        }
        if (!Files.isDirectory(directory) || !Files.isWritable(directory)) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "头像存储目录不可写");
        }
        return directory;
    }

    private void save(MultipartFile file, Path target) {
        try (InputStream input = file.getInputStream();
             OutputStream output = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            input.transferTo(output);
        } catch (IOException | SecurityException exception) {
            deleteQuietly(target);
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "头像保存失败");
        }
    }

    private Path resolveManagedAvatar(String avatarUrl) {
        if (avatarUrl == null || !avatarUrl.startsWith(PUBLIC_PREFIX)) {
            return null;
        }
        String filename = avatarUrl.substring(PUBLIC_PREFIX.length());
        final Path filenamePath;
        try {
            filenamePath = Path.of(filename);
        } catch (RuntimeException exception) {
            log.warn("Ignored unsafe avatar URL during cleanup");
            return null;
        }
        if (filename.isBlank() || filenamePath.getNameCount() != 1) {
            log.warn("Ignored unsafe avatar URL during cleanup");
            return null;
        }
        Path directory = Path.of(uploadDir).toAbsolutePath().normalize().resolve("avatars").normalize();
        Path target = directory.resolve(filename).normalize();
        return target.startsWith(directory) ? target : null;
    }

    private void requireInsideAvatarDirectory(Path directory, Path target) {
        if (!target.startsWith(directory)) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "头像保存路径无效");
        }
    }

    private void afterTransaction(Path newFile, Path oldFile, boolean removeNewOnRollback) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_COMMITTED) {
                    deleteQuietly(oldFile);
                } else if (removeNewOnRollback) {
                    deleteQuietly(newFile);
                }
            }
        });
    }

    private void deleteQuietly(Path file) {
        if (file == null) {
            return;
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException | SecurityException exception) {
            log.warn("Failed to clean up a managed avatar file");
        }
    }
}
