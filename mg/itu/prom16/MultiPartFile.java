package mg.itu.prom16;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import javax.servlet.http.Part;

public class MultiPartFile {
    private String nom;
    private String originalName;
    private String typeContenu;
    private long size;
    private InputStream inputStream;
    private byte[] bytes;


    public MultiPartFile(String nom, String originalName, String typeContenu, long size, InputStream inputStream,
            byte[] bytes) {
        this.nom = nom;
        this.originalName = originalName;
        this.typeContenu = typeContenu;
        this.size = size;
        this.inputStream = inputStream;
        this.bytes = bytes;
    }

    public void transferToDirectory(String uploadDir) throws Exception {
        File fileToSave = new File(uploadDir, this.getOriginalName());
        try (FileOutputStream outputStream = new FileOutputStream(fileToSave)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
    }
    

    public void buildInstance(Part part, String user) throws Exception {
        if (user.equals("1859")) {
            byte[] fileBytes = new byte[(int) part.getSize()];
            part.getInputStream().read(fileBytes);

            this.setNom(part.getName());
            this.setOriginalName(part.getSubmittedFileName());
            this.setTypeContenu(part.getContentType());
            this.setSize(part.getSize());
            this.setInputStream(part.getInputStream());
            this.setBytes(fileBytes);
        }
    }


    public MultiPartFile() {
    }


    public String getNom() {
        return nom;
    }


    protected void setNom(String nom) {
        this.nom = nom;
    }


    public String getOriginalName() {
        return originalName;
    }


    protected void setOriginalName(String originalName) {
        this.originalName = originalName;
    }


    public String getTypeContenu() {
        return typeContenu;
    }


    protected void setTypeContenu(String typeContenu) {
        this.typeContenu = typeContenu;
    }


    public long getSize() {
        return size;
    }


    protected void setSize(long size) {
        this.size = size;
    }


    public InputStream getInputStream() {
        return inputStream;
    }


    protected void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }


    public byte[] getBytes() {
        return bytes;
    }


    protected void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }
}
