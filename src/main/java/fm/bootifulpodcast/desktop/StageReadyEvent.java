package fm.bootifulpodcast.desktop;

import javafx.stage.Stage;
import org.springframework.context.ApplicationEvent;

public class StageReadyEvent extends ApplicationEvent {

	public StageReadyEvent(Stage stage) {
		super(stage);
	}

	@Override
	public Stage getSource() {
		return (Stage) super.getSource();
	}

}
