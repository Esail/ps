package activations;

import org.jblas.FloatMatrix;

public interface Activation {
	// 前向
	FloatMatrix forward(FloatMatrix x);
	// 后向
	FloatMatrix backward(FloatMatrix dy, FloatMatrix preY, FloatMatrix y);
}