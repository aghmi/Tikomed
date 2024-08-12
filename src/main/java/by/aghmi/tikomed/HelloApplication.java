package by.aghmi.tikomed;

import static java.lang.Integer.compare;
import static java.lang.String.format;
import static java.time.Instant.now;
import static java.util.Arrays.asList;
import static javafx.collections.FXCollections.observableArrayList;

import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import javafx.application.Application;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class HelloApplication extends Application {
    public static class Service {

        private String code;
        private String name;
        private volatile Double price;
        private Integer quantity;
        private Boolean isHeader;
        private Double totalPrice;

        public String getCode() {
            return code;
        }

        public void setCode(final String code) {
            this.code = code;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public Double getPrice() {
            return price;
        }

        public void setPrice(final Double price) {
            this.price = price;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(final Integer quantity) {
            this.quantity = quantity;
        }

        public Boolean getHeader() {
            return isHeader;
        }

        public void setHeader(final Boolean header) {
            isHeader = header;
        }

        public void setTotalPrice(final Double totalPrice) {
            this.totalPrice = totalPrice;
        }

        public Service(final String code, final String name, final Double price, final Integer quantity,
                final Boolean isHeader, final Double totalPrice) {
            this.code = code;
            this.name = name;
            this.price = price;
            this.quantity = quantity;
            this.isHeader = isHeader;
            this.totalPrice = totalPrice;
        }

        public Double getTotalPrice() {
            return quantity != null && quantity != 0 ? price * quantity : 0.0;
        }

        public boolean isHeader() {
            return isHeader;
        }
    }

    private TableView<Service> completeTableView;
    private TableView<Service> selectedTableView;
    private Label sumLabel;

    @Override
    public void start(final Stage primaryStage) {
        System.out.printf("start time: %s%n", now());

        primaryStage.setTitle("ТикоМед: Прейскурант");
        primaryStage.setMaximized(true);

        final TableColumn<Service, String> codeColumn = new TableColumn<>("Код услуги");
        codeColumn.setCellValueFactory(new PropertyValueFactory<>("code"));
        codeColumn.setComparator((code1, code2) -> {
            final String[] parts1 = code1.split("\\.");
            final String[] parts2 = code2.split("\\.");
            for (int i = 0; i < Math.min(parts1.length, parts2.length); i++) {
                int part1 = Integer.parseInt(parts1[i]);
                int part2 = Integer.parseInt(parts2[i]);
                if (part1 != part2) {
                    return compare(part1, part2);
                }
            }
            return compare(parts1.length, parts2.length);
        });

        final TableColumn<Service, String> nameColumn = new TableColumn<>("Наименование услуги");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        final TableColumn<Service, Double> priceColumn = new TableColumn<>("Цена, бел. руб.");
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        final TableColumn<Service, Integer> quantityColumn = getServiceTableQuantityColumn();

        completeTableView = new TableView<>();
        completeTableView.getColumns().addAll(asList(codeColumn, nameColumn, priceColumn, quantityColumn));
        completeTableView.setPrefHeight(primaryStage.getHeight() - 45);
        completeTableView.setPrefWidth(primaryStage.getWidth() * 0.5);
        loadServicesFromCSV();
        applySorting();
        final List<Service> allServices = new ArrayList<>(completeTableView.getItems());
        completeTableView.getItems().addListener((ListChangeListener<Service>) change -> {
            while (change.next()) {
                if (change.wasAdded() || change.wasRemoved()) {
                    updateSelectedTableView();
                }
            }
        });

        sumLabel = new Label("ИТОГО, бел. руб.: 0");

        final TableColumn<Service, String> chosenCodeColumn = new TableColumn<>("Код услуги");
        chosenCodeColumn.setCellValueFactory(new PropertyValueFactory<>("code"));
        final TableColumn<Service, Integer> chosenQuantityColumn = new TableColumn<>("Кол-во, шт.");
        chosenQuantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        final TableColumn<Service, String> chosenTotalPriceColumn = new TableColumn<>("Сумма, бел. руб.");
        chosenTotalPriceColumn.setCellValueFactory(new PropertyValueFactory<>("totalPrice"));

        selectedTableView = new TableView<>();
        selectedTableView.setPlaceholder(new Label("Нет выбранных услуг"));
        selectedTableView.setPrefHeight(primaryStage.getHeight() * 0.5);
        selectedTableView.setPrefWidth(primaryStage.getWidth() * 0.25);
        selectedTableView.getColumns().addAll(asList(chosenCodeColumn, chosenQuantityColumn, chosenTotalPriceColumn));
        selectedTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                boolean serviceExists = completeTableView.getItems().stream()
                        .anyMatch(service -> service.getCode().equals(newSelection.getCode()));
                if (!serviceExists) {
                    completeTableView.getItems().add(newSelection);
                    int [] nums = {2, 7, 11, 15};
                }
            }
        });

        final Button downloadButton = new Button("Акт выполненных работ");

        final TextField customerNameField = new TextField();
        customerNameField.setDisable(true);
        customerNameField.setPromptText("ВВОД ФИО - НЕ ИСПОЛЬЗОВАТЬ!");
        customerNameField.setPrefWidth(300);

        final DatePicker datePicker = new DatePicker();
        datePicker.setValue(LocalDate.now());

        downloadButton.setOnAction(e -> {
            final String timestamp = Long.toString(System.currentTimeMillis());
            //TODO Change to D:
            final String folderPath = "C:/Акты выполненных работ/";
            final File folder = new File(folderPath);
            if (!folder.exists()) {
                folder.mkdirs();
            }
            String fileName = folderPath + "Акт_выполненных_работ_" + timestamp + ".xlsx";
            ExcelGenerator.generateActOfWorks(fileName, selectedTableView.getItems(), customerNameField.getText(),
                    datePicker.getValue());

            try {
                Desktop.getDesktop().open(new File(fileName));
            } catch (IOException ex) {
                System.err.println("Ошибка при открытии файла: " + ex.getMessage());
            }
        });

        final Button resetButton = getResetButton();

        final TextField searchField = new TextField();
        searchField.setPromptText("Поиск...");
        searchField.setPrefWidth(completeTableView.getWidth() * 0.5);
        searchField.setPrefHeight(40);
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue.isEmpty()) {
                completeTableView.getItems().clear();
                completeTableView.getItems().addAll(allServices);
            } else {
                final String lowerCaseFilter = newValue.toLowerCase();
                final List<Service> filteredServices = allServices.stream()
                        .filter(service -> service.getCode().toLowerCase().startsWith(lowerCaseFilter)).toList();
                completeTableView.getItems().clear();
                completeTableView.getItems().addAll(filteredServices);
            }
            updateSelectedTableView();
            updateTotalPrice();
        });

        final HBox centerContainer = new HBox(40);
        centerContainer.setPadding(
                new javafx.geometry.Insets(0, 5, 0, 5)); // Устанавливаем отступы для центральной панели
        centerContainer.setAlignment(Pos.BOTTOM_CENTER);
        centerContainer.getChildren().addAll(customerNameField, datePicker); // Добавляем DatePicker

        final HBox rightContainer = new HBox(80);
        rightContainer.setPadding(new javafx.geometry.Insets(0, 5, 0, 5));
        rightContainer.setAlignment(Pos.BOTTOM_RIGHT);
        rightContainer.getChildren().addAll(resetButton, downloadButton, sumLabel);

        final HBox bottomContainer = new HBox(40);
        bottomContainer.setPadding(new javafx.geometry.Insets(20, 25, 20, 5));
        bottomContainer.getChildren().addAll(centerContainer, rightContainer);

        final BorderPane root = new BorderPane();
        root.setTop(searchField);
        root.setLeft(completeTableView);
        root.setRight(selectedTableView);
        root.setBottom(bottomContainer);
        BorderPane.setMargin(selectedTableView, new javafx.geometry.Insets(0, 5, 0, 5));
        BorderPane.setMargin(completeTableView, new javafx.geometry.Insets(0, 0, 0, 5));
        BorderPane.setMargin(centerContainer,
                new javafx.geometry.Insets(0, 5, 0, 5)); // Устанавливаем отступы для центральной панели
        BorderPane.setMargin(rightContainer, new javafx.geometry.Insets(0, 5, 0, 5));
        BorderPane.setMargin(searchField, new Insets(5, 5, 5, 5));

        // Создаем сцену и устанавливаем ее в primaryStage
        Scene scene = new Scene(root);
        scene.getStylesheets().add("styles.css");
        primaryStage.setScene(scene);
        primaryStage.show();

        completeTableView.setPrefWidth(primaryStage.getWidth() * 0.70);
        selectedTableView.setPrefWidth(primaryStage.getWidth() * 0.30);

        codeColumn.prefWidthProperty().bind(completeTableView.widthProperty().multiply(0.1));
        nameColumn.prefWidthProperty().bind(completeTableView.widthProperty().multiply(0.55));
        priceColumn.prefWidthProperty().bind(completeTableView.widthProperty().multiply(0.1));
        quantityColumn.prefWidthProperty().bind(completeTableView.widthProperty().multiply(0.2));

        chosenCodeColumn.prefWidthProperty().bind(selectedTableView.widthProperty().multiply(0.3));
        chosenQuantityColumn.prefWidthProperty().bind(selectedTableView.widthProperty().multiply(0.3));
        chosenTotalPriceColumn.prefWidthProperty().bind(selectedTableView.widthProperty().multiply(0.3));

        codeColumn.setStyle("-fx-font-size: 18;");
        nameColumn.setStyle("-fx-font-size: 18;");
        priceColumn.setStyle("-fx-font-size: 18;");
        quantityColumn.setStyle("-fx-font-size: 18;");

        codeColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    Service service = getTableView().getItems().get(getIndex());
                    if (service.isHeader()) {
                        setStyle("-fx-font-weight: bold;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });

        nameColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle(""); // Сбрасываем стиль
                } else {
                    setText(item);
                    Service service = getTableView().getItems().get(getIndex());
                    if (service.isHeader()) {
                        setStyle("-fx-font-weight: bold;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }

    private Button getResetButton() {
        Button resetButton = new Button("Сброс данных");
        resetButton.setOnAction(e -> {
            selectedTableView.getItems().clear();
            completeTableView.getItems().forEach(service -> {
                service.setQuantity(0);
            });

            completeTableView.refresh(); // Обновляем таблицу, чтобы отобразить изменения в спиннерах
            updateTotalPrice(); // Обновляем итоговую цену после сброса
        });
        return resetButton;
    }

    private void applySorting() {
        completeTableView.getSortOrder().addListener((ListChangeListener<TableColumn<Service, ?>>) change -> {
            if (!completeTableView.getItems().isEmpty()) {
                completeTableView.getItems().removeIf(Service::isHeader);
            }
        });
    }

    private TableColumn<Service, Integer> getServiceTableQuantityColumn() {
        final TableColumn<Service, Integer> quantityColumn = new TableColumn<>("Количество, шт.");
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        quantityColumn.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    setText(null);
                } else {
                    final Service service = getTableView().getItems().get(getIndex());
                    if (service.isHeader()) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        // Проверяем код услуги
                        if (isIncrementByFive(service.getCode())) {
                            final TextField textField = new TextField();
                            textField.setText(item != null ? item.toString() : "");
                            textField.setMaxWidth(50); // Устанавливаем максимальную ширину поля
                            textField.textProperty().addListener((observable, oldValue, newValue) -> {
                                if (!newValue.matches("\\d*")) { // Проверяем, что введено только число
                                    textField.setText(newValue.replaceAll("[^\\d]", ""));
                                }
                                // Обновляем количество только при изменении значения
                                if (!newValue.equals(oldValue)) {
                                    service.setQuantity(newValue.isEmpty() ? 0 : Integer.parseInt(newValue));
                                    updateTotalPrice();
                                    updateSelectedTableView();
                                }
                            });
                            setGraphic(textField);
                            setText(null);
                        } else {
                            // В противном случае используем спиннер
                            final Spinner<Integer> spinner = new Spinner<>(0, 99, item);
                            spinner.valueProperty().addListener((observable, oldValue, newValue) -> {
                                service.setQuantity(newValue);
                                updateTotalPrice();
                                updateSelectedTableView();
                            });
                            setGraphic(spinner);
                            setText(null);
                        }
                    }
                }
            }
        });
        return quantityColumn;
    }

    private void updateTotalPrice() {
        final Double totalPrice = completeTableView.getItems().stream().filter(service -> service.getQuantity() > 0)
                .mapToDouble(service -> {
                    final Double total = service.getTotalPrice();
                    return total != null ? total : 0.0; // Проверка на null
                }).sum();
        sumLabel.setText("ИТОГО: " + format("%.2f", totalPrice)); // Округление до 2 знаков после запятой
    }

    private void updateSelectedTableView() {
        selectedTableView.getItems()
                .setAll(completeTableView.getItems().stream().filter(service -> service.getQuantity() > 0)
                        .collect(Collectors.toList()));
    }

    private void loadServicesFromCSV() {
        try (final BufferedReader reader = new BufferedReader(
                new InputStreamReader(Objects.requireNonNull(getClass().getResourceAsStream("/prices.csv")),
                        StandardCharsets.UTF_8))) {
            final ObservableList<Service> services = observableArrayList();
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 2) {
                    final String code = parts[0].trim();
                    final String name = parts[1].trim();
                    Double price = null;
                    if (parts.length > 2 && !parts[2].isEmpty()) {
                        price = Double.parseDouble(parts[2].trim());
                    }
                    final boolean isHeader = (price == null);

                    services.add(new Service(code, name, price, 0, isHeader, null));
                }
            }
            completeTableView.setItems(services);
        } catch (IOException e) {
            System.err.println("Invalid data in table, check logs: " + e.getMessage());
        }
    }

    private boolean isIncrementByFive(String serviceCode) {
        return serviceCode.equals("1.13.") || serviceCode.equals("1.14.") || serviceCode.equals("3.1.14.")
                || serviceCode.equals("3.1.15.") || serviceCode.equals("3.1.16.");
    }

    public static class ExcelGenerator {
        public static void generateActOfWorks(final String fileName, final ObservableList<Service> services,
                final String customerName, final LocalDate date) {
            try (Workbook workbook = new XSSFWorkbook()) {
                DateTimeFormatter pattern = DateTimeFormatter.ofPattern("dd-MM-yyyy");
                String formattedDate = date.format(pattern);
                //TODO customerName - disabled
                Sheet sheet = workbook.createSheet("Акт выполненных работ");
                double sum = services.stream().mapToDouble(Service::getTotalPrice).sum();
                BigDecimal bd = new BigDecimal(Double.toString(sum));
                bd = bd.setScale(2, RoundingMode.HALF_UP);
                sheet.setColumnWidth(0, 300 * 20);
                sheet.setColumnWidth(1, 300 * 20);
                sheet.setColumnWidth(2, 300 * 20);

                Font boldFont = workbook.createFont();
                boldFont.setFontName("Times New Roman");
                boldFont.setFontHeightInPoints((short) 9);

                Font font = workbook.createFont();
                font.setFontName("Times New Roman");
                font.setFontHeightInPoints((short) 10);
                font.setBold(true);

                CellStyle style = workbook.createCellStyle();
                style.setFont(font);

                CellStyle boldStyle = workbook.createCellStyle();
                boldStyle.setFont(boldFont);

                CellStyle borderedCellStyle = workbook.createCellStyle();
                borderedCellStyle.setAlignment(HorizontalAlignment.CENTER);
                borderedCellStyle.setBorderTop(BorderStyle.THIN);
                borderedCellStyle.setBorderBottom(BorderStyle.THIN);
                borderedCellStyle.setBorderLeft(BorderStyle.THICK);
                borderedCellStyle.setBorderRight(BorderStyle.THIN);
                borderedCellStyle.setFont(boldFont);

                createRow(sheet, 0, "Акт выполненных работ", style);
                createRow(sheet, 1,
                        "ЧМУП \"ТикоМед\"                                                                                    8 7 6 5 4 3 2 1  1 2 3 4 5 6 7 8",
                        boldStyle);
                createRow(sheet, 2,
                        "г. Минск, пер. Музыкальный 3,                                                                 8 7 6 5 4 3 2 1  1 2 3 4 5 6 7 8",
                        boldStyle);
                createRow(sheet, 3, "Лицензия №02040/0571005, зарегистрирована 09.10.2014г.", boldStyle);
                createRow(sheet, 4, "Министерством Здравоохранения РБ за № М-6099", boldStyle);
                createRow(sheet, 5, "Дата: " + formattedDate, boldStyle);
                createRow(sheet, 6, "Заказчик: _________________________________________________ ", boldStyle);
                createRow(sheet, 7, "Исполнитель: ______________________________________________", boldStyle);
                createRow(sheet, 8, "", style);
                createDataTableFormula(sheet, borderedCellStyle);
                createRow(sheet, 12, "", style);
                createDataTable(sheet, services, borderedCellStyle);
                createRow(sheet, services.size() + 14, "", style);
                createRow(sheet, services.size() + 15, "ИТОГО: " + bd + " белорусских рублей", style);
                createRow(sheet, services.size() + 16, "", style);
                createRow(sheet, services.size() + 17,
                        "Исполнитель _________________                               Заказчик _________________",
                        style);
                try (FileOutputStream fileOut = new FileOutputStream(fileName)) {
                    workbook.write(fileOut);
                }
                System.out.println("Excel created.");
            } catch (IOException e) {
                System.out.println("Excel error: " + e.getMessage());
            }
        }

        private static void createRow(Sheet sheet, int rowNum, String content, CellStyle style) {
            Row row = sheet.createRow(rowNum);
            Cell cell = row.createCell(0);
            cell.setCellValue(content);
            cell.setCellStyle(style);
            sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum, 0, 13));
        }

        private static void createDataTable(Sheet sheet, List<Service> services, CellStyle borderedCellStyle) {
            // Заголовки таблицы
            Row headerRow = sheet.createRow(sheet.getLastRowNum() + 1);
            createCell(headerRow, 0, "Код услуги", borderedCellStyle);
            createCell(headerRow, 1, "Кол-во", borderedCellStyle);
            createCell(headerRow, 2, "Сумма, бел. руб.", borderedCellStyle);

            for (Service service : services) {
                Row row = sheet.createRow(sheet.getLastRowNum() + 1);
                createCell(row, 0, service.getCode(), borderedCellStyle);
                createCell(row, 1, String.valueOf(service.getQuantity()), borderedCellStyle);
                createCell(row, 2, String.valueOf(format("%.2f", service.getTotalPrice())), borderedCellStyle);
            }
        }

        private static void createDataTableFormula(Sheet sheet, CellStyle borderedCellStyle) {
            // Заголовки таблицы
            Row headerRow = sheet.createRow(sheet.getLastRowNum() + 1);
            createCell(headerRow, 0, "Зубная формула", borderedCellStyle);
            createCell(headerRow, 1, "Диагноз", borderedCellStyle);
            createCell(headerRow, 2, "Класс", borderedCellStyle);

            for (int i = 0; i < 2; i++) {
                Row row = sheet.createRow(sheet.getLastRowNum() + 1);
                createCell(row, 0, "", borderedCellStyle);
                createCell(row, 1, "", borderedCellStyle);
                createCell(row, 2, "", borderedCellStyle);
            }
        }

        private static void createCell(Row row, int column, String value, CellStyle style) {
            Cell cell = row.createCell(column);
            cell.setCellValue(value);
            cell.setCellStyle(style);
        }
    }
}
