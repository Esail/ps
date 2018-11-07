package update;


import org.jblas.FloatMatrix;

public interface Updater {

	FloatMatrix update(String t, FloatMatrix w, FloatMatrix dw);
	String getName();
}
