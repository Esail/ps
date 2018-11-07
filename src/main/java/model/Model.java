package model;

import org.jblas.FloatMatrix;
import update.Updater;

import java.util.Map;

public interface Model {

	float train(Map<String, FloatMatrix> datas);
	FloatMatrix predict(Map<String, FloatMatrix> datas);
	void pullWeights();
	Map<String, Updater> getUpdater();
}