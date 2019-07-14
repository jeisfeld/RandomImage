package de.jeisfeld.randomimage.util;

import android.widget.EditText;

import java.text.DecimalFormat;
import java.util.Locale;

/**
 * Class with utility methods for formatting.
 */
public final class FormattingUtil {
	/**
	 * Number 100. Used for percentage calculation.
	 */
	private static final int HUNDRED = 100;

	/**
	 * Hide default constructor.
	 */
	private FormattingUtil() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Get a rounded percentage String out of a probability.
	 *
	 * @param probability The probability.
	 * @return The percentage String.
	 */
	public static String getPercentageString(final double probability) {
		if (probability >= 0.1) { // MAGIC_NUMBER
			return new DecimalFormat("###.#").format(probability * HUNDRED);
		}
		else {
			return String.format(Locale.getDefault(), "%1$,.2G", probability * HUNDRED);
		}
	}

	/**
	 * Get percentage value from an EditText.
	 *
	 * @param inputField The EditText where the percentage value is entered.
	 * @return The double representing the percentage value.
	 */
	public static Double getPercentageValue(final EditText inputField) {
		String percentageString = inputField.getText().toString().replace(',', '.');
		Double weight = null;
		if (percentageString.length() > 0) {
			weight = Double.parseDouble(percentageString) / HUNDRED;
			if (weight > 1) {
				weight = 1.0;
			}
			if (weight < 0) {
				weight = 0.0;
			}
		}
		return weight;
	}
}
