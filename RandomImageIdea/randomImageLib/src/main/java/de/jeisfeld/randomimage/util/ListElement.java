package de.jeisfeld.randomimage.util;

import java.util.Properties;
import java.util.regex.Pattern;

/**
 * An element of an image list.
 */
public class ListElement {
	/**
	 * The dummy nested list, containing all files and folders.
	 */
	public static final ListElement DUMMY_NESTED_FOLDER = new ListElement(Type.FOLDER, "");

	/**
	 * The mType of a list element.
	 */
	private Type mType;
	/**
	 * The mName of a list element.
	 */
	private String mName;
	/**
	 * The properties of an element.
	 */
	private Properties mProperties = null;

	/**
	 * Constructor for a List Element.
	 *
	 * @param type The mType.
	 * @param name The mName.
	 */
	public ListElement(final Type type, final String name) {
		this.mType = type;
		this.mName = name;
	}

	/**
	 * Set the properties.
	 *
	 * @param properties The properties.
	 */
	public void setProperties(final Properties properties) {
		mProperties = properties;
	}

	/**
	 * Get the properties.
	 *
	 * @return The properties.
	 */
	public Properties getProperties() {
		if (mProperties == null) {
			mProperties = new Properties();
		}
		return mProperties;
	}

	public final Type getType() {
		return mType;
	}

	public final String getName() {
		return mName;
	}

	@Override
	public final int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((mName == null) ? 0 : mName.hashCode());
		result = prime * result + ((mType == null) ? 0 : mType.hashCode());
		return result;
	}

	@Override
	public final boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		ListElement other = (ListElement) obj;
		if (mName == null) {
			if (other.mName != null) {
				return false;
			}
		}
		else if (!mName.equals(other.mName)) {
			return false;
		}
		if (mType != other.mType) {
			return false;
		}
		return true;
	}

	@Override
	public final String toString() {
		StringBuilder result = new StringBuilder(getType().toString());
		result.append(":");
		result.append(getName());
		if (getProperties().size() > 0) {
			result.append(":");
			result.append(getProperties());
		}
		return result.toString();
	}

	/**
	 * The types of list elements.
	 */
	public enum Type {
		/**
		 * A nested list.
		 */
		NESTED_LIST("Nested List", "nestedList"),
		/**
		 * A folder.
		 */
		FOLDER("Folder", "folder"),
		/**
		 * A file.
		 */
		FILE("File"),
		/**
		 * A missing path.
		 */
		MISSING_PATH("Missing path");

		/**
		 * The prefix used in the configuration file.
		 */
		private String mPrefix = null;
		/**
		 * Descriptive name of the type.
		 */
		private String mDescription;
		/**
		 * Pattern for finding the element entries in the file.
		 */
		private Pattern mElementPattern;

		/**
		 * Pattern for finding the element property entries in the file.
		 */
		private Pattern mPropertyPattern;

		/**
		 * Base constructor of a type.
		 *@param description The description.
		 */
		Type(final String description) {
			this.mDescription = description;
		}

		/**
		 * Constructor of a type, with prefix.
		 *@param description The description.
		 * @param prefix The prefix.
		 */
		Type(final String description, final String prefix) {
			this.mDescription = description;
			this.mPrefix = prefix;
			mElementPattern = Pattern.compile("^" + prefix + "\\[(\\d+)]");
			mPropertyPattern = Pattern.compile("^" + prefix + "\\.([^\\[]*)\\[(\\d+)]");
		}

		/**
		 * Get the prefix of the type.
		 *
		 * @return The prefix.
		 */
		public String getPrefix() {
			return mPrefix;
		}

		/**
		 * Get information if the type has a prefix.
		 *
		 * @return The prefix.
		 */
		public boolean hasPrefix() {
			return mPrefix != null;
		}

		/**
		 * Get the description of a type.
		 *
		 * @return The descriptive name.
		 */
		public String getDescription() {
			return mDescription;
		}

		public Pattern getElementPattern() {
			return mElementPattern;
		}

		public Pattern getPropertyPattern() {
			return mPropertyPattern;
		}

	}
}
