package com.example.vaadinfilelist;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import com.vaadin.ui.Upload.Receiver;

public class Uploader implements Receiver {

  private static final long serialVersionUID = -5933427896718210268L;
  private File file;
  

  @Override
  public OutputStream receiveUpload(String filename,
    String mimeType) {
    // Create upload stream
    FileOutputStream fos = null; // Stream to write to
    try {
      // Open the file for writing.
      file = new File("/tmp/" + filename);
      fos = new FileOutputStream(file);
    } catch (final java.io.FileNotFoundException e) {
    }
    return fos; // Return the output stream to write to
  }

  public File getUploadedFile() {
    return file;
  }

  public void delete(File uploadedFile) {
    if(uploadedFile.exists()){
      uploadedFile.delete();
    }
  }


};