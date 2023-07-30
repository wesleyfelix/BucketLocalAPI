package br.com.bucket.service;

import br.com.bucket.DTO.BucketDTO;
import br.com.bucket.DTO.FolderInfoDTO;
import br.com.bucket.controller.BucketController;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class BucketService {

    @Value("${file.upload.dir}")
    private String uploadDir;

    private static final Logger LOGGER = LoggerFactory.getLogger(BucketService.class);

    public void uploadFile(MultipartFile file, String directory) throws IOException {
        LOGGER.info("UPLOAD - Iniciando");

        String targetDirectory = uploadDir;

        if (directory != null && !directory.isEmpty()) {
            targetDirectory += File.separator + directory;
        }

        File targetDir = new File(targetDirectory);
        if (!targetDir.exists()) {
            LOGGER.info("UPLOAD - Criando diretório");
            targetDir.mkdirs();
        }
        LOGGER.info("UPLOAD - Criando arquivo");
        File dest = new File(targetDirectory + File.separator + file.getOriginalFilename());
        file.transferTo(dest);
        LOGGER.info("UPLOAD -Criando arquivo Finalizado");
    }

    public void createFolder(String folderName, String directory) throws IOException {
        String targetDirectory = uploadDir;
        if (directory != null && !directory.isEmpty()) {
            targetDirectory += File.separator + directory;
        }

        File newFolder = new File(targetDirectory + File.separator + folderName);
        if (!newFolder.exists()) {
            LOGGER.info("UPLOAD - Criando pasta");
            newFolder.mkdirs();
            LOGGER.info("UPLOAD - Criando pasta finalizado");
        }
    }

    public List<BucketDTO> listFiles() {
        LOGGER.info("LISTAR PASTAS/ARQUIVOS - Iniciando");
        File directory = new File(uploadDir);
        List<BucketDTO> fileList = new ArrayList<>();

        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    fileList.add(getBucketDTOFromFile(file));
                }
            }
        }
        LOGGER.info("LISTAR PASTAS/ARQUIVOS - Finalizado");
        return fileList;
    }

    public List<BucketDTO> listFilesByFolder(String folderName) {
        File folder = new File(uploadDir + File.separator + folderName);
        List<BucketDTO> fileList = new ArrayList<>();

        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    fileList.add(getBucketDTOFromFile(file));
                }
            }
        }

        return fileList;
    }
    public boolean deleteFileOrFolder(String fileName) {
        File file = new File(uploadDir + File.separator + fileName);
        return deleteFileOrFolderRecursive(file);
    }

    private boolean deleteFileOrFolderRecursive(File file) {
        if (!file.exists()) {
            return false;
        }

        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File subFile : files) {
                    deleteFileOrFolderRecursive(subFile);
                }
            }
        }

        return file.delete();
    }

    public byte[] getFileContent(String fileName) throws IOException {
        File file = new File(uploadDir + File.separator + fileName);
        return Files.readAllBytes(file.toPath());
    }

    public void downloadFile(String fileName, HttpServletResponse response) {
        File file = new File(uploadDir + File.separator + fileName);

        if (!file.exists()) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return;
        }

        if (file.isDirectory()) {
            try {
                String zipFileName = file.getName() + ".zip";
                response.setContentType("application/zip");
                response.setHeader("Content-Disposition", "attachment; filename=" + zipFileName);

                ZipOutputStream zipOut = new ZipOutputStream(response.getOutputStream());
                zipFile(file, file.getName(), zipOut);
                zipOut.close();

                response.setStatus(HttpStatus.OK.value());
            } catch (IOException e) {
                response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            }
        } else {
            try {
                byte[] fileContent = getFileContent(fileName);
                response.setContentType("application/octet-stream");
                response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
                response.getOutputStream().write(fileContent);
                response.setStatus(HttpStatus.OK.value());
            } catch (IOException e) {
                response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            }
        }
    }

    private void zipFile(File fileToZip, String fileName, ZipOutputStream zipOut) throws IOException {
        if (fileToZip.isHidden()) {
            return;
        }

        if (fileToZip.isDirectory()) {
            if (fileName.endsWith("/")) {
                zipOut.putNextEntry(new ZipEntry(fileName));
                zipOut.closeEntry();
            } else {
                zipOut.putNextEntry(new ZipEntry(fileName + "/"));
                zipOut.closeEntry();
            }

            File[] children = fileToZip.listFiles();
            for (File childFile : children) {
                zipFile(childFile, fileName + "/" + childFile.getName(), zipOut);
            }
        } else {
            FileInputStream fis = new FileInputStream(fileToZip);
            ZipEntry zipEntry = new ZipEntry(fileName);
            zipOut.putNextEntry(zipEntry);

            byte[] bytes = new byte[1024];
            int length;
            while ((length = fis.read(bytes)) >= 0) {
                zipOut.write(bytes, 0, length);
            }

            fis.close();
        }
    }


    private BucketDTO getBucketDTOFromFile(File file) {
        BucketDTO dto = new BucketDTO();
        dto.setNome(file.getName());

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            BasicFileAttributes attrs = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
            Date creationDate = new Date(attrs.creationTime().toMillis());
            dto.setDataCriacao(dateFormat.format(creationDate));

            if (file.isDirectory()) {
                FolderInfoDTO folderInfo = getFolderInfo(file);
                dto.setTipo("folder");
                dto.setTamanho(formatFileSize(folderInfo.getSize()));
                dto.setDetalhes(String.format("Arquivos: %d, Pastas: %d", folderInfo.getFileCount(), folderInfo.getFolderCount()));
            } else {
                dto.setTipo(getFileType(file.getName()));
                dto.setTamanho(formatFileSize(file.length()));
                dto.setDetalhes("");

            }
        } catch (IOException e) {

            dto.setDataCriacao("Desconhecido");
            dto.setTamanho("Desconhecido");
            dto.setDetalhes("");
        }

        return dto;
    }


    private String formatFileSize(long fileSize) {
        final int unit = 1024;
        if (fileSize < unit) {
            return fileSize + " bytes";
        }
        int exp = (int) (Math.log(fileSize) / Math.log(unit));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f %cB", fileSize / Math.pow(unit, exp), pre);
    }

    private String getFileType(String fileName) {
        int lastDotIndex = fileName.lastIndexOf(".");
        if (lastDotIndex > 0) {
            return fileName.substring(lastDotIndex + 1);
        } else {
            return "Desconhecido";
        }
    }

    private FolderInfoDTO getFolderInfo(File folder) {
        long size = 0;
        int fileCount = 0;
        int folderCount = 0;

        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    FolderInfoDTO subFolderInfo = getFolderInfo(file);
                    size += subFolderInfo.getSize();
                    fileCount += subFolderInfo.getFileCount();
                    folderCount += subFolderInfo.getFolderCount() + 1;
                } else {
                    size += file.length();
                    fileCount++;
                }
            }
        }

        return new FolderInfoDTO(size, fileCount, folderCount);
    }

}
