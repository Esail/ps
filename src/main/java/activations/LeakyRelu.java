package activations;

import org.jblas.FloatMatrix;

// f(x)=alpha * x for x < 0, f(x) = x for x>=0
public class LeakyRelu implements Activation{

    public FloatMatrix forward(FloatMatrix x) {
        for (int i = 0; i < x.length; i ++) {
            x.data[i] = Math.max(0.01f * x.data[i], x.data[i]);
        }
        return x;
    }

    public FloatMatrix backward(FloatMatrix dy, FloatMatrix preY, FloatMatrix y) {
        for (int i = 0; i < y.length; i++) {
            dy.data[i] *= y.data[i] > 0.0f ? 1.0f : 0.01f;
        }
        return dy;
    }

}
