package fm.bootifulpodcast.bindings;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import lombok.Value;
import lombok.experimental.Accessors;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class Main {

	public static void main(String[] args) {
		var bill = new Bill();
		bill.amountDueProperty().addListener((o, oldVal, newVal) -> log.info("Electric bill has changed!"));
		bill.amountDueProperty().setValue(100.00);


	}
}


@Accessors(fluent = true)
@Value
class Bill {
	private final DoubleProperty amountDueProperty = new SimpleDoubleProperty();
}