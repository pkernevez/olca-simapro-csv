package org.openlca.simapro.csv.enums;

public enum ProcessType {

	SYSTEM("System"),

	UNIT_PROCESS("Unit process");

	private final String value;

	ProcessType(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public static ProcessType forValue(String value) {
		for (ProcessType type : values())
			if (type.getValue().equals(value))
				return type;
		return null;
	}

}
