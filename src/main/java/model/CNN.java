package model;

import activations.Relu;
import activations.Softmax;
import com.google.common.collect.Lists;

import layer.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import loss.SoftmaxLoss;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import update.AdamUpdater;

import java.util.List;

// 卷积神经网络（Convolutional Neural Network, CNN
@EqualsAndHashCode(callSuper = true)
@Data
public class CNN extends FullConnectedNN {

	private static Logger logger = LoggerFactory.getLogger(CNN.class);

	public static CNN buildModel(int inputW, int inputH, int inputD, int[] fcLayerDims) {
		// model construct
		CNN nn = new CNN();
		nn.getUpdater().put("default", new AdamUpdater(0.005, 0.9, 0.999, Math.pow(10, -8)));
		nn.setLoss(new SoftmaxLoss());
		List<Layer> layers = Lists.newArrayList();
		List<Layer> inputs = Lists.newArrayList();
		// 输入层
		Layer numberFeatureLayer = new InputLayer("number", 0, inputD * inputW * inputH).setIsInput(true);
		Conv2DLayer conv1 = new Conv2DLayer("conv1", inputW, inputH, inputD, 3, 1, 16, 1);
		conv1.setActivation(new Relu());
		logger.info("conv1 {} * {}", conv1.getOutputW(), conv1.getOutputH());
		PoolingLayer pool1 = new PoolingLayer("pool1", conv1.getOutputW(), conv1.getOutputH(), conv1.getK(), 2, 2);
		logger.info("pool1 {} * {}", pool1.getOutputW(), pool1.getOutputH());
		Conv2DLayer conv2 = new Conv2DLayer("conv2", pool1.getOutputW(), pool1.getOutputH(), pool1.getK(), 3, 1, 32, 1);
		conv2.setActivation(new Relu());
		logger.info("conv2 {} * {}", conv2.getOutputW(), conv2.getOutputH());
		PoolingLayer pool2 = new PoolingLayer("pool2", conv2.getOutputW(), conv2.getOutputH(), conv2.getK(), 2, 2);
		logger.info("pool2 {} * {}", pool2.getOutputW(), pool2.getOutputH());
		// 全连接
		logger.info("cnn {}", pool2.getOutputDims());
		List<Layer> fcLayers = FcLayer.build(pool2.getOutputDims(), fcLayerDims);
		((FcLayer)fcLayers.get(fcLayers.size() - 1)).setActivation(new Softmax());
		/*
		 * 组织层关系
		 * number -> conv1 -> max pooling -> conv2 -> max pooling -> full connected -> softmax
		 */
		numberFeatureLayer.setNext(conv1);
		conv1.setNext(pool1);
		pool1.setNext(conv2);
		conv2.setNext(pool2);
		pool2.setNext(fcLayers.get(0));
		// 添加到layers
		layers.add(conv1);
		layers.add(pool1);
		layers.add(conv2);
		layers.add(pool2);
		layers.addAll(fcLayers);
		// 添加到inputs
		inputs.add(numberFeatureLayer);
		nn.setInputs(inputs);
		nn.setLayers(layers);
		return nn;
	}

}
