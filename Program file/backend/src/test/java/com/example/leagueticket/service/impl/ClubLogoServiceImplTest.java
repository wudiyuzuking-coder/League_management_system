package com.example.leagueticket.service.impl;

import com.example.leagueticket.entity.ClubInfo;
import com.example.leagueticket.exception.BusinessException;
import com.example.leagueticket.mapper.ClubInfoMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ClubLogoServiceImplTest {

    @TempDir Path directory;
    @Mock ClubInfoMapper mapper;
    ClubLogoServiceImpl service;

    @BeforeEach void setUp(){
        ClubInfo club=new ClubInfo();club.setClubId(7L);
        lenient().when(mapper.findById(7L)).thenReturn(club);
        lenient().when(mapper.updateLogoUrl(org.mockito.ArgumentMatchers.eq(7L),org.mockito.ArgumentMatchers.anyString())).thenReturn(1);
        service=new ClubLogoServiceImpl(mapper);
        ReflectionTestUtils.setField(service,"uploadDir",directory.toString());
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach void tearDown(){
        if(TransactionSynchronizationManager.isSynchronizationActive()){
            TransactionSynchronizationUtils.triggerAfterCompletion(TransactionSynchronization.STATUS_COMMITTED);
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test void pngIsAccepted(){
        assertThat(service.upload(7L,file("club.png","image/png",image("png"))).avatarUrl()).endsWith(".png");
    }

    @Test void pngWithOctetStreamIsAcceptedAfterContentInspection(){
        assertThat(service.upload(7L,file("club.png","application/octet-stream",image("png"))).avatarUrl()).endsWith(".png");
    }

    @Test void jpegIsAccepted(){
        assertThat(service.upload(7L,file("club.jpeg","image/jpeg",image("jpg"))).avatarUrl()).endsWith(".jpg");
    }

    @Test void jpegDeclaredAsPngIsRejected(){
        assertThatThrownBy(()->service.upload(7L,file("club.jpg","image/png",image("jpg"))))
                .isInstanceOf(BusinessException.class).hasMessage("队徽文件类型与实际内容不一致");
    }

    @Test void disguisedNonImageIsRejected(){
        assertThatThrownBy(()->service.upload(7L,file("club.png","image/png","not an image".getBytes())))
                .isInstanceOf(BusinessException.class).hasMessage("队徽文件不是有效图片");
    }

    @Test void fileOverTwoMegabytesReturns413(){
        assertThatThrownBy(()->service.upload(7L,file("large.png","image/png",new byte[2*1024*1024+1])))
                .isInstanceOfSatisfying(BusinessException.class,e->assertThat(e.getHttpStatus()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE));
    }

    private MockMultipartFile file(String name,String contentType,byte[] data){return new MockMultipartFile("file",name,contentType,data);}
    private byte[] image(String format){try{BufferedImage value=new BufferedImage(2,2,BufferedImage.TYPE_INT_RGB);value.setRGB(0,0,Color.BLUE.getRGB());ByteArrayOutputStream output=new ByteArrayOutputStream();ImageIO.write(value,format,output);return output.toByteArray();}catch(Exception e){throw new RuntimeException(e);}}
}
