package arithmetic_compression.io;

@SuppressWarnings("serial")
public class EofUncheckedException extends IoUncheckedException {

	public EofUncheckedException() {
		super();
	}

	public EofUncheckedException(Throwable cause) {
		super(cause);
	}

	public EofUncheckedException(String message) {
		super(message);
	}

	public EofUncheckedException(String message, Throwable cause) {
		super(message, cause);
	}

}
