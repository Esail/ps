package model;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import context.Context;
import evaluate.AUC;
import layer.*;
import lombok.Data;
import loss.CrossEntropy;
import loss.Loss;
import org.jblas.FloatMatrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import update.AdamUpdater;
import update.Updater;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

@Data
public class DNN implements Model {

	private static Logger logger = LoggerFactory.getLogger(DNN.class);

	private Loss loss = new CrossEntropy();

	private List<Layer> layers = Lists.newArrayList();

	private List<Layer> inputs = Lists.newArrayList();

	private Map<String, Updater> updater = Maps.newHashMap();

	public float train(Map<String, FloatMatrix> datas) {
		FloatMatrix E = datas.get("E");
		FloatMatrix X = datas.get("X");
		FloatMatrix Y = datas.get("Y");
		// category
		inputs.get(0).setA(E);
		// number
		inputs.get(1).setA(X);
		// 前向
		for (Layer layer : layers) {
			layer.forward();
		}
		// loss计算
		FloatMatrix P = layers.get(layers.size()-1).getA();
		float lossVal = loss.forward(P, Y);
		FloatMatrix delta = loss.backward(P , Y);
		logger.debug("\nP {}\nY {}\ndelta {}", P, Y, delta);
		logger.info("LOSS is {}", lossVal);
		if (Context.step.get() % Context.nTermDump == 0) {
			AUC auc = new AUC(P.toArray(), Y.toArray());
			logger.info("Train AUC {}", auc.calculate());
			logger.info("\n\nP:{} \nY:{} \nD:{}\n", P.getRange(0, Math.min(20, P.columns)), Y.getRange(0, Math.min(20, Y.columns)), delta.getRange(0, Math.min(20, delta.columns)));
		}
		if (lossVal <= CrossEntropy.slim || Float.isNaN(lossVal)) {
			Context.finish = true;
			logger.info("\n\nP:{} \nY:{} \n\n", P, Y, delta);
			logger.info("Oh Yeah lossVal is too slim !!! model train success");
			return lossVal;
		}
		layers.get(layers.size() - 1).setDelta(delta);
		// 反向求导
		for (int i=layers.size() - 1; i>=0; i--) {
			layers.get(i).backward();
		}
		return lossVal;
	}

	public void pullWeights() {
		IntStream.range(0, layers.size()).forEach(i -> layers.get(i).pullWeights());
	}

	public FloatMatrix predict(Map<String, FloatMatrix> datas) {
		FloatMatrix E = datas.get("E");
		FloatMatrix X = datas.get("X");
		// category
		inputs.get(0).setA(E);
		// number
		inputs.get(1).setA(X);
		// 前向
		for (Layer layer : layers) {
			layer.forward();
		}
		return layers.get(layers.size()-1).getA();
	}

	public static DNN buildModel(int embeddingFieldNum, int embeddingSize, int numberFieldNum, int[] fcLayerDims) {
		// model construct
		DNN nn = new DNN();
		nn.getUpdater().put("default", new AdamUpdater(0.005, 0.9, 0.999, Math.pow(10, -8)));
		nn.setLoss(new CrossEntropy());
		List<Layer> layers = Lists.newArrayList();
		List<Layer> inputs = Lists.newArrayList();
		// 输入层
		Layer categoryFeatureLayer = new InputLayer("category", 0, embeddingFieldNum * embeddingSize).setIsInput(true);
		Layer numberFeatureLayer = new InputLayer("number", 0, numberFieldNum).setIsInput(true);
		// embedding 层
		Layer embeddingLayer = new EmbeddingLayer("embedding", embeddingFieldNum, embeddingFieldNum * embeddingSize).build(embeddingFieldNum, embeddingSize);
		// 合并 numberFeature和embedding
		Layer concatLayer = new ConcatLayer("concat", Lists.newArrayList(embeddingLayer, numberFeatureLayer));
		// 全连接
		int inputSize = concatLayer.getOutputDims();
		List<Layer> fcLayers = FcLayer.build(inputSize, fcLayerDims);

		/*
		 * 组织层关系
		 * category -> embedding + number -> full connected
		 */
		categoryFeatureLayer.setNext(embeddingLayer);
		numberFeatureLayer.setNext(concatLayer);
		embeddingLayer.setNext(concatLayer);
		concatLayer.setNext(fcLayers.get(0));
		// 添加到layers
		layers.add(embeddingLayer);
		layers.add(concatLayer);
		layers.addAll(fcLayers);
		// 添加到inputs
		inputs.add(categoryFeatureLayer);
		inputs.add(numberFeatureLayer);
		nn.setInputs(inputs);
		nn.setLayers(layers);
		return nn;
	}

}
