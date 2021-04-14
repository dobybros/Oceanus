package script.filter;


/**
 * 
 * @author aplomb
 *
 * @param <T>
 */
public interface JsonFilter<T> {
	Object filter(T target, Object... arguments);

	T from(Object doc, Object... arguments);
}
