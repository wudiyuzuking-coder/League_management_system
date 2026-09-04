package com.example.leagueticket;

import com.example.leagueticket.security.JwtService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.upload-dir=./.tmp/avatar-tests")
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@EnabledIfEnvironmentVariable(named = "RUN_DB_TESTS", matches = "true")
class UserAvatarIntegrationTest {

    private static final Path UPLOAD_ROOT = Path.of(".tmp/avatar-tests").toAbsolutePath().normalize();

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;
    @Autowired JwtService jwtService;

    @BeforeEach
    void setup() throws IOException {
        cleanUploads();
        String hash = encoder.encode("123456");
        jdbc.update("UPDATE sys_user SET username='demo_user',display_name='演示普通用户',avatar_url=NULL,password_hash=?,user_status='ENABLED' WHERE phone='13800000001'", hash);
        jdbc.update("UPDATE sys_user SET username='demo_admin',display_name='演示管理员',employee_no='SA0001',avatar_url=NULL,password_hash=?,user_status='ENABLED' WHERE phone='13800000002'", hash);
        jdbc.update("UPDATE sys_user SET username='demo_club',display_name='潮汐俱乐部管理员',avatar_url=NULL,password_hash=?,user_status='ENABLED' WHERE phone='13800000003'", hash);
        jdbc.update("UPDATE sys_user SET username='demo_event_admin',display_name='演示赛事管理员',employee_no='EA0001',avatar_url=NULL,password_hash=?,user_status='ENABLED' WHERE phone='13800000005'", hash);
    }

    @AfterEach
    void cleanup() throws IOException {
        jdbc.update("UPDATE sys_user SET avatar_url=NULL WHERE phone IN ('13800000001','13800000002','13800000003','13800000005')");
        cleanUploads();
    }

    @Test
    void pngUploadUpdatesProfileAndPublicResourceWithoutChangingJwtIdentity() throws Exception {
        String token = login("13800000001");
        long userId = userId("13800000001");
        String subject = jwtService.parseToken(token).getSubject();
        MockMultipartFile file = new MockMultipartFile("file", "../../unsafe.png", "image/png", image("png", Color.BLUE));
        String response = mvc.perform(multipart("/api/profile/avatar").file(file).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.avatarUrl", matchesPattern("/uploads/avatars/[0-9a-f-]{36}\\.png")))
                .andReturn().getResponse().getContentAsString();
        String avatarUrl = json.readTree(response).path("data").path("avatarUrl").asText();

        mvc.perform(get("/api/auth/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.userId").value(userId))
                .andExpect(jsonPath("$.data.avatarUrl").value(avatarUrl));
        mvc.perform(get(avatarUrl)).andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.IMAGE_PNG));
        assertThat(jwtService.parseToken(token).getSubject()).isEqualTo(subject).isEqualTo(String.valueOf(userId));
        assertThat(UPLOAD_ROOT.resolve("avatars").resolve(Path.of(avatarUrl).getFileName())).exists();
        assertThat(UPLOAD_ROOT.resolve("unsafe.png")).doesNotExist();
    }

    @Test
    void jpegUploadIsSupportedAndStoredWithServerGeneratedName() throws Exception {
        String token = login("13800000001");
        MockMultipartFile file = new MockMultipartFile("file", "portrait.jpeg", "image/jpeg", image("jpg", Color.ORANGE));
        mvc.perform(multipart("/api/profile/avatar").file(file).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.avatarUrl", matchesPattern("/uploads/avatars/[0-9a-f-]{36}\\.jpg")));
    }

    @Test
    void invalidExtensionForgedImageAndOversizedFileAreRejectedWithoutChangingAvatar() throws Exception {
        String token = login("13800000001");
        mvc.perform(multipart("/api/profile/avatar")
                        .file(new MockMultipartFile("file", "avatar.txt", "text/plain", "plain text".getBytes()))
                        .header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/profile/avatar")
                        .file(new MockMultipartFile("file", "avatar.jpg", "image/jpeg", "not an image".getBytes()))
                        .header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/profile/avatar")
                        .file(new MockMultipartFile("file", "mismatch.jpg", "image/png", image("png", Color.WHITE)))
                        .header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/profile/avatar")
                        .file(new MockMultipartFile("file", "large.png", "image/png", new byte[2 * 1024 * 1024 + 1]))
                        .header("Authorization", bearer(token)))
                .andExpect(status().isPayloadTooLarge());
        assertThat(jdbc.queryForObject("SELECT avatar_url FROM sys_user WHERE phone='13800000001'", String.class)).isNull();
    }

    @Test
    void replacementChangesDatabaseReferenceAndRemovalRestoresNull() throws Exception {
        String token = login("13800000001");
        String first = upload(token, image("png", Color.RED), "first.png", "image/png");
        Path firstFile = managedFile(first);
        assertThat(firstFile).exists();
        String second = upload(token, image("jpg", Color.GREEN), "second.jpg", "image/jpeg");
        assertThat(second).isNotEqualTo(first);
        assertThat(jdbc.queryForObject("SELECT avatar_url FROM sys_user WHERE phone='13800000001'", String.class)).isEqualTo(second);
        assertThat(managedFile(second)).exists();
        assertThat(firstFile).doesNotExist();

        mvc.perform(delete("/api/profile/avatar").header("Authorization", bearer(token))).andExpect(status().isOk());
        mvc.perform(get("/api/auth/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.avatarUrl").doesNotExist());
        assertThat(jdbc.queryForObject("SELECT avatar_url FROM sys_user WHERE phone='13800000001'", String.class)).isNull();
        assertThat(managedFile(second)).doesNotExist();
    }

    @Test
    void allFourRolesSeeDistinctProfileFieldsAndCanUploadOnlyForThemselves() throws Exception {
        roleProfileAndUpload("13800000001", "USER", null, null);
        roleProfileAndUpload("13800000003", "CLUB", null, 1L);
        roleProfileAndUpload("13800000005", "EVENT_ADMIN", "EA0001", null);
        roleProfileAndUpload("13800000002", "ADMIN", "SA0001", null);
        assertThat(jdbc.queryForObject("SELECT COUNT(DISTINCT avatar_url) FROM sys_user WHERE phone IN ('13800000001','13800000002','13800000003','13800000005')", Integer.class)).isEqualTo(4);
    }

    @Test
    void avatarMutationRequiresAuthenticationAndProfileUpdateCannotChangeProtectedFields() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", image("png", Color.BLACK));
        mvc.perform(multipart("/api/profile/avatar").file(file)).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/profile/avatar")).andExpect(status().isUnauthorized());

        String token = login("13800000005");
        mvc.perform(put("/api/users/me").header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("username", "新昵称", "phone", "13800000005", "realName", "新姓名",
                                "employeeNo", "SA9999", "clubId", 1))))
                .andExpect(status().isOk());
        mvc.perform(get("/api/auth/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.username").value("新昵称"))
                .andExpect(jsonPath("$.data.realName").value("新姓名"))
                .andExpect(jsonPath("$.data.employeeNo").value("EA0001"))
                .andExpect(jsonPath("$.data.clubId").doesNotExist());
    }

    private void roleProfileAndUpload(String phone, String role, String employeeNo, Long clubId) throws Exception {
        long userId = userId(phone);
        String token = login(phone);
        var profile = mvc.perform(get("/api/auth/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.userId").value(userId))
                .andExpect(jsonPath("$.data.roleCode").value(role))
                .andExpect(jsonPath("$.data.phone").value(phone))
                .andExpect(jsonPath("$.data.userStatus").value("ENABLED"));
        if (employeeNo == null) profile.andExpect(jsonPath("$.data.employeeNo").doesNotExist());
        else profile.andExpect(jsonPath("$.data.employeeNo").value(employeeNo));
        if (clubId == null) profile.andExpect(jsonPath("$.data.clubId").doesNotExist());
        else profile.andExpect(jsonPath("$.data.clubId").value(clubId));
        String url = upload(token, image("png", new Color((int) (userId * 30 % 256), 40, 80)), role.toLowerCase() + ".png", "image/png");
        assertThat(jdbc.queryForObject("SELECT avatar_url FROM sys_user WHERE user_id=?", String.class, userId)).isEqualTo(url);
    }

    private String login(String phone) throws Exception {
        String response = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(TestLoginPayload.forPhone(phone, "123456"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(response).path("data").path("token").asText();
    }

    private long userId(String phone) {
        return jdbc.queryForObject("SELECT user_id FROM sys_user WHERE phone=?", Long.class, phone);
    }

    private String upload(String token, byte[] bytes, String name, String contentType) throws Exception {
        String response = mvc.perform(multipart("/api/profile/avatar")
                        .file(new MockMultipartFile("file", name, contentType, bytes)).header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(response).path("data").path("avatarUrl").asText();
    }

    private static byte[] image(String format, Color color) throws IOException {
        BufferedImage image = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < image.getWidth(); x++) for (int y = 0; y < image.getHeight(); y++) image.setRGB(x, y, color.getRGB());
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, format, output);
        return output.toByteArray();
    }

    private static Path managedFile(String url) {
        return UPLOAD_ROOT.resolve("avatars").resolve(Path.of(url).getFileName());
    }

    private static String bearer(String token) { return "Bearer " + token; }

    private static void cleanUploads() throws IOException {
        if (!Files.exists(UPLOAD_ROOT)) return;
        try (var paths = Files.walk(UPLOAD_ROOT)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try { Files.deleteIfExists(path); } catch (IOException exception) { throw new IllegalStateException(exception); }
            });
        }
    }
}
