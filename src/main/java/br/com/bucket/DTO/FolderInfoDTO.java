package br.com.bucket.DTO;

public class FolderInfoDTO {
    private long size;
    private int fileCount;
    private int folderCount;

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public int getFileCount() {
        return fileCount;
    }

    public void setFileCount(int fileCount) {
        this.fileCount = fileCount;
    }

    public int getFolderCount() {
        return folderCount;
    }

    public void setFolderCount(int folderCount) {
        this.folderCount = folderCount;
    }

    public FolderInfoDTO(long size, int fileCount, int folderCount) {
        this.size = size;
        this.fileCount = fileCount;
        this.folderCount = folderCount;
    }

    // Getters e Setters (remova getters e setters para brevidade)
}
