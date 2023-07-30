package br.com.bucket.controller;

import br.com.bucket.service.BucketService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import br.com.bucket.DTO.BucketDTO;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.web.server.ResponseStatusException;


import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/bucket")
public class BucketController {

    private final BucketService bucketService;
    private static final Logger LOGGER = LoggerFactory.getLogger(BucketController.class);

    @Autowired
    public BucketController(BucketService bucketService) {
        this.bucketService = bucketService;
    }

    @GetMapping("/teste")
    public ResponseEntity<String> get() {
        return ResponseEntity.ok("Teste na API");
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam(value = "file", required = false) MultipartFile file,
                                             @RequestParam(value = "folder", required = false) String folderName,
                                             @RequestParam(value = "directory", required = false) String directory) {
        try {
            if (file != null) {
                bucketService.uploadFile(file, directory);
                return ResponseEntity.ok("Arquivo carregado com sucesso!");
            } else if (folderName != null && !folderName.isEmpty()) {
                bucketService.createFolder(folderName, directory);
                return ResponseEntity.ok("Pasta criada com sucesso!");
            } else {
                return ResponseEntity.badRequest().body("Requisição inválida. Por favor, forneça um arquivo ou nome de pasta válido.");
            }
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Erro ao carregar o arquivo ou criar a pasta.");
        }
    }

    @GetMapping("/list")
    public ResponseEntity<List<BucketDTO>> listFiles(@RequestParam(value = "folder", required = false) String folderName) {
        List<BucketDTO> fileList;
        if (folderName != null && !folderName.isEmpty()) {
            fileList = bucketService.listFilesByFolder(folderName);
        } else {
            fileList = bucketService.listFiles();
        }
        return ResponseEntity.ok(fileList);
    }

    @GetMapping("/view/{fileName}")
    public ResponseEntity<byte[]> viewFile(@PathVariable String fileName) {
        try {
            byte[] fileContent = bucketService.getFileContent(fileName);
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).body(fileContent);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/download/{fileName}")
    public ResponseEntity<Void> downloadFile(@PathVariable String fileName, HttpServletResponse response) {
        HttpStatus status = bucketService.downloadFile(fileName, response);

        if (status == HttpStatus.OK) {
            return ResponseEntity.ok().build();
        } else if (status == HttpStatus.NOT_FOUND) {
            return ResponseEntity.notFound().build();
        } else {
            return ResponseEntity.status(status).build();
        }
    }




    @DeleteMapping("/delete/{fileName}")
    public ResponseEntity<String> deleteFile(@PathVariable String fileName) {
        boolean deleted = bucketService.deleteFileOrFolder(fileName);
        if (deleted) {
            return ResponseEntity.ok("Arquivo ou pasta " + fileName + " deletado(a) com sucesso!");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Arquivo ou pasta não encontrado(a).");
        }
    }
}
