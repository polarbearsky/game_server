package flex.messaging.io.amf;

import flex.messaging.io.SerializationContext;

public class AmfObjectFactory {
	private static final ThreadLocal<Amf3Input> amf3InputStore = new ThreadLocal<Amf3Input>() {
		protected Amf3Input initialValue() {
			return new Amf3Input(SerializationContext.getSerializationContext());
		}
	};

	private static final ThreadLocal<Amf3Output> amf3OutputStore = new ThreadLocal<Amf3Output>() {
		protected Amf3Output initialValue() {
			return new Amf3Output(SerializationContext
					.getSerializationContext());
		}
	};

	public static Amf3Input createAmf3Input() {
		Amf3Input input = amf3InputStore.get();
		input.reset();
		return input;
	}

	public static Amf3Output createAmf3Output() {
		Amf3Output output = amf3OutputStore.get();
		output.reset();
		return output;
	}
}
