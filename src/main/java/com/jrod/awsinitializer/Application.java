package com.jrod.awsinitializer;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

@SpringBootApplication
public class Application extends javafx.application.Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("Aws Credential Initializer");
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui.fxml"));
        ApplicationContext cxt = new AnnotationConfigApplicationContext(Application.class);
        loader.setControllerFactory(cxt::getBean);
        Parent root = loader.load();
        GuiController controller = loader.getController();
        controller.setStage(primaryStage);

        primaryStage.setScene(new Scene(root));
        primaryStage.show();
        controller.init();
    }

    public static void main(String[] args) {
        ApplicationContext ctx = SpringApplication.run(Application.class, args);
        launch(args);
    }
}

