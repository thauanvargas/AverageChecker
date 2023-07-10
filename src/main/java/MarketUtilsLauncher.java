import gearth.extensions.ExtensionForm;
import gearth.extensions.ExtensionFormCreator;
import gearth.extensions.ExtensionInfo;
import gearth.extensions.ThemedExtensionFormCreator;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.net.URL;

public class MarketUtilsLauncher extends ThemedExtensionFormCreator {

    @Override
    public ExtensionForm createForm(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("MarketUtils.fxml"));
        Parent root = loader.load();

        primaryStage.setTitle("Market Utils");
        primaryStage.setScene(new Scene(root));
        primaryStage.setResizable(false);
        primaryStage.setAlwaysOnTop(true);
        primaryStage.getIcons().add(new Image("icon_white_bg.png"));

        return loader.getController();
    }

    @Override
    protected String getTitle() {
        return "Market Utils v" + MarketUtils.class.getAnnotation(ExtensionInfo.class).Version();
    }

    @Override
    protected URL getFormResource() {
        return getClass().getResource("/gtrigger.fxml");
    }

    public static void main(String[] args) {
        runExtensionForm(args, MarketUtilsLauncher.class);
    }
}