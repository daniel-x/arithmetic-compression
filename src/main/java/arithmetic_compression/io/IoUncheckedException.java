package arithmetic_compression.io;

@SuppressWarnings("serial")
public class IoUncheckedException extends RuntimeException {

	public IoUncheckedException() {
		super();
	}

	public IoUncheckedException(Throwable cause) {
		super(cause);
	}

	public IoUncheckedException(String message) {
		super(message);
	}

	public IoUncheckedException(String message, Throwable cause) {
		super(message, cause);
	}

}
