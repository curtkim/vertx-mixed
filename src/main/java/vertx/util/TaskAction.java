package vertx.util;

public interface TaskAction {
	void execute(Object task, int i, Callback callback);
}
