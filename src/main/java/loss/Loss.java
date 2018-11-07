package loss;

import org.jblas.FloatMatrix;

public interface Loss {

	float forward(FloatMatrix predict, FloatMatrix label);
	FloatMatrix backward(FloatMatrix predict, FloatMatrix label);
}
