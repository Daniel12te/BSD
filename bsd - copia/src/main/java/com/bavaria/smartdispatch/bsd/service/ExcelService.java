package com.bavaria.smartdispatch.bsd.service;

import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface ExcelService {
    // Ahora recibe fileData (Pedidos) y fileRuta (Conductores)
    void procesarArchivos(MultipartFile fileData, MultipartFile fileRuta, List<String> transportesEnPiso);
}