package layer;

import activations.Activation;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jblas.FloatMatrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.MatrixUtil;

@EqualsAndHashCode(callSuper = true)
@Data
public class AddLayer extends Layer {
	private static Logger logger = LoggerFactory.getLogger(AddLayer.class);

	protected Layer left;
	protected Layer right;
	protected FloatMatrix Z;
	protected Activation activation;

	public AddLayer(String name, Layer left, Layer right) {
		super(name, 0, 0);
		this.left = left;
		this.right = right;
	}

	public void clear() {}

	public FloatMatrix forward() {
		FloatMatrix l = left.A;
		FloatMatrix r = right.A;
		Z = l.add(r);
		if (logger.isDebugEnabled()) {
			logger.debug("Z {}", MatrixUtil.pretty(Z));
		}
		// 没有激活函数
		if (activation != null) {
			this.A = activation.forward(Z);
		} else {
			this.A = Z;
		}
		return this.A;
	}

	public FloatMatrix backward() {
		FloatMatrix delta;
		if (next == null) {
			delta = this.delta;
		} else {
			delta = next.delta;
		}
		if (activation != null) {
			delta = activation.backward(delta, Z, A);
		}
		this.delta = delta;
		return this.delta;
	}

	public void pullWeights() {
	}
}
