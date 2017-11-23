package com.jrod.awsinitializer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;

@Component
public class GuiController {
    @FXML
    private TextField mfaArn;

    @FXML
    private TextField mfaCode;

    @FXML
    private TextField awsCliPath;

    @FXML
    private TextField awsRegion;

    @FXML
    private TextField accessKeyId;

    @FXML
    private TextField secretAccessKey;

    private Stage stage;

    private Properties properties;

    public GuiController() {
        this.properties = new Properties();
    }

    void init() {
        Path propsPath = getAwsFolderPath().resolve("AwsCliInitializer.properties");
        if (Files.exists(propsPath)) {
            try {
                properties.load(Files.newInputStream(propsPath));
            } catch (IOException e) {
                e.printStackTrace();
            }
            updateSettingsTab();
        } else {
            properties.setProperty("awsCliPath", "");
            properties.setProperty("awsRegion", "");
            properties.setProperty("accessKeyId", "");
            properties.setProperty("secretAccessKey", "");
            properties.setProperty("mfaArn", "");
            updateSettingsTab();
        }
    }

    @FXML
    private void submit() throws IOException {
        deleteCredentialsIfExists();
        createDefaultCredentials();
        String[] command = {awsCliPath.getText(), "sts", "get-session-token", "--serial-number", mfaArn.getText(), "--token-code", mfaCode.getText()};
        String[] results = executeCommand(command);
        if (!results[1].isEmpty()) {
            createAlert(results[1], "Error!");
        }
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> map = mapper.readValue(results[0], new TypeReference<Map<String, Object>>() {
        });
        Map<String, String> tokenMap = (Map<String, String>) map.get("Credentials");
        String token = tokenMap.get("SessionToken");
        String keyId = tokenMap.get("AccessKeyId");
        String secret = tokenMap.get("SecretAccessKey");
        createCredentials(keyId, secret, token);
        createAlert("Success!!!", "Success!");
    }

    void updateSettingsTab() {
        awsCliPath.setText(properties.getProperty("awsCliPath"));
        awsRegion.setText(properties.getProperty("awsRegion"));
        accessKeyId.setText(properties.getProperty("accessKeyId"));
        secretAccessKey.setText(properties.getProperty("secretAccessKey"));
        mfaArn.setText(properties.getProperty("mfaArn"));
    }

    @FXML
    void writeSettingsProperties() throws IOException {
        properties.setProperty("awsCliPath", awsCliPath.getText());
        properties.setProperty("awsRegion", awsRegion.getText());
        properties.setProperty("accessKeyId", accessKeyId.getText());
        properties.setProperty("secretAccessKey", secretAccessKey.getText());
        properties.setProperty("mfaArn", mfaArn.getText());
        Path propsPath = getAwsFolderPath().resolve("AwsCliInitializer.properties");
        properties.store(Files.newOutputStream(propsPath), "Created By the Aws Cli Initializer.");
        createAlert("Properties updated and written to: " + propsPath.toString(), "Success!");
    }

    void createDefaultCredentials() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[default]\n");
        stringBuilder.append("aws_access_key_id = ");
        stringBuilder.append(properties.get("accessKeyId"));
        stringBuilder.append("\n");
        stringBuilder.append("aws_secret_access_key = ");
        stringBuilder.append(properties.get("secretAccessKey"));
        stringBuilder.append("\n");
        try {
            Files.write(getAwsFolderPath().resolve("credentials"), stringBuilder.toString().getBytes());
        } catch (Exception e) {
            createAlert(e.getMessage(), "Unable to write default credentials!");
        }
    }

    void createCredentials(String accessKeyId, String secretAccessKey, String sessionToken) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[default]\n");
        stringBuilder.append("aws_access_key_id = ");
        stringBuilder.append(accessKeyId);
        stringBuilder.append("\n");
        stringBuilder.append("aws_secret_access_key = ");
        stringBuilder.append(secretAccessKey);
        stringBuilder.append("\n");
        stringBuilder.append("aws_session_token = ");
        stringBuilder.append(sessionToken);
        stringBuilder.append("\n");
        try {
            Files.write(getAwsFolderPath().resolve("credentials"), stringBuilder.toString().getBytes());
        } catch (Exception e) {
            createAlert(e.getMessage(), "Unable to write credentials!");
        }
    }

    void deleteCredentialsIfExists() {
        try {
            Files.deleteIfExists(getAwsFolderPath().resolve("credentials"));
        } catch (IOException e) {
            createAlert(e.getMessage(), "Unable to delete old credentials!");
        }
    }

    Path getAwsFolderPath() {
        String homeDirString = System.getProperty("user.home");
        return Paths.get(homeDirString).resolve(".aws");
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void createAlert(String message, String title) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    String[] executeCommand(String[] command) {
        String[] results = new String[3];
        try {
            Process process = Runtime.getRuntime().exec(command);
            process.waitFor();
            results[0] = IOUtils.toString(process.getInputStream(), Charset.defaultCharset());
            results[1] = IOUtils.toString(process.getErrorStream(), Charset.defaultCharset());
            results[2] = String.valueOf(process.exitValue());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return results;
    }
}
