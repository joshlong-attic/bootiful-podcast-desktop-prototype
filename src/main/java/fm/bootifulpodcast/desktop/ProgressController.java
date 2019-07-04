package fm.bootifulpodcast.desktop;

import fm.bootifulpodcast.desktop.client.ApiClient;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.net.URI;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class ProgressController implements Initializable, EventHandler<MouseEvent> {

	private final String processingStatus;

	private final String fileDoneAlertTitle;

	private final Messages messages;

	private final String fileChooserTitle;

	private final AtomicReference<URI> uri = new AtomicReference<URI>();

	private final AtomicReference<Stage> stage = new AtomicReference<Stage>();

	private final ApiClient client;

	public Label processingLabel;

	public Hyperlink downloadMediaHyperlink;

	public ProgressController(Messages messages, ApiClient client) {
		this.messages = messages;
		this.client = client;

		var controllerClass = ProgressController.class;
		this.fileChooserTitle = this.messages.getMessage(controllerClass,
				"file-chooser-title");
		this.fileDoneAlertTitle = this.messages.getMessage(controllerClass,
				"file-done-alert-title");
		this.processingStatus = this.messages.getMessage(controllerClass,
				"processing-status");
	}

	@Override
	public void initialize(URL url, ResourceBundle resourceBundle) {
		this.processingLabel.setText(processingStatus);
		this.downloadMediaHyperlink
				.setText(this.messages.getMessage(getClass(), "click-to-download"));
		this.downloadMediaHyperlink.setVisible(false);
		this.downloadMediaHyperlink.setOnMouseClicked(this);
	}

	@EventListener
	public void processingCompleted(PodcastProductionCompletedEvent completed) {
		this.uri.set(completed.getSource().getMedia());
		Platform.runLater(() -> this.downloadMediaHyperlink.setVisible(true));
	}

	@EventListener
	public void stageIsReady(StageReadyEvent readyEvent) {
		this.stage.set(readyEvent.getSource());
	}

	@Override
	public void handle(MouseEvent mouseEvent) {
		var resolvedUri = this.uri.get();
		Platform.runLater(() -> {
			var extFilter = new FileChooser.ExtensionFilter(this.fileChooserTitle,
					"*.mp3", "*.wav");
			var fileChooser = new FileChooser();
			fileChooser.getExtensionFilters().add(extFilter);
			Assert.notNull(this.stage.get(), "the stage must have been set");
			Optional//
					.ofNullable(fileChooser.showSaveDialog(this.stage.get())) //
					.ifPresent(file -> this.client.download(resolvedUri, file)
							.thenAccept(downloadedFile -> Platform.runLater(() -> {
								var alert = new Alert(Alert.AlertType.INFORMATION);
								alert.setTitle(this.fileDoneAlertTitle);
								alert.setHeaderText(null);
								alert.setContentText(
										messages.getMessage(ProgressController.class,
												"file-has-been-downloaded",
												downloadedFile.getAbsolutePath()));
								alert.showAndWait();
							})));
		});

	}

}
