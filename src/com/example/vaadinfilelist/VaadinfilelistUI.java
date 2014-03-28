package com.example.vaadinfilelist;

import java.io.File;
import java.io.InputStream;
import java.io.Serializable;
import java.util.List;

import com.example.vaadinfilelist.singleton.Broadcaster;
import com.example.vaadinfilelist.singleton.MongoConnector;
import com.mongodb.DBObject;
import com.mongodb.gridfs.GridFSDBFile;
import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.data.Container.Filter;
import com.vaadin.data.Item;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.fieldgroup.FieldGroup;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.event.FieldEvents.TextChangeListener;
import com.vaadin.server.FileDownloader;
import com.vaadin.server.StreamResource;
import com.vaadin.server.StreamResource.StreamSource;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.AbstractTextField.TextChangeEventMode;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Table;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.Upload;
import com.vaadin.ui.Upload.SucceededEvent;
import com.vaadin.ui.Upload.SucceededListener;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
@Theme("vaadinfilelist")
@Push
public class VaadinfilelistUI extends UI implements SucceededListener, Serializable, BroadCasterListenerInt {

  private Table elementList = new Table();
  private TextField searchField = new TextField();
  private TextField metaInfo = new TextField();

  private static final String FILENAME = "Datei Name";
  private static final String FILE_DESC = "Kurzbeschreibung";
  private static final String[] fieldNames = new String[] { FILENAME, FILE_DESC };

  IndexedContainer fileFakeContainer = null;
  private MongoConnector connector;
  private Uploader uploader;
  private Upload upload;
  private FormLayout uploadLayout = new FormLayout();
  private FieldGroup uploadFields = new FieldGroup();
  private TextField beschreibungZurDatei;
  private Button downloadButton = new Button("Download File");
  private Button deleteButton = new Button("Delete File");
  FileDownloader fileDownloader;

  @Override
  protected void init(VaadinRequest request) {
    connector = MongoConnector.getConnector();
    fileFakeContainer = createDummyDatasource();
    uploader = new Uploader();
    initDeleteButton();
    initLayout();
    initFileList();
    initUploadEditor();
    initSearch();
    Broadcaster.register(this);
    setPollInterval(2000);
  }

  private void initLayout() {
    HorizontalSplitPanel splitPanel = new HorizontalSplitPanel();
    setContent(splitPanel);
    VerticalLayout leftLayout = new VerticalLayout();
    splitPanel.addComponent(leftLayout);
    splitPanel.addComponent(uploadLayout);
    leftLayout.addComponent(elementList);
    HorizontalLayout bottomLeftLayout = new HorizontalLayout();
    leftLayout.addComponent(bottomLeftLayout);
    bottomLeftLayout.addComponent(searchField);

    leftLayout.setSizeFull();

    leftLayout.setExpandRatio(elementList, 1);
    elementList.setSizeFull();

    bottomLeftLayout.setWidth("100%");
    searchField.setWidth("100%");
    bottomLeftLayout.setExpandRatio(searchField, 1);

    uploadLayout.setMargin(true);
    uploadLayout.setVisible(true);
    upload = new Upload("Upload it here", uploader);
    upload.setButtonCaption("Upload Now");
    upload.addSucceededListener(this);
    uploadLayout.addComponent(upload);

  }

  private void initSearch() {
    searchField.setInputPrompt("Search filenames");
    searchField.setTextChangeEventMode(TextChangeEventMode.LAZY);
    searchField.addTextChangeListener(new TextChangeListener() {
      @Override
      public void textChange(final TextChangeEvent event) {
        fileFakeContainer.removeAllContainerFilters();
        fileFakeContainer.addContainerFilter(new ContactFilter(event
            .getText()));
      }
    });
  }

  private void initUploadEditor() {
    String desc = "Kurzbeschreibung";
    beschreibungZurDatei = new TextField(desc);
    uploadLayout.addComponent(beschreibungZurDatei);
    uploadFields.bind(beschreibungZurDatei, desc);
    beschreibungZurDatei.setWidth("100%");
    // editorLayout.addComponent(removeContactButton);
    // editorFields.setBuffered(false);
    uploadLayout.addComponent(downloadButton);
    uploadLayout.addComponent(deleteButton);
  }

  private void initDeleteButton() {
    deleteButton.addClickListener(new ClickListener() {
      @Override
      public void buttonClick(ClickEvent event) {
        Object rowId = elementList.getValue();
        if(rowId != null){
          String fileName = (String) elementList.getContainerProperty(rowId, FILENAME).getValue();
          connector.deleteFile(fileName);
          Broadcaster.broadcast("removed File");
        }
      }
    });
  }

  private void initFileList() {
    elementList.setContainerDataSource(fileFakeContainer);
    elementList.setVisibleColumns(new String[] { FILENAME, FILE_DESC });
    elementList.setSelectable(true);
    elementList.setImmediate(true);

    elementList.addValueChangeListener(new ValueChangeListener() {

      @Override
      public void valueChange(ValueChangeEvent event) {
        Object rowId = elementList.getValue();
        if (rowId != null) {
          String fileName = (String) elementList.getContainerProperty(rowId, FILENAME).getValue();
          final GridFSDBFile dbFile = connector.loadFile(fileName);
          FileDownloader downloader = new FileDownloader(new StreamResource(new StreamSource() {
            @Override
            public InputStream getStream() {
              return dbFile.getInputStream();
            }
          }, dbFile.getFilename()));
          downloader.extend(downloadButton);
        }
      }
    });

  }

  private class ContactFilter implements Filter {
    private String needle;

    public ContactFilter(String needle) {
      this.needle = needle.toLowerCase();
    }

    @Override
    public boolean passesFilter(Object itemId, Item item) {
      String haystack = ("" + item.getItemProperty(FILENAME).getValue()
          + item.getItemProperty(FILE_DESC).getValue()).toLowerCase();
      return haystack.contains(needle);
    }

    @Override
    public boolean appliesToProperty(Object id) {
      return true;
    }
  }

  private IndexedContainer createDummyDatasource() {
    List<GridFSDBFile> findFiles = connector.findFiles();

    IndexedContainer ic = new IndexedContainer();

    for (String p : fieldNames) {
      ic.addContainerProperty(p, String.class, "");
    }
    for (GridFSDBFile gridFSDBFile : findFiles) {
      Object id = ic.addItem();
      ic.getContainerProperty(id, FILENAME).setValue(gridFSDBFile.getFilename());
      DBObject metaData = gridFSDBFile.getMetaData();
      ic.getContainerProperty(id, FILE_DESC).setValue(metaData.get("metafileText"));
    }

    return ic;
  }

  @Override
  public void uploadSucceeded(SucceededEvent event) {
    System.out.println("Upload succeeded...");
    File uploadedFile = uploader.getUploadedFile();
    connector.storeFile(uploadedFile, beschreibungZurDatei.getValue());
    Broadcaster.broadcast("new File");
    uploader.delete(uploadedFile);
  }

  @Override
  public void receiveBroadcast(String message) {
    fileFakeContainer = createDummyDatasource();
    initFileList();
    markAsDirty();
  }

  @Override
  public void detach() {
    connector.closeConnection();
    Broadcaster.unregister(this);
    super.detach();
  }

}
