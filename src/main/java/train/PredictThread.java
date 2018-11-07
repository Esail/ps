package train;

import context.Context;
import model.Model;
import org.jblas.FloatMatrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.Callable;

public class PredictThread implements Callable<FloatMatrix> {

	private static Logger logger = LoggerFactory.getLogger(PredictThread.class);

	private Map<String, FloatMatrix> datas;

	private Model nn;

	private int modelIndex;

	public PredictThread(int modelIndex, Model nn, Map<String, FloatMatrix> datas) {
		this.modelIndex = modelIndex;
		this.datas = datas;
		this.nn = nn;
	}

	@Override
	public FloatMatrix call() throws Exception{
		try {
			Context.modelIndex.set(modelIndex);
			// pull weights
			nn.pullWeights();
			return nn.predict(datas);
		} catch (Exception e) {
			logger.error("trainer error", e);
			throw new Exception(e);
		}
	}
}