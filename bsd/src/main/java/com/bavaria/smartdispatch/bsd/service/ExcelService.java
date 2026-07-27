package com.bavaria.smartdispatch.bsd.service;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List; 

public interface ExcelService {
    void procesarArchivos(MultipartFile fileData, MultipartFile fileRuta, List<String> transportesPiso) throws IOException;
}